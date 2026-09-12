package com.zeronetwork.connectivity.network

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.zeronetwork.connectivity.data.model.MessageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

sealed class FileTransferEvent {
    data class Progress(val transferId: String, val bytesTransferred: Long, val totalBytes: Long, val speedKbps: Float) : FileTransferEvent()
    data class Completed(val transferId: String, val senderIp: String, val fileName: String, val filePath: String, val fileSize: Long, val type: MessageType) : FileTransferEvent()
    data class Failed(val transferId: String, val errorMessage: String) : FileTransferEvent()
}

class LanFileTransferService(private val context: Context) {
    private val TAG = "LanFileTransferService"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _transferEvents = MutableSharedFlow<FileTransferEvent>(extraBufferCapacity = 128)
    val transferEvents: SharedFlow<FileTransferEvent> = _transferEvents.asSharedFlow()

    fun startServer() {
        serverJob?.cancel()
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(NetworkConstants.TCP_FILE_PORT)
                Log.d(TAG, "TCP File Server listening on port ${NetworkConstants.TCP_FILE_PORT}")

                while (isActive) {
                    val clientSocket = serverSocket?.accept() ?: break
                    scope.launch {
                        handleIncomingConnection(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "Server socket error: ${e.message}")
                }
            }
        }
    }

    private suspend fun handleIncomingConnection(socket: Socket) = withContext(Dispatchers.IO) {
        val senderIp = socket.inetAddress.hostAddress ?: "unknown"
        try {
            socket.use { sock ->
                val dis = DataInputStream(BufferedInputStream(sock.getInputStream()))
                val magic = dis.readInt()
                if (magic != 0x5A4E4554) { // 'ZNET'
                    Log.w(TAG, "Invalid magic number: $magic")
                    return@use
                }

                val typeOrd = dis.readInt()
                val messageType = MessageType.entries.getOrElse(typeOrd) { MessageType.FILE }
                val transferId = dis.readUTF()
                val fileName = dis.readUTF()
                val fileSize = dis.readLong()

                val saveDir = when (messageType) {
                    MessageType.IMAGE -> File(context.filesDir, "received_photos")
                    MessageType.VIDEO -> File(context.filesDir, "received_videos")
                    MessageType.AUDIO -> File(context.filesDir, "received_audio")
                    else -> File(context.filesDir, "received_files")
                }.apply { mkdirs() }

                val destinationFile = File(saveDir, "${System.currentTimeMillis()}_$fileName")
                val fos = FileOutputStream(destinationFile)
                val bos = BufferedOutputStream(fos)

                val buffer = ByteArray(32768)
                var bytesReadTotal = 0L
                var lastSpeedCheckTime = System.currentTimeMillis()
                var bytesSinceLastCheck = 0L
                var currentSpeed = 0f

                while (bytesReadTotal < fileSize) {
                    val toRead = minOf(buffer.size.toLong(), fileSize - bytesReadTotal).toInt()
                    val read = dis.read(buffer, 0, toRead)
                    if (read == -1) break
                    bos.write(buffer, 0, read)
                    bytesReadTotal += read
                    bytesSinceLastCheck += read

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastSpeedCheckTime
                    if (elapsed >= 300) {
                        currentSpeed = (bytesSinceLastCheck / 1024f) / (elapsed / 1000f)
                        lastSpeedCheckTime = now
                        bytesSinceLastCheck = 0L
                        _transferEvents.emit(FileTransferEvent.Progress(transferId, bytesReadTotal, fileSize, currentSpeed))
                    }
                }
                bos.flush()
                bos.close()

                // Save photos and videos directly to system Gallery!
                if (messageType == MessageType.IMAGE || messageType == MessageType.VIDEO) {
                    saveToGallery(destinationFile, messageType)
                }

                _transferEvents.emit(
                    FileTransferEvent.Completed(
                        transferId = transferId,
                        senderIp = senderIp,
                        fileName = fileName,
                        filePath = destinationFile.absolutePath,
                        fileSize = destinationFile.length(),
                        type = messageType
                    )
                )
                Log.d(TAG, "Received and saved file: $fileName (${destinationFile.length()} bytes)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming file: ${e.message}")
        }
    }

    private fun saveToGallery(file: File, messageType: MessageType) {
        try {
            val resolver = context.contentResolver
            val isImage = messageType == MessageType.IMAGE
            val contentUri = if (isImage) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val mimeType = if (isImage) {
                if (file.name.endsWith(".png", ignoreCase = true)) "image/png" else "image/jpeg"
            } else {
                "video/mp4"
            }
            val relativeDir = if (isImage) "${Environment.DIRECTORY_PICTURES}/ZeroNetwork" else "${Environment.DIRECTORY_MOVIES}/ZeroNetwork"

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(contentUri, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(file).use { input ->
                        input.copyTo(out)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
                Log.d(TAG, "Successfully exported media to Gallery: ${file.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving media to gallery: ${e.message}")
        }
    }

    suspend fun sendFile(
        targetIp: String,
        transferId: String,
        file: File,
        messageType: MessageType
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket(targetIp, NetworkConstants.TCP_FILE_PORT).use { socket ->
                socket.tcpNoDelay = true
                val dos = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
                val fis = FileInputStream(file)
                val bis = BufferedInputStream(fis)

                // Write Header: Magic + Type + TransferId + FileName + FileSize
                dos.writeInt(0x5A4E4554) // 'ZNET'
                dos.writeInt(messageType.ordinal)
                dos.writeUTF(transferId)
                dos.writeUTF(file.name)
                dos.writeLong(file.length())
                dos.flush()

                val buffer = ByteArray(32768)
                val totalBytes = file.length()
                var sentBytes = 0L
                var lastTime = System.currentTimeMillis()
                var bytesInterval = 0L
                var speed = 0f

                var read: Int
                while (bis.read(buffer).also { read = it } != -1) {
                    dos.write(buffer, 0, read)
                    sentBytes += read
                    bytesInterval += read

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastTime
                    if (elapsed >= 300) {
                        speed = (bytesInterval / 1024f) / (elapsed / 1000f)
                        lastTime = now
                        bytesInterval = 0L
                        _transferEvents.emit(FileTransferEvent.Progress(transferId, sentBytes, totalBytes, speed))
                    }
                }
                dos.flush()
                bis.close()
                _transferEvents.emit(FileTransferEvent.Progress(transferId, totalBytes, totalBytes, speed))
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending file to $targetIp: ${e.message}")
            _transferEvents.emit(FileTransferEvent.Failed(transferId, e.message ?: "Transfer failed"))
            return@withContext false
        }
    }

    fun stop() {
        serverJob?.cancel()
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
    }
}

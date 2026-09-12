package com.zeronetwork.connectivity

import android.app.Application
import com.zeronetwork.connectivity.data.local.AppDatabase
import com.zeronetwork.connectivity.data.local.PreferencesManager
import com.zeronetwork.connectivity.data.repository.ChatRepository
import com.zeronetwork.connectivity.data.repository.PeerRepository
import com.zeronetwork.connectivity.network.AppUpdateManager
import com.zeronetwork.connectivity.network.LanCallEngine
import com.zeronetwork.connectivity.network.LanDiscoveryService
import com.zeronetwork.connectivity.network.LanFileTransferService
import com.zeronetwork.connectivity.network.NetworkMonitor

class ZeroNetworkApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var peerRepository: PeerRepository
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var discoveryService: LanDiscoveryService
        private set

    lateinit var fileTransferService: LanFileTransferService
        private set

    lateinit var callEngine: LanCallEngine
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    lateinit var updateManager: AppUpdateManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferencesManager = PreferencesManager(this)
        database = AppDatabase.getInstance(this)
        discoveryService = LanDiscoveryService(this)
        fileTransferService = LanFileTransferService(this)

        peerRepository = PeerRepository(
            peerDao = database.peerDao(),
            discoveryService = discoveryService
        )

        chatRepository = ChatRepository(
            context = this,
            messageDao = database.messageDao(),
            conversationDao = database.conversationDao(),
            peerDao = database.peerDao(),
            discoveryService = discoveryService,
            fileTransferService = fileTransferService,
            preferencesManager = preferencesManager
        )

        callEngine = LanCallEngine(this, discoveryService)
        networkMonitor = NetworkMonitor(this)
        updateManager = AppUpdateManager(this)

        val username = preferencesManager.getUsername()
        discoveryService.start(username)
        fileTransferService.startServer()
    }

    override fun onTerminate() {
        super.onTerminate()
        discoveryService.stop()
        fileTransferService.stop()
        callEngine.endCall()
    }

    companion object {
        lateinit var instance: ZeroNetworkApp
            private set
    }
}

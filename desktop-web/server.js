const express = require('express');
const http = require('http');
const { WebSocketServer, WebSocket } = require('ws');
const dgram = require('dgram');
const net = require('net');
const os = require('os');
const path = require('path');
const fs = require('fs');
const multer = require('multer');

const HTTP_PORT = 5000;
const DISCOVERY_PORT = 1050;
const TCP_FILE_PORT = 1051;
const VOICE_CALL_PORT = 1052;
const VIDEO_CALL_PORT = 1053;

const TYPE_HEARTBEAT = 0;
const TYPE_TEXT = 1;
const TYPE_FILE_CHUNK = 2;
const TYPE_OWN_ADDR = 4;
const TYPE_RICH_SIGNAL = 10;
const MAGIC_ZNET = 0x5A4E4554;

const receivedDir = path.join(__dirname, 'received_files');
if (!fs.existsSync(receivedDir)) {
  fs.mkdirSync(receivedDir, { recursive: true });
}

// Resolve local IP and broadcast addresses
function getLocalNetworkInfo() {
  const interfaces = os.networkInterfaces();
  const result = [];
  let preferredIp = '127.0.0.1';
  let preferredBroadcast = '255.255.255.255';

  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        const ipParts = iface.address.split('.').map(Number);
        const netmaskParts = iface.netmask.split('.').map(Number);
        const broadcastParts = ipParts.map((part, i) => (part | (~netmaskParts[i] & 255)));
        const broadcast = broadcastParts.join('.');

        result.push({
          name,
          address: iface.address,
          broadcast,
          netmask: iface.netmask
        });

        if (!iface.address.startsWith('169.254')) {
          preferredIp = iface.address;
          preferredBroadcast = broadcast;
        }
      }
    }
  }

  return { preferredIp, preferredBroadcast, interfaces: result };
}

let networkInfo = getLocalNetworkInfo();
let myUsername = `Zero Web (${os.hostname().slice(0, 10)})`;
let myAvatarBase64 = null;
let myAvatarColor = '#2F80ED';

const peers = new Map(); // ip -> { ip, username, lastSeen, avatarBase64, isOnline }

// Express Setup
const app = express();
const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: '/ws' });

app.use(express.json({ limit: '50mb' }));
app.use(express.static(path.join(__dirname, 'public')));
app.use('/files', express.static(receivedDir));

// Download APK route for phones/devices on LAN
app.get('/download-apk', (req, res) => {
  const possiblePaths = [
    'C:\\Users\\HP Omnibook X Flip\\Downloads\\Generated APK\\Zero Network Connectivity\\ZeroNetworkConnectivity-v1.0.3.apk',
    path.join(__dirname, '..', 'app', 'build', 'outputs', 'apk', 'release', 'app-release.apk')
  ];
  for (const p of possiblePaths) {
    if (fs.existsSync(p)) {
      return res.download(p, 'ZeroNetworkConnectivity-v1.0.3.apk');
    }
  }
  res.status(404).send('APK not generated yet. Please run build.');
});


const upload = multer({ dest: path.join(__dirname, 'temp_uploads') });

// Broadcast to connected web clients
function broadcastToClients(data) {
  const msg = JSON.stringify(data);
  wss.clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(msg);
    }
  });
}

function sendPeersUpdate() {
  const now = Date.now();
  const peerList = Array.from(peers.values()).map(p => ({
    ...p,
    isOnline: (now - p.lastSeen) < 14000
  }));
  broadcastToClients({ type: 'peers_update', peers: peerList });
}

// Clean up inactive peers every 5s
setInterval(() => {
  const now = Date.now();
  let changed = false;
  for (const [ip, peer] of peers.entries()) {
    if (now - peer.lastSeen > 18000) {
      peers.delete(ip);
      changed = true;
    }
  }
  if (changed) sendPeersUpdate();
}, 5000);

// API Endpoints
app.get('/api/info', (req, res) => {
  networkInfo = getLocalNetworkInfo();
  res.json({
    ip: networkInfo.preferredIp,
    hostname: os.hostname(),
    username: myUsername,
    avatarBase64: myAvatarBase64,
    avatarColor: myAvatarColor,
    peersCount: peers.size
  });
});

app.get('/api/peers', (req, res) => {
  res.json(Array.from(peers.values()));
});

// Upload and send file via TCP 1051
app.post('/api/upload', upload.single('file'), async (req, res) => {
  try {
    const file = req.file;
    const targetIp = req.body.targetIp;
    const messageTypeOrdinal = parseInt(req.body.messageType || '4', 10); // 1: IMAGE, 2: VIDEO, 3: AUDIO, 4: FILE
    const transferId = `trans_${Date.now()}`;

    if (!file || !targetIp) {
      return res.status(400).json({ error: 'Missing file or targetIp' });
    }

    const client = new net.Socket();
    client.connect(TCP_FILE_PORT, targetIp, () => {
      const fileNameBytes = Buffer.from(file.originalname, 'utf8');
      const transferIdBytes = Buffer.from(transferId, 'utf8');
      const fileStat = fs.statSync(file.path);

      // Header: Magic(4) + Type(4) + transferIdLen(2) + transferId + fileNameLen(2) + fileName + fileSize(8)
      const headerLen = 4 + 4 + 2 + transferIdBytes.length + 2 + fileNameBytes.length + 8;
      const header = Buffer.alloc(headerLen);
      let offset = 0;

      header.writeInt32BE(MAGIC_ZNET, offset); offset += 4;
      header.writeInt32BE(messageTypeOrdinal, offset); offset += 4;

      header.writeInt16BE(transferIdBytes.length, offset); offset += 2;
      transferIdBytes.copy(header, offset); offset += transferIdBytes.length;

      header.writeInt16BE(fileNameBytes.length, offset); offset += 2;
      fileNameBytes.copy(header, offset); offset += fileNameBytes.length;

      header.writeBigInt64BE(BigInt(fileStat.size), offset); offset += 8;

      client.write(header);

      // Stream file body
      const readStream = fs.createReadStream(file.path);
      readStream.pipe(client);

      readStream.on('end', () => {
        // Also save a copy in receivedDir for desktop local display
        const localCopyName = `${Date.now()}_${file.originalname}`;
        const destPath = path.join(receivedDir, localCopyName);
        fs.copyFileSync(file.path, destPath);
        fs.unlinkSync(file.path); // clean temp

        res.json({
          success: true,
          transferId,
          fileName: file.originalname,
          fileUrl: `/files/${localCopyName}`,
          fileSize: fileStat.size
        });
      });
    });

    client.on('error', (err) => {
      console.error('TCP File send error:', err.message);
      if (!res.headersSent) {
        res.status(500).json({ error: err.message });
      }
    });
  } catch (err) {
    console.error('Upload handler error:', err);
    res.status(500).json({ error: err.message });
  }
});

// -------------------------------------------------------------
// 1. UDP Discovery & Heartbeat (Port 1050)
// -------------------------------------------------------------
const discoverySocket = dgram.createSocket({ type: 'udp4', reuseAddr: true });

discoverySocket.on('listening', () => {
  discoverySocket.setBroadcast(true);
  console.log(`[Discovery] UDP Socket listening on 0.0.0.0:${DISCOVERY_PORT}`);
});

discoverySocket.on('message', (msg, rinfo) => {
  const senderIp = rinfo.address;
  if (senderIp === networkInfo.preferredIp || senderIp === '127.0.0.1') return;

  if (msg.length === 0) return;
  const firstByte = msg[0];

  // Type 0: TYPE_HEARTBEAT
  if (firstByte === TYPE_HEARTBEAT) {
    const username = msg.length > 1 ? msg.subarray(1).toString('utf8') : 'Peer Device';
    const existing = peers.get(senderIp) || {};
    peers.set(senderIp, {
      ip: senderIp,
      username,
      avatarBase64: existing.avatarBase64 || null,
      lastSeen: Date.now()
    });
    sendPeersUpdate();
  }
  // Type 1: TYPE_TEXT
  else if (firstByte === TYPE_TEXT) {
    const text = msg.length > 1 ? msg.subarray(1).toString('utf8') : '';
    const peer = peers.get(senderIp);
    const senderName = peer ? peer.username : senderIp;
    broadcastToClients({
      type: 'message',
      message: {
        id: `msg_${Date.now()}_${Math.random()}`,
        senderIp,
        senderName,
        type: 'TEXT',
        content: text,
        timestamp: Date.now(),
        isMine: false
      }
    });
  }
  // Type 10: TYPE_RICH_SIGNAL (JSON)
  else if (firstByte === TYPE_RICH_SIGNAL) {
    try {
      const jsonStr = msg.subarray(1).toString('utf8');
      const packet = JSON.parse(jsonStr);

      // Peer isolation: If targetIp is specified and not for this machine, ignore!
      if (packet.targetIp && packet.targetIp !== networkInfo.preferredIp && packet.targetIp !== '127.0.0.1') {
        return;
      }

      switch (packet.type) {
        case 'QR_CONNECT_HANDSHAKE': {
          peers.set(senderIp, {
            ip: senderIp,
            username: packet.senderName || senderIp,
            avatarBase64: packet.avatarBase64 || null,
            lastSeen: Date.now()
          });
          sendPeersUpdate();
          broadcastToClients({
            type: 'qr_handshake_received',
            peerIp: senderIp,
            peerName: packet.senderName || senderIp
          });
          break;
        }
        case 'PEER_PROBE': {
          peers.set(senderIp, {
            ip: senderIp,
            username: packet.senderName || senderIp,
            avatarBase64: packet.avatarBase64 || null,
            lastSeen: Date.now()
          });
          sendPeersUpdate();
          sendDirectHeartbeat(senderIp);
          break;
        }
        case 'AVATAR_UPDATE': {
          const peer = peers.get(senderIp) || { ip: senderIp, username: packet.senderName || senderIp };
          peer.avatarBase64 = packet.avatarBase64;
          peer.lastSeen = Date.now();
          peers.set(senderIp, peer);
          sendPeersUpdate();
          break;
        }
        case 'TYPING': {
          broadcastToClients({
            type: 'typing',
            senderIp,
            isTyping: !!packet.isTyping
          });
          break;
        }
        case 'CALL_INVITE': {
          broadcastToClients({
            type: 'call_incoming',
            callId: packet.callId || `${Date.now()}`,
            peerIp: senderIp,
            peerName: packet.senderName || senderIp,
            isVideo: !!packet.isVideo
          });
          break;
        }
        case 'CALL_ACCEPT': {
          broadcastToClients({
            type: 'call_accepted',
            peerIp: senderIp,
            callId: packet.callId
          });
          break;
        }
        case 'CALL_REJECT': {
          broadcastToClients({
            type: 'call_rejected',
            peerIp: senderIp
          });
          break;
        }
        case 'CALL_HANGUP': {
          broadcastToClients({
            type: 'call_ended',
            peerIp: senderIp
          });
          break;
        }
        default: {
          if (packet.textContent) {
            broadcastToClients({
              type: 'message',
              message: {
                id: `msg_${Date.now()}_${Math.random()}`,
                senderIp,
                senderName: packet.senderName || senderIp,
                type: 'TEXT',
                content: packet.textContent,
                timestamp: packet.timestamp || Date.now(),
                isMine: false
              }
            });
          }
        }
      }
    } catch (e) {
      // Ignored malformed json
    }
  }
});

function sendBroadcast(buffer) {
  const targets = new Set(['255.255.255.255']);
  if (networkInfo.preferredBroadcast) targets.add(networkInfo.preferredBroadcast);
  networkInfo.interfaces.forEach(i => {
    if (i.broadcast) targets.add(i.broadcast);
  });

  for (const bcast of targets) {
    try {
      discoverySocket.send(buffer, 0, buffer.length, DISCOVERY_PORT, bcast);
    } catch (e) {}
  }
}

function sendDirectHeartbeat(targetIp) {
  const usernameBuf = Buffer.from(myUsername, 'utf8');
  const packet = Buffer.alloc(1 + usernameBuf.length);
  packet[0] = TYPE_HEARTBEAT;
  usernameBuf.copy(packet, 1);
  try {
    discoverySocket.send(packet, 0, packet.length, DISCOVERY_PORT, targetIp);
  } catch (e) {}
}

function sendHeartbeat() {
  const usernameBuf = Buffer.from(myUsername, 'utf8');
  const packet = Buffer.alloc(1 + usernameBuf.length);
  packet[0] = TYPE_HEARTBEAT;
  usernameBuf.copy(packet, 1);
  sendBroadcast(packet);

  // Also send rich probe packet with avatar
  const richPacket = {
    type: 'PEER_PROBE',
    senderId: networkInfo.preferredIp,
    senderName: myUsername,
    senderIp: networkInfo.preferredIp,
    avatarBase64: myAvatarBase64,
    timestamp: Date.now()
  };
  const jsonBuf = Buffer.from(JSON.stringify(richPacket), 'utf8');
  const richBuf = Buffer.alloc(1 + jsonBuf.length);
  richBuf[0] = TYPE_RICH_SIGNAL;
  jsonBuf.copy(richBuf, 1);
  sendBroadcast(richBuf);
}

// Subnet scan (ping /24 addresses to rapidly detect phones)
function triggerSubnetScan() {
  const ip = networkInfo.preferredIp;
  if (!ip || ip === '127.0.0.1') return;
  const prefix = ip.substring(0, ip.lastIndexOf('.') + 1);
  const usernameBuf = Buffer.from(myUsername, 'utf8');
  const packet = Buffer.alloc(1 + usernameBuf.length);
  packet[0] = TYPE_HEARTBEAT;
  usernameBuf.copy(packet, 1);

  for (let i = 1; i < 255; i++) {
    const target = `${prefix}${i}`;
    if (target !== ip) {
      try {
        discoverySocket.send(packet, 0, packet.length, DISCOVERY_PORT, target);
      } catch (e) {}
    }
  }
}

// Send heartbeats every 3.5s
setInterval(sendHeartbeat, 3500);

try {
  discoverySocket.bind(DISCOVERY_PORT);
} catch (e) {
  console.error('Error binding discovery socket:', e.message);
}

// -------------------------------------------------------------
// 2. TCP File Transfer Server (Port 1051)
// -------------------------------------------------------------
const tcpFileServer = net.createServer((socket) => {
  const senderIp = socket.remoteAddress ? socket.remoteAddress.replace(/^.*:/, '') : 'unknown';
  let bufferAcc = Buffer.alloc(0);
  let headerParsed = false;
  let messageType = 'FILE';
  let transferId = '';
  let fileName = '';
  let fileSize = 0n;
  let fileWriteStream = null;
  let writtenBytes = 0n;
  let destFileName = '';

  socket.on('data', (chunk) => {
    if (!headerParsed) {
      bufferAcc = Buffer.concat([bufferAcc, chunk]);
      // Header: Magic(4) + Type(4) + tIdLen(2) + tId + nameLen(2) + name + size(8)
      if (bufferAcc.length >= 10) {
        const magic = bufferAcc.readInt32BE(0);
        if (magic !== MAGIC_ZNET) {
          socket.destroy();
          return;
        }
        const typeOrd = bufferAcc.readInt32BE(4);
        const types = ['TEXT', 'IMAGE', 'VIDEO', 'AUDIO', 'FILE', 'SYSTEM', 'CALL_LOG'];
        messageType = types[typeOrd] || 'FILE';

        const tIdLen = bufferAcc.readInt16BE(8);
        if (bufferAcc.length >= 10 + tIdLen + 2) {
          transferId = bufferAcc.toString('utf8', 10, 10 + tIdLen);
          const nameOffset = 10 + tIdLen;
          const nameLen = bufferAcc.readInt16BE(nameOffset);

          if (bufferAcc.length >= nameOffset + 2 + nameLen + 8) {
            fileName = bufferAcc.toString('utf8', nameOffset + 2, nameOffset + 2 + nameLen);
            const sizeOffset = nameOffset + 2 + nameLen;
            fileSize = bufferAcc.readBigInt64BE(sizeOffset);

            headerParsed = true;
            destFileName = `${Date.now()}_${fileName}`;
            const destPath = path.join(receivedDir, destFileName);
            fileWriteStream = fs.createWriteStream(destPath);

            const remainingPayload = bufferAcc.subarray(sizeOffset + 8);
            if (remainingPayload.length > 0) {
              fileWriteStream.write(remainingPayload);
              writtenBytes += BigInt(remainingPayload.length);
            }
            bufferAcc = Buffer.alloc(0);
          }
        }
      }
    } else {
      if (fileWriteStream) {
        fileWriteStream.write(chunk);
        writtenBytes += BigInt(chunk.length);
      }
    }
  });

  socket.on('end', () => {
    if (fileWriteStream) {
      fileWriteStream.end(() => {
        const peer = peers.get(senderIp);
        const senderName = peer ? peer.username : senderIp;

        broadcastToClients({
          type: 'message',
          message: {
            id: `msg_${Date.now()}_${Math.random()}`,
            senderIp,
            senderName,
            type: messageType,
            fileName,
            fileSize: Number(fileSize),
            fileUrl: `/files/${destFileName}`,
            timestamp: Date.now(),
            isMine: false
          }
        });
      });
    }
  });

  socket.on('error', (err) => {
    console.error('TCP File transfer error:', err.message);
  });
});

tcpFileServer.listen(TCP_FILE_PORT, '0.0.0.0', () => {
  console.log(`[File Server] TCP listening on 0.0.0.0:${TCP_FILE_PORT}`);
});

// -------------------------------------------------------------
// 3. UDP Audio (1052) and Video (1053) Engine
// -------------------------------------------------------------
let activeCallTargetIp = null;

const audioSocket = dgram.createSocket({ type: 'udp4', reuseAddr: true });
audioSocket.on('listening', () => {
  console.log(`[Voice Call] UDP Audio listening on 0.0.0.0:${VOICE_CALL_PORT}`);
});
audioSocket.on('message', (msg, rinfo) => {
  const senderIp = rinfo.address;
  // Relay incoming PCM voice chunk to web clients
  broadcastToClients({
    type: 'audio_stream',
    senderIp,
    audioBase64: msg.toString('base64')
  });
});

try {
  audioSocket.bind(VOICE_CALL_PORT);
} catch (e) {
  console.error('Error binding audio socket:', e.message);
}

const videoSocket = dgram.createSocket({ type: 'udp4', reuseAddr: true });
videoSocket.on('listening', () => {
  console.log(`[Video Call] UDP Video listening on 0.0.0.0:${VIDEO_CALL_PORT}`);
});
videoSocket.on('message', (msg, rinfo) => {
  const senderIp = rinfo.address;
  // Relay JPEG video frame to web clients
  broadcastToClients({
    type: 'video_stream',
    senderIp,
    frameBase64: msg.toString('base64')
  });
});

try {
  videoSocket.bind(VIDEO_CALL_PORT);
} catch (e) {
  console.error('Error binding video socket:', e.message);
}

// -------------------------------------------------------------
// 4. WebSocket Client Messages
// -------------------------------------------------------------
wss.on('connection', (ws) => {
  console.log('[WebSocket] Desktop Web Client connected');
  // Immediately send initial peers and network info
  sendPeersUpdate();
  triggerSubnetScan();

  ws.send(JSON.stringify({
    type: 'init',
    ip: networkInfo.preferredIp,
    hostname: os.hostname(),
    username: myUsername,
    avatarBase64: myAvatarBase64,
    avatarColor: myAvatarColor
  }));

  ws.on('message', (raw) => {
    try {
      const data = JSON.parse(raw);
      switch (data.type) {
        case 'update_profile': {
          if (data.username) myUsername = data.username;
          if (data.avatarBase64 !== undefined) myAvatarBase64 = data.avatarBase64;
          if (data.avatarColor) myAvatarColor = data.avatarColor;
          sendHeartbeat();
          break;
        }

        case 'scan_subnet': {
          triggerSubnetScan();
          break;
        }

        case 'send_text': {
          const { targetIp, text } = data;
          if (!targetIp || !text) return;

          // Send TYPE_TEXT (byte 1)
          const textBuf = Buffer.from(text, 'utf8');
          const packet = Buffer.alloc(1 + textBuf.length);
          packet[0] = TYPE_TEXT;
          textBuf.copy(packet, 1);

          discoverySocket.send(packet, 0, packet.length, DISCOVERY_PORT, targetIp, (err) => {
            if (!err) {
              ws.send(JSON.stringify({
                type: 'message_sent',
                targetIp,
                text,
                timestamp: Date.now()
              }));
            }
          });
          break;
        }

        case 'send_typing': {
          const { targetIp, isTyping } = data;
          const richPacket = {
            type: 'TYPING',
            senderId: networkInfo.preferredIp,
            senderName: myUsername,
            isTyping: !!isTyping,
            targetIp
          };
          const jsonBuf = Buffer.from(JSON.stringify(richPacket), 'utf8');
          const packet = Buffer.alloc(1 + jsonBuf.length);
          packet[0] = TYPE_RICH_SIGNAL;
          jsonBuf.copy(packet, 1);

          discoverySocket.send(packet, 0, packet.length, DISCOVERY_PORT, targetIp);
          break;
        }

        case 'start_call': {
          const { targetIp, isVideo } = data;
          activeCallTargetIp = targetIp;
          const callId = `${Date.now()}`;
          const richPacket = {
            type: 'CALL_INVITE',
            senderId: networkInfo.preferredIp,
            senderName: myUsername,
            callId,
            isVideo: !!isVideo
          };
          const jsonBuf = Buffer.from(JSON.stringify(richPacket), 'utf8');
          const packet = Buffer.alloc(1 + jsonBuf.length);
          packet[0] = TYPE_RICH_SIGNAL;
          jsonBuf.copy(packet, 1);

          discoverySocket.send(packet, 0, packet.length, DISCOVERY_PORT, targetIp);
          break;
        }

        case 'accept_call': {
          const { targetIp, callId } = data;
          activeCallTargetIp = targetIp;
          const richPacket = {
            type: 'CALL_ACCEPT',
            senderId: networkInfo.preferredIp,
            senderName: myUsername,
            callId
          };
          const jsonBuf = Buffer.from(JSON.stringify(richPacket), 'utf8');
          const packet = Buffer.alloc(1 + jsonBuf.length);
          packet[0] = TYPE_RICH_SIGNAL;
          jsonBuf.copy(packet, 1);

          discoverySocket.send(packet, 0, packet.length, DISCOVERY_PORT, targetIp);
          break;
        }

        case 'reject_call': {
          const { targetIp, callId } = data;
          activeCallTargetIp = null;
          const richPacket = {
            type: 'CALL_REJECT',
            senderId: networkInfo.preferredIp,
            senderName: myUsername,
            callId
          };
          const jsonBuf = Buffer.from(JSON.stringify(richPacket), 'utf8');
          const packet = Buffer.alloc(1 + jsonBuf.length);
          packet[0] = TYPE_RICH_SIGNAL;
          jsonBuf.copy(packet, 1);

          discoverySocket.send(packet, 0, packet.length, DISCOVERY_PORT, targetIp);
          break;
        }

        case 'hangup_call': {
          const { targetIp, callId } = data;
          activeCallTargetIp = null;
          const richPacket = {
            type: 'CALL_HANGUP',
            senderId: networkInfo.preferredIp,
            senderName: myUsername,
            callId
          };
          const jsonBuf = Buffer.from(JSON.stringify(richPacket), 'utf8');
          const packet = Buffer.alloc(1 + jsonBuf.length);
          packet[0] = TYPE_RICH_SIGNAL;
          jsonBuf.copy(packet, 1);

          discoverySocket.send(packet, 0, packet.length, DISCOVERY_PORT, targetIp);
          break;
        }

        // Outgoing video frame from desktop webcam -> send to phone port 1053
        case 'video_frame': {
          const { targetIp, frameBase64 } = data;
          if (targetIp && frameBase64) {
            const buf = Buffer.from(frameBase64, 'base64');
            if (buf.length < 65000) {
              videoSocket.send(buf, 0, buf.length, VIDEO_CALL_PORT, targetIp);
            }
          }
          break;
        }

        // Outgoing voice chunk from desktop microphone -> send to phone port 1052
        case 'audio_frame': {
          const { targetIp, audioBase64 } = data;
          if (targetIp && audioBase64) {
            const buf = Buffer.from(audioBase64, 'base64');
            audioSocket.send(buf, 0, buf.length, VOICE_CALL_PORT, targetIp);
          }
          break;
        }
      }
    } catch (e) {
      console.error('WS message error:', e);
    }
  });
});

// Start Server
server.listen(HTTP_PORT, () => {
  networkInfo = getLocalNetworkInfo();
  console.log(`\n======================================================`);
  console.log(`🚀 Zero Network Connectivity Desktop Web Server Running!`);
  console.log(`🌐 Local Preview:   http://localhost:${HTTP_PORT}`);
  console.log(`📡 Local LAN IP:    ${networkInfo.preferredIp}`);
  console.log(`📻 Broadcast IP:   ${networkInfo.preferredBroadcast}`);
  console.log(`======================================================\n`);
});

// ==========================================================================
// Zero Network Connectivity Desktop Web - Client Application
// ==========================================================================

let ws = null;
let myInfo = { ip: '127.0.0.1', username: 'Zero Web (Desktop)', avatarColor: '#2F80ED' };
let peers = []; // [{ ip, username, lastSeen, isOnline, avatarBase64 }]
let currentPeerIp = null;
let activeMessages = {}; // peerIp -> [ { id, senderIp, senderName, type, content, fileName, fileSize, fileUrl, timestamp, isMine } ]
let typingTimeout = null;

// Media & Call State
let activeCall = null; // { peerIp, peerName, isVideo, callId, startTime, timerInterval }
let localStream = null;
let videoFrameInterval = null;
let audioContext = null;
let audioRecordStream = null;
let mediaRecorder = null;
let audioChunks = [];
let voiceTimerInterval = null;
let voiceStartTime = 0;

// Initialize on DOM load
window.addEventListener('DOMContentLoaded', () => {
  lucide.createIcons();
  initWebSocket();
  initNavigation();
  initChatInputs();

  document.getElementById('btnMobileBack')?.addEventListener('click', () => {
    document.querySelector('.app-layout')?.classList.remove('chat-active-mobile');
  });

  initProfileView();
  initRadar();
  initQrModal();
  initCallHandlers();
});

// -------------------------------------------------------------
// 1. WebSocket Connectivity
// -------------------------------------------------------------
function initWebSocket() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${protocol}//${window.location.host}/ws`;

  ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    console.log('[WS] Connected to Desktop Gateway Server');
  };

  ws.onclose = () => {
    console.log('[WS] Disconnected, retrying in 2s...');
    setTimeout(initWebSocket, 2000);
  };

  ws.onerror = (err) => {
    console.error('[WS] Error:', err);
  };

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      handleServerEvent(data);
    } catch (e) {
      console.error('[WS] Failed to parse message:', e);
    }
  };
}

function handleServerEvent(data) {
  switch (data.type) {
    case 'init': {
      myInfo = data;
      document.getElementById('localIpLabel').textContent = data.ip;
      document.getElementById('myProfileIpDisplay').textContent = `IP: ${data.ip}`;
      document.getElementById('myProfileNameDisplay').textContent = data.username;
      document.getElementById('inputProfileName').value = data.username;
      renderMyAvatar();
      updateQrCode();
      break;
    }

    case 'peers_update': {
      peers = data.peers;
      document.getElementById('peersCountLabel').textContent = `${peers.length} Phone${peers.length === 1 ? '' : 's'}`;
      document.getElementById('radarCount').textContent = peers.length;
      renderPeersList();
      renderRadarBlips();
      break;
    }

    case 'message': {
      const msg = data.message;
      const peerIp = msg.senderIp;
      if (!activeMessages[peerIp]) activeMessages[peerIp] = [];
      activeMessages[peerIp].push(msg);

      if (currentPeerIp === peerIp) {
        appendMessageBubble(msg);
        scrollToBottom();
      } else {
        renderPeersList(); // update unread state
      }
      playNotificationSound();
      break;
    }

    case 'message_sent': {
      const msg = {
        id: `my_${Date.now()}`,
        senderIp: myInfo.ip,
        senderName: myInfo.username,
        type: 'TEXT',
        content: data.text,
        timestamp: data.timestamp,
        isMine: true
      };
      if (!activeMessages[data.targetIp]) activeMessages[data.targetIp] = [];
      activeMessages[data.targetIp].push(msg);

      if (currentPeerIp === data.targetIp) {
        appendMessageBubble(msg);
        scrollToBottom();
      }
      break;
    }

    case 'typing': {
      if (currentPeerIp === data.senderIp) {
        const ind = document.getElementById('typingIndicator');
        const peer = peers.find(p => p.ip === data.senderIp);
        document.getElementById('typingLabel').textContent = `${peer ? peer.username : 'Peer'} is typing...`;
        ind.style.display = data.isTyping ? 'flex' : 'none';
      }
      break;
    }

    case 'call_incoming': {
      showIncomingCallModal(data.peerIp, data.peerName, data.isVideo, data.callId);
      break;
    }

    case 'call_accepted': {
      if (activeCall && activeCall.peerIp === data.peerIp) {
        startActiveCallMedia();
      }
      break;
    }

    case 'call_rejected': {
      alert('Call was declined by phone.');
      endActiveCall();
      break;
    }

    case 'call_ended': {
      endActiveCall();
      break;
    }

    case 'video_frame': {
      if (activeCall && activeCall.peerIp === data.senderIp) {
        drawRemoteVideoFrame(data.frameBase64);
      }
      break;
    }

    case 'audio_stream': {
      if (activeCall && activeCall.peerIp === data.senderIp) {
        playRemoteAudioChunk(data.audioBase64);
      }
      break;
    }
  }
}

// -------------------------------------------------------------
// 2. Navigation & Views
// -------------------------------------------------------------
function initNavigation() {
  const dockBtns = document.querySelectorAll('.dock-btn');
  dockBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      dockBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');

      const tab = btn.getAttribute('data-tab');
      document.querySelectorAll('.content-view').forEach(v => v.classList.remove('active'));

      if (tab === 'chats') {
        document.getElementById('chatView').classList.add('active');
        document.getElementById('peersColumn').style.display = 'flex';
      } else if (tab === 'radar') {
        document.getElementById('radarView').classList.add('active');
        document.getElementById('peersColumn').style.display = 'none';
        renderRadarBlips();
      } else if (tab === 'profile') {
        document.getElementById('profileView').classList.add('active');
        document.getElementById('peersColumn').style.display = 'none';
      }
    });
  });

  document.getElementById('btnExploreRadar').addEventListener('click', () => {
    document.querySelector('.dock-btn[data-tab="radar"]').click();
  });

  document.getElementById('btnSubnetScan').addEventListener('click', () => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'scan_subnet' }));
      const btn = document.getElementById('btnSubnetScan');
      btn.innerHTML = '<i data-lucide="loader-2"></i> Scanning...';
      lucide.createIcons();
      setTimeout(() => {
        btn.innerHTML = '<i data-lucide="refresh-cw"></i> Scan';
        lucide.createIcons();
      }, 3000);
    }
  });

  // Filter chips
  document.querySelectorAll('.chip').forEach(chip => {
    chip.addEventListener('click', () => {
      document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      renderPeersList();
    });
  });

  document.getElementById('peerSearchInput').addEventListener('input', () => {
    renderPeersList();
  });
}

// -------------------------------------------------------------
// 3. Peers & Master List
// -------------------------------------------------------------
function renderPeersList() {
  const list = document.getElementById('peersList');
  const search = document.getElementById('peerSearchInput').value.toLowerCase();
  const activeChip = document.querySelector('.chip.active').getAttribute('data-filter');

  let filtered = peers.filter(p => {
    const matchSearch = p.username.toLowerCase().includes(search) || p.ip.includes(search);
    if (!matchSearch) return false;
    if (activeChip === 'online') return p.isOnline;
    return true;
  });

  if (filtered.length === 0) {
    list.innerHTML = `
      <div class="empty-state">
        <i data-lucide="wifi-off" class="empty-icon"></i>
        <p>No active phones discovered</p>
        <small>Open Zero Network Connectivity on your phone to connect.</small>
      </div>
    `;
    lucide.createIcons();
    return;
  }

  list.innerHTML = filtered.map(p => {
    const isActive = p.ip === currentPeerIp;
    const initial = p.username ? p.username.charAt(0).toUpperCase() : 'P';
    const avatarContent = p.avatarBase64
      ? `<img src="data:image/jpeg;base64,${p.avatarBase64}" />`
      : `<span>${initial}</span>`;

    const msgs = activeMessages[p.ip] || [];
    const lastMsg = msgs.length > 0 ? msgs[msgs.length - 1].content || msgs[msgs.length - 1].fileName || 'Media' : p.ip;

    return `
      <div class="peer-card ${isActive ? 'active' : ''}" onclick="selectPeer('${p.ip}')">
        <div class="peer-avatar">
          ${avatarContent}
          <span class="status-dot ${p.isOnline ? 'online' : ''}"></span>
        </div>
        <div class="peer-meta">
          <div class="peer-name">${escapeHtml(p.username)}</div>
          <div class="peer-sub">${escapeHtml(lastMsg)}</div>
        </div>
      </div>
    `;
  }).join('');

  lucide.createIcons();
}

window.selectPeer = function(peerIp) {
  currentPeerIp = peerIp;
  const peer = peers.find(p => p.ip === peerIp);

  document.getElementById('noChatSelected').style.display = 'none';
  document.getElementById('activeChatContainer').style.display = 'flex';

  document.getElementById('chatPeerName').textContent = peer ? peer.username : peerIp;
  const sub = document.getElementById('chatPeerSubtitle');
  if (sub) sub.textContent = 'Contact';

  const chatAvatar = document.getElementById('chatAvatar');
  if (peer && peer.avatarBase64) {
    chatAvatar.innerHTML = `<img src="data:image/jpeg;base64,${peer.avatarBase64}" />`;
  } else {
    const initials = peer && peer.username ? peer.username.split(' ').map(w => w.charAt(0).toUpperCase()).slice(0,2).join('') : 'P';
    chatAvatar.innerHTML = `<span>${initials}</span>`;
  }
  document.querySelector('.app-layout')?.classList.add('chat-active-mobile');

  renderChatHistory(peerIp);
  renderPeersList();
};

// -------------------------------------------------------------
// 4. Chat & Messaging
// -------------------------------------------------------------
function renderChatHistory(peerIp) {
  const container = document.getElementById('chatMessages');
  const msgs = activeMessages[peerIp] || [];

  container.innerHTML = '';
  msgs.forEach(msg => appendMessageBubble(msg));
  scrollToBottom();
}

function appendMessageBubble(msg) {
  const container = document.getElementById('chatMessages');
  const row = document.createElement('div');
  row.className = `bubble-row ${msg.isMine ? 'mine' : 'theirs'}`;

  const timeStr = new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  if (msg.type === 'CALL_LOG' || (msg.content && msg.content.toLowerCase().includes('missed call'))) {
    row.innerHTML = `
      <div class="bubble-missed-call">
        <div class="missed-call-top">
          <div class="missed-call-icon-box">
            <i data-lucide="phone-missed" style="width:18px;height:18px;"></i>
          </div>
          <div class="missed-call-title">Missed call</div>
        </div>
        <div class="missed-call-time">${timeStr}</div>
      </div>
    `;
    container.appendChild(row);
    lucide.createIcons();
    return;
  }

  let innerContent = '';
  if (msg.type === 'IMAGE' && msg.fileUrl) {
    innerContent = `
      <img src="${msg.fileUrl}" class="bubble-img" onclick="openLightbox('${msg.fileUrl}')" />
      <div class="bubble-time" style="text-align:right;margin-top:4px;">${msg.isMine ? timeStr + ' · Read' : timeStr}</div>
    `;
  } else if (msg.type === 'VIDEO' && msg.fileUrl) {
    innerContent = `
      <video src="${msg.fileUrl}" controls class="bubble-video"></video>
      <div class="bubble-time" style="text-align:right;margin-top:4px;">${msg.isMine ? timeStr + ' · Read' : timeStr}</div>
    `;
  } else if (msg.type === 'AUDIO' && msg.fileUrl) {
    const bars = [8, 12, 16, 10, 14, 18, 12, 16, 20, 14, 18, 12, 16, 22, 14, 18, 12, 16, 10, 14, 18, 12, 16, 8]
      .map(h => `<span class="waveform-bar" style="height:${h}px"></span>`).join('');

    innerContent = `
      <div class="voice-bubble-content">
        <div class="voice-bubble-top">
          <button class="btn-voice-play" onclick="playVoiceAudio('${msg.fileUrl}', this)">
            <i data-lucide="play" style="width:18px;height:18px;"></i>
          </button>
          <div class="voice-waveform-static">${bars}</div>
        </div>
        <div class="voice-bubble-timers">
          <span>0:00</span>
          <span>0:12</span>
        </div>
        <div class="bubble-time" style="padding-left:4px;">${timeStr}</div>
      </div>
    `;
  } else if (msg.type === 'FILE' && msg.fileUrl) {
    innerContent = `
      <a href="${msg.fileUrl}" download="${msg.fileName || 'file'}" class="bubble-file">
        <div class="file-icon-box"><i data-lucide="file" style="color:white;width:18px;height:18px;"></i></div>
        <div>
          <div style="font-weight:600;">${escapeHtml(msg.fileName || 'File')}</div>
          <div style="font-size:11px;color:#64748B;">${formatBytes(msg.fileSize || 0)}</div>
        </div>
      </a>
      <div class="bubble-time" style="text-align:right;margin-top:4px;">${msg.isMine ? timeStr + ' · Read' : timeStr}</div>
    `;
  } else {
    innerContent = `
      <div>${escapeHtml(msg.content)}</div>
      <div class="bubble-time" style="text-align:right;margin-top:4px;">${msg.isMine ? timeStr + ' · Read' : timeStr}</div>
    `;
  }

  row.innerHTML = `<div class="bubble">${innerContent}</div>`;
  container.appendChild(row);
  lucide.createIcons();
}

window.playVoiceAudio = function(url, btn) {
  if (!url) return;
  const audio = new Audio(url);
  audio.play();
  btn.innerHTML = '<i data-lucide="pause" style="width:18px;height:18px;"></i>';
  lucide.createIcons();
  audio.onended = () => {
    btn.innerHTML = '<i data-lucide="play" style="width:18px;height:18px;"></i>';
    lucide.createIcons();
  };
};

function scrollToBottom() {
  const container = document.getElementById('chatMessages');
  container.scrollTop = container.scrollHeight;
}

function initChatInputs() {
  const input = document.getElementById('chatInputText');
  const btnSend = document.getElementById('btnSendMessage');
  const btnAttach = document.getElementById('btnAttach');
  const fileInput = document.getElementById('hiddenFileInput');

  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  });

  input.addEventListener('input', () => {
    const val = input.value.trim();
    const btnSend = document.getElementById('btnSendMessage');
    const btnMic = document.getElementById('btnVoiceRecord');
    if (val.length > 0) {
      btnSend.style.display = 'flex';
      btnMic.style.display = 'none';
    } else {
      btnSend.style.display = 'none';
      btnMic.style.display = 'flex';
    }
    if (!currentPeerIp) return;
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'send_typing', targetIp: currentPeerIp, isTyping: true }));
      clearTimeout(typingTimeout);
      typingTimeout = setTimeout(() => {
        ws.send(JSON.stringify({ type: 'send_typing', targetIp: currentPeerIp, isTyping: false }));
      }, 1500);
    }
  });

  btnSend.addEventListener('click', sendMessage);

  btnAttach.addEventListener('click', () => {
    fileInput.click();
  });

  fileInput.addEventListener('change', () => {
    const file = fileInput.files[0];
    if (!file || !currentPeerIp) return;
    uploadAndSendFile(file);
    fileInput.value = '';
  });

  // Voice recording
  initVoiceRecorder();
}

function sendMessage() {
  const input = document.getElementById('chatInputText');
  const text = input.value.trim();
  if (!text || !currentPeerIp) return;

  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({
      type: 'send_text',
      targetIp: currentPeerIp,
      text: text
    }));
    input.value = '';
  }
}

async function uploadAndSendFile(file) {
  if (!currentPeerIp) return;

  let messageTypeOrdinal = 4; // FILE
  let typeStr = 'FILE';
  if (file.type.startsWith('image/')) { messageTypeOrdinal = 1; typeStr = 'IMAGE'; }
  else if (file.type.startsWith('video/')) { messageTypeOrdinal = 2; typeStr = 'VIDEO'; }
  else if (file.type.startsWith('audio/')) { messageTypeOrdinal = 3; typeStr = 'AUDIO'; }

  const formData = new FormData();
  formData.append('file', file);
  formData.append('targetIp', currentPeerIp);
  formData.append('messageType', messageTypeOrdinal);

  try {
    const res = await fetch('/api/upload', {
      method: 'POST',
      body: formData
    });
    const result = await res.json();
    if (result.success) {
      const msg = {
        id: `my_${Date.now()}`,
        senderIp: myInfo.ip,
        senderName: myInfo.username,
        type: typeStr,
        fileName: result.fileName,
        fileUrl: result.fileUrl,
        fileSize: result.fileSize,
        timestamp: Date.now(),
        isMine: true
      };
      if (!activeMessages[currentPeerIp]) activeMessages[currentPeerIp] = [];
      activeMessages[currentPeerIp].push(msg);
      appendMessageBubble(msg);
      scrollToBottom();
    } else {
      alert(`Transfer failed: ${result.error}`);
    }
  } catch (err) {
    console.error('File upload error:', err);
    alert('File transfer failed.');
  }
}

// -------------------------------------------------------------
// 5. Telegram-style Voice Recorder
// -------------------------------------------------------------
function initVoiceRecorder() {
  const btn = document.getElementById('btnVoiceRecord');
  const banner = document.getElementById('voiceRecordingBanner');
  const timer = document.getElementById('recordingTimer');
  const btnCancel = document.getElementById('btnCancelVoice');
  const btnSend = document.getElementById('btnSendVoice');

  let isRecording = false;

  btn.addEventListener('click', async () => {
    if (!currentPeerIp) {
      alert('Select a peer first.');
      return;
    }

    if (!isRecording) {
      try {
        audioRecordStream = await navigator.mediaDevices.getUserMedia({ audio: true });
        mediaRecorder = new MediaRecorder(audioRecordStream);
        audioChunks = [];

        mediaRecorder.ondataavailable = (e) => {
          if (e.data.size > 0) audioChunks.push(e.data);
        };

        mediaRecorder.onstop = () => {
          clearInterval(voiceTimerInterval);
          banner.style.display = 'none';
          btn.classList.remove('recording');
          isRecording = false;
        };

        mediaRecorder.start();
        isRecording = true;
        btn.classList.add('recording');
        banner.style.display = 'flex';
        voiceStartTime = Date.now();

        voiceTimerInterval = setInterval(() => {
          const sec = Math.floor((Date.now() - voiceStartTime) / 1000);
          const m = String(Math.floor(sec / 60)).padStart(2, '0');
          const s = String(sec % 60).padStart(2, '0');
          timer.textContent = `${m}:${s}`;
        }, 500);

      } catch (err) {
        console.error('Microphone error:', err);
        alert('Could not access microphone: ' + err.message);
      }
    }
  });

  btnCancel.addEventListener('click', () => {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      audioChunks = [];
      mediaRecorder.stop();
      if (audioRecordStream) audioRecordStream.getTracks().forEach(t => t.stop());
    }
  });

  btnSend.addEventListener('click', () => {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop();
      if (audioRecordStream) audioRecordStream.getTracks().forEach(t => t.stop());

      setTimeout(() => {
        if (audioChunks.length > 0) {
          const audioBlob = new Blob(audioChunks, { type: 'audio/mp3' });
          const audioFile = new File([audioBlob], `voice_note_${Date.now()}.mp3`, { type: 'audio/mp3' });
          uploadAndSendFile(audioFile);
        }
      }, 200);
    }
  });
}

// -------------------------------------------------------------
// 6. Sonar Radar View
// -------------------------------------------------------------
function initRadar() {
  renderRadarBlips();
}

function renderRadarBlips() {
  const container = document.getElementById('blipsContainer');
  const cardsGrid = document.getElementById('radarCardsGrid');
  if (!container || !cardsGrid) return;

  container.innerHTML = '';
  cardsGrid.innerHTML = '';

  const radarSize = 440;
  const radiusCenter = radarSize / 2;

  peers.forEach((peer, index) => {
    // Generate deterministic polar angle from IP
    const hash = peer.ip.split('.').reduce((acc, part) => acc + parseInt(part, 10), 0);
    const angle = (hash * 47) % 360;
    const rad = (angle * Math.PI) / 180;
    // Radius between 60px and 180px
    const distance = 70 + ((index * 43) % 110);

    const x = radiusCenter + distance * Math.cos(rad);
    const y = radiusCenter + distance * Math.sin(rad);

    // Blip on radar screen
    const blip = document.createElement('div');
    blip.className = 'radar-blip';
    blip.style.left = `${x}px`;
    blip.style.top = `${y}px`;
    blip.innerHTML = `
      <div class="blip-dot"></div>
      <div class="blip-label">${escapeHtml(peer.username)}</div>
    `;
    blip.addEventListener('click', () => {
      document.querySelector('.dock-btn[data-tab="chats"]').click();
      selectPeer(peer.ip);
    });
    container.appendChild(blip);

    // Card in summary grid
    const card = document.createElement('div');
    card.className = 'radar-peer-card';
    card.innerHTML = `
      <div class="radar-card-top">
        <div class="peer-avatar" style="width:36px;height:36px;">
          <span>${peer.username.charAt(0).toUpperCase()}</span>
          <span class="status-dot ${peer.isOnline ? 'online' : ''}"></span>
        </div>
        <div>
          <div style="font-weight:600;font-size:14px;">${escapeHtml(peer.username)}</div>
          <div style="font-size:11px;color:var(--text-muted);">${peer.ip}</div>
        </div>
      </div>
      <div class="radar-card-actions">
        <button class="btn-primary" style="flex:1;padding:6px 10px;font-size:12px;" onclick="selectPeerAndOpen('${peer.ip}')">
          <i data-lucide="message-square" style="width:14px;height:14px;"></i> Chat
        </button>
        <button class="btn-ghost" style="padding:6px 10px;font-size:12px;" onclick="startCallWithPeer('${peer.ip}', false)">
          <i data-lucide="phone" style="width:14px;height:14px;"></i>
        </button>
        <button class="btn-ghost" style="padding:6px 10px;font-size:12px;" onclick="startCallWithPeer('${peer.ip}', true)">
          <i data-lucide="video" style="width:14px;height:14px;"></i>
        </button>
      </div>
    `;
    cardsGrid.appendChild(card);
  });

  lucide.createIcons();
}

window.selectPeerAndOpen = function(peerIp) {
  document.querySelector('.dock-btn[data-tab="chats"]').click();
  selectPeer(peerIp);
};

window.startCallWithPeer = function(peerIp, isVideo) {
  selectPeer(peerIp);
  initiateCall(isVideo);
};

// -------------------------------------------------------------
// 7. Voice and Video Calls (Ports 1052 & 1053)
// -------------------------------------------------------------
function initCallHandlers() {
  document.getElementById('btnStartAudioCall').addEventListener('click', () => initiateCall(false));
  document.getElementById('btnStartVideoCall').addEventListener('click', () => initiateCall(true));

  document.getElementById('btnAcceptCall').addEventListener('click', acceptIncomingCall);
  document.getElementById('btnDeclineCall').addEventListener('click', declineIncomingCall);
  document.getElementById('btnHangupCall').addEventListener('click', endActiveCall);

  document.getElementById('btnToggleMic').addEventListener('click', toggleCallMic);
  document.getElementById('btnToggleCam').addEventListener('click', toggleCallCam);
}

function initiateCall(isVideo) {
  if (!currentPeerIp) return;
  const peer = peers.find(p => p.ip === currentPeerIp);

  activeCall = {
    peerIp: currentPeerIp,
    peerName: peer ? peer.username : currentPeerIp,
    isVideo: isVideo,
    callId: `${Date.now()}`,
    startTime: Date.now()
  };

  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({
      type: 'start_call',
      targetIp: currentPeerIp,
      isVideo: isVideo
    }));
  }

  openActiveCallOverlay();
}

function showIncomingCallModal(peerIp, peerName, isVideo, callId) {
  activeCall = { peerIp, peerName, isVideo, callId, startTime: Date.now() };

  document.getElementById('callCallerName').textContent = peerName;
  document.getElementById('callCallerIp').textContent = peerIp;
  document.getElementById('callTypeBadge').textContent = isVideo ? 'Video Call' : 'Voice Call';
  document.getElementById('incomingCallModal').style.display = 'flex';

  playRingtone();
}

function acceptIncomingCall() {
  document.getElementById('incomingCallModal').style.display = 'none';
  stopRingtone();

  if (activeCall && ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({
      type: 'accept_call',
      targetIp: activeCall.peerIp,
      callId: activeCall.callId
    }));
    openActiveCallOverlay();
    startActiveCallMedia();
  }
}

function declineIncomingCall() {
  document.getElementById('incomingCallModal').style.display = 'none';
  stopRingtone();

  if (activeCall && ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({
      type: 'reject_call',
      targetIp: activeCall.peerIp,
      callId: activeCall.callId
    }));
  }
  activeCall = null;
}

function openActiveCallOverlay() {
  document.getElementById('activeCallOverlay').style.display = 'flex';
  document.getElementById('activeCallPeerName').textContent = `In Call with ${activeCall.peerName}`;

  const isVideo = activeCall.isVideo;
  document.getElementById('remoteVideoCanvas').style.display = isVideo ? 'block' : 'none';
  document.getElementById('localPipWrap').style.display = isVideo ? 'block' : 'none';
  document.getElementById('voiceCallDisplay').style.display = isVideo ? 'none' : 'flex';

  // Start timer
  const timerElem = document.getElementById('activeCallTimer');
  activeCall.startTime = Date.now();
  activeCall.timerInterval = setInterval(() => {
    const sec = Math.floor((Date.now() - activeCall.startTime) / 1000);
    const m = String(Math.floor(sec / 60)).padStart(2, '0');
    const s = String(sec % 60).padStart(2, '0');
    timerElem.textContent = `${m}:${s}`;
  }, 1000);
}

async function startActiveCallMedia() {
  try {
    const constraints = {
      audio: true,
      video: activeCall.isVideo ? { width: 320, height: 240, frameRate: 15 } : false
    };
    localStream = await navigator.mediaDevices.getUserMedia(constraints);

    if (activeCall.isVideo) {
      const pipVideo = document.getElementById('localWebcamVideo');
      pipVideo.srcObject = localStream;

      // Stream webcam frames to server -> UDP port 1053
      const canvas = document.createElement('canvas');
      canvas.width = 320;
      canvas.height = 240;
      const ctx = canvas.getContext('2d');

      videoFrameInterval = setInterval(() => {
        if (!activeCall || !activeCall.isVideo) return;
        ctx.drawImage(pipVideo, 0, 0, 320, 240);
        const dataUrl = canvas.toDataURL('image/jpeg', 0.5);
        const base64 = dataUrl.split(',')[1];
        if (ws && ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({
            type: 'video_frame',
            targetIp: activeCall.peerIp,
            frameBase64: base64
          }));
        }
      }, 120); // ~8-10 fps over LAN
    }
  } catch (err) {
    console.error('Call media error:', err);
  }
}

function drawRemoteVideoFrame(frameBase64) {
  const canvas = document.getElementById('remoteVideoCanvas');
  const ctx = canvas.getContext('2d');
  const img = new Image();
  img.onload = () => {
    canvas.width = img.width;
    canvas.height = img.height;
    ctx.drawImage(img, 0, 0);
  };
  img.src = `data:image/jpeg;base64,${frameBase64}`;
}

function playRemoteAudioChunk(pcmBase64) {
  // Can decode and play or rely on Web Audio API
}

function toggleCallMic() {
  if (localStream) {
    const audioTrack = localStream.getAudioTracks()[0];
    if (audioTrack) {
      audioTrack.enabled = !audioTrack.enabled;
      document.getElementById('btnToggleMic').classList.toggle('active', !audioTrack.enabled);
    }
  }
}

function toggleCallCam() {
  if (localStream) {
    const videoTrack = localStream.getVideoTracks()[0];
    if (videoTrack) {
      videoTrack.enabled = !videoTrack.enabled;
      document.getElementById('btnToggleCam').classList.toggle('active', !videoTrack.enabled);
    }
  }
}

function endActiveCall() {
  if (activeCall && ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({
      type: 'hangup_call',
      targetIp: activeCall.peerIp,
      callId: activeCall.callId
    }));
  }

  if (activeCall && activeCall.timerInterval) clearInterval(activeCall.timerInterval);
  if (videoFrameInterval) clearInterval(videoFrameInterval);
  if (localStream) {
    localStream.getTracks().forEach(t => t.stop());
    localStream = null;
  }

  document.getElementById('activeCallOverlay').style.display = 'none';
  document.getElementById('incomingCallModal').style.display = 'none';
  stopRingtone();
  activeCall = null;
}

// -------------------------------------------------------------
// 8. QR Connect Modal
// -------------------------------------------------------------
function initQrModal() {
  const btnOpen = document.getElementById('btnQrModal');
  const modal = document.getElementById('qrModal');
  const btnClose = document.getElementById('btnCloseQrModal');
  const tabShow = document.getElementById('tabShowQr');
  const tabScan = document.getElementById('tabScanQr');
  const panelShow = document.getElementById('panelShowQr');
  const panelScan = document.getElementById('panelScanQr');

  let qrScannerStream = null;
  let qrScanInterval = null;

  btnOpen.addEventListener('click', () => {
    modal.style.display = 'flex';
    updateQrCode();
  });

  btnClose.addEventListener('click', () => {
    modal.style.display = 'none';
    stopQrCamera();
  });

  tabShow.addEventListener('click', () => {
    tabShow.classList.add('active');
    tabScan.classList.remove('active');
    panelShow.style.display = 'block';
    panelScan.style.display = 'none';
    stopQrCamera();
  });

  tabScan.addEventListener('click', async () => {
    tabScan.classList.add('active');
    tabShow.classList.remove('active');
    panelShow.style.display = 'none';
    panelScan.style.display = 'block';
    startQrCamera();
  });

  async function startQrCamera() {
    try {
      const video = document.getElementById('webcamScanVideo');
      const canvas = document.getElementById('webcamScanCanvas');
      const ctx = canvas.getContext('2d');
      const status = document.getElementById('qrScanStatus');

      qrScannerStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' } });
      video.srcObject = qrScannerStream;

      qrScanInterval = setInterval(() => {
        if (video.readyState === video.HAVE_ENOUGH_DATA) {
          canvas.width = video.videoWidth;
          canvas.height = video.videoHeight;
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
          const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
          const code = jsQR(imageData.data, imageData.width, imageData.height);

          if (code) {
            status.textContent = `Found QR: ${code.data}`;
            try {
              const parsed = JSON.parse(code.data);
              if (parsed.ip) {
                stopQrCamera();
                modal.style.display = 'none';
                document.querySelector('.dock-btn[data-tab="chats"]').click();
                selectPeer(parsed.ip);
              }
            } catch (e) {
              // Not JSON, check if direct IP
              if (code.data.includes('.')) {
                stopQrCamera();
                modal.style.display = 'none';
                selectPeer(code.data);
              }
            }
          }
        }
      }, 300);
    } catch (err) {
      document.getElementById('qrScanStatus').textContent = 'Camera permission denied.';
    }
  }

  function stopQrCamera() {
    if (qrScanInterval) clearInterval(qrScanInterval);
    if (qrScannerStream) {
      qrScannerStream.getTracks().forEach(t => t.stop());
      qrScannerStream = null;
    }
  }
}

function updateQrCode() {
  const container = document.getElementById('qrcodeCanvas');
  if (!container) return;
  container.innerHTML = '';

  const payload = JSON.stringify({
    ip: myInfo.ip,
    name: myInfo.username,
    port: 1050,
    tcpPort: 1051,
    id: myInfo.ip
  });

  new QRCode(container, {
    text: payload,
    width: 200,
    height: 200,
    colorDark: "#0B111D",
    colorLight: "#FFFFFF",
    correctLevel: QRCode.CorrectLevel.M
  });

  document.getElementById('qrPayloadText').textContent = `${myInfo.username} (${myInfo.ip})`;
}

// -------------------------------------------------------------
// 9. Profile Settings
// -------------------------------------------------------------
function initProfileView() {
  document.getElementById('btnSaveProfile').addEventListener('click', () => {
    const name = document.getElementById('inputProfileName').value.trim();
    if (!name) return;

    myInfo.username = name;
    document.getElementById('myProfileNameDisplay').textContent = name;

    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({
        type: 'update_profile',
        username: name,
        avatarColor: myInfo.avatarColor
      }));
    }

    renderMyAvatar();
    updateQrCode();
    alert('Profile saved and announced to local Wi-Fi!');
  });

  document.querySelectorAll('.color-dot').forEach(dot => {
    dot.addEventListener('click', () => {
      document.querySelectorAll('.color-dot').forEach(d => d.classList.remove('active'));
      dot.classList.add('active');
      myInfo.avatarColor = dot.getAttribute('data-color');
      renderMyAvatar();
    });
  });
}

function renderMyAvatar() {
  const display = document.getElementById('myAvatarDisplay');
  if (display) {
    display.style.background = myInfo.avatarColor || '#2F80ED';
    display.innerHTML = `<span>${myInfo.username.charAt(0).toUpperCase()}</span>`;
  }
}

// -------------------------------------------------------------
// 10. Utilities & Sound
// -------------------------------------------------------------
window.openLightbox = function(src) {
  const lightbox = document.getElementById('imageLightbox');
  const img = document.getElementById('lightboxImg');
  img.src = src;
  lightbox.style.display = 'flex';
};

document.getElementById('btnCloseLightbox').addEventListener('click', () => {
  document.getElementById('imageLightbox').style.display = 'none';
});

let ringtoneAudio = null;
function playRingtone() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(440, ctx.currentTime);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start();
    ringtoneAudio = { ctx, osc };
  } catch (e) {}
}

function stopRingtone() {
  if (ringtoneAudio) {
    try {
      ringtoneAudio.osc.stop();
      ringtoneAudio.ctx.close();
    } catch (e) {}
    ringtoneAudio = null;
  }
}

function playNotificationSound() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.frequency.setValueAtTime(880, ctx.currentTime);
    gain.gain.setValueAtTime(0.1, ctx.currentTime);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + 0.15);
  } catch (e) {}
}

function escapeHtml(text) {
  if (!text) return '';
  return text.replace(/[&<>"']/g, m => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[m]));
}

function formatBytes(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

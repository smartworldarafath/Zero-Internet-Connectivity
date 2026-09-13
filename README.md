<div align="center">

  <img src="app/src/main/res/drawable/app_logo.png" alt="Zero Network Connectivity Logo" width="120" height="120" style="border-radius: 28px; box-shadow: 0 8px 24px rgba(0,0,0,0.2);" />

  # Zero Network Connectivity 📡
  
  **A 100% Offline, Peer-to-Peer (P2P) LAN & Hotspot Messenger, High-Speed File Transfer, Real-Time Voice & Video Calls for Android.**

  [![Android API](https://img.shields.io/badge/Android-API%2026%20--%2036-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
  [![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
  [![Latest Release](https://img.shields.io/github/v/release/smartworldarafath/Zero-Internet-Connectivity?color=00B4D8&style=for-the-badge&logo=github)](https://github.com/smartworldarafath/Zero-Internet-Connectivity/releases/latest)
  [![License](https://img.shields.io/badge/License-MIT-F77F00?style=for-the-badge)](LICENSE)
  [![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20%7C%20Zero%20Cloud-2A9D8F?style=for-the-badge)](#-security--privacy-first)

  <br />

  <p align="center">
    <a href="https://github.com/smartworldarafath/Zero-Internet-Connectivity/releases/latest"><strong>📥 Download Latest APK (v1.0.1)</strong></a> •
    <a href="#-key-features"><strong>Features</strong></a> •
    <a href="#-protocol-specification"><strong>Protocol Specs</strong></a> •
    <a href="#-architecture--tech-stack"><strong>Tech Stack</strong></a> •
    <a href="#-getting-started"><strong>Build Guide</strong></a>
  </p>
</div>

---

## 🌟 What is Zero Network Connectivity?

**Zero Network Connectivity** is an advanced, decentralized communication suite built natively for Android. It enables seamless communication and data transfer between Android devices without requiring:
- ❌ No Internet connection
- ❌ No Mobile data / SIM card
- ❌ No Cloud servers or third-party accounts
- ❌ No Tracking, telemetry, or metadata logging

Whether you are in remote locations, during emergency blackouts, on flights, inside campus LAN networks, or simply sharing huge files at maximum Wi-Fi hardware speeds, **Zero Network Connectivity** connects you directly device-to-device.

---

## 🚀 Key Features

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          ZERO NETWORK CONNECTIVITY v1.0.1                        │
├───────────────────────┬─────────────────────────┬────────────────────────────────┤
│   📡 ZERO-CONFIG P2P  │    💬 RICH MESSAGING    │    📞 ULTRA-LOW LATENCY CALLS  │
│ • Subnet Broadcast    │ • Instant Text Delivery │ • Real-Time PCM Voice Calls    │
│ • Fast IP Ping Sweep  │ • Telegram Voice Notes  │ • CameraX Live Video Streaming │
│ • 1-Tap QR Connect    │ • Auto-Save to Gallery  │ • Echo & Noise Cancellation    │
│ • Radar Pulse Radar   │ • Group LAN Chats       │ • Speaker/Mute/Cam Flipping    │
└───────────────────────┴─────────────────────────┴────────────────────────────────┘
```

### 📡 1. Zero-Config Device Discovery
- **Subnet-Wide Broadcasting:** Automatically computes active Wi-Fi interface broadcast addresses (e.g. `192.168.1.255`, `192.168.43.255`).
- **Parallel Subnet Ping Sweep:** Rapidly probes `1..254` on Port `1050` to instantly locate peers even across restrictive access points.
- **Connect with QR:** Generate peer credentials as an offline QR code and scan via built-in CameraX analyzer for instant pairing.
- **Radar Pulse Visualizer:** Fluid animated radar display tracking nearby active peers.

### 💬 2. Modern Telegram & Signal-Inspired Messaging
- **Hold-to-Record Voice Notes:** Telegram-style hold-to-record mic with live waveform audio visualizer, elapsed timer, slide-left-to-cancel, and release-to-send.
- **High-Speed Media Sharing:** Send HD Photos, 4K Videos, Audio tracks, and Documents at raw Wi-Fi throughput.
- **Automatic Gallery Indexing:** Incoming photos and videos are automatically written to `Pictures/ZeroNetwork` and `Movies/ZeroNetwork` with immediate MediaStore gallery indexing.
- **Offline LAN Groups:** Create local multi-party groups for decentralized broadcast messaging.

### 📞 3. Real-Time Offline Voice & Video Calls
- **PCM Audio Streaming:** Sub-50ms latency voice streaming over UDP on Port `1052` with hardware `AcousticEchoCanceler` and `NoiseSuppressor`.
- **Live Video Streaming:** High-framerate CameraX capture compressed via adaptive YUV-to-JPEG and transmitted over UDP on Port `1053`.
- **Full Call Controls:** Toggle microphone mute, switch to speakerphone, flip cameras, or disable video feeds on the fly.

### ⚙️ 4. Diagnostics & Deep Customization
- **Full-Width Sliding Drawer:** Clean 3-line hamburger menu that slides away upon tab selection to grant 100% screen space.
- **Network Diagnostics:** Real Hardware MAC address, Wi-Fi link speed in Mbps, frequency band (2.4 / 5 / 6 GHz), gateway, open ports, and live LAN ping latency tester.
- **Chat Customization:** Adjustable bubble corner radius slider (4dp - 28dp), preset styles (Telegram, Signal, Modern, Minimal), dark wallpapers, and font size scaling.
- **Profile Picture Manager:** Add, change, or remove avatars using Camera capture or Gallery picker.
- **GitHub Updater:** Automatic check against GitHub Releases API with 2-second home screen alerts and in-app APK installer.

---

## 📡 Protocol Specification

Zero Network Connectivity uses a lightweight, multi-port protocol designed for high efficiency and low battery consumption:

| Port | Transport | Function | Description |
| :--- | :--- | :--- | :--- |
| **`1050`** | **UDP** | **Discovery & Signals** | Periodic heartbeats, subnet broadcast, ping sweep, typing events, and call signaling packets. |
| **`1051`** | **TCP** | **File & Media Transfer** | Multi-threaded binary file transmission stream with chunks, checksums, and progress tracking. |
| **`1052`** | **UDP** | **Voice Calling** | Real-time 16kHz 16-bit PCM voice streaming with echo cancellation. |
| **`1053`** | **UDP** | **Video Calling** | Compressed JPEG camera frames streamed directly peer-to-peer. |

---

## 🛡️ Security & Privacy First

```mermaid
flowchart LR
    A[📱 Device A] <=== Direct Encrypted LAN Socket ===> B[📱 Device B]
    A -.x No Cloud Servers x.- C[☁️ External Internet]
    B -.x No Cloud Servers x.- C
```

- **Zero Cloud Dependence:** No servers, no authentication gateways, and no centralized databases.
- **Local Storage Only:** All messages, conversations, and avatars are stored in an encrypted local Room SQLite database on your device.
- **Air-Gapped Ready:** Works flawlessly in airplane mode with Wi-Fi / Hotspot turned on.

---

## 🛠️ Architecture & Tech Stack

```
com.zeronetwork.connectivity/
├── data/
│   ├── local/          # Room DB (AppDatabase, MessageDao, ConversationDao, PeerDao)
│   ├── model/          # Data Models (ChatMessage, Peer, CallSession, NetworkStats)
│   └── repository/     # ChatRepository, PeerRepository
├── network/            # P2P Engine (LanDiscovery, LanFileTransfer, LanCallEngine, NetworkMonitor)
├── ui/
│   ├── components/     # SpringBubble, RadarPulse, VoiceRecorder, QrDialog, BottomDock
│   ├── screens/        # ChatsTab, FindOthersScreen, CallScreens, SettingsScreen, ProfileScreen
│   ├── theme/          # Material 3 Color Schemes, Typography, Shapes
│   └── viewmodel/      # MainViewModel, ChatViewModel, CallViewModel, SettingsViewModel
└── MainActivity.kt     # Root Activity with Edge-to-Edge and Navigation Controller
```

- **Language:** Kotlin 2.0.21
- **UI Framework:** Jetpack Compose (Material 3)
- **Local Database:** Room 2.6.1 + KSP
- **Camera & Barcodes:** CameraX 1.4.1 + ZXing Core 3.5.3
- **Image Pipeline:** Coil Compose 2.7.0
- **Target OS:** Android 16 (API 36) • Min SDK: Android 8.0 (API 26)

---

## 📥 Getting Started & Installation

### Option 1: Download Pre-Built Release APK
Download the latest verified APK directly from the releases page:
👉 **[Download ZeroNetworkConnectivity-v1.0.1.apk](https://github.com/smartworldarafath/Zero-Internet-Connectivity/releases/latest)**

### Option 2: Build from Source
```bash
# 1. Clone the repository
git clone https://github.com/smartworldarafath/Zero-Internet-Connectivity.git

# 2. Open directory
cd Zero-Internet-Connectivity

# 3. Build debug APK
./gradlew assembleDebug

# 4. Build optimized release APK
./gradlew assembleRelease
```

---


---

## ☕ Support / Buy Me a Coffee

If you find **Zero Internet Connectivity** helpful and want to support ongoing development, maintenance, and new features, consider buying me a coffee! Your support means the world and helps keep this project open-source.

<div align="center">

<a href="https://www.supportkori.com/arafathrahman" target="_blank">
  <img src="https://img.shields.io/badge/Support_Me-SupportKori-FF5E5B?style=for-the-badge&logo=buy-me-a-coffee&logoColor=white" alt="Support Me on SupportKori" />
</a>

<br/><br/>

<a href="https://www.supportkori.com/arafathrahman" target="_blank">
  <img src="assets/supportkori-qr.jpg" alt="SupportKori QR Code - Arafath Rahman" width="220" style="border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.15);" />
</a>

<br/><br/>

Scan the QR code above or visit:  
👉 **[https://www.supportkori.com/arafathrahman](https://www.supportkori.com/arafathrahman)**

</div>

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md) before submitting pull requests.

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

<div align="center">
  <p>Crafted with ❤️ by <strong>Md Arafath Rahman</strong></p>
  <p>
    <a href="https://github.com/smartworldarafath">GitHub</a> •
    <a href="mailto:smartworld.bd.710@gmail.com">Contact</a>
  </p>
</div>

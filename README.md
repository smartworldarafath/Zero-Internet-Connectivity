# Zero Network Connectivity 📡

<div align="center">
  <h3>Offline P2P LAN & Hotspot Messenger, Voice/Video Calls & File Transfer for Android</h3>
  <p><strong>Developed by:</strong> Md Arafath Rahman</p>
  <p><strong>Current Version:</strong> v1.0.1 (API 36 Ready)</p>
</div>

---

## 🌟 Overview

**Zero Network Connectivity** is a modern, high-performance, and completely offline communication application built natively for Android using **Jetpack Compose**, **Kotlin Coroutines**, and **Zero-Config P2P Networking**.

It operates completely without internet access, cellular data, or external servers. All discovery, messaging, real-time audio/video calls, and file transfers take place directly between devices over local Wi-Fi networks or portable Wi-Fi hotspots.

---

## 🚀 Key Features

### 📡 1. Zero-Config LAN & Hotspot Discovery
- **Subnet Broadcast & Fast Sweep:** Auto-calculates network interface subnet addresses (`192.168.1.255`, `192.168.43.255`) combined with parallel port 1050 sweeps to find any device instantly.
- **Connect with QR:** Generate offline QR codes with pairing credentials and scan using the built-in CameraX scanner for instant 1-tap connection.
- **Radar Pulse Visualizer:** Animated radar showing active peers in real-time.

### 💬 2. Modern Telegram & Signal Inspired Messaging
- **Instant LAN Messaging:** Sub-millisecond peer-to-peer delivery over UDP and TCP.
- **Hold-to-Record Voice Notes:** Telegram-style hold-to-record mic button with live audio waveform visualization, elapsed timer, drag-to-cancel, and release-to-send.
- **Full Media Sharing:** Send images, high-definition videos, audio files, and documents over LAN.
- **Auto-Save to Gallery:** Incoming photos and videos are automatically indexed and saved to the Android MediaStore gallery (`Pictures/ZeroNetwork`, `Movies/ZeroNetwork`).
- **Group Chats:** Create offline LAN groups for multiple connected peers.

### 📞 3. Real-Time Offline Voice & Video Calls
- **Ultra-Low Latency Audio:** Real-time PCM audio streaming with Acoustic Echo Cancellation (AEC) and Noise Suppression (NS).
- **Live Video Streaming:** CameraX frame capture with adaptive YUV to JPEG compression streamed directly over UDP.
- **In-Call Controls:** Mute, speakerphone toggle, camera flip (front/back), and video feed toggles.

### ⚙️ 4. Diagnostics & Deep Customization
- **Full-Width 3-Line Sliding Drawer:** Clean navigation separating Chat, Appearance, Connections, Updates, and About.
- **Network Diagnostics:** Live Hardware MAC address, Wi-Fi link speed (Mbps), frequency band (2.4/5/6 GHz), subnet mask, gateway, and LAN ping latency tester.
- **Chat Customization:** Adjustable bubble corner radius slider (4dp - 28dp), custom bubble preset styles, chat wallpapers (Classic, Doodles, Gradient), and font scale multiplier.
- **Profile Management:** Add, update, or remove profile pictures using Camera capture or Gallery picker.
- **GitHub Updates Engine:** Automatic check against GitHub Releases (`v1.0.1+`) with animated home alerts and direct in-app APK installer.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 2.0.21
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM + Clean Repository Pattern + Kotlin Flow
- **Local Persistence:** Room Database (2.6.1) + SharedPreferences
- **Camera & Barcode Scanning:** CameraX 1.4.1 + ZXing Core 3.5.3
- **Image Loading:** Coil Compose
- **Networking Protocol:**
  - **Discovery & Heartbeat:** UDP Broadcast on Port `1050`
  - **Direct Text & Commands:** UDP / Direct Datagram on Port `1050`
  - **High-Speed File Transfer:** Multi-threaded TCP Server on Port `1051`
  - **Real-Time Voice Streaming:** UDP on Port `1052`
  - **Real-Time Video Streaming:** UDP on Port `1053`

---

## 📥 Getting Started & Building from Source

### Prerequisites
- Android Studio Ladybug | 2024.2.1+ or newer
- Android SDK 36
- Java Development Kit (JDK) 21

### Build Instructions
```bash
# Clone the repository
git clone https://github.com/smartworldarafath/Zero-Internet-Connectivity.git

# Navigate to project root
cd Zero-Internet-Connectivity

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">
  Crafted with ❤️ by <strong>Md Arafath Rahman</strong>
</div>

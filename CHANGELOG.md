# Changelog 📜

All notable changes to the **Zero Network Connectivity** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.1] - 2026-09-02

### Added
- **Subnet-Wide LAN Broadcast & Parallel Sweeper:** Auto-calculates active interface broadcast addresses (`192.168.1.255`, `192.168.43.255`) and executes parallel port `1050` ping sweep (`1..254`) on demand.
- **Telegram-Style Hold-To-Record Voice Notes:** Hold mic button to record, live audio waveform visualizer, elapsed timer, slide-to-cancel (`>150dp`), and release-to-send.
- **Auto-Save Photos & Videos to Gallery:** Received photos and videos are automatically written to `MediaStore.Images` and `MediaStore.Video` (`Pictures/ZeroNetwork`, `Movies/ZeroNetwork`) with `MediaScannerConnection`.
- **Connect with QR Code:** 1-tap pairing dialog featuring ZXing offline QR generation and CameraX live camera QR scanning.
- **GitHub In-App Updater:** Automatic version check against GitHub Releases API (`latest`) with 2-second animated home screen alert and direct APK downloader/installer.
- **Network Diagnostics Hub:** Real Hardware MAC address detection, Wi-Fi link speed (Mbps), frequency band (2.4/5/6 GHz), gateway, open port monitor, and LAN ping latency tester.
- **Settings 3-Line Sliding Drawer:** Full-width modal navigation drawer replacing cramped vertical tabs.
- **Deep Chat Customization:** Adjustable bubble corner radius (4dp-28dp), preset themes (Telegram, Signal, Modern, Minimal), wallpaper selector, and font scaling slider.
- **Profile Picture Management:** Add, update, or remove profile pictures via Camera capture or Gallery picker.

### Fixed
- Fixed peer discovery isolation on routers with unmetered multicast filtering.
- Fixed link speed and MAC address reporting 0 in diagnostics.
- Fixed database recreation on app updates; all conversations and preferences now persist smoothly.

---

## [1.0.0] - 2026-09-01

### Added
- Initial release of Zero Network Connectivity.
- Jetpack Compose offline messenger UI.
- UDP discovery on port 1050.
- High-speed TCP file transfer on port 1051.
- Real-time UDP voice calls on port 1052.
- Real-time UDP video calls on port 1053.
- Room database for local conversation and message storage.

# Contributing to Zero Network Connectivity 🤝

Thank you for your interest in contributing to **Zero Network Connectivity**! We welcome contributions ranging from bug fixes and documentation improvements to new offline networking features and UI enhancements.

---

## 🚀 Getting Started

### 1. Fork & Clone
```bash
git clone https://github.com/YOUR_USERNAME/Zero-Internet-Connectivity.git
cd Zero-Internet-Connectivity
```

### 2. Android Studio Setup
- Recommended IDE: **Android Studio Ladybug (2024.2.1+)** or newer.
- Ensure **JDK 21** is selected under `Settings -> Build, Execution, Deployment -> Build Tools -> Gradle`.
- Ensure **Android SDK 36** is installed.

### 3. Build & Test Locally
```bash
# Verify compilation and assemble debug APK
./gradlew assembleDebug
```

---

## 📐 Coding Guidelines

- **Architecture:** Follow the MVVM + Repository Pattern. UI components must be purely declarative with Jetpack Compose.
- **Coroutines & Flows:** Use structured concurrency (`viewModelScope`, `SupervisorJob() + Dispatchers.IO`) for background networking tasks.
- **Networking Integrity:** Never block the main thread with socket I/O. Ports `1050`, `1051`, `1052`, and `1053` should remain dedicated to their designated packet handlers.
- **Formatting:** Keep Kotlin code clean, idiomatic, and self-documenting.

---

## 🔄 Submitting a Pull Request

1. Create a feature branch (`git checkout -b feature/amazing-feature`).
2. Commit your changes with clear, descriptive commit messages (`git commit -m 'feat: Add mesh relay protocol'`).
3. Push to your branch (`git push origin feature/amazing-feature`).
4. Open a Pull Request on GitHub targeting `main`.

---

## 📜 Code of Conduct

Please review and adhere to our [Code of Conduct](CODE_OF_CONDUCT.md) in all project interactions.

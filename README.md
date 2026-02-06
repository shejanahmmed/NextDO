<h1 align="center">NextDO</h1>

<p align="center">
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License">
  </a>
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/API-24%2B-orange.svg" alt="API">
  <img src="https://img.shields.io/badge/Version-3.2.0-brightgreen.svg" alt="Version">
  <img src="https://img.shields.io/badge/Quality-10%2F10-success.svg" alt="Quality">
  <img src="https://img.shields.io/badge/Lint-Passing-success.svg" alt="Lint">
</p>

<p align="center">
  <strong>A modern, minimalist to-do app inspired by dot-matrix aesthetics.</strong>
  <br>
  Built with Java, MVVM, and Room Database.
  <br>
  <br>
  <a href="https://github.com/shejanahmmed/NextDO/releases/latest">
    <img src="https://img.shields.io/badge/Download-APK-2ea44f?style=for-the-badge&logo=android" alt="Download APK">
  </a>
</p>

---

## 📱 Overview

**NextDO** redefines task management with a unique, retro-futuristic "Nothing OS" inspired design. It strips away clutter to focus on what matters: your tasks. Featuring a high-contrast dot-matrix dashboard, fluid animations, and intuitive gesture controls, it makes productivity feel premium.

## ✨ Features

*   **🎨 Minimalist Dashboard**: A clean, grid-based interface with a dynamic task counter and greeting.
*   **🎹 Dot-Matrix Typography**: Distinctive design language that stands out from standard Material apps.
*   **👆 Gesture Controls**: 
    *   **Swipe Left**: Delete tasks with a custom animation.
    *   **Swipe Right**: Quick edit mode.
*   **🔔 Smart Reminders**: Three powerful reminder types:
    *   **📱 Notification**: Standard notification reminders
    *   **⏰ Alarm**: Full-screen alarm with `setAlarmClock` API for guaranteed wake from Doze mode
    *   **🗣️ Voice**: Spoken reminders using Text-to-Speech
        *   🎤 Voice preview with animated equalizer
        *   🗣️ Male voice preference
        *   📲 Smart TTS installation prompts
        *   🔄 Automatic fallback to notification if TTS unavailable
*   **🔁 Flexible Repeat Patterns**: Daily, Weekly, Monthly, Weekday, or Custom days with auto-rescheduling
*   **🌗 Customization**: Personalize your experience with custom accent colors and background themes (including gallery images).
*   **🏠 Widgets**: Stay updated at a glance with home screen widgets (Dark & Light variants).
*   **♻️ Recycle Bin**: Never lose a task accidentally; recover deleted items instantly (15-day retention).
*   **🔒 Production Ready**: Lint-free codebase with ProGuard enabled for optimized, secure releases.

## 📸 Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/62f953d8-ac58-4c9f-a80e-630b5a8fd8c4" width="30%" alt="Dashboard">
  <img src="https://github.com/user-attachments/assets/e65c808e-fb99-47a3-8058-b42a6ee99f73" width="30%" alt="Tasks">
  <img src="https://github.com/user-attachments/assets/b415d02a-32f2-4acd-8ad3-a15c5297f799" width="30%" alt="Settings">
</p>
<p align="center">
  <img src="https://github.com/user-attachments/assets/03c2f38c-563a-474a-a0ef-9b236ca9c647" width="30%" alt="Action">
  <img src="https://github.com/user-attachments/assets/417814a8-60d4-4ba3-b905-4a3c3f5796d2" width="30%" alt="Dark Mode">
  <img src="https://github.com/user-attachments/assets/fc7494d7-f811-4d5e-8bfa-082cdc536ede" width="30%" alt="Details">
</p>

## 🛠️ Tech Stack

This project follows modern Android development practices:

*   **Language**: [Java](https://www.java.com/) (Java 11)
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **Database**: [Room Database](https://developer.android.com/training/data-storage/room) 2.6.1 with complete migration chain (v1→v6)
*   **UI**: 
    *   XML Layouts with ViewBinding
    *   Custom `Canvas` drawing for dotted patterns
    *   `ConstraintLayout` for responsive UI
*   **Components**: 
    *   `RecyclerView` with `ListAdapter` and `DiffUtil`
    *   `ItemTouchHelper` for gestures
    *   `AlarmManager` with `setAlarmClock` for reliable scheduling
    *   `TextToSpeech` API for voice reminders
    *   `OnBackPressedDispatcher` for modern back navigation
*   **Build**: 
    *   ProGuard enabled with comprehensive rules
    *   Resource shrinking for optimized APK size
    *   Lint-free codebase (0 errors, 0 critical warnings)

## 📊 Code Quality

*   ✅ **Lint Status**: Passing (0 errors)
*   ✅ **Architecture**: Clean MVVM with Repository pattern
*   ✅ **Error Handling**: Proper logging throughout
*   ✅ **ProGuard**: Enabled with Room, Lifecycle, and Widget rules
*   ✅ **Build**: Optimized release builds with code shrinking
*   ✅ **Quality Score**: 10/10

## 🚀 Getting Started

### Prerequisites
*   Android Studio Iguana or newer
*   JDK 17
*   Min SDK: 24 (Android 7.0)
*   Target SDK: 34 (Android 14)

### Installation
1.  **Clone the repository**:
    ```bash
    git clone https://github.com/shejanahmmed/NextDO.git
    ```
2.  **Open in Android Studio**:
    Select `File > Open` and navigate to the cloned directory.
3.  **Build**:
    Let Gradle sync, then press `Run` (Shift+F10) to deploy to your emulator or device.

## 🤝 Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 📄 License

Distributed under the **GNU General Public License v3.0**. See `LICENSE` for more information.

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/shejanahmmed">Shejan Ahmmed</a>
</p>

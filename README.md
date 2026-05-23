# 🌐 Omni Web Browser (Android)

A powerful, feature-rich web browser for Android built using Kotlin, Jetpack Compose, and the Google Gemini API. Browse the web, extract content, and leverage AI capabilities—all in a modern mobile interface.

## ✨ Features

- **🔍 Web Browsing** - High-performance browsing powered by Android System WebView
- **🤖 AI-Powered Analysis** - Leverage Google Gemini API for intelligent content analysis and summarization
- **📄 Multiple View Modes**:
  - 🌍 Normal browsing mode
  - 📖 Reader mode for distraction-free reading
  - 💻 Source code viewer
  - 🔄 Markdown conversion
- **🎯 Media Detection** - Automatically detect and extract images, videos, and audio (Media Grabber)
- **🚫 Ad Blocking** - Built-in domain-based ad and tracker blocker
- **📸 Screenshot & PDF Export** - Capture web pages and export as PDF or Full Page Screenshots
- **⚡ High Performance** - Modern Jetpack Compose UI with efficient state management
- **🎨 Beautiful UI** - Modern, responsive interface with Material 3 and custom accent colors
- **🔒 Privacy-Focused** - Strict Privacy Mode, Incognito tabs, and Cookie management
- **📑 Tab Management** - Multiple tabs, recently closed tabs restoration, and tab hibernation

## 🚀 Quick Start

### Prerequisites
- Android Studio Ladybug | 2024.2.1 or newer
- JDK 17
- Android SDK 35 (API 35)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/Subbarao6338/Omni-Web.git
cd Omni-Web
```

2. **Generate Debug Keystore**
The app requires a debug keystore for signing. You can generate one using:
```bash
keytool -genkey -v -keystore app/debug.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
```

3. **Build the project**
```bash
./gradlew assembleDebug
```

4. **Run on Device/Emulator**
Open the project in Android Studio and click the "Run" button, or use:
```bash
./gradlew installDebug
```

## 🏗️ Project Structure

```
Omni-Web/
├── app/
│   ├── src/main/java/com/omniweb/app/
│   │   ├── data/           # Room Database, Entities, and DAOs
│   │   ├── ui/             # Jetpack Compose Screens and ViewModels
│   │   └── util/           # AdBlocker, Download Manager, and Utilities
│   └── src/main/res/       # Android resources (drawables, layouts, values)
├── build.gradle            # Project-level build configuration
├── settings.gradle         # Project settings
└── variables.gradle        # Version management for dependencies
```

## 🔧 Tech Stack

- **UI Framework**: Jetpack Compose (Material 3)
- **Language**: Kotlin
- **Database**: Room Persistence Library
- **Navigation**: Jetpack Navigation Compose
- **Network/Web**: Android WebView, OkHttp
- **AI**: Google Generative AI SDK
- **Media**: youtube-dl-android for video extraction support
- **Export**: Zxing for QR Codes, Android Print Framework for PDF

## 🎮 Usage

1. **Enter a URL** - Type any website URL or search query in the address bar
2. **Choose View Mode**:
   - Access Reader Mode, Source Viewer, or Markdown converter via the "Page Tools" menu
3. **Extract Media** - Use the Media Grabber icon in the bottom bar to see detected media
4. **Use AI Tools** - Tap "Summarize" in Page Tools to analyze content with Gemini AI
5. **Customization** - Change themes, accent colors, and search engines in Settings

## 🛡️ Security & Privacy

- **Incognito Mode**: Browsing data is not saved
- **Strict Privacy**: generic User-Agent and anti-fingerprinting measures
- **Ad Blocker**: Blocks known ad and tracking domains
- **Password Manager**: Securely store credentials locally using Android Keystore encryption

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is open source and available under the MIT License.

---

**Made with ❤️ by [Subbarao6338](https://github.com/Subbarao6338)**

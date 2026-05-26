# 🛠️ Project Skills & Technical Capabilities: Omni Web Browser

This document outlines the technical expertise, frameworks, and features implemented in the Omni Web Browser project.

## 🧰 Core Technical Stack

### **Android & Kotlin Development**
- **Language**: 100% Kotlin with modern syntax and coroutines for asynchronous operations.
- **UI Framework**: **Jetpack Compose** using Material 3 (M3) design principles for a modern, expressive interface.
- **Architecture**: **MVVM (Model-ViewModel-Intent)** pattern for clean separation of concerns and reactive UI state management.
- **Persistence**: **Room Database** for local storage of history, bookmarks, tabs, and settings, including custom migrations.
- **Navigation**: Type-safe navigation using **Jetpack Navigation Compose**.

### **Web & Network Technologies**
- **Engine**: **Android System WebView** integration with advanced configurations.
- **Content Manipulation**: Advanced **JavascriptInterface** bridges for bi-directional communication between web content and native Kotlin code.
- **Networking**: **OkHttp** for robust network requests and proxy handling.
- **Proxy/Backend**: Specialized URL rewriting and session management for proxied web access.

### **AI Integration**
- **LLM**: Deep integration with **Google Gemini API** (Generative AI SDK) for real-time page summarization and content analysis.

---

## 🚀 Key Feature Implementation Skills

### **Content Extraction & Processing**
- **Reader Mode**: Parsing and re-rendering web articles for distraction-free reading.
- **Media Grabber**: Real-time detection and extraction of video, audio, and image assets from active web sessions.
- **Code Utilities**: Integrated source code viewer and HTML-to-Markdown conversion.

### **Privacy & Security**
- **Ad Blocker**: Domain-based blocking with custom CSS injection and tracker prevention.
- **Password Manager**: Local-first credential storage secured with **Android Keystore** encryption.
- **Incognito Mode**: Isolated session handling to prevent data persistence.

### **Customization & Extensibility**
- **Userscripts**: Support for auto-injected JavaScript with pattern matching (Greasemonkey-style).
- **Bookmarklets**: Execution of `javascript:` URLs as interactive tools.
- **Download Manager**: Native categorized download handling with progress tracking and custom paths.

---

## 🛠️ Dev-Ops & Tooling
- **Build System**: Advanced Gradle configuration (KTS/Groovy) with externalized dependency management (`variables.gradle`).
- **CI/CD**: **GitHub Actions** workflows for automated versioning and debug APK releases with artifact retention policies.
- **Utilities**:
  - Full-page PNG screenshots.
  - PDF export via Android Print Framework.
  - QR Code generation (Zxing).
  - Terminal-style Console Viewer for debugging web logs.

---

**Omni Web Browser** represents a high-level mastery of the Android ecosystem, combining native performance with AI-driven web capabilities.

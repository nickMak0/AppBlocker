# 📱 AppBlocker Android App

## 🚀 Overview
AppBlocker is a personal-use Android app designed to help you **block distracting apps** like social media or adult content. It monitors foreground apps, overlays a blocking screen when necessary, and lets you manage the blocked list with a modern UI.

---

## ✅ Features Implemented

### 🎯 Core Functionality

| Feature | Description |
|--------|-------------|
| **1. App Monitoring** | Detects foreground apps using AccessibilityService and blocks specified ones. |
| **2. Usage Access Permission** | Prompts for and checks permission using AppOpsManager. |
| **3. App Blocking** | Launches fullscreen BlockScreenActivity to prevent access. |
| **4. Manage Blocked Apps** | RecyclerView with toggles to add/remove apps from blocklist. |
| **5. Persistent Storage** | Saves blocked apps and switch state via SharedPreferences. |
| **6. Global Toggle** | Switch on main screen to enable/disable blocking. |
| **7. Modern UI with Night Mode** | Uses MaterialComponents.DayNight theme for dark/light support. |
| **8. Clean Design** | MaterialCardView, MaterialSwitch, proper layouts with padding and spacing. |
| **9. Filters Launchable Apps** | Only shows user-installed apps with launch intent. |
| **10. Real-Time Feedback** | Toasts and UI updates confirm actions and permissions. |

---

## ⚙️ Tech Stack

- Kotlin
- AndroidX + Jetpack
- AccessibilityService
- SharedPreferences
- Material Components

---

## 🧪 Bug Fixes & Testing

- Fixed: Night theme not applying → Resolved with `DayNight.DarkActionBar`
- Fixed: MaterialSwitch cast crash → Used correct import
- Fixed: Toggle state reset on scroll → State synced with SharedPreferences
- Fixed: Apps like WhatsApp not showing → Filtered correctly by launch intent & system flag

---

## 🔮 Future Improvements

| Idea | Description |
|------|-------------|
| 🔒 App Unlock PIN | Prevent accidental unblock |
| ⏰ Scheduler | Time-based blocking (e.g., 10pm–7am) |
| 📊 Usage Stats | Monitor how often blocked apps are accessed |
| 🔍 App Search | Add search bar in blocked app manager |
| ☁️ Cloud Sync | Firebase sync for backup/restore |
| 🌐 Website Blocker | Extend to blocking adult sites via DNS/VPN |

---

## 🛠 Setting Up GitHub Repository

### From Android Studio:

1. Go to **VCS > Enable Version Control Integration…**
2. Choose **Git** and click OK.
3. Go to **Git > Commit** or press `Ctrl + K`, write a commit message and commit.
4. Then go to **Git > Push**, it will ask you to create a remote.
5. Click the link "Define remote" and choose **GitHub** (you must be signed in).
6. Enter repository name (e.g., `AppBlocker`), click **Share**.
7. Done! Project now tracked on GitHub.

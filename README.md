# 📵 AppBlocker – Stay Focused. Stay in Control.

AppBlocker is a powerful Android utility designed to help you reclaim your focus by **blocking distracting apps and adult websites**. Whether you're working, studying, or simply building digital discipline, AppBlocker is your personal productivity partner with advanced self-discipline features.

---

## 🚀 Features

### 🧠 Core Capabilities

| Feature                     | Description                                                                 |
|----------------------------|-----------------------------------------------------------------------------|
| 🔍 Real-Time App Monitoring | Tracks foreground apps via `AccessibilityService` and blocks unwanted ones. |
| 🚫 App Blocking             | Full-screen `BlockScreenActivity` prevents access to blocked apps.          |
| 🛡️ Adult Site Blocking      | Built-in VPN with DNS filtering blocks adult and unsafe websites.           |
| 🧩 Manage Blocked Apps      | RecyclerView-based list with toggles to manage blocked apps.                |
| 💾 Persistent Storage       | Saves settings and blocklists using `SharedPreferences`.                   |
| 🌓 Dark & Light UI Themes   | Material You interface with system-wide theme support.                     |
| 📊 Usage Dashboard          | Track screen time, focus time, app blocks, and streaks in real-time.       |
| 🔁 Live Feedback            | Immediate visual and toast feedback for all interactions.                  |
| 🎯 Launchable App Filter    | Lists only user-installed apps with launch intent.                         |
| 💬 Motivational Quotes      | Rotating quotes displayed on the block screen to keep you focused.         |
| ⏰ Per-App Scheduling       | Set individual time schedules for each blocked app with custom time ranges. |
| 🔒 Biometric Authentication | Secure access using fingerprint, face, or device password.                 |
| ⚡ Strict Mode              | Ultra-fast blocking with 200ms response time for maximum effectiveness.     |
| 🎨 Enhanced Block Screen    | Beautiful motivational quotes and clean UI when apps are blocked.           |
| 📅 Advanced Scheduling      | Multiple time ranges per app with weekday/weekend quick selection.          |
| 🛡️ Self-Discipline Protection | 5-minute delays and confirmations prevent impulsive disabling.           |
| ⏱️ Break Mode               | Structured 5-minute breaks with automatic re-enabling.                     |

---

## 🛡️ Self-Discipline Features

| Feature                        | Description                                                     |
|--------------------------------|-----------------------------------------------------------------|
| ⏰ 5-Minute Disable Delays     | All disable actions require 5-minute wait to prevent impulses  |
| 🔐 Confirmation Dialogs        | Multiple confirmations required before disabling any feature    |
| 📱 Biometric Security          | Fingerprint/face/password required to access settings          |
| ⚡ Strict Mode                 | Ultra-fast 200ms blocking with enhanced detection              |
| 🎨 Motivational Block Screen   | Inspiring quotes and clean design to maintain focus            |
| 📅 Flexible Time Scheduling    | Multiple time ranges per app with smart day selection          |
| ⏱️ Break Mode                  | Healthy 5-minute breaks with automatic timer and re-enabling   |
| 🚨 Emergency Override          | 5-second emergency disable for true emergencies only           |
| 🎯 Smart Schedule Fallback     | Individual app schedules with global schedule fallback         |
| 📊 Real-Time Stats             | Live tracking of daily blocking events and effectiveness       |

---
## 🏗 Tech Stack

- **Language:** Kotlin  
- **Frameworks:** AndroidX, Jetpack  
- **UI:** Material Components (Material3/You)  
- **Core APIs:** AccessibilityService, UsageStatsManager, VpnService, BiometricPrompt  
- **Storage:** SharedPreferences  
- **Security:** BiometricManager, Device Authentication  
- **Other:** ViewBinding, RecyclerView, Custom Adapters, CountDownTimer  

---

## 🧪 Bug Fixes & Improvements

| Issue                          | Status                                                     |
|--------------------------------|------------------------------------------------------------|
| 🌙 Night theme not applying    | ✅ Resolved using `Theme.MaterialComponents.DayNight`       |
| 🧩 Toggle state not persistent | ✅ Fixed with proper state management via SharedPreferences |
| ❌ App list missing launchables| ✅ Resolved using `launchIntent` and user app filters       |
| 🛠️ MaterialSwitch crash        | ✅ Fixed with correct imports and theme compatibility       |
| ⏰ Advanced Scheduler          | ✅ Implemented multiple time ranges per app with recurring patterns |
| 🌐 Website Blocker             | ✅ Extended to blocking adult sites via DNS/VPN            |
| 🔒 PIN Authentication Issues   | ✅ Replaced with modern biometric authentication system     |
| ⚡ Blocking Speed              | ✅ Enhanced with strict mode for 200ms ultra-fast blocking  |
| 🛡️ Easy Bypass Prevention     | ✅ Added 5-minute delays and confirmation dialogs           |
| 🎨 Block Screen Enhancement    | ✅ Added 160+ motivational quotes with beautiful UI design   |
| 📅 Advanced App Scheduling     | ✅ Multiple time ranges per app with enhanced dialog system  |

---

## 🧠 Future Enhancements

| Feature               | Description                                   |
|-----------------------|-----------------------------------------------|
| 📈 Advanced Analytics | Visualize daily/weekly app usage trends       |
| ☁️ Cloud Sync         | Firebase integration for backups and sync     |
| 🌐 DoH Support        | DNS-over-HTTPS for more robust web filtering  |
| 🎯 Focus Sessions     | Pomodoro-style focused work sessions          |
| 📱 Widget Support     | Home screen widgets for quick stats           |
| 🔔 Smart Notifications| Intelligent reminders and motivation          |

---

## 🔧 How to Run the Project Locally

### 🧑‍💻 Android Studio Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/AppBlocker.git
   ```
2. Open the project in **Android Studio**
3. Connect a **real Android device** (Accessibility & VPN features do not work on emulator)
4. Click **Run** or press `Shift + F10`
5. When prompted, grant:
   - 🔓 **Usage Access**
   - ♿ **Accessibility Permission**
   - 🔐 **Biometric Authentication** (optional)
   - 🌐 **VPN Permission** (for website blocking)

### 🛡️ First-Time Setup

1. **Enable Biometric Security**: Go to Settings → Toggle biometric authentication
2. **Configure Blocked Apps**: Navigate to Manage Apps → Select apps to block
3. **Set Schedules**: Use Schedule Settings for time-based blocking
4. **Enable Strict Mode**: For ultra-fast blocking (optional)
5. **Test Break Mode**: Try the 5-minute break feature

---

## 🌐 GitHub Integration (For Developers)

1. Go to **VCS > Enable Version Control Integration**
2. Choose **Git**, then click **OK**
3. Use `Ctrl + K` to commit changes
4. Use `Ctrl + Shift + K` or **Git > Push** to push changes
5. If not linked:
   - Click **Define Remote**
   - Choose **GitHub**
   - Enter repo name → Click **Share**
6. ✅ You're live!

---

## 📁 Project Structure

```
AppBlocker/
├── app/
│   ├── java/com/example/appblocker/
│   │   ├── MainActivity.kt                    # Main control interface
│   │   ├── BlockScreenActivity.kt             # App blocking screen
│   │   ├── ManageAppsActivity.kt              # App management with delays
│   │   ├── DashboardActivity.kt               # Usage statistics
│   │   ├── ScheduleSettingsActivity.kt        # Time-based scheduling
│   │   ├── BiometricAuthActivity.kt           # Biometric authentication
│   │   ├── BiometricSettingsActivity.kt       # Security settings
│   │   ├── BreakModeActivity.kt               # 5-minute break timer
│   │   ├── AppBlockerAccessibilityService.kt  # Core blocking service
│   │   └── utils/
│   │       ├── BiometricUtils.kt              # Biometric helper
│   │       ├── StatsManager.kt                # Statistics tracking
│   │       └── AppScheduleChecker.kt          # Schedule validation
│   ├── res/
│   │   ├── layout/                            # UI layouts
│   │   ├── drawable/                          # Icons and graphics
│   │   └── values/                            # Colors and strings
│   ├── AndroidManifest.xml
├── screenshots/
│   ├── block_screen.png
│   ├── dashboard_stats.png
│   ├── manage_apps.png
│   └── break_mode.png
├── README.md
```

---

## 💬 License

This project is currently for **personal use only**.  
A formal open-source license will be added prior to any public distribution.

---

## 🙌 Final Words

> **"Don't wait for motivation. Act."**

AppBlocker is built to help you reclaim your attention, block distractions, and build the discipline required to reach your goals. With advanced self-discipline features like 5-minute delays, biometric security, and structured break modes, it's designed to work with your psychology, not against it.

### 🎯 Key Principles

- **Friction by Design**: 5-minute delays prevent impulsive decisions
- **Structured Flexibility**: Break mode provides healthy temporary access
- **Security First**: Biometric authentication prevents unauthorized changes
- **Smart Scheduling**: Individual and global schedules work together
- **Real-Time Feedback**: Live stats show your progress and effectiveness

---

**Made with 💻 + discipline + psychology.**

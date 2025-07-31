# 📵 AppBlocker – Stay Focused. Stay in Control.

AppBlocker is a powerful Android utility designed to help you reclaim your focus by **blocking distracting apps and adult websites**. Whether you're working, studying, or simply building digital discipline, AppBlocker is your personal productivity partner.

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

---

## 📸 Screenshots

> *(Add your own screenshots in `/screenshots` folder)*

```markdown
![Block Screen](screenshots/block_screen.png)
![Dashboard](screenshots/dashboard_stats.png)
![Manage Apps](screenshots/manage_apps.png)
```

---

## 🏗 Tech Stack

- **Language:** Kotlin  
- **Frameworks:** AndroidX, Jetpack  
- **UI:** Material Components (Material3/You)  
- **Core APIs:** AccessibilityService, UsageStatsManager, VpnService  
- **Storage:** SharedPreferences  
- **Other:** ViewBinding, RecyclerView, Custom Adapters  

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

---

## 🧠 Future Enhancements

| Feature               | Description                                   |
|-----------------------|-----------------------------------------------|
| 🔐 PIN Lock           | Prevent bypassing or unauthorized changes     |
| 📅 Scheduler          | Time-based blocking (e.g., 10PM–7AM blocking) |
| 📈 Advanced Analytics | Visualize daily/weekly app usage trends       |
| 🔍 App Search         | Search bar in Manage Apps screen              |
| ☁️ Cloud Sync         | Firebase integration for backups and sync     |
| 🌐 DoH Support        | DNS-over-HTTPS for more robust web filtering  |

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
6. ✅ You’re live!

---

## 📁 Project Structure

```
AppBlocker/
├── app/
│   ├── java/com/example/appblocker/
│   │   ├── MainActivity.kt
│   │   ├── BlockScreenActivity.kt
│   │   ├── ManageAppsActivity.kt
│   │   └── ...
│   ├── res/
│   │   ├── layout/
│   │   └── drawable/
│   ├── AndroidManifest.xml
├── screenshots/
│   ├── block_screen.png
│   ├── dashboard_stats.png
│   └── manage_apps.png
├── README.md
```

---

## 💬 License

This project is currently for **personal use only**.  
A formal open-source license will be added prior to any public distribution.

---

## 🙌 Final Words

> **“Don’t wait for motivation. Act.”**

AppBlocker is built to help you reclaim your attention, block distractions, and build the discipline required to reach your goals.

---

**Made with 💻 + discipline.**

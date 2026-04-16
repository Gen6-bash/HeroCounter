# Hero Counter

A private, offline habit and activity tracker for Android. Count anything that matters — push-ups, dog walks, medications, glasses of water, daily Bible reading, prayers, or any repeating task — and track your progress over time.

<img width="1080" height="2400" alt="Screenshot_20260416_115502" src="https://github.com/user-attachments/assets/f48456a2-bcde-4244-b8ea-0a9824539ccb" />
<img width="1080" height="2400" alt="Screenshot_20260416_115542" src="https://github.com/user-attachments/assets/c0ad3c8f-7fdb-4e2d-bc36-9fb65b62a69b" />
<img width="1080" height="2400" alt="Screenshot_20260416_115616" src="https://github.com/user-attachments/assets/3ce2d113-c3eb-44c1-9c19-257f139061fe" />
<img width="1080" height="2400" alt="Screenshot_20260416_115655" src="https://github.com/user-attachments/assets/e2c8ab75-7bf6-4abe-8fb8-8f01b752d296" />
<img width="1080" height="2400" alt="Screenshot_20260416_115637" src="https://github.com/user-attachments/assets/e8e25e9a-be02-4a85-9333-060a61980b74" />

---

## Features

- **Simple tap-to-count interface** — large circular counter with add and subtract modes
- **Multiple counts** — track unlimited named activities simultaneously
- **Daily goals** — set daily, weekly, monthly, and yearly targets per count
- **Statistics** — view bar charts and totals broken down by day, week, month, and year
- **Projected totals** — see where you are headed based on your current pace
- **Goal celebration** — fireworks animation plays when you hit your daily goal
- **Midnight auto-reset** — counter display resets each day automatically
- **Edit history** — add or correct entries for any past date, including before the app was installed
- **Reminders** — optional push notifications with configurable time and days of the week
- **CSV export** — export data for any or all counts with a timestamped filename; save locally or share via any app
- **CSV import** — restore data from a previous export on a new device or after reinstall; duplicate entries are skipped automatically
- **Privacy first** — no internet connection required, no ads, no trackers, no analytics, all data stays on device

---

## Privacy

Hero Counter is built around privacy by design:

- No network permissions (except where explicitly required by user-initiated actions)
- No third-party SDKs, analytics, or advertising libraries
- No data leaves your device unless you explicitly export it
- All data is stored locally in a SQLite database via Room
- CSV exports are unencrypted — the app warns you before completing any export

---

## Requirements

- Android 8.0 (API 26) or higher
- No Google Play Services required — works on de-Googled devices (Iode OS, CalyxOS, GrapheneOS, /e/OS, Lineage OS,etc.)

---

## Installation

This app is not available on the Google Play Store. Install via sideloading:

1. Build the APK in Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Transfer `app-debug.apk` to your Android device
3. Enable **Install Unknown Apps** for your file manager in device Settings
4. Tap the APK to install

Alternatively, install via ADB:
```
adb install app-debug.apk
```

---

## Building from Source

### Prerequisites
- Android Studio (latest stable)
- JDK 17 (use the Embedded JDK bundled with Android Studio)
- Android SDK with API 34

### Steps
1. Clone the repository:
   ```
   git clone https://github.com/YOURUSERNAME/HeroCounter.git
   ```
2. Open Android Studio → **File → Open** → select the `HeroCounter` folder
3. Let Gradle sync complete
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**

---

## Open-Source Libraries

Hero Counter is built on the following open-source libraries, each used under the Apache 2.0 License:

| Library | Purpose |
|---|---|
| AndroidX AppCompat | Activity and UI compatibility |
| Material Components for Android | UI components and theming |
| Room Persistence Library | Local SQLite database |
| MPAndroidChart | Bar charts in the Statistics screen |
| ConstraintLayout | Layout engine |

---

## Data & Backup

### Exporting
Go to **Settings → Export data to CSV** to export your data. You will be prompted to choose between saving locally or sharing via another app, and warned that the CSV file is unencrypted.

### Importing
Go to **Settings → Import data from CSV** to restore from a previous export. The app will:
- Create any counts that don't exist yet
- Import all historical entries
- Skip any duplicate entries safely

### Database
Data is stored in a Room/SQLite database at the app's private storage location. It is not accessible to other apps without root access.

---

## Version

**Version 1.0** — Initial release

---

## License

This project is private and not licensed for public distribution or modification without permission.

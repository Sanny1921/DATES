# NeverMiss — Offline-First Date & Deadline Assistant

<p align="center">
  <strong>A 100% local, privacy-focused Android application designed to ensure you never miss critical dates, exams, birthdays, assignments, meetings, and payment deadlines.</strong>
</p>

---

## 🌟 Overview

**NeverMiss** is an offline-first Android application built with modern **Kotlin** and **Jetpack Compose**. It runs entirely on the user's device without any external cloud backends, databases, logins, or network calls. All event schedules, notes, and reminders remain private and securely stored in a local SQLite/Room database.

The reminder pipeline pairs Android's exact **AlarmManager** with **BroadcastReceivers** and **WorkManager** background reconciliation, ensuring reminder alerts fire on time—even across device reboots and low-power doze modes.

---

## 🛡️ Core Principles

- **Zero Network Footprint**: No backend, no cloud database, no analytics, no Firebase, and **no `INTERNET` permission** declared.
- **Privacy by Design**: All user data, event details, and deadlines reside exclusively on the device storage.
- **Reliable Local Alerts**: Uses exact and low-latency system alarms via `AlarmManager` with automatic fallback to inexact alarms when exact permissions are unavailable.
- **Reboot Resilience**: Automatically restores all future alarms upon device restart or package update using `BootReceiver`.
- **Periodic Sanity Reconciliation**: Employs `WorkManager` (`ReminderWorker`) to catch any edge-case alerts that might have been delayed while the device was off or in deep battery sleep.

---

## 🚀 Key Features

### 1. 🏠 Home Dashboard
- **Intelligent Bucketing**: Categorizes active events into **Overdue**, **Due Today**, and **Upcoming**, with distinct color indicators and urgency badges.
- **Live Countdown Labels**: Displays dynamic, human-readable relative time badges (e.g., *"4 days left"*, *"In 2 hours"*, *"Due now"*, *"Overdue by 2 days"*).
- **Attention Callouts**: Highlights immediate deadlines requiring urgent action.
- **Quick Completion**: One-tap checkbox to mark events complete or restore them.

### 2. 📅 Interactive Calendar View
- **Full Monthly Grid**: Visual dots indicate dates with scheduled deadlines.
- **Selected Day Agenda**: Inspect all events occurring on any selected date.
- **Quick-Add on Date**: Tap a date and easily launch creation with pre-selected dates.

### 3. ➕ Flexible Event Creation
- **Event Metadata**: Title (up to 80 chars), Category / Type (Birthday, Exam, Assignment, Meeting, Payment, Health, Renewal, Other), Target Date, and Target Time.
- **Multi-Tier Reminders**: Choose pre-set intervals (30 min, 15 min, 10 min, 5 min before) or custom intervals (in minutes, hours, or days).
- **Safety Constraints**: Enforces a maximum of 5 distinct reminder alerts per event, rejecting duplicates or negative offsets.

### 4. 📜 History & Management
- **Completed Archive**: View previously resolved events and milestones.
- **Restore & Cleanup**: Easily restore archived events back into active rotation or permanently delete them with confirmation.

### 5. 🔍 Detailed Event View
- View comprehensive event metadata, notes, priority levels, and full visual timeline of cascaded reminder milestones.
- System back gesture or top-app bar back button seamlessly returns to Home.

---

## 🏗️ Architecture & Pipeline

NeverMiss follows a strict single-door repository architecture:

```
                  ┌──────────────────────────────┐
                  │      Jetpack Compose UI      │
                  │ (Home, Calendar, Add, Detail)│
                  └──────────────┬───────────────┘
                                 │
                                 ▼
                  ┌──────────────────────────────┐
                  │       EventRepository        │
                  │ (Validation, Single Door API)│
                  └──────┬───────────────┬───────┘
                         │               │
                         ▼               ▼
            ┌──────────────────┐   ┌───────────────────────────┐
            │  Room Database   │   │      AlarmScheduler       │
            │ (SQLite Storage) │   │ (Next Upcoming Alarm API) │
            └──────────────────┘   └─────────────┬─────────────┘
                                                 │
                                                 ▼
                                   ┌───────────────────────────┐
                                   │    Android AlarmManager   │
                                   └─────────────┬─────────────┘
                                                 │ (fires at trigger time)
                                                 ▼
                                   ┌───────────────────────────┐
                                   │     ReminderReceiver      │
                                   └─────────────┬─────────────┘
                                                 │
                                                 ▼
                                   ┌───────────────────────────┐
                                   │    NotificationHelper     │
                                   │  (Shows Notification &    │
                                   │ Schedules Next Reminder)  │
                                   └───────────────────────────┘
```

### Background Reconciliation & Recovery
- **`BootReceiver`**: Listens for `ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED` to reschedule all pending alarms for active events.
- **`ReminderWorker`**: Periodic fallback worker running every 6 hours via `WorkManager` to audit missed reminders and keep the schedule synchronized without stale entity instances.

---

## 💻 Tech Stack

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with Material 3 design system
- **Architecture**: MVI / MVVM with Kotlin Coroutines & StateFlow
- **Local Persistence**: Android Jetpack Room (SQLite)
- **Scheduling**: Android `AlarmManager` (Exact & Low-Latency Alarms)
- **Background Tasks**: AndroidX `WorkManager`
- **Build System**: Gradle 8.9 with Android Gradle Plugin 8.5.2 (targetSdk 35, minSdk 26)

---

## 📁 Project Directory Structure

```
NeverMiss/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml          # Permissions, Receivers, MainActivity
│   │   │   ├── java/com/nevermiss/app/
│   │   │   │   ├── MainActivity.kt          # Compose root, navigation & BackHandler
│   │   │   │   ├── NeverMissApp.kt          # Custom Application & seed scheduling
│   │   │   │   ├── data/
│   │   │   │   │   ├── Entities.kt          # Room EventEntity & ReminderEntity
│   │   │   │   │   ├── Event.kt             # Domain models (Event, ReminderAlert)
│   │   │   │   │   ├── EventDao.kt          # Room DAO queries & transactions
│   │   │   │   │   ├── EventDatabase.kt     # RoomDatabase & initial seed insertion
│   │   │   │   │   └── EventRepository.kt   # Single door repository
│   │   │   │   ├── logic/
│   │   │   │   │   ├── AppGraph.kt          # Dependency injection & service locator
│   │   │   │   │   └── EventBuckets.kt      # Pure calculation & countdown logic
│   │   │   │   ├── reminders/
│   │   │   │   │   ├── AlarmScheduler.kt    # AlarmManager coordination
│   │   │   │   │   ├── BootReceiver.kt      # Device reboot alarm restoration
│   │   │   │   │   ├── NotificationHelper.kt# Local notification channels & builder
│   │   │   │   │   ├── ReminderReceiver.kt  # Alarm BroadcastReceiver
│   │   │   │   │   └── ReminderWorker.kt    # WorkManager fallback worker
│   │   │   │   └── ui/
│   │   │   │       ├── home/                # HomeScreen & EventCard components
│   │   │   │       ├── calendar/            # CalendarScreen & Day Agenda
│   │   │   │       ├── create/              # CreateEventScreen with reminder chips
│   │   │   │       ├── detail/              # EventDetailScreen
│   │   │   │       ├── history/             # HistoryScreen & Completed items
│   │   │   │       └── theme/               # Material 3 colors, typography & theme
│   │   │   └── res/                         # Drawables, strings, launcher icons
│   │   └── test/java/com/nevermiss/app/
│   │       └── logic/
│   │           ├── EventLabelsTest.kt       # Unit tests for countdown labels & bucketing
│   │           └── ReminderCalculationTest.kt# Unit tests for reminder math & constraints
│   └── build.gradle.kts                     # App module build configuration
├── build.gradle.kts                         # Root Gradle configuration
├── settings.gradle.kts                      # Gradle settings & repositories
├── solution.md                              # Team reference & specification
└── README.md                                # Application documentation
```

---

## 🔒 Permissions & Security

| Permission | Purpose | Enforcement |
|---|---|---|
| `POST_NOTIFICATIONS` | Required on Android 13+ (API 33+) to display local alerts | Requested dynamically in `MainActivity` |
| `SCHEDULE_EXACT_ALARM` | Required on Android 12+ (API 31+) for precise reminder timing | Checked at runtime; gracefully falls back to inexact alarms if withheld |
| `USE_EXACT_ALARM` | Standard exact alarm permission for reminder applications | Defined in `AndroidManifest.xml` |
| `RECEIVE_BOOT_COMPLETED` | Restores scheduled alarms after phone restart | Handled automatically by `BootReceiver` |
| `INTERNET` | **NEVER REQUESTED** | Excluded by design to guarantee 100% offline isolation |

---

## 🛠️ Building & Running Locally

### Prerequisites
- JDK 17 (e.g. OpenJDK 17)
- Android SDK (API 34/35 build tools installed)

### 1. Build the Debug APK
```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

./gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### 2. Run the Unit Test Suite
```bash
./gradlew test
```
All unit test suites validate:
- Dynamic countdown calculations (e.g. *Due in 2 hours*, *Tomorrow*, *4 days left*, *Overdue by 2 days*).
- Status bucketing (Overdue, Today, Upcoming, Completed).
- Completion rate percentages and statistics.
- Exact reminder fire time calculation (`eventAtMillis - minutesBefore * 60_000`).
- Nearest future reminder selection algorithm.
- Exclusion of delivered and past reminders.
- Maximum 5 reminders per event restriction.

---

## 📄 License
Offline-first application built with Google Jetpack Compose & Room. Free for personal productivity and deadline management.
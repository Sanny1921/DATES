# NeverMiss - Solution (updated)

NeverMiss is an offline Android app that makes sure you never miss a date: birthdays, exams, assignments, meetings, bills. It is written in Kotlin. Everything stays on the phone. There is **no backend, no server, no login, no internet permission**.

This file is the shared reference for the team. Names in `code font` are the **exact names in the delivered logic code** (package `com.example.offlineevents`). Use them as-is. Do not rename or retype them, or the code will break.

---

## 1. Who builds what

| Part | Owner | What it means |
|---|---|---|
| Logic layer | Sunny (done) | Local database, repository, alarms, notifications, boot restore, grouping and labels. 19 files, Kotlin only. |
| UI (screens) | Teammates | Home, Calendar, Create, History. Calls the repository and shows what it returns. |

Rule: the UI never writes to the database or sets alarms directly. **Every save, edit, done and delete goes through `EventRepository`.** Calling the DAO directly skips the alarm update and reminders will be wrong.

(DAO = "data access object", the small interface that talks to the database.)

---

## 2. App idea and flow

1. User opens the app (works with airplane mode on).
2. Home shows what needs attention: Overdue, Today, Upcoming.
3. User taps **Create** (plus button): types event name, picks date and time, picks reminders, saves.
4. The app stores the event in the phone database and sets one alarm with Android for the nearest reminder.
5. When the alarm fires, the app shows a notification. Title = event name. Tapping it opens that event.
6. The app sets the next alarm for the same event. This repeats until the reminders are used up.
7. User marks the event done. It moves to History. Its alarms are cancelled.

### Tabs (unchanged)

Home | Calendar | Create (plus) | History

---

## 3. What is saved in local storage

Storage is **Room** (Android's built-in local database, on top of SQLite). Two tables.

### 3.1 Table `events` - class `EventEntity`

| Field | Kotlin type | Meaning |
|---|---|---|
| `id` | `String` (primary key) | A UUID text, created automatically. |
| `title` | `String` | Event name. 1 to 80 characters. Also the notification title. |
| `type` | `String` | Name of an `EventType` value, e.g. `"BIRTHDAY"`. |
| `eventAtMillis` | `Long` | The event moment, in milliseconds since 1 Jan 1970 UTC. Indexed. |
| `reminderMinutes` | `List<Int>` | Reminder offsets in minutes before the event. Saved in one column as text like `"30,10,5"` by `EventConverters`. |
| `done` | `Boolean` | `true` = completed (shows in History). |
| `createdAtMillis` | `Long` | When it was first saved. |
| `updatedAtMillis` | `Long` | When it was last saved. |
| `subject` | `String` | Optional subject (for example the exam subject). Empty string if none. |
| `notes` | `String` | Optional notes. Empty string if none. |

### 3.2 Table `reminders` - class `ReminderEntity`

One row per reminder. The code creates these for you when you save an event. Teammates normally never touch this table.

| Field | Kotlin type | Meaning |
|---|---|---|
| `id` | `Int` (auto-generated primary key) | Permanent number. Also used as the Android notification ID. |
| `eventId` | `String` | Which event this belongs to. Deleting the event deletes these rows too. |
| `minutesBefore` | `Int` | The offset, e.g. 30. Unique per event. |
| `fireAtMillis` | `Long` | `eventAtMillis - minutesBefore * 60_000`. |
| `handled` | `Boolean` (default `false`) | `true` after the notification was processed. Never sent twice. |

### 3.3 Class `Event` - what the UI uses

The UI works with `Event`, not `EventEntity`. They are almost the same, with **two differences you must remember**:

| In `Event` (UI uses this) | In `EventEntity` (database) |
|---|---|
| `type: EventType` (an enum) | `type: String` |
| `reminders: List<Int>` | `reminderMinutes: List<Int>` |

Everything else has the same name and type: `id`, `title`, `eventAtMillis`, `done`, `createdAtMillis`, `updatedAtMillis`, `subject`, `notes`. Convert with `Event.toEntity()` and `EventEntity.toEvent()`. The repository already does this, so the UI only sees `Event`.

Defaults when you create one: `type = EventType.OTHER`, `reminders = emptyList()`, `done = false`, `subject = ""`, `notes = ""`, `id` = new UUID.

```kotlin
val event = Event(
    title = "Isha's birthday",
    type = EventType.BIRTHDAY,
    eventAtMillis = chosenLocalDateTime.toEventMillis(),
    reminders = listOf(30, 10, 5)
)
```

`toEventMillis()` is a helper on `LocalDateTime` (uses the phone time zone).

### 3.4 Event types - read this before coding

Delivered `EventType` has **5 values**:

| Value | `iconKey` (for your own icon choice) |
|---|---|
| `BIRTHDAY` | `"cake"` |
| `EXAM` | `"school"` |
| `ASSIGNMENT` | `"book"` |
| `MEETING` | `"people"` |
| `OTHER` | `"event"` |

The new UI mockups show category chips such as **Payment, Health, Renewal / Gov**. These do not exist in the code yet. Two options:

- **Option A - map to existing types.** Example: Payment, Health, Renewal all become `OTHER`. No code change. The category label is lost (it could be kept in `subject`).
- **Option B - extend `EventType` together.** Add values like `PAYMENT("card")`, `HEALTH("heart")`, `RENEWAL("doc")` in `Event.kt`. This is a small change. The type is saved by name, so old saved rows stay valid.

**Agree on A or B as a team before anyone writes UI code for categories.** Do not invent type names on your own.

---

## 4. What is shown on screen (computed values)

These are not stored. They are calculated each time from the saved events.

### 4.1 Home groups - `EventBuckets`

Get it with `repo.homeBuckets()`.

| Name | Kotlin type | How it is derived |
|---|---|---|
| `overdue` | `List<Event>` | Not done, and `eventAtMillis` is before now. |
| `today` | `List<Event>` | Not done, due now or later, and the date is today. |
| `upcoming` | `List<Event>` | Not done, due now or later, and the date is after today. |
| `completed` | `List<Event>` | Every event with `done = true`. This is the History list. |
| `overdueCount` | `Int` | `overdue.size` (red "Overdue" number) |
| `todayCount` | `Int` | `today.size` |
| `upcomingCount` | `Int` | `upcoming.size` |
| `isEmpty` | `Boolean` | `true` only when there are no saved events at all, including done ones. Use it for the empty state. |

The three pending groups never overlap. All lists are sorted by time. Note: an event from this morning moves to Overdue in the afternoon (it follows the exact time, not the date).

### 4.2 Relative label - `EventLabels.relative(event)`

Returns a `String`:

| Situation | Example result |
|---|---|
| `done = true` | `"Done"` |
| Exactly now | `"Due now"` |
| Other day, future | `"4 days left"` (counts calendar days) |
| Other day, past | `"2 days overdue"` |
| Same day, future | `"In 2 hours"` or `"In 15 minutes"` |
| Same day, past | `"3 hours overdue"` |

Room tells the UI when data changes, but not when time passes. **Recompute labels and groups at least once a minute, and at midnight or when the time zone changes.**

### 4.3 Other screens

| Screen item | Where the value comes from |
|---|---|
| Calendar: events on a tapped day | `repo.eventsOn(date: LocalDate)` returns `List<Event>` |
| Calendar: dot markers on days | A day has a marker if `eventsOn(day)` is not empty. Or collect `repo.observeAll()` and group by date in the UI. |
| Calendar: any time range | `repo.eventsBetween(from: Long, until: Long)` (`until` is excluded) |
| Live list that updates itself | `repo.observeAll()` returns `Flow<List<Event>>`. Collect it in the lifecycle scope. |
| Open one event | `repo.getById(id: String)` returns `Event?` (null = deleted) |
| History tab | `EventBuckets.completed` |
| Notification title | `Event.title` |
| Notification text | `NotificationHelper.body(event)`, e.g. `"Today, 5:30 PM"` or `"Tomorrow, 9:00 AM"` |
| Alarm result after save | `SaveResult.alarmMode`: `AlarmMode.EXACT`, `INEXACT` or `NONE` |

### 4.4 Not in the logic layer (UI team computes it)

These appear in the mockups but there is **no ready function** for them. Build them in the UI from the values above, and agree on the exact rule first:

- **Week progress bar** - for example done this week divided by all events this week. Use `completed` and the pending groups, filtered by `eventAtMillis` inside the week.
- **"Needs attention" callout** - for example show it when `overdueCount > 0`.
- **Amounts like "$142.80" on a bill** - no field exists. Put it in `notes` for now.

---

## 5. The scheduler (reminders)

### 5.1 Create flow

1. **Event name** - text field. Required. 1 to 80 characters.
2. **Date** and **time** - pickers. Must be in the future when creating or when moving the event.
3. **Reminders** - chips the user can turn on or off:
   - 30 minutes before = `30`
   - 15 minutes before = `15`
   - 10 minutes before = `10`
   - 5 minutes before = `5`
   - **Custom** - user enters a number of minutes, hours or days. UI converts to minutes: hours x 60, days x 1440.
4. **Maximum 5 reminders per event.** When 5 are chosen, block the 6th (disable the remaining chips and show text like "Maximum 5 reminders"). The repository also rejects more than 5 with an error, as a safety net.
5. Save: `repo.insert(event)`.

Rules the repository enforces (it throws an error if broken, so validate in the UI first):

- 5 reminders at most, all different (no duplicates).
- Each offset is a whole number from `0` to `5_256_000` minutes (about 10 years). `0` means "at event time". Negative numbers are rejected.
- Title not blank and at most 80 characters.
- New or moved events must be in the future. Old overdue events can still be edited or marked done.
- Reminders are saved sorted from largest to smallest.

### 5.2 Schedule, fire, reschedule

1. Fire time of each reminder = `eventAtMillis - minutesBefore * 60_000`.
2. The app sets **one alarm per event**, for the nearest reminder that is still in the future and not handled. Nothing is set for reminders in the past.
3. When Android fires it, `ReminderReceiver` checks the database, shows the notification (title = event name, tap opens the event), and marks that reminder `handled`.
4. It then sets the alarm for the next reminder of that event.

(Alarm = Android's `AlarmManager`, a system clock that can wake the app even when it is closed.)

### 5.3 Edge cases

| Case | What happens |
|---|---|
| Edit date or reminders | Old reminders are deleted and recreated, old notifications cleared, new alarm set. |
| Edit title only | Handled reminders stay handled. No repeat notification. |
| Delete event | Alarm cancelled, reminders deleted, notifications removed. |
| Mark done | Alarm cancelled and notifications removed. Event goes to History. |
| Mark not done again | Only future reminders are created again. |
| Reminder time already passed | Skipped. Past reminders are not sent in a burst. |
| Phone restarts, app updated, clock or time zone changed | Alarms are restored by `BootReceiver` and `RescheduleWorker`. Only future reminders come back. |
| Notification permission denied | No notification is shown, no crash. That reminder is counted as missed and is not replayed. UI shows a banner (`NotificationHelper.canNotify(context)`). |
| Exact alarm permission denied | App falls back to an inexact alarm (`AlarmMode.INEXACT`). It can be late. Show a warning, do not promise a time. |
| Tapped event was deleted | Show "Event no longer exists". |
| Reminders very close together | Because only the next one is scheduled, if Android delays an alarm past the next reminder time, that one can be missed. Avoid very tight gaps in the demo. |

---

## 6. Permissions

| Permission | Why | Notes |
|---|---|---|
| `POST_NOTIFICATIONS` | Show notifications on Android 13+ | The Activity must ask the user, after explaining why. The library cannot show the dialog. |
| `SCHEDULE_EXACT_ALARM` | Exact reminder time on Android 12+ | Explain, then open `AlarmScheduler.exactAlarmPermissionIntent()`. Check `canUseExactAlarms()` again when the user comes back. |
| `RECEIVE_BOOT_COMPLETED` | Restore alarms after restart | Automatic. |
| `INTERNET` | **Not requested. Never add it.** | Offline by design. |

Also set `allowBackup="false"` on the application tag (local-only build).

---

## 7. Offline rules

- No server, no Firebase, no login, no sync, no network calls.
- All data is in the phone's Room database. Uninstalling the app or clearing its data **deletes all events**. There is no backup.
- Alarms do not work when the phone is powered off, before first unlock after a restart, or after the user force-stops the app (until the app is opened again). Some phone makers add extra battery limits.
- Time is stored as one real moment (UTC milliseconds). A time zone change changes the displayed time, not the moment.
- Not included, so do not promise them: snooze, notification buttons, quiet hours, daily summary, repeating birthdays, undo.

---

## 8. Setup steps for teammates

1. Copy the Kotlin folders (`data`, `logic`, `reminders`) into the app. Keep the package name `com.example.offlineevents`, or rename it in every file and in both receivers in the manifest.
2. Merge the permissions and the two receivers (`ReminderReceiver`, `BootReceiver`) into the existing `AndroidManifest.xml`. Do not replace the whole file.
3. Add the deep-link filter (`offline-events://event/<id>`) to the activity that shows event details.
4. In both `onCreate` and `onNewIntent` call `NotificationHelper.eventIdFromIntent(intent)`. If it returns an id, load `repo.getById(id)` and open the detail screen.
5. On app open, and after returning from the exact-alarm settings, call `RescheduleWorker.enqueue(context)`.
6. Create the channel with `NotificationHelper.createChannel(context)` and ask for notification permission on Android 13+.
7. Get the repository with `AppGraph.get(context).repository`. Call it inside a coroutine (Kotlin background task).

The `README.md` in the logic ZIP has the full wording and a phone test checklist.

---

## 9. Quick name list (copy exactly)

Classes: `Event`, `EventEntity`, `ReminderEntity`, `EventType`, `EventConverters`, `EventDao`, `ReminderDao`, `EventDatabase`, `EventRepository`, `SaveResult`, `AppGraph`, `EventBuckets`, `EventLabels`, `AlarmScheduler`, `AlarmMode`, `NotificationHelper`, `ReminderReceiver`, `BootReceiver`, `ReminderWorker`, `RescheduleWorker`

Repository functions: `observeAll()`, `getById(id)`, `eventsBetween(from, until)`, `eventsOn(date)`, `homeBuckets()`, `insert(event)`, `update(event)`, `setDone(id, done)`, `delete(id)`, `restoreFutureAlarms()`

Tables: `events`, `reminders`

---

## 10. Testing status

The logic layer compiles and has 3 passing unit tests (grouping, labels, empty state). Real alarm delivery, restart behavior, permission dialogs, phone-maker battery limits, and tapping a notification still need testing on a real phone once the UI is connected.

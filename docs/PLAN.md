# Yaad — plan

A reminder app for Android phones and iPads that never misses a date that matters: everyday reminders, loan EMIs,
bills, subscriptions, credit cards, insurance, medicines, birthdays and document expiry.
One Kotlin Multiplatform codebase with Compose Multiplatform UI.

---

## 1. Goals

1. **Reliable:** every reminder fires on time, once, even after a reboot, an app update, Doze, a time change or a
   week without opening the app.
2. **Fast:** adding a reminder takes under 5 seconds (quick-add text, sensible defaults).
3. **Money-aware:** loans, cards and bills show amounts, due dates and what is left, and the maths is exact.
4. **Private:** works fully offline; no account, no ads, no analytics; backup is the user's choice.
5. **iPad-native:** two-pane layouts, keyboard shortcuts, Split View, and landscape as well as portrait.

## 2. Users

| User | Needs |
|---|---|
| Everyday user | quick reminders, repeating tasks, birthdays, medicines |
| Household money manager | EMIs, rent, electricity and phone bills, credit-card due dates, SIPs, insurance premiums |
| Small-business owner | client payment follow-ups, GST and tax dates, licence renewals |

## 3. Features

Legend: **MVP** = first public release; **Later** = after release.

### 3.1 Reminders (core)
| Feature | Details | When |
|---|---|---|
| Create, edit, delete | title, notes, date, time or all-day, category, colour, priority | MVP |
| Repeat rules | every N minutes / hours / days / weeks (chosen weekdays) / months (on day D, on the nth weekday, on the last day) / years; ends never, on a date, or after N times | MVP |
| Early alerts | any number of alerts before the due time (e.g. 3 days, 1 day, 1 hour before) | MVP |
| Notification actions | Done, Snooze (10 min, 1 hour, tomorrow, custom), Skip this one; work from the lock screen | MVP |
| Nagging mode | repeats every N minutes until marked done (for medicines and payments) | MVP |
| Alarm-style alerts | full-screen, sound through silent mode where the platform allows, for critical items | Later |
| Checklists | sub-items inside a reminder | MVP |
| Categories and tags | built-in (Money, Health, Personal, Work, Home, Vehicle) plus custom | MVP |
| Overdue handling | overdue list; a missed occurrence of a repeating item is kept until handled | MVP |
| History | completed and skipped occurrences per reminder | MVP |
| Attachments | photo of a bill, document or prescription | Later |
| Location reminders | "when I reach home" (geofence) | Later |
| Shared reminders | share a reminder or list with family | Later |

### 3.2 Views
| Feature | Details | When |
|---|---|---|
| Today | overdue, due today, next 24 hours; money due this week | MVP |
| Upcoming | grouped by day for the next 30 days | MVP |
| Calendar | month grid with dots, and an agenda list | MVP |
| Search and filters | by text, category, type, amount range, status | MVP |
| Money dashboard | this month's dues, paid vs pending, total EMI outgo, upcoming large payments | MVP |
| iPad two-pane | list on the left, detail or editor on the right; drag to reschedule in the calendar | MVP |

### 3.3 Loans and EMI tracker
| Feature | Details | When |
|---|---|---|
| Add a loan | lender, type (home, car, personal, education, gold, consumer durable), principal, annual rate, tenure, first EMI date, EMI day | MVP |
| EMI calculation | reducing balance; EMI from P, r, n, or n from P, r, EMI; or the user's EMI from the bank as given | MVP |
| Amortisation schedule | each month: EMI, interest, principal, balance; cumulative interest | MVP |
| Due reminders | N days before each EMI and on the day; "Mark paid" from the notification | MVP |
| Paid tracking | paid / pending / late per instalment; outstanding principal; EMIs left; % repaid | MVP |
| Prepayment | part-payment with choice: reduce tenure or reduce EMI; shows interest saved | MVP |
| Rate changes | floating rate changes from a date; recompute keeping EMI or keeping tenure | MVP |
| Foreclosure estimate | outstanding principal plus an optional foreclosure charge % | MVP |
| No-cost / flat-rate EMIs | consumer EMIs with processing fee and GST on interest shown separately | Later |
| Export | schedule as CSV / PDF | Later |

Worked example (a test fixture): P = ₹10,00,000, rate 8.5% a year, 240 months gives **EMI ₹8,678.23**. Month 1:
interest ₹7,083.33, principal ₹1,594.90. With interest rounded to the paisa each month, the 240th instalment is
₹8,679.66, so the total paid is ₹20,82,776.63 and the total interest ₹10,82,776.63 (corrected in Phase 6: the first
draft gave 240 × EMI, ₹20,82,775.20, which leaves ₹1.43 unpaid).
EMI = P·r·(1+r)ⁿ / ((1+r)ⁿ − 1), with r = annual rate / 12. Amounts are computed with exact decimal arithmetic and
rounded to the paisa each month; the last instalment absorbs the rounding.

### 3.4 Bills, subscriptions, cards, insurance, investments
| Feature | Details | When |
|---|---|---|
| Bills | electricity, water, gas, mobile, broadband, rent, maintenance, school fees; amount (fixed or varying), cycle, due day | MVP |
| Subscriptions | OTT, apps, gym; renewal date, price, cycle; free-trial-ending alerts | MVP |
| Credit cards | statement day, due day; amount and minimum due entered per cycle; due-date alerts | MVP |
| Insurance | life, health, vehicle; premium amount, frequency, renewal date, policy number | MVP |
| Investments | SIP dates, recurring deposits, FD / RD maturity, PPF yearly deposit before 31 March | MVP |
| Auto-pay flag | auto-pay items get a "check it went through" reminder instead of a "pay now" one | MVP |
| Monthly summary | total committed outgo by category; paid vs pending | MVP |
| Spend history | amount paid per cycle, a chart for varying bills | Later |

### 3.5 Life admin
| Feature | Details | When |
|---|---|---|
| Document expiry | passport, driving licence, vehicle registration and PUC, insurance, visas, professional licences; alerts 90/30/7/1 days before | MVP |
| Warranties | product, purchase date, warranty period, bill photo (Later); expiry alert | MVP |
| Birthdays and anniversaries | yearly, age or years shown; import from contacts | MVP (import Later) |
| Medicines | times per day, with or without food, course length, refill reminder from pill count; nagging until taken | MVP |
| Appointments | doctor, service centre, government office | MVP |
| Vehicle | service due by date or kilometres, insurance, PUC | Later |
| Taxes and compliance (India) | advance-tax dates, ITR due date, GST return dates as optional templates | Later |

### 3.6 Input and platform
| Feature | Details | When |
|---|---|---|
| Quick add | "rent 15000 on 5th every month", "call mom tomorrow 7pm", "car insurance expires 12 Mar 2027"; shows how it was understood before saving | MVP |
| Templates | one-tap templates for common items (EMI, rent, credit card, insurance, medicine) | MVP |
| Widgets | Android home-screen widget (Jetpack Glance): today's items; iPad / iOS widget (WidgetKit, Swift) | Later |
| Shortcuts | Android app shortcuts; iOS App Intents / Siri "Remind me…" | Later |
| Backup and restore | export / import of an encrypted JSON file; restore on a new device | MVP |
| Cloud backup | the user's own Google Drive / iCloud Drive file, no server of ours | Later |
| App lock | biometric / device PIN lock, hide amounts in notifications | MVP |
| Settings | default reminder time, quiet hours, early-alert defaults, currency, first day of week, 12/24-hour, theme | MVP |
| Languages | English; Hindi next; strings never hard-coded | English MVP, Hindi Later |

## 4. Getting notifications right (the hard part)

### Android
- **Exact alarms:**
  - Exact times come from `AlarmManager.setExactAndAllowWhileIdle` (or `setAlarmClock` for alarm-style alerts).
  - This needs `SCHEDULE_EXACT_ALARM`, which is denied by default for new installs on Android 14+. Ask for it with a
    clear explanation.
  - If it is denied, fall back to inexact alarms plus WorkManager, and show "times may drift by a few minutes" in
    Settings.
  - Check whether Play's policy allows `USE_EXACT_ALARM` for this app before relying on it.
- **Permissions and events:**
  - `POST_NOTIFICATIONS` runtime permission on Android 13+.
  - Reschedule everything on `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET` and `TIMEZONE_CHANGED`.
- **Delivery:**
  - Notification channels per category, so users can set each one's sound and importance.
  - Actions run through a BroadcastReceiver and update the database without opening the app.
- **Decided in Phase 3:**
  - Yaad does not use `USE_EXACT_ALARM` (Play reserves it for alarm-clock and calendar apps). Without
    `SCHEDULE_EXACT_ALARM`, Today shows "Reminders may arrive a few minutes late" with a button to allow it.
  - Alerts are planned 10 days ahead (at most 400 alarms; Android allows 500) and re-planned after every alert,
    action and change, on boot, app update, clock and time-zone changes, and twice a day.
  - A reminder that came due while the phone was off is shown as soon as Android delivers the boot broadcast
    (looking back at most 24 hours). On a busy emulator that broadcast took up to 4.5 minutes.
  - Each occurrence has one notification: its early, due and nag alerts replace each other.
- **OEM battery savers** (Xiaomi, Oppo, Vivo, Samsung): detect them and guide the user to "no restrictions"; test on
  at least one aggressive OEM device.

### iOS / iPadOS
- **Scheduling:**
  - Local notifications with `UNUserNotificationCenter`; no server is needed.
  - iOS keeps at most **64 pending** notifications per app. `core:scheduling` therefore keeps a rolling window (the
    next 64 occurrences across all reminders, by time) and refills it when the app opens.
  - It also refills on a background refresh (`BGAppRefreshTask`) and whenever an item changes.
- **Delivery:**
  - Notification categories carry the Done / Snooze / Skip actions.
  - Time-sensitive interruption level for money and medicine items (Focus-aware).
- iPad: same app, adaptive layout; no phone-only assumptions.
- **Decided in Phase 4:**
  - Notifications use calendar triggers with UTC date components, so each fires at its planned instant; a time-zone
    change re-plans floating reminders.
  - When more than 64 alerts are planned, the earliest 63 are scheduled plus a final "Open Yaad to keep reminders
    coming" notice, so reminders never stop silently.
  - iOS keeps pending notifications across reboots and updates, and does not wake the app when one fires, so
    there is no boot catch-up on iOS. Handling an occurrence clears all of its delivered notifications.
  - Time-sensitive interruption needs an entitlement from a paid developer account (Phase 12).

### Shared rules (`core:scheduling`)
- The schedule is a pure function: from (reminders, completions, now, time zone) to (the list of notifications that
  should exist). Platform code only adds what is missing and removes what is stale, so syncing is idempotent and
  testable.
- Every scheduled notification has a stable id derived from (reminder id, occurrence time, alert offset), so
  rescheduling never duplicates.

## 5. Correctness

| Area | How it is verified |
|---|---|
| Recurrence | property tests over thousands of random rules and dates; fixtures for 31st-of-month, 29 February, last weekday, every-N spans across year ends, DST changes (e.g. America/New_York), time-zone travel |
| Scheduling | pure-function tests: the expected notification set for given inputs; the iOS 64-limit window; idempotent resync |
| EMI and loans | fixtures from bank EMI calculators and hand-worked schedules (example in § 3.3); prepayment and rate-change scenarios; balance reaches exactly 0 |
| Money formatting | Indian grouping (₹1,23,45,678.90), other locales, no floating point anywhere |
| Quick add | a table of 200+ phrases with the expected parse, including Hinglish ("kal subah 8 baje") |
| Database | migration tests from every past schema version; export → import round trip |
| UI | Compose UI tests for the main flows on the desktop target; screenshot tests for phone and iPad sizes |
| Devices | manual checklist each release: reboot, Doze (`adb shell dumpsys deviceidle force-idle`), time change, one aggressive-OEM phone, an iPad |

## 6. Technology

| Part | Choice |
|---|---|
| Language and UI | Kotlin 2.x, Compose Multiplatform (Material 3) |
| Build | Gradle with version catalogs and convention plugins in `build-logic`, JDK 21 toolchain |
| Date and time | kotlinx-datetime (IANA zones) |
| Money | `Long` minor units; a small `Money` value class; exact decimal maths for interest |
| Storage | SQLDelight (SQLite) with migrations |
| Async | Kotlin coroutines and Flow |
| Navigation | Compose Navigation with type-safe routes |
| DI | constructor injection; a tiny hand-written container (add Koin only if it earns its place) |
| Android notifications | AlarmManager, WorkManager, NotificationCompat, Glance widgets |
| iOS notifications | UNUserNotificationCenter via Kotlin/Native interop; WidgetKit in Swift later |
| Testing | kotlin.test, Turbine for flows, Compose UI tests, property tests |
| CI | GitHub Actions: `./gradlew check` on Linux and iOS-simulator tests on macOS |

### Modules
```
core/model          entities and value types
core/recurrence     next-occurrence engine
core/finance        EMI, amortisation, prepayment, money
core/scheduling     what notifications should exist (pure)
core/quickadd       natural-language parser
data                SQLDelight database, repositories, backup
platform/notifications  expect/actual scheduler for Android and iOS
ui/designsystem     theme, components, adaptive layouts
feature/today, feature/reminders, feature/loans, feature/money, feature/calendar, feature/settings
app/android, app/ios, app/desktop
```

### Data model (first cut)
- `Reminder`: id, title, notes, category, type (general, bill, emi, card, subscription, insurance, investment,
  document, warranty, medicine, birthday, appointment), first due, all-day, time zone, recurrence rule, early alerts,
  priority, nag interval, amount (optional), auto-pay, linked loan id, archived.
- `Occurrence`: reminder id, due instant, status (pending, done, skipped, snoozed until), paid amount, note.
- `Loan`: id, lender, kind, principal, annual rate (basis points), tenure months, first EMI date, EMI amount (computed
  or given), rate changes, prepayments.
- `ChecklistItem`, `Category`, `Attachment` (Later), `Settings`.

## 7. Phases

Each phase ends with its acceptance checks passing, a merged PR (merge commit) and a short summary. Small commits
throughout.

| # | Phase | Delivers | Acceptance |
|---|---|---|---|
| 0 | **Foundation** | KMP project for Android, iOS, desktop; version catalog; build-logic; design system (colours, type, components, adaptive scaffold); app icon; CI | Builds and runs on an Android emulator, iPad simulator and desktop; CI green |
| 1 | **Model and recurrence** | `core:model`, `core:recurrence` with every rule in § 3.1 | Property and fixture tests pass, including month ends, leap years, DST and time-zone changes |
| 2 | **Storage** | SQLDelight schema, repositories, migrations scaffold, export / import | Round-trip and migration tests pass |
| 3 | **Scheduling and Android notifications** | `core:scheduling`; Android alarms, channels, actions, boot / time-change handling, permission flows | Notifications fire on time on an emulator after reboot, in Doze and after a time change; actions update the data; no duplicates |
| 4 | **iOS / iPad notifications** | UNUserNotificationCenter scheduler with the 64-item rolling window, actions, background refresh | Same checks on the iPad simulator and a device if available |
| 5 | **Reminders UI** | Today, Upcoming, editor, repeat picker, early alerts, snooze, checklists, categories, overdue, history; iPad two-pane | UI tests for create → notify → done; screenshots at phone and iPad sizes |
| 6 | **Loans and EMI** | `core:finance`; loan editor, schedule, paid tracking, prepayment, rate change, foreclosure estimate, EMI reminders | Finance fixtures (§ 3.3 and bank calculator cases) pass; balance ends at exactly 0 |
| 7 | **Money items** | bills, subscriptions, credit cards, insurance, investments, auto-pay, money dashboard and monthly summary | Templates create correct schedules; the dashboard totals match the fixtures |
| 8 | **Life admin** | document expiry, warranties, birthdays, medicines (nagging, refill), appointments | Tests for each template's schedule; medicine nagging stops on Done |
| 9 | **Quick add, calendar, search** | `core:quickadd`, calendar month / agenda, search and filters | 200+ phrase fixtures parse correctly; calendar matches the scheduling engine |
| 10 | **Polish and safety** | app lock, hide amounts, encrypted backup, settings, accessibility pass, Hindi strings | TalkBack / VoiceOver walkthrough; 200% font; backup restores on a fresh install |
| 11 | **Widgets and shortcuts** | Android Glance widget, app shortcuts; iOS widget and App Intents | Widgets show today's items and update after Done |
| 12 | **Release** | Play Store internal test, TestFlight for iPad, store listings, privacy policy | Approved for internal testing on both stores |

## 8. What we need from you
- Confirm the name **Yaad** and the app icon direction (or suggest another).
- **Accounts:** a Google Play developer account (one-time US$25) and an Apple Developer Program membership
  (US$99 / year, needed for TestFlight and running on a physical iPad).
- **Devices:** an Android phone for testing, ideally one from a brand with aggressive battery saving (Xiaomi, Oppo,
  Vivo, Realme, Samsung), and an iPad if available.
- **Licence:** for the code (e.g. MIT, Apache-2.0, or all rights reserved for a private project).

## 9. Risks and decisions
- **Exact-alarm policy (Android 14+):** users must grant exact alarms. The app explains why and works (less
  precisely) without it.
- **OEM battery killers** can stop alarms; the mitigation is onboarding guidance, plus `setAlarmClock` for critical
  items.
- **iOS 64-notification limit:** handled by the rolling window; heavy users with many nagging items get a warning.
- **History after travel (known gap, found in Phase 6):** history is keyed by the due instant, so for a floating
  reminder a past occurrence's instant moves when the phone changes time zone and its Done record no longer matches.
  EMI reminders are pinned to the zone they were made in; general reminders need the local due time stored with each
  record (a schema migration), planned for Phase 10.
- **Financial accuracy:** banks differ in day-count and rounding. Yaad lets the user enter the bank's EMI and dates as
  given, and labels its own figures "estimated".
- **Scope:** this is a large feature list. Phases 0–5 give a dependable reminder app to use daily; everything else
  builds on that.

- **Recurrence rules (decided in Phase 1):**
  - A monthly "day 31" falls on the last day of shorter months and returns to the 31st afterwards (no drift);
    a yearly 29 February falls on 28 February in common years.
  - Day rules and longer keep the local wall-clock time: "every day at 9:00" is 9:00 wherever the phone is. Minute
    and hour rules count elapsed time, so "every 2 hours" stays 2 hours apart through a DST change.
  - A time skipped by a DST gap (2:30 on a spring-forward night) fires just after the gap; a time that happens twice
    fires once, at the first.
  - "Every N weeks" counts weeks from the first occurrence's week, with weeks starting on Monday. The first
    occurrence is the first date on or after the start that matches the rule.

## 10. Progress

| # | Phase | Status | Notes |
|---|---|---|---|
| 0 | Foundation | Done | KMP project (Android, iOS, desktop), build-logic convention plugins, Material 3 design system with dynamic colour on Android 12+, adaptive navigation (bar below 600 dp, rail above) and list/detail layout, placeholder bell icon, CI. Package and bundle id `com.pranavshukla.yaad`; iOS 16+, Android 8.0+ (API 26). |
| 1 | Model and recurrence | Done | `core:model` (schedules, recurrence rules, reminders, occurrences, `Money` in `Long` minor units with overflow checks); `core:recurrence` (occurrences, next after, range) for every rule in § 3.1. 34 fixtures (31st, 29 February, nth and last weekdays, year ends, New York DST gap and overlap, travel) and property tests over 4,000 seeded random rules against a day-by-day oracle, on the JVM and the iOS simulator. |
| 2 | Storage | Done | SQLDelight schema v1 (reminders, checklists, occurrence history, custom categories, settings) with foreign keys enforced on Android, iOS and desktop; `ReminderStore` and `PreferenceStore` with flows; rules stored as RRULE-style text; migration harness (a schema snapshot and sample rows per version, checked on every build); versioned JSON backup, restored all or nothing. Android 8 ships SQLite 3.18, so no `UPSERT`: saves update or insert in a transaction. |
| 3 | Scheduling and Android notifications | Done | `core:scheduling` plans early, due, snoozed and nagging alerts with stable ids, finds alerts missed while the phone was off, and diffs against what is scheduled. Android: exact alarms (inexact fallback, explained in the app), channels per category, Done / Snooze / Skip from the lock screen, rescheduling on boot, update, clock, zone and permission changes. Emulator (API 36): on time to within 0.3–1 s, also in Doze; reboot, zone change and clock jump handled; Done stops nagging. Two bugs found on the emulator and fixed with regression tests (sub-millisecond start times; alarms orphaned by app updates). |
| 4 | iOS / iPad notifications | Done | `UNUserNotificationCenter` scheduler in `platform:notifications` (iosMain): 64-alert rolling window with a refill notice, Done / Snooze / Skip category, re-plans on launch, foreground, significant time change, zone change and background app refresh (`BGAppRefreshTask`). iPad Pro 11-inch simulator (iOS 26): due and nag notifications delivered to the second; Done recorded, nags removed and delivered notifications cleared; exactly 64 pending with the refill notice; pending notifications kept across a reboot. Not verified: tapping an action in the system UI (`simctl` cannot tap; the handler was driven directly), a time-zone change (the simulator follows the Mac's zone), background refresh (iOS decides when), and a physical iPad (needs an Apple Developer account). |
| 5 | Reminders UI | Done | `core:scheduling` gains `Agenda` (overdue / today / upcoming by day, next due) and day and time labels; `feature:reminders` adds the Today and Upcoming lists (one-tap done and undo), the reminder detail (Done, Snooze 10 min / 1 hour / tomorrow, Skip, checklist, history, edit, delete) and the editor (date, time or all day, every repeat rule with ends, early alerts, nagging, built-in category, priority, checklist, notes). Phones show detail and editor full screen with system back; from 600 dp the list sits beside them. Desktop UI tests run create → notification planned → done → notification cancelled at phone and iPad widths, and capture screenshots of both. Emulator: created a monthly reminder in the editor, its exact alarm was registered, Done in the app cancelled it, back returned to the list. iPad simulator: two-pane list and detail. Later: custom categories (Settings, Phase 10), opening a reminder from its notification, swapping the deprecated `BackHandler` for `NavigationEventHandler`. |

| 6 | Loans and EMI | Done | `core:finance` (bignum, Apache-2.0, for the EMI formula; monthly interest is integer arithmetic in paise): EMI, amortisation with the last instalment absorbing rounding, prepayment (shorten or lower EMI, interest saved), rate changes (keep EMI or tenure), foreclosure estimate, paid / due / late tracking, Indian and international money formatting. Schema v2 adds loans (first real migration, covered by the harness); backups include loans (format v2, v1 still restores). Each loan has one EMI reminder whose done occurrences are its paid instalments, pinned to the zone it was made in. Fixtures computed independently with Python `decimal` match to the paisa on JVM and iOS; EMIs match bank calculators to the rupee. Emulator: added a loan in the editor (EMI ₹4,247.46), saw the schedule, made a part-payment (300 → 218 EMIs, ₹2,99,964.29 interest saved) and the EMI reminder followed. iPad simulator: upgraded over a v1 database, data kept. |

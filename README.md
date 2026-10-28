# Yaad (याद)

A calm, reliable reminder app for Android phones and iPads, written in Kotlin (Kotlin Multiplatform with Compose
Multiplatform). "Yaad" means "remember" in Hindi.

Yaad remembers the things that cost money or trouble when forgotten:

- **Reminders:** one-time and repeating, with snooze.
- **Loans and EMIs:** the full repayment schedule and a reminder before every due date.
- **Bills and subscriptions:** credit-card due dates, insurance premiums and SIPs.
- **Dates that matter:** birthdays, medicines, and the expiry of documents and warranties.

Everything stays on the device unless you choose to back it up.

**Status:** Phases 0–5 are done: the recurrence engine, the on-device database (with migrations and backup) and
reliable notifications on Android (exact alarms, Doze, reboot, time changes) and iPad (a rolling 64-notification
window), with Done / Snooze / Skip, are in place and tested, and the reminder screens (Today, Upcoming, detail,
editor; two panes on iPad) work. Progress by phase is in [docs/PLAN.md § 10](docs/PLAN.md#10-progress); working
rules for contributors and AI assistants are in [CLAUDE.md](CLAUDE.md).

## Building

You need JDK 21, the Android SDK, and Xcode with an iPad simulator (macOS only).

| What | Command |
|---|---|
| All tests and lint | `./gradlew check` |
| Android, on a connected device or emulator | `./gradlew :app:android:installDebug` |
| iPad / iPhone | open `app/ios/Yaad.xcodeproj` in Xcode and run on an iPad simulator |
| Desktop preview (development only) | `./gradlew :app:desktop:run` |

The Xcode project is generated from `app/ios/project.yml`; after editing it, run `xcodegen generate` in `app/ios`.

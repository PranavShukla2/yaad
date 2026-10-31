# Yaad (याद)

A calm, reliable reminder app for Android phones and iPads, written in Kotlin (Kotlin Multiplatform with Compose
Multiplatform). "Yaad" means "remember" in Hindi.

Yaad remembers the things that cost money or trouble when forgotten:

- **Reminders:** one-time and repeating, with snooze.
- **Loans and EMIs:** the full repayment schedule and a reminder before every due date.
- **Bills and subscriptions:** credit-card due dates, insurance premiums and SIPs.
- **Dates that matter:** birthdays, medicines, and the expiry of documents and warranties.

Everything stays on the device unless you choose to back it up.

**Status:** Phases 0–10 are done: the recurrence engine, the on-device database (with migrations and backup) and
reliable notifications on Android (exact alarms, Doze, reboot, time changes) and iPad (a rolling 64-notification
window), with Done / Snooze / Skip, are in place and tested, and the reminder screens (Today, Upcoming, detail,
editor; two panes on iPad) work, and loans show their exact EMI schedule with part-payments, rate changes and EMI
reminders, and the Money tab sums up each month’s bills, cards, subscriptions, insurance and investments. Birthdays,
medicines (nagging until taken), document expiry, warranties and appointments each have a one-tap template, and
reminders can be typed in a line ("rent 15000 on 5th every month", "kal shaam 7 baje call Ravi"), found on a
calendar or searched. Yaad can be locked, backed up to an encrypted file, and used in Hindi. Progress by phase is in
[docs/PLAN.md § 10](docs/PLAN.md#10-progress); working rules for contributors and AI assistants are in
[CLAUDE.md](CLAUDE.md).

## Download

**Android:** [download the latest APK](https://github.com/PranavShukla2/yaad/releases/latest) on your phone, open it,
and allow installing when Android asks. New versions install over old ones and keep your reminders. iPhone and iPad
builds will come through TestFlight.

## Building

You need JDK 21, the Android SDK, and Xcode with an iPad simulator (macOS only).

| What | Command |
|---|---|
| All tests and lint | `./gradlew check` |
| Android, on a connected device or emulator | `./gradlew :app:android:installDebug` |
| iPad / iPhone | open `app/ios/Yaad.xcodeproj` in Xcode and run on an iPad simulator |
| Desktop preview (development only) | `./gradlew :app:desktop:run` |

The Xcode project is generated from `app/ios/project.yml`; after editing it, run `xcodegen generate` in `app/ios`.

**Releases:** pushing a tag such as `v0.10.0` runs `.github/workflows/release.yml`, which tests, builds an APK signed
with the release key (kept only in GitHub's encrypted secrets) and publishes it on the Releases page.

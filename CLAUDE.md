# Yaad — project instructions

Yaad (याद, "remember") is a reminder app for Android phones and iPads, written in Kotlin with Kotlin Multiplatform and
Compose Multiplatform. It covers reminders, loans and EMIs, bills, subscriptions, credit cards, insurance, medicines,
birthdays and document expiry.
The full roadmap is `docs/PLAN.md`; read the section for the current phase before starting work.

## Product, in one paragraph
A calm, trustworthy app that never misses a reminder. You add something in seconds, by typing or by quick-add
("rent 15000 on 5th every month"), and Yaad notifies you on time, even after a reboot, in Doze, or with the app
closed. Money items show what is due and when, and EMI loans show their full schedule and outstanding balance.
Everything works offline and stays on the device; backup is opt-in.

## Non-negotiable rules
1. **A reminder must never be missed or duplicated.** Notification scheduling is the core of the app and is covered by
   tests:
   - every recurrence rule is tested;
   - edge cases are tested: the 31st, 29 February, month ends, time-zone changes;
   - reboot, app update and time-change handling is tested, and the iOS 64-pending limit is handled.
2. **Money maths is exact.** Amounts are stored as `Long` paise (or minor units), never `Double`. EMI, interest and
   amortisation code lives in `core:finance`, is tested against worked examples (bank EMI calculators, RBI-style
   reducing balance), and rounds the way banks do (to the paisa; the last instalment absorbs the difference).
3. **Time is explicit.** Use `kotlinx-datetime`; store instants in UTC plus the user's IANA time zone; never hand-roll
   offsets. A reminder "every day at 9:00" means 9:00 local time, even after travel or a DST change.
4. **Every number and every next-due date on screen comes from a tested function** in a `core:*` module. UI code never
   computes schedules or money.
5. **Privacy first.** No accounts, no analytics, no ads, and no network access in the core app. Optional backup is an
   explicit export or the user's own cloud. Never log reminder contents.
6. **Licences:** only permissive dependencies (Apache-2.0, MIT, BSD; OFL for fonts). Check before adding one.
7. **No secrets in git:** keystores, `google-services.json`, `GoogleService-Info.plist`, `.env`, signing files.
   Example config files use obviously fake values.

## Git workflow
- **Commit very often:**
  - Each commit is small, about 20–200 lines, and does one logical thing.
  - It must build and pass the tests.
  - Push often.
- **Commit messages:** Conventional Commits, `type(scope): summary`. For example,
  `feat(recurrence): support "last day of the month"` or `fix(alarms): reschedule after reboot`.
- **No AI attribution anywhere:**
  - no `Co-Authored-By: Claude` (or any AI) trailers;
  - no "Generated with Claude Code" lines;
  - no AI mentions in commit messages, PR titles or PR bodies.
  The owner is the only author and contributor.
- **Author:** commits are authored as `PranavShukla2 <157887524+PranavShukla2@users.noreply.github.com>`. Never change
  the git author or config.
- **Before every commit:** run `git status` and `git diff --staged`, and unstage anything outside the intended change.
- **Branches and pull requests:**
  - One branch per phase (`phase/01-core-reminders`).
  - Open a PR when the phase's acceptance checks pass, review its diff, and wait for CI to go green.
  - Merge it yourself with a **merge commit** (`gh pr merge --merge`, never squash or rebase), so every small commit
    lands on `main`.
  - Summarise the phase in the PR, then carry on with the next phase without waiting for approval.
- Never force-push to `main`. Never rewrite published history.

## Architecture
- Kotlin 2.x, Compose Multiplatform, Gradle version catalogs (`gradle/libs.versions.toml`), JDK 21 toolchain.
- Targets: **Android** (phones, tablets) and **iOS / iPadOS** (iPad first-class: adaptive two-pane layouts, keyboard
  shortcuts, Split View). A JVM `desktop` target is kept only for fast UI tests and previews.
- Modules:
  - `core:model`: entities and value types (Money, Recurrence, Reminder, Loan...).
  - `core:recurrence`: the next-occurrence engine; pure Kotlin, exhaustively tested.
  - `core:finance`: EMI, amortisation, prepayment, interest, money formatting.
  - `core:scheduling`: decides what to schedule and when; a rolling window for iOS.
  - `core:quickadd`: the natural-language parser for quick-add.
  - `data`: SQLDelight database, migrations, backup and export.
  - `platform:notifications`: Android AlarmManager / WorkManager and iOS UNUserNotificationCenter behind one
    `expect`/`actual` interface.
  - `ui:designsystem`, `feature:*` (today, reminders, loans, bills, settings...).
  - `app:android`, `app:ios`, `app:desktop`.
- `core:*` modules are `commonMain`-only and depend on nothing UI- or platform-specific.
- Tests mirror the package layout. Every bug fix starts with a failing test.

## Design language
- Material 3 on Android with dynamic colour, and the same layout on iPad that feels native: large touch targets,
  two-pane list/detail at widths ≥ 600 dp.
- Calm and legible: a clear "Today / Upcoming / Overdue" hierarchy, money in tabular figures, ₹ formatting with Indian
  digit grouping (1,23,456.00) by default, and the currency configurable.
- Light and dark themes; every screen supports TalkBack / VoiceOver and font scaling to 200%.
- English first; strings in resources so Hindi and other languages can be added (Phase 9).

## Commands (fill in as they become real)
- `./gradlew check`: all tests and lint
- `./gradlew :app:android:installDebug`: install on a connected device or emulator
- iOS / iPad: open `app/ios/Yaad.xcodeproj` in Xcode and run on an iPad simulator
- `./gradlew :app:desktop:run`: quick desktop preview (development only)

## When unsure
Prefer the boring, reliable option. If a platform limit makes a promise impossible (e.g. exact alarms denied by the
user), say so in the UI and in `docs/PLAN.md` rather than silently degrading.

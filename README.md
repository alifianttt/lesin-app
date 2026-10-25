# Lesin

Lesin is a local-first private tutor assistant built with Kotlin Compose Multiplatform for Android and iOS.

## Product flow

```text
Schedule -> Attendance -> Session Report -> Auto Billing -> Payment Tracking
```

Student management, schedule creation, today's schedule dashboard, session attendance, session reports, session history, automatic billing, and invoice list/detail screens are implemented. Manual payment recording remains for a later iteration.

## Base architecture

```text
Compose UI
    -> ViewModel + StateFlow
    -> Use Case
    -> Repository Interface
    -> Repository Implementation
    -> Local Data Source
    -> SQLDelight
```

The codebase follows:

- Clean Architecture
- Feature-first package organization
- Immutable StateFlow UI state
- Kotlin Coroutines + Flow
- Koin dependency injection
- SQLDelight local persistence
- DTO -> Mapper -> Domain separation
- Focused utilities and design-system tokens

## Package

```text
id.lesin.app
```

## Current foundation

The Student feature follows the architecture from local persistence through to Compose screens:

```text
StudentDto
-> Student mapper
-> Student domain model
-> StudentLocalDataSource
-> StudentRepository
-> Student use cases and domain validation
-> List / detail / form ViewModels + StateFlow
-> Compose screens and typed navigation
```

Open **Students** from the bottom navigation to search by name or subject, add a student, or view their details. The detail screen supports editing and deletion with confirmation. The form stores the student's name, subject, optional parent contacts, price in whole rupiah, and billing interval locally with SQLDelight.

The screens follow `ui_lesin.pdf`, using compact rows, gray inputs, and black primary actions. Student Detail shows completed unbilled sessions against the configured billing interval.

From **Student Detail**, choose **Create schedule** to open a form with the student selected and their subject and rate prefilled. Choose a date and start time, optionally set an end time later on the same day, and save. Subject and price can be changed for that schedule without changing the student. Saved schedules appear on Student Detail in chronological order and remain available offline. Dates and times use the device time zone and are stored as epoch milliseconds. The database migrates existing student data automatically; deleting a student also deletes their schedules.

## Sessions and attendance

Home shows actual schedules for the device's current date, their expected income, and any unfinished sessions from earlier days. Choose **Start Session** on Home or **Start session** under a schedule in Student Detail. Starting records attendance with the actual start timestamp, copies the scheduled subject and price, and marks the schedule in progress. **Resume Session** reopens the existing session without creating another attendance record.

Enter the topic covered, choose the student's progress, and optionally add notes and homework. **Complete session** saves the report and marks both session and schedule completed in one transaction, then checks the student's billing interval. Reports are available read-only under **Recent sessions** in Student Detail.

Active sessions and completed reports persist locally across app restarts. Report edits are saved on completion; leaving an edited report asks for confirmation before discarding those edits. The database migration preserves existing students and schedules.

## Automatic billing and invoices

When a student reaches their billing interval, Lesin invoices the oldest completed, unbilled sessions. For an interval of four, seven unbilled sessions produce one invoice for the first four and leave three unbilled. Each item uses the session's saved price, subject, topic, and attendance date. Changing a student's rate or name does not alter existing invoices.

Invoice creation, item creation, invoice numbering, and marking sessions **BILLED** share one database transaction. Unique session references prevent duplicate billing. Invoice numbers follow `INV-{YEAR}-{SEQUENCE}`, with a separate persistent sequence per year. All new invoices start **UNPAID**; creating an invoice never records a payment.

Open **Billing** from the bottom navigation to filter by All, Unpaid, or Paid. Select an invoice to view its sessions, total, and original reports. Billing automatically checks previously completed sessions and processes all full pending intervals. **Check pending billing** can retry after a storage error. If billing fails after session completion, the saved report remains completed and Lesin shows a message directing the tutor to Billing.

Students, schedules, and sessions referenced by invoices cannot be deleted, preserving billing history. Students without invoices can still be deleted with confirmation. Existing data migrates locally without removing students, schedules, or reports.

## Project structure

```text
shared/src/commonMain/kotlin/id/lesin/app
├── core
│   ├── database
│   ├── designsystem
│   ├── di
│   └── util
└── feature
    ├── student
    │   ├── data
    │   │   ├── datasource
    │   │   ├── dto
    │   │   ├── mapper
    │   │   └── repository
    │   ├── domain
    │   │   ├── model
    │   │   ├── repository
    │   │   └── usecase
    │   └── presentation
    │       ├── list
    │       ├── detail
    │       └── form
    ├── schedule
    │   ├── data
    │   ├── domain
    │   └── presentation/form
    ├── session
    │   ├── data
    │   ├── domain
    │   └── presentation
    └── billing
        ├── data
        ├── domain
        └── presentation
            ├── list
            └── detail
```

Future features should follow the same structure:

```text
feature/payment
```

Do not create those feature implementations until they are being actively developed.

## Tech stack

- Kotlin Multiplatform
- Compose Multiplatform
- Android + iOS
- ViewModel + StateFlow
- Kotlin Coroutines / Flow
- Koin
- SQLDelight
- kotlinx-datetime

## Run

Android:

```bash
./gradlew :androidApp:assembleDebug
```

iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run the iOS target.

Student, schedule, session, billing, and Home regression tests, including SQLite migration, duplicate billing, invoice persistence, and transaction rollback tests:

```bash
./gradlew :shared:testAndroidHostTest
```

## Documentation

- `AGENTS.md` - architecture and coding rules for AI coding agents
- `PRD.md` - detailed product behavior and flows
- `ui_lesin.pdf` - mobile screen design reference
- `docs/PRD.md` - initial MVP scope and base-project boundary

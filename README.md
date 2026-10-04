# Lesin

Lesin is a local-first private tutor assistant built with Kotlin Compose Multiplatform for Android and iOS.

## Product flow

```text
Schedule -> Attendance -> Session Report -> Auto Billing -> Payment Tracking
```

The Student feature is implemented. Schedule, session, billing, and payment flows remain for later iterations.

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

The screens follow `ui_lesin.pdf`, using compact rows, gray inputs, and black primary actions. Billing intervals are shown until session data is available for calculating live billing progress.

## Project structure

```text
shared/src/commonMain/kotlin/id/lesin/app
├── core
│   ├── database
│   ├── designsystem
│   ├── di
│   └── util
└── feature
    └── student
        ├── data
        │   ├── datasource
        │   ├── dto
        │   ├── mapper
        │   └── repository
        ├── domain
        │   ├── model
        │   ├── repository
        │   └── usecase
        └── presentation
            ├── list
            ├── detail
            └── form
```

Future features should follow the same structure:

```text
feature/schedule
feature/session
feature/billing
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

Student business rules and ViewModel regression tests:

```bash
./gradlew :shared:testAndroidHostTest
```

## Documentation

- `AGENTS.md` - architecture and coding rules for AI coding agents
- `PRD.md` - detailed product behavior and flows
- `ui_lesin.pdf` - mobile screen design reference
- `docs/PRD.md` - initial MVP scope and base-project boundary

# Lesin

Lesin is a local-first private tutor assistant built with Kotlin Compose Multiplatform for Android and iOS.

## Product flow

```text
Schedule -> Attendance -> Session Report -> Auto Billing -> Payment Tracking
```

This repository is currently a **base project**, not a complete MVP implementation. Business screens and complete feature flows are intentionally left for later iterations.

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

The base project includes one minimal `student` vertical slice to demonstrate the architecture without building the full Student UI:

```text
StudentDto
-> Student mapper
-> Student domain model
-> StudentLocalDataSource
-> StudentRepository
-> GetStudentsUseCase / CreateStudentUseCase
-> StudentListViewModel + StateFlow
```

The initial SQLDelight `student_entity` table is included so local persistence and repository patterns are ready for future features.

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
            └── list
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

## Documentation

- `AGENT.md` - architecture and coding rules for AI coding agents
- `docs/PRD.md` - MVP product scope and base-project boundary

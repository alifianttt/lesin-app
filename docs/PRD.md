# Lesin MVP PRD

## Product

Lesin is a local-first private tutor assistant for managing tutoring sessions and payment tracking without a backend.

## Core flow

```text
Schedule -> Attendance -> Session Report -> Auto Billing -> Payment Tracking
```

## MVP scope

- Student management
- Schedule management
- Start and complete tutoring sessions
- Session report: topic, progress, notes, homework
- Automatic invoice generation after a configured number of completed, unbilled sessions
- Manual payment recording
- Local-only persistence

## Out of scope for the base project

The project implements student management, schedule creation and per-student viewing, today's schedule dashboard, session start/resume/completion, reports, session history, automatic billing, invoice list/detail screens, and student billing progress. Attendance is recorded when a session starts. Completing a session checks the student's billing interval and invoices eligible completed sessions atomically. Opening Billing recovers pending full intervals, including sessions completed before billing was available. Invoice items preserve session prices and reference their reports; invoiced records cannot be deleted.

Not included now:

- Schedule editing, rescheduling, and cancellation
- Receipt UI
- Payment UI
- Backend/API
- Authentication
- Firebase
- GPS
- Cloud sync
- Payment gateway
- Parent application

## Base project goal

The initial codebase should establish the architecture and conventions required by future features:

- Compose Multiplatform
- ViewModel + StateFlow
- Kotlin Coroutines + Flow
- Clean Architecture
- Feature-first packages
- DTO -> mapper -> domain separation
- Repository contracts in domain
- Repository implementations in data
- Use cases for business actions
- Koin dependency injection
- SQLDelight local database
- Shared design system and utilities

## Business rules to implement later

- Only completed sessions can be billed.
- Cancelled sessions must never be billed.
- A billed session cannot be included in another invoice.
- Invoice creation and marking sessions as billed must be atomic.
- Recording payment and marking an invoice paid must be atomic.

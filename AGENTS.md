# AGENT.md

## Project Overview

**Product Name: Lesin**

Lesin is a **Private Tutor Attendance & Billing** application built with **Kotlin Compose Multiplatform**.

The application is **local-first / offline-only** for the MVP.

Main business flow:

```text
Student
  ↓
Schedule
  ↓
Start Session
  ↓
Attendance
  ↓
Session Report
  ↓
Complete Session
  ↓
Check Billing Rule
  ↓
Generate Invoice Automatically
  ↓
Parent Pays
  ↓
Tutor Marks Invoice as Paid
```

The application does **not** require:

- Backend API
- Firebase
- Authentication server
- GPS
- Cloud database
- Payment gateway

All persistent data is stored locally.

---

# 0. Product Identity

Product name:

```text
Lesin
```

Use **Lesin** consistently in:

- App title
- User-facing product references
- Documentation
- Generated UI copy when the product name is needed
- Example package/project naming where appropriate

Do not rename the product to generic alternatives such as:

```text
Private Tutor
Tutor App
TutorFlow
Tutora
```

unless explicitly requested.

Recommended application naming:

```text
App Name:
Lesin

Suggested package namespace:
id.lesin.app
```

The product positioning is:

```text
A simple local-first app for private tutors to manage
schedule, attendance, session reports, billing, and payments.
```

Core flow:

```text
Schedule
  ↓
Attendance
  ↓
Session Report
  ↓
Auto Billing
  ↓
Payment Tracking
```

---

# 1. Core Engineering Principles

All generated and modified code must follow these principles:

1. Clean Architecture
2. Separation of concerns
3. Single responsibility
4. Dependency inversion
5. Unidirectional data flow
6. Immutable UI state
7. Explicit state transitions
8. Domain-driven business rules
9. Testable business logic
10. Small, focused classes and functions
11. No business logic inside Composables
12. No direct database access from presentation layer
13. No DTO or database entity exposed to UI
14. Avoid unnecessary abstraction
15. Prefer readable code over clever code

Do not introduce new architectural patterns unless they solve a concrete problem.

---

# 2. Architecture

Use this dependency direction:

```text
Presentation
    ↓
Domain
    ↓
Data
```

Detailed flow:

```text
Compose UI
    ↓
ViewModel
    ↓
UseCase
    ↓
Repository Interface
    ↓
Repository Implementation
    ↓
Local Data Source
    ↓
SQLDelight / SQLite
```

Dependencies must always point inward.

The `domain` layer must not depend on:

- Compose
- SQLDelight
- Android APIs
- iOS APIs
- Database generated models
- DTOs
- Presentation models

---

# 3. Recommended Package Structure

Use feature-first organization.

```text
com.yourapp
│
├── core
│   ├── common
│   ├── database
│   ├── di
│   ├── designsystem
│   ├── extension
│   ├── model
│   ├── util
│   └── validation
│
├── feature
│   ├── student
│   │   ├── data
│   │   │   ├── datasource
│   │   │   ├── dto
│   │   │   ├── mapper
│   │   │   └── repository
│   │   │
│   │   ├── domain
│   │   │   ├── model
│   │   │   ├── repository
│   │   │   └── usecase
│   │   │
│   │   └── presentation
│   │       ├── list
│   │       ├── detail
│   │       └── form
│   │
│   ├── schedule
│   ├── session
│   ├── billing
│   └── payment
│
└── navigation
```

Do not create folders that contain only one file unless separation materially improves clarity.

---

# 4. Layer Responsibilities

## Presentation

Responsible for:

- Rendering UI
- Receiving user interactions
- Displaying state
- Sending events to ViewModel
- Navigation callbacks

Presentation must not contain:

- Database queries
- Billing rules
- Repository implementation details
- DTO mapping
- Business calculations
- SQLDelight generated types

---

## Domain

Responsible for:

- Business models
- Business rules
- Repository contracts
- Use cases
- Domain-specific validation

The domain layer is the source of truth for application behavior.

Examples:

```text
StartSessionUseCase
CompleteSessionUseCase
CheckBillingRuleUseCase
GenerateInvoiceUseCase
RecordPaymentUseCase
```

---

## Data

Responsible for:

- SQLDelight access
- DTO/database models
- Mapping DTO/database entities to domain
- Repository implementations
- Local data sources
- Transactions

Data layer must implement interfaces defined in the domain layer.

---

# 5. State Management

Use:

- `ViewModel`
- `StateFlow`
- Immutable `UiState`
- `SharedFlow` or `Channel` only for one-time UI effects when necessary

Preferred pattern:

```kotlin
data class StudentUiState(
    val isLoading: Boolean = false,
    val students: List<Student> = emptyList(),
    val error: String? = null
)
```

ViewModel:

```kotlin
class StudentViewModel(
    private val getStudentsUseCase: GetStudentsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(StudentUiState())
    val state: StateFlow<StudentUiState> = _state.asStateFlow()

    init {
        observeStudents()
    }

    private fun observeStudents() {
        viewModelScope.launch {
            getStudentsUseCase()
                .collect { students ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            students = students,
                            error = null
                        )
                    }
                }
        }
    }
}
```

Never expose `MutableStateFlow` publicly.

Bad:

```kotlin
val state = MutableStateFlow(...)
```

Good:

```kotlin
private val _state = MutableStateFlow(...)
val state = _state.asStateFlow()
```

---

# 6. UI Events

User actions should be modeled explicitly.

Example:

```kotlin
sealed interface StudentFormEvent {

    data class NameChanged(
        val value: String
    ) : StudentFormEvent

    data class ParentNameChanged(
        val value: String
    ) : StudentFormEvent

    data class ParentPhoneChanged(
        val value: String
    ) : StudentFormEvent

    data class SubjectChanged(
        val value: String
    ) : StudentFormEvent

    data class PriceChanged(
        val value: String
    ) : StudentFormEvent

    data object SaveClicked : StudentFormEvent
}
```

ViewModel:

```kotlin
fun onEvent(event: StudentFormEvent) {
    when (event) {
        is StudentFormEvent.NameChanged -> {
            _state.update {
                it.copy(name = event.value)
            }
        }

        StudentFormEvent.SaveClicked -> {
            saveStudent()
        }

        else -> Unit
    }
}
```

Avoid passing many setter lambdas from a ViewModel into the UI.

Prefer:

```kotlin
onEvent(StudentFormEvent.NameChanged(value))
```

---

# 7. One-Time Effects

Use effects for events such as:

- Navigate back
- Show snackbar
- Open invoice
- Show confirmation message

Example:

```kotlin
sealed interface StudentFormEffect {
    data object Saved : StudentFormEffect
    data class ShowError(val message: String) : StudentFormEffect
}
```

Implementation may use:

```kotlin
private val _effect = MutableSharedFlow<StudentFormEffect>()
val effect = _effect.asSharedFlow()
```

Do not store one-time navigation events permanently inside `UiState`.

---

# 8. Compose Rules

Composable functions must primarily:

1. Observe state
2. Render state
3. Forward events

Example:

```kotlin
@Composable
fun StudentScreen(
    viewModel: StudentViewModel,
    onStudentClick: (Long) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StudentContent(
        state = state,
        onEvent = viewModel::onEvent,
        onStudentClick = onStudentClick
    )
}
```

Prefer separating route/screen from content:

```text
StudentScreen
    ↓
StudentContent
```

`StudentContent` should be previewable when possible.

Do not perform repository or database operations inside Composables.

Do not put billing logic inside Composables.

---

# 9. DTO Rules

DTOs belong to the data layer.

Even though the MVP has no backend, local database records or raw persistence models should be treated like data-layer DTOs.

Example:

```kotlin
data class StudentDto(
    val id: Long,
    val name: String,
    val parentName: String?,
    val parentPhone: String?,
    val subject: String,
    val pricePerSession: Long,
    val billingInterval: Int,
    val createdAt: Long
)
```

DTOs:

- Reflect persistence or external data
- May use primitive values
- May contain nullable fields
- Must not be exposed directly to UI

Do not place business behavior inside DTO classes.

---

# 10. Domain Models

Domain models represent business concepts.

Example:

```kotlin
data class Student(
    val id: Long,
    val name: String,
    val parentName: String?,
    val parentPhone: String?,
    val subject: String,
    val pricePerSession: Long,
    val billingInterval: Int,
    val createdAt: Long
)
```

Domain models should:

- Be framework-independent
- Be understandable without knowing database structure
- Represent business meaning
- Use domain enums where appropriate

---

# 11. Mapper Rules

Mapping must be explicit.

Example:

```kotlin
fun StudentDto.toDomain(): Student {
    return Student(
        id = id,
        name = name,
        parentName = parentName,
        parentPhone = parentPhone,
        subject = subject,
        pricePerSession = pricePerSession,
        billingInterval = billingInterval,
        createdAt = createdAt
    )
}
```

If needed:

```kotlin
fun Student.toDto(): StudentDto {
    return StudentDto(
        id = id,
        name = name,
        parentName = parentName,
        parentPhone = parentPhone,
        subject = subject,
        pricePerSession = pricePerSession,
        billingInterval = billingInterval,
        createdAt = createdAt
    )
}
```

Do not write mapping code repeatedly inside repositories.

Place mapper extensions in:

```text
data/mapper
```

---

# 12. Repository Pattern

Repository interfaces belong to the domain layer.

Example:

```kotlin
interface StudentRepository {

    fun observeStudents(): Flow<List<Student>>

    suspend fun getStudentById(
        id: Long
    ): Student?

    suspend fun createStudent(
        student: Student
    )

    suspend fun updateStudent(
        student: Student
    )

    suspend fun deleteStudent(
        id: Long
    )
}
```

Implementation belongs to data layer.

```kotlin
class StudentRepositoryImpl(
    private val localDataSource: StudentLocalDataSource
) : StudentRepository {

    override fun observeStudents(): Flow<List<Student>> {
        return localDataSource
            .observeStudents()
            .map { students ->
                students.map(StudentDto::toDomain)
            }
    }
}
```

Presentation must depend only on repository interfaces indirectly through use cases.

ViewModels should normally not depend directly on repository implementations.

---

# 13. Local Data Source

Database-specific access belongs in a local data source.

Example:

```kotlin
interface StudentLocalDataSource {

    fun observeStudents(): Flow<List<StudentDto>>

    suspend fun getStudentById(
        id: Long
    ): StudentDto?

    suspend fun insertStudent(
        student: StudentDto
    )

    suspend fun updateStudent(
        student: StudentDto
    )

    suspend fun deleteStudent(
        id: Long
    )
}
```

Implementation may use SQLDelight-generated queries.

Do not leak generated SQLDelight models outside the data source.

---

# 14. Use Case Rules

Each use case should represent one meaningful business operation.

Examples:

```text
GetStudentsUseCase
CreateStudentUseCase
UpdateStudentUseCase

CreateScheduleUseCase
StartSessionUseCase
CompleteSessionUseCase

CheckBillingRuleUseCase
GenerateInvoiceUseCase

RecordPaymentUseCase
```

Use cases should not simply duplicate repository methods unless they provide architectural consistency or expected future business logic.

Good:

```kotlin
class CompleteSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val scheduleRepository: ScheduleRepository,
    private val checkBillingRuleUseCase: CheckBillingRuleUseCase
) {

    suspend operator fun invoke(
        sessionId: Long,
        report: SessionReport
    ) {
        sessionRepository.completeSession(
            sessionId = sessionId,
            report = report
        )

        checkBillingRuleUseCase(
            studentId = report.studentId
        )
    }
}
```

Keep use cases focused.

Avoid giant use cases handling unrelated concerns.

---

# 15. Business Rules

## Session

Valid session lifecycle:

```text
SCHEDULED
    ↓
IN_PROGRESS
    ↓
COMPLETED
```

Optional cancellation:

```text
SCHEDULED
    ↓
CANCELLED
```

Only completed sessions may be billed.

---

## Billing

Session billing states:

```text
UNBILLED
    ↓
BILLED
```

Rules:

1. Only `COMPLETED` sessions can be invoiced.
2. `CANCELLED` sessions must never be invoiced.
3. A `BILLED` session must never be added to another invoice.
4. Auto billing runs after session completion.
5. Invoice creation occurs only when the configured billing interval is reached.
6. Invoice creation and marking sessions as billed must happen in one database transaction.

Example:

```text
billingInterval = 4

Completed unbilled sessions:
1
2
3
4

→ Generate Invoice
→ Mark sessions 1-4 as BILLED
```

If there are 7 unbilled sessions:

```text
Invoice:
1
2
3
4

Remain UNBILLED:
5
6
7
```

---

# 16. Invoice Rules

Invoice states:

```text
UNPAID
    ↓
PAID
```

Do not infer payment simply because an invoice exists.

Invoice contains invoice items.

Each invoice item must reference the corresponding session.

Example:

```text
Invoice
├── Session 1
├── Session 2
├── Session 3
└── Session 4
```

---

# 17. Payment Rules

Payment recording is manual in MVP.

Supported methods:

```kotlin
enum class PaymentMethod {
    BANK_TRANSFER,
    CASH,
    QRIS,
    OTHER
}
```

Recording payment should:

1. Insert payment record
2. Update invoice status to `PAID`
3. Set `paidAt`

Perform these operations within one transaction when possible.

---

# 18. Dependency Injection

Use dependency injection.

Preferred library:

```text
Koin
```

Modules should be grouped by responsibility.

Example:

```kotlin
val studentModule = module {

    single<StudentLocalDataSource> {
        StudentLocalDataSourceImpl(
            database = get()
        )
    }

    single<StudentRepository> {
        StudentRepositoryImpl(
            localDataSource = get()
        )
    }

    factory {
        GetStudentsUseCase(
            repository = get()
        )
    }

    factory {
        CreateStudentUseCase(
            repository = get()
        )
    }

    viewModel {
        StudentViewModel(
            getStudentsUseCase = get()
        )
    }
}
```

Prefer constructor injection.

Do not use service locator calls such as `get()` inside business classes.

Bad:

```kotlin
class StudentViewModel : ViewModel() {
    private val repository = get<StudentRepository>()
}
```

Good:

```kotlin
class StudentViewModel(
    private val getStudentsUseCase: GetStudentsUseCase
) : ViewModel()
```

---

# 19. Core Utils

Reusable generic helpers should go under:

```text
core/util
```

Examples:

```text
DateTimeUtils
CurrencyFormatter
InvoiceNumberGenerator
Result
DispatcherProvider
Logger
```

Do not create a generic `Utils.kt` dumping ground.

Bad:

```text
Utils.kt
```

Good:

```text
CurrencyFormatter.kt
DateTimeFormatter.kt
InvoiceNumberGenerator.kt
```

Each utility must have one clear responsibility.

---

# 20. Extensions

Extensions belong in:

```text
core/extension
```

Example:

```kotlin
fun Long.toRupiah(): String
```

or:

```kotlin
fun Instant.toDisplayDate(): String
```

Do not create extensions when a normal function is clearer.

Do not hide complex business logic inside extensions.

---

# 21. Result Handling

Use an application result abstraction only when it adds value.

Example:

```kotlin
sealed interface AppResult<out T> {

    data class Success<T>(
        val data: T
    ) : AppResult<T>

    data class Error(
        val throwable: Throwable
    ) : AppResult<Nothing>
}
```

Do not wrap every local synchronous calculation in `Result`.

Prefer exceptions or explicit validation results where appropriate.

---

# 22. Validation

Validation belongs in domain/use case or dedicated validation classes.

Example:

```kotlin
class ValidateStudentUseCase {

    operator fun invoke(
        name: String,
        subject: String,
        price: Long?,
        billingInterval: Int?
    ): StudentValidationResult {

        if (name.isBlank()) {
            return StudentValidationResult.InvalidName
        }

        if (subject.isBlank()) {
            return StudentValidationResult.InvalidSubject
        }

        if (price == null || price <= 0) {
            return StudentValidationResult.InvalidPrice
        }

        if (billingInterval == null || billingInterval <= 0) {
            return StudentValidationResult.InvalidBillingInterval
        }

        return StudentValidationResult.Valid
    }
}
```

Do not put complex validation directly in Composables.

---

# 23. Date and Time

Use Kotlin Multiplatform-compatible date/time APIs.

Prefer:

```text
kotlinx-datetime
```

Store persistent timestamps using a stable representation such as epoch milliseconds.

Do not persist formatted display strings such as:

```text
"27 September 2026"
```

Persist:

```text
epochMillis
```

Format only in presentation.

---

# 24. Currency

Store currency as integer values.

Example:

```kotlin
val price: Long = 150_000L
```

Do not use:

```kotlin
Double
```

for money.

Bad:

```kotlin
150000.0
```

Good:

```kotlin
150_000L
```

UI formatting belongs in formatter utilities.

---

# 25. IDs

Local database IDs may use:

```kotlin
Long
```

Do not expose SQLDelight row objects as IDs or domain models.

Invoice display number should be separate from database ID.

Example:

```text
Database ID:
42

Invoice Number:
INV-2026-0004
```

---

# 26. Database Transactions

Use transactions for operations that must succeed atomically.

Mandatory examples:

## Invoice Creation

```text
Create Invoice
+
Create Invoice Items
+
Mark Sessions BILLED
```

## Payment Recording

```text
Create Payment
+
Mark Invoice PAID
+
Set paidAt
```

Never allow partially completed billing state.

---

# 27. Clean Code Rules

Functions should normally:

- Do one thing
- Have descriptive names
- Avoid deep nesting
- Avoid unnecessary comments
- Avoid boolean parameter ambiguity
- Prefer early returns

Bad:

```kotlin
fun process(
    data: Student,
    shouldSave: Boolean,
    shouldUpdate: Boolean
)
```

Prefer explicit operations:

```kotlin
createStudent()
updateStudent()
```

---

# 28. Naming

Use descriptive names.

Bad:

```kotlin
val data
val item
val temp
val obj
```

Good:

```kotlin
val student
val invoice
val unbilledSessions
val completedSession
```

Boolean names should read naturally:

```kotlin
isLoading
isPaid
isSessionActive
canCompleteSession
```

---

# 29. Avoid Magic Values

Bad:

```kotlin
if (sessions.size >= 4)
```

Good:

```kotlin
if (sessions.size >= student.billingInterval)
```

Bad:

```kotlin
status == "paid"
```

Good:

```kotlin
status == InvoiceStatus.PAID
```

---

# 30. Enums

Use enums for bounded domain states.

Examples:

```kotlin
enum class ScheduleStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
```

```kotlin
enum class SessionBillingStatus {
    UNBILLED,
    BILLED
}
```

```kotlin
enum class InvoiceStatus {
    UNPAID,
    PAID
}
```

Avoid raw strings throughout the domain and presentation layers.

---

# 31. Error Handling

Errors must be handled intentionally.

Do not silently swallow exceptions.

Bad:

```kotlin
try {
    repository.save()
} catch (_: Exception) {
}
```

Preferred:

```kotlin
runCatching {
    createStudentUseCase(...)
}.onSuccess {
    ...
}.onFailure { throwable ->
    _state.update {
        it.copy(
            isSaving = false,
            error = throwable.message ?: "Unknown error"
        )
    }
}
```

For production-quality code, map technical errors into user-friendly domain/UI errors where appropriate.

---

# 32. Coroutines

Use structured concurrency.

ViewModels:

```kotlin
viewModelScope.launch {
    ...
}
```

Repositories and use cases should expose `suspend` functions or `Flow`.

Do not create unmanaged `CoroutineScope`.

Bad:

```kotlin
CoroutineScope(Dispatchers.IO).launch {
}
```

unless lifecycle ownership is explicit and justified.

---

# 33. Flow

Use `Flow` for observable streams from the database.

Example:

```kotlin
fun observeStudents(): Flow<List<Student>>
```

Use `StateFlow` for presentation state.

Typical path:

```text
SQLDelight Query
    ↓
Flow<List<StudentDto>>
    ↓
Repository Mapper
    ↓
Flow<List<Student>>
    ↓
UseCase
    ↓
ViewModel
    ↓
StateFlow<StudentUiState>
    ↓
Compose
```

Do not expose database flows directly to Composables.

---

# 34. Flow Operators

Use flow operators intentionally.

Common operators:

```text
map
combine
distinctUntilChanged
filter
stateIn
```

Avoid chains that are difficult to understand.

Prefer extracting complex transformations into named functions.

---

# 35. ViewModel Rules

A ViewModel may:

- Hold UI state
- Handle presentation events
- Call use cases
- Coordinate screen-level operations
- Transform domain data into UI state

A ViewModel must not:

- Execute SQL
- Access SQLDelight queries
- Format raw SQL rows
- Contain billing algorithms
- Know repository implementation classes
- Contain platform-specific Android/iOS logic unless through abstraction

---

# 36. UI Models

Create presentation models only when domain models are not suitable for direct display.

Example:

```kotlin
data class InvoiceUiModel(
    val id: Long,
    val invoiceNumber: String,
    val studentName: String,
    val formattedTotal: String,
    val statusLabel: String
)
```

Do not duplicate models unnecessarily.

If a domain model is already appropriate, use it directly in presentation state.

---

# 37. Screen State Example

```kotlin
data class SessionUiState(
    val isLoading: Boolean = false,
    val sessionId: Long? = null,
    val studentName: String = "",
    val subject: String = "",
    val startedAt: Long? = null,
    val topic: String = "",
    val notes: String = "",
    val homework: String = "",
    val progress: StudentProgress? = null,
    val isCompleting: Boolean = false,
    val canCompleteSession: Boolean = false,
    val error: String? = null
)
```

State must be immutable.

Update using:

```kotlin
_state.update {
    it.copy(...)
}
```

---

# 38. Business Logic Location

Examples:

## Presentation

```text
User clicked Complete Session
```

## ViewModel

```text
Call CompleteSessionUseCase
```

## Use Case

```text
Complete Session
→ Change schedule state
→ Check billing
```

## Repository

```text
Persist data
```

## Local Data Source

```text
Execute SQLDelight query
```

Do not move these responsibilities between layers.

---

# 39. Feature Dependencies

Features should not directly depend on another feature's presentation layer.

Bad:

```text
billing/presentation
    ↓
session/presentation
```

Use shared domain contracts or core abstractions instead.

Cross-feature business logic belongs in domain use cases.

---

# 40. Navigation

Navigation must remain outside domain logic.

Use typed routes where possible.

Example:

```kotlin
sealed interface Route {

    data object Home : Route

    data object Students : Route

    data class StudentDetail(
        val studentId: Long
    ) : Route

    data class Session(
        val scheduleId: Long
    ) : Route

    data object Billing : Route

    data class InvoiceDetail(
        val invoiceId: Long
    ) : Route
}
```

ViewModels should generally emit navigation effects rather than directly controlling a navigator implementation.

---

# 41. Dependency Rule

Allowed:

```text
presentation → domain
data → domain
```

Not allowed:

```text
domain → presentation
domain → data
```

Data implementations can depend on domain interfaces/models.

Domain must remain independent.

---

# 42. Testing Strategy

Prioritize tests for business rules.

Must test:

```text
CheckBillingRuleUseCase
GenerateInvoiceUseCase
CompleteSessionUseCase
RecordPaymentUseCase
```

Example cases:

```text
3 sessions + interval 4
→ no invoice

4 sessions + interval 4
→ invoice generated

7 sessions + interval 4
→ first 4 billed
→ 3 remain unbilled

cancelled session
→ never billed

already billed session
→ never reused

payment recorded
→ invoice becomes PAID
```

ViewModel tests should verify:

- Initial state
- Loading state
- Success state
- Error state
- Events
- Effects

---

# 43. No Premature Abstraction

Do not create:

```text
BaseRepository
BaseUseCase
BaseViewModel
BaseDataSource
```

unless there is proven duplicated behavior that benefits from abstraction.

Prefer composition.

---

# 44. No God Classes

Avoid classes like:

```text
TutorManager
AppManager
DatabaseManager
BillingManager
```

that contain unrelated logic.

Split responsibilities into focused repositories and use cases.

---

# 45. No Generic Utils Dump

Never create:

```text
Utils.kt
Helpers.kt
CommonUtils.kt
```

with unrelated methods.

Use focused names:

```text
CurrencyFormatter.kt
DateTimeFormatter.kt
PhoneValidator.kt
InvoiceNumberGenerator.kt
```

---

# 46. Dependency Injection Modules

Recommended:

```text
coreDatabaseModule
studentModule
scheduleModule
sessionModule
billingModule
paymentModule
```

Example:

```kotlin
val billingModule = module {

    single<InvoiceRepository> {
        InvoiceRepositoryImpl(
            localDataSource = get()
        )
    }

    factory {
        CheckBillingRuleUseCase(
            studentRepository = get(),
            sessionRepository = get(),
            generateInvoiceUseCase = get()
        )
    }

    factory {
        GenerateInvoiceUseCase(
            invoiceRepository = get()
        )
    }

    viewModel {
        BillingViewModel(
            getInvoicesUseCase = get()
        )
    }
}
```

---

# 47. Main Domain Models

Expected core models:

```text
Student
Schedule
Session
SessionReport
Invoice
InvoiceItem
Payment
```

Expected status enums:

```text
ScheduleStatus
SessionStatus
SessionBillingStatus
InvoiceStatus
PaymentMethod
StudentProgress
```

---

# 48. Expected Use Cases

## Student

```text
GetStudentsUseCase
GetStudentDetailUseCase
CreateStudentUseCase
UpdateStudentUseCase
DeleteStudentUseCase
```

## Schedule

```text
GetTodaySchedulesUseCase
GetSchedulesByStudentUseCase
CreateScheduleUseCase
UpdateScheduleUseCase
CancelScheduleUseCase
```

## Session

```text
StartSessionUseCase
GetActiveSessionUseCase
CompleteSessionUseCase
GetSessionHistoryUseCase
```

## Billing

```text
CheckBillingRuleUseCase
GenerateInvoiceUseCase
GetInvoicesUseCase
GetInvoiceDetailUseCase
```

## Payment

```text
RecordPaymentUseCase
GetPaymentHistoryUseCase
```

Only add a use case when it represents a real application action.

---

# 49. Database Guidelines

Database schema should enforce integrity where practical.

Use:

- Primary keys
- Foreign keys
- Unique invoice number
- Cascade behavior intentionally
- Database transactions

Do not depend exclusively on UI validation for database integrity.

---

# 50. SQLDelight Boundary

Generated SQLDelight entities must remain inside the data layer.

Convert:

```text
SQLDelight Model
    ↓
DTO
    ↓
Domain
```

If an intermediate DTO provides no value, direct mapping from SQLDelight model to domain inside the data layer is acceptable.

The important rule is:

```text
Never leak SQLDelight generated models into domain or presentation.
```

---

# 51. Offline-Only Assumptions

The MVP is single-device and local-only.

Therefore:

- No remote synchronization
- No conflict resolution
- No server timestamps
- No authentication
- No multi-device consistency

Do not add cloud infrastructure unless explicitly requested.

Design IDs and business logic cleanly enough that sync can be added later.

---

# 52. Future-Proofing

Do not build future features now, but avoid blocking them.

Potential future capabilities:

```text
Cloud backup
Parent application
Multiple tutors
Multiple devices
PDF invoice
WhatsApp sharing
Notifications
Payment gateway
Analytics
```

Current architecture should allow replacing local repositories with remote-backed implementations later without changing domain contracts significantly.

---

# 53. Agent Coding Workflow

When asked to implement a feature:

1. Understand the requested business behavior.
2. Inspect existing domain models and architecture.
3. Reuse existing patterns.
4. Define or update domain model if necessary.
5. Define repository contract.
6. Implement data source/repository.
7. Add mapper.
8. Add use case.
9. Add ViewModel state/events/effects.
10. Implement Compose UI.
11. Register dependencies in Koin.
12. Add tests for business rules.
13. Check imports and compilation consistency.
14. Avoid unrelated refactors.

Do not skip layers merely to make implementation faster unless the feature is truly trivial.

---

# 54. Agent Modification Rules

When editing existing code:

- Preserve existing naming conventions.
- Preserve package conventions.
- Do not rewrite unrelated files.
- Do not change public APIs unnecessarily.
- Avoid adding dependencies without a concrete need.
- Avoid architectural migrations unless explicitly requested.
- Prefer small, reviewable changes.
- Remove unused imports.
- Remove dead code introduced by changes.
- Keep code compiling conceptually across commonMain targets.

---

# 55. Agent Output Expectations

When generating code:

- Provide complete implementation for modified classes when practical.
- Do not leave pseudo-code inside production files.
- Do not use `TODO()` unless explicitly requested.
- Do not invent unavailable APIs.
- State assumptions when existing project context is missing.
- Reuse project components when they already exist.
- Prefer domain naming over technical naming.

---

# 56. Preferred Technology Choices

For this project:

```text
UI:
Compose Multiplatform

State:
ViewModel + StateFlow

Async:
Kotlin Coroutines + Flow

Architecture:
Clean Architecture

Dependency Injection:
Koin

Database:
SQLDelight

Date/Time:
kotlinx-datetime

Navigation:
Compose-compatible typed navigation approach

Testing:
kotlin.test + coroutine test utilities
```

Do not replace these technologies unless explicitly requested.

---

# 57. Example End-to-End Feature Flow

Starting a session:

```text
SessionScreen
    ↓
SessionViewModel
    ↓
StartSessionUseCase
    ↓
SessionRepository
    ↓
SessionRepositoryImpl
    ↓
SessionLocalDataSource
    ↓
SQLDelight
```

Completing a session:

```text
Complete Session Click
    ↓
SessionViewModel
    ↓
CompleteSessionUseCase
    ↓
SessionRepository.completeSession()
    ↓
ScheduleRepository.markCompleted()
    ↓
CheckBillingRuleUseCase
    ↓
GenerateInvoiceUseCase if required
    ↓
Database Transaction
```

---

# 58. Definition of Done

A feature is considered complete when:

- Business logic lives in the correct layer
- UI uses immutable StateFlow state
- UI actions are modeled as events
- Domain models are not persistence models
- Repository interface exists in domain
- Repository implementation exists in data
- DTO/entity mapping is explicit where needed
- Use cases contain business behavior
- DI registrations are added
- No database access occurs in UI/ViewModel
- No business logic exists in Composables
- Error/loading states are handled
- Important business rules have tests
- Code remains clean and readable
- No unrelated architectural changes are introduced

---

# 59. Final Architecture Reference

```text
┌──────────────────────────────────────┐
│             PRESENTATION             │
│                                      │
│ Compose                              │
│ ViewModel                            │
│ UiState                              │
│ UiEvent                              │
│ UiEffect                             │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│                DOMAIN                │
│                                      │
│ Domain Model                         │
│ Repository Interface                 │
│ UseCase                              │
│ Business Rule                        │
│ Validation                           │
└──────────────────▲───────────────────┘
                   │
                   │ implements
                   │
┌──────────────────┴───────────────────┐
│                 DATA                 │
│                                      │
│ DTO                                  │
│ Mapper                               │
│ RepositoryImpl                       │
│ LocalDataSource                      │
│ SQLDelight                           │
└──────────────────────────────────────┘
```

Core principle:

```text
UI displays state.
ViewModel manages state.
UseCase owns business actions.
Domain defines business meaning.
Repository abstracts data.
Data layer persists data.
DI wires dependencies.
Utils solve focused reusable technical concerns.
```

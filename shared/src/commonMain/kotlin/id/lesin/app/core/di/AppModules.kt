package id.lesin.app.core.di

import id.lesin.app.core.database.DatabaseDriverFactory
import id.lesin.app.core.database.LesinDatabase
import id.lesin.app.feature.billing.data.datasource.InvoiceLocalDataSource
import id.lesin.app.feature.billing.data.datasource.InvoiceLocalDataSourceImpl
import id.lesin.app.feature.billing.data.repository.InvoiceRepositoryImpl
import id.lesin.app.feature.billing.domain.repository.InvoiceRepository
import id.lesin.app.feature.billing.domain.usecase.CheckBillingRuleUseCase
import id.lesin.app.feature.billing.domain.usecase.GenerateInvoiceUseCase
import id.lesin.app.feature.billing.domain.usecase.GeneratePendingInvoicesUseCase
import id.lesin.app.feature.billing.domain.usecase.GetInvoiceDetailUseCase
import id.lesin.app.feature.billing.domain.usecase.GetInvoicesUseCase
import id.lesin.app.feature.billing.presentation.detail.InvoiceDetailViewModel
import id.lesin.app.feature.billing.presentation.list.BillingViewModel
import id.lesin.app.feature.home.presentation.HomeViewModel
import id.lesin.app.feature.schedule.data.datasource.ScheduleLocalDataSource
import id.lesin.app.feature.schedule.data.datasource.ScheduleLocalDataSourceImpl
import id.lesin.app.feature.schedule.data.repository.ScheduleRepositoryImpl
import id.lesin.app.feature.schedule.domain.repository.ScheduleRepository
import id.lesin.app.feature.schedule.domain.usecase.CreateScheduleUseCase
import id.lesin.app.feature.schedule.domain.usecase.GetScheduleUseCase
import id.lesin.app.feature.schedule.domain.usecase.GetSchedulesByStudentUseCase
import id.lesin.app.feature.schedule.domain.usecase.GetTodaySchedulesUseCase
import id.lesin.app.feature.schedule.domain.usecase.ValidateScheduleUseCase
import id.lesin.app.feature.schedule.presentation.form.ScheduleFormViewModel
import id.lesin.app.feature.session.data.datasource.SessionLocalDataSource
import id.lesin.app.feature.session.data.datasource.SessionLocalDataSourceImpl
import id.lesin.app.feature.session.data.repository.SessionRepositoryImpl
import id.lesin.app.feature.session.domain.repository.SessionRepository
import id.lesin.app.feature.session.domain.usecase.CompleteSessionUseCase
import id.lesin.app.feature.session.domain.usecase.GetActiveSessionsUseCase
import id.lesin.app.feature.session.domain.usecase.GetSessionHistoryUseCase
import id.lesin.app.feature.session.domain.usecase.GetSessionUseCase
import id.lesin.app.feature.session.domain.usecase.StartSessionUseCase
import id.lesin.app.feature.session.presentation.SessionViewModel
import id.lesin.app.feature.student.data.datasource.StudentLocalDataSource
import id.lesin.app.feature.student.data.datasource.StudentLocalDataSourceImpl
import id.lesin.app.feature.student.data.repository.StudentRepositoryImpl
import id.lesin.app.feature.student.domain.repository.StudentRepository
import id.lesin.app.feature.student.domain.usecase.CreateStudentUseCase
import id.lesin.app.feature.student.domain.usecase.DeleteStudentUseCase
import id.lesin.app.feature.student.domain.usecase.GetStudentDetailUseCase
import id.lesin.app.feature.student.domain.usecase.GetStudentsUseCase
import id.lesin.app.feature.student.domain.usecase.UpdateStudentUseCase
import id.lesin.app.feature.student.domain.usecase.ValidateStudentUseCase
import id.lesin.app.feature.student.presentation.detail.StudentDetailViewModel
import id.lesin.app.feature.student.presentation.form.StudentFormViewModel
import id.lesin.app.feature.student.presentation.list.StudentListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModules(databaseDriverFactory: DatabaseDriverFactory) = listOf(
    databaseModule(databaseDriverFactory),
    studentModule,
    scheduleModule,
    homeModule,
    sessionModule,
    billingModule,
)

private fun databaseModule(databaseDriverFactory: DatabaseDriverFactory) = module {
    single { databaseDriverFactory.createDriver() }
    single { LesinDatabase(get()) }
}

private val studentModule = module {
    single<StudentLocalDataSource> { StudentLocalDataSourceImpl(get()) }
    single<StudentRepository> { StudentRepositoryImpl(get()) }

    factory { GetStudentsUseCase(get()) }
    factory { ValidateStudentUseCase() }
    factory { CreateStudentUseCase(get(), get()) }
    factory { UpdateStudentUseCase(get(), get()) }
    factory { GetStudentDetailUseCase(get()) }
    factory { DeleteStudentUseCase(get()) }

    viewModel { StudentListViewModel(get()) }
    viewModel { parameters -> StudentFormViewModel(parameters.getOrNull<Long>(), get(), get(), get(), get()) }
    viewModel { parameters -> StudentDetailViewModel(parameters.get<Long>(), get(), get(), get(), get()) }
}

private val scheduleModule = module {
    single<ScheduleLocalDataSource> { ScheduleLocalDataSourceImpl(get()) }
    single<ScheduleRepository> { ScheduleRepositoryImpl(get()) }

    factory { ValidateScheduleUseCase() }
    factory { CreateScheduleUseCase(get(), get(), get()) }
    factory { GetSchedulesByStudentUseCase(get()) }
    factory { GetScheduleUseCase(get()) }
    factory { GetTodaySchedulesUseCase(get()) }

    viewModel { parameters -> ScheduleFormViewModel(parameters.get<Long>(), get(), get(), get()) }
}

private val homeModule = module {
    viewModel { HomeViewModel(get(), get(), get()) }
}

private val sessionModule = module {
    single<SessionLocalDataSource> { SessionLocalDataSourceImpl(get()) }
    single<SessionRepository> { SessionRepositoryImpl(get()) }
    factory { StartSessionUseCase(get(), get()) }
    factory { CompleteSessionUseCase(get(), get()) }
    factory { GetSessionUseCase(get()) }
    factory { GetActiveSessionsUseCase(get()) }
    factory { GetSessionHistoryUseCase(get()) }
    viewModel { parameters -> SessionViewModel(parameters.get<Long>(), get(), get(), get(), get(), get()) }
}

private val billingModule = module {
    single<InvoiceLocalDataSource> { InvoiceLocalDataSourceImpl(get()) }
    single<InvoiceRepository> { InvoiceRepositoryImpl(get()) }
    factory { GenerateInvoiceUseCase(get()) }
    factory { CheckBillingRuleUseCase(get(), get(), get()) }
    factory { GeneratePendingInvoicesUseCase(get(), get()) }
    factory { GetInvoicesUseCase(get()) }
    factory { GetInvoiceDetailUseCase(get()) }
    viewModel { BillingViewModel(get(), get()) }
    viewModel { parameters -> InvoiceDetailViewModel(parameters.get<Long>(), get()) }
}

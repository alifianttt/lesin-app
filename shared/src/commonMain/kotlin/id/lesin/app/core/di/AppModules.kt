package id.lesin.app.core.di

import id.lesin.app.core.database.DatabaseDriverFactory
import id.lesin.app.core.database.LesinDatabase
import id.lesin.app.feature.student.data.datasource.StudentLocalDataSource
import id.lesin.app.feature.student.data.datasource.StudentLocalDataSourceImpl
import id.lesin.app.feature.student.data.repository.StudentRepositoryImpl
import id.lesin.app.feature.student.domain.repository.StudentRepository
import id.lesin.app.feature.student.domain.usecase.CreateStudentUseCase
import id.lesin.app.feature.student.domain.usecase.GetStudentsUseCase
import id.lesin.app.feature.student.presentation.list.StudentListViewModel
import id.lesin.app.feature.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModules(databaseDriverFactory: DatabaseDriverFactory) = listOf(
    databaseModule(databaseDriverFactory),
    studentModule,
    homeModule,
)

private fun databaseModule(databaseDriverFactory: DatabaseDriverFactory) = module {
    single { databaseDriverFactory.createDriver() }
    single { LesinDatabase(get()) }
}

private val studentModule = module {
    single<StudentLocalDataSource> { StudentLocalDataSourceImpl(get()) }
    single<StudentRepository> { StudentRepositoryImpl(get()) }

    factory { GetStudentsUseCase(get()) }
    factory { CreateStudentUseCase(get()) }

    viewModel { StudentListViewModel(get()) }
}

private val homeModule = module {
    viewModel { HomeViewModel(get()) }
}

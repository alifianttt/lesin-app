package id.lesin.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import id.lesin.app.feature.schedule.FakeScheduleRepository
import id.lesin.app.feature.schedule.domain.usecase.CreateScheduleUseCase
import id.lesin.app.feature.schedule.domain.usecase.ValidateScheduleUseCase
import id.lesin.app.feature.schedule.presentation.form.ScheduleFormViewModel
import id.lesin.app.feature.student.FakeStudentRepository
import id.lesin.app.feature.student.domain.usecase.GetStudentDetailUseCase
import id.lesin.app.feature.student.testStudent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class RouteViewModelScopeTest {
    @Test
    fun navigatingFromStudentDetailToScheduleDoesNotCancelFormLoading() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val parent = controller.get()
        val frameClock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext + frameClock)
        val composition = Composition(UnitApplier(), recomposer)
        val route = mutableStateOf<Route>(Route.StudentDetail(1))
        val students = FakeStudentRepository(listOf(testStudent()))
        val schedules = FakeScheduleRepository()
        lateinit var form: ScheduleFormViewModel
        lateinit var detail: TrackingViewModel
        lateinit var detailStore: ViewModelStore
        lateinit var formStore: ViewModelStore
        backgroundScope.launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }
        try {
            composition.setContent {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides parent,
                    LocalLifecycleOwner provides parent,
                    LocalSavedStateRegistryOwner provides parent,
                ) {
                    key(route.value) {
                        val owner = rememberRouteViewModelStoreOwner(route.value)
                        when (route.value) {
                            is Route.StudentDetail -> remember(owner) {
                                detailStore = owner.viewModelStore
                                detail = TrackingViewModel().also { detailStore.put("detail", it) }
                            }
                            is Route.ScheduleForm -> remember(owner) {
                                formStore = owner.viewModelStore
                                form = ScheduleFormViewModel(
                                    1, GetStudentDetailUseCase(students),
                                    CreateScheduleUseCase(schedules, students, ValidateScheduleUseCase()),
                                    ValidateScheduleUseCase(),
                                ).also { formStore.put("form", it) }
                            }
                            else -> Unit
                        }
                    }
                }
            }
            runCurrent()
            // These distinct routes have identical generated hash codes.
            assertEquals(Route.StudentDetail(1).hashCode(), Route.ScheduleForm(1).hashCode())
            route.value = Route.ScheduleForm(1)
            runCurrent()
            frameClock.sendFrame(0L)
            runCurrent()
            assertNotSame(detailStore, formStore)
            assertTrue(detail.isCleared)
            assertFalse(form.state.value.isLoading)
            assertEquals("Budi", form.state.value.student?.name)

            val firstForm = form
            route.value = Route.StudentDetail(1)
            runCurrent()
            frameClock.sendFrame(16_000_000L)
            runCurrent()
            route.value = Route.ScheduleForm(1)
            runCurrent()
            frameClock.sendFrame(32_000_000L)
            runCurrent()
            assertNotSame(firstForm, form)
            assertFalse(form.state.value.isLoading)
            assertEquals("Mathematics", form.state.value.subject)
        } finally {
            composition.dispose()
            recomposer.cancel()
            controller.pause().stop().destroy()
            Dispatchers.resetMain()
        }
    }
}

private class TrackingViewModel : ViewModel() {
    var isCleared = false
    override fun onCleared() { isCleared = true }
}

private class UnitApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun onClear() = Unit
}

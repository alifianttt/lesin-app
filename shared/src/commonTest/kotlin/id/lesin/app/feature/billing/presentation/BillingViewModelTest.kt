package id.lesin.app.feature.billing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import id.lesin.app.feature.billing.FakeInvoiceRepository
import id.lesin.app.feature.billing.domain.model.InvoiceDetail
import id.lesin.app.feature.billing.domain.model.InvoiceStatus
import id.lesin.app.feature.billing.domain.usecase.GeneratePendingInvoicesUseCase
import id.lesin.app.feature.billing.domain.usecase.GetInvoiceDetailUseCase
import id.lesin.app.feature.billing.domain.usecase.GetInvoicesUseCase
import id.lesin.app.feature.billing.presentation.detail.InvoiceDetailEvent
import id.lesin.app.feature.billing.presentation.detail.InvoiceDetailViewModel
import id.lesin.app.feature.billing.presentation.list.BillingEvent
import id.lesin.app.feature.billing.presentation.list.BillingViewModel
import id.lesin.app.feature.billing.testBillingRule
import id.lesin.app.feature.billing.testInvoice
import id.lesin.app.feature.session.FakeSessionRepository
import id.lesin.app.feature.session.domain.model.SessionReport
import id.lesin.app.feature.session.domain.model.SessionStatus
import id.lesin.app.feature.session.domain.model.StudentProgress
import id.lesin.app.feature.session.testSession
import id.lesin.app.feature.student.FakeStudentRepository
import id.lesin.app.feature.student.testStudent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BillingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var key = 0

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private fun <T : ViewModel> track(viewModel: T) = viewModel.also { store.put("billing-${key++}", it) }

    private fun list(invoices: FakeInvoiceRepository, sessions: FakeSessionRepository = FakeSessionRepository()): BillingViewModel {
        val students = FakeStudentRepository(listOf(testStudent()))
        return track(BillingViewModel(GetInvoicesUseCase(invoices),
            GeneratePendingInvoicesUseCase(students, testBillingRule(sessions, students, invoices))))
    }

    @Test fun filtersRemainAppliedToLiveInvoiceUpdates() = runTest(dispatcher) {
        val invoices = FakeInvoiceRepository().apply {
            details.value = listOf(InvoiceDetail(testInvoice(), emptyList()), InvoiceDetail(testInvoice(2, InvoiceStatus.PAID), emptyList()))
        }
        val viewModel = list(invoices)
        assertTrue(viewModel.state.value.isLoading)
        runCurrent()
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(2, viewModel.state.value.invoices.size)
        viewModel.onEvent(BillingEvent.FilterChanged(InvoiceStatus.UNPAID))
        assertEquals(listOf(1L), viewModel.state.value.invoices.map { it.id })
        invoices.details.value += InvoiceDetail(testInvoice(3), emptyList())
        runCurrent()
        assertEquals(listOf(1L, 3L), viewModel.state.value.invoices.map { it.id })
        viewModel.onEvent(BillingEvent.FilterChanged(InvoiceStatus.PAID))
        assertEquals(listOf(2L), viewModel.state.value.invoices.map { it.id })
        viewModel.onEvent(BillingEvent.FilterChanged(null))
        assertEquals(3, viewModel.state.value.invoices.size)
    }

    @Test fun loadFailureCanBeRetriedWithoutLosingSelectedFilter() = runTest(dispatcher) {
        val invoices = FakeInvoiceRepository().apply { loadError = IllegalStateException("disk") }
        val viewModel = list(invoices)
        runCurrent()
        assertNotNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
        viewModel.onEvent(BillingEvent.FilterChanged(InvoiceStatus.PAID))
        invoices.loadError = null
        invoices.details.value = listOf(InvoiceDetail(testInvoice(), emptyList()), InvoiceDetail(testInvoice(2, InvoiceStatus.PAID), emptyList()))
        viewModel.onEvent(BillingEvent.RetryClicked)
        runCurrent()
        assertNull(viewModel.state.value.error)
        assertEquals(listOf(2L), viewModel.state.value.invoices.map { it.id })
    }

    @Test fun billingRecoveryFailureIsVisibleAndRetryBlocksDuplicateChecks() = runTest(dispatcher) {
        val report = SessionReport("Fractions", StudentProgress.GOOD)
        val sessions = FakeSessionRepository(initial = (1L..4L).map {
            testSession().copy(id = it, scheduleId = it, status = SessionStatus.COMPLETED, report = report, endedAt = testSession().startedAt + 100)
        })
        val invoices = FakeInvoiceRepository(sessions).apply { createError = IllegalStateException("disk") }
        val viewModel = list(invoices, sessions)
        runCurrent()
        assertNotNull(viewModel.state.value.billingError)
        assertFalse(viewModel.state.value.isCheckingBilling)
        invoices.createError = null
        invoices.createGate = CompletableDeferred()
        viewModel.onEvent(BillingEvent.CheckBillingClicked)
        viewModel.onEvent(BillingEvent.CheckBillingClicked)
        runCurrent()
        assertTrue(viewModel.state.value.isCheckingBilling)
        invoices.createGate!!.complete(Unit)
        runCurrent()
        assertFalse(viewModel.state.value.isCheckingBilling)
        assertNull(viewModel.state.value.billingError)
        assertEquals(1, invoices.drafts.size)
        assertEquals(1, viewModel.state.value.invoices.size)
    }

    @Test fun detailLoadsAndReflectsPaymentStatusUpdates() = runTest(dispatcher) {
        val invoices = FakeInvoiceRepository().apply { details.value = listOf(InvoiceDetail(testInvoice(), emptyList())) }
        val viewModel = track(InvoiceDetailViewModel(1, GetInvoiceDetailUseCase(invoices)))
        assertTrue(viewModel.state.value.isLoading)
        runCurrent()
        assertEquals(InvoiceStatus.UNPAID, viewModel.state.value.detail?.invoice?.status)
        invoices.details.value = listOf(InvoiceDetail(testInvoice(status = InvoiceStatus.PAID), emptyList()))
        runCurrent()
        assertEquals(InvoiceStatus.PAID, viewModel.state.value.detail?.invoice?.status)
    }

    @Test fun detailDistinguishesMissingInvoiceFromLoadFailureAndSupportsRetry() = runTest(dispatcher) {
        val invoices = FakeInvoiceRepository().apply { loadError = IllegalStateException("disk") }
        val viewModel = track(InvoiceDetailViewModel(99, GetInvoiceDetailUseCase(invoices)))
        runCurrent()
        assertFalse(viewModel.state.value.isLoading)
        assertNotNull(viewModel.state.value.error)
        invoices.loadError = null
        viewModel.onEvent(InvoiceDetailEvent.RetryClicked)
        runCurrent()
        assertNull(viewModel.state.value.error)
        assertNull(viewModel.state.value.detail)
    }
}

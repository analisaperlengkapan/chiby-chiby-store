package com.chibychibystore.service

import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.PengeluaranRepository
import com.chibychibystore.service.impl.ExpenseServiceImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * Expense recording and the date-window queries behind the expense list and
 * reports. The legacy `ExpenseServiceTest` was removed with the excluded suite;
 * this restores coverage of the current service. Its authorization moved to the
 * UI/ViewModel layer, so the cases here exercise the data behaviour instead of
 * the removed permission checks.
 */
class ExpenseServiceTest {

    @Mock lateinit var repository: PengeluaranRepository

    private lateinit var service: ExpenseServiceImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        service = ExpenseServiceImpl(repository)
    }

    private fun expense(id: Long = 1, amount: Double = 50_000.0) = Pengeluaran(
        id = id,
        expenseDate = Date(),
        category = KategoriPengeluaran.UTILITIES,
        amount = amount,
        createdBy = 1L
    )

    @Test
    fun `createPengeluaran returns the persisted row`() = runTest {
        val persisted = expense(id = 5)
        whenever(repository.createPengeluaran(any())).thenReturn(Result.success(5L))
        whenever(repository.getPengeluaranById(5L)).thenReturn(Result.success(persisted))

        val result = service.createPengeluaran(expense(id = 0))

        assertEquals(persisted, result.getOrNull())
    }

    @Test
    fun `createPengeluaran fails when the insert fails`() = runTest {
        whenever(repository.createPengeluaran(any())).thenReturn(Result.failure(Exception("db down")))

        assertTrue(service.createPengeluaran(expense(id = 0)).isFailure)
    }

    @Test
    fun `updatePengeluaran returns the updated entity`() = runTest {
        val updated = expense(id = 3, amount = 75_000.0)
        whenever(repository.updatePengeluaran(any())).thenReturn(Result.success(Unit))

        assertEquals(updated, service.updatePengeluaran(updated).getOrNull())
    }

    @Test
    fun `deletePengeluaran delegates to the repository`() = runTest {
        whenever(repository.deletePengeluaran(3L)).thenReturn(Result.success(Unit))

        assertTrue(service.deletePengeluaran(3L).isSuccess)
        verify(repository).deletePengeluaran(3L)
    }

    @Test
    fun `getPengeluaran delegates to the repository`() = runTest {
        whenever(repository.getPengeluaranById(3L)).thenReturn(Result.success(expense(id = 3)))

        assertEquals(3L, service.getPengeluaran(3L).getOrNull()?.id)
    }

    @Test
    fun `getPengeluarans with no dates spans a 30 day window ending today`() = runTest {
        whenever(repository.getPengeluaransByDateRangeList(any(), any())).thenReturn(listOf(expense()))

        val result = service.getPengeluarans(null, null, null)

        assertEquals(1, result.getOrNull()?.size)
        val rangeCaptor = argumentCaptor<Date>()
        verify(repository).getPengeluaransByDateRangeList(rangeCaptor.capture(), rangeCaptor.capture())
        val start = rangeCaptor.firstValue
        val end = rangeCaptor.lastValue
        // The end bound is the last instant of the chosen day, so allow a day of
        // slack when measuring the span.
        val days = ChronoUnit.DAYS.between(
            start.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
            end.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        )
        assertTrue("window should be about 30 days but was $days", days in 29..30)
    }

    @Test
    fun `getPengeluarans filters by a valid category`() = runTest {
        whenever(repository.getPengeluaransByDateRangeAndKategori(any(), any(), any()))
            .thenReturn(listOf(expense()))

        val result = service.getPengeluarans(LocalDate.now().minusDays(1), LocalDate.now(), "UTILITIES")

        assertEquals(1, result.getOrNull()?.size)
        val categoryCaptor = argumentCaptor<KategoriPengeluaran>()
        verify(repository).getPengeluaransByDateRangeAndKategori(any(), any(), categoryCaptor.capture())
        assertEquals(KategoriPengeluaran.UTILITIES, categoryCaptor.firstValue)
    }

    @Test
    fun `getPengeluarans falls back to the unfiltered query for an unknown category`() = runTest {
        whenever(repository.getPengeluaransByDateRangeList(any(), any())).thenReturn(listOf(expense(), expense(id = 2)))

        val result = service.getPengeluarans(LocalDate.now().minusDays(1), LocalDate.now(), "NOT_A_CATEGORY")

        assertEquals(2, result.getOrNull()?.size)
        verify(repository).getPengeluaransByDateRangeList(any(), any())
    }

    @Test
    fun `getTotalPengeluarans returns the repository total`() = runTest {
        whenever(repository.getTotalPengeluaranAmount(any(), any())).thenReturn(Result.success(123_000.0))

        assertEquals(123_000.0, service.getTotalPengeluarans(LocalDate.now().minusDays(7), LocalDate.now()).getOrNull()!!, 0.001)
    }

    @Test
    fun `getTotalPengeluarans returns zero when the repository fails`() = runTest {
        whenever(repository.getTotalPengeluaranAmount(any(), any())).thenReturn(Result.failure(Exception("db down")))

        assertEquals(0.0, service.getTotalPengeluarans(LocalDate.now().minusDays(7), LocalDate.now()).getOrNull()!!, 0.001)
    }

    @Test
    fun `approvePengeluaran delegates to the repository`() = runTest {
        whenever(repository.approvePengeluaran(3L, 9L)).thenReturn(Result.success(Unit))

        assertTrue(service.approvePengeluaran(3L, 9L).isSuccess)
        verify(repository).approvePengeluaran(3L, 9L)
    }

    @Test
    fun `observePengeluarans streams the repository flow`() = runTest {
        whenever(repository.getAllPengeluarans()).thenReturn(flowOf(listOf(expense())))

        assertEquals(1, service.observePengeluarans().first().size)
    }

    @Test
    fun `observePengeluaransPerKategori returns an empty flow for an unknown category`() = runTest {
        val result = service.observePengeluaransPerKategori("NOT_A_CATEGORY").first()

        assertTrue(result.isEmpty())
    }
}

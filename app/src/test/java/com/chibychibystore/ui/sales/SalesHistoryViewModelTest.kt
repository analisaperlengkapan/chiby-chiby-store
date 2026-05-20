package com.chibychibystore.ui.sales
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.SaleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.mockito.kotlin.*
@ExperimentalCoroutinesApi
class SalesHistoryViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()
    @Before
    fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After
    fun tearDown() { Dispatchers.resetMain() }
    @Test
    fun loadSalesSuccess() = runTest {
        val ss: SaleService = mock()
        whenever(ss.getPenjualanByRentangTanggal(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Result.success(emptyList()))
        whenever(ss.observePenjualanFiltered(any(), any(), anyOrNull())).thenReturn(flowOf(emptyList()))
        val vm = SalesHistoryViewModel(ss)
        advanceUntilIdle()
        Assert.assertTrue(vm.uiState.value.sales.isEmpty())
    }
}

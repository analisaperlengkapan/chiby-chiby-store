package com.chibychibystore.ui.viewmodel
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.*
import com.chibychibystore.ui.dashboard.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.mockito.kotlin.*
@ExperimentalCoroutinesApi
class DashboardViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()
    @Before
    fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After
    fun tearDown() { Dispatchers.resetMain() }
    @Test
    fun initialLoadSuccess() = runTest {
        val rs: ReportingService = mock()
        val ss: SaleService = mock()
        whenever(rs.observeSalesMetrics()).thenReturn(flowOf(MetrikPenjualan(1.0, 1, 1.0, 1.0)))
        whenever(ss.getRecentPenjualan(any())).thenAnswer { Result.success(emptyList<com.chibychibystore.data.local.entity.Penjualan>()) }
        whenever(rs.getSalesTrend(any(), any())).thenAnswer { Result.success(emptyList<DataTren>()) }
        val vm = DashboardViewModel(ss, rs, mock())
        advanceUntilIdle()
        Assert.assertEquals(1.0, vm.uiState.value.todaySales, 0.0)
    }
}

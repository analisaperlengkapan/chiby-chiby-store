package com.chibychibystore.ui.pos
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.StokGudangRepository
import com.chibychibystore.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.mockito.kotlin.*
@ExperimentalCoroutinesApi
class PosViewModelTest {
    @get:Rule val rule = InstantTaskExecutorRule()
    private val testDispatcher = StandardTestDispatcher()
    @Before
    fun setup() { Dispatchers.setMain(testDispatcher) }
    @After
    fun tearDown() { Dispatchers.resetMain() }
    @Test
    fun addProductToCartSuccess() = runTest {
        val sgr: StokGudangRepository = mock()
        val pms: PromoService = mock()
        val pls: PelangganService = mock()
        val ws: WarehouseService = mock()
        whenever(pls.ambilSemuaPelanggan()).thenReturn(flowOf(emptyList()))
        whenever(ws.observeGudangs()).thenReturn(flowOf(emptyList()))
        val vm = PosViewModel(mock(), mock(), mock(), pms, pls, ws, sgr)
        val product = Produk(id = 1L, name = "P", costPrice = 1.0, sellingPrice = 2.0, categoryId = 1L, warehouseId = 1L)
        whenever(sgr.getStock(eq(1L), any())).thenReturn(Result.success(StokGudang(productId = 1L, warehouseId = 1L, quantity = 10)))
        whenever(pms.calculateDiscount(any())).thenReturn(0.0)
        vm.addProductToCart(product, 2)
        advanceUntilIdle()
        Assert.assertEquals(1, vm.uiState.value.cartItems.size)
    }
}

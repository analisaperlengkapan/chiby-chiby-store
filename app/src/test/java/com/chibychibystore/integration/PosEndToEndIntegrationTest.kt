package com.chibychibystore.integration
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.*
import com.chibychibystore.ui.pos.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.mockito.kotlin.*
import java.util.*
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
@ExperimentalCoroutinesApi
class PosEndToEndIntegrationTest {
    @get:Rule val rule = InstantTaskExecutorRule()
    @Before
    fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After
    fun tearDown() { Dispatchers.resetMain() }
    @Test
    fun testFullSaleFlow() = runTest {
        val ss: SaleService = mock()
        val auth: AuthService = mock()
        val ps: ProductService = mock()
        val pms: PromoService = mock()
        val pls: PelangganService = mock()
        val ws: WarehouseService = mock()
        val sgr: com.chibychibystore.repository.StokGudangRepository = mock()
        whenever(pls.ambilSemuaPelanggan()).thenReturn(flowOf(emptyList()))
        whenever(ws.observeGudangs()).thenReturn(flowOf(listOf(Gudang(id = 1L, name = "G"))))
        whenever(sgr.getStock(any(), any())).thenReturn(Result.success(StokGudang(productId = 1L, warehouseId = 1L, quantity = 10)))
        whenever(pms.calculateDiscount(any())).thenReturn(0.0)
        whenever(auth.getCurrentUser()).thenReturn(Pengguna(id = 1L, username = "k", passwordHash = "h", role = Role.CASHIER))
        whenever(ss.getOpenShift(any())).thenReturn(Result.success(null))
        whenever(ss.createPenjualan(any(), any())).thenReturn(Result.success(PenjualanWithItems(Penjualan(id = 1L, saleDate = Date(), totalAmount = 1.1, paymentMethod = PaymentMethod.CASH, cashierId = 1L), emptyList())))
        val vm = PosViewModel(ps, ss, auth, pms, pls, ws, sgr)
        vm.addProductToCart(Produk(id = 1L, name = "P", costPrice = 1.0, sellingPrice = 1.0, categoryId = 1L, warehouseId = 1L), 1)
        advanceUntilIdle()
        vm.processPayment()
        advanceUntilIdle()
        Assert.assertEquals(1L, vm.uiState.value.completedSaleId)
    }
}

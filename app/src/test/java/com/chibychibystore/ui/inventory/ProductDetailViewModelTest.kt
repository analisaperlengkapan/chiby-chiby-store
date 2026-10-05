package com.chibychibystore.ui.inventory

import androidx.lifecycle.SavedStateHandle
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.BarcodeService
import com.chibychibystore.service.ProductService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Restores the ViewModel coverage deleted with `ProductDetailViewModelTest`.
 * The legacy suite targeted a different API (string ids, `data.model`); these
 * cases pin the behaviour the current [ProductDetailViewModel] owns: init
 * mode, load/save/update-stock/delete, edit toggling, and validation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var productService: ProductService
    private lateinit var barcodeService: BarcodeService

    private fun product(
        id: Long = 1L,
        name: String = "Test Product",
        barcode: String = "111",
        costPrice: Double = 10_000.0,
        sellingPrice: Double = 15_000.0,
        stock: Int = 10
    ) = Produk(
        id = id,
        name = name,
        barcode = barcode,
        categoryId = 1L,
        costPrice = costPrice,
        sellingPrice = sellingPrice,
        stockQuantity = stock,
        warehouseId = 1L
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        productService = mock()
        barcodeService = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(productId: Long? = null): ProductDetailViewModel {
        val handle = if (productId == null) SavedStateHandle() else SavedStateHandle(mapOf("productId" to productId.toString()))
        return ProductDetailViewModel(productService, barcodeService, handle)
    }

    @Test
    fun `create mode has no product and starts editing`() = runTest {
        val vm = viewModel(productId = null)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isEditing)
        assertNull(vm.uiState.value.product)
    }

    @Test
    fun `init loads the product named by the saved state`() = runTest {
        val stored = product()
        whenever(productService.getProduk("1")).thenReturn(Result.success(stored))

        val vm = viewModel(productId = 1L)
        advanceUntilIdle()

        assertEquals(stored, vm.uiState.value.product)
        assertFalse(vm.uiState.value.isEditing)
        assertFalse(vm.uiState.value.isLoading)
        verify(productService).getProduk("1")
    }

    @Test
    fun `loadProduct surfaces a failure as an error`() = runTest {
        whenever(productService.getProduk("9")).thenReturn(Result.failure(Exception("Produk tidak ditemukan")))

        val vm = viewModel(productId = null)
        advanceUntilIdle()
        vm.loadProduct("9")
        advanceUntilIdle()

        assertEquals("Produk tidak ditemukan", vm.uiState.value.error)
        assertNull(vm.uiState.value.product)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `saveProduct updates an existing product`() = runTest {
        val stored = product()
        val edited = stored.copy(name = "New Name")
        whenever(productService.getProduk("1")).thenReturn(Result.success(stored))
        whenever(productService.updateProduk(edited)).thenReturn(Result.success(edited))

        val vm = viewModel(productId = 1L)
        advanceUntilIdle()
        vm.saveProduct(edited)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(edited, state.product)
        assertFalse(state.isEditing)
        assertFalse(state.isSaving)
        assertEquals("Product berhasil diperbarui", state.successMessage)
        verify(productService).updateProduk(edited)
    }

    @Test
    fun `saveProduct creates a new product in create mode`() = runTest {
        val draft = product(id = 0L, name = "New Product")
        val created = draft.copy(id = 5L)
        whenever(productService.createProduk(draft)).thenReturn(Result.success(created))

        val vm = viewModel(productId = null)
        advanceUntilIdle()
        vm.saveProduct(draft)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(created, state.product)
        assertFalse(state.isEditing)
        assertEquals("Product berhasil dibuat", state.successMessage)
        verify(productService).createProduk(draft)
    }

    @Test
    fun `saveProduct surfaces a failure and keeps editing`() = runTest {
        val draft = product(id = 0L)
        whenever(productService.createProduk(draft)).thenReturn(Result.failure(Exception("Barcode sudah ada")))

        val vm = viewModel(productId = null)
        advanceUntilIdle()
        vm.saveProduct(draft)
        advanceUntilIdle()

        assertEquals("Barcode sudah ada", vm.uiState.value.error)
        assertNull(vm.uiState.value.successMessage)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `updateStock succeeds and reloads the product`() = runTest {
        val stored = product(stock = 10)
        whenever(productService.getProduk("1")).thenReturn(Result.success(stored))
        whenever(productService.updateStock("1", 25)).thenReturn(Result.success(Unit))

        val vm = viewModel(productId = 1L)
        advanceUntilIdle()
        vm.updateStock(25)
        advanceUntilIdle()

        assertEquals("Stok berhasil diperbarui", vm.uiState.value.successMessage)
        verify(productService).updateStock("1", 25)
    }

    @Test
    fun `updateStock surfaces a failure`() = runTest {
        val stored = product()
        whenever(productService.getProduk("1")).thenReturn(Result.success(stored))
        whenever(productService.updateStock("1", -5)).thenReturn(Result.failure(Exception("Stok tidak boleh negatif")))

        val vm = viewModel(productId = 1L)
        advanceUntilIdle()
        vm.updateStock(-5)
        advanceUntilIdle()

        assertEquals("Stok tidak boleh negatif", vm.uiState.value.error)
        assertNull(vm.uiState.value.successMessage)
    }

    @Test
    fun `deleteProduct succeeds and reports it`() = runTest {
        val stored = product()
        whenever(productService.getProduk("1")).thenReturn(Result.success(stored))
        whenever(productService.deleteProduk("1")).thenReturn(Result.success(Unit))

        val vm = viewModel(productId = 1L)
        advanceUntilIdle()
        vm.deleteProduct()
        advanceUntilIdle()

        assertEquals("Product berhasil dihapus", vm.uiState.value.successMessage)
        verify(productService).deleteProduk("1")
    }

    @Test
    fun `deleteProduct does nothing without a loaded product`() = runTest {
        val vm = viewModel(productId = null)
        advanceUntilIdle()
        vm.deleteProduct()
        advanceUntilIdle()

        verify(productService, never()).deleteProduk(any())
    }

    @Test
    fun `toggleEditMode flips the editing flag`() = runTest {
        val stored = product()
        whenever(productService.getProduk("1")).thenReturn(Result.success(stored))

        val vm = viewModel(productId = 1L)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isEditing)

        vm.toggleEditMode()
        assertTrue(vm.uiState.value.isEditing)
    }

    @Test
    fun `cancelEdit reloads the stored product`() = runTest {
        val stored = product()
        whenever(productService.getProduk("1")).thenReturn(Result.success(stored))

        val vm = viewModel(productId = 1L)
        advanceUntilIdle()
        vm.cancelEdit()
        advanceUntilIdle()

        assertEquals(stored, vm.uiState.value.product)
        assertFalse(vm.uiState.value.isEditing)
        verify(productService, times(2)).getProduk("1")
    }

    @Test
    fun `clearError and clearSuccessMessage reset their fields`() = runTest {
        whenever(productService.createProduk(any())).thenReturn(Result.failure(Exception("boom")))

        val vm = viewModel(productId = null)
        advanceUntilIdle()
        vm.saveProduct(product(id = 0L))
        advanceUntilIdle()
        assertEquals("boom", vm.uiState.value.error)

        vm.clearError()
        assertNull(vm.uiState.value.error)
        vm.clearSuccessMessage()
        assertNull(vm.uiState.value.successMessage)
    }

    @Test
    fun `validateProduct rejects blank fields and bad prices`() {
        val vm = viewModel(productId = null)

        assertEquals("Nama product tidak boleh kosong", vm.validateProduct(product(name = " ")))
        assertEquals("Barcode product tidak boleh kosong", vm.validateProduct(product(barcode = "")))
        assertEquals(
            "Harga jual harus lebih besar dari harga beli",
            vm.validateProduct(product(costPrice = 20_000.0, sellingPrice = 15_000.0))
        )
        assertNull(vm.validateProduct(product()))
    }
}

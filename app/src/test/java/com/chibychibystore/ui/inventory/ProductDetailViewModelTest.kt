package com.chibychibystore.ui.inventory
import org.robolectric.annotation.Config

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.ProductService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class ProductDetailViewModelTest {

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: ProductDetailViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `init should load product when productId exists in savedStateHandle`() = runTest {
        // Given
        val productId = "1"
        val product = Produk(productId, "Test Product", "111", "1", 10000.0, 15000.0, 10, "1")
        `when`(savedStateHandle.get<String>("productId")).thenReturn(productId)
        `when`(productService.getProduct(productId)).thenReturn(Result.success(product))

        // When
        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(product, loadedState.product)
            assertFalse(loadedState.isEditing)
        }

        verify(productService).getProduct(productId)
    }

    @Test
    fun `init should set editing mode when productId is null (create mode)`() = runTest {
        // Given
        `when`(savedStateHandle.get<String>("productId")).thenReturn(null)

        // When
        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // Then
        assertTrue(viewModel.uiState.value.isEditing)
        assertNull(viewModel.uiState.value.product)
    }

    @Test
    fun `loadProduct should update state with product on success`() = runTest {
        // Given
        val productId = "1"
        val product = Produk(productId, "Test Product", "111", "1", 10000.0, 15000.0, 10, "1")
        `when`(productService.getProduct(productId)).thenReturn(Result.success(product))

        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // When
        viewModel.loadProduct(productId)

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(product, loadedState.product)
            assertFalse(loadedState.isEditing)
        }
    }

    @Test
    fun `loadProduct should update state with error on failure`() = runTest {
        // Given
        val productId = "1"
        val errorMessage = "Product not found"
        `when`(productService.getProduct(productId)).thenReturn(Result.failure(Exception(errorMessage)))

        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // When
        viewModel.loadProduct(productId)

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals(errorMessage, errorState.error)
            assertNull(errorState.product)
        }
    }

    @Test
    fun `saveProduct should update existing product on success`() = runTest {
        // Given
        val productId = "1"
        val existingProduct = Produk(productId, "Old Name", "111", "1", 10000.0, 15000.0, 10, "1")
        val updatedProduct = existingProduct.copy(nama = "New Name")

        `when`(savedStateHandle.get<String>("productId")).thenReturn(productId)
        `when`(productService.getProduct(productId)).thenReturn(Result.success(existingProduct))
        `when`(productService.updateProduct(updatedProduct)).thenReturn(Result.success(updatedProduct))

        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // When
        viewModel.saveProduct(updatedProduct)

        // Then
        viewModel.uiState.test {
            // Skip initial states
            skipItems(2)

            val savedState = awaitItem()
            assertFalse(savedState.isSaving)
            assertEquals(updatedProduct, savedState.product)
            assertFalse(savedState.isEditing)
            assertEquals("Produk berhasil diperbarui", savedState.successMessage)
        }

        verify(productService).updateProduct(updatedProduct)
    }

    @Test
    fun `saveProduct should create new product when productId is null`() = runTest {
        // Given
        val newProduct = Produk("1", "New Product", "111", "1", 10000.0, 15000.0, 10, "1")

        `when`(savedStateHandle.get<String>("productId")).thenReturn(null)
        `when`(productService.createProduct(newProduct)).thenReturn(Result.success(newProduct))

        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // When
        viewModel.saveProduct(newProduct)

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val savedState = awaitItem()
            assertFalse(savedState.isSaving)
            assertEquals(newProduct, savedState.product)
            assertFalse(savedState.isEditing)
            assertEquals("Produk berhasil dibuat", savedState.successMessage)
        }

        verify(productService).createProduct(newProduct)
    }

    @Test
    fun `saveProduct should update state with error on failure`() = runTest {
        // Given
        val product = Produk("1", "Test Product", "111", "1", 10000.0, 15000.0, 10, "1")
        val errorMessage = "Validation error"

        `when`(savedStateHandle.get<String>("productId")).thenReturn(null)
        `when`(productService.createProduct(product)).thenReturn(Result.failure(Exception(errorMessage)))

        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // When
        viewModel.saveProduct(product)

        // Then
        viewModel.uiState.test {
            // Skip initial state
            skipItems(1)

            val errorState = awaitItem()
            assertFalse(errorState.isSaving)
            assertEquals(errorMessage, errorState.error)
            assertNull(errorState.successMessage)
        }
    }

    @Test
    fun `updateStock should update product stock on success`() = runTest {
        // Given
        val productId = "1"
        val product = Produk(productId, "Test Product", "111", "1", 10000.0, 15000.0, 10, "1")
        val quantity = 5

        `when`(savedStateHandle.get<String>("productId")).thenReturn(productId)
        `when`(productService.getProduct(productId)).thenReturn(Result.success(product))
        `when`(productService.adjustStock(productId, quantity)).thenReturn(Result.success(Unit))

        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // When
        viewModel.adjustStock(quantity)

        // Then
        viewModel.uiState.test {
            // Skip initial loading states
            skipItems(2)

            val updatedState = awaitItem()
            assertFalse(updatedState.isSaving)
            assertEquals("Stok berhasil diperbarui", updatedState.successMessage)
        }

        verify(productService).adjustStock(productId, quantity)
        verify(productService, times(2)).getProduct(productId) // Initial load + reload after update
    }

    @Test
    fun `deleteProduct should delete product on success`() = runTest {
        // Given
        val productId = "1"
        val product = Produk(productId, "Test Product", "111", "1", 10000.0, 15000.0, 10, "1")

        `when`(savedStateHandle.get<String>("productId")).thenReturn(productId)
        `when`(productService.getProduct(productId)).thenReturn(Result.success(product))
        `when`(productService.deleteProduct(productId)).thenReturn(Result.success(Unit))

        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // When
        viewModel.deleteProduct()

        // Then
        viewModel.uiState.test {
            // Skip initial loading states
            skipItems(2)

            val deletedState = awaitItem()
            assertFalse(deletedState.isSaving)
            assertEquals("Produk berhasil dihapus", deletedState.successMessage)
        }

        verify(productService).deleteProduct(productId)
    }

    @Test
    fun `toggleEditMode should toggle editing state`() = runTest {
        // Given
        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // When
        viewModel.toggleEditMode()

        // Then
        assertTrue(viewModel.uiState.value.isEditing)

        // When
        viewModel.toggleEditMode()

        // Then
        assertFalse(viewModel.uiState.value.isEditing)
    }

    @Test
    fun `cancelEdit should reload product when in edit mode for existing product`() = runTest {
        // Given
        val productId = "1"
        val product = Produk(productId, "Test Product", "111", "1", 10000.0, 15000.0, 10, "1")

        `when`(savedStateHandle.get<String>("productId")).thenReturn(productId)
        `when`(productService.getProduct(productId)).thenReturn(Result.success(product))

        viewModel = ProductDetailViewModel(productService, savedStateHandle)
        viewModel.toggleEditMode() // Enter edit mode

        // When
        viewModel.cancelEdit()

        // Then
        assertFalse(viewModel.uiState.value.isEditing)
        verify(productService, times(2)).getProduct(productId) // Initial load + reload on cancel
    }

    @Test
    fun `validateProduct should return error for blank name`() {
        // Given
        val product = Produk("1", "", "111", "1", 10000.0, 15000.0, 10, "1")

        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // When
        val result = viewModel.validateProduct(product)

        // Then
        assertEquals("Nama produk tidak boleh kosong", result)
    }

    @Test
    fun `validateProduct should return error for blank barcode`() {
        // Given
        val product = Produk("1", "Test Product", "", "1", 10000.0, 15000.0, 10, "1")

        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // When
        val result = viewModel.validateProduct(product)

        // Then
        assertEquals("Barcode produk tidak boleh kosong", result)
    }

    @Test
    fun `validateProduct should return error when selling price is less than cost price`() {
        // Given
        val product = Produk("1", "Test Product", "111", "1", 15000.0, 10000.0, 10, "1")

        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // When
        val result = viewModel.validateProduct(product)

        // Then
        assertEquals("Harga jual harus lebih besar dari harga beli", result)
    }

    @Test
    fun `validateProduct should return null for valid product`() {
        // Given
        val product = Produk("1", "Test Product", "111", "1", 10000.0, 15000.0, 10, "1")

        viewModel = ProductDetailViewModel(productService, savedStateHandle)

        // When
        val result = viewModel.validateProduct(product)

        // Then
        assertNull(result)
    }
}
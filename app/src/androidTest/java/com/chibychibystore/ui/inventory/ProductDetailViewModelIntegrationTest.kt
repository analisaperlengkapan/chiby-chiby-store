package com.chibychibystore.ui.inventory

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.chibychibystore.data.model.Produk
import com.chibychibystore.service.ProductService
import com.chibychibystore.testutils.BaseIntegrationTest
import com.chibychibystore.testutils.TestDataBuilder
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class ProductDetailViewModelIntegrationTest : BaseIntegrationTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var productService: ProductService

    private lateinit var viewModel: ProductDetailViewModel
    private lateinit var savedStateHandle: SavedStateHandle

    override fun setupDatabase() {
        super.setupDatabase()
        savedStateHandle = SavedStateHandle()
        viewModel = ProductDetailViewModel(productService, savedStateHandle)
    }

    @Test
    fun `loadProduct should load existing product and update state`() = runTest {
        // Given
        setupTestData()
        val testProduct = TestDataBuilder.createTestProduct()
        val savedProduct = productService.createProduct(testProduct).getOrThrow()
        savedStateHandle["productId"] = savedProduct.id

        // When
        viewModel.loadProduct()

        // Then
        viewModel.uiState.test {
            // Skip initial loading state
            skipItems(1)

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertNull(loadedState.error)
            assertNotNull(loadedState.product)
            assertEquals(savedProduct.nama, loadedState.product?.nama)
            assertEquals(savedProduct.barcode, loadedState.product?.barcode)
        }
    }

    @Test
    fun `loadProduct should handle non-existent product gracefully`() = runTest {
        // Given
        savedStateHandle["productId"] = 999L // Non-existent ID

        // When
        viewModel.loadProduct()

        // Then
        viewModel.uiState.test {
            // Skip initial loading state
            skipItems(1)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNotNull(errorState.error)
            assertNull(errorState.product)
        }
    }

    @Test
    fun `saveProduct should create new product when in create mode`() = runTest {
        // Given
        setupTestData()
        val newProduct = TestDataBuilder.createTestProduct().copy(
            nama = "New Test Product",
            barcode = "999999999999"
        )

        // When
        viewModel.saveProduct(newProduct)

        // Then
        viewModel.uiState.test {
            // Skip to success state
            skipItems(1) // initial state

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertNull(successState.error)
            assertTrue(successState.isSaved)

            // Verify product was created in database
            val createdProduct = productService.getProduct(successState.product?.id ?: 0L).getOrThrow()
            assertEquals("New Test Product", createdProduct?.nama)
        }
    }

    @Test
    fun `saveProduct should update existing product when in edit mode`() = runTest {
        // Given
        setupTestData()
        val originalProduct = productService.createProduct(TestDataBuilder.createTestProduct()).getOrThrow()
        savedStateHandle["productId"] = originalProduct.id
        viewModel.loadProduct()

        // Skip to loaded state
        viewModel.uiState.test {
            skipItems(2) // initial + loading
        }

        val updatedProduct = originalProduct.copy(nama = "Updated Product Name")

        // When
        viewModel.saveProduct(updatedProduct)

        // Then
        viewModel.uiState.test {
            // Skip to success state
            skipItems(1) // current state

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertNull(successState.error)
            assertTrue(successState.isSaved)

            // Verify product was updated in database
            val updatedFromDb = productService.getProduct(originalProduct.id).getOrThrow()
            assertEquals("Updated Product Name", updatedFromDb?.nama)
        }
    }

    @Test
    fun `saveProduct should handle validation errors gracefully`() = runTest {
        // Given - invalid product with empty name
        val invalidProduct = TestDataBuilder.createTestProduct().copy(nama = "")

        // When
        viewModel.saveProduct(invalidProduct)

        // Then
        viewModel.uiState.test {
            // Skip to error state
            skipItems(1) // initial state

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNotNull(errorState.error)
            assertFalse(errorState.isSaved)
        }
    }

    @Test
    fun `deleteProduct should remove product from database and update state`() = runTest {
        // Given
        setupTestData()
        val productToDelete = productService.createProduct(TestDataBuilder.createTestProduct()).getOrThrow()
        savedStateHandle["productId"] = productToDelete.id
        viewModel.loadProduct()

        // Skip to loaded state
        viewModel.uiState.test {
            skipItems(2)
        }

        // When
        viewModel.deleteProduct()

        // Then
        viewModel.uiState.test {
            // Skip to success state
            skipItems(1)

            val deletedState = awaitItem()
            assertFalse(deletedState.isLoading)
            assertNull(deletedState.error)
            assertTrue(deletedState.isDeleted)

            // Verify product was deleted from database
            val deletedProduct = productService.getProduct(productToDelete.id).getOrNull()
            assertNull(deletedProduct)
        }
    }

    @Test
    fun `deleteProduct should handle errors gracefully`() = runTest {
        // Given - product with dependencies that prevent deletion
        savedStateHandle["productId"] = 999L // Non-existent ID

        // When
        viewModel.deleteProduct()

        // Then
        viewModel.uiState.test {
            // Skip to error state
            skipItems(1)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNotNull(errorState.error)
            assertFalse(errorState.isDeleted)
        }
    }

    @Test
    fun `updateStock should modify product stock quantity`() = runTest {
        // Given
        setupTestData()
        val product = productService.createProduct(TestDataBuilder.createTestProduct()).getOrThrow()
        savedStateHandle["productId"] = product.id
        viewModel.loadProduct()

        // Skip to loaded state
        viewModel.uiState.test {
            skipItems(2)
        }

        // When
        viewModel.updateStock(50)

        // Then
        viewModel.uiState.test {
            // Skip to success state
            skipItems(1)

            val updatedState = awaitItem()
            assertFalse(updatedState.isLoading)
            assertNull(updatedState.error)
            assertTrue(updatedState.stockUpdated)

            // Verify stock was updated in database
            val updatedProduct = productService.getProduct(product.id).getOrThrow()
            assertEquals(50, updatedProduct?.stok)
        }
    }

    @Test
    fun `updateStock should handle negative stock values gracefully`() = runTest {
        // Given
        setupTestData()
        val product = productService.createProduct(TestDataBuilder.createTestProduct()).getOrThrow()
        savedStateHandle["productId"] = product.id

        // When
        viewModel.updateStock(-10)

        // Then
        viewModel.uiState.test {
            // Skip to error state
            skipItems(1)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertNotNull(errorState.error)
            assertFalse(errorState.stockUpdated)
        }
    }

    @Test
    fun `toggleEditMode should switch between view and edit modes`() = runTest {
        // Given
        setupTestData()
        val product = productService.createProduct(TestDataBuilder.createTestProduct()).getOrThrow()
        savedStateHandle["productId"] = product.id
        viewModel.loadProduct()

        // Skip to loaded state
        viewModel.uiState.test {
            skipItems(2)
        }

        // When - toggle to edit mode
        viewModel.toggleEditMode()

        // Then
        viewModel.uiState.test {
            val editState = awaitItem()
            assertTrue(editState.isEditing)
        }

        // When - toggle back to view mode
        viewModel.toggleEditMode()

        // Then
        viewModel.uiState.test {
            val viewState = awaitItem()
            assertFalse(viewState.isEditing)
        }
    }

    @Test
    fun `cancelEdit should revert changes and exit edit mode`() = runTest {
        // Given
        setupTestData()
        val originalProduct = productService.createProduct(TestDataBuilder.createTestProduct()).getOrThrow()
        savedStateHandle["productId"] = originalProduct.id
        viewModel.loadProduct()

        // Skip to loaded state and enter edit mode
        viewModel.uiState.test {
            skipItems(2)
        }
        viewModel.toggleEditMode()

        // When
        viewModel.cancelEdit()

        // Then
        viewModel.uiState.test {
            val cancelledState = awaitItem()
            assertFalse(cancelledState.isEditing)
            assertEquals(originalProduct.nama, cancelledState.product?.nama)
        }
    }

    @Test
    fun `clearError should clear error message`() = runTest {
        // Given - simulate error state
        viewModel.uiState.value.copy(error = "Test error")

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.error)
    }
}
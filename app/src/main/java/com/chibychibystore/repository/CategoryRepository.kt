package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.CategoryDao
import com.chibychibystore.data.local.entity.Category
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk operasi data Kategori
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    /**
     * Get semua kategori
     */
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    /**
     * Get kategori by ID
     */
    suspend fun getCategoryById(id: Long): Result<Category> {
        return try {
            val category = categoryDao.getCategoryById(id)
            if (category != null) {
                Result.success(category)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Category with ID $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getCategoryById", e))
        }
    }

    /**
     * Get kategori by name
     */
    suspend fun getCategoryByName(name: String): Result<Category> {
        return try {
            val category = categoryDao.getCategoryByName(name)
            if (category != null) {
                Result.success(category)
            } else {
                Result.failure(ChibyChibyException.DatabaseError("Category with name $name not found"))
            }
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getCategoryByName", e))
        }
    }

    /**
     * Create kategori baru
     */
    suspend fun createCategory(category: Category): Result<Long> {
        return try {
            // Validasi input
            validateCategoryData(category)

            // Check if name already exists
            val existingCategory = categoryDao.getCategoryByName(category.name)
            if (existingCategory != null) {
                return Result.failure(ChibyChibyException.ValidationError("name", "Category name already taken"))
            }

            val id = categoryDao.insertCategory(category)
            Result.success(id)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createCategory", e))
        }
    }

    /**
     * Update kategori
     */
    suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            // Validasi input
            validateCategoryData(category)

            // Check if category exists
            categoryDao.getCategoryById(category.id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Category not found"))

            // Check name uniqueness (exclude current category)
            val categoryWithSameName = categoryDao.getCategoryByName(category.name)
            if (categoryWithSameName != null && categoryWithSameName.id != category.id) {
                return Result.failure(ChibyChibyException.ValidationError("name", "Category name already taken"))
            }

            categoryDao.updateCategory(category)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateCategory", e))
        }
    }

    /**
     * Delete kategori
     */
    suspend fun deleteCategory(id: Long): Result<Unit> {
        return try {
            // Check if category exists
            val category = categoryDao.getCategoryById(id)
                ?: return Result.failure(ChibyChibyException.DatabaseError("Category not found"))

            // Check if category is used by products (business rule)
            // This would require checking ProductDao, but for now we'll allow deletion
            // In a full implementation, you'd check for foreign key constraints

            categoryDao.deleteCategoryById(id)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("deleteCategory", e))
        }
    }

    /**
     * Get jumlah total kategori
     */
    suspend fun getCategoryCount(): Result<Int> {
        return try {
            val count = categoryDao.getCategoryCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getCategoryCount", e))
        }
    }

    private fun validateCategoryData(category: Category) {
        if (category.name.isBlank()) {
            throw ChibyChibyException.ValidationError("name", "Category name cannot be empty")
        }
        if (category.name.length < 2) {
            throw ChibyChibyException.ValidationError("name", "Category name must be at least 2 characters")
        }
    }
}

package com.chibychibystore.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chibychibystore.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM kategori ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM kategori WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM kategori WHERE name = :name")
    suspend fun getCategoryByName(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategoryList(categoryList: List<Category>): List<Long>

    @Update
    suspend fun updateCategory(category: Category)

    @Query("DELETE FROM kategori WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("SELECT COUNT(*) FROM kategori")
    suspend fun getCategoryCount(): Int
}

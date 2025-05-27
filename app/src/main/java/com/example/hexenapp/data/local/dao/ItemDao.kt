package com.example.hexenapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.hexenapp.data.local.model.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>) // Mungkin perlu userId jika untuk user spesifik

    @Update
    suspend fun updateItem(item: ItemEntity): Int // Pastikan item yang diupdate memiliki userId yang benar

    @Delete
    suspend fun deleteItem(item: ItemEntity): Int // Pastikan item yang dihapus memiliki userId yang benar

    // Menghapus item berdasarkan ID dan userId.
    @Query("DELETE FROM items WHERE id = :itemId AND userId = :userId")
    suspend fun deleteItemById(itemId: Int, userId: String): Int

    // Menghapus semua item milik pengguna tertentu.
    @Query("DELETE FROM items WHERE userId = :userId")
    suspend fun deleteAllItemsForUser(userId: String)

    // Mengambil semua item milik pengguna tertentu, diurutkan berdasarkan waktu pembuatan.
    @Query("SELECT * FROM items WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllItems(userId: String): Flow<List<ItemEntity>>

    // Mengambil satu item berdasarkan ID dan userId.
    @Query("SELECT * FROM items WHERE id = :itemId AND userId = :userId")
    fun getItemById(itemId: Int, userId: String): Flow<ItemEntity?>

    // Mencari item berdasarkan nama untuk pengguna tertentu.
    @Query("SELECT * FROM items WHERE userId = :userId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchItemsByName(query: String, userId: String): Flow<List<ItemEntity>>
}

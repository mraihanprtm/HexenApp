package com.example.hexenapp.data.repository

import android.util.Log
import com.example.hexenapp.data.local.dao.ItemDao
import com.example.hexenapp.data.local.model.ItemEntity
import com.example.hexenapp.data.remote.api.ApiService
import com.example.hexenapp.data.remote.dto.ItemCreateRequest
import com.example.hexenapp.data.remote.dto.ItemResponse
import com.example.hexenapp.data.remote.dto.ItemUpdateRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository untuk mengelola data Item, berinteraksi dengan sumber data lokal (Room)
 * dan remote (API Laravel via ApiService).
 *
 * @property itemDao DAO untuk akses database lokal.
 * @property apiService Service untuk panggilan API ke backend.
 * @property firebaseAuth Untuk mendapatkan Firebase ID Token pengguna saat ini.
 */
@Singleton // Jika repository ini juga ingin di-scope sebagai Singleton oleh Hilt
class ItemRepository @Inject constructor(
    private val itemDao: ItemDao,
    private val apiService: ApiService,
    private val firebaseAuth: FirebaseAuth
) {

    private suspend fun getCurrentUserIdToken(): String? {
        return try {
            firebaseAuth.currentUser?.getIdToken(false)?.await()?.token // false: jangan paksa refresh jika tidak perlu
        } catch (e: Exception) {
            Log.e("ItemRepository", "Error getting ID token", e)
            null
        }
    }

    private fun getFormattedToken(idToken: String?): String? {
        return idToken?.let { "Bearer $it" }
    }

    /**
     * Mengambil semua item milik pengguna dari Room.
     * Juga memicu sinkronisasi dari server jika diperlukan (misalnya, saat pertama kali load atau refresh).
     */
    fun getAllItems(userId: String): Flow<List<ItemEntity>> {
        // Selalu kembalikan data dari Room sebagai sumber utama ke UI (untuk offline-first)
        // Sinkronisasi dari network bisa dilakukan secara terpisah atau saat inisialisasi.
        return itemDao.getAllItems(userId).flowOn(Dispatchers.IO)
    }

    /**
     * Fungsi untuk menyegarkan (refresh) data item dari server dan menyimpannya ke Room.
     * Ini bisa dipanggil saat aplikasi dimulai, saat pull-to-refresh, dll.
     */
    suspend fun refreshItemsFromServer() {
        withContext(Dispatchers.IO) {
            val idToken = getCurrentUserIdToken()
            val formattedToken = getFormattedToken(idToken)
            val currentFirebaseUid = firebaseAuth.currentUser?.uid

            if (formattedToken != null && currentFirebaseUid != null) {
                try {
                    val response = apiService.getItems(formattedToken)
                    if (response.isSuccessful) {
                        val itemsFromApi = response.body()
                        itemsFromApi?.let { apiItems ->
                            // Konversi ItemResponse (DTO) ke ItemEntity (Lokal)
                            val itemEntities = apiItems.map { dto ->
                                ItemEntity(
                                    id = dto.id, // Asumsi ID dari server adalah primary key yang sama
                                    userId = dto.userId, // Pastikan ini adalah UID Firebase
                                    name = dto.name,
                                    description = dto.description,
                                    quantity = dto.quantity,
                                    // createdAt dan updatedAt dari server mungkin perlu konversi jika formatnya beda
                                    // Untuk saat ini, kita bisa abaikan atau gunakan timestamp lokal jika API tidak mengembalikannya
                                    // atau jika DTO sudah mengembalikannya sebagai Long
                                    createdAt = System.currentTimeMillis(), // Ganti dengan parsing dari dto.createdAt jika ada
                                    updatedAt = System.currentTimeMillis()  // Ganti dengan parsing dari dto.updatedAt jika ada
                                )
                            }
                            // Hapus item lama milik pengguna ini (atau lakukan strategi upsert yang lebih canggih)
                            itemDao.deleteAllItemsForUser(currentFirebaseUid)
                            itemDao.insertItems(itemEntities)
                            Log.d("ItemRepository", "Items refreshed from server and saved to Room.")
                        }
                    } else {
                        Log.e("ItemRepository", "Error fetching items from server: ${response.code()} - ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e("ItemRepository", "Exception fetching items from server", e)
                }
            } else {
                Log.w("ItemRepository", "Cannot refresh items: Token or UID is null.")
            }
        }
    }


    fun getItemById(itemId: Int, userId: String): Flow<ItemEntity?> {
        // Bisa juga mengambil dari server jika tidak ada di lokal atau ingin data terbaru
        return itemDao.getItemById(itemId, userId).flowOn(Dispatchers.IO)
    }

    suspend fun insertItem(itemCreateRequest: ItemCreateRequest): ResultWrapper<ItemEntity> {
        return withContext(Dispatchers.IO) {
            val idToken = getCurrentUserIdToken()
            val formattedToken = getFormattedToken(idToken)
            val currentFirebaseUid = firebaseAuth.currentUser?.uid

            if (formattedToken == null || currentFirebaseUid == null) {
                return@withContext ResultWrapper.Error(Exception("User tidak terautentikasi untuk membuat item."))
            }

            try {
                val response = apiService.createItem(formattedToken, itemCreateRequest)
                if (response.isSuccessful) {
                    val createdItemDto = response.body()
                    if (createdItemDto != null) {
                        // Konversi DTO ke Entity dan simpan ke Room
                        val newItemEntity = ItemEntity(
                            id = createdItemDto.id, // Gunakan ID dari server
                            userId = currentFirebaseUid, // Pastikan userId adalah UID Firebase yang benar
                            name = createdItemDto.name,
                            description = createdItemDto.description,
                            quantity = createdItemDto.quantity,
                            // createdAt & updatedAt bisa diambil dari DTO jika ada, atau default
                            createdAt = System.currentTimeMillis(), // Atau parse dari DTO jika ada
                            updatedAt = System.currentTimeMillis()  // Atau parse dari DTO jika ada
                        )
                        itemDao.insertItem(newItemEntity)
                        Log.d("ItemRepository", "Item created on server and saved to Room: ${newItemEntity.id}")
                        ResultWrapper.Success(newItemEntity)
                    } else {
                        ResultWrapper.Error(Exception("Gagal membuat item: Respons body kosong."))
                    }
                } else {
                    Log.e("ItemRepository", "Error creating item: ${response.code()} - ${response.errorBody()?.string()}")
                    ResultWrapper.Error(Exception("Gagal membuat item di server: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("ItemRepository", "Exception creating item", e)
                ResultWrapper.Error(e)
            }
        }
    }

    suspend fun updateItem(itemId: Int, itemUpdateRequest: ItemUpdateRequest): ResultWrapper<ItemEntity> {
        return withContext(Dispatchers.IO) {
            val idToken = getCurrentUserIdToken()
            val formattedToken = getFormattedToken(idToken)
            val currentFirebaseUid = firebaseAuth.currentUser?.uid

            if (formattedToken == null || currentFirebaseUid == null) {
                return@withContext ResultWrapper.Error(Exception("User tidak terautentikasi untuk update item."))
            }

            try {
                val response = apiService.updateItem(formattedToken, itemId, itemUpdateRequest)
                if (response.isSuccessful) {
                    val updatedItemDto = response.body()
                    if (updatedItemDto != null) {
                        val updatedItemEntity = ItemEntity(
                            id = updatedItemDto.id,
                            userId = currentFirebaseUid, // Pastikan ini benar
                            name = updatedItemDto.name,
                            description = updatedItemDto.description,
                            quantity = updatedItemDto.quantity,
                            updatedAt = System.currentTimeMillis() // Atau parse dari DTO
                        )
                        itemDao.updateItem(updatedItemEntity) // Update di Room
                        Log.d("ItemRepository", "Item updated on server and in Room: ${updatedItemEntity.id}")
                        ResultWrapper.Success(updatedItemEntity)
                    } else {
                        ResultWrapper.Error(Exception("Gagal update item: Respons body kosong."))
                    }
                } else {
                    Log.e("ItemRepository", "Error updating item: ${response.code()} - ${response.errorBody()?.string()}")
                    ResultWrapper.Error(Exception("Gagal update item di server: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("ItemRepository", "Exception updating item", e)
                ResultWrapper.Error(e)
            }
        }
    }

    suspend fun deleteItem(itemId: Int): ResultWrapper<Unit> {
        return withContext(Dispatchers.IO) {
            val idToken = getCurrentUserIdToken()
            val formattedToken = getFormattedToken(idToken)
            val currentFirebaseUid = firebaseAuth.currentUser?.uid

            if (formattedToken == null || currentFirebaseUid == null) {
                return@withContext ResultWrapper.Error(Exception("User tidak terautentikasi untuk hapus item."))
            }

            try {
                val response = apiService.deleteItem(formattedToken, itemId)
                if (response.isSuccessful) {
                    itemDao.deleteItemById(itemId, currentFirebaseUid) // Hapus dari Room
                    Log.d("ItemRepository", "Item deleted on server and from Room: $itemId")
                    ResultWrapper.Success(Unit)
                } else {
                    Log.e("ItemRepository", "Error deleting item: ${response.code()} - ${response.errorBody()?.string()}")
                    ResultWrapper.Error(Exception("Gagal hapus item di server: ${response.code()} - ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("ItemRepository", "Exception deleting item", e)
                ResultWrapper.Error(e)
            }
        }
    }

    // Fungsi deleteAllItemsForUser dan searchItemsByName mungkin juga perlu interaksi API jika diperlukan
    // Untuk saat ini, kita biarkan mereka beroperasi hanya pada data lokal (Room)
    // atau Anda bisa menambahkan logika serupa untuk sinkronisasi/pencarian via API.

    suspend fun deleteAllItemsForUser(userId: String) {
        // TODO: Implementasi penghapusan di server jika diperlukan, lalu di lokal.
        // Untuk saat ini, hanya lokal.
        withContext(Dispatchers.IO) {
            itemDao.deleteAllItemsForUser(userId)
        }
    }

    fun searchItemsByName(query: String, userId: String): Flow<List<ItemEntity>> {
        // TODO: Implementasi pencarian di server jika diperlukan.
        // Untuk saat ini, hanya lokal.
        return itemDao.searchItemsByName(query, userId).flowOn(Dispatchers.IO)
    }
}

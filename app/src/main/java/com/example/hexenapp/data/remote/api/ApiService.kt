package com.example.hexenapp.data.remote.api

import com.example.hexenapp.data.remote.dto.ItemCreateRequest
import com.example.hexenapp.data.remote.dto.ItemResponse
import com.example.hexenapp.data.remote.dto.ItemUpdateRequest
import retrofit2.Response // Import Response dari Retrofit untuk mendapatkan detail HTTP response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Interface untuk mendefinisikan endpoint API HexenApp menggunakan Retrofit.
 */
interface ApiService {

    // --- Item Endpoints ---

    /**
     * Mendapatkan semua item milik pengguna yang terautentikasi.
     * Endpoint: GET /api/items
     * Memerlukan Firebase ID Token di header Authorization.
     */
    @GET("items") // Path relatif terhadap Base URL
    suspend fun getItems(
        @Header("Authorization") token: String // "Bearer YOUR_FIREBASE_ID_TOKEN"
    ): Response<List<ItemResponse>> // Mengembalikan List dari ItemResponse

    /**
     * Membuat item baru.
     * Endpoint: POST /api/items
     * Memerlukan Firebase ID Token di header Authorization.
     *
     * @param token Firebase ID Token.
     * @param itemData Data item yang akan dibuat.
     * @return ItemResponse dari item yang baru dibuat.
     */
    @POST("items")
    suspend fun createItem(
        @Header("Authorization") token: String,
        @Body itemData: ItemCreateRequest
    ): Response<ItemResponse>

    /**
     * Mendapatkan detail satu item berdasarkan ID.
     * Endpoint: GET /api/items/{id}
     * Memerlukan Firebase ID Token di header Authorization.
     *
     * @param token Firebase ID Token.
     * @param itemId ID dari item yang akan diambil.
     * @return ItemResponse dari item yang diminta.
     */
    @GET("items/{id}")
    suspend fun getItemById(
        @Header("Authorization") token: String,
        @Path("id") itemId: Int // Menggunakan @Path untuk mengganti {id} di URL
    ): Response<ItemResponse>

    /**
     * Memperbarui item yang sudah ada.
     * Endpoint: PUT /api/items/{id}
     * Memerlukan Firebase ID Token di header Authorization.
     *
     * @param token Firebase ID Token.
     * @param itemId ID dari item yang akan diperbarui.
     * @param itemData Data item yang akan diperbarui.
     * @return ItemResponse dari item yang sudah diperbarui.
     */
    @PUT("items/{id}")
    suspend fun updateItem(
        @Header("Authorization") token: String,
        @Path("id") itemId: Int,
        @Body itemData: ItemUpdateRequest
    ): Response<ItemResponse>

    /**
     * Menghapus item.
     * Endpoint: DELETE /api/items/{id}
     * Memerlukan Firebase ID Token di header Authorization.
     *
     * @param token Firebase ID Token.
     * @param itemId ID dari item yang akan dihapus.
     * @return Response kosong jika berhasil.
     */
    @DELETE("items/{id}")
    suspend fun deleteItem(
        @Header("Authorization") token: String,
        @Path("id") itemId: Int
    ): Response<Unit> // Response<Unit> untuk response tanpa body (misalnya 204 No Content)

    // Anda bisa menambahkan endpoint lain di sini jika diperlukan
    // misalnya untuk autentikasi pengguna dengan backend (jika tidak hanya mengandalkan Firebase)
    // atau endpoint lain yang spesifik.
}

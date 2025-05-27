package com.example.hexenapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object (DTO) untuk merepresentasikan respons item dari API.
 * Nama field di sini harus cocok dengan nama field di JSON respons dari API Laravel Anda.
 * Anotasi @SerializedName digunakan jika nama field di JSON berbeda dengan nama variabel di Kotlin.
 */
data class ItemResponse(
    @SerializedName("id") // Cocokkan dengan field 'id' di JSON respons Laravel
    val id: Int,

    @SerializedName("user_id") // Cocokkan dengan field 'user_id' di JSON respons Laravel
    val userId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String?, // Bisa null jika deskripsi opsional

    @SerializedName("quantity")
    val quantity: Int,

    @SerializedName("created_at") // Format timestamp dari Laravel (misalnya "2023-10-27T10:30:00.000000Z")
    val createdAt: String?, // Simpan sebagai String, bisa di-parse ke Date/LocalDateTime nanti jika perlu

    @SerializedName("updated_at")
    val updatedAt: String?
)

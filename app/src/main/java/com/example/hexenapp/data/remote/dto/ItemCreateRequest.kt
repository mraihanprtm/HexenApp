package com.example.hexenapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object (DTO) untuk request membuat item baru.
 * Field di sini adalah yang akan dikirim ke API Laravel.
 * user_id tidak perlu dikirim dari klien karena akan diambil dari token di backend.
 */
data class ItemCreateRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("quantity")
    val quantity: Int? // Bisa null jika backend memiliki default atau opsional
)

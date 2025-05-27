package com.example.hexenapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object (DTO) untuk request memperbarui item.
 * Semua field biasanya opsional, karena pengguna mungkin hanya ingin mengupdate beberapa field.
 */
data class ItemUpdateRequest(
    @SerializedName("name")
    val name: String? = null, // Opsional

    @SerializedName("description")
    val description: String? = null, // Opsional

    @SerializedName("quantity")
    val quantity: Int? = null // Opsional
)

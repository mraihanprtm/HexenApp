package com.example.hexenapp.data.local.model

import androidx.room.Entity
import androidx.room.Index // Import Index
import androidx.room.PrimaryKey

// Menandakan bahwa kelas ini adalah sebuah tabel database.
// Tambahkan index pada kolom userId untuk performa query yang lebih baik
@Entity(
    tableName = "items",
    indices = [Index(value = ["userId"])] // Membuat index pada kolom userId
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val userId: String, // ID pengguna dari Firebase Auth yang memiliki item ini

    val name: String,
    val description: String?,
    val quantity: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

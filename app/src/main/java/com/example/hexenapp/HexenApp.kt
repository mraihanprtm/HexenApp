package com.example.hexenapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp // Import anotasi Hilt

@HiltAndroidApp // Tambahkan anotasi ini
class HexenApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inisialisasi global lainnya bisa tetap di sini
        // Timber.plant(Timber.DebugTree()) // Contoh jika menggunakan Timber
    }
}

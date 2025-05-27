package com.example.hexenapp.util

object Constants {
    // Tag untuk logging
    const val TAG = "HexenAppTag"

    // Nama Database Lokal (jika menggunakan Room)
    const val DATABASE_NAME = "hexen_app_db"

    // Base URL untuk API Backend Laravel
    // Menggunakan alamat IP lokal laptop Anda di jaringan WiFi yang sama.
    // Pastikan server Laravel Anda berjalan di port 8000 (atau port lain jika Anda mengubahnya).
    const val BASE_URL_API = "http://40.82.213.209/api/" // IP Anda sudah dimasukkan
    // Jika masih menggunakan emulator, Anda bisa kembali ke:
    // const val BASE_URL_API = "http://10.0.2.2:8000/api/"


    // Preferensi (jika menggunakan DataStore atau SharedPreferences)
    const val PREFS_NAME = "hexen_app_prefs"
    const val PREF_KEY_USER_TOKEN = "user_token"
    const val PREF_KEY_IS_LOGGED_IN = "is_logged_in"

    // Delay untuk Splash Screen (dalam milidetik)
    const val SPLASH_SCREEN_DELAY = 2000L

    // Batas waktu (timeout) untuk koneksi jaringan (dalam detik)
    const val NETWORK_TIMEOUT_SECONDS = 30L
}

package com.example.hexenapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
// import androidx.room.migration.Migration // Import jika Anda membuat migrasi manual
// import androidx.sqlite.db.SupportSQLiteDatabase // Import jika Anda membuat migrasi manual
import com.example.hexenapp.data.local.dao.ItemDao
import com.example.hexenapp.data.local.model.ItemEntity
import com.example.hexenapp.util.Constants

@Database(
    entities = [ItemEntity::class],
    version = 2, // Naikkan versi database menjadi 2
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Definisikan migrasi jika diperlukan (contoh untuk produksi)
        // val MIGRATION_1_2 = object : Migration(1, 2) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         // Tambahkan kolom userId ke tabel items
        //         db.execSQL("ALTER TABLE items ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
        //         // Buat index baru untuk userId (opsional jika sudah didefinisikan di Entity)
        //         // db.execSQL("CREATE INDEX IF NOT EXISTS index_items_userId ON items(userId)")
        //     }
        // }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    // Untuk pengembangan, fallbackToDestructiveMigration() cukup.
                    // Ini akan menghapus dan membuat ulang database jika versi berubah tanpa migrasi.
                    // Data akan hilang.
                    .fallbackToDestructiveMigration()
                    // Untuk produksi, gunakan .addMigrations(MIGRATION_1_2, ...)
                    // .addMigrations(MIGRATION_1_2) // Aktifkan ini jika Anda membuat migrasi manual
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

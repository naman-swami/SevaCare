package com.saathi.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.saathi.app.data.model.Medicine
import com.saathi.app.data.model.ScamPhrase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Medicine::class, ScamPhrase::class],
    version = 1,
    exportSchema = false
)
abstract class SaathiDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao
    abstract fun scamPhraseDao(): ScamPhraseDao

    companion object {
        @Volatile
        private var INSTANCE: SaathiDatabase? = null

        fun getInstance(context: Context): SaathiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SaathiDatabase::class.java,
                    "saathi_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate on first run in background
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    DatabaseSeeder.seed(database)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

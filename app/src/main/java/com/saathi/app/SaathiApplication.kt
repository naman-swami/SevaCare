package com.saathi.app

import android.app.Application
import com.saathi.app.data.db.DatabaseSeeder
import com.saathi.app.data.db.SaathiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SaathiApplication : Application() {

    val database: SaathiDatabase by lazy { SaathiDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()

        // Seed the database with medicines and scam phrases on first launch
        CoroutineScope(Dispatchers.IO).launch {
            val medicineDao = database.medicineDao()
            val scamPhraseDao = database.scamPhraseDao()

            if (medicineDao.getAllMedicines().isEmpty()) {
                medicineDao.insertAll(DatabaseSeeder.medicines)
                scamPhraseDao.insertAll(DatabaseSeeder.scamPhrases)
            }
        }
    }
}

package com.sevacare.app

import android.app.Application
import com.sevacare.app.data.db.DatabaseSeeder
import com.sevacare.app.data.db.SevaCareDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SevaCareApplication : Application() {

    val database: SevaCareDatabase by lazy { SevaCareDatabase.getInstance(this) }

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

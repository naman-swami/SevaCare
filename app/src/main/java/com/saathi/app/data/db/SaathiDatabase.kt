package com.saathi.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.saathi.app.data.model.ChecklistItemEntity
import com.saathi.app.data.model.CircleContact
import com.saathi.app.data.model.ConflictReportEntity
import com.saathi.app.data.model.DocumentEntity
import com.saathi.app.data.model.DrugInteractionEntity
import com.saathi.app.data.model.ExpiredMedicineEntity
import com.saathi.app.data.model.Medicine
import com.saathi.app.data.model.PrivacyLedgerEntry
import com.saathi.app.data.model.ScamPhrase
import com.saathi.app.data.model.ScheduleEntity
import com.saathi.app.data.model.StoryEntity
import com.saathi.app.data.model.SuspiciousMedicineEntity
import com.saathi.app.data.pairing.FamilyPairEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Medicine::class,
        ScamPhrase::class,
        CircleContact::class,
        PrivacyLedgerEntry::class,
        FamilyPairEntity::class,
        StoryEntity::class,
        DocumentEntity::class,
        ChecklistItemEntity::class,
        DrugInteractionEntity::class,
        SuspiciousMedicineEntity::class,
        ScheduleEntity::class,
        ConflictReportEntity::class,
        ExpiredMedicineEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(TimeConverters::class)
abstract class SaathiDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao
    abstract fun scamPhraseDao(): ScamPhraseDao
    abstract fun circleContactDao(): CircleContactDao
    abstract fun privacyLedgerDao(): PrivacyLedgerDao
    abstract fun familyPairDao(): FamilyPairDao
    abstract fun storyDao(): StoryDao
    abstract fun lifeAdminDao(): LifeAdminDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun conflictDao(): ConflictDao
    abstract fun expiredMedicineDao(): ExpiredMedicineDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

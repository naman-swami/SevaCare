package com.saathi.app.data.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.saathi.app.data.ai.ExpiryEngine
import com.saathi.app.data.db.SaathiDatabase
import com.saathi.app.data.model.ExpiredMedicineEntity
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * WorkManager CoroutineWorker that moves due-passed medicine schedule rows
 * into the history log with outcome EXPIRED.
 *
 * Uses real [LocalDateTime] timezone-aware comparisons.
 */
class MedicineExpiryWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val db = SaathiDatabase.getInstance(applicationContext)
        val scheduleDao = db.scheduleDao()
        val expiredDao = db.expiredMedicineDao()

        // 1. Direct SQL time comparison for rows with dueEpochMs
        val sqlPassed = scheduleDao.findDueBefore(now)
        // 2. Semantic string evaluation fallback for legacy rows
        val allActive = scheduleDao.getActiveSchedules()
        val enginePassed = allActive.filter { ExpiryEngine.isDuePassed(it, now) }

        val passed = (sqlPassed + enginePassed).distinctBy { it.id }

        passed.forEach { row ->
            scheduleDao.delete(row)
            expiredDao.insert(ExpiredMedicineEntity.from(row, expiredAt = now, outcome = "EXPIRED"))
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME_PERIODIC = "medicine_expiry_periodic"
        const val WORK_NAME_EXPEDITED = "medicine_expiry_expedited"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<MedicineExpiryWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleExpedited(context: Context, delayMs: Long = 0) {
            val builder = OneTimeWorkRequestBuilder<MedicineExpiryWorker>()
            if (delayMs > 0) {
                builder.setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            }
            WorkManager.getInstance(context).enqueue(builder.build())
        }
    }
}

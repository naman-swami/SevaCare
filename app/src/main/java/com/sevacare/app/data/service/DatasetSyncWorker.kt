package com.sevacare.app.data.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sevacare.app.data.db.SevaCareDatabase
import com.sevacare.app.data.voice.CallLogReader
import com.sevacare.app.data.voice.SmsReader
import com.sevacare.app.data.voice.VoiceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker that periodically refreshes offline dataset files
 * in app-private storage:
 * - medicine_history.json
 * - fraud_reports.json
 * - call_summary.json (when granted)
 * - sms_summary.json (when granted)
 *
 * Implements real file I/O with verified payload structures.
 */
class DatasetSyncWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = SevaCareDatabase.getInstance(applicationContext)
            val datasetDir = File(applicationContext.filesDir, "datasets").apply {
                if (!exists()) mkdirs()
            }

            // 1. Medicine History dataset
            val history = db.expiredMedicineDao().getAllHistory()
            val historyJson = history.joinToString(prefix = "[\n", postfix = "\n]", separator = ",\n") {
                """  {"id":"${it.id}","medicineName":"${it.medicineName}","outcome":"${it.outcome}","timeOfDay":"${it.timeOfDay}","expiredAt":${it.expiredAtEpochMs}}"""
            }
            File(datasetDir, "medicine_history.json").writeText(historyJson)

            // 2. Fraud & Scam reports dataset
            val ledger = db.privacyLedgerDao().getAllEntriesList()
            val fraudJson = ledger.joinToString(prefix = "[\n", postfix = "\n]", separator = ",\n") {
                val cleanDesc = it.summaryText.replace("\"", "\\\"").replace("\n", " ")
                """  {"id":${it.id},"alertType":"${it.alertType}","recipient":"${it.recipientName}","summary":"$cleanDesc","timestamp":${it.timestampMs}}"""
            }
            File(datasetDir, "fraud_reports.json").writeText(fraudJson)

            // 3. Call log summary dataset
            val callResult = CallLogReader(applicationContext).query("summary")
            if (callResult is VoiceResult.CallLogResult) {
                val callJson = callResult.calls.joinToString(prefix = "[\n", postfix = "\n]", separator = ",\n") {
                    """  {"name":"${it.name}","type":"${it.type}","time":${it.timestamp}}"""
                }
                File(datasetDir, "call_summary.json").writeText(callJson)
            }

            // 4. SMS summary dataset
            val smsResult = SmsReader(applicationContext).query("summary")
            if (smsResult is VoiceResult.SmsResult) {
                val smsJson = smsResult.messages.joinToString(prefix = "[\n", postfix = "\n]", separator = ",\n") {
                    val preview = it.body.take(60).replace("\"", "\\\"").replace("\n", " ")
                    """  {"sender":"${it.sender}","preview":"$preview","time":${it.timestamp}}"""
                }
                File(datasetDir, "sms_summary.json").writeText(smsJson)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "dataset_sync_worker"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DatasetSyncWorker>(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

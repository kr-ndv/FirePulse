package com.firepulse.app.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.firepulse.app.data.local.FirePulseDatabase
import com.firepulse.app.data.local.toEntity
import com.firepulse.app.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Refreshes the default India snapshot when the device has an unmetered or
 * metered network connection. This is best-effort background freshness,
 * not an emergency monitoring or alerting service.
 */
class FireRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val source = DEFAULT_SOURCE
        val days = DEFAULT_DAYS
        val response = withContext(Dispatchers.IO) {
            ApiClient.firePulseApi.getFires(
                west = INDIA_WEST,
                south = INDIA_SOUTH,
                east = INDIA_EAST,
                north = INDIA_NORTH,
                days = days,
                source = source
            )
        }

        if (response.data_status == "live") {
            val dao = FirePulseDatabase.getInstance(applicationContext).fireObservationDao()
            val now = System.currentTimeMillis()
            withContext(Dispatchers.IO) {
                dao.replaceSnapshot(
                    source = source,
                    days = days,
                    items = response.observations.map { it.toEntity(now, source, days) }
                )
                dao.deleteOlderThan(now - CACHE_RETENTION_MILLIS)
            }
        }
        Result.success()
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        Result.retry()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "firepulse-periodic-fire-refresh"
        const val DEFAULT_SOURCE = "VIIRS_SNPP_NRT"
        const val DEFAULT_DAYS = 5

        private const val INDIA_WEST = 68.0
        private const val INDIA_SOUTH = 6.0
        private const val INDIA_EAST = 98.0
        private const val INDIA_NORTH = 37.5
        private const val CACHE_RETENTION_MILLIS = 30L * 24L * 60L * 60L * 1000L
    }
}

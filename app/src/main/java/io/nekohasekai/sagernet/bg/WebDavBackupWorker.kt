package io.nekohasekai.sagernet.bg

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy.UPDATE
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteWorkManager
import io.nekohasekai.sagernet.backup.BackupCrypto
import io.nekohasekai.sagernet.backup.BackupManager
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.app
import io.nekohasekai.sagernet.sync.WebDavClient
import io.nekohasekai.sagernet.sync.WebDavException
import io.nekohasekai.sagernet.sync.WebDavSettings
import io.nekohasekai.sagernet.sync.WebDavTransferLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

object WebDavBackupScheduler {

    private const val WORK_NAME = "WebDavBackup"

    fun reconfigure() {
        val config = WebDavSettings.load()
        if (!config.autoBackup || !config.isComplete) {
            RemoteWorkManager.getInstance(app).cancelUniqueWork(WORK_NAME)
            return
        }
        val networkType = if (config.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = PeriodicWorkRequest.Builder(
            WebDavBackupWorker::class.java,
            6,
            TimeUnit.HOURS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(networkType)
                    .build()
            )
            .build()
        RemoteWorkManager.getInstance(app).enqueueUniquePeriodicWork(
            WORK_NAME,
            UPDATE,
            request,
        )
    }

    fun cancel() = RemoteWorkManager.getInstance(app).cancelUniqueWork(WORK_NAME)
}

class WebDavBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val jobContext = currentCoroutineContext()
        return@withContext try {
            WebDavTransferLock.withLock {
                jobContext.ensureActive()
                if (WebDavSettings.isManualRestorePending()) {
                    return@withLock Result.success()
                }
                val config = WebDavSettings.load()
                if (!config.autoBackup || !config.isComplete) {
                    return@withLock Result.success()
                }
                val backup = BackupManager.createBackup(profile = true, rule = true, setting = true)
                val encrypted = BackupCrypto.encrypt(backup, config.backupPassword)
                jobContext.ensureActive()
                if (WebDavSettings.isManualRestorePending() || WebDavSettings.load() != config) {
                    return@withLock Result.success()
                }
                WebDavClient(config).upload(encrypted)
                WebDavSettings.markSynced()
                Result.success()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Logs.w(error)
            when (error) {
                is WebDavException -> if (error.isRetryable()) Result.retry() else Result.failure()
                is IOException -> Result.retry()
                else -> Result.failure()
            }
        }
    }

    private fun WebDavException.isRetryable(): Boolean {
        val code = statusCode ?: return false
        return code in setOf(408, 409, 423, 425, 429) || code in 500..599
    }
}

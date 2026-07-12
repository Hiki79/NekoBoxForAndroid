package io.nekohasekai.sagernet.ui

import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.backup.BackupManager
import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.databinding.LayoutImportBinding
import io.nekohasekai.sagernet.databinding.LayoutProgressBinding
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.app
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.ktx.runOnIoDispatcher
import io.nekohasekai.sagernet.ktx.triggerFullRestart
import io.nekohasekai.sagernet.sync.WebDavTransferLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

object BackupRestoreDialog {

    fun show(
        activity: FragmentActivity,
        content: JSONObject,
        onFinished: () -> Unit = {},
    ) {
        val finished = AtomicBoolean(false)
        fun finishOnce() {
            if (finished.compareAndSet(false, true)) {
                runCatching(onFinished).onFailure { Logs.w(it) }
            }
        }

        if (activity.isFinishing || activity.isDestroyed) {
            finishOnce()
            return
        }

        val choices = LayoutImportBinding.inflate(activity.layoutInflater).apply {
            backupConfigurations.isVisible = content.has("profiles") && content.has("groups")
            backupRules.isVisible = content.has("rules")
            backupSettings.isVisible = content.has("settings")
        }
        var restoreStarted = false

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.backup_import)
            .setView(choices.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.backup_import) { _, _ ->
                restoreStarted = true
                val profile = choices.backupConfigurations.isChecked
                val rule = choices.backupRules.isChecked
                val setting = choices.backupSettings.isChecked

                val progress = LayoutProgressBinding.inflate(activity.layoutInflater).apply {
                    content.text = activity.getString(R.string.backup_importing)
                }
                val progressDialog = MaterialAlertDialogBuilder(activity)
                    .setView(progress.root)
                    .setCancelable(false)
                    .show()

                runOnIoDispatcher {
                    val result = runCatching {
                        SagerNet.stopService()
                        val stopped = withTimeoutOrNull(SERVICE_STOP_TIMEOUT_MILLIS) {
                            while (DataStore.serviceState != BaseService.State.Stopped &&
                                DataStore.serviceState != BaseService.State.Idle
                            ) {
                                delay(100L)
                            }
                            true
                        } ?: false
                        check(stopped) { app.getString(R.string.backup_stop_timeout) }
                        WebDavTransferLock.withLock {
                            BackupManager.restore(content, profile, rule, setting)
                        }
                    }

                    finishOnce()
                    onMainDispatcher {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            progressDialog.dismiss()
                            result.fold(
                                onSuccess = { triggerFullRestart(app) },
                                onFailure = { error ->
                                    Logs.w(error)
                                    val restarted = AtomicBoolean(false)
                                    fun restartOnce() {
                                        if (restarted.compareAndSet(false, true)) {
                                            triggerFullRestart(app)
                                        }
                                    }
                                    MaterialAlertDialogBuilder(activity)
                                        .setTitle(R.string.error_title)
                                        .setMessage(error.readableMessage)
                                        .setPositiveButton(android.R.string.ok) { _, _ ->
                                            restartOnce()
                                        }
                                        .setOnDismissListener { restartOnce() }
                                        .show()
                                },
                            )
                        } else {
                            result.exceptionOrNull()?.let { Logs.w(it) }
                            triggerFullRestart(app)
                        }
                    }
                }
            }
            .setOnDismissListener {
                if (!restoreStarted) finishOnce()
            }
            .show()
    }

    private const val SERVICE_STOP_TIMEOUT_MILLIS = 15_000L
}

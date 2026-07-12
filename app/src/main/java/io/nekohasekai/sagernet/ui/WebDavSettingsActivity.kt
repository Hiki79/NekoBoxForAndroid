package io.nekohasekai.sagernet.ui

import android.net.Uri
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.backup.BackupCrypto
import io.nekohasekai.sagernet.backup.BackupManager
import io.nekohasekai.sagernet.bg.WebDavBackupScheduler
import io.nekohasekai.sagernet.databinding.LayoutWebdavSettingsBinding
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.ktx.runOnIoDispatcher
import io.nekohasekai.sagernet.sync.WebDavClient
import io.nekohasekai.sagernet.sync.WebDavConfig
import io.nekohasekai.sagernet.sync.WebDavSettings
import io.nekohasekai.sagernet.sync.WebDavTransferLock
import java.util.Date

class WebDavSettingsActivity : ThemedActivity() {

    private lateinit var binding: LayoutWebdavSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LayoutWebdavSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setTitle(R.string.webdav_sync)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24)
        }

        bindSettings(WebDavSettings.load())
        updateLastSync()

        binding.saveSettings.setOnClickListener {
            if (saveSettings() != null) {
                binding.operationStatus.setText(R.string.webdav_settings_saved)
                snackbar(R.string.webdav_settings_saved).show()
            }
        }
        binding.testConnection.setOnClickListener {
            launchOperation(
                progressText = R.string.webdav_testing,
                successText = R.string.webdav_connection_success,
                action = { WebDavClient(it).testConnection() },
            )
        }
        binding.uploadNow.setOnClickListener {
            launchOperation(
                progressText = R.string.webdav_uploading,
                successText = R.string.webdav_upload_success,
                action = { config ->
                    WebDavTransferLock.withLock {
                        val backup = BackupManager.createBackup(
                            profile = true,
                            rule = true,
                            setting = true,
                        )
                        WebDavClient(config).upload(
                            BackupCrypto.encrypt(backup, config.backupPassword)
                        )
                        WebDavSettings.markSynced()
                    }
                },
                onSuccess = { updateLastSync() },
            )
        }
        binding.restoreBackup.setOnClickListener {
            launchOperation(
                progressText = R.string.webdav_downloading,
                action = { config ->
                    WebDavSettings.beginManualRestore()
                    try {
                        WebDavBackupScheduler.cancel().get()
                        WebDavTransferLock.withLock {
                            val encrypted = WebDavClient(config).download()
                            BackupManager.parse(
                                BackupCrypto.decrypt(encrypted, config.backupPassword)
                            )
                        }
                    } catch (error: Throwable) {
                        finishManualRestore()
                        throw error
                    }
                },
                onSuccess = { content ->
                    BackupRestoreDialog.show(this, content, ::finishManualRestore)
                },
                onDiscarded = { finishManualRestore() },
            )
        }
    }

    private fun bindSettings(config: WebDavConfig) = with(binding) {
        serverUrl.setText(config.serverUrl)
        username.setText(config.username)
        appPassword.setText(config.appPassword)
        backupPassword.setText(config.backupPassword)
        remotePath.setText(config.remotePath)
        automaticBackup.isChecked = config.autoBackup
        wifiOnly.isChecked = config.wifiOnly
    }

    private fun saveSettings(): WebDavConfig? {
        val config = readSettings() ?: return null
        return runCatching {
            WebDavSettings.save(config)
            WebDavBackupScheduler.reconfigure()
            config
        }.getOrElse { error ->
            Logs.w(error)
            snackbar(error.readableMessage).show()
            null
        }
    }

    private fun readSettings(): WebDavConfig? {
        clearErrors()

        val server = binding.serverUrl.text?.toString().orEmpty().trim()
        val username = binding.username.text?.toString().orEmpty().trim()
        val appPassword = binding.appPassword.text?.toString().orEmpty()
        val backupPassword = binding.backupPassword.text?.toString().orEmpty()
        val remotePath = binding.remotePath.text?.toString().orEmpty().trim()
        var valid = true

        val serverUri = runCatching { Uri.parse(server) }.getOrNull()
        val validServer = serverUri != null &&
            serverUri.scheme.equals("https", ignoreCase = true) &&
            !serverUri.host.isNullOrBlank() &&
            serverUri.encodedUserInfo == null &&
            serverUri.encodedQuery == null &&
            serverUri.encodedFragment == null
        if (!validServer) {
            binding.serverUrlLayout.fail(R.string.webdav_requires_https)
            valid = false
        }
        if (username.isBlank()) {
            binding.usernameLayout.fail(R.string.webdav_required)
            valid = false
        }
        if (appPassword.isBlank()) {
            binding.appPasswordLayout.fail(R.string.webdav_required)
            valid = false
        } else if (appPassword.toByteArray(Charsets.UTF_8).size > MAX_CREDENTIAL_BYTES) {
            binding.appPasswordLayout.fail(R.string.webdav_credential_too_long)
            valid = false
        }
        if (backupPassword.length < MIN_BACKUP_PASSWORD_LENGTH) {
            binding.backupPasswordLayout.fail(R.string.webdav_password_too_short)
            valid = false
        } else if (backupPassword.toByteArray(Charsets.UTF_8).size > MAX_CREDENTIAL_BYTES) {
            binding.backupPasswordLayout.fail(R.string.webdav_credential_too_long)
            valid = false
        }

        val pathSegments = remotePath.trim('/').split('/').filter(String::isNotBlank)
        if (pathSegments.isEmpty() || pathSegments.any { it == "." || it == ".." }) {
            binding.remotePathLayout.fail(R.string.webdav_invalid_remote_path)
            valid = false
        }

        if (!valid) return null
        return WebDavConfig(
            serverUrl = server,
            username = username,
            appPassword = appPassword,
            backupPassword = backupPassword,
            remotePath = remotePath,
            autoBackup = binding.automaticBackup.isChecked,
            wifiOnly = binding.wifiOnly.isChecked,
        )
    }

    private fun clearErrors() = with(binding) {
        serverUrlLayout.error = null
        usernameLayout.error = null
        appPasswordLayout.error = null
        backupPasswordLayout.error = null
        remotePathLayout.error = null
    }

    private fun TextInputLayout.fail(@StringRes message: Int) {
        error = getString(message)
        requestFocus()
    }

    private fun <T> launchOperation(
        @StringRes progressText: Int,
        @StringRes successText: Int? = null,
        action: (WebDavConfig) -> T,
        onSuccess: (T) -> Unit = {},
        onDiscarded: (T) -> Unit = {},
    ) {
        val config = readSettings() ?: return
        setBusy(true, progressText)
        runOnIoDispatcher {
            val result = runCatching { action(config) }
            onMainDispatcher {
                if (isFinishing || isDestroyed) {
                    result.getOrNull()?.let(onDiscarded)
                    return@onMainDispatcher
                }
                setBusy(false)
                result.fold(
                    onSuccess = { value ->
                        if (successText != null) {
                            binding.operationStatus.setText(successText)
                            snackbar(successText).show()
                        }
                        onSuccess(value)
                    },
                    onFailure = { error ->
                        Logs.w(error)
                        binding.operationStatus.text = getString(
                            R.string.webdav_operation_failed,
                            error.readableMessage,
                        )
                        snackbar(error.readableMessage).show()
                    },
                )
            }
        }
    }

    private fun finishManualRestore() {
        runCatching(WebDavSettings::endManualRestore).onFailure { Logs.w(it) }
        runCatching(WebDavBackupScheduler::reconfigure).onFailure { Logs.w(it) }
    }

    private fun setBusy(busy: Boolean, @StringRes status: Int? = null) = with(binding) {
        progress.isVisible = busy
        saveSettings.isEnabled = !busy
        testConnection.isEnabled = !busy
        uploadNow.isEnabled = !busy
        restoreBackup.isEnabled = !busy
        if (status != null) operationStatus.setText(status)
    }

    private fun updateLastSync() {
        val timestamp = WebDavSettings.lastSync()
        val value = if (timestamp == 0L) {
            getString(R.string.webdav_never)
        } else {
            val date = Date(timestamp)
            "${android.text.format.DateFormat.getMediumDateFormat(this).format(date)} " +
                android.text.format.DateFormat.getTimeFormat(this).format(date)
        }
        binding.lastSync.text = getString(R.string.webdav_last_sync, value)
    }

    override fun snackbarInternal(text: CharSequence): Snackbar =
        Snackbar.make(binding.coordinator, text, Snackbar.LENGTH_LONG)

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private companion object {
        const val MIN_BACKUP_PASSWORD_LENGTH = 8
        const val MAX_CREDENTIAL_BYTES = 190
    }
}

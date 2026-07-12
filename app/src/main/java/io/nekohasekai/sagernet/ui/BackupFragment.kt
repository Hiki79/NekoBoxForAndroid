package io.nekohasekai.sagernet.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import io.nekohasekai.sagernet.BuildConfig
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.backup.BackupManager
import io.nekohasekai.sagernet.bg.WebDavBackupScheduler
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.databinding.LayoutBackupBinding
import io.nekohasekai.sagernet.databinding.LayoutProgressBinding
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.app
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.ktx.runOnLifecycleDispatcher
import io.nekohasekai.sagernet.ktx.runOnIoDispatcher
import io.nekohasekai.sagernet.ktx.snackbar
import io.nekohasekai.sagernet.ktx.startFilesForResult
import io.nekohasekai.sagernet.ktx.triggerFullRestart
import io.nekohasekai.sagernet.sync.WebDavSettings
import io.nekohasekai.sagernet.sync.WebDavTransferLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupFragment : NamedFragment(R.layout.layout_backup) {

    override fun name0() = app.getString(R.string.backup)

    private var pendingExport = ""

    private val exportSettings =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@registerForActivityResult
            runOnLifecycleDispatcher {
                try {
                    requireContext().contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(pendingExport)
                    } ?: error("Unable to open the selected file")
                    onMainDispatcher {
                        snackbar(getString(R.string.action_export_msg)).show()
                    }
                } catch (error: Exception) {
                    Logs.w(error)
                    onMainDispatcher {
                        snackbar(error.readableMessage).show()
                    }
                }
            }
        }

    private val importFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runOnLifecycleDispatcher { startImport(uri) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutBackupBinding.bind(view)

        binding.actionExport.setOnClickListener {
            val selection = binding.backupSelection()
            runOnLifecycleDispatcher {
                pendingExport = createBackup(selection)
                onMainDispatcher {
                    startFilesForResult(exportSettings, backupFileName())
                }
            }
        }

        binding.actionShare.setOnClickListener {
            val selection = binding.backupSelection()
            runOnLifecycleDispatcher {
                val content = createBackup(selection)
                val cacheFile = File(requireContext().cacheDir, backupFileName()).apply {
                    parentFile?.mkdirs()
                    writeText(content, Charsets.UTF_8)
                }
                onMainDispatcher {
                    startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND)
                                .setType("application/json")
                                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                .putExtra(
                                    Intent.EXTRA_STREAM,
                                    FileProvider.getUriForFile(
                                        requireContext(),
                                        BuildConfig.APPLICATION_ID + ".cache",
                                        cacheFile,
                                    ),
                                ),
                            getString(R.string.abc_shareactionprovider_share_with),
                        )
                    )
                }
            }
        }

        binding.actionImportFile.setOnClickListener {
            startFilesForResult(importFile, "application/json")
        }

        binding.webdavSync.setOnClickListener {
            startActivity(Intent(requireContext(), WebDavSettingsActivity::class.java))
        }

        binding.resetSettings.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm)
                .setMessage(R.string.reset_settings_message_with_cloud)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes) { _, _ ->
                    val activity = requireActivity()
                    val progress = LayoutProgressBinding.inflate(layoutInflater).apply {
                        content.text = getString(R.string.resetting_settings)
                    }
                    val progressDialog =
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
                            .setView(progress.root)
                            .setCancelable(false)
                            .show()
                    runOnIoDispatcher {
                        val result = runCatching {
                            WebDavBackupScheduler.cancel().get()
                            WebDavTransferLock.withLock {
                                WebDavSettings.clear()
                                DataStore.configurationStore.reset()
                            }
                        }
                        if (result.isSuccess) {
                            onMainDispatcher {
                                if (!activity.isFinishing && !activity.isDestroyed) {
                                    progressDialog.dismiss()
                                }
                            }
                            triggerFullRestart(app)
                        } else {
                            val error = result.exceptionOrNull()!!
                            Logs.w(error)
                            onMainDispatcher {
                                if (!activity.isFinishing && !activity.isDestroyed) {
                                    progressDialog.dismiss()
                                    val restarted = java.util.concurrent.atomic.AtomicBoolean(false)
                                    fun restartOnce() {
                                        if (restarted.compareAndSet(false, true)) {
                                            triggerFullRestart(app)
                                        }
                                    }
                                    com.google.android.material.dialog.MaterialAlertDialogBuilder(
                                        activity
                                    )
                                        .setTitle(R.string.error_title)
                                        .setMessage(error.readableMessage)
                                        .setPositiveButton(android.R.string.ok) { _, _ ->
                                            restartOnce()
                                        }
                                        .setOnDismissListener { restartOnce() }
                                        .show()
                                } else {
                                    triggerFullRestart(app)
                                }
                            }
                        }
                    }
                }
                .show()
        }
    }

    private fun LayoutBackupBinding.backupSelection() = BackupSelection(
        profile = backupConfigurations.isChecked,
        rule = backupRules.isChecked,
        setting = backupSettings.isChecked,
    )

    private fun createBackup(selection: BackupSelection) = BackupManager.createBackup(
        profile = selection.profile,
        rule = selection.rule,
        setting = selection.setting,
    )

    private suspend fun startImport(uri: Uri) {
        val fileName = runCatching {
            requireContext().contentResolver.query(uri, null, null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        .takeIf { it >= 0 }
                        ?.let(cursor::getString)
                }
        }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment.orEmpty().substringAfterLast('/').substringAfter(':')

        if (!fileName.endsWith(".json", ignoreCase = true)) {
            onMainDispatcher {
                snackbar(getString(R.string.backup_not_file, fileName)).show()
            }
            return
        }

        val content = try {
            requireContext().contentResolver.openInputStream(uri)?.use(BackupManager::parse)
                ?: error("Unable to open the selected file")
        } catch (error: Exception) {
            Logs.w(error)
            onMainDispatcher {
                snackbar(getString(R.string.invalid_backup_file)).show()
            }
            return
        }

        onMainDispatcher {
            BackupRestoreDialog.show(requireActivity(), content)
        }
    }

    private fun backupFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "nekobox_backup_$timestamp.json"
    }

    private data class BackupSelection(
        val profile: Boolean,
        val rule: Boolean,
        val setting: Boolean,
    )
}

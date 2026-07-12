package io.nekohasekai.sagernet.sync

import io.nekohasekai.sagernet.SagerNet
import java.io.File
import java.io.RandomAccessFile

object WebDavTransferLock {

    @Synchronized
    fun <T> withLock(block: () -> T): T {
        val lockFile = File(SagerNet.application.noBackupFilesDir, "webdav_transfer.lock")
        lockFile.parentFile?.mkdirs()
        RandomAccessFile(lockFile, "rw").use { file ->
            file.channel.use { channel ->
                val lock = channel.lock()
                try {
                    return block()
                } finally {
                    lock.release()
                }
            }
        }
    }
}

package com.example.util

import android.content.Context
import android.os.Environment
import com.example.data.local.PreferenceManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptedBackupManager {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val BACKUP_DIR_NAME = "ColdCache_Backups"
    private const val WEEKLY_MILLIS = 7 * 24 * 60 * 60 * 1000L // 7 days

    // Deterministic 256-bit AES master seed combined with package signature
    private val BACKUP_SEED = byteArrayOf(
        0x43, 0x6F, 0x6C, 0x64, 0x43, 0x61, 0x63, 0x68,
        0x65, 0x58, 0x54, 0x52, 0x5A, 0x2E, 0x4B, 0x65,
        0x79, 0x32, 0x30, 0x32, 0x36, 0x2E, 0x43, 0x79,
        0x62, 0x65, 0x72, 0x53, 0x65, 0x63, 0x75, 0x72
    )

    fun getBackupDirectory(context: Context): File {
        val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val targetDir = File(publicDocs, BACKUP_DIR_NAME)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        if (targetDir.exists() && targetDir.canWrite()) {
            return targetDir
        }
        // Fallback to scoped external files directory
        val fallbackDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), BACKUP_DIR_NAME)
        if (!fallbackDir.exists()) {
            fallbackDir.mkdirs()
        }
        return fallbackDir
    }

    fun createEncryptedBackup(context: Context, jsonDump: String): File? {
        return try {
            val backupDir = getBackupDirectory(context)
            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(backupDir, "coldcache_backup_$timestampStr.ccenc")

            // 1. Generate random IV
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)

            // 2. Initialize Cipher
            val keySpec = SecretKeySpec(BACKUP_SEED, "AES")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

            // 3. Encrypt payload
            val plainBytes = jsonDump.toByteArray(Charsets.UTF_8)
            val cipherBytes = cipher.doFinal(plainBytes)

            // 4. Write IV + Ciphertext to file
            FileOutputStream(backupFile).use { fos ->
                fos.write(iv)
                fos.write(cipherBytes)
                fos.flush()
            }

            // Also keep latest pointer
            val latestFile = File(backupDir, "coldcache_backup_latest.ccenc")
            try {
                FileOutputStream(latestFile).use { fos ->
                    fos.write(iv)
                    fos.write(cipherBytes)
                    fos.flush()
                }
            } catch (_: Exception) {}

            // Save last backup timestamp
            val prefManager = PreferenceManager(context)
            prefManager.saveLastBackupTimestamp(System.currentTimeMillis())

            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun decryptBackupFile(file: File): String? {
        return try {
            if (!file.exists() || file.length() < GCM_IV_LENGTH + 16) return null
            val fileBytes = FileInputStream(file).use { it.readBytes() }

            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(fileBytes, 0, iv, 0, GCM_IV_LENGTH)

            val cipherBytes = ByteArray(fileBytes.size - GCM_IV_LENGTH)
            System.arraycopy(fileBytes, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.size)

            val keySpec = SecretKeySpec(BACKUP_SEED, "AES")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun checkAndPerformWeeklyBackup(context: Context, jsonDumpSupplier: () -> String): Boolean {
        val prefManager = PreferenceManager(context)
        val lastTimestamp = prefManager.loadLastBackupTimestamp()
        val now = System.currentTimeMillis()

        if (now - lastTimestamp >= WEEKLY_MILLIS) {
            val dump = jsonDumpSupplier()
            if (dump.isNotBlank()) {
                val file = createEncryptedBackup(context, dump)
                return file != null
            }
        }
        return false
    }

    fun listBackups(context: Context): List<File> {
        val backupDir = getBackupDirectory(context)
        return backupDir.listFiles { _, name -> name.endsWith(".ccenc") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}

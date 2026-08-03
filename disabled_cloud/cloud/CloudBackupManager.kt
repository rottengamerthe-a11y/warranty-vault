package com.warrantyvault.cloud

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.warrantyvault.security.BackupEncryption
import kotlinx.coroutines.tasks.await
import java.io.File

class CloudBackupManager(private val context: Context) {
    
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    suspend fun uploadEncryptedBackup(
        data: ByteArray,
        password: String,
        filename: String = "backup_${System.currentTimeMillis()}.json"
    ): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            // Encrypt the data
            val encryptedData = BackupEncryption.encrypt(data, password)
            
            // Create metadata
            val metadata = StorageMetadata.Builder()
                .setContentType("application/json")
                .setCustomMetadata("encrypted", "true")
                .setCustomMetadata("version", "2.0")
                .setCustomMetadata("timestamp", System.currentTimeMillis().toString())
                .build()
            
            // Upload to Firebase Storage
            val ref = storage.reference
                .child("users")
                .child(userId)
                .child("backups")
                .child(filename)
            
            ref.putBytes(encryptedData.toByteArray(), metadata).await()
            
            Result.success(filename)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun downloadEncryptedBackup(
        filename: String,
        password: String
    ): Result<ByteArray> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            // Download from Firebase Storage
            val ref = storage.reference
                .child("users")
                .child(userId)
                .child("backups")
                .child(filename)
            
            val bytes = ref.getBytes(Long.MAX_VALUE).await()
            
            // Decrypt the data
            val decryptedData = BackupEncryption.decrypt(String(bytes), password)
                ?: throw Exception("Failed to decrypt backup. Wrong password?")
            
            Result.success(decryptedData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun listBackups(): Result<List<BackupMetadata>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            val ref = storage.reference
                .child("users")
                .child(userId)
                .child("backups")
            
            val result = ref.listAll().await()
            
            val backups = result.items.map { item ->
                val metadata = item.metadata.await()
                BackupMetadata(
                    name = item.name,
                    size = metadata.sizeBytes,
                    created = metadata.creationTimeMillis,
                    encrypted = metadata.getCustomMetadata("encrypted") == "true",
                    version = metadata.getCustomMetadata("version") ?: "1.0"
                )
            }
            
            Result.success(backups.sortedByDescending { it.created })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteBackup(filename: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            val ref = storage.reference
                .child("users")
                .child(userId)
                .child("backups")
                .child(filename)
            
            ref.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getStorageUsage(): Result<StorageUsage> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            val ref = storage.reference
                .child("users")
                .child(userId)
            
            // List all files in user's directory
            val result = ref.listAll().await()
            
            var totalSize = 0L
            var fileCount = 0
            
            fun calculateSize(prefix: com.google.firebase.storage.StorageReference) {
                val items = prefix.listAll().await()
                totalSize += items.items.sumOf { it.metadata.await().sizeBytes }
                fileCount += items.items.size
                items.prefixes.forEach { calculateSize(it) }
            }
            
            calculateSize(ref)
            
            Result.success(StorageUsage(totalSize, fileCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class BackupMetadata(
    val name: String,
    val size: Long,
    val created: Long,
    val encrypted: Boolean,
    val version: String
)

data class StorageUsage(
    val totalBytes: Long,
    val fileCount: Int
) {
    fun formattedSize(): String {
        val kb = totalBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> "%.2f GB".format(gb)
            mb >= 1 -> "%.2f MB".format(mb)
            kb >= 1 -> "%.2f KB".format(kb)
            else -> "$totalBytes bytes"
        }
    }
}
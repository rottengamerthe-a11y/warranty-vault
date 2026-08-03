package com.warrantyvault.cloud

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.warrantyvault.data.WarrantyItemEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(private val context: Context) {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    suspend fun syncWarrantyItem(item: WarrantyItemEntity): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            val itemData = hashMapOf(
                "id" to item.id,
                "name" to item.name,
                "category" to item.category,
                "storeOrBrand" to item.storeOrBrand,
                "purchaseDate" to item.purchaseDate,
                "warrantyEndDate" to item.warrantyEndDate,
                "returnDeadline" to item.returnDeadline,
                "serialNumber" to item.serialNumber,
                "notes" to item.notes,
                "reminderDaysBefore" to item.reminderDaysBefore,
                "createdAt" to item.createdAt,
                "updatedAt" to item.updatedAt,
                "syncedAt" to System.currentTimeMillis()
            )
            
            db.collection("users")
                .document(userId)
                .collection("warranties")
                .document(item.id.toString())
                .set(itemData, SetOptions.merge())
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteWarrantyItem(itemId: Long): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            db.collection("users")
                .document(userId)
                .collection("warranties")
                .document(itemId.toString())
                .delete()
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllWarranties(): Result<List<WarrantyItemEntity>> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            val snapshot = db.collection("users")
                .document(userId)
                .collection("warranties")
                .get()
                .await()
            
            val items = snapshot.documents.mapNotNull { doc ->
                try {
                    WarrantyItemEntity(
                        id = doc.getLong("id") ?: return@mapNotNull null,
                        name = doc.getString("name") ?: "",
                        category = doc.getString("category") ?: "",
                        storeOrBrand = doc.getString("storeOrBrand") ?: "",
                        purchaseDate = doc.getLong("purchaseDate"),
                        warrantyEndDate = doc.getLong("warrantyEndDate"),
                        returnDeadline = doc.getLong("returnDeadline"),
                        serialNumber = doc.getString("serialNumber") ?: "",
                        notes = doc.getString("notes") ?: "",
                        reminderDaysBefore = doc.getLong("reminderDaysBefore")?.toInt() ?: 14,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun warrantyChanges(): Flow<WarrantyChange> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            close(Exception("User not authenticated"))
            return@callbackFlow
        }
        
        val listener = db.collection("users")
            .document(userId)
            .collection("warranties")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                snapshot?.documentChanges?.forEach { change ->
                    val item = try {
                        val doc = change.document
                        WarrantyItemEntity(
                            id = doc.getLong("id") ?: return@forEach,
                            name = doc.getString("name") ?: "",
                            category = doc.getString("category") ?: "",
                            storeOrBrand = doc.getString("storeOrBrand") ?: "",
                            purchaseDate = doc.getLong("purchaseDate"),
                            warrantyEndDate = doc.getLong("warrantyEndDate"),
                            returnDeadline = doc.getLong("returnDeadline"),
                            serialNumber = doc.getString("serialNumber") ?: "",
                            notes = doc.getString("notes") ?: "",
                            reminderDaysBefore = doc.getLong("reminderDaysBefore")?.toInt() ?: 14,
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        return@forEach
                    }
                    
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            trySend(WarrantyChange.Added(item))
                        }
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            trySend(WarrantyChange.Modified(item))
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            trySend(WarrantyChange.Removed(item.id))
                        }
                        else -> {}
                    }
                }
            }
        
        awaitClose {
            listener.remove()
        }
    }
    
    suspend fun syncSettings(settings: Map<String, Any>): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            db.collection("users")
                .document(userId)
                .collection("settings")
                .document("preferences")
                .set(settings)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSettings(): Result<Map<String, Any>> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            val doc = db.collection("users")
                .document(userId)
                .collection("settings")
                .document("preferences")
                .get()
                .await()
            
            if (doc.exists()) {
                Result.success(doc.data ?: emptyMap())
            } else {
                Result.success(emptyMap())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun enableSync(enabled: Boolean): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            db.collection("users")
                .document(userId)
                .update("syncEnabled", enabled)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun isSyncEnabled(): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            val doc = db.collection("users")
                .document(userId)
                .get()
                .await()
            
            val syncEnabled = doc.getBoolean("syncEnabled") ?: false
            Result.success(syncEnabled)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

sealed class WarrantyChange {
    data class Added(val item: WarrantyItemEntity) : WarrantyChange()
    data class Modified(val item: WarrantyItemEntity) : WarrantyChange()
    data class Removed(val itemId: Long) : WarrantyChange()
}
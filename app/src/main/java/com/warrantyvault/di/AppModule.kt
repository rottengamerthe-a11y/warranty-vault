package com.warrantyvault.di

import android.content.Context
import com.warrantyvault.data.AppDatabase
import com.warrantyvault.data.getDatabase
import com.warrantyvault.backup.AutoBackupScheduler
// Firebase imports - commented out until Firebase is properly configured
// import com.warrantyvault.cloud.FirebaseAuthManager
// import com.warrantyvault.cloud.CloudBackupManager
// import com.warrantyvault.cloud.FirestoreSyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideWarrantyDao(appDatabase: AppDatabase) = appDatabase.warrantyDao()

    @Provides
    @Singleton
    fun provideAutoBackupScheduler(@ApplicationContext context: Context): AutoBackupScheduler {
        return AutoBackupScheduler(context)
    }

    // LocalBackupManager and GoogleDriveBackupManager are auto-wired by Hilt
    // via their @Inject constructors.

    // Firebase providers - commented out until Firebase is properly configured
    /*
    @Provides
    @Singleton
    fun provideFirebaseAuthManager(@ApplicationContext context: Context): FirebaseAuthManager {
        return FirebaseAuthManager(context)
    }
    
    @Provides
    @Singleton
    fun provideCloudBackupManager(@ApplicationContext context: Context): CloudBackupManager {
        return CloudBackupManager(context)
    }
    
    @Provides
    @Singleton
    fun provideFirestoreSyncManager(@ApplicationContext context: Context): FirestoreSyncManager {
        return FirestoreSyncManager(context)
    }
    */
}

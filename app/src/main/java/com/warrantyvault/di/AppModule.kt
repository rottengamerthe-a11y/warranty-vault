package com.warrantyvault.di

import android.content.Context
import com.warrantyvault.data.AppDatabase
import com.warrantyvault.data.getDatabase
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
}

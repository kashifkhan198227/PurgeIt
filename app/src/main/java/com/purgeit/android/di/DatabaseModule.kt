package com.purgeit.android.di

import android.content.Context
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.purgeit.android.data.local.PurgeItDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PurgeItDatabase =
        Room.databaseBuilder(context, PurgeItDatabase::class.java, "purgeit.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideScanHistoryDao(db: PurgeItDatabase) = db.scanHistoryDao()
    @Provides fun provideSnapshotDao(db: PurgeItDatabase) = db.storageSnapshotDao()
    @Provides fun provideExcludedFolderDao(db: PurgeItDatabase) = db.excludedFolderDao()
    @Provides fun providePinnedAppDao(db: PurgeItDatabase) = db.pinnedAppDao()

    @Provides
    @Singleton
    fun provideEncryptedPrefs(@ApplicationContext context: Context) =
        EncryptedSharedPreferences.create(
            context,
            "purgeit_prefs",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
}

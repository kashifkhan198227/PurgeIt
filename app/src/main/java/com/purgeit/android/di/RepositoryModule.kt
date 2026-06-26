package com.purgeit.android.di

import com.purgeit.android.data.repository.AppRepositoryImpl
import com.purgeit.android.data.repository.FileRepositoryImpl
import com.purgeit.android.data.repository.PhotoRepositoryImpl
import com.purgeit.android.data.repository.StorageRepositoryImpl
import com.purgeit.android.data.repository.SubscriptionRepositoryImpl
import com.purgeit.android.domain.repository.AppRepository
import com.purgeit.android.domain.repository.FileRepository
import com.purgeit.android.domain.repository.PhotoRepository
import com.purgeit.android.domain.repository.StorageRepository
import com.purgeit.android.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository

    @Binds @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds @Singleton
    abstract fun bindPhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository

    @Binds @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository
}

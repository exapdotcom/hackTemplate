package com.hustle.core.di

import com.hustle.core.storage.DataStoreTokenStore
import com.hustle.core.storage.TokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindTokenStore(implementation: DataStoreTokenStore): TokenStore
}

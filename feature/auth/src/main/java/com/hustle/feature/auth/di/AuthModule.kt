package com.hustle.feature.auth.di

import com.hustle.core.config.AppConfig
import com.hustle.feature.auth.data.AuthApi
import com.hustle.feature.auth.data.AuthRepositoryImpl
import com.hustle.feature.auth.data.MockAuthRepository
import com.hustle.feature.auth.domain.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideAuthRepository(
        config: AppConfig,
        real: AuthRepositoryImpl,
        mock: MockAuthRepository,
    ): AuthRepository = if (config.useMocks) mock else real
}

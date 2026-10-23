package com.hustle.core.di

import com.hustle.core.BuildConfig
import com.hustle.core.config.AppConfig
import com.hustle.core.config.MockScenario
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideAppConfig(): AppConfig = AppConfig(
        backendUrl = BuildConfig.BACKEND_URL,
        useMocks = BuildConfig.USE_MOCKS,
        mockScenario = runCatching {
            MockScenario.valueOf(BuildConfig.MOCK_SCENARIO.uppercase())
        }.getOrDefault(MockScenario.SUCCESS),
    )
}

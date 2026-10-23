package com.hustle.feature.demo.di

import com.hustle.core.config.AppConfig
import com.hustle.feature.demo.data.DemoApi
import com.hustle.feature.demo.data.DemoRepositoryImpl
import com.hustle.feature.demo.data.MockDemoRepository
import com.hustle.feature.demo.domain.DemoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object DemoModule {

    @Provides
    @Singleton
    fun provideDemoApi(retrofit: Retrofit): DemoApi = retrofit.create(DemoApi::class.java)

    @Provides
    @Singleton
    fun provideDemoRepository(
        config: AppConfig,
        real: DemoRepositoryImpl,
        mock: MockDemoRepository,
    ): DemoRepository = if (config.useMocks) mock else real
}

package com.fabbixmb.app.di

import com.fabbixmb.app.data.remote.ZabbixApiClientFactory
import com.fabbixmb.app.data.repository.ZabbixRepositoryImpl
import com.fabbixmb.app.domain.repository.ZabbixRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideZabbixApiClientFactory(): ZabbixApiClientFactory = ZabbixApiClientFactory()

    @Provides
    @Singleton
    fun provideZabbixRepository(factory: ZabbixApiClientFactory): ZabbixRepository =
        ZabbixRepositoryImpl(factory)
}

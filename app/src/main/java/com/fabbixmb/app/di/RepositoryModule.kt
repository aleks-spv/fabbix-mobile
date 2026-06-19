package com.fabbixmb.app.di

import com.fabbixmb.app.data.repository.ServerRepositoryImpl
import com.fabbixmb.app.domain.repository.ServerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideServerRepository(impl: ServerRepositoryImpl): ServerRepository = impl
}

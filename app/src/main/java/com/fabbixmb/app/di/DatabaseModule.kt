package com.fabbixmb.app.di

import androidx.room.Room
import com.fabbixmb.app.data.local.AppDatabase
import com.fabbixmb.app.data.local.ServerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(app: android.app.Application): AppDatabase {
        return Room.databaseBuilder(app, AppDatabase::class.java, "fabbix.db").build()
    }

    @Provides
    fun provideServerDao(db: AppDatabase): ServerDao = db.serverDao()
}

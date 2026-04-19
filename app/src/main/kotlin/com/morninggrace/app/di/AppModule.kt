package com.morninggrace.app.di

import com.morninggrace.app.location.SharedPrefsLocationRepository
import com.morninggrace.core.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds @Singleton
    abstract fun bindsLocationRepository(impl: SharedPrefsLocationRepository): LocationRepository
}

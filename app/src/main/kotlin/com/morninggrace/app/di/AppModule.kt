package com.morninggrace.app.di

import android.content.Context
import com.morninggrace.core.model.LocationPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val PREFS_NAME = "alarm_prefs"
    private const val KEY_LAT = "location_lat"
    private const val KEY_LON = "location_lon"
    private const val DEFAULT_LAT = -33.87f   // Sydney
    private const val DEFAULT_LON = 151.21f

    @Provides @Singleton
    fun providesLocationPrefs(@ApplicationContext context: Context): LocationPrefs {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return LocationPrefs(
            lat = prefs.getFloat(KEY_LAT, DEFAULT_LAT).toDouble(),
            lon = prefs.getFloat(KEY_LON, DEFAULT_LON).toDouble()
        )
    }
}

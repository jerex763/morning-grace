package com.morninggrace.app.location

import android.content.Context
import com.morninggrace.core.model.LocationPrefs
import com.morninggrace.core.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationRepository {

    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun get(): LocationPrefs {
        return LocationPrefs(
            lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: DEFAULT_LAT,
            lon = prefs.getString(KEY_LON, null)?.toDoubleOrNull() ?: DEFAULT_LON
        )
    }

    override fun save(lat: Double, lon: Double) {
        prefs.edit()
            .putString(KEY_LAT, lat.toString())
            .putString(KEY_LON, lon.toString())
            .apply()
    }

    override fun hasLocation(): Boolean =
        prefs.getString(KEY_LAT, null) != null

    companion object {
        private const val PREFS_NAME = "alarm_prefs"
        private const val KEY_LAT = "location_lat"
        private const val KEY_LON = "location_lon"
        private const val DEFAULT_LAT = -33.87   // Sydney fallback
        private const val DEFAULT_LON = 151.21
    }
}

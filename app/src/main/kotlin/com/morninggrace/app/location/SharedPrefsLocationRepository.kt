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
            lon = prefs.getString(KEY_LON, null)?.toDoubleOrNull() ?: DEFAULT_LON,
            cityName = prefs.getString(KEY_CITY, null) ?: DEFAULT_CITY
        )
    }

    override fun save(lat: Double, lon: Double, cityName: String) {
        prefs.edit()
            .putString(KEY_LAT, lat.toString())
            .putString(KEY_LON, lon.toString())
            .putString(KEY_CITY, cityName)
            .apply()
    }

    override fun useDefault() {
        prefs.edit()
            .remove(KEY_LAT)
            .remove(KEY_LON)
            .remove(KEY_CITY)
            .apply()
    }

    override fun hasLocation(): Boolean =
        prefs.getString(KEY_LAT, null) != null

    companion object {
        private const val PREFS_NAME = "alarm_prefs"
        private const val KEY_LAT = "location_lat"
        private const val KEY_LON = "location_lon"
        private const val KEY_CITY = "location_city"
        private const val DEFAULT_LAT = 39.9042
        private const val DEFAULT_LON = 116.4074
        private const val DEFAULT_CITY = "北京"
    }
}

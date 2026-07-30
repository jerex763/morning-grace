package com.morninggrace.core.repository

import com.morninggrace.core.model.LocationPrefs

interface LocationRepository {
    fun get(): LocationPrefs
    fun save(lat: Double, lon: Double, cityName: String)
    fun useDefault()
    fun hasLocation(): Boolean
}

package com.carife.app.data

import android.content.Context
import com.carife.app.core.model.City
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CityRepository(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    val cities: List<City> = runCatching {
        val raw = context.assets.open("cities.json").bufferedReader().use { it.readText() }
        parseCities(json.parseToJsonElement(raw) as JsonArray)
    }.getOrDefault(emptyList())

    fun byId(id: String): City? = cities.firstOrNull { it.id == id }

    val defaultCity: City get() = byId("bekasi") ?: cities.first()

    private fun parseCities(array: JsonArray): List<City> = array.map { element ->
        val obj = element as JsonObject
        City(
            id = obj["id"]!!.jsonPrimitive.content,
            name = obj["name"]!!.jsonPrimitive.content,
            province = obj["province"]!!.jsonPrimitive.content,
            lat = obj["lat"]!!.jsonPrimitive.double,
            lon = obj["lon"]!!.jsonPrimitive.double,
            radiusMeters = obj["radiusMeters"]!!.jsonPrimitive.int,
        )
    }
}

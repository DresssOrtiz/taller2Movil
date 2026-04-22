package com.example.taller2.ui.map

import android.util.Log
import com.example.taller2.BuildConfig
import com.example.taller2.model.GeoTaggedPhoto
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Consulta una ruta real por calles usando Directions API.
 *
 * Asi evitamos unir los puntos con una linea recta y mantenemos el recorrido
 * dibujado dentro de la propia app.
 */
object DirectionsRouteService {

    suspend fun fetchRoutePoints(geoTaggedPhotos: List<GeoTaggedPhoto>): List<LatLng> =
        withContext(Dispatchers.IO) {
            if (geoTaggedPhotos.size < 2) return@withContext emptyList()
            if (BuildConfig.DIRECTIONS_API_KEY.isBlank()) return@withContext emptyList()

            val orderedPhotos = geoTaggedPhotos.asReversed()
            val origin = orderedPhotos.first().toLatLngString()
            val destination = orderedPhotos.last().toLatLngString()
            val waypoints = orderedPhotos
                .drop(1)
                .dropLast(1)
                .joinToString(separator = "|") { it.toLatLngString() }

            val requestUrl = buildString {
                append("https://maps.googleapis.com/maps/api/directions/json")
                append("?origin=${origin.encodeUrl()}")
                append("&destination=${destination.encodeUrl()}")
                if (waypoints.isNotBlank()) {
                    append("&waypoints=${waypoints.encodeUrl()}")
                }
                append("&mode=driving")
                append("&key=${BuildConfig.DIRECTIONS_API_KEY.encodeUrl()}")
            }

            val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
            }

            return@withContext try {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                parseRoutePoints(response)
            } catch (error: Exception) {
                Log.e("DirectionsRouteService", "No fue posible consultar la ruta por calles.", error)
                emptyList()
            } finally {
                connection.disconnect()
            }
        }

    private fun parseRoutePoints(response: String): List<LatLng> {
        val json = JSONObject(response)
        val status = json.optString("status")
        if (status != "OK") {
            Log.w("DirectionsRouteService", "Directions API respondio con estado: $status")
            return emptyList()
        }

        val routes = json.optJSONArray("routes") ?: return emptyList()
        if (routes.length() == 0) return emptyList()

        val encodedPolyline = routes
            .getJSONObject(0)
            .getJSONObject("overview_polyline")
            .optString("points")

        if (encodedPolyline.isBlank()) return emptyList()
        return decodePolyline(encodedPolyline)
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val points = mutableListOf<LatLng>()
        var index = 0
        var latitude = 0
        var longitude = 0

        while (index < encoded.length) {
            var result = 0
            var shift = 0
            var currentByte: Int
            do {
                currentByte = encoded[index++].code - 63
                result = result or ((currentByte and 0x1f) shl shift)
                shift += 5
            } while (currentByte >= 0x20)
            latitude += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            result = 0
            shift = 0
            do {
                currentByte = encoded[index++].code - 63
                result = result or ((currentByte and 0x1f) shl shift)
                shift += 5
            } while (currentByte >= 0x20)
            longitude += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            points.add(LatLng(latitude / 1E5, longitude / 1E5))
        }

        return points
    }

    private fun GeoTaggedPhoto.toLatLngString(): String = "$lat,$lng"

    private fun String.encodeUrl(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
}

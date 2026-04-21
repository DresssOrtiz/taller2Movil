package com.example.taller2.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.taller2.BuildConfig
import com.example.taller2.model.GeoTaggedPhoto
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapRouteScreen(
    modifier: Modifier = Modifier,
    geoTaggedPhotos: List<GeoTaggedPhoto> = emptyList(),
    onClearRoute: () -> Unit = {}
) {
    val context = LocalContext.current
    // Camara inicial fija mientras aun no llega una ubicacion real.
    val initialLocation = LatLng(4.7110, -74.0721)
    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 13f)
    }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var locationStatus by remember { mutableStateOf("Esperando ubicacion actual.") }
    var isRouteActive by remember { mutableStateOf(false) }
    var routeStatus by remember { mutableStateOf("Recorrido inactivo.") }

    LaunchedEffect(Unit) {
        if (!BuildConfig.HAS_GOOGLE_MAPS_API_KEY) {
            Log.e(
                "MapRouteScreen",
                "MAPS_API_KEY esta vacia. El mapa puede verse negro hasta configurarla en local.properties."
            )
        }

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            locationStatus = "Sin permisos de ubicacion. Se mantiene la camara inicial."
            Log.w("MapRouteScreen", "No hay permisos de ubicacion para leer la posicion actual.")
            return@LaunchedEffect
        }

        locationStatus = "Solicitando una ubicacion actualizada..."

        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .build()

        fusedLocationClient.getCurrentLocation(currentLocationRequest, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    locationStatus = "Mapa centrado en una ubicacion actualizada."
                    return@addOnSuccessListener
                }

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { lastKnownLocation ->
                        if (lastKnownLocation == null) {
                            locationStatus =
                                "Ubicacion no disponible todavia. Se mantiene la camara inicial."
                            return@addOnSuccessListener
                        }

                        currentLocation = LatLng(
                            lastKnownLocation.latitude,
                            lastKnownLocation.longitude
                        )
                        locationStatus = "Se uso la ultima ubicacion conocida como respaldo."
                    }
                    .addOnFailureListener { error ->
                        locationStatus = "No fue posible obtener la ubicacion actual."
                        Log.e("MapRouteScreen", "Error obteniendo lastLocation de respaldo.", error)
                    }
            }
            .addOnFailureListener { error ->
                locationStatus = "No fue posible obtener una ubicacion actualizada."
                Log.e("MapRouteScreen", "Error obteniendo una ubicacion fresca.", error)
            }
    }

    LaunchedEffect(currentLocation) {
        val userLatLng = currentLocation ?: return@LaunchedEffect
        // La camara se recentra solo con la ubicacion actual del usuario.
        cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 16f)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties()
            ) {
                geoTaggedPhotos.forEach { geoTaggedPhoto ->
                    // Estos marcadores vienen de fotos geolocalizadas tomadas con la camara.
                    // Usan lat/lng y titulo de GeoTaggedPhoto, separados del marcador del usuario.
                    Marker(
                        state = MarkerState(
                            position = LatLng(geoTaggedPhoto.lat, geoTaggedPhoto.lng)
                        ),
                        title = geoTaggedPhoto.title
                    )
                }

                currentLocation?.let { userLatLng ->
                    // El marcador de ubicacion actual del usuario sigue independiente.
                    Marker(
                        state = MarkerState(position = userLatLng),
                        title = "Mi ubicacion actual"
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Panel del recorrido",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!BuildConfig.HAS_GOOGLE_MAPS_API_KEY) {
                Text(
                    text = "Falta configurar MAPS_API_KEY en local.properties.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = "Los marcadores del recorrido ahora salen de fotos geolocalizadas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = routeStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isRouteActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = locationStatus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Button(
                    onClick = {
                        // Ya no se crean mocks: al activar el recorrido,
                        // el mapa esperara marcadores reales desde geoTaggedPhotos.
                        isRouteActive = true
                        routeStatus = "Recorrido activo. Esperando fotos geolocalizadas."
                    },
                    enabled = !isRouteActive,
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text("Iniciar recorrido")
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = {
                        // Este borrado solo afecta el estado de la app.
                        // Al vaciar geoTaggedPhotos desaparecen automaticamente los marcadores del mapa.
                        isRouteActive = false
                        routeStatus = "Recorrido inactivo."
                        onClearRoute()
                    }
                ) {
                    Text("Borrar recorrido")
                }
            }
            Text(
                text = "Marcadores del recorrido: ${geoTaggedPhotos.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

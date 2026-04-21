package com.example.taller2.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
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
            locationStatus = "Sin permisos de ubicacion."
            Log.w("MapRouteScreen", "No hay permisos de ubicacion para leer la posicion actual.")
            return@LaunchedEffect
        }

        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .build()

        fusedLocationClient.getCurrentLocation(currentLocationRequest, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    locationStatus = "Ubicacion actualizada."
                    return@addOnSuccessListener
                }

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { lastKnownLocation ->
                        if (lastKnownLocation == null) {
                            locationStatus = "Ubicacion no disponible."
                            return@addOnSuccessListener
                        }

                        currentLocation = LatLng(
                            lastKnownLocation.latitude,
                            lastKnownLocation.longitude
                        )
                        locationStatus = "Usando ultima ubicacion conocida."
                    }
                    .addOnFailureListener { error ->
                        locationStatus = "No fue posible obtener ubicacion."
                        Log.e("MapRouteScreen", "Error obteniendo lastLocation de respaldo.", error)
                    }
            }
            .addOnFailureListener { error ->
                locationStatus = "No fue posible obtener ubicacion."
                Log.e("MapRouteScreen", "Error obteniendo una ubicacion fresca.", error)
            }
    }

    LaunchedEffect(currentLocation) {
        val userLatLng = currentLocation ?: return@LaunchedEffect
        cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 16f)
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties()
        ) {
            // Estos marcadores salen de fotos geolocalizadas tomadas con la camara.
            geoTaggedPhotos.forEach { geoTaggedPhoto ->
                Marker(
                    state = MarkerState(position = LatLng(geoTaggedPhoto.lat, geoTaggedPhoto.lng)),
                    title = geoTaggedPhoto.title
                )
            }

            // El marcador del usuario se mantiene separado de los marcadores del recorrido.
            currentLocation?.let { userLatLng ->
                Marker(
                    state = MarkerState(position = userLatLng),
                    title = "Mi ubicacion actual"
                )
            }
        }

        // Controles flotantes sobre el mapa para acercarse a la referencia visual del taller.
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { isRouteActive = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.Layers,
                    contentDescription = "Iniciar recorrido"
                )
            }

            FloatingActionButton(
                onClick = {
                    // Este borrado solo limpia el estado de la app; no elimina fotos de la galeria.
                    isRouteActive = false
                    onClearRoute()
                },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "Borrar recorrido"
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 4.dp
            ) {
                Text(
                    text = if (isRouteActive) {
                        "Recorrido activo"
                    } else {
                        "Recorrido inactivo"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 4.dp
            ) {
                Text(
                    text = "Marcadores: ${geoTaggedPhotos.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 4.dp
            ) {
                Text(
                    text = locationStatus,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

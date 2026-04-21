package com.example.taller2.ui.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.example.taller2.model.GeoTaggedPhoto
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * ViewModel que gestiona el estado de la pantalla de camara:
 * - Lista de URIs de fotos tomadas en la sesion actual.
 * - Lente activo (trasero o frontal).
 * - Lista temporal de fotos geolocalizadas para la futura integracion mapa + camara.
 *
 * Se expone onFotoTomada para que feature/integration-map-camera
 * pueda reaccionar cuando se capture una nueva foto.
 */
class CameraViewModel : ViewModel() {

    // Lente de camara activo; empieza con la camara trasera.
    private val _lenteActivo = MutableStateFlow(LenteCamara.TRASERO)
    val lenteActivo: StateFlow<LenteCamara> = _lenteActivo.asStateFlow()

    // Lista acumulada de URIs de fotos tomadas durante la sesion.
    private val _fotosUri = MutableStateFlow<List<Uri>>(emptyList())
    val fotosUri: StateFlow<List<Uri>> = _fotosUri.asStateFlow()

    // Estado temporal para la futura integracion: cada foto guardada junto con su ubicacion.
    private val _geoTaggedPhotos = MutableStateFlow<List<GeoTaggedPhoto>>(emptyList())
    val geoTaggedPhotos: StateFlow<List<GeoTaggedPhoto>> = _geoTaggedPhotos.asStateFlow()

    /** Alterna entre camara frontal y trasera. */
    fun cambiarLente() {
        _lenteActivo.value = when (_lenteActivo.value) {
            LenteCamara.TRASERO -> LenteCamara.FRONTAL
            LenteCamara.FRONTAL -> LenteCamara.TRASERO
        }
    }

    /**
     * Captura una foto usando [imageCapture] y la guarda en MediaStore (galeria).
     *
     * @param context       Contexto de la aplicacion.
     * @param imageCapture  Caso de uso de CameraX para captura de imagen.
     * @param onFotoTomada  Callback opcional; lo usa feature/integration-map-camera
     *                      para crear un marcador en el mapa con la URI resultante.
     */
    fun tomarFoto(
        context: Context,
        imageCapture: ImageCapture,
        onFotoTomada: ((Uri) -> Unit)? = null
    ) {
        val nombreArchivo = "foto_${System.currentTimeMillis()}"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            // En Android 10+ se usa RELATIVE_PATH para ubicar la foto en la galeria.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Taller2")
            }
        }

        val opcionesSalida = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            opcionesSalida,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(resultado: ImageCapture.OutputFileResults) {
                    val uri = resultado.savedUri ?: return
                    val photoId = UUID.randomUUID().toString()

                    // La foto sale del resultado de CameraX y se mantiene en la lista visual actual.
                    _fotosUri.value = listOf(uri) + _fotosUri.value

                    // Aqui se conecta la captura con la ubicacion actual disponible en la app.
                    // El objeto GeoTaggedPhoto se guardara para que luego mapa y lista usen la misma fuente.
                    construirFotoGeolocalizada(
                        context = context,
                        photoId = photoId,
                        photoUri = uri,
                        photoTitle = nombreArchivo
                    )

                    onFotoTomada?.invoke(uri)
                }

                override fun onError(excepcion: ImageCaptureException) {
                    // Los errores de captura se propagaran en feature/permissions si
                    // el permiso no fue concedido; por ahora se ignoran silenciosamente.
                }
            }
        )
    }

    /** Vacia la lista de fotos de la sesion (util para feature/integration-map-camera). */
    fun limpiarFotos() {
        _fotosUri.value = emptyList()
    }

    private fun construirFotoGeolocalizada(
        context: Context,
        photoId: String,
        photoUri: Uri,
        photoTitle: String
    ) {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .build()

        fusedLocationClient.getCurrentLocation(currentLocationRequest, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    // La ubicacion principal sale de una lectura fresca de Play Services.
                    agregarGeoTaggedPhoto(
                        photoId = photoId,
                        photoUri = photoUri,
                        photoTitle = photoTitle,
                        location = LatLng(location.latitude, location.longitude)
                    )
                    return@addOnSuccessListener
                }

                // Si no llega una ubicacion fresca, se usa el ultimo valor conocido como respaldo.
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { lastKnownLocation ->
                        if (lastKnownLocation == null) return@addOnSuccessListener

                        agregarGeoTaggedPhoto(
                            photoId = photoId,
                            photoUri = photoUri,
                            photoTitle = photoTitle,
                            location = LatLng(
                                lastKnownLocation.latitude,
                                lastKnownLocation.longitude
                            )
                        )
                    }
            }
    }

    private fun agregarGeoTaggedPhoto(
        photoId: String,
        photoUri: Uri,
        photoTitle: String,
        location: LatLng
    ) {
        val geoTaggedPhoto = GeoTaggedPhoto(
            id = photoId,
            photoUri = photoUri.toString(),
            title = photoTitle,
            lat = location.latitude,
            lng = location.longitude
        )

        _geoTaggedPhotos.value = listOf(geoTaggedPhoto) + _geoTaggedPhotos.value
    }
}

/** Enum que representa el lente de camara seleccionado. */
enum class LenteCamara {
    FRONTAL,
    TRASERO
}

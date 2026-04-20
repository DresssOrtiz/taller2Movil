package com.example.taller2.ui.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel que gestiona el estado de la pantalla de cámara:
 * - Lista de URIs de fotos tomadas en la sesión actual.
 * - Lente activo (trasero o frontal).
 *
 * Se expone onFotoTomada para que feature/integration-map-camera
 * pueda reaccionar cuando se capture una nueva foto.
 */
class CameraViewModel : ViewModel() {

    // Lente de cámara activo; empieza con la cámara trasera.
    private val _lenteActivo = MutableStateFlow(LenteCamara.TRASERO)
    val lenteActivo: StateFlow<LenteCamara> = _lenteActivo.asStateFlow()

    // Lista acumulada de URIs de fotos tomadas durante la sesión.
    private val _fotosUri = MutableStateFlow<List<Uri>>(emptyList())
    val fotosUri: StateFlow<List<Uri>> = _fotosUri.asStateFlow()

    /** Alterna entre cámara frontal y trasera. */
    fun cambiarLente() {
        _lenteActivo.value = when (_lenteActivo.value) {
            LenteCamara.TRASERO -> LenteCamara.FRONTAL
            LenteCamara.FRONTAL -> LenteCamara.TRASERO
        }
    }

    /**
     * Captura una foto usando [imageCapture] y la guarda en MediaStore (galería).
     *
     * @param context       Contexto de la aplicación.
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
            // En Android 10+ se usa RELATIVE_PATH para ubicar la foto en la galería.
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
                    // Agregar la nueva URI al inicio para mostrar la más reciente primero.
                    _fotosUri.value = listOf(uri) + _fotosUri.value
                    onFotoTomada?.invoke(uri)
                }

                override fun onError(excepcion: ImageCaptureException) {
                    // Los errores de captura se propagarán en feature/permissions si
                    // el permiso no fue concedido; por ahora se ignoran silenciosamente.
                }
            }
        )
    }

    /** Vacía la lista de fotos de la sesión (útil para feature/integration-map-camera). */
    fun limpiarFotos() {
        _fotosUri.value = emptyList()
    }
}

/** Enum que representa el lente de cámara seleccionado. */
enum class LenteCamara {
    FRONTAL,
    TRASERO
}

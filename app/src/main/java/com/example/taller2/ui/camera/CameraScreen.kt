package com.example.taller2.ui.camera

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Pantalla principal de cámara para la rama feature/camera-photos.
 *
 * Expone [onFotoTomada] para que feature/integration-map-camera pueda reaccionar
 * a cada captura (por ejemplo, obtener la ubicación y crear un marcador en el mapa).
 *
 * Los permisos de cámara se asumen concedidos; el manejo de permisos
 * se implementará en la rama feature/permissions.
 *
 * @param modifier      Modificador Compose estándar.
 * @param viewModel     ViewModel que gestiona el estado de la cámara.
 * @param onFotoTomada  Callback invocado con la URI de cada foto guardada.
 */
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = viewModel(),
    onFotoTomada: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val lenteActivo by viewModel.lenteActivo.collectAsStateWithLifecycle()
    val fotosUri by viewModel.fotosUri.collectAsStateWithLifecycle()
    val geoTaggedPhotos by viewModel.geoTaggedPhotos.collectAsStateWithLifecycle()

    // Se recrean los casos de uso cuando cambia el lente para forzar el rebind.
    val preview = remember(lenteActivo) { Preview.Builder().build() }
    val imageCapture = remember(lenteActivo) { ImageCapture.Builder().build() }

    // Vincula los casos de uso de CameraX al ciclo de vida cada vez que cambia el lente.
    LaunchedEffect(lenteActivo) {
        val camaraProvider = context.obtenerProveedorCamara()

        val selectorLente = CameraSelector.Builder()
            .requireLensFacing(
                when (lenteActivo) {
                    LenteCamara.TRASERO -> CameraSelector.LENS_FACING_BACK
                    LenteCamara.FRONTAL -> CameraSelector.LENS_FACING_FRONT
                }
            )
            .build()

        try {
            camaraProvider.unbindAll()
            camaraProvider.bindToLifecycle(
                lifecycleOwner,
                selectorLente,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            // Si el hardware no soporta el lente solicitado se ignora el error;
            // feature/permissions mostrará mensajes apropiados al usuario.
        }
    }

    Column(
        modifier = modifier.background(Color.Black)
    ) {
        // ── Área de preview de la cámara ──────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        // COMPATIBLE garantiza mejor integración con Compose.
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                update = { previewView ->
                    // Se actualiza el provider de superficie cada vez que el composable
                    // se recompone, para mantener sincronización con el ciclo de vida.
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Botón de cambio de cámara superpuesto en la esquina superior derecha.
            IconButton(
                onClick = { viewModel.cambiarLente() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = "Cambiar cámara",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // ── Panel inferior: botón de captura + miniaturas ─────────────────────
        Surface(
            color = Color(0xFF1A1A1A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                // Botón circular de captura.
                BotonaCaptura(
                    onClick = {
                        viewModel.tomarFoto(
                            context = context,
                            imageCapture = imageCapture,
                            onFotoTomada = onFotoTomada
                        )
                    }
                )

                // Lista horizontal de miniaturas; se muestra solo si hay fotos.
                if (fotosUri.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ListaMiniaturas(fotosUri = fotosUri)
                }

                Text(
                    text = "Fotos geolocalizadas listas para integrar: ${geoTaggedPhotos.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

/**
 * Botón circular de disparo, siguiendo el diseño clásico de apps de cámara.
 */
@Composable
private fun BotonaCaptura(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .border(3.dp, Color.White, CircleShape)
            .padding(6.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, CircleShape)
        ) {
            // El ícono vacío sirve de área táctil; el círculo blanco es el indicador visual.
        }
    }
}

/**
 * Fila horizontal de miniaturas de las fotos tomadas en la sesión.
 * La foto más reciente aparece primero (el ViewModel la inserta al inicio de la lista).
 */
@Composable
private fun ListaMiniaturas(fotosUri: List<Uri>) {
    Column {
        Text(
            text = "Fotos tomadas (${fotosUri.size})",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(fotosUri, key = { it.toString() }) { uri ->
                Miniatura(uri = uri)
            }
        }
    }
}

/**
 * Miniatura individual de una foto tomada.
 */
@Composable
private fun Miniatura(uri: Uri) {
    AsyncImage(
        model = uri,
        contentDescription = "Foto tomada",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    )
}

/**
 * Suspends hasta obtener el [ProcessCameraProvider] del contexto dado.
 * Función de extensión utilitaria para usar en corrutinas.
 */
suspend fun android.content.Context.obtenerProveedorCamara(): ProcessCameraProvider =
    suspendCoroutine { continuacion ->
        val futuro = ProcessCameraProvider.getInstance(this)
        futuro.addListener(
            { continuacion.resume(futuro.get()) },
            ContextCompat.getMainExecutor(this)
        )
    }

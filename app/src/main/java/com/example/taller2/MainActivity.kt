package com.example.taller2

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taller2.ui.camera.CameraScreen
import com.example.taller2.ui.camera.CameraViewModel
import com.example.taller2.ui.map.MapRouteScreen
import com.example.taller2.ui.permissions.EstadoPermiso
import com.example.taller2.ui.permissions.ManejadorPermisos
import com.example.taller2.ui.permissions.PlaceholderSinPermiso
import com.example.taller2.ui.theme.Taller2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Taller2Theme {
                // ViewModel compartido entre la camara y el mapa para sincronizar
                // las fotos geolocalizadas sin acoplar directamente los dos componentes.
                val cameraViewModel: CameraViewModel = viewModel()
                val geoTaggedPhotos = cameraViewModel.geoTaggedPhotos.collectAsStateWithLifecycle()

                // Detectar la orientacion actual para decidir el layout adaptativo.
                val configuracion = LocalConfiguration.current
                val esHorizontal = configuracion.orientation == Configuration.ORIENTATION_LANDSCAPE

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // ManejadorPermisos solicita camara y ubicacion al arrancar la app
                    // y decide que se muestra segun el estado de cada permiso.
                    ManejadorPermisos { estadoPermisos, solicitarPermisos ->
                        if (esHorizontal) {
                            // ── Layout horizontal: mapa izquierda (~50%) | camara derecha (~50%) ──
                            // Ambas secciones son visibles al mismo tiempo sin necesidad de scroll.
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                // Mapa a la izquierda ocupando la mitad del ancho disponible.
                                if (estadoPermisos.ubicacion is EstadoPermiso.Concedido) {
                                    MapRouteScreen(
                                        modifier = Modifier.weight(1f),
                                        geoTaggedPhotos = geoTaggedPhotos.value,
                                        onClearRoute = {
                                            cameraViewModel.limpiarRecorridoActual()
                                        }
                                    )
                                } else {
                                    PlaceholderSinPermiso(
                                        modifier = Modifier.weight(1f),
                                        nombrePermiso = "Ubicacion",
                                        motivo = "El mapa necesita acceso a tu ubicacion para " +
                                                "centrar la vista y geolocalizar cada foto del recorrido.",
                                        estadoPermiso = estadoPermisos.ubicacion,
                                        onSolicitarPermiso = solicitarPermisos
                                    )
                                }

                                // Camara a la derecha ocupando el 50% restante del ancho.
                                if (estadoPermisos.camara is EstadoPermiso.Concedido) {
                                    CameraScreen(
                                        modifier = Modifier.weight(1f),
                                        viewModel = cameraViewModel
                                    )
                                } else {
                                    PlaceholderSinPermiso(
                                        modifier = Modifier.weight(1f),
                                        nombrePermiso = "Camara",
                                        motivo = "La camara necesita permiso para tomar fotos " +
                                                "y asociarlas con las coordenadas del recorrido.",
                                        estadoPermiso = estadoPermisos.camara,
                                        onSolicitarPermiso = solicitarPermisos
                                    )
                                }
                            }
                        } else {
                            // ── Layout vertical: camara arriba | mapa abajo ──────────────────
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                // Seccion de la camara (arriba).
                                // Se muestra la camara real solo si el permiso fue concedido.
                                if (estadoPermisos.camara is EstadoPermiso.Concedido) {
                                    CameraScreen(
                                        modifier = Modifier.weight(1f),
                                        viewModel = cameraViewModel
                                    )
                                } else {
                                    PlaceholderSinPermiso(
                                        modifier = Modifier.weight(1f),
                                        nombrePermiso = "Camara",
                                        motivo = "La camara necesita permiso para tomar fotos " +
                                                "y asociarlas con las coordenadas del recorrido.",
                                        estadoPermiso = estadoPermisos.camara,
                                        onSolicitarPermiso = solicitarPermisos
                                    )
                                }

                                // Seccion del mapa (abajo).
                                // Se muestra el mapa real solo si el permiso fue concedido.
                                if (estadoPermisos.ubicacion is EstadoPermiso.Concedido) {
                                    MapRouteScreen(
                                        modifier = Modifier.weight(1f),
                                        geoTaggedPhotos = geoTaggedPhotos.value,
                                        onClearRoute = {
                                            cameraViewModel.limpiarRecorridoActual()
                                        }
                                    )
                                } else {
                                    PlaceholderSinPermiso(
                                        modifier = Modifier.weight(1f),
                                        nombrePermiso = "Ubicacion",
                                        motivo = "El mapa necesita acceso a tu ubicacion para " +
                                                "centrar la vista y geolocalizar cada foto del recorrido.",
                                        estadoPermiso = estadoPermisos.ubicacion,
                                        onSolicitarPermiso = solicitarPermisos
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

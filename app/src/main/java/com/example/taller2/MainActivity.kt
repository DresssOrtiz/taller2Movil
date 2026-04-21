package com.example.taller2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
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

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // ManejadorPermisos solicita camara y ubicacion al arrancar la app
                    // y decide que se muestra segun el estado de cada permiso.
                    // No modifica la logica interna de CameraScreen ni de MapRouteScreen.
                    ManejadorPermisos { estadoPermisos, solicitarPermisos ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // ── Seccion de la camara (arriba) ────────────────────────────
                            // Se muestra la camara real solo si el permiso de camara fue concedido.
                            // Sin este permiso CameraX no puede inicializar el proveedor de camara.
                            if (estadoPermisos.camara is EstadoPermiso.Concedido) {
                                CameraScreen(
                                    modifier = Modifier.weight(1f),
                                    viewModel = cameraViewModel
                                )
                            } else {
                                // Sustituye la camara con un mensaje claro mientras el permiso falta.
                                PlaceholderSinPermiso(
                                    modifier = Modifier.weight(1f),
                                    nombrePermiso = "Camara",
                                    motivo = "La camara necesita permiso para tomar fotos " +
                                            "y asociarlas con las coordenadas del recorrido.",
                                    estadoPermiso = estadoPermisos.camara,
                                    onSolicitarPermiso = solicitarPermisos
                                )
                            }

                            // ── Seccion del mapa (abajo) ──────────────────────────────────
                            // Se muestra el mapa real solo si el permiso de ubicacion fue concedido.
                            // Sin ubicacion el mapa no puede centrar la camara ni geolocalizar marcadores.
                            if (estadoPermisos.ubicacion is EstadoPermiso.Concedido) {
                                MapRouteScreen(
                                    modifier = Modifier.weight(1f),
                                    geoTaggedPhotos = geoTaggedPhotos.value,
                                    onClearRoute = {
                                        // El borrado solo limpia el estado compartido en memoria.
                                        cameraViewModel.limpiarRecorridoActual()
                                    }
                                )
                            } else {
                                // Sustituye el mapa con un mensaje claro mientras el permiso falta.
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

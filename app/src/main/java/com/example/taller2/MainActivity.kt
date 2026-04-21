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
import com.example.taller2.ui.theme.Taller2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Taller2Theme {
                // La opcion mas simple para esta rama es compartir un mismo ViewModel
                // entre camara y mapa para que ambos lean las mismas fotos geolocalizadas.
                val cameraViewModel: CameraViewModel = viewModel()
                val geoTaggedPhotos = cameraViewModel.geoTaggedPhotos.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MapRouteScreen(
                            modifier = Modifier.weight(1f),
                            geoTaggedPhotos = geoTaggedPhotos.value,
                            onClearRoute = {
                                // El borrado solo limpia el estado compartido de la app.
                                cameraViewModel.limpiarRecorridoActual()
                            }
                        )
                        CameraScreen(
                            modifier = Modifier.weight(1f),
                            viewModel = cameraViewModel
                        )
                    }
                }
            }
        }
    }
}

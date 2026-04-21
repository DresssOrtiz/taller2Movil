package com.example.taller2.ui.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Placeholder simple para reemplazar temporalmente una seccion bloqueada por permisos.
 *
 * No toca la logica de mapa ni de camara: solo informa el estado y ofrece
 * reintentar o abrir ajustes si el permiso fue bloqueado permanentemente.
 */
@Composable
fun PlaceholderSinPermiso(
    modifier: Modifier = Modifier,
    nombrePermiso: String,
    motivo: String,
    estadoPermiso: EstadoPermiso,
    onSolicitarPermiso: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$nombrePermiso no disponible",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = motivo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )

        when (estadoPermiso) {
            EstadoPermiso.Concedido -> Unit
            EstadoPermiso.Denegado -> {
                Button(
                    onClick = onSolicitarPermiso,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Conceder permiso")
                }
            }
            EstadoPermiso.DenegadoPermanentemente -> {
                Text(
                    text = "El permiso fue bloqueado. Debes habilitarlo desde ajustes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Abrir ajustes")
                }
            }
        }
    }
}

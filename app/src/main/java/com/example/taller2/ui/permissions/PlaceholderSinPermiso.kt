package com.example.taller2.ui.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Sustituye visualmente a un componente (camara o mapa) cuando su permiso
 * correspondiente no ha sido concedido.
 *
 * Muestra un mensaje claro indicando:
 * - Que permiso falta y por que es necesario.
 * - Un boton para volver a solicitar el permiso si aun es posible.
 * - Un boton para ir a los ajustes del sistema si el permiso fue bloqueado permanentemente.
 * - Un indicador de carga mientras el dialogo del sistema esta activo.
 *
 * @param nombrePermiso      Nombre legible del permiso faltante (p.ej. "Camara").
 * @param motivo             Explicacion de por que la app necesita este permiso.
 * @param estadoPermiso      Estado actual del permiso para decidir que accion mostrar.
 * @param onSolicitarPermiso Callback para reintentar la solicitud del permiso.
 * @param modifier           Modificador Compose estandar.
 */
@Composable
fun PlaceholderSinPermiso(
    nombrePermiso: String,
    motivo: String,
    estadoPermiso: EstadoPermiso,
    onSolicitarPermiso: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            when (estadoPermiso) {
                // El dialogo del sistema sigue activo; se muestra un indicador de espera.
                is EstadoPermiso.PendienteDeSolicitar -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Solicitando permiso de $nombrePermiso...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // El permiso fue denegado con posibilidad de volver a solicitarlo.
                is EstadoPermiso.Denegado -> {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Permiso de $nombrePermiso requerido",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Permiso de $nombrePermiso requerido",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = motivo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = onSolicitarPermiso) {
                        Text("Conceder permiso")
                    }
                }

                // El usuario marco "No volver a preguntar"; solo los ajustes del sistema
                // pueden reactivar el permiso.
                is EstadoPermiso.DenegadoPermanente -> {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Permiso de $nombrePermiso bloqueado",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Permiso de $nombrePermiso bloqueado",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = motivo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Has seleccionado \"No volver a preguntar\". " +
                                "Para habilitar este permiso debes ir a los ajustes de la aplicacion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Boton de accion principal: lleva al usuario a los ajustes del sistema.
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Abrir ajustes del sistema")
                    }
                    // Boton secundario por si el usuario quiere intentar la solicitud
                    // igualmente (algunos fabricantes no bloquean el dialogo tras una denegacion).
                    OutlinedButton(onClick = onSolicitarPermiso) {
                        Text("Intentar de nuevo")
                    }
                }

                // Este caso no deberia ocurrir: si el permiso esta concedido,
                // el componente real se muestra en lugar de este placeholder.
                is EstadoPermiso.Concedido -> Unit
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

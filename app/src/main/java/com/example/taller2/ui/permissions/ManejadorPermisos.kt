package com.example.taller2.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

/**
 * Solicita los permisos clave al arrancar y expone un estado resumido a la UI.
 *
 * La implementacion se mantiene simple:
 * - camara depende de CAMERA
 * - ubicacion se considera concedida si existe FINE o COARSE
 */
@Composable
fun ManejadorPermisos(
    content: @Composable (EstadoPermisos, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var permisoCamaraSolicitado by remember { mutableStateOf(false) }
    var permisoUbicacionSolicitado by remember { mutableStateOf(false) }

    val launcherPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permisoCamaraSolicitado = true
        permisoUbicacionSolicitado = true
    }

    val solicitarPermisos = {
        launcherPermisos.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(Unit) {
        solicitarPermisos()
    }

    val estadoPermisos = EstadoPermisos(
        camara = calcularEstadoCamara(
            context = context,
            fueSolicitado = permisoCamaraSolicitado
        ),
        ubicacion = calcularEstadoUbicacion(
            context = context,
            fueSolicitado = permisoUbicacionSolicitado
        )
    )

    content(estadoPermisos, solicitarPermisos)
}

private fun calcularEstadoCamara(context: Context, fueSolicitado: Boolean): EstadoPermiso {
    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    if (granted) return EstadoPermiso.Concedido

    val activity = context.findActivity() ?: return EstadoPermiso.Denegado
    val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.CAMERA
    )

    return if (fueSolicitado && !shouldShowRationale) {
        EstadoPermiso.DenegadoPermanentemente
    } else {
        EstadoPermiso.Denegado
    }
}

private fun calcularEstadoUbicacion(context: Context, fueSolicitado: Boolean): EstadoPermiso {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (fineGranted || coarseGranted) return EstadoPermiso.Concedido

    val activity = context.findActivity() ?: return EstadoPermiso.Denegado
    val shouldShowRationaleFine = ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val shouldShowRationaleCoarse = ActivityCompat.shouldShowRequestPermissionRationale(
        activity,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    return if (fueSolicitado && !shouldShowRationaleFine && !shouldShowRationaleCoarse) {
        EstadoPermiso.DenegadoPermanentemente
    } else {
        EstadoPermiso.Denegado
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

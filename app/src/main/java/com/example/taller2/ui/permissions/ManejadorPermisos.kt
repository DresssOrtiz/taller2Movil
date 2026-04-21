package com.example.taller2.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
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

/**
 * Representa el estado de un permiso individual en la aplicacion.
 *
 * El flujo normal es:
 *   PendienteDeSolicitar → (dialogo del sistema) → Concedido | Denegado | DenegadoPermanente
 */
sealed class EstadoPermiso {
    /** El permiso fue concedido por el usuario o ya lo estaba de una sesion anterior. */
    object Concedido : EstadoPermiso()

    /** El usuario denego el permiso pero puede ser solicitado de nuevo. */
    object Denegado : EstadoPermiso()

    /**
     * El usuario marco "No volver a preguntar"; el sistema bloqueara futuros dialogos.
     * La unica opcion es redirigir al usuario a los ajustes del sistema operativo.
     */
    object DenegadoPermanente : EstadoPermiso()

    /** La aplicacion aun no ha lanzado el dialogo de solicitud. */
    object PendienteDeSolicitar : EstadoPermiso()
}

/**
 * Agrupa el estado de todos los permisos necesarios para la app.
 *
 * @param camara    Estado actual del permiso [Manifest.permission.CAMERA].
 * @param ubicacion Estado actual del permiso de ubicacion (fino o grueso).
 */
data class EstadoPermisos(
    val camara: EstadoPermiso = EstadoPermiso.PendienteDeSolicitar,
    val ubicacion: EstadoPermiso = EstadoPermiso.PendienteDeSolicitar
)

/**
 * Comprueba si un permiso especifico ya fue otorgado sin mostrar ningun dialogo.
 * Se usa para la verificacion inicial al arrancar la app.
 *
 * @param context Contexto de la aplicacion.
 * @param permiso Identificador del permiso (p.ej. [Manifest.permission.CAMERA]).
 * @return        true si el permiso ya esta concedido.
 */
private fun permisoConcedido(context: Context, permiso: String): Boolean =
    ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED

/**
 * Composable raiz de gestion de permisos en runtime.
 *
 * Al montarse, solicita de forma agrupada los permisos de camara y ubicacion
 * usando [ActivityResultContracts.RequestMultiplePermissions]. Segun la respuesta
 * del usuario actualiza el [EstadoPermisos] y lo proporciona al [contenido] mediante
 * un patron de slot (content slot).
 *
 * No modifica la logica interna de los componentes de camara ni de mapa; solo
 * decide si se renderizan o se sustituyen por un [PlaceholderSinPermiso].
 *
 * @param contenido Lambda que recibe el estado actual de los permisos y una funcion
 *                  para relanzar la solicitud si el usuario quiere reintentarlo.
 */
@Composable
fun ManejadorPermisos(
    contenido: @Composable (
        estadoPermisos: EstadoPermisos,
        solicitarPermisos: () -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    // La actividad se necesita para consultar shouldShowRequestPermissionRationale,
    // que determina si el permiso fue denegado permanentemente.
    val actividad = context as ComponentActivity

    // Estado inicial: verifica si los permisos ya fueron concedidos en sesiones anteriores
    // para no solicitar innecesariamente el dialogo del sistema.
    var estadoPermisos by remember {
        mutableStateOf(
            EstadoPermisos(
                camara = if (permisoConcedido(context, Manifest.permission.CAMERA))
                    EstadoPermiso.Concedido
                else
                    EstadoPermiso.PendienteDeSolicitar,
                ubicacion = if (
                    permisoConcedido(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                    permisoConcedido(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                ) EstadoPermiso.Concedido
                else EstadoPermiso.PendienteDeSolicitar
            )
        )
    }

    /**
     * Launcher que solicita todos los permisos necesarios de una sola vez.
     * El mapa de resultados indica si cada permiso fue concedido o denegado.
     * Se evalua shouldShowRequestPermissionRationale despues del resultado para
     * distinguir entre una denegacion simple y una denegacion permanente.
     */
    val lanzadorPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val camaraOtorgada = resultados[Manifest.permission.CAMERA] == true

        // La ubicacion se considera concedida si al menos uno de los dos permisos fue otorgado.
        val ubicacionOtorgada =
            resultados[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    resultados[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        estadoPermisos = EstadoPermisos(
            camara = when {
                camaraOtorgada -> EstadoPermiso.Concedido
                // Si shouldShowRationale devuelve false despues de una denegacion,
                // el usuario marco "No volver a preguntar".
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    actividad,
                    Manifest.permission.CAMERA
                ) -> EstadoPermiso.DenegadoPermanente

                else -> EstadoPermiso.Denegado
            },
            ubicacion = when {
                ubicacionOtorgada -> EstadoPermiso.Concedido
                // Para ubicacion se comprueba shouldShowRationale en ambos permisos;
                // si ninguno puede mostrarse de nuevo, se considera denegado permanentemente.
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    actividad,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) && !ActivityCompat.shouldShowRequestPermissionRationale(
                    actividad,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) -> EstadoPermiso.DenegadoPermanente

                else -> EstadoPermiso.Denegado
            }
        )
    }

    // Funcion para relanzar la solicitud de permisos que aun no han sido concedidos.
    // Se recrea solo cuando cambia el estado de permisos para evitar recomposiciones innecesarias.
    val solicitarPermisos: () -> Unit = remember(estadoPermisos) {
        {
            val permisosPendientes = buildList {
                if (estadoPermisos.camara !is EstadoPermiso.Concedido) {
                    add(Manifest.permission.CAMERA)
                }
                if (estadoPermisos.ubicacion !is EstadoPermiso.Concedido) {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }
            if (permisosPendientes.isNotEmpty()) {
                lanzadorPermisos.launch(permisosPendientes.toTypedArray())
            }
        }
    }

    // Al montar el composable se solicitan automaticamente los permisos que falten.
    // Si todos ya estaban concedidos, el dialogo no se muestra.
    LaunchedEffect(Unit) {
        val hayPermisosPendientes =
            estadoPermisos.camara !is EstadoPermiso.Concedido ||
                    estadoPermisos.ubicacion !is EstadoPermiso.Concedido
        if (hayPermisosPendientes) {
            solicitarPermisos()
        }
    }

    // Proporciona el estado actualizado y la funcion de solicitud al contenido hijo.
    contenido(estadoPermisos, solicitarPermisos)
}

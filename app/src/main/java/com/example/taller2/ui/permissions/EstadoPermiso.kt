package com.example.taller2.ui.permissions

/**
 * Representa el estado actual de un permiso dentro de la app.
 *
 * Se usa para decidir si una seccion puede mostrarse normalmente,
 * si debemos pedir el permiso de nuevo o si conviene enviar al usuario a ajustes.
 */
sealed interface EstadoPermiso {
    data object Concedido : EstadoPermiso
    data object Denegado : EstadoPermiso
    data object DenegadoPermanentemente : EstadoPermiso
}

/**
 * Estado agregado de los permisos que usa la app en esta etapa.
 */
data class EstadoPermisos(
    val camara: EstadoPermiso,
    val ubicacion: EstadoPermiso
)

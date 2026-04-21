package com.example.taller2.model

/**
 * Representa una foto tomada durante el recorrido junto con la ubicacion donde fue capturada.
 *
 * Esta clase servira como contrato comun para la futura integracion entre camara y mapa:
 * - la camara aportara los datos de la foto (id, uri y titulo)
 * - la capa de ubicacion aportara latitud y longitud
 * - el mapa podra usar esta misma informacion para crear marcadores reales
 */
data class GeoTaggedPhoto(
    val id: String,
    val photoUri: String,
    val title: String,
    val lat: Double,
    val lng: Double
)

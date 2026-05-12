package com.jornadasaludable.app.data.api.dto

import com.google.gson.annotations.SerializedName

data class ResumenDto(
    @SerializedName("semana_actual") val semanaActual: PeriodoResumen,
    @SerializedName("mes_actual")    val mesActual: PeriodoResumen,
    @SerializedName("anio_actual")   val anioActual: PeriodoResumen,
)

data class PeriodoResumen(
    @SerializedName("minutos_trabajados")    val minutosTrabajados: Int,
    @SerializedName("minutos_extra")         val minutosExtra: Int,
    @SerializedName("dias_trabajados")       val diasTrabajados: Int,
    @SerializedName("jornadas_total")        val jornadasTotal: Int,
    @SerializedName("horas_contrato")        val horasContrato: Double?,
    // Solo en mes_actual.
    @SerializedName("jornadas_incompletas")  val jornadasIncompletas: Int? = null,
)

data class JornadasIndexResponse(
    val items: List<JornadaListItemDto>,
    val limit: Int,
    val offset: Int,
)

data class JornadaListItemDto(
    val uuid: String,
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String?,
    @SerializedName("hora_fin")    val horaFin: String?,
    @SerializedName("minutos_trabajados") val minutosTrabajados: Int,
    @SerializedName("minutos_pausa")      val minutosPausa: Int,
    @SerializedName("minutos_extra")      val minutosExtra: Int,
    val estado: String,
)

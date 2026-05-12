package com.jornadasaludable.app.data.api.dto

import com.google.gson.annotations.SerializedName

data class BurnoutResponse(
    val items: List<BurnoutEvaluacionDto>,
    val actual: BurnoutEvaluacionDto?,
    val limit: Int,
    val offset: Int,
)

data class BurnoutEvaluacionDto(
    @SerializedName("fecha_evaluacion")    val fechaEvaluacion: String?,
    @SerializedName("horas_promedio_dia")  val horasPromedioDia: Double?,
    @SerializedName("dias_sin_descanso")   val diasSinDescanso: Int?,
    @SerializedName("jornadas_excesivas")  val jornadasExcesivas: Int?,
    val puntuacion: Double?,
    val nivel: String?,
)

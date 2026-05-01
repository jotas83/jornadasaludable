package com.jornadasaludable.app.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * GET /burnout — historial de evaluaciones + más reciente.
 */
data class BurnoutResponse(
    val items: List<BurnoutEvaluacionDto>,
    /** Última evaluación; null si nunca se ha evaluado. */
    val actual: BurnoutEvaluacionDto?,
    val limit: Int,
    val offset: Int,
)

data class BurnoutEvaluacionDto(
    /** Null cuando viene del bloque `burnout` de POST /alertas/generar (no incluye fecha). */
    @SerializedName("fecha_evaluacion")    val fechaEvaluacion: String?,
    @SerializedName("horas_promedio_dia")  val horasPromedioDia: Double?,
    @SerializedName("dias_sin_descanso")   val diasSinDescanso: Int?,
    @SerializedName("jornadas_excesivas")  val jornadasExcesivas: Int?,
    val puntuacion: Double?,
    /** BAJO / MODERADO / ALTO / CRITICO */
    val nivel: String?,
)

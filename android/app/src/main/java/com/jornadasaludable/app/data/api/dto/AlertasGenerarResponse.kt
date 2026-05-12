package com.jornadasaludable.app.data.api.dto

data class AlertasGenerarResponse(
    val generadas: Int,
    val alertas: List<AlertaDto>,
    val burnout: BurnoutEvaluacionDto?,
)

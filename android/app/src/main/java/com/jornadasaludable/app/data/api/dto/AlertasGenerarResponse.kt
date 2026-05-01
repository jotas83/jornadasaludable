package com.jornadasaludable.app.data.api.dto

/**
 * POST /alertas/generar — el backend devuelve las recién generadas (que
 * pasaron el dedup), su número, y la evaluación de burnout actualizada.
 */
data class AlertasGenerarResponse(
    val generadas: Int,
    val alertas: List<AlertaDto>,
    val burnout: BurnoutEvaluacionDto?,
)

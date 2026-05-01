package com.jornadasaludable.app.ui.estadisticas

import com.jornadasaludable.app.data.api.dto.DocumentoDto
import java.time.YearMonth

data class InformesTabUiState(
    val mesSeleccionado: YearMonth = YearMonth.now(),
    val generando: Boolean = false,
    val documentos: List<DocumentoDto> = emptyList(),
    val loadingList: Boolean = true,
    val errorList: String? = null,
    val transientMessage: String? = null,
)

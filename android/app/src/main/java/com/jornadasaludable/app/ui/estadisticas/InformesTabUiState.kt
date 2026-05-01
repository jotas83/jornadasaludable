package com.jornadasaludable.app.ui.estadisticas

import com.jornadasaludable.app.data.api.dto.DocumentoDto
import java.io.File
import java.time.YearMonth

data class InformesTabUiState(
    val mesSeleccionado: YearMonth = YearMonth.now(),
    val generando: Boolean = false,
    val documentos: List<DocumentoDto> = emptyList(),
    val loadingList: Boolean = true,
    val errorList: String? = null,
    val transientMessage: String? = null,
    /** UUID del documento siendo descargado en este momento (para mostrar spinner). */
    val downloadingUuid: String? = null,
    /**
     * Fichero PDF listo para abrir. El Fragment lo consume con
     * `consumePendingOpenFile()` tras lanzar el Intent ACTION_VIEW.
     */
    val pendingOpenFile: File? = null,
)

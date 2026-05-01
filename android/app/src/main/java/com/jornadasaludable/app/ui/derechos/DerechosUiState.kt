package com.jornadasaludable.app.ui.derechos

import com.jornadasaludable.app.data.api.dto.CategoriaDto
import com.jornadasaludable.app.data.api.dto.DerechoListItemDto

data class DerechosUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val categorias: List<CategoriaDto> = emptyList(),
    /** consultasCount aproximado por categoría (suma de los items). */
    val articulosPorCategoria: Map<String, Int> = emptyMap(),
    /** Top N más consultados global. */
    val masConsultados: List<DerechoListItemDto> = emptyList(),
    /** Resultados de búsqueda; null = sin búsqueda activa. */
    val searchQuery: String? = null,
    val searchResults: List<DerechoListItemDto>? = null,
    val searching: Boolean = false,
)

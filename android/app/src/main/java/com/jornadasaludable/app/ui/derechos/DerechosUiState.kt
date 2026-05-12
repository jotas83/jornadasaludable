package com.jornadasaludable.app.ui.derechos

import com.jornadasaludable.app.data.api.dto.CategoriaDto
import com.jornadasaludable.app.data.api.dto.DerechoListItemDto

data class DerechosUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val categorias: List<CategoriaDto> = emptyList(),
    val articulosPorCategoria: Map<String, Int> = emptyMap(),
    val masConsultados: List<DerechoListItemDto> = emptyList(),
    // null = sin búsqueda activa
    val searchQuery: String? = null,
    val searchResults: List<DerechoListItemDto>? = null,
    val searching: Boolean = false,
)

package com.jornadasaludable.app.ui.derechos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.api.dto.DerechoListItemDto
import com.jornadasaludable.app.data.repository.DerechoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * VM principal de Mis Derechos. Carga categorías, agrega los contenidos por
 * categoría (para conocer counts) y mantiene un estado de búsqueda separado.
 *
 * "Más consultados" (top N) se obtiene tras agregar todos los items: la
 * API no expone un endpoint global de derechos, así que componemos la lista
 * a partir de los contenidos de cada categoría.
 */
@HiltViewModel
class DerechosViewModel @Inject constructor(
    private val derechoRepository: DerechoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DerechosUiState())
    val state: StateFlow<DerechosUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }

            val cats = derechoRepository.categorias().getOrElse {
                _state.update { st -> st.copy(loading = false, errorMessage = it.message) }
                return@launch
            }

            // Cargar contenidos de cada categoría en paralelo
            val porCategoria = cats.associate { cat ->
                val key = cat.codigo
                key to async { derechoRepository.contenidosByCategoria(cat.codigo) }
            }
            val resultados = porCategoria.mapValues { (_, deferred) ->
                deferred.await().getOrDefault(emptyList())
            }
            val countsMap   = resultados.mapValues { it.value.size }
            val allItems    = resultados.values.flatten()
            // Sin consultas_count en el listado, usamos `orden` ascendente como
            // proxy "destacados" del catálogo. El backend ordena así por defecto.
            val mas = allItems.distinctBy { it.codigo }.take(5)

            _state.update {
                it.copy(
                    loading = false,
                    categorias = cats,
                    articulosPorCategoria = countsMap,
                    masConsultados = mas,
                    errorMessage = null,
                )
            }
        }
    }

    /** Búsqueda con debounce muy ligero — el caller decide cuándo invocar. */
    fun buscar(query: String) {
        val q = query.trim()
        if (q.length < 3) {
            _state.update { it.copy(searchQuery = null, searchResults = null, searching = false) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(searchQuery = q, searching = true) }
            val res = derechoRepository.buscar(q).getOrDefault(emptyList())
            _state.update { it.copy(searchResults = res, searching = false) }
        }
    }

    fun clearSearch() {
        _state.update { it.copy(searchQuery = null, searchResults = null, searching = false) }
    }
}

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

// VM de "Mis Derechos": categorías + contenidos por categoría + búsqueda.
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

            val porCategoria = cats.associate { cat ->
                val key = cat.codigo
                key to async { derechoRepository.contenidosByCategoria(cat.codigo) }
            }
            val resultados = porCategoria.mapValues { (_, deferred) ->
                deferred.await().getOrDefault(emptyList())
            }
            val countsMap   = resultados.mapValues { it.value.size }
            val allItems    = resultados.values.flatten()
            // "Más consultados" se aproxima por el `orden` del catálogo (no hay endpoint global).
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

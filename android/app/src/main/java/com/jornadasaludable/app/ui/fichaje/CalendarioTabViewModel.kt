package com.jornadasaludable.app.ui.fichaje

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.repository.JornadaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarioTabViewModel @Inject constructor(
    private val jornadaRepository: JornadaRepository,
) : ViewModel() {

    private val _mes = MutableStateFlow(YearMonth.now())

    private val _state = MutableStateFlow<CalendarioTabUiState>(CalendarioTabUiState.Loading)
    val state: StateFlow<CalendarioTabUiState> = _state.asStateFlow()

    init { load(_mes.value) }

    fun load(mes: YearMonth = _mes.value) {
        _mes.value = mes
        viewModelScope.launch {
            _state.value = CalendarioTabUiState.Loading
            val mesStr = "%04d-%02d".format(mes.year, mes.monthValue)
            jornadaRepository.jornadasOfMonth(mesStr)
                .onSuccess { resp ->
                    _state.value = CalendarioTabUiState.Ready(
                        mes              = mes,
                        jornadasPorFecha = resp.items.associateBy { it.fecha },
                    )
                }
                .onFailure {
                    _state.value = CalendarioTabUiState.Error(
                        it.message ?: "Error cargando jornadas del mes."
                    )
                }
        }
    }

    fun mesAnterior()  = load(_mes.value.minusMonths(1))
    fun mesSiguiente() = load(_mes.value.plusMonths(1))
}

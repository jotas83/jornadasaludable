package com.jornadasaludable.app.ui.estadisticas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.repository.DocumentoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class InformesTabViewModel @Inject constructor(
    private val documentoRepository: DocumentoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(InformesTabUiState())
    val state: StateFlow<InformesTabUiState> = _state.asStateFlow()

    init { reloadList() }

    fun setMes(mes: YearMonth) {
        _state.update { it.copy(mesSeleccionado = mes) }
    }

    fun reloadList() {
        viewModelScope.launch {
            _state.update { it.copy(loadingList = true, errorList = null) }
            documentoRepository.list()
                .onSuccess { docs ->
                    _state.update { it.copy(loadingList = false, documentos = docs) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loadingList = false, errorList = e.message ?: "Error cargando documentos.") }
                }
        }
    }

    fun generar() {
        val current = _state.value
        if (current.generando) return
        viewModelScope.launch {
            _state.update { it.copy(generando = true, transientMessage = null) }
            val mesStr = "%04d-%02d".format(current.mesSeleccionado.year, current.mesSeleccionado.monthValue)
            documentoRepository.generarMensual(mesStr)
                .onSuccess { resp ->
                    _state.update { it.copy(generando = false, transientMessage = "PDF generado: ${resp.nombreFichero}") }
                    reloadList()
                }
                .onFailure { e ->
                    _state.update { it.copy(generando = false, transientMessage = "Error: ${e.message ?: "no se pudo generar."}") }
                }
        }
    }

    /**
     * Descarga el PDF a `cacheDir/pdf_{uuid}.pdf` y emite `pendingOpenFile`
     * para que el Fragment lance Intent ACTION_VIEW vía FileProvider.
     */
    fun openDocumento(uuid: String, cacheDir: File) {
        if (_state.value.downloadingUuid != null) return
        viewModelScope.launch {
            _state.update { it.copy(downloadingUuid = uuid, transientMessage = null) }
            val target = File(cacheDir, "pdf_$uuid.pdf")
            documentoRepository.download(uuid, target)
                .onSuccess { file ->
                    _state.update { it.copy(downloadingUuid = null, pendingOpenFile = file) }
                }
                .onFailure { e ->
                    _state.update { it.copy(
                        downloadingUuid = null,
                        transientMessage = "Error descargando: ${e.message ?: "."}",
                    ) }
                }
        }
    }

    fun consumePendingOpenFile() {
        _state.update { it.copy(pendingOpenFile = null) }
    }

    fun consumeMessage() {
        _state.update { it.copy(transientMessage = null) }
    }
}

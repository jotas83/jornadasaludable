package com.jornadasaludable.app.ui.fichaje

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jornadasaludable.app.R
import com.jornadasaludable.app.data.api.dto.JornadaListItemDto
import com.jornadasaludable.app.databinding.FragmentCalendarioTabBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@AndroidEntryPoint
class CalendarioTabFragment : Fragment() {

    private var _binding: FragmentCalendarioTabBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalendarioTabViewModel by viewModels()
    private lateinit var adapter: CalendarioAdapter

    private val esLocale = Locale("es", "ES")
    private val mesFormatter  = DateTimeFormatter.ofPattern("MMMM yyyy", esLocale)
    private val diaFormatter  = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(esLocale)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCalendarioTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CalendarioAdapter(::showDiaDetail)
        binding.rvDias.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvDias.adapter = adapter

        binding.btnAnterior.setOnClickListener  { viewModel.mesAnterior() }
        binding.btnSiguiente.setOnClickListener { viewModel.mesSiguiente() }
        binding.btnRetry.setOnClickListener     { viewModel.load() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: CalendarioTabUiState) {
        binding.progress.isVisible       = state is CalendarioTabUiState.Loading
        binding.errorContainer.isVisible = state is CalendarioTabUiState.Error
        binding.contentContainer.isVisible = state is CalendarioTabUiState.Ready

        when (state) {
            is CalendarioTabUiState.Error -> binding.tvError.text = state.message
            is CalendarioTabUiState.Ready -> {
                binding.tvMes.text = state.mes.atDay(1).format(mesFormatter)
                    .replaceFirstChar { it.uppercase() }
                adapter.submit(state.mes, state.jornadasPorFecha)
            }
            else -> Unit
        }
    }

    private fun showDiaDetail(fecha: LocalDate, jornada: JornadaListItemDto?) {
        val titulo = fecha.format(diaFormatter).replaceFirstChar { it.uppercase() }
        val msg = if (jornada == null) {
            getString(R.string.cal_dia_sin_jornada)
        } else {
            buildString {
                appendLine("Estado: ${jornada.estado}")
                appendLine("Inicio: ${jornada.horaInicio ?: "—"}")
                appendLine("Fin:    ${jornada.horaFin ?: "—"}")
                appendLine("Trabajado: ${formatMinutos(jornada.minutosTrabajados)}")
                if (jornada.minutosPausa > 0) appendLine("Pausa: ${formatMinutos(jornada.minutosPausa)}")
                if (jornada.minutosExtra > 0) appendLine("Extra: ${formatMinutos(jornada.minutosExtra)}")
            }.trimEnd()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setMessage(msg)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun formatMinutos(min: Int): String {
        if (min < 60) return "${min}min"
        val h = min / 60
        val m = min % 60
        return if (m == 0) "${h}h" else "${h}h ${m}min"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvDias.adapter = null
        _binding = null
    }
}

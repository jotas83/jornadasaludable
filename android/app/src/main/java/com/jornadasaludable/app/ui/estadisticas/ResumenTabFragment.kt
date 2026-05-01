package com.jornadasaludable.app.ui.estadisticas

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
import com.jornadasaludable.app.data.api.dto.PeriodoResumen
import com.jornadasaludable.app.databinding.FragmentResumenTabBinding
import com.jornadasaludable.app.databinding.IncludePeriodoCardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ResumenTabFragment : Fragment() {

    private var _binding: FragmentResumenTabBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ResumenTabViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentResumenTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnRetry.setOnClickListener { viewModel.load() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: ResumenTabUiState) {
        binding.progress.isVisible = state is ResumenTabUiState.Loading
        binding.errorContainer.isVisible = state is ResumenTabUiState.Error
        binding.contentContainer.isVisible = state is ResumenTabUiState.Success

        when (state) {
            is ResumenTabUiState.Error -> binding.tvError.text = state.message
            is ResumenTabUiState.Success -> {
                renderCard(binding.cardSemana, "Esta semana", state.resumen.semanaActual)
                renderCard(binding.cardMes,    "Este mes",    state.resumen.mesActual)
                renderCard(binding.cardAnio,   "Este año",    state.resumen.anioActual)
            }
            else -> Unit
        }
    }

    private fun renderCard(card: IncludePeriodoCardBinding, titulo: String, p: PeriodoResumen) {
        card.tvTitulo.text = titulo
        val realMin = p.minutosTrabajados
        val esperadoMin = ((p.horasContrato ?: 0.0) * 60).toInt()
        val pct = when {
            esperadoMin <= 0       -> 0
            realMin >= esperadoMin -> 100
            else -> ((realMin * 100.0) / esperadoMin).toInt()
        }
        card.progressBar.progress = pct
        card.tvReal.text     = formatHoras(realMin)
        card.tvEsperado.text = if (esperadoMin > 0) formatHoras(esperadoMin) else "—"

        val diff = realMin - esperadoMin
        card.tvDiferencia.text = if (esperadoMin <= 0) "—" else when {
            diff > 0 -> "+${formatHoras(diff)}"
            diff < 0 -> "−${formatHoras(-diff)}"
            else     -> "0h"
        }
    }

    private fun formatHoras(min: Int): String {
        val h = min / 60
        val m = min % 60
        return if (m == 0) "${h}h" else "${h}h ${m}min"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

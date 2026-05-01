package com.jornadasaludable.app.ui.dashboard

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
import com.jornadasaludable.app.databinding.FragmentDashboardTabBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardTabFragment : Fragment() {

    private var _binding: FragmentDashboardTabBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardTabViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDashboardTabBinding.inflate(inflater, container, false)
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

    private fun render(state: DashboardTabUiState) {
        binding.progress.isVisible = state is DashboardTabUiState.Loading
        binding.errorContainer.isVisible = state is DashboardTabUiState.Error
        binding.contentContainer.isVisible = state is DashboardTabUiState.Success

        when (state) {
            is DashboardTabUiState.Error -> binding.tvError.text = state.message
            is DashboardTabUiState.Success -> renderSuccess(state)
            else -> Unit
        }
    }

    private fun renderSuccess(s: DashboardTabUiState.Success) {
        // ----- Card 1: estado de la jornada -----
        val jornada = s.jornadaHoy
        binding.tvJornadaEstado.text = when {
            jornada == null               -> getString(com.jornadasaludable.app.R.string.dashboard_sin_fichaje)
            jornada.estado == "ABIERTA"   -> getString(com.jornadasaludable.app.R.string.dashboard_jornada_abierta)
            jornada.estado == "INCOMPLETA"-> getString(com.jornadasaludable.app.R.string.dashboard_jornada_incompleta)
            jornada.estado == "CERRADA"   -> getString(com.jornadasaludable.app.R.string.dashboard_jornada_cerrada)
            jornada.estado == "VALIDADA"  -> getString(com.jornadasaludable.app.R.string.dashboard_jornada_validada)
            else -> jornada.estado
        }
        val minutosHoy = jornada?.minutosTrabajados ?: 0
        binding.tvJornadaMinutos.text = formatMinutos(minutosHoy) + " hoy"

        // ----- Card 2: resumen semanal -----
        val sem = s.resumen.semanaActual
        val real    = sem.minutosTrabajados
        val esperado = ((sem.horasContrato ?: 0.0) * 60).toInt()
        val pct = when {
            esperado <= 0       -> 0
            real >= esperado    -> 100
            else                -> ((real * 100.0) / esperado).toInt()
        }
        binding.progressSemanal.progress = pct
        binding.tvSemanalReal.text     = formatMinutos(real)
        binding.tvSemanalEsperado.text = if (esperado > 0) formatMinutos(esperado) else "—"
        binding.tvSemanalPct.text      = "$pct%"
        binding.tvSemanalDias.text     = getString(
            com.jornadasaludable.app.R.string.dashboard_dias_trabajados,
            sem.diasTrabajados
        )

        // ----- Card 3: alerta sin leer (visibility según haya o no) -----
        val alerta = s.alertaSinLeer
        binding.cardAlerta.isVisible = alerta != null
        if (alerta != null) {
            binding.tvAlertaTitulo.text = "${alerta.tipoNombre}  ·  ${alerta.nivel}"
            binding.tvAlertaMensaje.text = alerta.mensaje
        }
    }

    /** "75 min" → "1h 15min". */
    private fun formatMinutos(min: Int): String {
        if (min < 60) return "${min}min"
        val h = min / 60
        val m = min % 60
        return if (m == 0) "${h}h" else "${h}h ${m}min"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

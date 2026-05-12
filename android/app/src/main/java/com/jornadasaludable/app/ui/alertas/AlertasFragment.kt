package com.jornadasaludable.app.ui.alertas

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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.jornadasaludable.app.R
import com.jornadasaludable.app.data.api.dto.SobrecargaEvaluacionDto
import com.jornadasaludable.app.databinding.FragmentAlertasBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// Pantalla Alertas: card de sobrecarga laboral arriba + tabs Hoy/Semana/Mes.
@AndroidEntryPoint
class AlertasFragment : Fragment() {

    private var _binding: FragmentAlertasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlertasViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAlertasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewPager.adapter = AlertasPagerAdapter(this)
        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0    -> getString(R.string.alertas_tab_hoy)
                1    -> getString(R.string.alertas_tab_semana)
                else -> getString(R.string.alertas_tab_mes)
            }
        }.attach()

        binding.btnRetrySobrecarga.setOnClickListener { viewModel.load() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    renderSobrecarga(state.sobrecarga)
                    state.transientMessage?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    private fun renderSobrecarga(loadable: Loadable<SobrecargaEvaluacionDto?>) {
        binding.progressSobrecarga.isVisible = loadable is Loadable.Loading
        binding.errorSobrecarga.isVisible    = loadable is Loadable.Error
        binding.contentSobrecarga.isVisible  = loadable is Loadable.Ready

        when (loadable) {
            is Loadable.Error -> binding.tvSobrecargaError.text = loadable.message
            is Loadable.Ready -> {
                val ev = loadable.data
                if (ev == null) {
                    binding.tvSobrecargaNivel.text = getString(R.string.alertas_sobrecarga_sin_datos)
                    binding.progressSobrecargaScore.progress = 0
                    binding.tvSobrecargaPuntuacion.text = "—"
                    binding.tvSobrecargaHoras.text       = "—"
                    binding.tvSobrecargaExcesivas.text   = "—"
                    binding.tvSobrecargaDiasSin.text     = "—"
                } else {
                    binding.tvSobrecargaNivel.text = ev.nivel ?: "—"
                    val score = (ev.puntuacion ?: 0.0).toInt().coerceIn(0, 100)
                    binding.progressSobrecargaScore.progress = score
                    binding.progressSobrecargaScore.setIndicatorColor(
                        requireContext().getColor(colorForSobrecargaNivel(ev.nivel))
                    )
                    binding.tvSobrecargaPuntuacion.text =
                        "%.1f".format(ev.puntuacion ?: 0.0)
                    binding.tvSobrecargaHoras.text     = "%.1f h".format(ev.horasPromedioDia ?: 0.0)
                    binding.tvSobrecargaExcesivas.text = (ev.jornadasExcesivas ?: 0).toString()
                    binding.tvSobrecargaDiasSin.text   = (ev.diasSinDescanso ?: 0).toString()
                }
            }
            else -> Unit
        }
    }

    private fun colorForSobrecargaNivel(nivel: String?): Int = when (nivel) {
        "BAJO"      -> R.color.sobrecarga_bajo
        "MODERADO"  -> R.color.sobrecarga_moderado
        "ALTO"      -> R.color.sobrecarga_alto
        "CRITICO"   -> R.color.sobrecarga_critico
        else        -> R.color.sobrecarga_bajo
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager.adapter = null
        _binding = null
    }
}

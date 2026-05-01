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
import com.jornadasaludable.app.data.api.dto.BurnoutEvaluacionDto
import com.jornadasaludable.app.databinding.FragmentAlertasBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Pantalla Alertas. Estructura:
 *   - Card fija arriba con indicador de burnout + 4 métricas.
 *   - TabLayout + ViewPager2 con 3 períodos: Hoy / Semana / Mes.
 *
 * Cada sub-fragmento (AlertaListFragment) toma el AlertasViewModel del
 * padre y filtra client-side por su período.
 */
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

        binding.btnRetryBurnout.setOnClickListener { viewModel.load() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    renderBurnout(state.burnout)
                    state.transientMessage?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    private fun renderBurnout(loadable: Loadable<BurnoutEvaluacionDto?>) {
        binding.progressBurnout.isVisible = loadable is Loadable.Loading
        binding.errorBurnout.isVisible    = loadable is Loadable.Error
        binding.contentBurnout.isVisible  = loadable is Loadable.Ready

        when (loadable) {
            is Loadable.Error -> binding.tvBurnoutError.text = loadable.message
            is Loadable.Ready -> {
                val ev = loadable.data
                if (ev == null) {
                    binding.tvBurnoutNivel.text = getString(R.string.alertas_burnout_sin_datos)
                    binding.progressBurnoutScore.progress = 0
                    binding.tvBurnoutPuntuacion.text = "—"
                    binding.tvBurnoutHoras.text       = "—"
                    binding.tvBurnoutExcesivas.text   = "—"
                    binding.tvBurnoutDiasSin.text     = "—"
                } else {
                    binding.tvBurnoutNivel.text = ev.nivel ?: "—"
                    val score = (ev.puntuacion ?: 0.0).toInt().coerceIn(0, 100)
                    binding.progressBurnoutScore.progress = score
                    binding.progressBurnoutScore.setIndicatorColor(
                        requireContext().getColor(colorForBurnoutNivel(ev.nivel))
                    )
                    binding.tvBurnoutPuntuacion.text =
                        "%.1f".format(ev.puntuacion ?: 0.0)
                    binding.tvBurnoutHoras.text     = "%.1f h".format(ev.horasPromedioDia ?: 0.0)
                    binding.tvBurnoutExcesivas.text = (ev.jornadasExcesivas ?: 0).toString()
                    binding.tvBurnoutDiasSin.text   = (ev.diasSinDescanso ?: 0).toString()
                }
            }
            else -> Unit
        }
    }

    private fun colorForBurnoutNivel(nivel: String?): Int = when (nivel) {
        "BAJO"      -> R.color.burnout_bajo
        "MODERADO"  -> R.color.burnout_moderado
        "ALTO"      -> R.color.burnout_alto
        "CRITICO"   -> R.color.burnout_critico
        else        -> R.color.burnout_bajo
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager.adapter = null
        _binding = null
    }
}

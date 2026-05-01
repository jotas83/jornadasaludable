package com.jornadasaludable.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.jornadasaludable.app.R
import com.jornadasaludable.app.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Pantalla post-login. Hostea dos pestañas en un ViewPager2:
 *   0 → DashboardTabFragment  (estado de jornada + resumen semanal + alertas)
 *   1 → PerfilTabFragment     (datos del usuario + empresa + logout)
 *
 * No tiene lógica propia — solo cosé TabLayout + ViewPager2.
 */
@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewPager.adapter = DashboardPagerAdapter(this)
        // Desactivar swipe horizontal para evitar conflictos con scrolls
        // verticales dentro de las tabs (no es necesario, lo dejamos activo).

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0    -> getString(R.string.tab_dashboard)
                else -> getString(R.string.tab_perfil)
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager.adapter = null  // evita leaks del FragmentStateAdapter
        _binding = null
    }
}

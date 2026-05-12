package com.jornadasaludable.app.ui.fichaje

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.jornadasaludable.app.R
import com.jornadasaludable.app.databinding.FragmentFichajeBinding
import dagger.hilt.android.AndroidEntryPoint

// Dos tabs: Fichar y Calendario.
@AndroidEntryPoint
class FichajeFragment : Fragment() {

    private var _binding: FragmentFichajeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFichajeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewPager.adapter = FichajePagerAdapter(this)

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0    -> getString(R.string.tab_fichar)
                else -> getString(R.string.tab_calendario)
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager.adapter = null
        _binding = null
    }
}

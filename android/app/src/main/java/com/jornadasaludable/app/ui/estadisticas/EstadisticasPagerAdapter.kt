package com.jornadasaludable.app.ui.estadisticas

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class EstadisticasPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2
    override fun createFragment(position: Int): Fragment = when (position) {
        0    -> ResumenTabFragment()
        else -> InformesTabFragment()
    }
}

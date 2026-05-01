package com.jornadasaludable.app.ui.fichaje

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class FichajePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2
    override fun createFragment(position: Int): Fragment = when (position) {
        0    -> FicharTabFragment()
        else -> CalendarioTabFragment()
    }
}

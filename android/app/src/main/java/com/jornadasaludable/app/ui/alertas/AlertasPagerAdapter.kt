package com.jornadasaludable.app.ui.alertas

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class AlertasPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment =
        AlertaListFragment.newInstance(AlertaPeriod.fromTabIndex(position))
}

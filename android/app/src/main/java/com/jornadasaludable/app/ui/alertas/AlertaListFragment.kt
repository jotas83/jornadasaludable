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
import androidx.recyclerview.widget.LinearLayoutManager
import com.jornadasaludable.app.databinding.FragmentAlertaListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlertaListFragment : Fragment() {

    private var _binding: FragmentAlertaListBinding? = null
    private val binding get() = _binding!!

    private val parentVM: AlertasViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: AlertaListAdapter

    private val period: AlertaPeriod
        get() = AlertaPeriod.valueOf(requireArguments().getString(ARG_PERIOD)!!)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAlertaListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AlertaListAdapter(
            onMarcarLeida = { uuid -> parentVM.marcarLeida(uuid) },
        )
        binding.rvAlertas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAlertas.adapter = adapter

        binding.btnRetry.setOnClickListener { parentVM.load() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                parentVM.state.collect { state ->
                    binding.progress.isVisible = state.alertas is Loadable.Loading
                    binding.errorContainer.isVisible = state.alertas is Loadable.Error
                    binding.emptyContainer.isVisible = false

                    when (val a = state.alertas) {
                        is Loadable.Error -> binding.tvError.text = a.message
                        is Loadable.Ready -> {
                            val filtradas = a.data.filterByPeriod(period)
                            adapter.submit(filtradas, state.markingUuid)
                            binding.rvAlertas.isVisible = filtradas.isNotEmpty()
                            binding.emptyContainer.isVisible = filtradas.isEmpty()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvAlertas.adapter = null
        _binding = null
    }

    companion object {
        private const val ARG_PERIOD = "period"
        fun newInstance(period: AlertaPeriod): AlertaListFragment =
            AlertaListFragment().apply {
                arguments = Bundle().apply { putString(ARG_PERIOD, period.name) }
            }
    }
}

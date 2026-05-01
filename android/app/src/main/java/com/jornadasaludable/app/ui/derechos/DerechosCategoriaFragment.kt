package com.jornadasaludable.app.ui.derechos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.jornadasaludable.app.R
import com.jornadasaludable.app.databinding.FragmentDerechosCategoriaBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DerechosCategoriaFragment : Fragment() {

    private var _binding: FragmentDerechosCategoriaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DerechosCategoriaViewModel by viewModels()
    private lateinit var adapter: DerechoListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDerechosCategoriaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DerechoListAdapter { d ->
            findNavController().navigate(
                R.id.action_derechosCategoria_to_detail,
                bundleOf("codigo" to d.codigo),
            )
        }
        binding.rvDerechos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDerechos.adapter = adapter

        binding.btnRetry.setOnClickListener { viewModel.load() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(s: DerechosCategoriaUiState) {
        binding.progress.isVisible       = s is DerechosCategoriaUiState.Loading
        binding.errorContainer.isVisible = s is DerechosCategoriaUiState.Error
        binding.contentContainer.isVisible = s is DerechosCategoriaUiState.Success

        when (s) {
            is DerechosCategoriaUiState.Error -> binding.tvError.text = s.message
            is DerechosCategoriaUiState.Success -> {
                binding.tvCategoriaTitulo.text = s.nombreCategoria
                if (s.derechos.isEmpty()) {
                    binding.tvEmpty.isVisible = true
                    binding.rvDerechos.isVisible = false
                } else {
                    binding.tvEmpty.isVisible = false
                    binding.rvDerechos.isVisible = true
                    adapter.submit(s.derechos)
                }
            }
            else -> Unit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvDerechos.adapter = null
        _binding = null
    }
}

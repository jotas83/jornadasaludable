package com.jornadasaludable.app.ui.derechos

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
import com.jornadasaludable.app.databinding.FragmentDerechoDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DerechoDetailFragment : Fragment() {

    private var _binding: FragmentDerechoDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DerechoDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDerechoDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnRetry.setOnClickListener { viewModel.load() }

        val markwon = Markwon.create(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progress.isVisible = state is DerechoDetailUiState.Loading
                    binding.errorContainer.isVisible = state is DerechoDetailUiState.Error
                    binding.contentContainer.isVisible = state is DerechoDetailUiState.Success

                    when (state) {
                        is DerechoDetailUiState.Error -> binding.tvError.text = state.message
                        is DerechoDetailUiState.Success -> {
                            val d = state.derecho
                            binding.tvCategoria.text = d.categoriaNombre
                            binding.tvTitulo.text    = d.titulo
                            binding.tvArticulo.text  = d.articuloReferencia
                            binding.tvResumen.text   = d.resumen
                            markwon.setMarkdown(binding.tvContenido, d.contenido)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

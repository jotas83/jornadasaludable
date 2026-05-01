package com.jornadasaludable.app.ui.derechos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jornadasaludable.app.R
import com.jornadasaludable.app.databinding.FragmentDerechosBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DerechosFragment : Fragment() {

    private var _binding: FragmentDerechosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DerechosViewModel by viewModels()
    private lateinit var categoriaAdapter: CategoriaAdapter
    private lateinit var derechoAdapter:   DerechoListAdapter
    private lateinit var searchAdapter:    DerechoListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDerechosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val openDetail: (String) -> Unit = { codigo ->
            findNavController().navigate(
                R.id.action_derechos_to_detail,
                bundleOf("codigo" to codigo),
            )
        }

        categoriaAdapter = CategoriaAdapter { cat ->
            // Al pulsar una categoría, abrimos un dialog con los items para
            // mantener el scope reducido. Otra opción sería navegar a otra
            // pantalla; lo dejamos como mejora futura.
            viewModel.buscar(cat.nombre)  // shortcut: filtramos por nombre
        }
        derechoAdapter = DerechoListAdapter { d -> openDetail(d.codigo) }
        searchAdapter  = DerechoListAdapter { d -> openDetail(d.codigo) }

        binding.rvCategorias.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCategorias.adapter = categoriaAdapter

        binding.rvMasConsultados.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMasConsultados.adapter = derechoAdapter

        binding.rvSearch.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearch.adapter = searchAdapter

        binding.btnRetry.setOnClickListener { viewModel.load() }

        binding.etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val txt = s?.toString().orEmpty()
                if (txt.isBlank()) viewModel.clearSearch()
                else viewModel.buscar(txt)
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(s: DerechosUiState) {
        binding.progress.isVisible = s.loading
        binding.errorContainer.isVisible = s.errorMessage != null
        s.errorMessage?.let { binding.tvError.text = it }
        binding.contentContainer.isVisible = !s.loading && s.errorMessage == null

        categoriaAdapter.submit(s.categorias, s.articulosPorCategoria)
        derechoAdapter.submit(s.masConsultados)

        // Modo búsqueda: oculta categorías + más consultados, muestra resultados.
        val searching = s.searchQuery != null
        binding.searchSection.isVisible = searching
        binding.normalSection.isVisible = !searching
        if (searching) {
            binding.tvSearchTitle.text = if (s.searching) {
                getString(R.string.derechos_buscando)
            } else {
                getString(R.string.derechos_resultados, s.searchResults?.size ?: 0, s.searchQuery!!)
            }
            searchAdapter.submit(s.searchResults ?: emptyList())
            binding.tvSearchEmpty.isVisible = !s.searching && (s.searchResults?.isEmpty() == true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvCategorias.adapter = null
        binding.rvMasConsultados.adapter = null
        binding.rvSearch.adapter = null
        _binding = null
    }
}

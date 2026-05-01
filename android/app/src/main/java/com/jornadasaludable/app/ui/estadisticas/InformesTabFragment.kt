package com.jornadasaludable.app.ui.estadisticas

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
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.jornadasaludable.app.databinding.FragmentInformesTabBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@AndroidEntryPoint
class InformesTabFragment : Fragment() {

    private var _binding: FragmentInformesTabBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InformesTabViewModel by viewModels()
    private lateinit var adapter: DocumentoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInformesTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DocumentoAdapter()
        binding.rvDocumentos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDocumentos.adapter = adapter

        binding.btnSeleccionarMes.setOnClickListener { showMesPicker() }
        binding.btnGenerar.setOnClickListener { viewModel.generar() }
        binding.btnRefresh.setOnClickListener { viewModel.reloadList() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(s: InformesTabUiState) {
        binding.tvMesSeleccionado.text = "%04d-%02d".format(s.mesSeleccionado.year, s.mesSeleccionado.monthValue)
        binding.btnGenerar.isEnabled = !s.generando
        binding.progressGenerar.isVisible = s.generando

        binding.progressList.isVisible = s.loadingList
        binding.tvErrorList.isVisible = s.errorList != null
        s.errorList?.let { binding.tvErrorList.text = it }

        binding.tvEmpty.isVisible = !s.loadingList && s.errorList == null && s.documentos.isEmpty()
        binding.rvDocumentos.isVisible = s.documentos.isNotEmpty()
        adapter.submit(s.documentos)

        s.transientMessage?.let {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    private fun showMesPicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Selecciona un día del mes")
            .build()
        picker.addOnPositiveButtonClickListener { selectionMs ->
            val ld = Instant.ofEpochMilli(selectionMs).atZone(ZoneId.systemDefault()).toLocalDate()
            viewModel.setMes(YearMonth.of(ld.year, ld.monthValue))
        }
        picker.show(parentFragmentManager, "mes_picker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvDocumentos.adapter = null
        _binding = null
    }
}

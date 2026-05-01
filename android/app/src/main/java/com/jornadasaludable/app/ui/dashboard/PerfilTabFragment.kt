package com.jornadasaludable.app.ui.dashboard

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
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.jornadasaludable.app.R
import com.jornadasaludable.app.databinding.FragmentPerfilTabBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PerfilTabFragment : Fragment() {

    private var _binding: FragmentPerfilTabBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PerfilTabViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPerfilTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRetry.setOnClickListener { viewModel.load() }

        binding.btnLogout.setOnClickListener {
            viewModel.logout {
                // requireActivity().findNavController(...) es robusto cuando
                // este fragmento está dentro de un ViewPager2 (donde el
                // findNavController del child fragment puede no encontrar
                // el NavHost).
                requireActivity()
                    .findNavController(R.id.nav_host_fragment)
                    .navigate(
                        R.id.action_dashboard_to_login,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.dashboardFragment, inclusive = true)
                            .build()
                    )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: PerfilTabUiState) {
        binding.progress.isVisible        = state is PerfilTabUiState.Loading
        binding.errorContainer.isVisible  = state is PerfilTabUiState.Error
        binding.contentContainer.isVisible = state is PerfilTabUiState.Success

        when (state) {
            is PerfilTabUiState.Error -> binding.tvError.text = state.message
            is PerfilTabUiState.Success -> renderSuccess(state)
            else -> Unit
        }
    }

    private fun renderSuccess(s: PerfilTabUiState.Success) {
        val p = s.perfil
        // Avatar: iniciales del nombre + apellidos
        val initials = listOfNotNull(
            p.nombre.firstOrNull()?.toString(),
            p.apellidos.firstOrNull()?.toString(),
        ).joinToString("").uppercase()
        binding.tvAvatar.text = initials.ifBlank { "?" }

        binding.tvNombre.text = "${p.nombre} ${p.apellidos}"
        binding.tvEmail.text  = p.email ?: "—"
        binding.tvNif.text    = p.nif

        val empresa = s.empresa
        binding.tvEmpresa.text = empresa?.razonSocial ?: getString(R.string.perfil_sin_contrato)
        binding.tvSector.text  = empresa?.sectorNombre ?: "—"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.jornadasaludable.app.ui.login

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
import androidx.navigation.fragment.findNavController
import com.jornadasaludable.app.R
import com.jornadasaludable.app.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val identifier = binding.tilIdentifier.editText?.text?.toString().orEmpty()
            val password   = binding.tilPassword.editText?.text?.toString().orEmpty()
            viewModel.login(identifier, password)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: LoginUiState) {
        binding.progress.isVisible = state is LoginUiState.Loading
        binding.btnLogin.isEnabled = state !is LoginUiState.Loading
        binding.tilIdentifier.isEnabled = state !is LoginUiState.Loading
        binding.tilPassword.isEnabled   = state !is LoginUiState.Loading

        when (state) {
            is LoginUiState.Error -> {
                binding.tvError.text = state.message
                binding.tvError.isVisible = true
            }
            is LoginUiState.Success -> {
                binding.tvError.isVisible = false
                viewModel.reset()
                findNavController().navigate(R.id.action_login_to_home)
            }
            else -> {
                binding.tvError.isVisible = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

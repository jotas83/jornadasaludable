package com.jornadasaludable.app.ui.fichaje

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.jornadasaludable.app.R
import com.jornadasaludable.app.data.api.dto.FichajeDto
import com.jornadasaludable.app.databinding.FragmentFicharTabBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@AndroidEntryPoint
class FicharTabFragment : Fragment() {

    private var _binding: FragmentFicharTabBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FicharTabViewModel by viewModels()

    private val timeFormatter   = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val dateFormatter   = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale("es", "ES"))
    private val historiaFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Si el usuario concede o deniega, refrescamos GPS status.
        viewModel.onPermissionResult()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFicharTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvDate.text = OffsetDateTime.now().format(dateFormatter)
            .replaceFirstChar { it.uppercase() }

        binding.btnEntrada.setOnClickListener { viewModel.ficharEntrada() }
        binding.btnSalida.setOnClickListener  { viewModel.ficharSalida() }
        binding.btnPausa.setOnClickListener {
            Snackbar.make(binding.root, R.string.fichar_pausa_pendiente, Snackbar.LENGTH_SHORT).show()
        }
        binding.btnRetry.setOnClickListener  { viewModel.refresh() }
        binding.btnRequestPermission.setOnClickListener { requestLocationPermission() }

        // Reloj en vivo
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    binding.tvClock.text = LocalTime.now().format(timeFormatter)
                    delay(1_000L)
                }
            }
        }

        // Estado del ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!hasLocationPermission()) requestLocationPermission()
        else viewModel.onPermissionResult()
    }

    private fun hasLocationPermission(): Boolean {
        val ctx = requireContext()
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ))
    }

    private fun render(state: FicharTabUiState) {
        binding.progress.isVisible       = state is FicharTabUiState.Loading
        binding.errorContainer.isVisible = state is FicharTabUiState.Error
        binding.contentContainer.isVisible = state is FicharTabUiState.Ready

        when (state) {
            is FicharTabUiState.Error -> binding.tvError.text = state.message
            is FicharTabUiState.Ready -> renderReady(state)
            else -> Unit
        }
    }

    private fun renderReady(s: FicharTabUiState.Ready) {
        // GPS status
        val gps = s.gps
        binding.tvGpsStatus.text = when {
            !gps.hasPermission       -> getString(R.string.fichar_gps_sin_permiso)
            !gps.gpsEnabled && !gps.networkEnabled -> getString(R.string.fichar_gps_apagado)
            gps.lastFix != null      -> getString(R.string.fichar_gps_ok, gps.lastFix)
            else                     -> getString(R.string.fichar_gps_sin_fix)
        }
        binding.btnRequestPermission.isVisible = !gps.hasPermission

        // Botones según estado
        val canEntrada = s.jornadaEstado == JornadaEstado.IDLE && !s.submitting
        val canSalida  = s.jornadaEstado == JornadaEstado.TRABAJANDO && !s.submitting
        val canPausa   = s.jornadaEstado == JornadaEstado.TRABAJANDO && !s.submitting
        binding.btnEntrada.isEnabled = canEntrada
        binding.btnSalida.isEnabled  = canSalida
        binding.btnPausa.isEnabled   = canPausa

        binding.tvEstadoLabel.text = when (s.jornadaEstado) {
            JornadaEstado.IDLE        -> getString(R.string.fichar_estado_idle)
            JornadaEstado.TRABAJANDO  -> getString(R.string.fichar_estado_trabajando)
        }

        binding.submittingProgress.isVisible = s.submitting

        // Historial de hoy
        renderHistorial(s.historial)

        // Mensaje transitorio
        s.transientMessage?.let { msg ->
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    private fun renderHistorial(fichajes: List<FichajeDto>) {
        binding.historialList.removeAllViews()
        if (fichajes.isEmpty()) {
            binding.historialEmpty.isVisible = true
            return
        }
        binding.historialEmpty.isVisible = false
        val inflater = LayoutInflater.from(binding.root.context)
        // Mostrar de más reciente a más antiguo
        fichajes.sortedByDescending { it.timestampEvento }.forEach { f ->
            val item = inflater.inflate(R.layout.item_fichaje_historial, binding.historialList, false)
            val tvHora = item.findViewById<android.widget.TextView>(R.id.tvHora)
            val tvTipo = item.findViewById<android.widget.TextView>(R.id.tvTipo)
            val hora = runCatching {
                OffsetDateTime.parse(f.timestampEvento).format(historiaFormatter)
            }.getOrDefault(f.timestampEvento.takeLast(8).take(5))
            tvHora.text = hora
            tvTipo.text = f.tipo
            binding.historialList.addView(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

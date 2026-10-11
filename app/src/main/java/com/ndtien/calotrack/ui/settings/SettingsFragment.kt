package com.ndtien.calotrack.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.ndtien.calotrack.R
import com.ndtien.calotrack.databinding.FragmentSettingsBinding
import com.ndtien.calotrack.util.PrefsManager

/**
 * Màn hình "Cài đặt": mục tiêu calo/ngày, giao diện tối, đơn vị năng lượng,
 * phiên bản app.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: PrefsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsManager(requireContext())

        // Điền sẵn mục tiêu hiện tại
        binding.etTargetCalories.setText(prefs.targetCalories.toString())

        binding.btnSaveTarget.setOnClickListener {
            val value = binding.etTargetCalories.text?.toString().orEmpty().trim().toIntOrNull()
            if (value != null && value in 500..10000) {
                prefs.targetCalories = value
                Toast.makeText(requireContext(), R.string.target_saved, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), R.string.invalid_target, Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchDarkMode.isChecked = prefs.darkMode
        binding.switchDarkMode.setOnCheckedChangeListener { _, checked ->
            prefs.darkMode = checked
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            requireActivity().recreate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

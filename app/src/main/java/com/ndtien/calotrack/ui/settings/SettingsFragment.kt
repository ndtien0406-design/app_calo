package com.ndtien.calotrack.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.ndtien.calotrack.R
import com.ndtien.calotrack.databinding.FragmentSettingsBinding
import com.ndtien.calotrack.ui.home.HomeViewModel
import com.ndtien.calotrack.util.PrefsManager

/**
 * Màn hình "Cài đặt": mục tiêu calo/ngày, giao diện tối, đơn vị năng lượng,
 * phiên bản app.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: PrefsManager

    /** Dùng chung với Trang chủ / Lịch sử để mục tiêu mới hiện ngay sau khi lưu. */
    private val homeViewModel: HomeViewModel by activityViewModels()

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

        binding.btnSaveTarget.setOnClickListener { saveTarget() }
        binding.etTargetCalories.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveTarget()
                true
            } else false
        }

        binding.switchDarkMode.isChecked = prefs.darkMode
        binding.switchDarkMode.setOnCheckedChangeListener { _, checked ->
            prefs.darkMode = checked
            // setDefaultNightMode tự dựng lại Activity khi chế độ đổi; gọi thêm recreate()
            // sẽ dựng lại 2 lần (màn hình nháy) nên không gọi nữa.
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Thông tin chỉ để xem: đơn vị lưu trong SharedPreferences, phiên bản lấy từ gói cài đặt
        binding.tvUnitValue.text = prefs.unit
        binding.tvVersionValue.text = appVersionName()
    }

    private fun saveTarget() {
        val value = binding.etTargetCalories.text?.toString().orEmpty().trim().toIntOrNull()
        if (value != null && value in 500..10000) {
            prefs.targetCalories = value
            // Báo ngay cho Trang chủ và Lịch sử (các tab dùng hide/show nên không có onResume)
            homeViewModel.refreshTarget()
            binding.etTargetCalories.clearFocus()
            requireContext().getSystemService<InputMethodManager>()
                ?.hideSoftInputFromWindow(binding.etTargetCalories.windowToken, 0)
            Toast.makeText(requireContext(), R.string.target_saved, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), R.string.invalid_target, Toast.LENGTH_SHORT).show()
        }
    }

    private fun appVersionName(): String = try {
        @Suppress("DEPRECATION")
        requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName ?: "—"
    } catch (_: Exception) {
        "—"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

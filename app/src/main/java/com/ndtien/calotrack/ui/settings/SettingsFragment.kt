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

        // Điền sẵn mục tiêu hiện tại + hiệu ứng viền sáng Uiverse khi focus
        binding.etTargetCalories.setText(prefs.targetCalories.toString())
        binding.etTargetCalories.setOnFocusChangeListener { _, hasFocus ->
            binding.targetInputContainer.isActivated = hasFocus
        }
        binding.targetInputContainer.setOnClickListener {
            binding.etTargetCalories.requestFocus()
            requireContext().getSystemService<InputMethodManager>()
                ?.showSoftInput(binding.etTargetCalories, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.btnSaveTarget.setOnClickListener { saveTarget() }
        binding.etTargetCalories.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveTarget()
                true
            } else false
        }

        binding.switchDarkMode.isChecked = prefs.darkMode
        binding.ivThemeIcon.setImageResource(
            if (prefs.darkMode) R.drawable.ic_moon else R.drawable.ic_sun
        )
        binding.switchDarkMode.setOnCheckedChangeListener { switchView, checked ->
            if (prefs.darkMode == checked) return@setOnCheckedChangeListener
            prefs.darkMode = checked
            binding.ivThemeIcon.animate().cancel()
            binding.ivThemeIcon.animate()
                .rotationBy(180f)
                .scaleX(0.75f)
                .scaleY(0.75f)
                .setDuration(110L)
                .withEndAction {
                    _binding?.ivThemeIcon?.setImageResource(
                        if (checked) R.drawable.ic_moon else R.drawable.ic_sun
                    )
                    _binding?.ivThemeIcon?.animate()
                        ?.scaleX(1f)
                        ?.scaleY(1f)
                        ?.setDuration(110L)
                        ?.start()
                }
                .start()

            // Chờ nút gạt trượt xong (140ms) rồi chuyển giao diện bằng sóng tròn Circular Reveal không khựng
            switchView.postDelayed({
                if (!isAdded) return@postDelayed
                (activity as? com.ndtien.calotrack.MainActivity)
                    ?.switchThemeSmoothly(checked, switchView)
            }, 140L)
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

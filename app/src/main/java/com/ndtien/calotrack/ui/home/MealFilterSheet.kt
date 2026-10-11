package com.ndtien.calotrack.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ndtien.calotrack.R
import com.ndtien.calotrack.databinding.DialogFilterMealsBinding
import java.io.Serializable

enum class SortMode { BY_TIME, CAL_HIGH_LOW, CAL_LOW_HIGH }

/** Trạng thái lọc danh sách bữa ăn. mealType null = tất cả. */
data class MealFilter(
    val mealType: String? = null,
    val kcalMin: Double? = null,
    val kcalMax: Double? = null,
    val sort: SortMode = SortMode.BY_TIME
) : Serializable

/**
 * Bottom sheet "Lọc bữa ăn": nhóm Bữa (chọn 1), khoảng calo Từ/Đến,
 * sắp xếp (chọn 1). Không có dòng mô tả phụ dưới các nhóm.
 */
class MealFilterSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onFilterApplied(filter: MealFilter)
    }

    private var _binding: DialogFilterMealsBinding? = null
    private val binding get() = _binding!!

    private var current: MealFilter = MealFilter()
    private var defaultStroke: Int = 0

    companion object {
        private const val ARG_FILTER = "arg_filter"

        fun newInstance(current: MealFilter): MealFilterSheet {
            return MealFilterSheet().apply {
                arguments = Bundle().apply { putSerializable(ARG_FILTER, current) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        current = arguments?.getSerializable(ARG_FILTER) as? MealFilter ?: MealFilter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFilterMealsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding
        defaultStroke = b.tilKcalMin.boxStrokeColor

        // Điền sẵn theo filter hiện tại
        b.chipMealFilter.check(
            when (current.mealType) {
                "BREAKFAST" -> R.id.chip_mf_breakfast
                "LUNCH" -> R.id.chip_mf_lunch
                "DINNER" -> R.id.chip_mf_dinner
                "SNACK" -> R.id.chip_mf_snack
                else -> R.id.chip_mf_all
            }
        )
        b.chipSort.check(
            when (current.sort) {
                SortMode.CAL_HIGH_LOW -> R.id.chip_sort_desc
                SortMode.CAL_LOW_HIGH -> R.id.chip_sort_asc
                else -> R.id.chip_sort_time
            }
        )
        current.kcalMin?.let { b.etKcalMin.setText(fmtNum(it)) }
        current.kcalMax?.let { b.etKcalMax.setText(fmtNum(it)) }

        b.btnClearMealFilter.setOnClickListener {
            (parentFragment as? Listener)?.onFilterApplied(MealFilter())
            dismiss()
        }
        b.btnApplyMealFilter.setOnClickListener { applyAndClose() }
    }

    private fun applyAndClose() {
        val b = _binding ?: return
        val mealType = when (b.chipMealFilter.checkedChipId) {
            R.id.chip_mf_breakfast -> "BREAKFAST"
            R.id.chip_mf_lunch -> "LUNCH"
            R.id.chip_mf_dinner -> "DINNER"
            R.id.chip_mf_snack -> "SNACK"
            else -> null
        }
        val sort = when (b.chipSort.checkedChipId) {
            R.id.chip_sort_desc -> SortMode.CAL_HIGH_LOW
            R.id.chip_sort_asc -> SortMode.CAL_LOW_HIGH
            else -> SortMode.BY_TIME
        }
        val min = b.etKcalMin.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()
        val max = b.etKcalMax.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()

        // Validate: số không âm, Từ <= Đến. Sai -> chỉ viền đỏ, không cho áp dụng.
        val invalid = (min != null && min < 0) || (max != null && max < 0) ||
                (min != null && max != null && min > max)
        setRangeInvalid(invalid)
        if (invalid) return

        (parentFragment as? Listener)?.onFilterApplied(
            MealFilter(mealType = mealType, kcalMin = min, kcalMax = max, sort = sort)
        )
        dismiss()
    }

    private fun setRangeInvalid(invalid: Boolean) {
        val b = _binding ?: return
        val color = if (invalid) {
            ContextCompat.getColor(requireContext(), R.color.red_over)
        } else {
            defaultStroke
        }
        b.tilKcalMin.boxStrokeColor = color
        b.tilKcalMax.boxStrokeColor = color
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fmtNum(d: Double): String =
        if (d % 1.0 == 0.0) "%.0f".format(d) else d.toString()
}

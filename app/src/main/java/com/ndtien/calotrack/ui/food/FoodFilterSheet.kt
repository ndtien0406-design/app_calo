package com.ndtien.calotrack.ui.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.ndtien.calotrack.R
import com.ndtien.calotrack.databinding.DialogFilterFoodsBinding
import java.io.Serializable

/** Trạng thái lọc danh sách thực phẩm (kết hợp AND với từ khóa tìm kiếm). */
data class FoodFilter(
    val categories: Set<String> = emptySet(),
    val units: Set<String> = emptySet(),
    val kcalMin: Double? = null,
    val kcalMax: Double? = null
) : Serializable

/**
 * Bottom sheet "Lọc thực phẩm": nhóm món + đơn vị khẩu phần (chip chọn nhiều,
 * liệt kê động từ dữ liệu) + khoảng calo Từ/Đến. Không có dòng mô tả phụ.
 */
class FoodFilterSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onFilterApplied(filter: FoodFilter)
    }

    private var _binding: DialogFilterFoodsBinding? = null
    private val binding get() = _binding!!

    private var current: FoodFilter = FoodFilter()
    private var allCategories: List<String> = emptyList()
    private var allUnits: List<String> = emptyList()
    private var defaultStroke: Int = 0

    companion object {
        private const val ARG_FILTER = "arg_filter"
        private const val ARG_CATEGORIES = "arg_categories"
        private const val ARG_UNITS = "arg_units"

        fun newInstance(
            current: FoodFilter,
            categories: List<String>,
            units: List<String>
        ): FoodFilterSheet {
            return FoodFilterSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_FILTER, current)
                    putStringArrayList(ARG_CATEGORIES, ArrayList(categories))
                    putStringArrayList(ARG_UNITS, ArrayList(units))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        current = arguments?.getSerializable(ARG_FILTER) as? FoodFilter ?: FoodFilter()
        allCategories = arguments?.getStringArrayList(ARG_CATEGORIES).orEmpty()
        allUnits = arguments?.getStringArrayList(ARG_UNITS).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFilterFoodsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding
        defaultStroke = b.tilFoodKcalMin.boxStrokeColor

        allCategories.forEach { name ->
            b.chipFoodCategories.addView(makeChip(name, name in current.categories))
        }
        allUnits.forEach { name ->
            b.chipFoodUnits.addView(makeChip(name, name in current.units))
        }
        current.kcalMin?.let { b.etFoodKcalMin.setText(fmtNum(it)) }
        current.kcalMax?.let { b.etFoodKcalMax.setText(fmtNum(it)) }

        b.btnClearFoodFilter.setOnClickListener {
            (parentFragment as? Listener)?.onFilterApplied(FoodFilter())
            dismiss()
        }
        b.btnApplyFoodFilter.setOnClickListener { applyAndClose() }
    }

    private fun makeChip(label: String, checked: Boolean): Chip {
        return Chip(requireContext()).apply {
            text = label
            isCheckable = true
            isChecked = checked
        }
    }

    private fun checkedLabels(group: com.google.android.material.chip.ChipGroup): Set<String> {
        val out = mutableSetOf<String>()
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip ?: continue
            if (chip.isChecked) out.add(chip.text.toString())
        }
        return out
    }

    private fun applyAndClose() {
        val b = _binding ?: return
        val min = b.etFoodKcalMin.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()
        val max = b.etFoodKcalMax.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()

        val invalid = (min != null && min < 0) || (max != null && max < 0) ||
                (min != null && max != null && min > max)
        val color = if (invalid) {
            ContextCompat.getColor(requireContext(), R.color.red_over)
        } else {
            defaultStroke
        }
        b.tilFoodKcalMin.boxStrokeColor = color
        b.tilFoodKcalMax.boxStrokeColor = color
        if (invalid) return

        (parentFragment as? Listener)?.onFilterApplied(
            FoodFilter(
                categories = checkedLabels(b.chipFoodCategories),
                units = checkedLabels(b.chipFoodUnits),
                kcalMin = min,
                kcalMax = max
            )
        )
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fmtNum(d: Double): String =
        if (d % 1.0 == 0.0) "%.0f".format(d) else d.toString()
}

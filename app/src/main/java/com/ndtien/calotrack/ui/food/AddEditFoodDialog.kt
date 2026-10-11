package com.ndtien.calotrack.ui.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.ndtien.calotrack.R
import com.ndtien.calotrack.data.AppDatabase
import com.ndtien.calotrack.data.Food
import com.ndtien.calotrack.data.FoodCategories
import com.ndtien.calotrack.databinding.DialogAddEditFoodBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Callback mà Fragment chứa dialog phải implement để refresh danh sách
 * sau khi thêm/sửa thực phẩm.
 */
interface FoodDialogCallback {
    fun onFoodSaved()
}

/**
 * Dialog thêm mới / chỉnh sửa thực phẩm.
 * Dùng [newInstance] với foodId = -1 (mặc định) để thêm mới,
 * hoặc truyền id thực phẩm để vào chế độ sửa.
 */
class AddEditFoodDialog : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_FOOD_ID = "food_id"

        fun newInstance(foodId: Long = -1L): AddEditFoodDialog =
            AddEditFoodDialog().apply {
                arguments = Bundle().apply { putLong(ARG_FOOD_ID, foodId) }
            }
    }

    private var _binding: DialogAddEditFoodBinding? = null
    private val binding get() = _binding!!

    private val foodDao by lazy { AppDatabase.getInstance(requireContext()).foodDao() }
    private var editingFood: Food? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val foodId = arguments?.getLong(ARG_FOOD_ID, -1L) ?: -1L
        val isEdit = foodId != -1L
        binding.tvFoodDialogTitle.setText(if (isEdit) R.string.edit_food else R.string.add_food)
        binding.btnSaveFood.setText(if (isEdit) R.string.save else R.string.add)
        buildCategoryChips(FoodCategories.DEFAULT)

        if (isEdit) {
            // Chế độ sửa: tải thực phẩm theo id rồi điền vào form
            lifecycleScope.launch {
                val food = withContext(Dispatchers.IO) { foodDao.getById(foodId) }
                food?.let {
                    editingFood = it
                    binding.etFoodName.setText(it.name)
                    binding.etFoodCalories.setText(fmtCal(it.caloriesPerServing))
                    binding.etFoodUnit.setText(it.servingUnit)
                    buildCategoryChips(it.category)
                }
            }
        }

        binding.btnCancelFood.setOnClickListener { dismiss() }
        binding.btnSaveFood.setOnClickListener { saveFood() }
    }

    /** Chip chọn một nhóm món; nhóm cũ không thuộc danh sách chuẩn vẫn được giữ lại. */
    private fun buildCategoryChips(selected: String) {
        val group = binding.chipFoodCategory
        group.removeAllViews()
        val names = if (selected in FoodCategories.ALL) FoodCategories.ALL
        else FoodCategories.ALL + selected
        names.forEach { name ->
            group.addView(Chip(requireContext()).apply {
                id = View.generateViewId()
                text = name
                isCheckable = true
                isChecked = name == selected
            })
        }
    }

    private fun selectedCategory(): String {
        val group = binding.chipFoodCategory
        val chip = group.findViewById<Chip>(group.checkedChipId)
        return chip?.text?.toString() ?: FoodCategories.DEFAULT
    }

    private fun saveFood() {
        val name = binding.etFoodName.text?.toString().orEmpty().trim()
        if (name.isEmpty()) {
            binding.etFoodName.error = getString(R.string.err_food_name)
            return
        }
        val calories = binding.etFoodCalories.text?.toString().orEmpty().trim().toDoubleOrNull()
        if (calories == null || calories <= 0) {
            binding.etFoodCalories.error = getString(R.string.err_food_calories)
            return
        }
        val unit = binding.etFoodUnit.text?.toString().orEmpty().trim().ifEmpty { getString(R.string.default_unit) }
        val category = selectedCategory()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val existing = editingFood
                if (existing == null) {
                    foodDao.insert(
                        Food(
                            name = name,
                            caloriesPerServing = calories,
                            servingUnit = unit,
                            category = category
                        )
                    )
                } else {
                    foodDao.update(
                        existing.copy(
                            name = name,
                            caloriesPerServing = calories,
                            servingUnit = unit,
                            category = category
                        )
                    )
                }
            }
            (parentFragment as? FoodDialogCallback)?.onFoodSaved()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/** Định dạng calo: bỏ phần thập phân nếu là số nguyên. */
private fun fmtCal(d: Double): String =
    if (d % 1.0 == 0.0) "%.0f".format(d) else "%.1f".format(d)

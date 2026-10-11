package com.ndtien.calotrack.ui.meal

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.lifecycle.lifecycleScope
import com.ndtien.calotrack.R
import com.ndtien.calotrack.data.AppDatabase
import com.ndtien.calotrack.data.Food
import com.ndtien.calotrack.data.FoodDao
import com.ndtien.calotrack.data.MealDao
import com.ndtien.calotrack.data.MealEntry
import com.ndtien.calotrack.databinding.DialogAddEditMealBinding
import com.ndtien.calotrack.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

interface MealDialogCallback {
    fun onMealSaved()
}

class AddEditMealDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddEditMealBinding? = null
    private val binding get() = _binding!!

    private lateinit var mealDao: MealDao
    private lateinit var foodDao: FoodDao

    private var mealId: Long = -1L
    private var selectedDate: String = DateUtils.today()
    private var selectedTime: String = DateUtils.nowTime()
    private var existingMeal: MealEntry? = null

    private var foods: List<Food> = emptyList()
    private var selectedFood: Food? = null

    companion object {
        private const val ARG_MEAL_ID = "arg_meal_id"
        private const val ARG_DATE = "arg_date"

        fun newInstance(mealId: Long = -1, date: String = DateUtils.today()): AddEditMealDialog {
            return AddEditMealDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_MEAL_ID, mealId)
                    putString(ARG_DATE, date)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mealId = arguments?.getLong(ARG_MEAL_ID, -1L) ?: -1L
        selectedDate = arguments?.getString(ARG_DATE) ?: DateUtils.today()
        val appContext = requireContext().applicationContext
        val db = AppDatabase.getInstance(appContext)
        mealDao = db.mealDao()
        foodDao = db.foodDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isEdit = mealId != -1L
        binding.tvDialogTitle.text = getString(if (isEdit) R.string.edit_meal else R.string.add_meal)
        binding.btnSaveMeal.setText(if (isEdit) R.string.save else R.string.add_meal_confirm)
        refreshDateTime()

        binding.cbQuickEntry.setOnCheckedChangeListener { _, checked ->
            binding.layoutQuickEntry.visibility = if (checked) View.VISIBLE else View.GONE
            binding.tilFood.visibility = if (checked) View.GONE else View.VISIBLE
            binding.tilFood.error = null
            updateComputedCalories()
        }

        // Stepper khẩu phần: nút −/+ bước 0,5, tối thiểu 0,5 (theo bản xem thử).
        binding.btnPortionMinus.setOnClickListener { stepPortion(-0.5) }
        binding.btnPortionPlus.setOnClickListener { stepPortion(0.5) }

        binding.etPortion.doOnTextChanged { _, _, _, _ -> updateComputedCalories() }
        binding.etQuickCalories.doOnTextChanged { _, _, _, _ -> updateComputedCalories() }
        binding.actvFood.doOnTextChanged { text, _, _, _ ->
            // User gõ tay không khớp món nào -> coi như chưa chọn
            val current = selectedFood
            if (current != null && text?.toString() != foodDisplay(current)) {
                selectedFood = null
            }
            updateComputedCalories()
        }

        binding.etMealDate.setOnClickListener { pickDate() }
        binding.tilMealDate.setEndIconOnClickListener { pickDate() }
        binding.etMealTime.setOnClickListener { pickTime() }
        binding.tilMealTime.setEndIconOnClickListener { pickTime() }
        binding.btnCancelMeal.setOnClickListener { dismiss() }
        binding.btnSaveMeal.setOnClickListener { saveMeal() }

        loadData(isEdit)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadData(isEdit: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            val foodList = foodDao.getAllFoods().first()
            val meal = if (isEdit) mealDao.getById(mealId) else null
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                foods = foodList
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    foodList.map { foodDisplay(it) }
                )
                binding.actvFood.setAdapter(adapter)
                binding.actvFood.threshold = 1
                binding.actvFood.setOnItemClickListener { _, _, position, _ ->
                    val name = adapter.getItem(position)
                    selectedFood = foodList.find { foodDisplay(it) == name }
                    updateComputedCalories()
                }
                if (isEdit) {
                    if (meal == null) {
                        dismiss()
                        return@withContext
                    }
                    existingMeal = meal
                    populateForEdit(meal)
                } else {
                    binding.chipMealType.check(chipIdFor(defaultMealTypeByHour()))
                }
                updateComputedCalories()
            }
        }
    }

    private fun populateForEdit(meal: MealEntry) {
        binding.chipMealType.check(chipIdFor(meal.mealType))
        selectedDate = meal.date
        selectedTime = meal.time
        refreshDateTime()
        val food = foods.find { it.id == meal.foodId }
        if (food != null) {
            selectedFood = food
            binding.actvFood.setText(foodDisplay(food), false)
        } else {
            // Món nhập nhanh (hoặc món đã bị xóa khỏi danh mục) -> dùng quick entry
            binding.cbQuickEntry.isChecked = true
            binding.etQuickName.setText(meal.foodName)
            binding.etQuickCalories.setText(fmtCal(meal.caloriesPerServing))
        }
        binding.etPortion.setText(fmtCal(meal.portion))
    }

    private fun foodDisplay(food: Food): String =
        "${food.name} (${fmtCal(food.caloriesPerServing)} ${getString(R.string.unit_kcal)}/${food.servingUnit})"

    private fun chipIdFor(mealType: String): Int = when (mealType) {
        "LUNCH" -> R.id.chip_lunch
        "DINNER" -> R.id.chip_dinner
        "SNACK" -> R.id.chip_snack
        else -> R.id.chip_breakfast
    }

    private fun currentMealType(): String = when (binding.chipMealType.checkedChipId) {
        R.id.chip_lunch -> "LUNCH"
        R.id.chip_dinner -> "DINNER"
        R.id.chip_snack -> "SNACK"
        else -> "BREAKFAST"
    }

    /** Mặc định loại bữa theo giờ hiện tại: 5-10h sáng, 10-14h trưa, 17-21h tối, còn lại bữa phụ. */
    private fun defaultMealTypeByHour(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 5..10 -> "BREAKFAST"
            hour in 10..14 -> "LUNCH"
            hour in 17..21 -> "DINNER"
            else -> "SNACK"
        }
    }

    private fun updateComputedCalories() {
        val b = _binding ?: return
        setPortionInvalid(false)
        val portion = parsePortion()
        val calPerServing = if (b.cbQuickEntry.isChecked) {
            b.etQuickCalories.text?.toString()?.toDoubleOrNull() ?: 0.0
        } else {
            selectedFood?.caloriesPerServing ?: 0.0
        }
        val total = calPerServing * portion

        // Đơn vị khẩu phần + dòng calo/khẩu phần dưới ô chọn món (theo bản xem thử)
        val food = if (b.cbQuickEntry.isChecked) null else selectedFood
        b.tvPortionUnit.text = food?.servingUnit ?: getString(R.string.default_unit)
        b.tilFood.helperText = food?.let {
            "${fmtCal(it.caloriesPerServing)} ${getString(R.string.unit_kcal)} / ${it.servingUnit}"
        }

        // "Tổng calo: **450 kcal**"
        val label = "${getString(R.string.total_calories)}: "
        val value = "${fmtCal(total)} ${getString(R.string.unit_kcal)}"
        b.tvComputedCalories.text = SpannableStringBuilder(label + value).apply {
            setSpan(StyleSpan(Typeface.BOLD), label.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun refreshDateTime() {
        val b = _binding ?: return
        b.etMealDate.setText(DateUtils.toDisplay(selectedDate))
        b.etMealTime.setText(selectedTime)
    }

    private fun pickTime() {
        val parts = selectedTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(requireContext(), { _, hour, minute ->
            selectedTime = "%02d:%02d".format(hour, minute)
            refreshDateTime()
        }, h, m, true).show()
    }

    /** Đọc khẩu phần, chấp nhận cả dấu phẩy (VD: 1,5) như bản xem thử. */
    private fun parsePortion(): Double =
        _binding?.etPortion?.text?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0

    /** Nút −/+ bước 0,5, không cho xuống dưới 0,5. */
    private fun stepPortion(delta: Double) {
        val b = _binding ?: return
        val next = ((parsePortion() + delta) * 2).toInt() / 2.0
        b.etPortion.setText(fmtCal(next.coerceAtLeast(0.5)))
    }

    private var portionStrokeDefault: Int? = null

    /**
     * Báo lỗi khẩu phần: chỉ viền đỏ, không dòng mô tả (theo bản xem thử).
     * Khôi phục màu viền gốc khi hợp lệ trở lại.
     */
    private fun setPortionInvalid(invalid: Boolean) {
        val b = _binding ?: return
        if (portionStrokeDefault == null) portionStrokeDefault = b.tilPortion.boxStrokeColor
        b.tilPortion.boxStrokeColor = if (invalid) {
            ContextCompat.getColor(requireContext(), R.color.red_over)
        } else {
            portionStrokeDefault!!
        }
    }

    private fun pickDate() {
        val cal = Calendar.getInstance()
        val parts = selectedDate.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                cal.set(y, m - 1, d)
            }
        }
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                selectedDate = "%04d-%02d-%02d".format(y, m + 1, d)
                refreshDateTime()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveMeal() {
        binding.tilFood.error = null
        binding.etQuickName.error = null
        binding.etQuickCalories.error = null
        setPortionInvalid(false)

        val quick = binding.cbQuickEntry.isChecked
        val name = if (quick) {
            binding.etQuickName.text?.toString()?.trim().orEmpty()
        } else {
            selectedFood?.name.orEmpty()
        }
        if (name.isEmpty()) {
            if (quick) {
                binding.etQuickName.error = getString(R.string.hint_food_name)
            } else {
                binding.tilFood.error = getString(R.string.choose_food)
            }
            return
        }

        val calPerServing = if (quick) {
            binding.etQuickCalories.text?.toString()?.toDoubleOrNull() ?: 0.0
        } else {
            selectedFood?.caloriesPerServing ?: 0.0
        }
        if (calPerServing <= 0) {
            if (quick) {
                binding.etQuickCalories.error = getString(R.string.hint_cal_per_serving)
            } else {
                binding.tilFood.error = getString(R.string.choose_food)
            }
            return
        }

        val portion = parsePortion()
        if (portion <= 0) {
            setPortionInvalid(true)
            return
        }

        val old = existingMeal
        val entry = MealEntry(
            id = old?.id ?: 0L,
            foodId = if (quick) null else selectedFood?.id,
            foodName = name,
            caloriesPerServing = calPerServing,
            portion = portion,
            totalCalories = calPerServing * portion,
            mealType = currentMealType(),
            date = selectedDate,
            time = selectedTime,
            createdAt = old?.createdAt ?: System.currentTimeMillis()
        )
        lifecycleScope.launch(Dispatchers.IO) {
            if (old != null) mealDao.update(entry) else mealDao.insert(entry)
            withContext(Dispatchers.Main) {
                if (isAdded) {
                    (parentFragment as? MealDialogCallback)?.onMealSaved()
                    dismiss()
                }
            }
        }
    }
}

private fun fmtCal(d: Double): String =
    if (d % 1 == 0.0) "%.0f".format(d) else "%.1f".format(d)

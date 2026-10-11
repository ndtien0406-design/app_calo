package com.ndtien.calotrack.ui.home

import android.animation.ValueAnimator
import androidx.core.animation.doOnEnd
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.ndtien.calotrack.R
import com.ndtien.calotrack.data.MealEntry
import com.ndtien.calotrack.databinding.FragmentHomeBinding
import com.ndtien.calotrack.ui.meal.AddEditMealDialog
import com.ndtien.calotrack.ui.meal.MealDialogCallback
import com.ndtien.calotrack.util.DateUtils
import com.ndtien.calotrack.util.PrefsManager

class HomeFragment : Fragment(), MealDialogCallback, MealFilterSheet.Listener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var adapter: MealAdapter
    private var filterBadge: BadgeDrawable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MealAdapter(
            onEdit = { meal -> openMealDialog(meal.id) },
            onDelete = { meal -> deleteMealWithUndo(meal) }
        )
        binding.rvMeals.adapter = adapter
        attachSwipeToDelete()

        // Điều hướng ngày: ‹ › và chọn ngày bằng lịch (không cho chọn tương lai)
        binding.btnPrevDay.setOnClickListener { viewModel.shiftDay(-1) }
        binding.btnNextDay.setOnClickListener { viewModel.shiftDay(1) }
        binding.tvDayLabel.setOnClickListener { pickDay() }
        viewModel.selectedDate.observe(viewLifecycleOwner) { updateDayUi() }

        // Quan sát filteredMeals (MediatorLiveData) để nó luôn được cập nhật khi bữa ăn / bộ lọc đổi
        viewModel.filteredMeals.observe(viewLifecycleOwner) { updateMealList() }
        viewModel.mealFilter.observe(viewLifecycleOwner) { refreshFilterUi() }

        binding.btnFilterMeals.setOnClickListener {
            MealFilterSheet.newInstance(viewModel.mealFilter.value ?: MealFilter())
                .show(childFragmentManager, TAG_FILTER_MEALS)
        }
        binding.btnClearFilter.setOnClickListener {
            viewModel.clearMealFilter()
        }

        viewModel.totalCalories.observe(viewLifecycleOwner) { updateOverview() }
        viewModel.targetCalories.observe(viewLifecycleOwner) { updateOverview() }

        binding.fabAddMeal.setOnClickListener {
            val date = viewModel.selectedDate.value ?: DateUtils.today()
            // Dùng childFragmentManager để dialog gọi được (parentFragment as? MealDialogCallback)
            AddEditMealDialog.newInstance(date = date).show(childFragmentManager, TAG_ADD_MEAL)
        }
    }

    override fun onResume() {
        super.onResume()
        // Đã sang ngày mới khi app đang mở -> tự về hôm nay (trống), mục tiêu giữ nguyên
        if (viewModel.rollToTodayIfNewDay()) {
            _binding?.let { b ->
                Snackbar.make(b.root, R.string.new_day_started, Snackbar.LENGTH_SHORT).show()
            }
        }
        // User có thể đổi mục tiêu ở Settings -> đọc lại mỗi lần quay về
        viewModel.refreshTarget()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        overviewAnimator?.cancel()
        overviewAnimator = null
        filterBadge = null
        _binding = null
    }

    override fun onMealSaved() {
        _binding?.let { b ->
            Snackbar.make(b.root, R.string.meal_saved, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onFilterApplied(filter: MealFilter) {
        viewModel.mealFilter.value = filter
    }

    /** Vẽ lại danh sách bữa ăn theo filter + cập nhật đếm "N món" và empty state. */
    private fun updateMealList() {
        val b = _binding ?: return
        val list = viewModel.filteredMeals.value.orEmpty()
        adapter.submitList(list)
        b.tvMealsCount.text = getString(R.string.meals_count, list.size)
        val empty = list.isEmpty()
        b.tvEmptyMeals.isVisible = empty
        b.rvMeals.isVisible = !empty
        if (empty) {
            val hasMeals = viewModel.meals.value.orEmpty().isNotEmpty()
            b.tvEmptyMeals.text = getString(
                if (hasMeals) R.string.no_filter_result else R.string.no_meals
            )
        }
    }

    /** Badge số nhóm lọc đang bật + hàng chip hiển thị từng bộ lọc (bấm × để gỡ). */
    private fun refreshFilterUi() {
        val b = _binding ?: return
        val filter = viewModel.mealFilter.value ?: MealFilter()

        val badge = filterBadge ?: BadgeDrawable.create(requireContext()).also {
            filterBadge = it
            BadgeUtils.attachBadgeDrawable(it, b.btnFilterMeals)
        }
        val count = viewModel.activeFilterCount()
        badge.number = count
        badge.isVisible = count > 0

        b.chipActiveFilters.removeAllViews()
        var hasChip = false

        filter.mealType?.let { type ->
            b.chipActiveFilters.addView(makeFilterChip(mealTypeLabel(type)) {
                viewModel.mealFilter.value = filter.copy(mealType = null)
            })
            hasChip = true
        }
        if (filter.kcalMin != null || filter.kcalMax != null) {
            val label = when {
                filter.kcalMin != null && filter.kcalMax != null ->
                    "${fmtNum(filter.kcalMin)}–${fmtNum(filter.kcalMax)} kcal"
                filter.kcalMin != null -> "≥${fmtNum(filter.kcalMin)} kcal"
                else -> "≤${fmtNum(filter.kcalMax!!)} kcal"
            }
            b.chipActiveFilters.addView(makeFilterChip(label) {
                viewModel.mealFilter.value = filter.copy(kcalMin = null, kcalMax = null)
            })
            hasChip = true
        }
        if (filter.sort != SortMode.BY_TIME) {
            val label = getString(
                if (filter.sort == SortMode.CAL_HIGH_LOW) R.string.sort_cal_desc
                else R.string.sort_cal_asc
            )
            b.chipActiveFilters.addView(makeFilterChip(label) {
                viewModel.mealFilter.value = filter.copy(sort = SortMode.BY_TIME)
            })
            hasChip = true
        }
        b.chipActiveFilters.isVisible = hasChip
    }

    private fun makeFilterChip(label: String, onClose: () -> Unit): Chip {
        return Chip(requireContext()).apply {
            text = label
            isCloseIconVisible = true
            setOnCloseIconClickListener { onClose() }
        }
    }

    private fun mealTypeLabel(type: String): String = getString(
        when (type) {
            "LUNCH" -> R.string.meal_lunch
            "DINNER" -> R.string.meal_dinner
            "SNACK" -> R.string.meal_snack
            else -> R.string.meal_breakfast
        }
    )

    private fun fmtNum(d: Double): String =
        if (d % 1.0 == 0.0) "%.0f".format(d) else d.toString()

    private fun openMealDialog(mealId: Long) {
        AddEditMealDialog.newInstance(mealId = mealId).show(childFragmentManager, TAG_EDIT_MEAL)
    }

    /** Xóa ngay + Snackbar "Hoàn tác" (nhanh hơn hộp thoại xác nhận, vẫn không mất dữ liệu). */
    private fun deleteMealWithUndo(meal: MealEntry) {
        viewModel.deleteMeal(meal)
        _binding?.let { b ->
            Snackbar.make(b.root, R.string.meal_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) { viewModel.restoreMeal(meal) }
                .show()
        }
    }

    /** Vuốt ngang để xóa bữa ăn; nền đỏ + icon thùng rác hiện ra khi vuốt. */
    private fun attachSwipeToDelete() {
        val context = requireContext()
        val icon = ContextCompat.getDrawable(context, R.drawable.ic_delete)?.mutate()
        val paintColor = ContextCompat.getColor(context, R.color.proto_error_container)
        val iconColor = ContextCompat.getColor(context, R.color.proto_error)
        icon?.setTint(iconColor)
        val paint = android.graphics.Paint().apply { color = paintColor }
        val radius = resources.displayMetrics.density * 16

        val callback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val meal = adapter.currentList.getOrNull(vh.bindingAdapterPosition) ?: return
                deleteMealWithUndo(meal)
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val v = vh.itemView
                if (dX != 0f) {
                    val rect = if (dX > 0) {
                        android.graphics.RectF(v.left.toFloat(), v.top.toFloat(), v.left + dX, v.bottom.toFloat())
                    } else {
                        android.graphics.RectF(v.right + dX, v.top.toFloat(), v.right.toFloat(), v.bottom.toFloat())
                    }
                    c.drawRoundRect(rect, radius, radius, paint)
                    icon?.let {
                        val size = it.intrinsicHeight
                        val top = v.top + (v.height - size) / 2
                        val margin = (size * 0.9f).toInt()
                        if (dX > 0) it.setBounds(v.left + margin, top, v.left + margin + size, top + size)
                        else it.setBounds(v.right - margin - size, top, v.right - margin, top + size)
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvMeals)
    }

    private fun pickDay() {
        val cur = viewModel.selectedDate.value ?: DateUtils.today()
        val p = cur.split("-").mapNotNull { it.toIntOrNull() }
        if (p.size != 3) return
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val picked = "%04d-%02d-%02d".format(y, m + 1, d)
            if (picked <= DateUtils.today()) viewModel.setSelectedDateManual(picked)
        }, p[0], p[1] - 1, p[2]).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    /** Cập nhật nhãn ngày, trạng thái nút ›, và các tiêu đề "hôm nay" / "ngày dd/MM". */
    private fun updateDayUi() {
        val b = _binding ?: return
        val date = viewModel.selectedDate.value ?: DateUtils.today()
        val today = DateUtils.today()
        val prefix = when (date) {
            today -> getString(R.string.today)
            DateUtils.addDays(today, -1) -> getString(R.string.yesterday)
            else -> null
        }
        val full = DateUtils.toPill(date)
        b.tvDayLabel.text = if (prefix != null) "$prefix · $full" else full
        b.btnNextDay.isEnabled = date < today
        b.btnNextDay.alpha = if (date < today) 1f else 0.4f

        val short = DateUtils.toDisplay(date).substring(0, 5)
        val isToday = date == today
        b.tvEatenLabel.text = if (isToday) getString(R.string.eaten_today)
        else getString(R.string.eaten_on_day, short)
        b.tvMealsTitle.text = if (isToday) getString(R.string.meals_today)
        else getString(R.string.meals_on_day, short)
    }

    /**
     * Thẻ tổng quan kiểu bản xem thử: số lớn + mục tiêu + thanh tiến trình + nhãn còn lại.
     * Khi vượt mục tiêu, cả thẻ chuyển sang tông đỏ (error-container).
     */
    private fun updateOverview() {
        val b = _binding ?: return
        val context = b.root.context
        val eaten = viewModel.totalCalories.value ?: 0.0
        val target = (viewModel.targetCalories.value ?: PrefsManager.DEFAULT_TARGET).coerceAtLeast(1)
        val over = eaten > target

        b.tvTargetBig.text = fmtCal(target.toDouble())

        val remaining = target - eaten
        b.tvRemainLabel.text = if (remaining >= 0) {
            "${getString(R.string.label_remaining)} ${fmtCal(remaining)} ${getString(R.string.unit_kcal)}"
        } else {
            "${getString(R.string.label_over)} ${fmtCal(-remaining)} ${getString(R.string.unit_kcal)}"
        }

        // Màu thẻ + chữ + thanh fill theo trạng thái vượt/dưới mục tiêu
        val cardColor = ContextCompat.getColor(
            context,
            if (over) R.color.proto_error_container else R.color.proto_primary_container
        )
        val onCardColor = ContextCompat.getColor(
            context,
            if (over) R.color.proto_error else R.color.proto_on_primary_container
        )
        b.cardOverview.setCardBackgroundColor(cardColor)
        b.tvEatenBig.setTextColor(onCardColor)
        b.tvTargetBig.setTextColor(onCardColor)
        b.tvRemainLabel.setTextColor(onCardColor)

        b.viewProgressFill.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, if (over) R.color.chart_over_label else R.color.chart_under_label)
        )

        animateOverview(eaten, (eaten / target).toFloat().coerceIn(0f, 1f))
    }

    private var shownEaten = 0.0
    private var shownFraction = 0f
    private var overviewAnimator: ValueAnimator? = null

    /** Số calo đếm tăng/giảm mượt + thanh tiến trình chạy tới giá trị mới (~600ms). */
    private fun animateOverview(eaten: Double, fraction: Float) {
        val b = _binding ?: return
        overviewAnimator?.cancel()
        val fromEaten = shownEaten
        val fromFraction = shownFraction
        overviewAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { a ->
                val t = a.animatedFraction
                val bind = _binding ?: return@addUpdateListener
                val e = fromEaten + (eaten - fromEaten) * t
                val f = fromFraction + (fraction - fromFraction) * t
                bind.tvEatenBig.text = fmtCal(Math.round(e).toDouble())
                (bind.viewProgressFill.layoutParams as LinearLayout.LayoutParams).weight = f
                (bind.spaceProgressRest.layoutParams as LinearLayout.LayoutParams).weight = 1f - f
                bind.viewProgressFill.requestLayout()
            }
            doOnEnd {
                shownEaten = eaten
                shownFraction = fraction
                _binding?.tvEatenBig?.text = fmtCal(eaten)
            }
            start()
        }
        // Nếu bị huỷ giữa chừng thì lần sau bắt đầu từ giá trị đích gần nhất
        shownEaten = eaten
        shownFraction = fraction
        b.tvEatenBig.contentDescription = fmtCal(eaten)
    }

    companion object {
        private const val TAG_ADD_MEAL = "add_meal"
        private const val TAG_EDIT_MEAL = "edit_meal"
        private const val TAG_FILTER_MEALS = "filter_meals"
    }
}

private fun fmtCal(d: Double): String =
    if (d % 1 == 0.0) "%.0f".format(d) else "%.1f".format(d)

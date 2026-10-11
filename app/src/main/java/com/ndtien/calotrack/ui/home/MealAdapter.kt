package com.ndtien.calotrack.ui.home

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ndtien.calotrack.R
import com.ndtien.calotrack.data.MealEntry
import com.ndtien.calotrack.databinding.ItemMealBinding

class MealAdapter(
    private val onEdit: (MealEntry) -> Unit,
    private val onDelete: (MealEntry) -> Unit
) : ListAdapter<MealEntry, MealAdapter.MealViewHolder>(MealDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemMealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MealViewHolder(binding, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MealViewHolder(
        private val binding: ItemMealBinding,
        private val onEdit: (MealEntry) -> Unit,
        private val onDelete: (MealEntry) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(meal: MealEntry) {
            val context = binding.root.context
            binding.tvMealFoodName.text = meal.foodName
            // Kiểu bản xem thử: "00:02 · 1 suất"
            binding.tvMealInfo.text = "${meal.time} · ${fmtCal(meal.portion)} ${context.getString(R.string.portion_unit_label)}"
            binding.tvMealCalories.text = fmtCal(meal.totalCalories)

            // Badge rút gọn kiểu bản xem thử: Sáng / Trưa / Tối / Phụ
            binding.tvMealTypeBadge.text = context.getString(
                when (meal.mealType) {
                    "BREAKFAST" -> R.string.meal_tag_breakfast
                    "LUNCH" -> R.string.meal_tag_lunch
                    "DINNER" -> R.string.meal_tag_dinner
                    else -> R.string.meal_tag_snack
                }
            )

            // Màu badge theo loại bữa: sáng (hổ phách) / trưa (xanh lá) / tối (xanh dương) / phụ (tím)
            val (bgRes, fgRes) = when (meal.mealType) {
                "BREAKFAST" -> R.color.meal_breakfast_bg to R.color.meal_breakfast_fg
                "LUNCH" -> R.color.meal_lunch_bg to R.color.meal_lunch_fg
                "DINNER" -> R.color.meal_dinner_bg to R.color.meal_dinner_fg
                else -> R.color.meal_snack_bg to R.color.meal_snack_fg
            }
            binding.tvMealTypeBadge.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, bgRes))
            binding.tvMealTypeBadge.setTextColor(ContextCompat.getColor(context, fgRes))

            binding.btnEditMeal.setOnClickListener { onEdit(meal) }
            binding.btnDeleteMeal.setOnClickListener { onDelete(meal) }
        }
    }

    private class MealDiffCallback : DiffUtil.ItemCallback<MealEntry>() {
        override fun areItemsTheSame(oldItem: MealEntry, newItem: MealEntry): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MealEntry, newItem: MealEntry): Boolean =
            oldItem == newItem
    }
}

private fun fmtCal(d: Double): String =
    if (d % 1 == 0.0) "%.0f".format(d) else "%.1f".format(d)

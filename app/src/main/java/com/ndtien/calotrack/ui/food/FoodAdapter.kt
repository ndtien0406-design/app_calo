package com.ndtien.calotrack.ui.food

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ndtien.calotrack.R
import com.ndtien.calotrack.data.Food
import com.ndtien.calotrack.databinding.ItemFoodBinding
import com.ndtien.calotrack.util.FoodImageLoader

/**
 * Adapter hiển thị danh sách thực phẩm (item_food.xml kiểu thẻ nổi Soft-Tech).
 * [onEdit]/[onDelete] là lambda được gọi khi bấm thẻ/nút sửa hoặc nút xóa của từng dòng.
 */
class FoodAdapter(
    private val onEdit: (Food) -> Unit,
    private val onDelete: (Food) -> Unit
) : ListAdapter<Food, FoodAdapter.FoodViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Food>() {
            override fun areItemsTheSame(oldItem: Food, newItem: Food): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Food, newItem: Food): Boolean =
                oldItem == newItem
        }
    }

    inner class FoodViewHolder(
        private val binding: ItemFoodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(food: Food) {
            val context = binding.root.context
            binding.tvFoodName.text = food.name
            binding.tvFoodCategory.text = food.category
            FoodImageLoader.loadInto(binding.ivFoodThumb, food.name, food.category)
            // Kiểu bản xem thử: "550" + "kcal / suất"
            binding.tvFoodCalories.text = fmtCal(food.caloriesPerServing)
            binding.tvFoodUnit.text =
                "${context.getString(R.string.unit_kcal)} / ${food.servingUnit}"

            // Bảng màu ngữ nghĩa theo nhóm thực phẩm (hổ phách / xanh ngọc / xanh lam / tím)
            val (bgRes, fgRes, accentRes) = when (food.category) {
                "Tinh bột", "Bánh kẹo" -> Triple(
                    R.color.meal_breakfast_bg,
                    R.color.meal_breakfast_fg,
                    R.color.meal_breakfast_accent
                )
                "Rau củ", "Trái cây" -> Triple(
                    R.color.meal_lunch_bg,
                    R.color.meal_lunch_fg,
                    R.color.meal_lunch_accent
                )
                "Món chính", "Đồ uống" -> Triple(
                    R.color.meal_dinner_bg,
                    R.color.meal_dinner_fg,
                    R.color.meal_dinner_accent
                )
                else -> Triple(
                    R.color.meal_snack_bg,
                    R.color.meal_snack_fg,
                    R.color.meal_snack_accent
                )
            }
            binding.tvFoodCategory.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, bgRes))
            binding.tvFoodCategory.setTextColor(ContextCompat.getColor(context, fgRes))
            binding.viewFoodCatAccent.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, accentRes))

            binding.root.setOnClickListener { onEdit(food) }
            binding.btnEditFood.setOnClickListener { onEdit(food) }
            binding.btnDeleteFood.setOnClickListener { onDelete(food) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemFoodBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.itemView.apply {
            alpha = 0f
            translationY = 16f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
}

/** Định dạng calo: bỏ phần thập phân nếu là số nguyên. */
private fun fmtCal(d: Double): String =
    if (d % 1.0 == 0.0) "%.0f".format(d) else "%.1f".format(d)

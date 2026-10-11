package com.ndtien.calotrack.ui.food

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ndtien.calotrack.R
import com.ndtien.calotrack.data.Food
import com.ndtien.calotrack.databinding.ItemFoodBinding

/**
 * Adapter hiển thị danh sách thực phẩm (item_food.xml).
 * [onEdit]/[onDelete] là lambda được gọi khi bấm nút sửa/xóa của từng dòng.
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
            // Kiểu bản xem thử: "550" + "kcal / suất"
            binding.tvFoodCalories.text = fmtCal(food.caloriesPerServing)
            binding.tvFoodUnit.text =
                "${context.getString(R.string.unit_kcal)} / ${food.servingUnit}"
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
    }
}

/** Định dạng calo: bỏ phần thập phân nếu là số nguyên. */
private fun fmtCal(d: Double): String =
    if (d % 1.0 == 0.0) "%.0f".format(d) else "%.1f".format(d)

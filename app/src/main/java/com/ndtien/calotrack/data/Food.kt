package com.ndtien.calotrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val caloriesPerServing: Double,
    val servingUnit: String = "phần",
    val category: String = "Khác"
)

/** Các nhóm món chuẩn (thứ tự hiển thị ở chip chọn nhóm khi thêm/sửa món). */
object FoodCategories {
    val ALL = listOf(
        "Tinh bột", "Món chính", "Chất đạm", "Rau củ",
        "Món ăn nhẹ", "Trái cây", "Đồ uống", "Bánh kẹo"
    )
    const val DEFAULT = "Món chính"
}

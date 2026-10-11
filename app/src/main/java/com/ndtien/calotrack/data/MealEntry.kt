package com.ndtien.calotrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: Long? = null,
    val foodName: String,
    val caloriesPerServing: Double,
    val portion: Double,
    val totalCalories: Double,
    val mealType: String,          // "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK"
    val date: String,              // yyyy-MM-dd
    val time: String,              // HH:mm
    val createdAt: Long
)

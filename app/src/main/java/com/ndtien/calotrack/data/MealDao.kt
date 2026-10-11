package com.ndtien.calotrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE date = :date ORDER BY time ASC")
    fun getMealsByDate(date: String): Flow<List<MealEntry>>

    @Query("SELECT COALESCE(SUM(totalCalories),0) FROM meals WHERE date = :date")
    fun getTotalCaloriesByDate(date: String): Flow<Double>

    @Query("SELECT date AS date, COALESCE(SUM(totalCalories),0) AS total FROM meals WHERE date BETWEEN :from AND :to GROUP BY date ORDER BY date ASC")
    fun getDailyTotals(from: String, to: String): Flow<List<DayTotal>>

    @Query("SELECT date AS date, COALESCE(SUM(totalCalories),0) AS total FROM meals GROUP BY date ORDER BY date DESC")
    fun getAllDayTotals(): Flow<List<DayTotal>>

    @Insert
    suspend fun insert(meal: MealEntry): Long

    @Update
    suspend fun update(meal: MealEntry)

    @Delete
    suspend fun delete(meal: MealEntry)

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getById(id: Long): MealEntry?
}

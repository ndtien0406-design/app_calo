package com.ndtien.calotrack.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Food::class, MealEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun mealDao(): MealDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            val appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(appContext, AppDatabase::class.java, "calotrack.db")
                    .addCallback(object : RoomDatabase.Callback() {
                        // Seed đồng bộ trong chính transaction tạo DB: không race với UI,
                        // và nếu lỗi thì DB không bị tạo dở với bảng rỗng.
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            SeedData.getFoods().forEach { food ->
                                db.insert(
                                    "foods",
                                    SQLiteDatabase.CONFLICT_NONE,
                                    ContentValues().apply {
                                        put("name", food.name)
                                        put("caloriesPerServing", food.caloriesPerServing)
                                        put("servingUnit", food.servingUnit)
                                        put("category", food.category)
                                    }
                                )
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

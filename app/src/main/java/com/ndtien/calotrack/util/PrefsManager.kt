package com.ndtien.calotrack.util

import android.content.Context

class PrefsManager(context: Context) {

    companion object {
        const val PREFS_NAME = "calotrack_prefs"
        const val KEY_TARGET_CALORIES = "target_calories"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_UNIT = "unit"
        const val KEY_DEMO_SEEDED = "demo_seeded"
        const val DEFAULT_TARGET = 2000
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var targetCalories: Int
        get() = prefs.getInt(KEY_TARGET_CALORIES, DEFAULT_TARGET)
        set(v) = prefs.edit().putInt(KEY_TARGET_CALORIES, v).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(v) = prefs.edit().putBoolean(KEY_DARK_MODE, v).apply()

    /** Đã nạp dữ liệu demo (bữa ăn mẫu) hay chưa. */
    var demoSeeded: Boolean
        get() = prefs.getBoolean(KEY_DEMO_SEEDED, false)
        set(v) = prefs.edit().putBoolean(KEY_DEMO_SEEDED, v).apply()

    val unit: String
        get() = prefs.getString(KEY_UNIT, "kcal") ?: "kcal"
}

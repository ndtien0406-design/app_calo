package com.ndtien.calotrack.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.ndtien.calotrack.data.AppDatabase
import com.ndtien.calotrack.data.DayTotal
import com.ndtien.calotrack.data.MealDao
import com.ndtien.calotrack.util.DateUtils

/**
 * ViewModel màn hình "Lịch sử".
 * - [last7]: 7 ngày gần nhất (tăng dần, yyyy-MM-dd) — dùng làm trục X của biểu đồ.
 * - [weeklyTotals]: tổng calo từng ngày trong 7 ngày (ngày không có dữ liệu
 *   sẽ không xuất hiện trong list → fragment tự điền 0).
 * - [dayTotals]: tổng calo của mọi ngày từng ghi bữa ăn (mới nhất trước) — dùng cho list chi tiết.
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val mealDao: MealDao = AppDatabase.getInstance(application).mealDao()

    val last7: List<String> = DateUtils.last7Days()

    val weeklyTotals: LiveData<List<DayTotal>> =
        mealDao.getDailyTotals(last7.first(), last7.last()).asLiveData()

    val dayTotals: LiveData<List<DayTotal>> =
        mealDao.getAllDayTotals().asLiveData()
}

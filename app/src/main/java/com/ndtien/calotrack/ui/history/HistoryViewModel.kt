package com.ndtien.calotrack.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
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

    /** Khoảng 7 ngày đang hiển thị; tính lại khi đã qua nửa đêm (xem [refreshRangeIfNewDay]). */
    private val range = MutableLiveData(DateUtils.last7Days())

    val last7: List<String>
        get() = range.value ?: DateUtils.last7Days()

    val weeklyTotals: LiveData<List<DayTotal>> =
        range.switchMap { days -> mealDao.getDailyTotals(days.first(), days.last()).asLiveData() }

    val dayTotals: LiveData<List<DayTotal>> =
        mealDao.getAllDayTotals().asLiveData()

    /** Gọi khi màn hình hiện lại: nếu đã sang ngày mới thì dời trục 7 ngày tới hôm nay. */
    fun refreshRangeIfNewDay() {
        val fresh = DateUtils.last7Days()
        if (fresh != range.value) range.value = fresh
    }
}

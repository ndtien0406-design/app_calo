package com.ndtien.calotrack.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.ndtien.calotrack.data.AppDatabase
import com.ndtien.calotrack.data.MealEntry
import com.ndtien.calotrack.util.DateUtils
import com.ndtien.calotrack.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val mealDao = AppDatabase.getInstance(application).mealDao()
    private val prefs = PrefsManager(application)

    val selectedDate = MutableLiveData(DateUtils.today())

    /**
     * Ngày mà app tự đặt (không phải user chọn tay).
     * Dùng để quyết định có tự nhảy sang ngày mới khi qua nửa đêm hay không.
     */
    private var autoDate: String = DateUtils.today()

    /**
     * User chuyển ngày thủ công bằng nút < >.
     * Nếu user tự quay về đúng hôm nay thì coi như ngày tự động trở lại.
     */
    fun setSelectedDateManual(date: String) {
        selectedDate.value = date
        autoDate = if (date == DateUtils.today()) date else ""
    }

    /**
     * Tự về "hôm nay" khi đã sang ngày mới và user không đang xem tay một ngày khác.
     * Gọi trong onResume của HomeFragment. Ngày mới bắt đầu trống (0 bữa ăn, 0 kcal);
     * mục tiêu calo nằm trong SharedPreferences nên giữ nguyên.
     * @return true nếu vừa nhảy sang ngày mới (để hiện snackbar "Đã sang ngày mới").
     */
    fun rollToTodayIfNewDay(): Boolean {
        val t = DateUtils.today()
        val cur = selectedDate.value
        if (cur != null && cur < t && cur == autoDate) {
            selectedDate.value = t
            autoDate = t
            return true
        }
        return false
    }

    val meals: LiveData<List<MealEntry>> =
        selectedDate.switchMap { date -> mealDao.getMealsByDate(date).asLiveData() }

    val totalCalories: LiveData<Double> =
        selectedDate.switchMap { date -> mealDao.getTotalCaloriesByDate(date).asLiveData() }

    /** Bộ lọc danh sách bữa ăn (bản xem thử: chỉ lọc qua bottom sheet, không chip nhanh). */
    val mealFilter = MutableLiveData(MealFilter())

    private val _filteredMeals = MediatorLiveData<List<MealEntry>>()
    val filteredMeals: LiveData<List<MealEntry>> = _filteredMeals

    init {
        _filteredMeals.addSource(meals) { applyMealFilter() }
        _filteredMeals.addSource(mealFilter) { applyMealFilter() }
    }

    /** Lọc + sắp xếp danh sách bữa ăn theo MealFilter hiện tại (kết hợp AND). */
    private fun applyMealFilter() {
        val f = mealFilter.value ?: MealFilter()
        var result = meals.value.orEmpty().filter { m ->
            (f.mealType == null || m.mealType == f.mealType) &&
                    (f.kcalMin == null || m.totalCalories >= f.kcalMin) &&
                    (f.kcalMax == null || m.totalCalories <= f.kcalMax)
        }
        result = when (f.sort) {
            SortMode.BY_TIME -> result.sortedBy { it.time }
            SortMode.CAL_HIGH_LOW -> result.sortedByDescending { it.totalCalories }
            SortMode.CAL_LOW_HIGH -> result.sortedBy { it.totalCalories }
        }
        _filteredMeals.value = result
    }

    /** Số nhóm lọc đang bật (hiển thị badge trên icon phễu). */
    fun activeFilterCount(): Int {
        val f = mealFilter.value ?: return 0
        var c = 0
        if (f.mealType != null) c++
        if (f.kcalMin != null || f.kcalMax != null) c++
        if (f.sort != SortMode.BY_TIME) c++
        return c
    }

    fun clearMealFilter() {
        mealFilter.value = MealFilter()
    }

    private val _targetCalories = MutableLiveData(prefs.targetCalories)
    val targetCalories: LiveData<Int> = _targetCalories

    /** Đọc lại mục tiêu từ Settings (gọi trong onResume của Fragment). */
    fun refreshTarget() {
        _targetCalories.value = prefs.targetCalories
    }

    fun deleteMeal(meal: MealEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            mealDao.delete(meal)
        }
    }

    /** Hoàn tác xóa: chèn lại đúng bản ghi cũ (giữ nguyên id). */
    fun restoreMeal(meal: MealEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            mealDao.insert(meal)
        }
    }

    /** Chuyển ngày đang xem (±n ngày), không vượt quá hôm nay. */
    fun shiftDay(n: Int) {
        val cur = selectedDate.value ?: DateUtils.today()
        val next = DateUtils.addDays(cur, n)
        if (next > DateUtils.today()) return
        setSelectedDateManual(next)
    }
}

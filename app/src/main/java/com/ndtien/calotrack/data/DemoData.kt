package com.ndtien.calotrack.data

import android.content.Context
import com.ndtien.calotrack.util.DateUtils
import com.ndtien.calotrack.util.PrefsManager
import kotlinx.coroutines.flow.first

/**
 * Dữ liệu demo: bữa ăn của 14 ngày gần nhất (tính lùi từ hôm nay) để Trang chủ, biểu đồ 7 ngày
 * và Lịch sử có nội dung ngay khi mở app. Chỉ nạp một lần, và chỉ khi chưa có bữa ăn nào.
 * Mỗi bữa tham chiếu món có trong [SeedData] theo tên.
 */
object DemoData {

    private data class Plan(
        val type: String,
        val time: String,
        val food: String,
        val portion: Double = 1.0
    )

    private const val B = "BREAKFAST"
    private const val L = "LUNCH"
    private const val D = "DINNER"
    private const val S = "SNACK"

    /** Key = số ngày lùi so với hôm nay (0 = hôm nay). */
    private val plans: Map<Int, List<Plan>> = mapOf(
        13 to listOf(
            Plan(B, "07:10", "Xôi mặn"), Plan(B, "07:15", "Cà phê sữa"),
            Plan(L, "12:05", "Cơm tấm sườn"), Plan(D, "19:00", "Phở bò"), Plan(S, "15:30", "Chuối")
        ),
        12 to listOf(
            Plan(B, "07:30", "Bánh mì thịt"), Plan(B, "07:35", "Cà phê sữa"),
            Plan(L, "12:15", "Bún chả"), Plan(D, "19:20", "Cơm gà"), Plan(S, "16:00", "Trà sữa")
        ),
        11 to listOf(
            Plan(B, "07:00", "Bánh mì trứng"), Plan(L, "12:00", "Hủ tiếu"),
            Plan(D, "18:45", "Cơm trắng"), Plan(D, "18:45", "Cá thu", 1.5),
            Plan(D, "18:50", "Canh chua cá"), Plan(S, "15:45", "Táo")
        ),
        10 to listOf(
            Plan(B, "06:50", "Phở gà"), Plan(B, "06:55", "Cà phê sữa"),
            Plan(L, "12:30", "Cơm tấm sườn"), Plan(D, "19:30", "Bún bò Huế"),
            Plan(S, "15:20", "Trà sữa"), Plan(S, "21:00", "Khoai tây chiên")
        ),
        9 to listOf(
            Plan(B, "07:20", "Xôi xéo"), Plan(L, "12:10", "Cơm chiên dương châu"),
            Plan(D, "19:10", "Bánh xèo", 2.0), Plan(S, "16:10", "Sữa tươi")
        ),
        8 to listOf(
            Plan(B, "07:05", "Bánh mì pate"), Plan(B, "07:10", "Cà phê đen"),
            Plan(L, "12:20", "Bún riêu"), Plan(D, "19:00", "Cơm trắng", 1.5),
            Plan(D, "19:00", "Thịt ba chỉ"), Plan(D, "19:05", "Rau muống luộc"), Plan(S, "15:40", "Chuối")
        ),
        7 to listOf(
            Plan(B, "07:40", "Mì tôm"), Plan(L, "12:00", "Cơm gà"),
            Plan(D, "19:15", "Gà rán", 3.0), Plan(S, "20:30", "Nước ngọt")
        ),
        6 to listOf(
            Plan(B, "07:00", "Phở bò"), Plan(B, "07:05", "Cà phê sữa"),
            Plan(L, "12:10", "Cơm tấm sườn"), Plan(D, "19:25", "Bún chả"),
            Plan(S, "15:30", "Bánh flan"), Plan(S, "16:15", "Trà sữa")
        ),
        5 to listOf(
            Plan(B, "07:15", "Bánh cuốn"), Plan(B, "07:20", "Sữa đậu nành"),
            Plan(L, "12:00", "Cơm trắng"), Plan(L, "12:00", "Thịt heo nạc"), Plan(L, "12:05", "Canh rau"),
            Plan(L, "12:05", "Rau cải xào tỏi"), Plan(D, "18:50", "Cháo gà"), Plan(S, "15:50", "Cam")
        ),
        4 to listOf(
            Plan(B, "07:25", "Bánh mì thịt"), Plan(B, "07:30", "Cà phê sữa"),
            Plan(L, "12:20", "Hủ tiếu xào"), Plan(D, "19:05", "Bún bò Huế"), Plan(S, "16:00", "Chuối")
        ),
        3 to listOf(
            Plan(B, "07:10", "Xôi mặn"), Plan(B, "07:15", "Sữa tươi"),
            Plan(L, "12:05", "Cơm chiên dương châu"), Plan(D, "19:20", "Phở bò"),
            Plan(S, "15:30", "Bánh flan"), Plan(S, "16:00", "Nước cam ép")
        ),
        2 to listOf(
            Plan(B, "07:20", "Bánh mì chả cá"), Plan(B, "07:25", "Cà phê sữa"),
            Plan(L, "12:15", "Cơm tấm sườn"), Plan(D, "19:10", "Cơm gà"), Plan(S, "16:20", "Trà sữa")
        ),
        1 to listOf(
            Plan(B, "07:00", "Phở bò"), Plan(B, "07:05", "Cà phê sữa"),
            Plan(L, "12:10", "Bún chả"), Plan(D, "19:00", "Cơm trắng"),
            Plan(D, "19:00", "Tôm", 1.5), Plan(D, "19:05", "Canh chua cá"), Plan(D, "19:05", "Salad trộn"),
            Plan(S, "15:45", "Táo")
        ),
        // Hôm nay: chỉ các bữa có giờ <= bây giờ mới được thêm (xem seedIfNeeded)
        0 to listOf(
            Plan(B, "06:30", "Bánh mì thịt"), Plan(B, "06:35", "Cà phê sữa"),
            Plan(L, "12:10", "Bún chả"), Plan(S, "15:30", "Chuối"), Plan(D, "19:00", "Cơm gà")
        )
    )

    /** Nạp dữ liệu demo một lần. An toàn khi gọi nhiều lần / trên luồng nền. */
    suspend fun seedIfNeeded(context: Context) {
        val prefs = PrefsManager(context)
        if (prefs.demoSeeded) return

        val db = AppDatabase.getInstance(context)
        val mealDao = db.mealDao()
        if (mealDao.getAllDayTotals().first().isNotEmpty()) {
            prefs.demoSeeded = true
            return
        }

        val foods = db.foodDao().getAllFoods().first().associateBy { it.name }
        val today = DateUtils.today()
        val now = DateUtils.nowTime()
        val baseMillis = System.currentTimeMillis()
        var seq = 0L

        plans.toSortedMap(compareByDescending { it }).forEach { (daysAgo, list) ->
            val date = DateUtils.addDays(today, -daysAgo)
            for (p in list) {
                // Hôm nay: bữa sáng luôn có (giờ kẹp về "bây giờ"), các bữa sau chỉ có khi đã đến giờ
                val time = if (date == today && p.type == B) minOf(p.time, now) else p.time
                if (date == today && time > now) continue
                val food = foods[p.food] ?: continue
                mealDao.insert(
                    MealEntry(
                        foodId = food.id,
                        foodName = food.name,
                        caloriesPerServing = food.caloriesPerServing,
                        portion = p.portion,
                        totalCalories = food.caloriesPerServing * p.portion,
                        mealType = p.type,
                        date = date,
                        time = time,
                        createdAt = baseMillis + seq++
                    )
                )
            }
        }
        prefs.demoSeeded = true
    }
}

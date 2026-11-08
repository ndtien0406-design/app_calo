package com.ndtien.calotrack

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.ndtien.calotrack.databinding.ActivityMainBinding
import com.ndtien.calotrack.ui.food.FoodListFragment
import com.ndtien.calotrack.ui.history.HistoryFragment
import com.ndtien.calotrack.ui.home.HomeFragment
import com.ndtien.calotrack.ui.settings.SettingsFragment
import com.ndtien.calotrack.util.DateUtils
import com.ndtien.calotrack.util.PrefsManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val homeFragment = HomeFragment()
    private val foodListFragment = FoodListFragment()
    private val historyFragment = HistoryFragment()
    private val settingsFragment = SettingsFragment()
    private var activeFragment: Fragment = homeFragment
    private var activeTabIndex: Int = 0
    private var sliderAnimator: AnimatorSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PrefsManager(this)
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // App bar kiểu bản xem thử: pill ngày hôm nay
        binding.tvHeaderDate.text = DateUtils.todayPill()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, settingsFragment, TAG_SETTINGS).hide(settingsFragment)
                .add(R.id.fragment_container, historyFragment, TAG_HISTORY).hide(historyFragment)
                .add(R.id.fragment_container, foodListFragment, TAG_FOOD).hide(foodListFragment)
                .add(R.id.fragment_container, homeFragment, TAG_HOME)
                .commit()
        } else {
            val tag = savedInstanceState.getString(KEY_ACTIVE, TAG_HOME)
            activeFragment = supportFragmentManager.findFragmentByTag(tag) ?: homeFragment
            activeTabIndex = tabIndexForTag(tag)
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val (target, newIndex) = when (item.itemId) {
                R.id.menu_home -> (supportFragmentManager.findFragmentByTag(TAG_HOME) ?: homeFragment) to 0
                R.id.menu_food -> (supportFragmentManager.findFragmentByTag(TAG_FOOD) ?: foodListFragment) to 1
                R.id.menu_history -> (supportFragmentManager.findFragmentByTag(TAG_HISTORY) ?: historyFragment) to 2
                R.id.menu_settings -> (supportFragmentManager.findFragmentByTag(TAG_SETTINGS) ?: settingsFragment) to 3
                else -> return@setOnItemSelectedListener false
            }

            val direction = if (newIndex >= activeTabIndex) 1 else -1
            val changed = target != activeFragment

            if (changed) {
                supportFragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(target)
                    .commitNowAllowingStateLoss()
                activeFragment = target
                activeTabIndex = newIndex

                // Hiệu ứng chuyển màn hình sắc nét theo hướng trượt (Shared-Axis X + Micro-Scale)
                animateFragmentEntrance(target.view, direction)
            }

            animateNavSlider(newIndex, animate = changed)
            animateSelectedTabItem(newIndex)
            updateHeaderSubtitle(direction = if (changed) direction else 0)
            true
        }

        val initialMenu = if (savedInstanceState == null) {
            R.id.menu_home
        } else {
            when (savedInstanceState.getString(KEY_ACTIVE, TAG_HOME)) {
                TAG_FOOD -> R.id.menu_food
                TAG_HISTORY -> R.id.menu_history
                TAG_SETTINGS -> R.id.menu_settings
                else -> R.id.menu_home
            }
        }
        binding.bottomNav.selectedItemId = initialMenu
        binding.bottomNav.doOnLayout {
            animateNavSlider(activeTabIndex, animate = false)
        }
        updateHeaderSubtitle(direction = 0)
    }

    /**
     * Chuyển cảnh nội dung Fragment sắc nét kiểu Uiverse Shared-Axis:
     * trượt ngắn theo hướng tab (24dp) + phóng nhẹ từ 0.985 -> 1.0 + làm rõ nhanh (210ms).
     */
    private fun animateFragmentEntrance(view: View?, direction: Int) {
        val v = view ?: return
        val shiftPx = 24f * resources.displayMetrics.density * direction
        v.animate().cancel()
        v.translationX = shiftPx
        v.scaleX = 0.985f
        v.scaleY = 0.985f
        v.alpha = 0f
        v.animate()
            .translationX(0f)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(210L)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    /**
     * Thanh trượt phát sáng (Uiverse Sliding Pill Indicator) ở mép trên BottomNavigationView:
     * co giãn đàn hồi trục X (liquid stretch 1.0 -> 1.45 -> 1.0) trong khi lướt sang vị trí tab mới.
     */
    private fun animateNavSlider(tabIndex: Int, animate: Boolean) {
        val navWidth = binding.bottomNav.width
        if (navWidth <= 0) return
        val tabWidth = navWidth / 4f
        val sliderWidth = binding.viewNavSlider.width.takeIf { it > 0 }
            ?: (42f * resources.displayMetrics.density).toInt()
        val targetX = tabIndex * tabWidth + (tabWidth - sliderWidth) / 2f

        sliderAnimator?.cancel()
        if (!animate) {
            binding.viewNavSlider.translationX = targetX
            binding.viewNavSlider.scaleX = 1f
            return
        }

        val moveX = ObjectAnimator.ofFloat(binding.viewNavSlider, View.TRANSLATION_X, targetX).apply {
            duration = 260L
            interpolator = OvershootInterpolator(0.85f)
        }
        val stretchX = ObjectAnimator.ofFloat(binding.viewNavSlider, View.SCALE_X, 1f, 1.45f, 1f).apply {
            duration = 260L
            interpolator = FastOutSlowInInterpolator()
        }
        sliderAnimator = AnimatorSet().apply {
            playTogether(moveX, stretchX)
            start()
        }
    }

    /** Hiệu ứng nảy lò xo (Spring Pop) trên mục tab vừa chạm. */
    private fun animateSelectedTabItem(tabIndex: Int) {
        val menuView = binding.bottomNav.getChildAt(0) as? ViewGroup ?: return
        val itemView = menuView.getChildAt(tabIndex) ?: return
        itemView.animate().cancel()
        itemView.scaleX = 0.88f
        itemView.scaleY = 0.88f
        itemView.translationY = 3f * resources.displayMetrics.density
        itemView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(250L)
            .setInterpolator(OvershootInterpolator(2.1f))
            .start()
    }

    /** Cho Fragment khác (Lịch sử) đưa người dùng về tab Trang chủ. */
    fun showHome() {
        binding.bottomNav.selectedItemId = R.id.menu_home
    }

    private fun updateHeaderSubtitle(direction: Int) {
        val newText = getString(
            when (activeFragment) {
                is FoodListFragment -> R.string.nav_food
                is HistoryFragment -> R.string.nav_history
                is SettingsFragment -> R.string.nav_settings
                else -> R.string.nav_home
            }
        )
        if (direction != 0 && binding.tvHeaderSubtitle.text != newText) {
            val shift = 12f * resources.displayMetrics.density * direction
            binding.tvHeaderSubtitle.animate().cancel()
            binding.tvHeaderSubtitle.text = newText
            binding.tvHeaderSubtitle.translationX = shift
            binding.tvHeaderSubtitle.alpha = 0f
            binding.tvHeaderSubtitle.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(190L)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        } else {
            binding.tvHeaderSubtitle.text = newText
        }
        // Trang chủ đã có hàng chọn ngày "Hôm nay · Thứ…, dd/MM" -> ẩn pill ngày để không lặp
        binding.tvHeaderDate.visibility =
            if (activeFragment is HomeFragment) View.GONE else View.VISIBLE
    }

    private fun tabIndexForTag(tag: String?): Int = when (tag) {
        TAG_FOOD -> 1
        TAG_HISTORY -> 2
        TAG_SETTINGS -> 3
        else -> 0
    }

    override fun onResume() {
        super.onResume()
        // Ngày trên pill được tính lại mỗi lần quay về app (đúng cả khi đã qua nửa đêm)
        binding.tvHeaderDate.text = DateUtils.todayPill()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val tag = when (activeFragment) {
            is FoodListFragment -> TAG_FOOD
            is HistoryFragment -> TAG_HISTORY
            is SettingsFragment -> TAG_SETTINGS
            else -> TAG_HOME
        }
        outState.putString(KEY_ACTIVE, tag)
    }

    companion object {
        private const val KEY_ACTIVE = "active_fragment"
        private const val TAG_HOME = "home"
        private const val TAG_FOOD = "food"
        private const val TAG_HISTORY = "history"
        private const val TAG_SETTINGS = "settings"
    }
}

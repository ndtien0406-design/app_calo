package com.ndtien.calotrack

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.animation.doOnEnd
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
import kotlin.math.hypot

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var activeFragment: Fragment? = null
    private var activeTag: String = TAG_HOME
    private var activeTabIndex: Int = 0
    private var sliderAnimator: AnimatorSet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PrefsManager(this)
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        if (themeSnapshot != null) {
            window.setWindowAnimations(R.style.WindowNoAnimation)
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nếu vừa chuyển Sáng/Tối: đặt ảnh chụp giao diện cũ phía dưới và mở giao diện mới bằng sóng tròn Circular Reveal
        runThemeCircularRevealIfNeeded()

        // App bar kiểu bản xem thử: pill ngày hôm nay
        binding.tvHeaderDate.text = DateUtils.todayPill()

        // Khởi tạo lười (Lazy Fragment): chỉ dựng đúng Fragment đang hiển thị để vào app và đổi Sáng/Tối tức thì (< 15ms)
        activeTag = savedInstanceState?.getString(KEY_ACTIVE, TAG_HOME) ?: TAG_HOME
        activeTabIndex = tabIndexForTag(activeTag)
        val initialFragment = supportFragmentManager.findFragmentByTag(activeTag)
            ?: createFragmentForTag(activeTag).also { frag ->
                supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_container, frag, activeTag)
                    .commitNowAllowingStateLoss()
            }
        activeFragment = initialFragment

        // Ẩn các fragment phụ nếu hệ thống khôi phục nhiều hơn 1 fragment từ trạng thái cũ
        supportFragmentManager.fragments.forEach { frag ->
            if (frag != null && frag != initialFragment && !frag.isHidden) {
                supportFragmentManager.beginTransaction()
                    .hide(frag)
                    .commitNowAllowingStateLoss()
            }
        }

        binding.bottomNav.selectedItemId = menuIdForTag(activeTag)

        binding.bottomNav.setOnItemSelectedListener { item ->
            val (targetTag, newIndex) = when (item.itemId) {
                R.id.menu_home -> TAG_HOME to 0
                R.id.menu_food -> TAG_FOOD to 1
                R.id.menu_history -> TAG_HISTORY to 2
                R.id.menu_settings -> TAG_SETTINGS to 3
                else -> return@setOnItemSelectedListener false
            }

            val direction = if (newIndex >= activeTabIndex) 1 else -1
            val changed = targetTag != activeTag

            if (changed) {
                binding.bottomNav.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                val tx = supportFragmentManager.beginTransaction()
                activeFragment?.let { tx.hide(it) }
                var target = supportFragmentManager.findFragmentByTag(targetTag)
                if (target == null) {
                    target = createFragmentForTag(targetTag)
                    tx.add(R.id.fragment_container, target, targetTag)
                } else {
                    tx.show(target)
                }
                tx.commitNowAllowingStateLoss()

                activeFragment = target
                activeTag = targetTag
                activeTabIndex = newIndex

                // Hiệu ứng chuyển màn hình sắc nét theo hướng trượt (Shared-Axis X + Micro-Scale)
                animateFragmentEntrance(target.view, direction)
            }

            animateNavSlider(newIndex, animate = changed)
            animateSelectedTabItem(newIndex)
            updateHeaderSubtitle(direction = if (changed) direction else 0)
            true
        }

        binding.bottomNav.doOnLayout {
            animateNavSlider(activeTabIndex, animate = false)
        }
        updateHeaderSubtitle(direction = 0)
    }

    /**
     * Chuyển đổi chế độ Sáng/Tối không khựng kiểu Telegram:
     * 1. Chụp lại giao diện hiện tại vào bộ nhớ đệm (themeSnapshot) + tọa độ tâm nút gạt
     * 2. Gỡ các Fragment đang ẩn để Activity chỉ cần dựng lại đúng 1 Fragment đang mở (< 15ms)
     * 3. Sau khi dựng lại, chạy sóng tròn Circular Reveal 60fps lan tỏa từ công tắc ra toàn màn hình
     */
    fun switchThemeSmoothly(darkMode: Boolean, anchorView: View) {
        val root = binding.root
        if (root.width > 0 && root.height > 0) {
            runCatching {
                val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                root.draw(canvas)
                themeSnapshot = bmp

                val rootLoc = IntArray(2)
                val anchorLoc = IntArray(2)
                root.getLocationInWindow(rootLoc)
                anchorView.getLocationInWindow(anchorLoc)
                revealCenterX = anchorLoc[0] - rootLoc[0] + anchorView.width / 2
                revealCenterY = anchorLoc[1] - rootLoc[1] + anchorView.height / 2
            }
        }

        // Gỡ các Fragment đang ẩn trước khi recreate để không phải inflate lại cả 4 màn hình cùng lúc
        val current = activeFragment
        val hiddenFragments = supportFragmentManager.fragments.filter { it != null && it != current }
        if (hiddenFragments.isNotEmpty()) {
            val tx = supportFragmentManager.beginTransaction()
            hiddenFragments.forEach { tx.remove(it) }
            tx.commitNowAllowingStateLoss()
        }

        window.setWindowAnimations(R.style.WindowNoAnimation)
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun runThemeCircularRevealIfNeeded() {
        val snapshot = themeSnapshot ?: return
        val contentLayout = findViewById<ViewGroup>(android.R.id.content) ?: return

        val overlayView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_XY
            setImageBitmap(snapshot)
        }
        // Đặt ảnh chụp giao diện cũ ở lớp 0 (phía dưới binding.root)
        contentLayout.addView(overlayView, 0)
        binding.root.visibility = View.INVISIBLE

        binding.root.doOnLayout { rootView ->
            rootView.visibility = View.VISIBLE
            val cx = revealCenterX.coerceIn(0, rootView.width)
            val cy = revealCenterY.coerceIn(0, rootView.height)
            val maxRadius = hypot(rootView.width.toDouble(), rootView.height.toDouble()).toFloat()

            ViewAnimationUtils.createCircularReveal(rootView, cx, cy, 0f, maxRadius).apply {
                duration = 380L
                interpolator = FastOutSlowInInterpolator()
                doOnEnd {
                    contentLayout.removeView(overlayView)
                    themeSnapshot = null
                    window.setWindowAnimations(0)
                }
                start()
            }
        }
    }

    private fun createFragmentForTag(tag: String): Fragment = when (tag) {
        TAG_FOOD -> FoodListFragment()
        TAG_HISTORY -> HistoryFragment()
        TAG_SETTINGS -> SettingsFragment()
        else -> HomeFragment()
    }

    private fun menuIdForTag(tag: String): Int = when (tag) {
        TAG_FOOD -> R.id.menu_food
        TAG_HISTORY -> R.id.menu_history
        TAG_SETTINGS -> R.id.menu_settings
        else -> R.id.menu_home
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
            when (activeTag) {
                TAG_FOOD -> R.string.nav_food
                TAG_HISTORY -> R.string.nav_history
                TAG_SETTINGS -> R.string.nav_settings
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
            if (activeTag == TAG_HOME) View.GONE else View.VISIBLE
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
        outState.putString(KEY_ACTIVE, activeTag)
    }

    companion object {
        private const val KEY_ACTIVE = "active_fragment"
        private const val TAG_HOME = "home"
        private const val TAG_FOOD = "food"
        private const val TAG_HISTORY = "history"
        private const val TAG_SETTINGS = "settings"

        private var themeSnapshot: Bitmap? = null
        private var revealCenterX: Int = 0
        private var revealCenterY: Int = 0
    }
}

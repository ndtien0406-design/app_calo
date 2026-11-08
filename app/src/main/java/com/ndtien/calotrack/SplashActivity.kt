package com.ndtien.calotrack

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.ndtien.calotrack.data.DemoData
import com.ndtien.calotrack.databinding.ActivitySplashBinding
import com.ndtien.calotrack.util.PrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Màn hình khởi động chuyên nghiệp lấy cảm hứng từ Uiverse.io (/loaders):
 * - Vòng xoay kép ngược chiều + sóng xung Sonar bao quanh logo
 * - Thanh 5 cột sóng nhịp điệu (Wave Bars) ở chân trang
 * - Chuyển cảnh mượt sang MainActivity sau 1,75 giây
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PrefsManager(this)
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hoạt ảnh xuất hiện theo tầng (staggered entrance)
        binding.ivSplashLogo.apply {
            scaleX = 0.65f
            scaleY = 0.65f
            alpha = 0f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(520L)
                .setInterpolator(OvershootInterpolator(1.35f))
                .start()
        }

        binding.tvSplashTitle.apply {
            alpha = 0f
            translationY = 18f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120L)
                .setDuration(420L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        binding.tvSplashTagline.apply {
            alpha = 0f
            translationY = 18f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(200L)
                .setDuration(420L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        binding.layoutSplashLoader.apply {
            alpha = 0f
            translationY = 16f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(260L)
                .setDuration(420L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Nạp bữa ăn demo (một lần); scope riêng để không bị huỷ khi splash đóng giữa chừng
        CoroutineScope(Dispatchers.IO).launch { DemoData.seedIfNeeded(applicationContext) }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1750)
    }
}

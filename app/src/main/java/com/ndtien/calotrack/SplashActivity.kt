package com.ndtien.calotrack

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import com.ndtien.calotrack.data.DemoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ndtien.calotrack.databinding.ActivitySplashBinding

/**
 * Màn hình chào khi mở app: logo chính thức + tên app + vòng xoay Material 3,
 * tự chuyển sang MainActivity sau ~1,3 giây.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = com.ndtien.calotrack.util.PrefsManager(this)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            if (prefs.darkMode) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        ActivitySplashBinding.inflate(layoutInflater).also { setContentView(it.root) }

        // Nạp bữa ăn demo (một lần); scope riêng để không bị huỷ khi splash đóng giữa chừng
        CoroutineScope(Dispatchers.IO).launch { DemoData.seedIfNeeded(applicationContext) }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1300)
    }
}

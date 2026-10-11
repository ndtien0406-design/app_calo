package com.ndtien.calotrack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
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
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val target = when (item.itemId) {
                R.id.menu_home -> supportFragmentManager.findFragmentByTag(TAG_HOME) ?: homeFragment
                R.id.menu_food -> supportFragmentManager.findFragmentByTag(TAG_FOOD) ?: foodListFragment
                R.id.menu_history -> supportFragmentManager.findFragmentByTag(TAG_HISTORY) ?: historyFragment
                R.id.menu_settings -> supportFragmentManager.findFragmentByTag(TAG_SETTINGS) ?: settingsFragment
                else -> return@setOnItemSelectedListener false
            }
            if (target != activeFragment) {
                supportFragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(target)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit()
                activeFragment = target
            }
            // Subtitle trên app bar đổi theo tab (kiểu bản xem thử)
            updateHeaderSubtitle()
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
        updateHeaderSubtitle()
    }

    /** Cho Fragment khác (Lịch sử) đưa người dùng về tab Trang chủ. */
    fun showHome() {
        binding.bottomNav.selectedItemId = R.id.menu_home
    }

    private fun updateHeaderSubtitle() {
        binding.tvHeaderSubtitle.text = getString(
            when (activeFragment) {
                is FoodListFragment -> R.string.nav_food
                is HistoryFragment -> R.string.nav_history
                is SettingsFragment -> R.string.nav_settings
                else -> R.string.nav_home
            }
        )
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

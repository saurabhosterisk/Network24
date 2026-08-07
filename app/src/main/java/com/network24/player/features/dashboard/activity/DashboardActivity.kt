package com.network24.player.features.dashboard.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.internal.NavigationMenuView
import com.network24.player.R
import com.network24.player.features.live.activity.FavouriteChannelsActivity
import com.network24.player.features.live.activity.LiveCategoryActivity
import com.network24.player.core.base.BaseActivity
import com.network24.player.core.cache.CacheManager
import com.network24.player.core.preferences.PreferenceManager
import com.network24.player.databinding.ActivityDashboardBinding
import com.network24.player.features.chat.activity.ChatHubActivity
import com.network24.player.features.login.activity.LoginActivity
import com.network24.player.features.live.repository.LiveRepository
import com.network24.player.features.live.repository.SyncCallback
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DashboardActivity : BaseActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var prefs: PreferenceManager

    // 🔥 1. Repository add kiya data sync karne ke liye
    private lateinit var repository: LiveRepository

    // 🔥 2. Loading dialog variable
    private var loadingDialog: AlertDialog? = null

    private val handler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            val now = Date()
            binding.txtClock.text =
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
            binding.txtDate.text =
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(now)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PreferenceManager(this)

        // 🔥 3. Repository initialize kiya
        repository = LiveRepository(CacheManager(this))

        loadDashboard()

        binding.cardLiveTv.post {
            binding.cardLiveTv.requestFocus()
        }

        // ===== Right drawer menu wiring (3 dots) =====
        binding.btnMore.setOnClickListener {
            openRightDrawer(binding.drawerLayout)
        }

        setupOptionalRightDrawerMenu(
            drawerLayout = binding.drawerLayout,
            navView = binding.rightNav
        ) { itemId ->
            when (itemId) {
                R.id.action_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    true
                }
                R.id.action_refresh_all -> {
                    // 🔥 Refresh button click hone par data dubara sync karwa do
                    syncInitialData(forceRefresh = true)
                    true
                }
                R.id.action_refresh_guide -> {
                    Toast.makeText(this, "Refreshing TV Guide...", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_settings -> {
                    true
                }
                R.id.action_logout -> {
                    prefs.clear()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                    true
                }
                else -> false
            }
        }

        // Focus first item when the right drawer opens (DPAD friendly)
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                if (drawerView.id == binding.rightNav.id) {
                    binding.rightNav.post {
                        val menuView = binding.rightNav.getChildAt(0) as? NavigationMenuView
                        if (menuView != null) {
                            for (i in 0 until menuView.childCount) {
                                val child = menuView.getChildAt(i)
                                if (child.isFocusable) {
                                    child.requestFocus()
                                    break
                                }
                            }
                        }
                    }
                }
            }
        })

        setClickListeners()
        handler.post(clockRunnable)

        // 🔥 4. App khulte hi background mein data sync karna shuru kar do
        syncInitialData(forceRefresh = false)
    }

    // ============================================
    // 🔥 LOADER DIALOG FUNCTIONS
    // ============================================
    private fun showLoader() {
        if (loadingDialog == null) {
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
            val builder = AlertDialog.Builder(this)
            builder.setView(view)
            builder.setCancelable(false) // User click karke band nahi kar payega
            loadingDialog = builder.create()
            // Dialog background transparent karna zaroori hai agar custom corners chahiye toh
            loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        if (!isFinishing && loadingDialog?.isShowing == false) {
            loadingDialog?.show()
        }
    }

    private fun hideLoader() {
        if (loadingDialog != null && loadingDialog!!.isShowing) {
            loadingDialog?.dismiss()
        }
    }

    // ============================================
    // 🔥 DATA SYNC FUNCTION (24 HOURS LOGIC K SATH)
    // ============================================
    private fun syncInitialData(forceRefresh: Boolean = false) {
        val lastSyncTime = prefs.getLastSyncTime()
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursInMillis = 24 * 60 * 60 * 1000L // 24 hours in milliseconds

        // Agar forceRefresh nahi hai (yani automatic call hai) aur 24 ghante nahi hue hain, toh return kar jao
        if (!forceRefresh && (currentTime - lastSyncTime < twentyFourHoursInMillis)) {
            // Already synced in last 24 hours, so don't show loader or download again.
            return
        }

        // 1. Screen par "Downloading..." ka loader show karo
        showLoader()

        // 2. Repository ko call karo data laane ke liye
        repository.syncAllData(
            server = prefs.getServer(),
            username = prefs.getUsername(),
            password = prefs.getPassword(),
            callback = object : SyncCallback {
                override fun onSuccess() {
                    hideLoader()

                    // 🔥 Data download success hone par current time save kar lo
                    prefs.setLastSyncTime(System.currentTimeMillis())

                    Toast.makeText(this@DashboardActivity, "Channels Updated Successfully!", Toast.LENGTH_SHORT).show()
                }

                override fun onError(message: String) {
                    hideLoader()
                    Toast.makeText(this@DashboardActivity, "Failed to update: $message", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun loadDashboard() {
        binding.txtUserName.text = prefs.getUsername()
        binding.txtStatus.text = prefs.getStatus()
        binding.txtPlan.text = if (prefs.isTrial()) "Trial" else "Premium"
        binding.txtConnections.text = "${prefs.getActiveConnections()} / ${prefs.getMaxConnections()}"

        val expiry = prefs.getExpiry()
        if (expiry > 0) {
            val expiryDate = Date(expiry * 1000)
            binding.txtExpiry.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(expiryDate)
            val remainingDays = TimeUnit.MILLISECONDS.toDays(expiryDate.time - System.currentTimeMillis())

            if (remainingDays > 0) {
                binding.txtRemaining.text = "$remainingDays Days"
            } else {
                binding.txtRemaining.text = "Expired"
            }
            binding.btnRenew.visibility = if (remainingDays <= 15) View.VISIBLE else View.GONE
        } else {
            binding.txtExpiry.text = "--"
            binding.txtRemaining.text = "--"
            binding.btnRenew.visibility = View.GONE
        }
    }

    private fun setClickListeners() {
        binding.cardLiveTv.setOnClickListener {
            startActivity(Intent(this, LiveCategoryActivity::class.java))
        }
        binding.cardFavorites.setOnClickListener {
            startActivity(Intent(this, FavouriteChannelsActivity::class.java))
        }
        binding.cardNotification.setOnClickListener {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
        }
        binding.cardSupport.setOnClickListener {
            startActivity(Intent(this, ChatHubActivity::class.java))
        }
        binding.cardSettings.setOnClickListener {
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
        }
        binding.btnRenew.setOnClickListener {
            Toast.makeText(this, "Renew Subscription", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clockRunnable)
        hideLoader() // Activity destroy hone par loader band karna zaroori hai (Memory leak se bachne ke liye)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}
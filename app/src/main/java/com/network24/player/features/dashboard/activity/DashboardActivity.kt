package com.network24.player.features.dashboard.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.internal.NavigationMenuView
import com.network24.player.R
import com.network24.player.core.base.BaseActivity
import com.network24.player.core.preferences.PreferenceManager
import com.network24.player.databinding.ActivityDashboardBinding
import com.network24.player.features.chat.activity.ChatHubActivity
import com.network24.player.features.live.activity.FavouriteChannelsActivity
import com.network24.player.features.live.activity.LiveCategoryActivity
import com.network24.player.features.live.repository.LiveRepository
import com.network24.player.features.live.repository.SyncCallback
import com.network24.player.features.login.activity.LoginActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var prefs: PreferenceManager
    private lateinit var repository: LiveRepository

    private val handler = Handler(Looper.getMainLooper())
    private var isInitialSyncRunning = false

    private val clockRunnable = object : Runnable {
        override fun run() {
            val now = Date()
            binding.txtClock.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
            binding.txtDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(now)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)
        repository = LiveRepository(this)

        // 1. Check Credentials (Offline Check)
        if (!hasCredentials()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
            return
        }

        // 2. Load UI instantly from local data (Offline-First)
        loadDashboard()

        // 3. Setup UI interactions
        binding.cardLiveTv.post { binding.cardLiveTv.requestFocus() }
        setupDrawerAndMenu()
        setClickListeners()

        // 4. Start Clock
        handler.post(clockRunnable)

        // 5. Auto sync on start (respects 24h policy)
        syncInitialData(forceRefresh = false)
    }

    private fun hasCredentials(): Boolean {
        return prefs.getServer().isNotBlank() &&
                prefs.getUsername().isNotBlank() &&
                prefs.getPassword().isNotBlank()
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

            binding.txtRemaining.text = if (remainingDays > 0) "$remainingDays Days" else "Expired"
            binding.btnRenew.visibility = if (remainingDays <= 15) View.VISIBLE else View.GONE
        } else {
            binding.txtExpiry.text = "--"
            binding.txtRemaining.text = "--"
            binding.btnRenew.visibility = View.GONE
        }
    }

    private fun setupDrawerAndMenu() {
        binding.btnMore.setOnClickListener { openRightDrawer(binding.drawerLayout) }

        setupOptionalRightDrawerMenu(
            drawerLayout = binding.drawerLayout,
            navView = binding.rightNav
        ) { itemId ->
            when (itemId) {
                R.id.action_home -> {
                    // Just close drawer since we are already on Home
                    closeRightDrawer(binding.drawerLayout)
                    true
                }
                R.id.action_refresh_all -> {
                    syncInitialData(forceRefresh = true)
                    true
                }
                R.id.action_refresh_guide -> {
                    refreshTvGuide() // Uses the BaseActivity helper
                    true
                }
                R.id.action_settings -> {
                    Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
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

    // ----------------------------
    // Sync with 24h policy
    // ----------------------------
    private fun syncInitialData(forceRefresh: Boolean = false) {
        if (!hasCredentials()) return
        if (isInitialSyncRunning) return

        val lastSyncTime = prefs.getLastSyncTime()
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursInMillis = 24L * 60L * 60L * 1000L
        val isFirstSync = lastSyncTime <= 0L

        // If not forced and not first sync and within 24h -> skip sync
        if (!forceRefresh && !isFirstSync && (currentTime - lastSyncTime < twentyFourHoursInMillis)) {
            return
        }

        isInitialSyncRunning = true

        // Using the BaseActivity helper for clean UI loading
        runCallbackSyncWithLoader(
            loadingMessage = "Refreshing categories & channels…",
            successMessage = "Channels Updated Successfully!"
        ) { ok, fail ->
            repository.syncAllData(
                server = prefs.getServer(),
                username = prefs.getUsername(),
                password = prefs.getPassword(),
                callback = object : SyncCallback {
                    override fun onSuccess() {
                        isInitialSyncRunning = false
                        prefs.setLastSyncTime(System.currentTimeMillis())
                        ok() // Hides loader and shows success toast
                    }

                    override fun onError(message: String) {
                        isInitialSyncRunning = false
                        fail("Failed to update: $message") // Hides loader and shows error toast
                    }
                }
            )
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            closeRightDrawer(binding.drawerLayout)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clockRunnable)
    }
}

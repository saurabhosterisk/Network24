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
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var prefs: PreferenceManager
    private lateinit var repository: LiveRepository

    private var loadingDialog: AlertDialog? = null

    private val handler = Handler(Looper.getMainLooper())
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

        // If no login data, go to login (prevents weird sync calls)
        if (!hasCredentials()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
            return
        }

        loadDashboard()

        binding.cardLiveTv.post { binding.cardLiveTv.requestFocus() }

        binding.btnMore.setOnClickListener { openRightDrawer(binding.drawerLayout) }

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
                    syncInitialData(forceRefresh = true)
                    true
                }

                R.id.action_refresh_guide -> {
                    showLoader("Updating TV Guide… This can take a minute.")
                    lifecycleScope.launch {
                        val result = com.network24.player.core.sync.SyncManager(this@DashboardActivity)
                            .syncFullEpg(force = true)
                        hideLoader()
                        when (result) {
                            is com.network24.player.core.sync.SyncResult.Success -> {
                                Toast.makeText(this@DashboardActivity, "TV Guide Updated", Toast.LENGTH_SHORT).show()
                                sendBroadcast(Intent("ACTION_EPG_UPDATED"))
                            }

                            is com.network24.player.core.sync.SyncResult.Error -> {
                                Toast.makeText(this@DashboardActivity, result.message, Toast.LENGTH_LONG).show()
                            }

                            else -> {
                                Toast.makeText(this@DashboardActivity, "TV Guide sync finished.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    true
                }

                R.id.action_settings -> true

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

        setClickListeners()
        handler.post(clockRunnable)

        // Auto sync on start (respects 24h unless first run)
        syncInitialData(forceRefresh = false)
    }

    private fun hasCredentials(): Boolean {
        return prefs.getServer().isNotBlank() &&
                prefs.getUsername().isNotBlank() &&
                prefs.getPassword().isNotBlank()
    }

    // ----------------------------
    // Loader dialog
    // ----------------------------
    private fun showLoader(message: String = "Loading...") {
        if (loadingDialog == null) {
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
            val builder = AlertDialog.Builder(this)
            builder.setView(view)
            builder.setCancelable(false)
            loadingDialog = builder.create()
            loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        if (!isFinishing && loadingDialog?.isShowing == false) {
            loadingDialog?.show()
        }

        // ✅ update message
        loadingDialog?.findViewById<android.widget.TextView>(R.id.txtLoadingMessage)?.text = message
    }

    private fun hideLoader() {
        if (loadingDialog != null && loadingDialog!!.isShowing) {
            loadingDialog?.dismiss()
        }
    }

    // ----------------------------
    // Sync with 24h policy
    // ----------------------------
    private fun syncInitialData(forceRefresh: Boolean = false) {
        if (!hasCredentials()) return

        val lastSyncTime = prefs.getLastSyncTime()
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursInMillis = 24 * 60 * 60 * 1000L

        val isFirstSync = lastSyncTime <= 0L

        // If not forced and not first sync and within 24h -> skip
        if (!forceRefresh && !isFirstSync && (currentTime - lastSyncTime < twentyFourHoursInMillis)) {
            return
        }

        showLoader()

        repository.syncAllData(
            server = prefs.getServer(),
            username = prefs.getUsername(),
            password = prefs.getPassword(),
            callback = object : SyncCallback {
                override fun onSuccess() {
                    hideLoader()
                    prefs.setLastSyncTime(System.currentTimeMillis())
                    Toast.makeText(
                        this@DashboardActivity,
                        "Channels Updated Successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(message: String) {
                    hideLoader()
                    Toast.makeText(
                        this@DashboardActivity,
                        "Failed to update: $message",
                        Toast.LENGTH_LONG
                    ).show()
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
            binding.txtRemaining.text = if (remainingDays > 0) "$remainingDays Days" else "Expired"
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
        hideLoader()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}

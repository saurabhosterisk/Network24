package com.network24.player.features.live.activity

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.internal.NavigationMenuView
import com.network24.player.R
import com.network24.player.features.live.adapter.ChannelAdapter
import com.network24.player.core.base.BaseActivity
import com.network24.player.core.cache.CacheManager
import com.network24.player.core.preferences.PreferenceManager
import com.network24.player.databinding.ActivityChannelListBinding
import com.network24.player.features.dashboard.activity.DashboardActivity
import com.network24.player.features.live.models.LiveChannel
import com.network24.player.features.login.activity.LoginActivity
import com.network24.player.features.player.activity.PlayerActivity
import com.network24.player.features.player.manager.PlayerManager
import com.network24.player.features.player.state.PlayerState
import com.network24.player.features.live.repository.LiveRepository
import com.network24.player.features.live.repository.SyncCallback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ChannelListActivity : BaseActivity() {
    private lateinit var binding: ActivityChannelListBinding
    private lateinit var repository: LiveRepository
    private lateinit var prefs: PreferenceManager
    private var isGoingToFullscreen = false
    private lateinit var adapter: ChannelAdapter

    // 🔥 Loading Dialog Variable
    private var loadingDialog: AlertDialog? = null

    // 🔥 Retry Logic Variables
    private var retryCount = 0
    private val MAX_RETRIES = 1
    private var retryJob: Job? = null

    private val isTouchDevice by lazy {
        !packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
    private var previewPosition = -1

    // DO LISTS BANAYI HAIN (Ek backup ke liye, Ek dikhane ke liye)
    private val allChannels = mutableListOf<LiveChannel>()
    private val channelList = mutableListOf<LiveChannel>()
    private lateinit var categoryId: String

    // ==========================================
    // CUSTOM LOADER LISTENER W/ RETRY LOGIC
    // ==========================================
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_BUFFERING) {
                binding.progressLoading.visibility = View.VISIBLE
            } else {
                binding.progressLoading.visibility = View.GONE
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)

            if (retryCount < MAX_RETRIES) {
                retryCount++

                val toastMsg = "Playback Error. Trying to reconnect in 3 sec. ($retryCount)"
                Toast.makeText(this@ChannelListActivity, toastMsg, Toast.LENGTH_SHORT).show()

                retryJob?.cancel()
                retryJob = lifecycleScope.launch {
                    delay(3000)
                    if (channelList.isNotEmpty() && previewPosition != -1) {
                        val currentChannel = channelList[previewPosition]
                        val streamUrl = buildStreamUrl(currentChannel)
                        binding.progressLoading.visibility = View.VISIBLE
                        PlayerManager.play(this@ChannelListActivity, binding.playerView, streamUrl)
                    }
                }
            } else {
                binding.progressLoading.visibility = View.GONE
                binding.txtPlayerError.visibility = View.VISIBLE
                binding.txtNowTitle.text = "Playback Failed"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)
        repository = LiveRepository(CacheManager(this))
        categoryId = intent.getStringExtra("category_id") ?: ""
        binding.txtCategoryName.text = intent.getStringExtra("category_name") ?: "Live TV"

        binding.btnBack.setOnClickListener { finish() }

        // 🔥 subtitles hide (preview)
        binding.playerView.setShowSubtitleButton(false)
        binding.playerView.subtitleView?.visibility = View.GONE

        // 3 dots
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
                    forceRefreshData()
                    true
                }
                R.id.action_refresh_guide -> {
                    Toast.makeText(this, "Refreshing TV Guide...", Toast.LENGTH_SHORT).show()
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

        // drawer focus
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

        // Player click -> fullscreen
        binding.playerView.setOnClickListener {
            if (isTouchDevice && previewPosition != -1 && channelList.isNotEmpty()) {
                val currentChannel = channelList[previewPosition]
                openFullscreen(currentChannel, previewPosition)
            }
        }

        setupRecycler()
        setupSearch()
        loadChannels(forceRefresh = false)
    }

    // ============================================
    // LOADER DIALOG
    // ============================================
    private fun showLoader() {
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
    }

    private fun hideLoader() {
        if (loadingDialog != null && loadingDialog!!.isShowing) {
            loadingDialog?.dismiss()
        }
    }

    // ============================================
    // MANUAL REFRESH
    // ============================================
    private fun forceRefreshData() {
        showLoader()
        repository.syncAllData(
            server = prefs.getServer(),
            username = prefs.getUsername(),
            password = prefs.getPassword(),
            callback = object : SyncCallback {
                override fun onSuccess() {
                    hideLoader()
                    prefs.setLastSyncTime(System.currentTimeMillis())
                    Toast.makeText(this@ChannelListActivity, "Channels Refreshed Successfully!", Toast.LENGTH_SHORT).show()
                    loadChannels(forceRefresh = true)
                }

                override fun onError(message: String) {
                    hideLoader()
                    Toast.makeText(this@ChannelListActivity, "Failed to refresh: $message", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    // ==========================================
    // RECYCLER + ✅ favouriteIds pass
    // ==========================================
    private fun setupRecycler() {
        binding.rvChannels.layoutManager = LinearLayoutManager(this)

        adapter = ChannelAdapter(
            channels = mutableListOf(),
            favouriteIds = getSavedFavouriteChannels(), // ✅ FIX 1
            onFocused = { _, _ -> },
            onClicked = { channel, position ->
                if (previewPosition == position) {
                    openFullscreen(channel, position)
                } else {
                    previewPosition = position
                    adapter.setPlaying(position)
                    showPreview(channel)
                    loadProgramGuide(channel)
                }
            },
            onLongClicked = { channel, _ ->
                confirmToggleFavourite(channel)
            }
        )

        binding.rvChannels.adapter = adapter
        PlayerManager.attach(this, binding.playerView)
    }

    // ==========================================
    // SEARCH / FILTER
    // ==========================================
    private fun setupSearch() {
        binding.edtSearch.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterChannels(s.toString())
                }
                override fun afterTextChanged(s: Editable?) {}
            }
        )
    }

    private fun filterChannels(keyword: String) {
        val filtered = allChannels.filter { channel ->
            channel.name?.contains(keyword, ignoreCase = true) ?: false
        }

        channelList.clear()
        channelList.addAll(filtered)

        adapter.updateData(channelList)
        adapter.updateFavourites(getSavedFavouriteChannels()) // ✅ FIX (filter ke baad bhi hearts apply)

        if (previewPosition != -1 && allChannels.isNotEmpty()) {
            val currentlyPlayingChannel = allChannels[previewPosition]
            val newPosition = channelList.indexOf(currentlyPlayingChannel)
            adapter.setPlaying(newPosition)
        } else {
            adapter.setPlaying(-1)
        }
    }

    private fun loadChannels(forceRefresh: Boolean = false) {
        binding.edtSearch.text?.clear()
        binding.edtSearch.clearFocus()
        lifecycleScope.launch {
            try {
                val channels = repository.getChannels(
                    server = prefs.getServer(),
                    username = prefs.getUsername(),
                    password = prefs.getPassword(),
                    categoryId = categoryId,
                    forceRefresh = forceRefresh
                )

                allChannels.clear()
                allChannels.addAll(channels)
                channelList.clear()
                channelList.addAll(channels)

                adapter.updateData(channelList)
                adapter.updateFavourites(getSavedFavouriteChannels()) // ✅ FIX 2 (load ke baad hearts)

                if (channelList.isEmpty()) return@launch

                val targetPos = if (PlayerState.currentPosition in channelList.indices) {
                    PlayerState.currentPosition
                } else 0

                previewPosition = targetPos
                adapter.setPlaying(targetPos)
                showPreview(channelList[targetPos])
                loadProgramGuide(channelList[targetPos])

                if (!isTouchDevice) {
                    binding.rvChannels.post {
                        binding.rvChannels
                            .findViewHolderForAdapterPosition(targetPos)
                            ?.itemView
                            ?.requestFocus()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChannelListActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showPreview(channel: LiveChannel) {
        retryJob?.cancel()
        retryCount = 0

        binding.txtPlayerError.visibility = View.GONE

        val streamUrl = buildStreamUrl(channel)
        PlayerManager.play(this, binding.playerView, streamUrl)

        binding.txtOverlayChannel.text = channel.name ?: ""
        binding.txtOverlayProgram.text = "Loading TV Guide..."
        binding.txtNowTitle.text = "Loading TV Guide..."
        binding.txtNowTime.text = ""
        binding.txtNextTitle.text = ""
        binding.txtNextTime.text = ""
    }

    private fun loadProgramGuide(channel: LiveChannel) {
        val streamId = channel.stream_id ?: return
        lifecycleScope.launch {
            try {
                val epg = repository.getShortEPG(
                    prefs.getServer(),
                    prefs.getUsername(),
                    prefs.getPassword(),
                    streamId
                )
                val list = epg.epg_listings
                if (list.isEmpty()) {
                    binding.txtNowTitle.text = "No EPG"
                    binding.txtNowTime.text = ""
                    binding.txtNextTitle.text = ""
                    binding.txtNextTime.text = ""
                    binding.txtOverlayProgram.text = ""
                    return@launch
                }
                val now = list[0]
                binding.txtNowTitle.text = decodeBase64(now.title)
                binding.txtNowTime.text = "${formatTime(now.start)} - ${formatTime(now.end)}"
                binding.txtOverlayProgram.text = decodeBase64(now.title)
                if (list.size > 1) {
                    val next = list[1]
                    binding.txtNextTitle.text = decodeBase64(next.title)
                    binding.txtNextTime.text = "${formatTime(next.start)} - ${formatTime(next.end)}"
                } else {
                    binding.txtNextTitle.text = ""
                    binding.txtNextTime.text = ""
                }
            } catch (e: Exception) {
                binding.txtNowTitle.text = "EPG unavailable"
                binding.txtNowTime.text = ""
                binding.txtNextTitle.text = ""
                binding.txtNextTime.text = ""
                binding.txtOverlayProgram.text = ""
            }
        }
    }

    private fun openFullscreen(channel: LiveChannel, position: Int) {
        isGoingToFullscreen = true
        PlayerState.channels.clear()
        PlayerState.channels.addAll(channelList)
        PlayerState.currentPosition = position
        val streamUrl = buildStreamUrl(channel)
        PlayerManager.play(this, binding.playerView, streamUrl)
        startActivity(Intent(this, PlayerActivity::class.java))
    }

    private fun buildStreamUrl(channel: LiveChannel): String {
        val server = prefs.getServer().trim().trimEnd('/')
        val username = prefs.getUsername()
        val password = prefs.getPassword()
        return "$server/live/$username/$password/${channel.stream_id}.m3u8"
    }

    private fun decodeBase64(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return try {
            String(Base64.decode(value, Base64.DEFAULT))
        } catch (e: Exception) {
            value
        }
    }

    private fun formatTime(dateTime: String?): String {
        if (dateTime.isNullOrBlank()) return ""
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val output = SimpleDateFormat("hh:mm a", Locale.getDefault())
            output.format(input.parse(dateTime)!!)
        } catch (e: Exception) {
            dateTime
        }
    }

    override fun onPause() {
        super.onPause()
        binding.playerView.player?.removeListener(playerListener)
        if (!isGoingToFullscreen) PlayerManager.pause()
        PlayerManager.detach(binding.playerView)
    }

    override fun onResume() {
        super.onResume()
        isGoingToFullscreen = false
        PlayerManager.attach(this, binding.playerView)
        PlayerManager.resume()
        binding.playerView.player?.addListener(playerListener)

        val player = binding.playerView.player
        if (player?.playbackState == Player.STATE_READY) {
            binding.progressLoading.visibility = View.GONE
            binding.txtPlayerError.visibility = View.GONE
        } else if (player?.playbackState == Player.STATE_BUFFERING) {
            binding.progressLoading.visibility = View.VISIBLE
            binding.txtPlayerError.visibility = View.GONE
        } else if (player?.playerError != null) {
            binding.progressLoading.visibility = View.GONE
            binding.txtPlayerError.visibility = View.VISIBLE
            val finalError = "Sorry, This video can not be played. Please try again or pick another video."
            binding.txtNowTitle.text = finalError
            binding.txtOverlayProgram.text = finalError
        }

        // ✅ ensure heart icons correct after coming back from fullscreen
        adapter.updateFavourites(getSavedFavouriteChannels())
    }

    override fun onDestroy() {
        retryJob?.cancel()
        PlayerManager.detach(binding.playerView)
        if (isFinishing) PlayerManager.stop()
        hideLoader()
        super.onDestroy()
    }

    // ==========================================
    // FAVOURITES
    // ==========================================
    private fun getSavedFavouriteChannels(): Set<String> {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        return sharedPreferences.getStringSet("fav_channels", emptySet()) ?: emptySet()
    }

    private fun saveFavouriteChannels(favIds: Set<String>) {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("fav_channels", favIds).apply()
    }

    private fun toggleChannelFavourite(channel: LiveChannel) {
        val streamId = channel.stream_id?.toString() ?: return
        val currentFavs = getSavedFavouriteChannels().toMutableSet()

        if (currentFavs.contains(streamId)) {
            currentFavs.remove(streamId)
            saveFavouriteChannels(currentFavs)
            Toast.makeText(this, "${channel.name} removed from Favourites", Toast.LENGTH_SHORT).show()
        } else {
            currentFavs.add(streamId)
            saveFavouriteChannels(currentFavs)
            Toast.makeText(this, "${channel.name} added to Favourites", Toast.LENGTH_SHORT).show()
        }

        // ✅ FIX 3: instant UI update
        adapter.updateFavourites(currentFavs)
    }

    private fun confirmToggleFavourite(channel: LiveChannel) {
        val streamId = channel.stream_id?.toString() ?: return
        val name = channel.name ?: "this channel"

        val currentFavs = getSavedFavouriteChannels()
        val isFav = currentFavs.contains(streamId)

        val title = if (isFav) "Remove Favourite" else "Add Favourite"
        val message = if (isFav) {
            "Do you want to remove \"$name\" from favourites?"
        } else {
            "Do you want to add \"$name\" to favourites?"
        }
        val positiveText = if (isFav) "Remove" else "Add"

        // Use AppCompat dialog for best theme support
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { dialog, _ ->
                dialog.dismiss()
                toggleChannelFavourite(channel) // existing function (save + adapter update)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}
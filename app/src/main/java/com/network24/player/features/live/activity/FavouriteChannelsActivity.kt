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
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import com.network24.player.R
import com.network24.player.core.base.BaseActivity
import com.network24.player.core.preferences.PreferenceManager
import com.network24.player.databinding.ActivityFavouriteChannelsBinding
import com.network24.player.features.dashboard.activity.DashboardActivity
import com.network24.player.features.live.adapter.ChannelAdapter
import com.network24.player.features.live.models.LiveChannel
import com.network24.player.features.live.repository.LiveRepository
import com.network24.player.features.live.repository.SyncCallback
import com.network24.player.features.login.activity.LoginActivity
import com.network24.player.features.player.activity.PlayerActivity
import com.network24.player.features.player.manager.PlayerManager
import com.network24.player.features.player.state.PlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FavouriteChannelsActivity : BaseActivity() {

    private lateinit var binding: ActivityFavouriteChannelsBinding
    private lateinit var repository: LiveRepository
    private lateinit var prefs: PreferenceManager

    private var isGoingToFullscreen = false
    private lateinit var adapter: ChannelAdapter

    private var loadingDialog: AlertDialog? = null

    private var retryCount = 0
    private val MAX_RETRIES = 3
    private var retryJob: Job? = null

    private val isTouchDevice by lazy {
        !packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    private var previewPosition = -1
    private val allChannels = mutableListOf<LiveChannel>()
    private val channelList = mutableListOf<LiveChannel>()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            binding.progressLoading.visibility =
                if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)

            if (retryCount < MAX_RETRIES) {
                retryCount++
                Toast.makeText(
                    this@FavouriteChannelsActivity,
                    "Playback Error. Trying to reconnect in 3 sec. ($retryCount)",
                    Toast.LENGTH_SHORT
                ).show()

                retryJob?.cancel()
                retryJob = lifecycleScope.launch {
                    delay(3000)
                    if (channelList.isNotEmpty() && previewPosition in channelList.indices) {
                        val currentChannel = channelList[previewPosition]
                        val streamUrl = buildStreamUrl(currentChannel)
                        binding.progressLoading.visibility = View.VISIBLE
                        PlayerManager.play(this@FavouriteChannelsActivity, binding.playerView, streamUrl)
                    }
                }
            } else {
                binding.progressLoading.visibility = View.GONE
                binding.txtPlayerError.visibility = View.VISIBLE
                val finalError =
                    "Sorry, This video can not be played. Please try again or pick another video."
                binding.txtNowTitle.text = "Playback Failed"
                binding.txtOverlayProgram.text = finalError
                Toast.makeText(this@FavouriteChannelsActivity, finalError, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavouriteChannelsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)
        repository = LiveRepository(this)

        binding.btnBack.setOnClickListener { finish() }

        binding.playerView.setShowSubtitleButton(false)
        binding.playerView.subtitleView?.visibility = View.GONE

        binding.btnMore.setOnClickListener { openRightDrawer(binding.drawerLayout) }
        setupOptionalRightDrawerMenu(binding.drawerLayout, binding.rightNav) { itemId ->
            when (itemId) {
                R.id.action_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
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

        binding.playerView.setOnClickListener {
            if (isTouchDevice && previewPosition != -1 && channelList.isNotEmpty()) {
                openFullscreen(channelList[previewPosition], previewPosition)
            }
        }

        setupRecycler()
        setupSearch()

        // ✅ NEW: ensure DB has data before favourites filtering
        ensureInitialSyncThenLoadFavourites()
    }

    // ----------------------------
    // Loader dialog
    // ----------------------------
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

    // ----------------------------
    // FAV STORAGE
    // ----------------------------
    private fun getSavedFavouriteChannels(): Set<String> {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        return sharedPreferences.getStringSet("fav_channels", emptySet()) ?: emptySet()
    }

    private fun saveFavouriteChannels(favIds: Set<String>) {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("fav_channels", favIds).apply()
    }

    // Long press remove logic
    private fun removeFromFavourites(channel: LiveChannel) {
        val streamId = channel.stream_id?.toString() ?: return
        val favs = getSavedFavouriteChannels().toMutableSet()

        if (!favs.contains(streamId)) {
            Toast.makeText(this, "Already removed", Toast.LENGTH_SHORT).show()
            return
        }

        favs.remove(streamId)
        saveFavouriteChannels(favs)

        val removeIndex = channelList.indexOfFirst { it.stream_id?.toString() == streamId }
        allChannels.removeAll { it.stream_id?.toString() == streamId }
        if (removeIndex != -1) channelList.removeAt(removeIndex)

        adapter.updateData(channelList)
        adapter.updateFavourites(favs)

        Toast.makeText(this, "${channel.name} removed from Favourites", Toast.LENGTH_SHORT).show()

        if (channelList.isEmpty()) {
            previewPosition = -1
            binding.txtOverlayChannel.text = ""
            binding.txtOverlayProgram.text = ""
            binding.txtNowTitle.text = "No favourite channels"
            binding.txtNowTime.text = ""
            binding.txtNextTitle.text = ""
            binding.txtNextTime.text = ""
            binding.txtPlayerError.visibility = View.GONE
            PlayerManager.pause()
            return
        }

        if (previewPosition > removeIndex) previewPosition -= 1

        if (previewPosition == removeIndex || previewPosition !in channelList.indices) {
            previewPosition = previewPosition.coerceIn(0, channelList.lastIndex)
            adapter.setPlaying(previewPosition)
            showPreview(channelList[previewPosition])
            loadProgramGuide(channelList[previewPosition])
        } else {
            adapter.setPlaying(previewPosition)
        }
    }

    // ----------------------------
    // Auto First Sync + Load favourites
    // ----------------------------
    private fun ensureInitialSyncThenLoadFavourites() {
        lifecycleScope.launch {
            try {
                // Try loading ALL channels from DB (categoryId="")
                val allDbChannels = repository.getChannels(
                    server = prefs.getServer(),
                    username = prefs.getUsername(),
                    password = prefs.getPassword(),
                    categoryId = "",
                    forceRefresh = false
                )

                if (allDbChannels.isNotEmpty()) {
                    applyFavouriteFilterAndShow(allDbChannels)
                    return@launch
                }

                // If DB empty -> sync once
                showLoader()
                repository.syncAllData(
                    server = prefs.getServer(),
                    username = prefs.getUsername(),
                    password = prefs.getPassword(),
                    callback = object : SyncCallback {
                        override fun onSuccess() {
                            hideLoader()
                            prefs.setLastSyncTime(System.currentTimeMillis())
                            loadFavouriteChannels(forceRefresh = true)
                        }

                        override fun onError(message: String) {
                            hideLoader()
                            Toast.makeText(
                                this@FavouriteChannelsActivity,
                                "Initial sync failed: $message",
                                Toast.LENGTH_LONG
                            ).show()
                            // fallback attempt
                            loadFavouriteChannels(forceRefresh = false)
                        }
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@FavouriteChannelsActivity,
                    e.message ?: "Initial load failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ----------------------------
    // LOAD & FILTER
    // ----------------------------
    private fun loadFavouriteChannels(forceRefresh: Boolean = false) {
        binding.edtSearch.text?.clear()
        binding.edtSearch.clearFocus()

        lifecycleScope.launch {
            try {
                val allChannelsFromRepo = repository.getChannels(
                    server = prefs.getServer(),
                    username = prefs.getUsername(),
                    password = prefs.getPassword(),
                    categoryId = "",
                    forceRefresh = forceRefresh
                )

                applyFavouriteFilterAndShow(allChannelsFromRepo)
            } catch (e: Exception) {
                Toast.makeText(this@FavouriteChannelsActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun applyFavouriteFilterAndShow(allChannelsFromRepo: List<LiveChannel>) {
        val favIds = getSavedFavouriteChannels()

        val favChannels = allChannelsFromRepo.filter { channel ->
            favIds.contains(channel.stream_id?.toString().orEmpty())
        }

        allChannels.clear()
        allChannels.addAll(favChannels)

        channelList.clear()
        channelList.addAll(favChannels)

        adapter.updateData(channelList)
        adapter.updateFavourites(favIds)

        if (channelList.isEmpty()) {
            Toast.makeText(this, "No favourite channels found.", Toast.LENGTH_LONG).show()
            previewPosition = -1
            binding.txtNowTitle.text = "No favourite channels"
            binding.txtOverlayProgram.text = ""
            binding.txtOverlayChannel.text = ""
            return
        }

        previewPosition = 0
        adapter.setPlaying(0)
        showPreview(channelList[0])
        loadProgramGuide(channelList[0])
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
        return "$server/live/${prefs.getUsername()}/${prefs.getPassword()}/${channel.stream_id}.m3u8"
    }

    private fun setupRecycler() {
        binding.rvChannels.layoutManager = LinearLayoutManager(this)

        adapter = ChannelAdapter(
            channels = mutableListOf(),
            favouriteIds = getSavedFavouriteChannels(),
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
                confirmRemoveFavourite(channel)
            }
        )

        binding.rvChannels.adapter = adapter
        PlayerManager.attach(this, binding.playerView)
    }

    private fun setupSearch() {
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keyword = s.toString()
                val filtered = allChannels.filter {
                    it.name?.contains(keyword, ignoreCase = true) ?: false
                }
                channelList.clear()
                channelList.addAll(filtered)
                adapter.updateData(channelList)

                if (previewPosition !in channelList.indices) {
                    adapter.setPlaying(-1)
                } else {
                    adapter.setPlaying(previewPosition)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
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
                    binding.txtOverlayProgram.text = ""
                    return@launch
                }
                val now = list[0]
                binding.txtNowTitle.text = decodeBase64(now.title)
                binding.txtOverlayProgram.text = binding.txtNowTitle.text
            } catch (e: Exception) {
                binding.txtNowTitle.text = "EPG unavailable"
            }
        }
    }

    private fun decodeBase64(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return try {
            String(Base64.decode(value, Base64.DEFAULT))
        } catch (e: Exception) {
            value
        }
    }

    override fun onResume() {
        super.onResume()
        isGoingToFullscreen = false
        PlayerManager.attach(this, binding.playerView)
        PlayerManager.resume()
        binding.playerView.player?.addListener(playerListener)
    }

    override fun onPause() {
        super.onPause()
        binding.playerView.player?.removeListener(playerListener)
        if (!isGoingToFullscreen) PlayerManager.pause()
        PlayerManager.detach(binding.playerView)
    }

    override fun onDestroy() {
        retryJob?.cancel()
        PlayerManager.detach(binding.playerView)
        if (isFinishing) PlayerManager.stop()
        hideLoader()
        super.onDestroy()
    }

    private fun confirmRemoveFavourite(channel: LiveChannel) {
        val channelName = channel.name ?: "this channel"
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Remove Favourite")
            .setMessage("Do you want to remove \"$channelName\" from favourites?")
            .setPositiveButton("Remove") { dialog, _ ->
                dialog.dismiss()
                removeFromFavourites(channel)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

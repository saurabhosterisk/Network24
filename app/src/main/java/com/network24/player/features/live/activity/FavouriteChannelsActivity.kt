package com.network24.player.features.live.activity

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import com.network24.player.R
import com.network24.player.features.live.adapter.ChannelAdapter
import com.network24.player.core.base.BaseActivity
import com.network24.player.core.cache.CacheManager
import com.network24.player.core.preferences.PreferenceManager
import com.network24.player.databinding.ActivityFavouriteChannelsBinding
import com.network24.player.features.dashboard.activity.DashboardActivity
import com.network24.player.features.live.models.LiveChannel
import com.network24.player.features.login.activity.LoginActivity
import com.network24.player.features.player.activity.PlayerActivity
import com.network24.player.features.player.manager.PlayerManager
import com.network24.player.features.player.state.PlayerState
import com.network24.player.features.live.repository.LiveRepository
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
                Toast.makeText(this@FavouriteChannelsActivity, toastMsg, Toast.LENGTH_SHORT).show()

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
        repository = LiveRepository(CacheManager(this))

        binding.btnBack.setOnClickListener { finish() }

        binding.playerView.setShowSubtitleButton(false)
        binding.playerView.subtitleView?.visibility = View.GONE

        // Drawer Setup
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
        loadFavouriteChannels()
    }

    // ==========================================
    // FAV STORAGE
    // ==========================================
    private fun getSavedFavouriteChannels(): Set<String> {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        return sharedPreferences.getStringSet("fav_channels", emptySet()) ?: emptySet()
    }

    private fun saveFavouriteChannels(favIds: Set<String>) {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().putStringSet("fav_channels", favIds).apply()
    }

    // ✅ Long press remove logic
    private fun removeFromFavourites(channel: LiveChannel) {
        val streamId = channel.stream_id?.toString() ?: return
        val favs = getSavedFavouriteChannels().toMutableSet()

        if (!favs.contains(streamId)) {
            Toast.makeText(this, "Already removed", Toast.LENGTH_SHORT).show()
            return
        }

        favs.remove(streamId)
        saveFavouriteChannels(favs)

        // Remove from lists
        val removeIndex = channelList.indexOfFirst { it.stream_id?.toString() == streamId }
        allChannels.removeAll { it.stream_id?.toString() == streamId }
        if (removeIndex != -1) channelList.removeAt(removeIndex)

        adapter.updateData(channelList)

        Toast.makeText(this, "${channel.name} removed from Favourites", Toast.LENGTH_SHORT).show()

        // Handle preview if we removed current preview item
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

        // If removed item was before current index, shift left
        if (previewPosition > removeIndex) previewPosition -= 1

        // If removed current preview, play the nearest available
        if (previewPosition == removeIndex || previewPosition !in channelList.indices) {
            previewPosition = previewPosition.coerceIn(0, channelList.lastIndex)
            adapter.setPlaying(previewPosition)
            showPreview(channelList[previewPosition])
            loadProgramGuide(channelList[previewPosition])
        } else {
            adapter.setPlaying(previewPosition)
        }
    }

    // ==========================================
    // LOAD & FILTER
    // ==========================================
    private fun loadFavouriteChannels() {
        binding.edtSearch.text?.clear()
        binding.edtSearch.clearFocus()

        lifecycleScope.launch {
            try {
                val allNetworkChannels = repository.getChannels(
                    server = prefs.getServer(),
                    username = prefs.getUsername(),
                    password = prefs.getPassword(),
                    categoryId = ""
                )

                val favIds = getSavedFavouriteChannels()

                val favChannels = allNetworkChannels.filter { channel ->
                    favIds.contains(channel.stream_id?.toString().orEmpty())
                }

                allChannels.clear()
                allChannels.addAll(favChannels)

                channelList.clear()
                channelList.addAll(favChannels)

                adapter.updateData(channelList)

                if (channelList.isEmpty()) {
                    Toast.makeText(
                        this@FavouriteChannelsActivity,
                        "No favourite channels found.",
                        Toast.LENGTH_LONG
                    ).show()
                    previewPosition = -1
                    return@launch
                }

                previewPosition = 0
                adapter.setPlaying(0)
                showPreview(channelList[0])
                loadProgramGuide(channelList[0])

            } catch (e: Exception) {
                Toast.makeText(this@FavouriteChannelsActivity, e.message, Toast.LENGTH_LONG).show()
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
            favouriteIds = getSavedFavouriteChannels(), // optional
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
            // ✅ Long press = remove from favourites + remove from list
            onLongClicked = { channel, _ ->
                confirmRemoveFavourite(channel) // ✅ confirmation dialog first
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

                // keep playing marker safe
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
                    binding.txtNowTime.text = ""
                    binding.txtNextTitle.text = ""
                    binding.txtNextTime.text = ""
                    binding.txtOverlayProgram.text = ""
                    return@launch
                }

                val now = list[0]
                binding.txtNowTitle.text =
                    String(Base64.decode(now.title, Base64.DEFAULT))
                binding.txtOverlayProgram.text = binding.txtNowTitle.text

            } catch (e: Exception) {
                binding.txtNowTitle.text = "EPG unavailable"
            }
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
        super.onDestroy()
    }

    private fun confirmRemoveFavourite(channel: LiveChannel) {
        val channelName = channel.name ?: "this channel"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Remove Favourite")
            .setMessage("Do you want to remove \"$channelName\" from favourites?")
            .setPositiveButton("Remove") { dialog, _ ->
                dialog.dismiss()
                removeFromFavourites(channel) // ✅ actual remove
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
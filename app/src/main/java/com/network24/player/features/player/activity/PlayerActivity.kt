package com.network24.player.features.player.activity

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import com.network24.player.R
import com.network24.player.core.base.BaseActivity
import com.network24.player.core.cache.CacheManager
import com.network24.player.core.preferences.PreferenceManager
import com.network24.player.databinding.ActivityPlayerBinding
import com.network24.player.features.live.models.LiveChannel
import com.network24.player.features.live.models.ShortEPGResponse
import com.network24.player.features.player.manager.PlayerManager
import com.network24.player.features.player.state.PlayerState
import com.network24.player.features.live.repository.LiveRepository
import com.network24.player.features.player.ui.dialogs.StreamInfoDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class PlayerActivity : BaseActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var prefs: PreferenceManager
    private lateinit var repository: LiveRepository

    // 🔥 Retry Logic Variables
    private var retryCount = 0
    private val MAX_RETRIES = 1
    private var retryJob: Job? = null

    // 🔥 Variable to track if subtitles are currently ON or OFF
    private var isSubtitleEnabled = false

    // 🔥 Variable to track current aspect ratio mode
    private var currentAspectRatioIndex = 0

    // ==========================================
    // AUTO-HIDE UI HANDLER (Animated)
    // ==========================================
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        val animationDuration = 300L
        binding.topTint.animate().alpha(0f).setDuration(animationDuration).withEndAction {
            binding.topTint.visibility = View.GONE
        }.start()
        binding.txtChannelTitle.animate().alpha(0f).setDuration(animationDuration).withEndAction {
            binding.txtChannelTitle.visibility = View.GONE
        }.start()
        binding.bottomOverlay.animate().alpha(0f).translationY(50f).setDuration(animationDuration).withEndAction {
            binding.bottomOverlay.visibility = View.GONE
        }.start()
    }

    // ==========================================
    // CUSTOM LOADER LISTENER WITH RETRY LOGIC
    // ==========================================
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_BUFFERING) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            } else {
                binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)

            if (retryCount < MAX_RETRIES) {
                // 1. Retry Attempt Badao
                retryCount++

                // 2. Toast Dikhao
                val toastMsg = "Playback Error. Trying to reconnect in 3 sec. ($retryCount)"
                Toast.makeText(this@PlayerActivity, toastMsg, Toast.LENGTH_SHORT).show()

                // 3. 3 Seconds wait karke dobara play karo
                retryJob?.cancel()
                retryJob = lifecycleScope.launch {
                    delay(3000)

                    val currentChannel = PlayerState.currentChannel()
                    if (currentChannel != null) {
                        val streamUrl = "${prefs.getServer()}/live/${prefs.getUsername()}/${prefs.getPassword()}/${currentChannel.stream_id}.m3u8"
                        binding.progressBar.visibility = View.VISIBLE
                        PlayerManager.play(this@PlayerActivity, binding.playerView, streamUrl)
                    }
                }
            } else {
                // Maximum Retry (3) cross ho gaye, error dikhao
                binding.progressBar.visibility = View.GONE

                // 🔥 Naya Center Error TextView dikhao (XML me zarur add karein)
                binding.txtPlayerError.visibility = View.VISIBLE

                // Agar user UI hide na kar paye is error ke baad, toh aap ye line bhi chala sakte hain:
                showUiWithTimeout()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)
        repository = LiveRepository(CacheManager(this))

        // FULLSCREEN IMMERSIVE MODE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        binding.progressBar.visibility = View.GONE

        // ENABLE SUBTITLES (CC) PROGRAMMATICALLY BUT HIDDEN IN UI BY DEFAULT
        binding.playerView.setShowSubtitleButton(false)
        binding.playerView.subtitleView?.setApplyEmbeddedStyles(false)

        // ==========================================
        // INITIAL UI SETUP
        // ==========================================
        updateChannelUI(PlayerState.currentChannel())
        showUiWithTimeout()
        setupClickListeners()
        PlayerManager.moveTo(this, binding.playerView)

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    // =========================================================
    // BUTTON CLICK LISTENERS (Touch & Remote)
    // =========================================================
    private fun setupClickListeners() {
        binding.root.setOnClickListener { toggleUi() }
        binding.playerView.setOnClickListener { toggleUi() }

        binding.btnPlayPause.setOnClickListener {
            if (PlayerManager.isPlaying()) {
                PlayerManager.pause()
            } else {
                PlayerManager.resume()
            }
            showUiWithTimeout()
        }

        binding.btnNext.setOnClickListener {
            playNextChannel()
            showUiWithTimeout()
        }

        binding.btnPrev.setOnClickListener {
            playPreviousChannel()
            showUiWithTimeout()
        }

        binding.btnInfo.setOnClickListener {
            val currentChannel = PlayerState.currentChannel()
            val streamId = currentChannel?.stream_id
            if (streamId == null) {
                Toast.makeText(this, "Channel not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            StreamInfoDialog.newInstance(streamId.toString())
                .show(supportFragmentManager, "StreamInfoDialog")

            showUiWithTimeout()
        }

        binding.btnAspect.setOnClickListener {
            // Har click par agla mode select karein
            currentAspectRatioIndex = (currentAspectRatioIndex + 1) % 4

            val toastMessage = when (currentAspectRatioIndex) {
                0 -> {
                    // Default / Fit
                    binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    "Aspect Ratio: Fit"
                }
                1 -> {
                    // Fill (Stretches the video)
                    binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    "Aspect Ratio: Fill"
                }
                2 -> {
                    // Zoom (Crops the edges to fill screen)
                    binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    "Aspect Ratio: Zoom"
                }
                3 -> {
                    // Fixed Width / Height (Depends on orientation)
                    binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    "Aspect Ratio: Fixed Width"
                }
                else -> "Aspect Ratio: Fit"
            }

            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
            showUiWithTimeout()
        }

        // 🔥 CUSTOM CC BUTTON LOGIC
        binding.btnSubtitle.setOnClickListener {
            isSubtitleEnabled = !isSubtitleEnabled
            toggleSubtitles(isSubtitleEnabled)
            if (isSubtitleEnabled) {
                Toast.makeText(this, "Subtitles Enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Subtitles Disabled", Toast.LENGTH_SHORT).show()
            }
            showUiWithTimeout()
        }

        binding.btnGrid.setOnClickListener {
            Toast.makeText(this, "Grid Button Clicked", Toast.LENGTH_SHORT).show()
            showUiWithTimeout()
        }
    }

    private fun toggleSubtitles(enable: Boolean) {
        val player = binding.playerView.player ?: return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enable)
            .build()

        if (enable) {
            binding.btnSubtitle.setColorFilter(Color.parseColor("#FFC107"))
        } else {
            binding.btnSubtitle.setColorFilter(Color.WHITE)
        }
    }

    // PlayerActivity.kt mein onResume update karein:
    override fun onResume() {
        super.onResume()
        PlayerManager.attach(this, binding.playerView)
        PlayerManager.resume()
        binding.playerView.player?.addListener(playerListener)

        val currentState = binding.playerView.player?.playbackState

        // 🔥 Agar aate hi video chal rahi hai toh loader hide karein
        if (currentState == Player.STATE_READY) {
            binding.progressBar.visibility = View.GONE
        }
        // 🔥 Agar buffering ho rahi thi peeche, toh yahan loader dikhayein
        else if (currentState == Player.STATE_BUFFERING) {
            binding.progressBar.visibility = View.VISIBLE
        }

        toggleSubtitles(isSubtitleEnabled)
        showUiWithTimeout()

        binding.root.postDelayed({
            binding.btnPlayPause.requestFocus()
        }, 350)
    }


    override fun onPause() {
        super.onPause()
        binding.playerView.player?.removeListener(playerListener)
        hideHandler.removeCallbacks(hideRunnable)
        PlayerManager.pause()
        PlayerManager.detach(binding.playerView)
    }

    override fun onDestroy() {
        retryJob?.cancel() // Safely cancel coroutine job on destroy
        PlayerManager.detach(binding.playerView)
        super.onDestroy()
    }

    // =========================================================
    // UI HELPER FUNCTIONS
    // =========================================================
    private fun showUiWithTimeout() {
        val animationDuration = 300L
        if (binding.bottomOverlay.visibility != View.VISIBLE) {
            binding.topTint.alpha = 0f
            binding.topTint.visibility = View.VISIBLE
            binding.topTint.animate().alpha(1f).setDuration(animationDuration).start()

            binding.txtChannelTitle.alpha = 0f
            binding.txtChannelTitle.visibility = View.VISIBLE
            binding.txtChannelTitle.animate().alpha(1f).setDuration(animationDuration).start()

            binding.bottomOverlay.alpha = 0f
            binding.bottomOverlay.translationY = 50f
            binding.bottomOverlay.visibility = View.VISIBLE
            binding.bottomOverlay.animate().alpha(1f).translationY(0f).setDuration(animationDuration)
                .withEndAction {
                    binding.btnPlayPause.post {
                        binding.btnPlayPause.requestFocus()
                    }
                }.start()
        }
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, 5000)
    }

    private fun toggleUi() {
        if (binding.bottomOverlay.visibility == View.VISIBLE) {
            hideHandler.removeCallbacks(hideRunnable)
            hideRunnable.run()
        } else {
            showUiWithTimeout()
        }
    }

    private fun updateChannelUI(channel: LiveChannel?) {
        if (channel == null) return

        // Naya channel aane par purane retry reset kar do aur error hide kar do
        retryJob?.cancel()
        retryCount = 0
        binding.txtPlayerError.visibility = View.GONE

        val num = channel.num?.let { "$it - " } ?: ""
        val name = channel.name ?: "Unknown Channel"
        binding.txtChannelTitle.text = "$num$name"

        channel.stream_id?.let {
            loadEpg(it)
        }
    }

    // =========================================================
    // CHANNEL SURFING (NEXT / PREV LOGIC)
    // =========================================================
    private fun playNextChannel() {
        val nextChannel = PlayerState.next()
        if (nextChannel != null) {
            val streamUrl = "${prefs.getServer()}/live/${prefs.getUsername()}/${prefs.getPassword()}/${nextChannel.stream_id}.m3u8"
            PlayerManager.play(this, binding.playerView, streamUrl)
            updateChannelUI(nextChannel)
            toggleSubtitles(isSubtitleEnabled)
        }
    }

    private fun playPreviousChannel() {
        val prevChannel = PlayerState.previous()
        if (prevChannel != null) {
            val streamUrl = "${prefs.getServer()}/live/${prefs.getUsername()}/${prefs.getPassword()}/${prevChannel.stream_id}.m3u8"
            PlayerManager.play(this, binding.playerView, streamUrl)
            updateChannelUI(prevChannel)
            toggleSubtitles(isSubtitleEnabled)
        }
    }

    // =========================================================
    // TV REMOTE KEY EVENTS
    // =========================================================
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (binding.bottomOverlay.visibility != View.VISIBLE) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    showUiWithTimeout()
                    return true
                }
            }
        } else {
            hideHandler.removeCallbacks(hideRunnable)
            hideHandler.postDelayed(hideRunnable, 5000)
            when (keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_DPAD_UP -> {
                    playNextChannel()
                    return true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    playPreviousChannel()
                    return true
                }
            }
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            showUiWithTimeout()
            if (PlayerManager.isPlaying()) PlayerManager.pause() else PlayerManager.resume()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // =========================================================
    // EPG LOGIC
    // =========================================================
    private fun loadEpg(streamId: Int) {
        lifecycleScope.launch {
            try {
                val epg = repository.getShortEPG(
                    prefs.getServer(),
                    prefs.getUsername(),
                    prefs.getPassword(),
                    streamId
                )
                updateEpg(epg)
            } catch (e: Exception) {
                binding.txtNowTitle.text = "No EPG Data"
                binding.txtNextTitle.text = ""
                binding.txtNowTime.text = ""
                binding.txtNextTime.text = ""
                val layoutParams = binding.epgProgress.layoutParams
                layoutParams.width = 0
                binding.epgProgress.layoutParams = layoutParams
            }
        }
    }

    private fun decode(value: String?): String {
        if (value.isNullOrEmpty()) return ""
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

    private fun calculateEpgProgress(startStr: String?, endStr: String?): Float {
        if (startStr.isNullOrBlank() || endStr.isNullOrBlank()) return 0f
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val startTime = format.parse(startStr)?.time ?: return 0f
            val endTime = format.parse(endStr)?.time ?: return 0f
            val currentTime = System.currentTimeMillis()
            if (currentTime <= startTime) return 0f
            if (currentTime >= endTime) return 1f
            val totalDuration = endTime - startTime
            val elapsed = currentTime - startTime
            elapsed.toFloat() / totalDuration.toFloat()
        } catch (e: Exception) {
            0f
        }
    }

    private fun updateEpg(response: ShortEPGResponse) {
        val now = response.epg_listings.getOrNull(0)
        val next = response.epg_listings.getOrNull(1)
        if (now != null) {
            binding.txtNowTitle.text = decode(now.title)
            binding.txtNowTime.text = "${formatTime(now.start)} - ${formatTime(now.end)}"
            val progressPercent = calculateEpgProgress(now.start, now.end)
            binding.epgTrack.post {
                val trackWidth = binding.epgTrack.width
                val layoutParams = binding.epgProgress.layoutParams
                layoutParams.width = (trackWidth * progressPercent).toInt()
                binding.epgProgress.layoutParams = layoutParams
            }
        } else {
            binding.txtNowTitle.text = "No Program Info"
            binding.txtNowTime.text = ""
            val layoutParams = binding.epgProgress.layoutParams
            layoutParams.width = 0
            binding.epgProgress.layoutParams = layoutParams
        }
        if (next != null) {
            binding.txtNextTitle.text = decode(next.title)
            binding.txtNextTime.text = "${formatTime(next.start)} - ${formatTime(next.end)}"
        } else {
            binding.txtNextTitle.text = ""
            binding.txtNextTime.text = ""
        }
    }
}
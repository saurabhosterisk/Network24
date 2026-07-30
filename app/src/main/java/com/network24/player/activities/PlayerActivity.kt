package com.network24.player.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.network24.player.databinding.ActivityPlayerBinding
import com.network24.player.utils.PreferenceManager
import android.view.WindowManager
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.DefaultLoadControl
import android.os.Build
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var player: ExoPlayer
    private lateinit var prefs: PreferenceManager
    private var retryCount = 0
    private val maxRetry = 2

    private lateinit var connectivityManager: ConnectivityManager

    private var networkAvailable = true

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)

            runOnUiThread {

                if (!networkAvailable) {

                    networkAvailable = true

                    if (!player.isPlaying) {
                        player.prepare()
                        player.play()
                    }
                }
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)

            runOnUiThread {
                networkAvailable = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // ✅ Screen Always ON
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager(this)

        val streamId = intent.getIntExtra("stream_id", 0)

        val server = prefs.getServer().trimEnd('/')

        val username = prefs.getUsername()

        val password = prefs.getPassword()

        val url =
            "$server/live/$username/$password/$streamId.ts"

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                3000,   // Min Buffer
                10000,  // Max Buffer
                1000,   // Playback Start
                2000    // Rebuffer
            )
            .build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .build()

        binding.playerView.player = player

        player.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(state: Int) {

                when (state) {

                    Player.STATE_BUFFERING -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    Player.STATE_READY -> {
                        retryCount = 0
                        binding.progressBar.visibility = View.GONE
                    }

                    Player.STATE_ENDED -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {

                binding.progressBar.visibility = View.GONE

                if (retryCount < maxRetry) {

                    retryCount++

                    binding.playerView.postDelayed({

                        if (!isFinishing && !isDestroyed) {

                            player.prepare()
                            player.play()

                        }

                    }, retryCount * 1000L)

                } else {

                    Toast.makeText(
                        this@PlayerActivity,
                        "Unable to play this channel.\nPlease check your internet connection or try again.",
                        Toast.LENGTH_LONG
                    ).show()

                }
            }
        })

        player.setMediaItem(MediaItem.fromUri(url))

        player.prepare()
        player.play()
    }

    override fun onStart() {
        super.onStart()
        binding.playerView.onResume()
    }

    override fun onStop() {
        player.pause()
        binding.playerView.onPause()
        super.onStop()
    }

    override fun onDestroy() {

        player.stop()
        player.clearMediaItems()
        player.release()

        super.onDestroy()
    }
}
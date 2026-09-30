package com.example.streambox

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var errorPanel: LinearLayout
    private lateinit var errorText: TextView
    private var currentUrl = ""
    private var titleText = "Video"
    private var position = 0L
    private var playWhenReady = true
    private var speed = 1f
    private var maxHeight = Int.MAX_VALUE
    private val resume by lazy { getSharedPreferences("playback", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentUrl = intent.getStringExtra("url").orEmpty()
        titleText = intent.getStringExtra("title") ?: "Video"
        if (!MediaSourcePolicy.isPlayable(currentUrl)) {
            Toast.makeText(this, "A direct video stream is required.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        position = savedInstanceState?.getLong("position") ?: resume.getLong(currentUrl, 0L)
        playWhenReady = savedInstanceState?.getBoolean("playWhenReady") ?: true
        speed = savedInstanceState?.getFloat("speed") ?: 1f
        maxHeight = savedInstanceState?.getInt("maxHeight") ?: Int.MAX_VALUE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(button("Back") { finish() })
        top.addView(TextView(this).apply {
            text = titleText; textSize = 16f; setTextColor(Color.WHITE)
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(button("Options") { showOptions() })
        root.addView(top)
        val videoFrame = FrameLayout(this)
        playerView = PlayerView(this).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }
        videoFrame.addView(playerView, FrameLayout.LayoutParams(-1, -1))
        errorPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(12), dp(24), dp(12))
            setBackgroundColor(Color.argb(235, 0, 0, 0))
            visibility = View.GONE
        }
        errorText = TextView(this).apply { setTextColor(Color.WHITE); textSize = 16f; gravity = Gravity.CENTER }
        errorPanel.addView(errorText)
        errorPanel.addView(button("Retry") {
            errorPanel.visibility = View.GONE
            player?.prepare()
            player?.play()
        })
        if (intent.getStringExtra("mediaType") !in setOf("music", "video")) {
            errorPanel.addView(button("Watch options") {
                startActivity(WatchOptionsActivity.createIntent(this, titleText,
                    intent.getIntExtra("tmdbId", 0), intent.getStringExtra("mediaType") ?: "movie",
                    intent.getIntExtra("year", 0)))
            })
        }
        videoFrame.addView(errorPanel, FrameLayout.LayoutParams(-1, -1))
        root.addView(videoFrame, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        if (!::playerView.isInitialized || player != null) return
        player = ExoPlayer.Builder(this).build().also { active ->
            playerView.player = active
            active.setAudioAttributes(AudioAttributes.DEFAULT, true)
            active.setHandleAudioBecomingNoisy(true)
            active.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    errorText.text = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                            "Connection failed. Check your internet, then tap Retry."
                        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                            "This video is unavailable or its link has expired. Try again later."
                        else -> "This video could not play. Tap Retry or choose another film."
                    }
                    errorPanel.visibility = View.VISIBLE
                    playerView.keepScreenOn = false
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) { playerView.keepScreenOn = isPlaying }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) errorPanel.visibility = View.GONE
                    if (playbackState == Player.STATE_ENDED) resume.edit().remove(currentUrl).apply()
                }
            })
            errorPanel.visibility = View.GONE
            active.setMediaItem(MediaItem.fromUri(currentUrl))
            active.seekTo(position)
            active.playbackParameters = PlaybackParameters(speed)
            active.trackSelectionParameters = active.trackSelectionParameters.buildUpon()
                .setMaxVideoSize(Int.MAX_VALUE, maxHeight).build()
            active.playWhenReady = playWhenReady
            active.prepare()
        }
    }

    private fun showOptions() {
        val options = mutableListOf<Pair<String, () -> Unit>>(
            "Playback speed" to { chooseSpeed() },
            "Video quality" to { chooseQuality() },
            "Details & credits" to {
                AlertDialog.Builder(this).setTitle(titleText)
                    .setMessage(intent.getStringExtra("description").orEmpty().ifBlank { titleText })
                    .setPositiveButton("OK", null).show()
                Unit
            }
        )
        if (MediaSourcePolicy.isDownloadable(currentUrl)) options += "Download" to {
            MovieDownloads.start(this, titleText, currentUrl)
        }
        AlertDialog.Builder(this).setTitle(titleText)
            .setItems(options.map { it.first }.toTypedArray()) { _, which -> options[which].second() }.show()
    }

    private fun chooseSpeed() {
        val values = floatArrayOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
        AlertDialog.Builder(this).setTitle("Playback speed")
            .setItems(values.map { "${it}x" }.toTypedArray()) { _, which ->
                speed = values[which]
                player?.playbackParameters = PlaybackParameters(speed)
            }.show()
    }

    private fun chooseQuality() {
        val active = player ?: return
        val heights = active.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }.flatMap { group ->
            (0 until group.length).filter { group.isTrackSupported(it) }.map { group.getTrackFormat(it).height }
        }.filter { it > 0 }.distinct().sorted()
        if (heights.size <= 1) {
            Toast.makeText(this, "This stream has one video quality.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this).setTitle("Video quality limit")
            .setItems((listOf("Auto") + heights.map { "Up to ${it}p" }).toTypedArray()) { _, which ->
                maxHeight = if (which == 0) Int.MAX_VALUE else heights[which - 1]
                active.trackSelectionParameters = active.trackSelectionParameters.buildUpon()
                    .setMaxVideoSize(Int.MAX_VALUE, maxHeight).build()
            }.show()
    }

    private fun capturePosition() {
        player?.let { active ->
            position = if (active.playbackState == Player.STATE_ENDED) 0L else active.currentPosition.coerceAtLeast(0)
            playWhenReady = active.playWhenReady
            resume.edit().putLong(currentUrl, position).apply()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        capturePosition()
        outState.putLong("position", position)
        outState.putBoolean("playWhenReady", playWhenReady)
        outState.putFloat("speed", speed)
        outState.putInt("maxHeight", maxHeight)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        capturePosition()
        if (::playerView.isInitialized) {
            playerView.player = null
            playerView.keepScreenOn = false
        }
        player?.release()
        player = null
        super.onStop()
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; setOnClickListener { action() }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

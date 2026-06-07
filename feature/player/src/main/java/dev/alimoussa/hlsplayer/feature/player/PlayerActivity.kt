package dev.alimoussa.hlsplayer.feature.player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dev.alimoussa.hlsplayer.feature.player.service.PlayerAdOverlay
import dev.alimoussa.hlsplayer.feature.player.service.PlayerService


@SuppressLint("UnsafeOptInUsageError")
class PlayerActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null

    /** The connected controller, exposed to Compose; null until the session connects. */
    private val playerState = mutableStateOf<Player?>(null)

    /** The View IMA draws its ad chrome into, shared with the in-process service. */
    private val adOverlay by lazy { FrameLayout(this) }

    private var subscribed = false

    private var isFreshStart = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        // Don't let the screen dim while watching.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        subscribed = intent.getBooleanExtra(EXTRA_SUBSCRIBED, false)
        PlayerAdOverlay.setView(adOverlay)

        setContent {
            PlayerTheme {
                PlayerScreen(
                    player = playerState.value,
                    adOverlay = adOverlay,
                    subscribed = subscribed,
                    onBack = { finish() },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val future = obtainControllerFuture()
        future.addListener({
            val controller = future.get()
            playerState.value = controller
            if (isFreshStart) {
                // Fresh launch: (re)load the subscription-aware media and play, replacing whatever
                // stale item the reused service player may still be holding.
                isFreshStart = false
                controller.setMediaItem(PlayerService.requestItem(subscribed))
                controller.prepare()
                controller.play()
            } else {
                // Returning from the background: just resume the existing item.
                controller.play()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        // Pause when backgrounded; we don't support background playback.
        (controllerFuture?.takeIf { it.isDone }?.get())?.pause()
        super.onStop()
    }

    override fun onDestroy() {
        // Stop playback and release this Activity's controller (no background playback). The
        // service cleans itself up when the app's task is removed.
        (controllerFuture?.takeIf { it.isDone }?.get())?.stop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        playerState.value = null
        PlayerAdOverlay.setView(null)
        super.onDestroy()
    }

    private fun obtainControllerFuture(): ListenableFuture<MediaController> {
        controllerFuture?.let { return it }
        val token = SessionToken(
            applicationContext,
            ComponentName(applicationContext, PlayerService::class.java)
        )
        return MediaController.Builder(applicationContext, token).buildAsync()
            .also { controllerFuture = it }
    }

    companion object {
        /** Boolean extra: true when the user has an active subscription (ad-free playback). */
        const val EXTRA_SUBSCRIBED = "extra_subscribed"

        /** Builds the intent used to open the player for a given subscription state. */
        fun newIntent(context: Context, subscribed: Boolean): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_SUBSCRIBED, subscribed)
    }
}
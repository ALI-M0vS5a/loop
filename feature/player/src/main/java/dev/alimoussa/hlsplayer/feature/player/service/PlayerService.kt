package dev.alimoussa.hlsplayer.feature.player.service

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

// Media URLs come straight from the task spec.
private const val HLS_URL =
    "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
private const val AD_TAG_URI =
    "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples" +
            "&sz=640x480&cust_params=sample_ar%3Dpremidpost&ciu_szs=300x250&gdfp_req=1&ad_rule=1" +
            "&output=vmap&unviewed_position_start=1&env=vp&cmsid=496&vid=short_onecue&correlator="

/** Media id the UI uses to signal subscription state across the session boundary. */
private const val MEDIA_ID_SUBSCRIBED = "hls_subscribed"
private const val MEDIA_ID_ADS = "hls_ads"

@UnstableApi
class PlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var adsLoader: ImaAdsLoader? = null

    override fun onCreate() {
        super.onCreate()

        // IMA draws its ad UI into the Activity's overlay, shared (weakly) via PlayerAdOverlay.
        val imaAdsLoader = ImaAdsLoader.Builder(applicationContext).build()
        adsLoader = imaAdsLoader

        val mediaSourceFactory = DefaultMediaSourceFactory(applicationContext)
            .setLocalAdInsertionComponents({ imaAdsLoader }, PlayerAdOverlay)

        val player = ExoPlayer.Builder(applicationContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { imaAdsLoader.setPlayer(it) }

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaSessionCallback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            adsLoader?.setPlayer(null)
            player.release()
            release()
        }
        adsLoader?.release()
        adsLoader = null
        mediaSession = null
        super.onDestroy()
    }

    /** Rebuilds the real media item (URI + ad tag) from the subscription-encoding media id. */
    private object MediaSessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            val resolved =
                mediaItems.map { item -> buildMediaItem(subscribed = item.mediaId == MEDIA_ID_SUBSCRIBED) }
            return Futures.immediateFuture(resolved)
        }
    }

    companion object {
        fun requestItem(subscribed: Boolean): MediaItem =
            MediaItem.Builder()
                .setMediaId(if (subscribed) MEDIA_ID_SUBSCRIBED else MEDIA_ID_ADS)
                .build()
    }
}

/** The full HLS media item, with the VMAP ad tag attached only for non-subscribers. */
private fun buildMediaItem(subscribed: Boolean): MediaItem = if (subscribed) {
    MediaItem.Builder().setMediaId(MEDIA_ID_SUBSCRIBED).setUri(HLS_URL).build()
} else {
    MediaItem.Builder()
        .setMediaId(MEDIA_ID_ADS)
        .setUri(HLS_URL)
        .setAdsConfiguration(
            MediaItem.AdsConfiguration.Builder(AD_TAG_URI.toUri()).build(),
        )
        .build()
}

package dev.alimoussa.hlsplayer.feature.player.service

import android.view.ViewGroup
import androidx.media3.common.AdOverlayInfo
import androidx.media3.common.AdViewProvider
import java.lang.ref.WeakReference

internal object PlayerAdOverlay : AdViewProvider {

    @Volatile
    private var overlayRef: WeakReference<ViewGroup>? = null

    /** Publishes (or clears, with null) the Activity's ad-overlay View. */
    fun setView(view: ViewGroup?) {
        overlayRef = view?.let(::WeakReference)
    }

    override fun getAdViewGroup(): ViewGroup = checkNotNull(overlayRef?.get()) {
        "PlayerAdOverlay.getAdViewGroup() called with no live overlay registered"
    }

    override fun getAdOverlayInfos(): List<AdOverlayInfo> = emptyList()
}
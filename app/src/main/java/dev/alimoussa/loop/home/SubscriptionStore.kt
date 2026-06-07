package dev.alimoussa.loop.home

import android.content.Context
import androidx.core.content.edit

/**
 * Persists the user's subscription choice in [android.content.SharedPreferences], exactly as the
 * spec requires: the chosen state survives leaving and reopening the app.
 *
 *  - subscribed = true  -> ad-free playback
 *  - subscribed = false -> Google IMA pre-/mid-/post-roll ads
 */
class SubscriptionStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The persisted subscription flag. Defaults to "not subscribed" on first launch. */
    var isSubscribed: Boolean
        get() = prefs.getBoolean(KEY_SUBSCRIBED, false)
        set(value) = prefs.edit { putBoolean(KEY_SUBSCRIBED, value) }

    private companion object {
        const val PREFS_NAME = "hls_player_prefs"
        const val KEY_SUBSCRIBED = "subscribed"
    }
}
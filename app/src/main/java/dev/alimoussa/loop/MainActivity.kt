package dev.alimoussa.loop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.alimoussa.hlsplayer.feature.player.PlayerActivity
import dev.alimoussa.loop.home.HomeScreen
import dev.alimoussa.loop.home.SubscriptionStore
import dev.alimoussa.loop.ui.theme.LoopTheme

/**
 * Single-activity host for the Home screen. The subscription state lives in
 * [SubscriptionStore] (SharedPreferences) so it survives app restarts; tapping any carousel tile
 * launches [PlayerActivity] with that state, which decides whether to show ads.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val store = SubscriptionStore(this)

        setContent {
            LoopTheme {
                val context = LocalContext.current
                // Seed UI state from the persisted value; writes go back to SharedPreferences.
                var subscribed by remember { mutableStateOf(store.isSubscribed) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        subscribed = subscribed,
                        onSubscribedChange = { value ->
                            subscribed = value
                            store.isSubscribed = value
                        },
                        onOpenPlayer = {
                            context.startActivity(
                                PlayerActivity.newIntent(context, subscribed = subscribed),
                            )
                        },
                        // Inset the content below the status bar / above the nav bar.
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
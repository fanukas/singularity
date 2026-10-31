package dev.fanukas.singularity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var holder: GameHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        holder = GameHolder(applicationContext)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App(holder)
                }
            }
            // Live time: one engine tick per second; the save is written every 15 seconds.
            LaunchedEffect(Unit) {
                var n = 0
                while (true) {
                    delay(1000)
                    // Only while on screen: time away is applied once by catchUp() on resume.
                    if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) continue
                    holder.tick(1.0)
                    if (++n % 15 == 0) holder.save()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        holder.catchUp()
    }

    override fun onPause() {
        holder.save()
        super.onPause()
    }
}

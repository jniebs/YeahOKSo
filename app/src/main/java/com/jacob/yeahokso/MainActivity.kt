package com.jacob.yeahokso

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.random.Random

enum class Voice(val label: String, val prefix: String) {
    Male("Male", "male"),
    Female("Female", "female")
}

/**
 * Speaks with the phone's built-in text-to-speech.
 * Picks an installed English voice that reports the requested gender when the engine
 * provides that info; otherwise falls back to the default voice with a pitch shift.
 */
class VoicePlayer(context: Context) {
    private var ready = false
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ready = true
            } else {
                Toast.makeText(appContext, "Text-to-speech isn't available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun findVoice(voice: Voice): android.speech.tts.Voice? {
        val all = tts?.voices ?: return null
        return all.firstOrNull { v ->
            v.locale.language == "en" && !v.isNetworkConnectionRequired &&
                (v.features.any { it.equals(voice.prefix, ignoreCase = true) } ||
                    v.name.contains(voice.prefix, ignoreCase = true))
        }
    }

    fun speak(voice: Voice, text: String) {
        val engine = tts ?: return
        if (!ready) return
        val match = findVoice(voice)
        if (match != null) {
            engine.voice = match
            engine.setPitch(1.0f)
        } else {
            engine.language = Locale.US
            engine.setPitch(if (voice == Voice.Male) PITCH_MALE else PITCH_FEMALE)
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "yeahokso")
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private companion object {
        const val PITCH_MALE = 0.75f
        const val PITCH_FEMALE = 1.2f
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { YeahOKSoApp() }
    }
}

@Composable
fun YeahOKSoApp() {
    val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            HomeScreen()
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val player = remember { VoicePlayer(context) }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var voice by remember { mutableStateOf(Voice.Male) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        VoiceToggle(selected = voice, onSelect = { voice = it })

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "YeahOKSo",
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { player.speak(voice, "So") },
                modifier = Modifier.fillMaxWidth().height(72.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) { Text("So", fontSize = 20.sp, fontWeight = FontWeight.Medium) }

            OutlinedButton(
                onClick = {
                    val word = if (Random.nextBoolean()) "Yeah" else "OK"
                    player.speak(voice, word)
                },
                modifier = Modifier.fillMaxWidth().height(72.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) { Text("Yeah or OK", fontSize = 20.sp, fontWeight = FontWeight.Medium) }

            Button(
                onClick = { player.speak(voice, "Yeah OK So") },
                modifier = Modifier.fillMaxWidth().height(72.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) { Text("Yeah OK So", fontSize = 20.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
fun VoiceToggle(selected: Voice, onSelect: (Voice) -> Unit) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val thumb = MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(track)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Voice.values().forEach { v ->
            val active = v == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) thumb else track)
                    .clickable { onSelect(v) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = v.label,
                    fontSize = 16.sp,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

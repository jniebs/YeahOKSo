package com.jacob.yeahokso

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class Voice(val label: String, val key: String) {
    Male("Male", "voice_male"),
    Female("Female", "voice_female")
}

/**
 * Speaks with the phone's built-in text-to-speech.
 * Uses the voice the user picked for each toggle position. If none is picked (or it was
 * uninstalled), falls back to the default US voice with a pitch shift.
 */
class VoicePlayer(context: Context) {
    var ready by mutableStateOf(false)
        private set

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("yeahokso", Context.MODE_PRIVATE)
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

    /** Installed English voices that work offline, sorted by country then name. */
    fun englishVoices(): List<android.speech.tts.Voice> {
        val all = try { tts?.voices } catch (e: Exception) { null } ?: return emptyList()
        return all
            .filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
            .sortedWith(compareBy({ it.locale.country }, { it.name }))
    }

    fun chosenName(voice: Voice): String? = prefs.getString(voice.key, null)

    fun choose(voice: Voice, name: String) {
        prefs.edit().putString(voice.key, name).apply()
    }

    fun preview(match: android.speech.tts.Voice, text: String) {
        val engine = tts ?: return
        if (!ready) return
        engine.voice = match
        engine.setPitch(1.0f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "yeahokso")
    }

    fun speak(voice: Voice, text: String) {
        val engine = tts ?: return
        if (!ready) return
        val picked = chosenName(voice)?.let { name -> englishVoices().firstOrNull { it.name == name } }
        if (picked != null) {
            engine.voice = picked
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
        const val PITCH_MALE = 0.6f
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
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                VoiceToggle(selected = voice, onSelect = { voice = it })
            }
            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { showPicker = true },
                contentAlignment = Alignment.Center
            ) {
                GearIcon(color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
            }
        }

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

    if (showPicker) {
        VoicePicker(player = player, voice = voice, onDismiss = { showPicker = false })
    }
}

@Composable
fun GearIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2f
        drawCircle(color, radius = r * 0.45f, center = c, style = Stroke(width = r * 0.18f))
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            drawLine(
                color = color,
                start = Offset(c.x + dx * r * 0.62f, c.y + dy * r * 0.62f),
                end = Offset(c.x + dx * r * 0.95f, c.y + dy * r * 0.95f),
                strokeWidth = r * 0.26f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun voiceLabels(list: List<android.speech.tts.Voice>): Map<String, String> {
    val counts = mutableMapOf<String, Int>()
    return list.associate { v ->
        val country = if (v.locale.country == "GB") "UK" else v.locale.country
        val n = (counts[country] ?: 0) + 1
        counts[country] = n
        v.name to listOf(country, "English", n.toString()).filter { it.isNotEmpty() }.joinToString(" ")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicePicker(player: VoicePlayer, voice: Voice, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = remember(player.ready) { player.englishVoices() }
    val labels = remember(options) { voiceLabels(options) }
    var chosen by remember(voice) { mutableStateOf(player.chosenName(voice)) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
        ) {
            Text("${voice.label} voice", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(
                "Tap one to hear it and select it",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (options.isEmpty()) {
                Text(
                    "No English voices found. Install voice data in your phone's text-to-speech settings.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(options) { v ->
                        val selected = v.name == chosen
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    player.choose(voice, v.name)
                                    chosen = v.name
                                    player.preview(v, "Yeah OK So")
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(8.dp))
                            Text(
                                text = labels[v.name] ?: v.name,
                                fontSize = 15.sp,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
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

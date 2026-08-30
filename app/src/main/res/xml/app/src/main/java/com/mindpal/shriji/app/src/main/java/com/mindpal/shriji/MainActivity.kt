package com.mindpal.shriji

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request Permissions
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        )
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 101)
        }

        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        setContent {
            ShrijiHomeScreen(
                onSpeak = { text -> speakOut(text) },
                onStartListening = { onResult -> startListening(onResult) },
                onLaunchApp = { appName -> launchAnyApp(appName) }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN")
            tts?.setPitch(1.15f)
            tts?.setSpeechRate(1.0f)
        }
    }

    private fun speakOut(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "shriji_voice")
    }

    private fun startListening(onResult: (String) -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { onResult(it) }
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { onResult("Maine suna nahi, kripya fir se boliye.") }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    private fun launchAnyApp(appName: String): String {
        val pm = packageManager
        val clean = appName.lowercase().replace("kholo", "").replace("open", "").trim()
        val apps = pm.getInstalledApplications(0)
        for (app in apps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(clean) || clean.contains(label)) {
                val launch = pm.getLaunchIntentForPackage(app.packageName)
                if (launch != null) {
                    startActivity(launch)
                    return "$label open kar rahi hoon!"
                }
            }
        }
        return "App nahi mila."
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}

@Composable
fun ShrijiHomeScreen(
    onSpeak: (String) -> Unit,
    onStartListening: ((String) -> Unit) -> Unit,
    onLaunchApp: (String) -> String
) {
    var responseText by remember { mutableStateOf("Namaste! Main Shriji hoon. Main aapki kya madad karoon? 😄") }
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    val bgGradient = Brush.verticalGradient(listOf(Color(0xFF090414), Color(0xFF130926), Color(0xFF05020A)))

    Box(modifier = Modifier.fillMaxSize().background(bgGradient).padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                Text("Shri Ji AI", color = Color.White, fontSize = 26.sp)
                Text("Your Personal AI Companion", color = Color(0xFFB388FF), fontSize = 13.sp)
            }

            // Animated Glowing Avatar Centerpiece
            ShrijiAvatarUI(isListening = isListening)

            // Response Box
            Surface(
                color = Color(0x3325124A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Text(
                    text = responseText,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Mic and Text Controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        isListening = true
                        onStartListening { spoken ->
                            isListening = false
                            if (spoken.contains("kholo") || spoken.contains("open")) {
                                val reply = onLaunchApp(spoken)
                                responseText = reply
                                onSpeak(reply)
                            } else {
                                val reply = "Achha ji! 😄 Aapne kaha: $spoken"
                                responseText = reply
                                onSpeak(reply)
                            }
                        }
                    },
                    modifier = Modifier.size(64.dp).background(Color(0xFFFF4081), CircleShape)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Mic", tint = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask Shriji anything...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1F1138),
                            unfocusedContainerColor = Color(0xFF1F1138),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                if (inputText.contains("kholo") || inputText.contains("open")) {
                                    val res = onLaunchApp(inputText)
                                    responseText = res
                                    onSpeak(res)
                                } else {
                                    val res = "Haan haan, samajh gayi! 😄"
                                    responseText = res
                                    onSpeak(res)
                                }
                                inputText = ""
                            }
                        },
                        modifier = Modifier.size(48.dp).background(Color(0xFF7C4DFF), CircleShape)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ShrijiAvatarUI(isListening: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2.6f

            // Glowing Outer Ring
            drawCircle(
                color = if (isListening) Color(0xFF00E676) else Color(0xFFFF4081),
                radius = radius * if (isListening) pulse else 1f,
                style = Stroke(width = 4.dp.toPx())
            )

            // Inner Face Silhouette
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFF4A148C), Color(0xFF1A0033))),
                radius = radius * 0.8f,
                center = center
            )
        }
        Text("🤖 Shri Ji", color = Color(0xFFFF80AB), fontSize = 18.sp)
    }
}

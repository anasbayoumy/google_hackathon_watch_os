package com.example.ai_comp_wearableos.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.*
import com.example.ai_comp_wearableos.presentation.theme.Ai_comp_wearableosTheme
import com.example.ai_comp_wearableos.services.PhoneCommunicationService
import com.example.ai_comp_wearableos.services.WearDataListenerService
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class MainActivity : ComponentActivity() {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private lateinit var phoneCommunicationService: PhoneCommunicationService

    // Broadcast receiver for handling responses from WearDataListenerService
    private val dataListenerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WearDataListenerService.ACTION_SMS_RECEIVED -> {
                    val smsText = intent.getStringExtra(WearDataListenerService.EXTRA_SMS_TEXT) ?: ""
                    Log.d("WearApp", "📱 [BROADCAST] SMS received: $smsText")
                    handleSmsReceived(smsText)
                }
                WearDataListenerService.ACTION_CONNECTION_STATUS -> {
                    val isConnected = intent.getBooleanExtra(WearDataListenerService.EXTRA_IS_CONNECTED, false)
                    Log.d("WearApp", "📱 [BROADCAST] Connection status: $isConnected")
                    handleConnectionStatus(isConnected)
                }
                WearDataListenerService.ACTION_SMS_SENT -> {
                    val success = intent.getBooleanExtra(WearDataListenerService.EXTRA_SMS_SUCCESS, false)
                    Log.d("WearApp", "📱 [BROADCAST] SMS sent: $success")
                    handleSmsSent(success)
                }
                WearDataListenerService.ACTION_ERROR -> {
                    val error = intent.getStringExtra(WearDataListenerService.EXTRA_ERROR_MESSAGE) ?: "Unknown error"
                    Log.e("WearApp", "📱 [BROADCAST] Error: $error")
                    handleError(error)
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("WearApp", "Audio permission granted")
        } else {
            Log.d("WearApp", "Audio permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Initialize phone communication service
        phoneCommunicationService = PhoneCommunicationService(this)

        // Register broadcast receiver for WearDataListenerService responses
        val intentFilter = IntentFilter().apply {
            addAction(WearDataListenerService.ACTION_SMS_RECEIVED)
            addAction(WearDataListenerService.ACTION_CONNECTION_STATUS)
            addAction(WearDataListenerService.ACTION_SMS_SENT)
            addAction(WearDataListenerService.ACTION_ERROR)
        }
        registerReceiver(dataListenerReceiver, intentFilter)

        // Request audio permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            EmergencyWearApp(
                onStartRecording = { startRecording() },
                onStopRecording = { stopRecording() },
                phoneCommunicationService = phoneCommunicationService
            )
        }
    }

    override fun onResume() {
        super.onResume()
        phoneCommunicationService.startListening()
    }

    override fun onPause() {
        super.onPause()
        phoneCommunicationService.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(dataListenerReceiver)
        } catch (e: Exception) {
            Log.w("WearApp", "Error unregistering receiver: ${e.message}")
        }
    }

    // Handler methods for broadcast receiver
    private fun handleSmsReceived(smsText: String) {
        Log.d("WearApp", "📱 [HANDLER] SMS received: $smsText")
        // Delete the audio file after successful processing
        audioFile?.let { file ->
            deleteAudioFile(file)
            audioFile = null
        }
        // Update UI state - this will be handled by the Compose state management
    }

    private fun handleConnectionStatus(isConnected: Boolean) {
        Log.d("WearApp", "📱 [HANDLER] Connection status: $isConnected")
        // Update connection status in UI if needed
    }

    private fun handleSmsSent(success: Boolean) {
        Log.d("WearApp", "📱 [HANDLER] SMS sent: $success")
        // Update UI to show SMS send result
    }

    private fun handleError(error: String) {
        Log.e("WearApp", "📱 [HANDLER] Error: $error")
        // Update UI to show error
        audioFile?.let { file ->
            deleteAudioFile(file)
            audioFile = null
        }
    }

    private fun startRecording() {
        try {
            // Create audio file
            audioFile = File(externalCacheDir, "emergency_voice_${System.currentTimeMillis()}.3gp")

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(audioFile?.absolutePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

                prepare()
                start()
            }
            Log.d("WearApp", "Recording started: ${audioFile?.absolutePath}")
        } catch (e: IOException) {
            Log.e("WearApp", "Recording failed: ${e.message}")
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Log.d("WearApp", "Recording stopped: ${audioFile?.absolutePath}")

            // Send audio to phone app for processing
            audioFile?.let { file ->
                processAudioWithPhone(file)
            }

        } catch (e: Exception) {
            Log.e("WearApp", "Stop recording failed: ${e.message}")
        }
    }

    private fun processAudioWithPhone(audioFile: File) {
        Log.d("WearApp", "🎤 [WATCH] processAudioWithPhone called with file: ${audioFile.absolutePath}")
        Log.d("WearApp", "🎤 [WATCH] File exists: ${audioFile.exists()}")
        Log.d("WearApp", "🎤 [WATCH] File size: ${audioFile.length()} bytes")

        // Send audio to phone via the communication service
        lifecycleScope.launch {
            Log.d("WearApp", "🎤 [WATCH] Starting coroutine to send audio to phone...")
            val success = phoneCommunicationService.sendVoiceToPhone(audioFile)
            Log.d("WearApp", "🎤 [WATCH] Audio send result: $success")

            if (!success) {
                Log.e("WearApp", "❌ [WATCH] Failed to send audio to phone")
                // Delete audio file on failure
                deleteAudioFile(audioFile)
            }
        }
    }

    private fun deleteAudioFile(audioFile: File) {
        try {
            Log.d("WearApp", "🗑️ [WATCH] Deleting audio file: ${audioFile.absolutePath}")
            if (audioFile.exists()) {
                val deleted = audioFile.delete()
                Log.d("WearApp", "🗑️ [WATCH] Audio file deleted: $deleted")
            } else {
                Log.d("WearApp", "🗑️ [WATCH] Audio file doesn't exist, nothing to delete")
            }
        } catch (e: Exception) {
            Log.e("WearApp", "❌ [WATCH] Error deleting audio file: ${e.message}")
        }
    }
}

@Composable
fun EmergencyWearApp(
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    phoneCommunicationService: PhoneCommunicationService
) {
    var isRecording by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var smsResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPhoneConnected by remember { mutableStateOf(false) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }
    var isSendingSms by remember { mutableStateOf(false) }
    var smsStatus by remember { mutableStateOf<String?>(null) }

    // Set up communication callbacks
    LaunchedEffect(phoneCommunicationService) {
        phoneCommunicationService.onSmsReceived = { sms ->
            Log.d("WearApp", "📱 [WATCH UI] SMS received: $sms")
            smsResult = sms
            isProcessing = false
            errorMessage = null
            // Delete audio file after successful SMS
            currentAudioFile?.let { file ->
                Log.d("WearApp", "🗑️ [WATCH UI] Deleting audio file after success")
                if (file.exists()) file.delete()
                currentAudioFile = null
            }
        }

        phoneCommunicationService.onConnectionStatusChanged = { connected ->
            Log.d("WearApp", "📶 [WATCH UI] Connection status changed: $connected")
            isPhoneConnected = connected
        }

        phoneCommunicationService.onError = { error ->
            Log.e("WearApp", "❌ [WATCH UI] Error received: $error")
            errorMessage = error
            isProcessing = false
            isSendingSms = false
            // Delete audio file after error
            currentAudioFile?.let { file ->
                Log.d("WearApp", "🗑️ [WATCH UI] Deleting audio file after error")
                if (file.exists()) file.delete()
                currentAudioFile = null
            }
        }

        phoneCommunicationService.onSmsSent = { success ->
            Log.d("WearApp", "📱 [WATCH UI] SMS send result: $success")
            isSendingSms = false
            if (success) {
                smsStatus = "SMS sent successfully!"
                errorMessage = null
            } else {
                smsStatus = null
                errorMessage = "Failed to send SMS"
            }
        }

        // Check initial connection
        phoneCommunicationService.checkPhoneConnection()
    }

    // Handle audio processing when recording stops
    LaunchedEffect(isProcessing, currentAudioFile) {
        if (isProcessing && currentAudioFile != null) {
            val success = phoneCommunicationService.sendVoiceToPhone(currentAudioFile!!)
            if (!success) {
                isProcessing = false
                errorMessage = "Failed to send voice to phone"
            }
        }
    }

    // Handle SMS sending when requested
    LaunchedEffect(isSendingSms, smsResult) {
        if (isSendingSms && smsResult != null) {
            val success = phoneCommunicationService.sendSmsViaPhone(smsResult!!)
            if (!success) {
                isSendingSms = false
                errorMessage = "Failed to send SMS request"
            }
        }
    }

    Ai_comp_wearableosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            // Animated transitions between screens
            AnimatedContent(
                targetState = when {
                    smsResult != null -> "result"
                    isProcessing -> "processing"
                    else -> "recording"
                },
                transitionSpec = {
                    when (targetState) {
                        "processing" -> {
                            slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(500, easing = EaseOut)
                            ) + fadeIn(animationSpec = tween(500)) togetherWith
                            slideOutVertically(
                                targetOffsetY = { -it },
                                animationSpec = tween(500, easing = EaseIn)
                            ) + fadeOut(animationSpec = tween(500))
                        }
                        "result" -> {
                            slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(600, easing = EaseOut)
                            ) + fadeIn(animationSpec = tween(600)) togetherWith
                            slideOutVertically(
                                targetOffsetY = { -it },
                                animationSpec = tween(600, easing = EaseIn)
                            ) + fadeOut(animationSpec = tween(600))
                        }
                        else -> {
                            slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = tween(500, easing = EaseOut)
                            ) + fadeIn(animationSpec = tween(500)) togetherWith
                            slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(500, easing = EaseIn)
                            ) + fadeOut(animationSpec = tween(500))
                        }
                    }
                },
                label = "screenTransition"
            ) { screen ->
                when (screen) {
                    "result" -> {
                        SmsResultScreen(
                            sms = smsResult!!,
                            isSendingSms = isSendingSms,
                            smsStatus = smsStatus,
                            onSendSms = { smsText: String ->
                                isSendingSms = true
                                smsStatus = null
                                errorMessage = null
                                // Send SMS via phone - will be handled in LaunchedEffect
                            },
                            onBack = {
                                smsResult = null
                                isProcessing = false
                                isRecording = false
                                isSendingSms = false
                                smsStatus = null
                            }
                        )
                    }
                    "processing" -> {
                        ProcessingScreen()
                    }
                    else -> {
                        MainRecordingScreen(
                            isRecording = isRecording,
                            isPhoneConnected = isPhoneConnected,
                            errorMessage = errorMessage,
                            onRecordingToggle = { audioFile ->
                                if (isRecording) {
                                    onStopRecording()
                                    isRecording = false
                                    currentAudioFile = audioFile
                                    isProcessing = true
                                    errorMessage = null
                                } else {
                                    onStartRecording()
                                    isRecording = true
                                    errorMessage = null
                                }
                            },
                            onRetryConnection = {
                                // This will be handled in the composable
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainRecordingScreen(
    isRecording: Boolean,
    isPhoneConnected: Boolean,
    errorMessage: String?,
    onRecordingToggle: (File?) -> Unit,
    onRetryConnection: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isRound = screenWidth == screenHeight // Approximate round screen detection

    // Animation values
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isRecording) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "buttonScale"
    )

    // Dynamic sizing based on screen
    val buttonSize = minOf(screenWidth * 0.4f, screenHeight * 0.35f, 100.dp)
    val iconSize = buttonSize * 0.4f
    val titleSize = if (screenWidth < 200.dp) 14.sp else 16.sp
    val bodySize = if (screenWidth < 200.dp) 10.sp else 12.sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colors.background,
                        MaterialTheme.colors.background.copy(alpha = 0.8f)
                    ),
                    radius = screenWidth.value * 0.8f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Emergency icon and title with connection status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Emergency,
                        contentDescription = "Emergency",
                        tint = Color(0xFFE53E3E),
                        modifier = Modifier.size((titleSize.value + 2).dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI Emergency",
                        fontSize = titleSize,
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Connection status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPhoneConnected) Color(0xFF48BB78) else Color(0xFFE53E3E)
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPhoneConnected) "Phone Connected" else "Phone Disconnected",
                        fontSize = (titleSize.value - 4f).sp,
                        color = if (isPhoneConnected) Color(0xFF48BB78) else Color(0xFFE53E3E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isRound) 12.dp else 16.dp))

            // Recording button with animations
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(buttonScale)
            ) {
                // Outer pulse ring for recording state
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(buttonSize + 20.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = Color.Red.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }

                // Main button
                CompactButton(
                    onClick = {
                        if (!isPhoneConnected && !isRecording) {
                            onRetryConnection()
                        } else {
                            onRecordingToggle(null) // Will be updated with actual file
                        }
                    },
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = when {
                            !isPhoneConnected && !isRecording -> Color(0xFF718096)
                            isRecording -> Color(0xFFE53E3E)
                            else -> Color(0xFF3182CE)
                        }
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Background gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = if (isRecording) {
                                            listOf(Color(0xFFFC8181), Color(0xFFE53E3E))
                                        } else {
                                            listOf(Color(0xFF63B3ED), Color(0xFF3182CE))
                                        },
                                        radius = buttonSize.value * 0.8f
                                    ),
                                    shape = CircleShape
                                )
                        )

                        // Icon
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                            tint = Color.White,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isRound) 8.dp else 12.dp))

            // Status text with animation and error handling
            AnimatedContent(
                targetState = when {
                    errorMessage != null -> "error"
                    !isPhoneConnected -> "disconnected"
                    isRecording -> "recording"
                    else -> "ready"
                },
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
                },
                label = "statusText"
            ) { state ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (state) {
                        "error" -> {
                            Text(
                                text = "Error",
                                fontSize = (bodySize.value + 1f).sp,
                                color = Color(0xFFE53E3E),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage ?: "Unknown error",
                                fontSize = (bodySize.value - 1f).sp,
                                color = Color(0xFFE53E3E).copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                        "disconnected" -> {
                            Text(
                                text = "Tap to Connect",
                                fontSize = (bodySize.value + 1f).sp,
                                color = Color(0xFF718096),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Phone connection required",
                                fontSize = (bodySize.value - 1f).sp,
                                color = Color(0xFF718096).copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                        "recording" -> {
                            Text(
                                text = "Recording...",
                                fontSize = (bodySize.value + 1f).sp,
                                color = Color(0xFFE53E3E),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Describe your emergency",
                                fontSize = (bodySize.value - 1f).sp,
                                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            Text(
                                text = "Tap to Record",
                                fontSize = (bodySize.value + 1f).sp,
                                color = MaterialTheme.colors.onBackground,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Voice emergency assistant",
                                fontSize = (bodySize.value - 1f).sp,
                                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessingScreen() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Animation for the processing dots
    val infiniteTransition = rememberInfiniteTransition(label = "processing")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    // Rotating animation for progress indicator
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val progressSize = minOf(screenWidth * 0.25f, screenHeight * 0.2f, 50.dp)
    val titleSize = if (screenWidth < 200.dp) 12.sp else 14.sp
    val bodySize = if (screenWidth < 200.dp) 9.sp else 11.sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A202C),
                        Color(0xFF2D3748)
                    ),
                    radius = screenWidth.value * 0.6f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // AI Brain icon with pulsing effect
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(progressSize + 20.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF3182CE).copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Progress indicator
                CircularProgressIndicator(
                    modifier = Modifier.size(progressSize),
                    strokeWidth = 3.dp
                )

                // Center AI icon
                Icon(
                    imageVector = Icons.Default.Emergency,
                    contentDescription = "Processing",
                    tint = Color(0xFF63B3ED),
                    modifier = Modifier.size(progressSize * 0.4f)
                )
            }

            // Main status text
            Text(
                text = "Analyzing Emergency",
                fontSize = titleSize,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Animated processing dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val delay = index * 200
                    val animatedAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = delay, easing = EaseInOut),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF63B3ED).copy(alpha = animatedAlpha))
                    )

                    if (index < 2) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle with gradient text effect
            Text(
                text = "AI is generating your emergency SMS",
                fontSize = bodySize,
                color = Color(0xFFA0AEC0),
                textAlign = TextAlign.Center,
                lineHeight = (bodySize.value + 2).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress steps indicator
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                listOf("Voice", "Analysis", "SMS").forEachIndexed { index, step ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index <= 1) Color(0xFF48BB78) else Color(0xFF4A5568)
                                )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = step,
                            fontSize = (bodySize.value - 2).sp,
                            color = if (index <= 1) Color(0xFF48BB78) else Color(0xFF718096)
                        )
                    }

                    if (index < 2) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(1.dp)
                                .background(Color(0xFF4A5568))
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SmsResultScreen(
    sms: String,
    isSendingSms: Boolean = false,
    smsStatus: String? = null,
    onSendSms: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Success animation
    val successScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "successScale"
    )

    val titleSize = if (screenWidth < 200.dp) 12.sp else 14.sp
    val bodySize = if (screenWidth < 200.dp) 9.sp else 11.sp
    val buttonHeight = if (screenHeight < 250.dp) 32.dp else 40.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F4C75),
                        Color(0xFF1B1B2F)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with success indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(successScale)
            ) {
                // Success checkmark
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF48BB78))
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Emergency SMS Ready",
                    fontSize = titleSize,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }

            // SMS content card
            Card(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF7FAFC),
                                    Color(0xFFEDF2F7)
                                )
                            )
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        // SMS header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = "Emergency",
                                tint = Color(0xFFE53E3E),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Emergency Alert",
                                fontSize = (bodySize.value - 1).sp,
                                color = Color(0xFF2D3748),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // SMS content
                        Text(
                            text = sms,
                            fontSize = bodySize,
                            color = Color(0xFF1A202C),
                            lineHeight = (bodySize.value + 2).sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Action buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Send SMS button (primary action)
                CompactButton(
                    onClick = {
                        if (!isSendingSms) {
                            onSendSms(sms)
                        }
                    },
                    enabled = !isSendingSms,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isSendingSms) Color(0xFF718096) else Color(0xFF48BB78)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSendingSms) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sending...",
                                color = Color.White,
                                fontSize = (bodySize.value + 1).sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "📱",
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Send Emergency SMS",
                                color = Color.White,
                                fontSize = (bodySize.value + 1).sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // SMS status display
                smsStatus?.let { status ->
                    Text(
                        text = status,
                        color = Color(0xFF48BB78),
                        fontSize = (bodySize.value - 1).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // New emergency button (secondary action)
                CompactButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight - 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF4A5568)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "New Emergency",
                        color = Color.White,
                        fontSize = bodySize,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}


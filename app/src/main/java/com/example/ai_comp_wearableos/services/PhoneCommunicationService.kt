package com.example.ai_comp_wearableos.services

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File

class PhoneCommunicationService(private val context: Context) {
    
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)
    
    companion object {
        private const val TAG = "PhoneCommunication"
        private const val EMERGENCY_VOICE_PATH = "/emergency_voice"
        private const val SMS_RESPONSE_PATH = "/sms_response"
        private const val REQUEST_SMS_PATH = "/request_sms"
        private const val CONNECTION_CHECK_PATH = "/connection_check"
        private const val SEND_SMS_PATH = "/send_sms"
    }
    
    // Callback for receiving SMS responses
    var onSmsReceived: ((String) -> Unit)? = null
    var onConnectionStatusChanged: ((Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onSmsSent: ((Boolean) -> Unit)? = null
    
    private val dataListener = DataClient.OnDataChangedListener { dataEvents ->
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                when (dataItem.uri.path) {
                    SMS_RESPONSE_PATH -> {
                        handleSmsResponse(dataItem)
                    }
                }
            }
        }
    }
    
    private val messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        when (messageEvent.path) {
            CONNECTION_CHECK_PATH -> {
                Log.d(TAG, "Connection check received from phone")
                onConnectionStatusChanged?.invoke(true)
            }
            SMS_RESPONSE_PATH -> {
                Log.d(TAG, "📱 SMS response received from phone")
                val smsText = String(messageEvent.data)
                Log.d(TAG, "📱 SMS content: $smsText")
                onSmsReceived?.invoke(smsText)
            }
            SEND_SMS_PATH -> {
                Log.d(TAG, "📱 SMS send result received from phone")
                val result = String(messageEvent.data)
                val success = result == "success"
                Log.d(TAG, "📱 SMS send result: $success")
                onSmsSent?.invoke(success)
            }
        }
    }
    
    fun startListening() {
        Log.d(TAG, "Starting to listen for phone communication")
        dataClient.addListener(dataListener)
        messageClient.addListener(messageListener)
    }
    
    fun stopListening() {
        Log.d(TAG, "Stopping phone communication listeners")
        dataClient.removeListener(dataListener)
        messageClient.removeListener(messageListener)
    }
    
    suspend fun sendVoiceToPhone(audioFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🎤 STEP 1: Starting voice transmission to phone")
                Log.d(TAG, "🎤 Audio file: ${audioFile.name}")
                Log.d(TAG, "🎤 Audio path: ${audioFile.absolutePath}")
                Log.d(TAG, "🎤 File exists: ${audioFile.exists()}")
                Log.d(TAG, "🎤 File size: ${audioFile.length()} bytes")

                // Check if phone is connected
                Log.d(TAG, "🎤 STEP 2: Checking phone connection...")
                val connectedNodes = nodeClient.connectedNodes.await()
                Log.d(TAG, "🎤 Connected nodes found: ${connectedNodes.size}")

                if (connectedNodes.isEmpty()) {
                    Log.e(TAG, "❌ STEP 2 FAILED: No connected phone found")
                    onError?.invoke("Phone not connected")
                    return@withContext false
                }

                connectedNodes.forEach { node ->
                    Log.d(TAG, "🎤 Connected node: ${node.displayName} (${node.id})")
                }

                // Create asset from audio file
                Log.d(TAG, "🎤 STEP 3: Creating audio asset...")
                val audioBytes = audioFile.readBytes()
                Log.d(TAG, "🎤 Audio bytes loaded: ${audioBytes.size}")
                val asset = Asset.createFromBytes(audioBytes)
                Log.d(TAG, "🎤 Asset created successfully")

                // Create data request
                Log.d(TAG, "🎤 STEP 4: Creating data request...")
                val putDataRequest = PutDataRequest.create(EMERGENCY_VOICE_PATH).apply {
                    val dataMap = DataMap().apply {
                        putAsset("audio_file", asset)
                        putLong("timestamp", System.currentTimeMillis())
                    }
                    setData(dataMap.toByteArray())
                    setUrgent() // High priority for emergency
                }
                Log.d(TAG, "🎤 Data request created for path: $EMERGENCY_VOICE_PATH")

                // Send to phone
                Log.d(TAG, "🎤 STEP 5: Sending data to phone...")
                val result = dataClient.putDataItem(putDataRequest).await()
                Log.d(TAG, "✅ STEP 6: Voice sent to phone successfully!")
                Log.d(TAG, "✅ Result URI: ${result.uri}")
                Log.d(TAG, "✅ Waiting for phone to process and respond...")

                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ FAILED: Exception in sendVoiceToPhone", e)
                Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
                Log.e(TAG, "❌ Exception message: ${e.message}")
                onError?.invoke("Failed to send voice: ${e.message}")
                false
            }
        }
    }
    
    suspend fun checkPhoneConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔍 CONNECTION CHECK: Starting phone connection check...")
                val connectedNodes = nodeClient.connectedNodes.await()
                val isConnected = connectedNodes.isNotEmpty()

                Log.d(TAG, "🔍 CONNECTION CHECK: Found ${connectedNodes.size} connected nodes")

                if (isConnected) {
                    connectedNodes.forEach { node ->
                        Log.d(TAG, "🔍 Connected node: ${node.displayName} (${node.id}) - ${node.isNearby}")
                    }

                    // Send ping to phone
                    for (node in connectedNodes) {
                        Log.d(TAG, "🔍 Sending ping to node: ${node.displayName}")
                        messageClient.sendMessage(
                            node.id,
                            CONNECTION_CHECK_PATH,
                            "ping".toByteArray()
                        ).await()
                    }
                    Log.d(TAG, "✅ CONNECTION CHECK: Phone connection verified - ${connectedNodes.size} nodes")
                } else {
                    Log.w(TAG, "❌ CONNECTION CHECK: No phone connection found")
                }

                Log.d(TAG, "🔍 CONNECTION CHECK: Notifying UI - connected: $isConnected")
                onConnectionStatusChanged?.invoke(isConnected)
                isConnected
            } catch (e: Exception) {
                Log.e(TAG, "❌ CONNECTION CHECK: Error checking phone connection", e)
                Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
                Log.e(TAG, "❌ Exception message: ${e.message}")
                onConnectionStatusChanged?.invoke(false)
                false
            }
        }
    }
    
    private fun handleSmsResponse(dataItem: DataItem) {
        try {
            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
            val smsText = dataMap.getString("sms_text")
            val success = dataMap.getBoolean("success", false)
            
            if (success && !smsText.isNullOrEmpty()) {
                Log.d(TAG, "SMS response received: $smsText")
                onSmsReceived?.invoke(smsText)
            } else {
                val error = dataMap.getString("error", "Unknown error")
                Log.e(TAG, "SMS generation failed: $error")
                onError?.invoke("SMS generation failed: $error")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse SMS response", e)
            onError?.invoke("Failed to parse response: ${e.message}")
        }
    }
    
    suspend fun requestEmergencySms(voiceText: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connectedNodes = nodeClient.connectedNodes.await()
                if (connectedNodes.isEmpty()) {
                    onError?.invoke("Phone not connected")
                    return@withContext false
                }
                
                // Send text-based request as fallback
                val putDataRequest = PutDataRequest.create(REQUEST_SMS_PATH).apply {
                    val dataMap = DataMap().apply {
                        putString("voice_text", voiceText)
                        putLong("timestamp", System.currentTimeMillis())
                    }
                    setData(dataMap.toByteArray())
                    setUrgent()
                }
                
                dataClient.putDataItem(putDataRequest).await()
                Log.d(TAG, "Emergency SMS request sent: $voiceText")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request emergency SMS", e)
                onError?.invoke("Failed to request SMS: ${e.message}")
                false
            }
        }
    }

    suspend fun sendSmsViaPhone(smsText: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📱 [WATCH] Requesting phone to send SMS: $smsText")
                val connectedNodes = nodeClient.connectedNodes.await()
                if (connectedNodes.isEmpty()) {
                    Log.e(TAG, "❌ [WATCH] No connected phone found for SMS sending")
                    onError?.invoke("Phone not connected")
                    return@withContext false
                }

                // Send SMS request to phone
                for (node in connectedNodes) {
                    Log.d(TAG, "📱 [WATCH] Sending SMS request to node: ${node.displayName}")
                    messageClient.sendMessage(
                        node.id,
                        SEND_SMS_PATH,
                        smsText.toByteArray()
                    ).await()
                }

                Log.d(TAG, "✅ [WATCH] SMS send request sent to phone")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ [WATCH] Failed to request SMS sending", e)
                onError?.invoke("Failed to send SMS request: ${e.message}")
                false
            }
        }
    }
}

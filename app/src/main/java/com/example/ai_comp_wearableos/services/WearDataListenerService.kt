package com.example.ai_comp_wearableos.services

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.*

/**
 * WearDataListenerService for the watch app
 * Receives SMS responses and other data from the phone app
 */
class WearDataListenerService : WearableListenerService() {
    
    companion object {
        private const val TAG = "WearDataListener"
        private const val SMS_RESPONSE_PATH = "/sms_response"
        private const val CONNECTION_CHECK_PATH = "/connection_check"
        private const val SEND_SMS_PATH = "/send_sms"
        
        // Broadcast actions for communicating with MainActivity
        const val ACTION_SMS_RECEIVED = "com.example.ai_comp_wearableos.SMS_RECEIVED"
        const val ACTION_CONNECTION_STATUS = "com.example.ai_comp_wearableos.CONNECTION_STATUS"
        const val ACTION_SMS_SENT = "com.example.ai_comp_wearableos.SMS_SENT"
        const val ACTION_ERROR = "com.example.ai_comp_wearableos.ERROR"
        
        const val EXTRA_SMS_TEXT = "sms_text"
        const val EXTRA_IS_CONNECTED = "is_connected"
        const val EXTRA_SMS_SUCCESS = "sms_success"
        const val EXTRA_ERROR_MESSAGE = "error_message"
    }
    
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        
        Log.d(TAG, "⌚ WATCH: Data changed event received from phone")
        Log.d(TAG, "⌚ Number of data events: ${dataEvents.count}")
        
        for (event in dataEvents) {
            Log.d(TAG, "⌚ Processing data event type: ${event.type}")
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                Log.d(TAG, "⌚ Data item URI: ${dataItem.uri}")
                Log.d(TAG, "⌚ Data item path: ${dataItem.uri.path}")
                
                when (dataItem.uri.path) {
                    SMS_RESPONSE_PATH -> {
                        Log.d(TAG, "✅ WATCH: SMS response received from phone!")
                        handleSmsResponse(dataItem)
                    }
                    else -> {
                        Log.w(TAG, "⚠️ WATCH: Unknown data path: ${dataItem.uri.path}")
                    }
                }
            }
        }
    }
    
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        
        Log.d(TAG, "⌚ WATCH: Message received from phone")
        Log.d(TAG, "⌚ Message path: ${messageEvent.path}")
        
        when (messageEvent.path) {
            CONNECTION_CHECK_PATH -> {
                Log.d(TAG, "✅ WATCH: Connection check received from phone")
                broadcastConnectionStatus(true)
            }
            SMS_RESPONSE_PATH -> {
                Log.d(TAG, "✅ WATCH: SMS response message received")
                val smsText = String(messageEvent.data)
                Log.d(TAG, "✅ WATCH: SMS content: $smsText")
                broadcastSmsReceived(smsText)
            }
            SEND_SMS_PATH -> {
                Log.d(TAG, "✅ WATCH: SMS send result received")
                val result = String(messageEvent.data)
                val success = result == "success"
                Log.d(TAG, "✅ WATCH: SMS send success: $success")
                broadcastSmsSent(success)
            }
            else -> {
                Log.w(TAG, "⚠️ WATCH: Unknown message path: ${messageEvent.path}")
            }
        }
    }
    
    private fun handleSmsResponse(dataItem: DataItem) {
        try {
            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
            
            // Check if it's an error response
            if (dataMap.containsKey("error")) {
                val error = dataMap.getString("error") ?: "Unknown error"
                val success = dataMap.getBoolean("success", false)
                Log.e(TAG, "❌ WATCH: Error from phone: $error")
                broadcastError(error)
                return
            }
            
            // Check if it's a successful SMS response
            if (dataMap.containsKey("sms_text")) {
                val smsText = dataMap.getString("sms_text") ?: ""
                Log.d(TAG, "✅ WATCH: SMS text received: $smsText")
                broadcastSmsReceived(smsText)
            } else {
                Log.w(TAG, "⚠️ WATCH: SMS response missing sms_text field")
                broadcastError("Invalid SMS response from phone")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ WATCH: Error processing SMS response", e)
            broadcastError("Error processing phone response: ${e.message}")
        }
    }
    
    private fun broadcastSmsReceived(smsText: String) {
        val intent = Intent(ACTION_SMS_RECEIVED).apply {
            putExtra(EXTRA_SMS_TEXT, smsText)
        }
        sendBroadcast(intent)
        Log.d(TAG, "📡 WATCH: Broadcast SMS received: $smsText")
    }
    
    private fun broadcastConnectionStatus(isConnected: Boolean) {
        val intent = Intent(ACTION_CONNECTION_STATUS).apply {
            putExtra(EXTRA_IS_CONNECTED, isConnected)
        }
        sendBroadcast(intent)
        Log.d(TAG, "📡 WATCH: Broadcast connection status: $isConnected")
    }
    
    private fun broadcastSmsSent(success: Boolean) {
        val intent = Intent(ACTION_SMS_SENT).apply {
            putExtra(EXTRA_SMS_SUCCESS, success)
        }
        sendBroadcast(intent)
        Log.d(TAG, "📡 WATCH: Broadcast SMS sent: $success")
    }
    
    private fun broadcastError(errorMessage: String) {
        val intent = Intent(ACTION_ERROR).apply {
            putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
        }
        sendBroadcast(intent)
        Log.d(TAG, "📡 WATCH: Broadcast error: $errorMessage")
    }
}

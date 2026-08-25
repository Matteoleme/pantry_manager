package com.mobileapp.xpensa

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mobileapp.xpensa.data.FcmTokenRepository
import com.mobileapp.xpensa.data.local.DataStoreManager
import com.mobileapp.xpensa.ui.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class XpensaFirebaseMessagingService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "Nuovo token ricevuto: $token")
        
        val dataStoreManager = DataStoreManager(applicationContext)
        val fcmTokenRepository = FcmTokenRepository(dataStoreManager)
        
        scope.launch {
            fcmTokenRepository.saveToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCMService", "Messaggio ricevuto da: ${remoteMessage.from}")

        // Gestione data payload
        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"]
            val requestId = remoteMessage.data["request_id"]
            
            Log.d("FCMService", "Payload ricevuto: type=$type, requestId=$requestId")

            if (type == NotificationHelper.TYPE_PANTRY_SHARE_REQUEST && requestId != null) {
                val notificationHelper = NotificationHelper(applicationContext)
                notificationHelper.showPantryShareRequestNotification(requestId)
            }
        }
        
        // Se c'è un notification payload (opzionale, dato che preferiamo gestire via data)
        remoteMessage.notification?.let {
            Log.d("FCMService", "Notification Body: ${it.body}")
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}

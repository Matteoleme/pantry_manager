package com.mobileapp.xpensa

/*
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mobileapp.xpensa.data.FcmTokenRepository
import com.mobileapp.xpensa.data.local.DataStoreManager
import com.mobileapp.xpensa.ui.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class XpensaFirebaseMessagingService : FirebaseMessagingService() {

    private val job = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "Nuovo token ricevuto: $token")
        
        val dataStoreManager = DataStoreManager(applicationContext)
        val fcmTokenRepository = FcmTokenRepository(dataStoreManager)
        job.launch {
            fcmTokenRepository.saveToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCMService", "Messaggio ricevuto da: ${remoteMessage.from}")

        // Estrazione dei dati personalizzati
        val type = remoteMessage.data["type"]
        val requestId = remoteMessage.data["requestId"]
        val message = remoteMessage.data["message"]

        if (type != null && requestId != null) {
            Log.d("FCMService", "Payload ricevuto: type=$type, requestId=$requestId")
            if (type == "RESTOCK_REQUEST") {
                NotificationHelper(applicationContext).showNotification(
                    title = "Richiesta Rifornimento",
                    message = message ?: "Un membro della famiglia ha segnato un prodotto come esaurito.",
                    type = type,
                    requestId = requestId
                )
            }
        }

        remoteMessage.notification?.let {
            Log.d("FCMService", "Notification Body: ${it.body}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // La CoroutineScope non va cancellata qui perché è legata al ciclo di vita del Service
    }
}
*/
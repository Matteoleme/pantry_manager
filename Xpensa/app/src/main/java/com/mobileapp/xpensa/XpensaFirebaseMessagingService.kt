package com.mobileapp.xpensa

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mobileapp.xpensa.data.FcmTokenRepository
import com.mobileapp.xpensa.data.api.AuthApiFactory
import com.mobileapp.xpensa.data.local.DataStoreManager
import com.mobileapp.xpensa.ui.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class XpensaFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("XpensaFCMService", "Nuovo FCM token ricevuto: $token")

        val dataStoreManager = DataStoreManager(applicationContext)

        serviceScope.launch {
            try {
                // 1. Salva sempre il nuovo token localmente nel DataStore
                val localRepository = FcmTokenRepository(dataStoreManager)
                localRepository.saveToken(token)

                // 2. Verifica se l'utente è attualmente autenticato
                val authToken = dataStoreManager.authTokenFlow.first()
                if (!authToken.isNullOrBlank()) {
                    Log.d("XpensaFCMService", "Utente autenticato rilevato. Invio del nuovo FCM token al backend...")
                    val authApi = AuthApiFactory.createAuthApi(dataStoreManager)
                    val authenticatedRepository = FcmTokenRepository(dataStoreManager, authApi)
                    authenticatedRepository.registerTokenWithBackend()
                } else {
                    Log.d("XpensaFCMService", "Utente non autenticato. Il nuovo FCM token è stato salvato solo localmente.")
                }
            } catch (e: Exception) {
                Log.e("XpensaFCMService", "Errore non bloccante durante la gestione di onNewToken", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("XpensaFCMService", "Messaggio FCM ricevuto da: ${remoteMessage.from}")

        // Step 4: Estrazione ed interpretazione del payload FCM
        val data = remoteMessage.data
        Log.d("XpensaFCMService", "Payload dati ricevuto: $data")

        val type = data["type"]
        val requestId = data["request_id"] ?: data["requestId"]
        val requesterName = data["requester_name"] ?: data["requesterName"] ?: data["sender_name"]

        Log.d("XpensaFCMService", "Payload interpretato: type=$type, requestId=$requestId, requesterName=$requesterName")

        // Step 5: Generazione della notifica Android per le richieste di condivisione della dispensa
        if (type == NotificationHelper.TYPE_PANTRY_SHARE_REQUEST && !requestId.isNullOrBlank()) {
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.showPantryShareRequestNotification(
                requestId = requestId,
                requesterName = requesterName
            )
        } else {
            remoteMessage.notification?.let { notification ->
                Log.d("XpensaFCMService", "Notifica standard ricevuta: ${notification.title} - ${notification.body}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

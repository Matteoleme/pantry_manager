package com.mobileapp.xpensa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.mobileapp.xpensa.data.FcmTokenRepository
import com.mobileapp.xpensa.data.local.DataStoreManager
import com.mobileapp.xpensa.ui.PantryApp
import com.mobileapp.xpensa.ui.notifications.NotificationHelper
import com.mobileapp.xpensa.ui.theme.XpensaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val pendingRequestIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Inizializza il canale delle notifiche
        NotificationHelper(this).createNotificationChannel()

        // Recupero iniziale del token FCM
        val dataStoreManager = DataStoreManager(applicationContext)
        val fcmTokenRepository = FcmTokenRepository(dataStoreManager)
        CoroutineScope(Dispatchers.IO).launch {
            fcmTokenRepository.fetchAndSaveToken()
        }

        // Gestione eventuale click su notifica all'avvio
        handleNotificationIntent(intent)

        setContent {
            XpensaTheme {
                PantryApp(
                    pendingRequestId = pendingRequestIdState.value,
                    onPendingRequestConsumed = { pendingRequestIdState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return

        val type = intent.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_TYPE)
            ?: intent.getStringExtra("type")
            ?: intent.getStringExtra("notification_type")

        val requestId = intent.getStringExtra(NotificationHelper.EXTRA_REQUEST_ID)
            ?: intent.getStringExtra("request_id")
            ?: intent.getStringExtra("requestId")
            ?: intent.getStringExtra("id")

        Log.d("MainActivity", "handleNotificationIntent: type=$type, requestId=$requestId, extras=${intent.extras}")

        val isShareRequest = type == NotificationHelper.TYPE_PANTRY_SHARE_REQUEST || !requestId.isNullOrBlank()

        if (isShareRequest && !requestId.isNullOrBlank()) {
            Log.d("MainActivity", "Notifica di richiesta condivisione cliccata! Imposto pendingRequestId=$requestId")
            pendingRequestIdState.value = requestId
        }
    }
}

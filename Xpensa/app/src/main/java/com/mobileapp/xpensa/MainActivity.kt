package com.mobileapp.xpensa

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mobileapp.xpensa.data.FcmTokenRepository
import com.mobileapp.xpensa.data.local.DataStoreManager
import com.mobileapp.xpensa.ui.PantryApp
import com.mobileapp.xpensa.ui.notifications.NotificationHelper
import com.mobileapp.xpensa.ui.theme.XpensaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Inizializza il canale delle notifiche
        NotificationHelper(this).createNotificationChannel()

        /*
        // Recupero iniziale del token FCM
        val dataStoreManager = DataStoreManager(applicationContext)
        val fcmTokenRepository = FcmTokenRepository(dataStoreManager)
        CoroutineScope(Dispatchers.IO).launch {
            fcmTokenRepository.fetchAndSaveToken()
        }
        */

        // Gestione eventuale click su notifica all'avvio
        handleNotificationIntent(intent)

        setContent {
            XpensaTheme {
                PantryApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val type = intent?.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_TYPE)
        val requestId = intent?.getStringExtra(NotificationHelper.EXTRA_REQUEST_ID)
        
        if (type != null && requestId != null) {
            Log.d("MainActivity", "Notifica cliccata: type=$type, requestId=$requestId")
            // TODO: In futuro qui si può scatenare la navigazione verso la schermata delle richieste
            // o aggiornare il ViewModel per mostrare un modal specifico.
        }
    }
}

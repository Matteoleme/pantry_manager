package com.mobileapp.xpensa.ui.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mobileapp.xpensa.MainActivity
import com.mobileapp.xpensa.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "pantry_notifications"
        const val CHANNEL_NAME = "Pantry Notifications"
        const val CHANNEL_DESC = "Notifiche per le richieste di condivisione e aggiornamenti pantry"
        
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_REQUEST_ID = "request_id"
        
        const val TYPE_PANTRY_SHARE_REQUEST = "pantry_share_request"
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPantryShareRequestNotification(requestId: String, requesterName: String? = null) {
        // Garantisce che il canale delle notifiche sia creato
        createNotificationChannel()

        // Verifica permesso notifiche per Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("NotificationHelper", "Permesso POST_NOTIFICATIONS non concesso. Impossibile mostrare la notifica.")
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_PANTRY_SHARE_REQUEST)
            putExtra("type", TYPE_PANTRY_SHARE_REQUEST)
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra("request_id", requestId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 
            requestId.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "New Pantry Sharing Request"
        val contentText = if (!requesterName.isNullOrBlank()) {
            "$requesterName wants to join your pantry."
        } else {
            "Someone wants to join your pantry."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(requestId.hashCode(), builder.build())
    }
}

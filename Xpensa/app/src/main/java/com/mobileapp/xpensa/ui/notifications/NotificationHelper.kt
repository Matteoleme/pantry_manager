package com.mobileapp.xpensa.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
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

    fun showPantryShareRequestNotification(requestId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_PANTRY_SHARE_REQUEST)
            putExtra(EXTRA_REQUEST_ID, requestId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 
            requestId.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // TODO: Inserire un'icona appropriata (es. R.drawable.ic_notification).
        // Al momento usiamo ic_launcher come fallback.
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nuova richiesta di condivisione")
            .setContentText("Qualcuno vuole condividere una dispensa con te!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(requestId.hashCode(), builder.build())
    }
}

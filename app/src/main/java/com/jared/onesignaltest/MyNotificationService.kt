package com.jared.onesignaltest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.onesignal.notifications.INotificationReceivedEvent
import com.onesignal.notifications.INotificationServiceExtension
import org.json.JSONObject

class NotificationServiceExtension : INotificationServiceExtension {

    companion object {
        private const val TAG = "UberStyleNotification"
        private const val CHANNEL_ID = "live_updates"
        private const val NOTIFICATION_ID = 1001
        private var notificationChannelsCreated = false
    }

    override fun onNotificationReceived(event: INotificationReceivedEvent) {
        val context = event.context
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: run {
                Log.w(TAG, "NotificationManager not available.")
                return
            }

        // Create notification channels if needed
        if (!notificationChannelsCreated) {
            createNotificationChannels(notificationManager, context)
            notificationChannelsCreated = true
        }

        // Get additional data
        val additionalData = event.notification.additionalData
        val deliveryData = additionalData?.optJSONObject("delivery")

        if (deliveryData == null) {
            Log.i(TAG, "No delivery data found. Showing original notification.")
            return
        }

        // Prevent default notification and show our custom one
        event.preventDefault()
        showDeliveryNotification(deliveryData, notificationManager, context)
    }

    private fun createNotificationChannels(
        notificationManager: NotificationManager,
        context: Context
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Delivery Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time delivery tracking notifications"
                enableVibration(true)
                setShowBadge(true)
                lightColor = Color.BLUE
            }

            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Created notification channel")
        }
    }

    private fun showDeliveryNotification(
        data: JSONObject,
        notificationManager: NotificationManager,
        context: Context
    ) {
        // Extract data from JSON
        val status = data.optString("status", "preparing")
        val driverName = data.optString("driver_name", "Driver")
        val eta = data.optString("eta", "Unknown")
        val progress = data.optInt("progress", 0)

        // Get status-specific details
        val (title, message, emoji, color) = getStatusDetails(status, driverName, eta)

        Log.d(TAG, "Showing delivery notification: $status | Progress: $progress% | ETA: $eta")

        // Create intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build the notification with progress indicator
        val progressText = "[${"█".repeat(progress / 10)}${"░".repeat(10 - progress / 10)}] $progress%"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_directions) // You can change this to your own icon
            .setContentTitle(title)
            .setContentText(message)
            .setSubText("$emoji ETA: $eta • $progressText")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setColor(Color.parseColor(color))
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true) // Don't make sound/vibration on updates
            .setAutoCancel(false) // Don't dismiss when tapped (stays until completed)
            .setOngoing(status != "delivered" && status != "cancelled") // Can't swipe away unless completed

        // Add progress bar (both visual and numeric)
        if (progress in 0..100) {
            notification.setProgress(100, progress, false)
        }

        // Add action buttons based on status
        when (status) {
            "confirmed", "preparing" -> {
                // Add "Cancel Order" button
                notification.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Cancel",
                    createActionIntent(context, "cancel")
                )
            }
            "on_the_way", "nearby" -> {
                // Add "Track" and "Call Driver" buttons
                notification.addAction(
                    android.R.drawable.ic_menu_mapmode,
                    "Track",
                    createActionIntent(context, "track")
                )
                notification.addAction(
                    android.R.drawable.ic_menu_call,
                    "Call Driver",
                    createActionIntent(context, "call")
                )
            }
            "delivered" -> {
                // Order completed - make dismissible
                notification.setOngoing(false)
                notification.setAutoCancel(true)
            }
        }

        // Show/update the notification
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }

    private fun getStatusDetails(status: String, driverName: String, eta: String): StatusDetails {
        return when (status) {
            "confirmed" -> StatusDetails(
                title = "Order Confirmed",
                message = "Your order has been confirmed and is being prepared",
                emoji = "✅",
                color = "#34A853"
            )
            "preparing" -> StatusDetails(
                title = "Preparing Your Order",
                message = "The restaurant is preparing your food",
                emoji = "👨‍🍳",
                color = "#FBBC04"
            )
            "ready_for_pickup" -> StatusDetails(
                title = "Ready for Pickup",
                message = "Waiting for $driverName to collect",
                emoji = "📦",
                color = "#4285F4"
            )
            "on_the_way" -> StatusDetails(
                title = "$driverName is on the way",
                message = "Your order is being delivered",
                emoji = "🚗",
                color = "#4285F4"
            )
            "nearby" -> StatusDetails(
                title = "$driverName is nearby",
                message = "Driver is approaching your location",
                emoji = "📍",
                color = "#EA4335"
            )
            "arrived" -> StatusDetails(
                title = "Driver has arrived!",
                message = "$driverName is outside",
                emoji = "🎯",
                color = "#EA4335"
            )
            "delivered" -> StatusDetails(
                title = "Order Delivered",
                message = "Enjoy your meal! Thanks for ordering",
                emoji = "🎉",
                color = "#34A853"
            )
            "cancelled" -> StatusDetails(
                title = "Order Cancelled",
                message = "Your order has been cancelled",
                emoji = "❌",
                color = "#EA4335"
            )
            else -> StatusDetails(
                title = "Delivery Update",
                message = "Status: $status",
                emoji = "📱",
                color = "#4285F4"
            )
        }
    }

    private fun createActionIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("action", action)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    data class StatusDetails(
        val title: String,
        val message: String,
        val emoji: String,
        val color: String
    )
}
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
        private const val DELIVERY_NOTIFICATION_ID = 1001
        private const val SCORE_NOTIFICATION_ID = 2001
        private var notificationChannelsCreated = false
    }

    override fun onNotificationReceived(event: INotificationReceivedEvent) {
        Log.d(TAG, "🔥🔥🔥 === NOTIFICATION RECEIVED === 🔥🔥🔥")

        val context = event.context
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: run {
                Log.w(TAG, "❌ NotificationManager not available.")
                return
            }

        // Create notification channels if needed
        if (!notificationChannelsCreated) {
            createNotificationChannels(notificationManager, context)
            notificationChannelsCreated = true
            Log.d(TAG, "✅ Channels created")
        }

        // Get additional data
        val additionalData = event.notification.additionalData
        Log.d(TAG, "📦 Additional Data: $additionalData")

        // Check for delivery data
        val deliveryData = additionalData?.optJSONObject("delivery")
        Log.d(TAG, "🚚 Delivery Data: $deliveryData")

        // Check for score data
        val scoreData = additionalData?.optJSONObject("score")
        Log.d(TAG, "🏈 Score Data: $scoreData")

        // Determine which type of notification to show
        when {
            deliveryData != null -> {
                Log.d(TAG, "🛑 Preventing default notification - Showing DELIVERY")
                event.preventDefault()
                showDeliveryNotification(deliveryData, notificationManager, context)
            }
            scoreData != null -> {
                Log.d(TAG, "🛑 Preventing default notification - Showing SCORE")
                event.preventDefault()
                showScoreNotification(scoreData, notificationManager, context)
            }
            else -> {
                Log.i(TAG, "⚠️ No custom data found. Showing original notification.")
                return
            }
        }
    }

    private fun createNotificationChannels(
        notificationManager: NotificationManager,
        context: Context
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time delivery and score updates"
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
        try {
            // Extract data from JSON
            val status = data.optString("status", "preparing")
            val driverName = data.optString("driver_name", "Driver")
            val eta = data.optString("eta", "Unknown")
            val progress = data.optInt("progress", 0)

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🚚 DELIVERY NOTIFICATION DETAILS:")
            Log.d(TAG, "   Status: $status")
            Log.d(TAG, "   Driver: $driverName")
            Log.d(TAG, "   ETA: $eta")
            Log.d(TAG, "   Progress: $progress%")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // Get status-specific details
            val (title, message, emoji, color) = getStatusDetails(status, driverName, eta)

            // Build visual progress indicator
            val progressBarChars = progress / 10
            val emptyChars = 10 - progressBarChars
            val progressBar = "█".repeat(progressBarChars) + "░".repeat(emptyChars)
            val progressText = "$progressBar $progress%"

            Log.d(TAG, "📊 Progress Bar: $progressText")

            // Build expanded message with progress details
            val expandedMessage = """
                $message
                
                Progress: $progressText
                ETA: $eta
                Driver: $driverName
                Status: $status
            """.trimIndent()

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

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setContentTitle("$emoji $title")
                .setContentText("$message • $progress% • $eta")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(expandedMessage)
                        .setBigContentTitle("$emoji $title")
                )
                .setSubText(progressText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setColor(Color.parseColor(color))
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setOngoing(status != "delivered" && status != "cancelled")
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())

            // Add progress bar
            Log.d(TAG, "➕ Adding progress bar: $progress/100")
            if (progress in 0..100) {
                notification.setProgress(100, progress, false)
            }

            // Add action buttons based on status
            when (status) {
                "confirmed", "preparing" -> {
                    notification.addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Cancel",
                        createActionIntent(context, "cancel")
                    )
                    Log.d(TAG, "🔘 Added Cancel button")
                }
                "on_the_way", "nearby" -> {
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
                    Log.d(TAG, "🔘 Added Track and Call buttons")
                }
                "delivered" -> {
                    notification.setOngoing(false)
                    notification.setAutoCancel(true)
                    Log.d(TAG, "✅ Set as dismissible (delivered)")
                }
            }

            // Show/update the notification
            notificationManager.notify(DELIVERY_NOTIFICATION_ID, notification.build())
            Log.d(TAG, "✅✅✅ DELIVERY NOTIFICATION POSTED WITH ID: $DELIVERY_NOTIFICATION_ID ✅✅✅")

        } catch (e: Exception) {
            Log.e(TAG, "❌❌❌ ERROR SHOWING DELIVERY NOTIFICATION: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun showScoreNotification(
        data: JSONObject,
        notificationManager: NotificationManager,
        context: Context
    ) {
        try {
            // Extract score data
            val homeTeam = data.optString("home_team", "Home")
            val awayTeam = data.optString("away_team", "Away")
            val homeScore = data.optInt("home_score", 0)
            val awayScore = data.optInt("away_score", 0)
            val gameTime = data.optString("game_time", "LIVE")
            val quarter = data.optString("quarter", "")
            val progress = data.optInt("progress", 0)

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🏈 SCORE UPDATE DETAILS:")
            Log.d(TAG, "   Home: $homeTeam ($homeScore)")
            Log.d(TAG, "   Away: $awayTeam ($awayScore)")
            Log.d(TAG, "   Time: $gameTime")
            Log.d(TAG, "   Quarter: $quarter")
            Log.d(TAG, "   Progress: $progress%")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // Determine which team is leading
            val leadingEmoji = when {
                homeScore > awayScore -> "🔥"
                awayScore > homeScore -> "🔥"
                else -> "⚖️"
            }

            val scoreDiff = kotlin.math.abs(homeScore - awayScore)
            val gameStatus = when {
                gameTime.contains("Final", ignoreCase = true) -> "Final Score"
                gameTime.contains("Half", ignoreCase = true) -> "Halftime"
                else -> "LIVE"
            }

            // Build progress indicator for game time
            val progressBar = "█".repeat(progress / 10) + "░".repeat(10 - progress / 10)
            val progressText = "$progressBar $progress%"

            // Build expanded message
            val expandedMessage = """
                $homeTeam: $homeScore
                $awayTeam: $awayScore
                
                Game Progress: $progressText
                Time: $gameTime
                ${if (quarter.isNotEmpty()) "Quarter: $quarter" else ""}
                ${if (scoreDiff > 0) "Lead: $scoreDiff points" else "Tied game"}
            """.trimIndent()

            // Create intent
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("game", "$homeTeam vs $awayTeam")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Determine notification color based on game status
            val notificationColor = when {
                gameTime.contains("Final", ignoreCase = true) -> "#34A853" // Green
                else -> "#EA4335" // Red (LIVE)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setContentTitle("$leadingEmoji $homeTeam $homeScore - $awayScore $awayTeam")
                .setContentText("$gameStatus • $gameTime")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(expandedMessage)
                        .setBigContentTitle("$leadingEmoji $homeTeam $homeScore - $awayScore $awayTeam")
                )
                .setSubText(progressText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setColor(Color.parseColor(notificationColor))
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setOngoing(!gameTime.contains("Final", ignoreCase = true))
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())

            // Add progress bar
            Log.d(TAG, "➕ Adding game progress bar: $progress/100")
            if (progress in 0..100) {
                notification.setProgress(100, progress, false)
            }

            // Add action buttons
            notification.addAction(
                android.R.drawable.ic_menu_view,
                "View Game",
                pendingIntent
            )

            // If game is final, make it dismissible
            if (gameTime.contains("Final", ignoreCase = true)) {
                notification.setOngoing(false)
                notification.setAutoCancel(true)
                Log.d(TAG, "✅ Set as dismissible (game finished)")
            }

            // Show notification
            notificationManager.notify(SCORE_NOTIFICATION_ID, notification.build())
            Log.d(TAG, "✅✅✅ SCORE NOTIFICATION POSTED WITH ID: $SCORE_NOTIFICATION_ID ✅✅✅")

        } catch (e: Exception) {
            Log.e(TAG, "❌❌❌ ERROR SHOWING SCORE NOTIFICATION: ${e.message}", e)
            e.printStackTrace()
        }
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
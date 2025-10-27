package com.jared.onesignaltest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.jared.onesignaltest.ui.theme.MyApplicationTheme
import com.onesignal.OneSignal
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    OneSignalTestScreen(
                        modifier = Modifier.padding(innerPadding),
                        context = this
                    )
                }
            }
        }
    }
}

@Composable
fun OneSignalTestScreen(
    modifier: Modifier = Modifier,
    context: ComponentActivity
) {
    var userId by remember { mutableStateOf("Loading...") }
    var isSubscribed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Load initial subscription status
    LaunchedEffect(Unit) {
        try {
            userId = OneSignal.User.pushSubscription.id ?: "Not available"
            isSubscribed = OneSignal.User.pushSubscription.optedIn
            isLoading = false
        } catch (e: Exception) {
            userId = "Error loading"
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header
        Text(
            text = "Jared Android Test",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Push Notification Manager",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Bell Icon with Status
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(60.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSubscribed)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = if (isSubscribed) "Subscribed" else "Not Subscribed",
                    modifier = Modifier.size(64.dp),
                    tint = if (isSubscribed)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Text
        Text(
            text = if (isSubscribed) "Notifications Enabled" else "Notifications Disabled",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isSubscribed)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSubscribed)
                "You'll receive push notifications"
            else
                "Enable to receive notifications",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Subscribe/Unsubscribe Toggle
        Button(
            onClick = {
                scope.launch {
                    if (isSubscribed) {
                        OneSignal.User.pushSubscription.optOut()
                    } else {
                        OneSignal.User.pushSubscription.optIn()
                        OneSignal.Notifications.requestPermission(true)
                    }
                    kotlinx.coroutines.delay(500)
                    isSubscribed = OneSignal.User.pushSubscription.optedIn
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSubscribed)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (isSubscribed) "Disable Notifications" else "Enable Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TEST PROGRESS BAR BUTTON (NEW!)
        Button(
            onClick = {
                testProgressNotification(context)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "🧪 Test Progress Bar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // User ID Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Subscription ID",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isLoading) "Loading..." else userId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Refresh Button
        OutlinedButton(
            onClick = {
                userId = OneSignal.User.pushSubscription.id ?: "Not available"
                isSubscribed = OneSignal.User.pushSubscription.optedIn
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Refresh Status")
        }
    }
}

// Test function to see if progress bars work on this device
fun testProgressNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Create channel
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "test_progress",
            "Test Progress",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    // Create notification with 75% progress bar
    val notification = NotificationCompat.Builder(context, "test_progress")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("🧪 Progress Bar Test")
        .setContentText("Testing if progress bars work on your device")
        .setProgress(100, 75, false)  // 75% progress
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText("If you see a horizontal bar below this text, progress bars work!\n\nProgress: ██████████░░░ 75%")
        )
        .build()

    notificationManager.notify(888, notification)

    Toast.makeText(context, "Test notification sent! Check your notification shade", Toast.LENGTH_LONG).show()
}
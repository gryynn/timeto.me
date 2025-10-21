package me.timeto.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.timeto.app.ui.HStack
import me.timeto.app.ui.c
import me.timeto.app.ui.roundedShape
import me.timeto.shared.sync.SyncStatus

@Composable
fun HomeSyncButtonView(
    isSyncing: Boolean,
    status: SyncStatus,
    statusMessage: String,
    isConfigured: Boolean,
    onClick: () -> Unit,
) {
    
    if (!isConfigured) return

    Box(
        modifier = Modifier
            .padding(horizontal = 50.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(roundedShape)
            .background(
                when (status) {
                    SyncStatus.SUCCESS -> c.green.copy(alpha = 0.15f)
                    SyncStatus.ERROR -> c.red.copy(alpha = 0.15f)
                    SyncStatus.SYNCING -> c.blue.copy(alpha = 0.15f)
                    else -> c.white.copy(alpha = 0.05f)
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        HStack(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = c.blue,
                )
                
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = if (isSyncing) "Syncing..." else statusMessage,
                color = when (status) {
                    SyncStatus.SUCCESS -> c.green
                    SyncStatus.ERROR -> c.red
                    SyncStatus.SYNCING -> c.blue
                    else -> c.text
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "⟳",
                color = c.blue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}


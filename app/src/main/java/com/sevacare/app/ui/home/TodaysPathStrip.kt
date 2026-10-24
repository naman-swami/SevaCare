package com.sevacare.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sevacare.app.ui.theme.SevaBodyTeal
import com.sevacare.app.ui.theme.SevaCream
import com.sevacare.app.ui.theme.SevaCreamContainer
import com.sevacare.app.ui.theme.SevaCreamSurface
import com.sevacare.app.ui.theme.SevaDarkText
import com.sevacare.app.ui.theme.SevaDeepTeal
import com.sevacare.app.ui.theme.SevaSafeGreen
import com.sevacare.app.ui.theme.SevaSafeGreenContainer
import com.sevacare.app.ui.theme.SevaSaffron
import com.sevacare.app.ui.theme.SevaWhite

enum class NodeStatus {
    DONE,
    CURRENT,
    PENDING
}

data class PathNode(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeLabel: String,
    val icon: ImageVector,
    val status: NodeStatus
)

/**
 * Today's Path — Single 3-Node Vertical Strip (Section 4.6 & Section 5).
 * Replaces old streak strip with a calm, guilt-free daily path (Morning, Afternoon, Night).
 * Done nodes glow green; current node features Chhotu; missed nodes remain calm without loss penalty.
 */
@Composable
fun TodaysPathStrip(
    nodes: List<PathNode>,
    onNodeClick: (PathNode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SevaCreamSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Routine Path",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SevaDeepTeal
                )
                Text(
                    text = "3 Steps",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SevaBodyTeal
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            nodes.forEachIndexed { index, node ->
                PathNodeRow(
                    node = node,
                    isLast = index == nodes.size - 1,
                    onClick = { onNodeClick(node) }
                )
            }
        }
    }
}

@Composable
private fun PathNodeRow(
    node: PathNode,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Node Indicator Column (Dot + Connecting line)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            val (nodeBg, nodeContentColor) = when (node.status) {
                NodeStatus.DONE -> Pair(SevaSafeGreen, SevaWhite)
                NodeStatus.CURRENT -> Pair(SevaSaffron, SevaDarkText)
                NodeStatus.PENDING -> Pair(Color.LightGray.copy(alpha = 0.5f), Color.DarkGray)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(nodeBg)
                    .border(
                        width = if (node.status == NodeStatus.CURRENT) 3.dp else 0.dp,
                        color = if (node.status == NodeStatus.CURRENT) SevaDeepTeal else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (node.status == NodeStatus.DONE) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = nodeContentColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = node.icon,
                        contentDescription = null,
                        tint = nodeContentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(36.dp)
                        .background(
                            if (node.status == NodeStatus.DONE) SevaSafeGreen else Color.LightGray.copy(alpha = 0.5f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = node.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (node.status == NodeStatus.DONE) SevaSafeGreen else SevaDarkText
                )
                Text(
                    text = node.timeLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SevaBodyTeal
                )
            }
            Text(
                text = node.subtitle,
                fontSize = 14.sp,
                color = SevaBodyTeal.copy(alpha = 0.85f),
                lineHeight = 18.sp
            )
        }

        // Companion Indicator beside Current Node
        if (node.status == NodeStatus.CURRENT) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, top = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SevaCreamContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🐦 Companion Is Here",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SevaDeepTeal
                )
            }
        }
    }
}

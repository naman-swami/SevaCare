package com.sevacare.app.ui.medicine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sevacare.app.data.model.ConflictReportEntity
import com.sevacare.app.ui.theme.SevaBodyTeal
import com.sevacare.app.ui.theme.SevaCream
import com.sevacare.app.ui.theme.SevaCreamContainer
import com.sevacare.app.ui.theme.SevaDanger
import com.sevacare.app.ui.theme.SevaDarkText
import com.sevacare.app.ui.theme.SevaDeepTeal
import com.sevacare.app.ui.theme.SevaMint
import com.sevacare.app.ui.theme.SevaSafeGreen
import com.sevacare.app.ui.theme.SevaSaffron
import com.sevacare.app.ui.theme.SevaWhite

@Composable
fun ConflictApprovalDialog(
    report: ConflictReportEntity,
    isFamilyPaired: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onNavigatePairing: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = SevaCream,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = SevaDanger,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Drug Conflict Detected",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevaDanger
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(SevaMint, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = SevaDeepTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interacting medicines card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFDAD6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Conflict Between:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF410002)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${report.candidateMedicineName}  ↔  ${report.conflictingMedicineName}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevaDarkText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Severity: ${report.severity}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevaDanger
                        )
                        Text(
                            text = report.interactionReason,
                            fontSize = 13.sp,
                            color = SevaDarkText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Corrected schedule recommendation card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SevaWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, SevaMint, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Recommended Safe Schedule:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevaDeepTeal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Proposed Time: ${report.proposedScheduleTime}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevaSafeGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.correctedRecommendation,
                            fontSize = 13.sp,
                            color = SevaBodyTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Approval actions
                if (isFamilyPaired) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SevaSafeGreen)
                    ) {
                        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Approve Corrected Schedule")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onReject,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SevaMint, contentColor = SevaDeepTeal)
                    ) {
                        Text("Reject Candidate Medicine")
                    }
                } else {
                    // Unpaired State: Must explicitly prompt to pair family member (Zero-mock rule)
                    Text(
                        text = "A family member is not currently paired to review this safety conflict.",
                        fontSize = 13.sp,
                        color = SevaDanger,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onNavigatePairing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SevaDeepTeal)
                    ) {
                        Icon(imageVector = Icons.Rounded.GroupAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connect Family Member")
                    }
                }
            }
        }
    }
}

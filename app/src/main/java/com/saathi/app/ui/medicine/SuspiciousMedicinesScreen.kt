package com.saathi.app.ui.medicine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saathi.app.data.db.SaathiDatabase
import com.saathi.app.ui.theme.SevaBodyTeal
import com.saathi.app.ui.theme.SevaCream
import com.saathi.app.ui.theme.SevaDanger
import com.saathi.app.ui.theme.SevaDarkText
import com.saathi.app.ui.theme.SevaDeepTeal
import com.saathi.app.ui.theme.SevaMint
import com.saathi.app.ui.theme.SevaWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SuspiciousMedicinesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { SaathiDatabase.getInstance(context) }
    val suspiciousList by db.conflictDao().getSuspiciousMedicinesFlow().collectAsState(initial = emptyList())

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SevaCream
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(54.dp)
                        .background(SevaMint, CircleShape)
                        .semantics { contentDescription = "Go back" }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = SevaDeepTeal
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Suspicious Flagged Medicines",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                    Text(
                        text = "${suspiciousList.size} contradictory intakes logged",
                        fontSize = 14.sp,
                        color = SevaBodyTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (suspiciousList.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SevaWhite),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Suspicious Medicines",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevaDarkText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Medicines with stated reasons contradicting clinical indications will be securely logged here for doctor review.",
                            fontSize = 14.sp,
                            color = SevaBodyTeal
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(suspiciousList, key = { it.id }) { item ->
                        val dateStr = remember(item.flaggedAt) {
                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.flaggedAt))
                        }
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SevaWhite),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, SevaDanger, RoundedCornerShape(18.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
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
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.medicineName,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SevaDarkText
                                        )
                                    }
                                    Text(
                                        text = dateStr,
                                        fontSize = 12.sp,
                                        color = SevaBodyTeal
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Stated Reason: \"${item.statedReason}\"",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SevaDeepTeal
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = item.explanation,
                                    fontSize = 13.sp,
                                    color = SevaDanger
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

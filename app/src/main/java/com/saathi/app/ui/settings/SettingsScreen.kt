package com.saathi.app.ui.settings

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saathi.app.R
import com.saathi.app.ui.theme.LocalSevaColors
import com.saathi.app.util.AppLocaleManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val colors = LocalSevaColors.current
    val prefs = remember { context.getSharedPreferences("sevacare_prefs", Context.MODE_PRIVATE) }

    val currentTag = remember { AppLocaleManager.getCurrentLocaleTag(context) }
    var selectedTag by remember { mutableStateOf(currentTag) }
    var showLegalDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.canvas,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.canvas)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language Settings Card
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface1),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = colors.teal)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.settings_language_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.ink
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.settings_language_desc),
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // 6 Indian Language Selector Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLocaleManager.SUPPORTED_LANGUAGES.take(3).forEach { lang ->
                                val isSelected = selectedTag.startsWith(lang.tag, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTag = lang.tag
                                        AppLocaleManager.setLocale(context, lang.tag, activity)
                                    },
                                    label = { Text("${lang.scriptChar} ${lang.nativeName}") }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLocaleManager.SUPPORTED_LANGUAGES.drop(3).forEach { lang ->
                                val isSelected = selectedTag.startsWith(lang.tag, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTag = lang.tag
                                        AppLocaleManager.setLocale(context, lang.tag, activity)
                                    },
                                    label = { Text("${lang.scriptChar} ${lang.nativeName}") }
                                )
                            }
                        }
                    }
                }
            }

            // Circle Contacts Settings Card
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface1),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = colors.teal)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.nav_circle), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.ink)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.pause_mode_dismiss_hint), fontSize = 14.sp, color = colors.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { /* Open contact manager */ }) {
                        Text(stringResource(R.string.pause_mode_call_family))
                    }
                }
            }

            // Privacy & Legal Terms Card
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface1),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.settings_legal_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.ink)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_legal_desc),
                        fontSize = 14.sp,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = { showLegalDialog = true }) {
                        Text(stringResource(R.string.settings_btn_view_terms))
                    }
                }
            }

            // Reset App Data Card
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.danger.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = colors.danger)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.settings_btn_reset_data), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.danger)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_reset_desc), fontSize = 14.sp, color = colors.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            prefs.edit().clear().apply()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                    ) {
                        Text(stringResource(R.string.settings_btn_reset_confirm), color = Color.White)
                    }
                }
            }
        }
    }

    if (showLegalDialog) {
        AlertDialog(
            onDismissRequest = { showLegalDialog = false },
            title = { Text(stringResource(R.string.settings_legal_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "🔒 " + stringResource(R.string.home_offline_banner_title),
                        fontWeight = FontWeight.Bold,
                        color = colors.teal
                    )
                    Text(
                        stringResource(R.string.home_offline_banner_desc),
                        fontSize = 13.sp
                    )
                    HorizontalDivider()
                    Text(
                        "⚕️ " + stringResource(R.string.review_disclaimer),
                        fontWeight = FontWeight.Bold,
                        color = colors.warning
                    )
                    HorizontalDivider()
                    Text(
                        "🚨 " + stringResource(R.string.emergency_sos) + " (112)",
                        fontWeight = FontWeight.Bold,
                        color = colors.danger
                    )
                    Text(
                        stringResource(R.string.trust_btn_call_emergency),
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLegalDialog = false }) {
                    Text(stringResource(R.string.btn_understood))
                }
            }
        )
    }
}

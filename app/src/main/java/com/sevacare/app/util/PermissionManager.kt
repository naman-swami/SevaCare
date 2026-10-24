package com.sevacare.app.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sevacare.app.ui.theme.SevaDeepTeal
import com.sevacare.app.ui.theme.SevaSaffron

object PermissionManager {

    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isPermanentlyDenied(activity: Activity?, permission: String): Boolean {
        if (activity == null) return false
        val isDenied = ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        return isDenied && !shouldShowRationale
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

/**
 * Real runtime permission gate that requests system dialogs on mount,
 * evaluates granted state, and shows rationale with a Settings deep-link
 * when permanently denied.
 */
@Composable
fun RememberPermissionGate(
    permissions: Array<String>,
    rationaleTitle: String = "Permission Required",
    rationaleMessage: String = "This safety feature requires hardware permissions to function properly.",
    onGranted: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var allGranted by remember {
        mutableStateOf(PermissionManager.hasPermissions(context, permissions))
    }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        allGranted = grants.values.all { it }
        if (!allGranted) {
            permanentlyDenied = grants.keys.any { perm ->
                PermissionManager.isPermanentlyDenied(activity, perm)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!allGranted) {
            launcher.launch(permissions)
        }
    }

    when {
        allGranted -> {
            onGranted()
        }
        permanentlyDenied -> {
            PermissionDeniedView(
                title = rationaleTitle,
                message = "$rationaleMessage\n\nPermissions were denied. Please enable them in App Settings to proceed.",
                buttonText = "Open App Settings",
                onAction = { PermissionManager.openAppSettings(context) }
            )
        }
        else -> {
            PermissionDeniedView(
                title = rationaleTitle,
                message = rationaleMessage,
                buttonText = "Grant Permission",
                onAction = { launcher.launch(permissions) }
            )
        }
    }
}

@Composable
fun PermissionDeniedView(
    title: String,
    message: String,
    buttonText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SevaDeepTeal.copy(alpha = 0.1f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = SevaDeepTeal,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SevaDeepTeal,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    fontSize = 15.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = SevaDeepTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

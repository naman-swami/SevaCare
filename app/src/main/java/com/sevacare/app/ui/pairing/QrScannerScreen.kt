package com.sevacare.app.ui.pairing

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sevacare.app.data.pairing.FamilyPairEntity
import com.sevacare.app.data.pairing.PairingRepository
import com.sevacare.app.ui.components.LiveQrScannerView
import com.sevacare.app.ui.theme.SevaCream
import com.sevacare.app.ui.theme.SevaDeepTeal
import com.sevacare.app.util.RememberPermissionGate
import kotlinx.coroutines.launch

/**
 * Production-ready QR Scanner Screen.
 * Enforces real runtime Camera permissions through RememberPermissionGate.
 * Connects LiveQrScannerView with PairingRepository for cryptographic handshake.
 */
@Composable
fun QrScannerScreen(
    pairingRepository: PairingRepository,
    onPairingSuccess: (FamilyPairEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    RememberPermissionGate(
        permissions = arrayOf(Manifest.permission.CAMERA),
        rationaleTitle = "Camera Access Required for Pairing",
        rationaleMessage = "SevaCare needs camera access to scan your family member's pairing QR code securely."
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LiveQrScannerView(
                onPayloadDecoded = { payload ->
                    if (!isVerifying) {
                        isVerifying = true
                        errorMessage = null
                        scope.launch {
                            val result = pairingRepository.establishPair(payload)
                            result.onSuccess { pairEntity ->
                                isVerifying = false
                                Toast.makeText(
                                    context,
                                    "Connected securely with ${pairEntity.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onPairingSuccess(pairEntity)
                            }.onFailure { err ->
                                isVerifying = false
                                errorMessage = err.message ?: "Invalid pairing QR code"
                                Toast.makeText(
                                    context,
                                    "Pairing failed: ${err.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                },
                onDismiss = onBack,
                instructionText = "Point camera at the elder's SevaCare QR code"
            )

            if (isVerifying) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SevaCream),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = SevaDeepTeal, strokeWidth = 3.dp)
                        Column {
                            Text(
                                "Verifying Handshake...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SevaDeepTeal
                            )
                            Text(
                                "Deriving session keys via HKDF",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

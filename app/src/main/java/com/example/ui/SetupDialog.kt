package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.CosmicBackground
import com.example.CosmicGlass
import com.example.CosmicSlate
import com.example.NeonCyan
import com.example.TextGold
import com.example.TextPrimary
import com.example.TextSecondary
import com.example.CardBorderColor
import android.widget.Toast

@Composable
fun SetupDialog(
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    var tokenInput by remember { mutableStateOf(viewModel.configManager.getBotToken() ?: "") }
    var chatInput by remember { mutableStateOf(viewModel.configManager.getChatId() ?: "") }
    var hideToken by remember { mutableStateOf(true) }
    var isTesting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val parseAndAutoFill: (String) -> Unit = { input ->
        tokenInput = input.trim()
    }
    
    val parseAndAutoFillChat: (String) -> Unit = { input ->
        chatInput = input.trim()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true, 
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CosmicGlass,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .border(1.dp, CardBorderColor, RoundedCornerShape(24.dp))
                .testTag("setup_dialog_surface")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    "INITIAL VAULT SETUP",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Text(
                    "Configure your personal Telegram Bot Token and Chat ID to establish private cloud storage.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Bot token Input
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { parseAndAutoFill(it) },
                    label = { Text("Telegram Bot Token", fontSize = 12.sp) },
                    placeholder = { Text("e.g. 123456789:ABCdef...", fontSize = 11.sp) },
                    visualTransformation = if (hideToken) PasswordVisualTransformation() else VisualTransformation.None,
                    trailingIcon = {
                        IconButton(onClick = { hideToken = !hideToken }) {
                            Icon(
                                imageVector = if (hideToken) Icons.Default.Lock else Icons.Default.Close,
                                contentDescription = "Toggle Visibility",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderColor,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("bot_token_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Chat ID Input
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { parseAndAutoFillChat(it) },
                    label = { Text("Telegram Chat ID", fontSize = 12.sp) },
                    placeholder = { Text("e.g. -100123456789", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderColor,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("chat_id_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                var autoSyncEnabled by remember { mutableStateOf(viewModel.configManager.isAutoSyncEnabled()) }

                // Auto Sync Toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Background Auto-Sync",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Automatically monitor enrolled folders and sync over Wi-Fi.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = { checked ->
                            autoSyncEnabled = checked
                            viewModel.configManager.setAutoSyncEnabled(checked)
                            viewModel.setupPeriodicAutoSync()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CardBorderColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Connection buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (tokenInput.isEmpty() || chatInput.isEmpty()) {
                                Toast.makeText(context, "Fill in Bot Token and Chat ID to test!", Toast.LENGTH_SHORT).show()
                            } else {
                                val cleanToken = com.example.data.TelegramConfigManager.sanitizeBotToken(tokenInput)
                                val cleanChatId = com.example.data.TelegramConfigManager.sanitizeChatId(chatInput)
                                tokenInput = cleanToken
                                chatInput = cleanChatId
                                isTesting = true
                                viewModel.testTelegramConnection(cleanToken, cleanChatId) { success, msg ->
                                    isTesting = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("test_connection_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = NeonCyan)
                        } else {
                            Text("TEST", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            val cleanToken = com.example.data.TelegramConfigManager.sanitizeBotToken(tokenInput)
                            val cleanChatId = com.example.data.TelegramConfigManager.sanitizeChatId(chatInput)
                            tokenInput = cleanToken
                            chatInput = cleanChatId
                            
                            viewModel.saveTelegramSettings(cleanToken, cleanChatId)
                            viewModel.configManager.setAutoSyncEnabled(autoSyncEnabled)
                            viewModel.setupPeriodicAutoSync()

                            if (cleanToken.isNotEmpty() && cleanChatId.isNotEmpty()) {
                                Toast.makeText(context, "Settings securely saved & Cloud Vault active!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Settings saved! You can complete setup anytime in Settings.", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_and_proceed_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SAVE & PROCEED", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_setup_dialog_button")
                ) {
                    Text(
                        if (viewModel.configManager.isConfigured()) "Close" else "Explore App / Configure Later in Settings",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

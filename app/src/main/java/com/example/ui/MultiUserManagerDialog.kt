package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.CardBorderColor
import com.example.CosmicBackground
import com.example.CosmicGlass
import com.example.CosmicSlate
import com.example.NeonCyan
import com.example.NeonGreen
import com.example.NeonPurple
import com.example.SoftCoral
import com.example.TextGold
import com.example.TextPrimary
import com.example.TextSecondary
import com.example.data.TelegramConfigManager
import com.example.data.UserAccount

val HumanAvatarColors = listOf(
    "#C85A32", // Warm Terracotta
    "#3E6B52", // Sage Botanical
    "#B45309", // Amber Stone
    "#334155", // Nordic Slate
    "#5D4037", // Warm Espresso
    "#8D5B4C"  // Roasted Clay
)

fun parseHexColor(hex: String, fallback: Color = Color(0xFFC85A32)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

@Composable
fun MultiUserManagerDialog(
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val users by viewModel.users.collectAsStateWithLifecycle()
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val checkingUserId by viewModel.checkingUserId.collectAsStateWithLifecycle()
    val isTestingAll by viewModel.isTestingAllBots.collectAsStateWithLifecycle()
    val availableTargets by viewModel.availableUploadTargets.collectAsStateWithLifecycle()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<UserAccount?>(null) }
    var checkResultMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, CardBorderColor, RoundedCornerShape(24.dp))
                .testTag("multi_user_dialog"),
            colors = CardDefaults.cardColors(containerColor = CosmicSlate),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "USER ACCOUNTS",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Multiple credentials & independent vaults",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(CosmicGlass, CircleShape)
                            .size(36.dp)
                            .testTag("close_multi_user_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Multi-Bot Sharing Status Card
                val validProfileBots = users.filter { it.botToken.isNotBlank() }
                if (validProfileBots.size >= 2 || availableTargets.size >= 2) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonGreen.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "MULTI-BOT SHARING ACTIVE",
                                        color = NeonGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NeonGreen.copy(alpha = 0.2f),
                                    border = BorderStroke(0.5.dp, NeonGreen)
                                ) {
                                    Text(
                                        text = "${availableTargets.size.coerceAtLeast(validProfileBots.size)} BOTS SHARING",
                                        color = NeonGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "All connected user profile bots share backup uploads round-robin. Files automatically rotate across both bots so neither bot is overloaded.",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val targetChatId = activeUser.chatId.ifEmpty { viewModel.configManager.getChatId() }
                                    viewModel.verifyAllBots(targetChatId) { passed, total, summary ->
                                        checkResultMessage = summary
                                        Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = !isTestingAll,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen.copy(alpha = 0.2f)
                                ),
                                border = BorderStroke(1.dp, NeonGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("verify_all_profile_bots_button"),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                if (isTestingAll) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = NeonGreen)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("VERIFYING BOTH BOTS...", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("VERIFY BOTH BOTS LIVE", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CosmicGlass,
                        border = BorderStroke(1.dp, CardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TextGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Each account uses its own Telegram Bot and Chat ID. Add a second profile or bot to enable 2-bot round-robin file upload sharing.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Users list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(users, key = { it.id }) { user ->
                        val isActive = user.id == activeUser.id
                        val isChecking = checkingUserId == user.id
                        val avatarColor = parseHexColor(user.avatarColorHex)

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) CosmicGlass else CosmicBackground.copy(alpha = 0.6f)
                            ),
                            border = BorderStroke(
                                if (isActive) 1.5.dp else 1.dp,
                                if (isActive) NeonCyan else CardBorderColor
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("user_account_${user.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // User info row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Avatar
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(avatarColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.name.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = user.name,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isActive) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = NeonCyan.copy(alpha = 0.15f),
                                                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                                                    ) {
                                                        Text(
                                                            text = "ACTIVE",
                                                            color = NeonCyan,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "${user.role} • Chat: ${user.chatId.ifEmpty { "Not configured" }}",
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Action buttons (Edit & Delete)
                                    Row {
                                        IconButton(
                                            onClick = { userToEdit = user },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit User",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (users.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteUser(user.id)
                                                    Toast.makeText(context, "Removed profile '${user.name}'", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete User",
                                                    tint = SoftCoral.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Credentials details snippet
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CosmicBackground.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, CardBorderColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Bot Token:",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                            val maskedToken = if (user.botToken.length > 12) {
                                                "${user.botToken.take(7)}...${user.botToken.takeLast(4)}"
                                            } else if (user.botToken.isNotEmpty()) "••••••••" else "Not set"
                                            Text(
                                                text = maskedToken,
                                                fontFamily = FontFamily.Monospace,
                                                color = TextPrimary,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Chat ID:",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = user.chatId.ifEmpty { "Not set" },
                                                fontFamily = FontFamily.Monospace,
                                                color = TextPrimary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Verification status row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val statusColor = if (user.isVerified) NeonGreen else TextGold
                                    val statusIcon = if (user.isVerified) Icons.Default.CheckCircle else Icons.Default.Info

                                    Icon(
                                        imageVector = statusIcon,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (user.isVerified) "Verified & Active" else "Needs Check",
                                        color = statusColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (user.lastChecked > 0L) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val timeStr = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
                                            .format(java.util.Date(user.lastChecked))
                                        Text(
                                            text = "• checked $timeStr",
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }

                                    if (user.botToken.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• Upload Sharing Active",
                                            color = NeonCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                if (user.checkStatusMessage.isNotEmpty() && user.checkStatusMessage != "Not checked yet") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = user.checkStatusMessage,
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Action Buttons Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // "Check it" / Verify Credentials Button
                                    Button(
                                        onClick = {
                                            viewModel.checkUserCredentials(user) { success, msg ->
                                                checkResultMessage = msg
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (user.isVerified) NeonGreen.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (user.isVerified) NeonGreen else NeonCyan
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("check_credentials_${user.id}"),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        if (isChecking) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 1.5.dp,
                                                color = NeonCyan
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "CHECKING...",
                                                color = NeonCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (user.isVerified) Icons.Default.DoneAll else Icons.Default.Refresh,
                                                contentDescription = "Check it",
                                                tint = if (user.isVerified) NeonGreen else NeonCyan,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (user.isVerified) "RE-CHECK" else "CHECK IT",
                                                color = if (user.isVerified) NeonGreen else NeonCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    // Switch to profile button
                                    if (!isActive) {
                                        Button(
                                            onClick = {
                                                viewModel.switchActiveUser(user.id)
                                                Toast.makeText(context, "Switched to '${user.name}'", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).testTag("switch_user_${user.id}"),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = CosmicBackground,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "USE ACCOUNT",
                                                color = CosmicBackground,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Add Account Button
                Button(
                    onClick = { showAddUserDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_user_profile_button")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add User",
                        tint = CosmicBackground,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "ADD NEW USER PROFILE",
                        color = CosmicBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }

    // Add User Dialog
    if (showAddUserDialog) {
        UserEditDialog(
            title = "Add User Profile",
            initialName = "",
            initialToken = "",
            initialChatId = "",
            initialColor = "#C85A32",
            initialRole = "Personal",
            onDismiss = { showAddUserDialog = false },
            onSave = { name, token, chatId, color, role, checkNow ->
                viewModel.addUser(name, token, chatId, color, role)
                val added = viewModel.users.value.lastOrNull()
                if (checkNow && added != null) {
                    viewModel.checkUserCredentials(added) { _, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
                showAddUserDialog = false
                Toast.makeText(context, "Added user profile '$name'", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit User Dialog
    userToEdit?.let { editingUser ->
        UserEditDialog(
            title = "Edit User Profile",
            initialName = editingUser.name,
            initialToken = editingUser.botToken,
            initialChatId = editingUser.chatId,
            initialColor = editingUser.avatarColorHex,
            initialRole = editingUser.role,
            onDismiss = { userToEdit = null },
            onSave = { name, token, chatId, color, role, checkNow ->
                val updated = editingUser.copy(
                    name = name,
                    botToken = token,
                    chatId = chatId,
                    avatarColorHex = color,
                    role = role
                )
                viewModel.updateUser(updated)
                if (checkNow) {
                    viewModel.checkUserCredentials(updated) { _, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
                userToEdit = null
                Toast.makeText(context, "Updated profile '$name'", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun UserEditDialog(
    title: String,
    initialName: String,
    initialToken: String,
    initialChatId: String,
    initialColor: String,
    initialRole: String,
    onDismiss: () -> Unit,
    onSave: (name: String, token: String, chatId: String, color: String, role: String, checkNow: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var token by remember { mutableStateOf(initialToken) }
    var chatId by remember { mutableStateOf(initialChatId) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedRole by remember { mutableStateOf(initialRole) }
    var hideToken by remember { mutableStateOf(true) }

    val roles = listOf("Personal", "Work", "Family", "Archive")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CardBorderColor, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = CosmicSlate),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Account Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Work Vault, Family Photos", fontSize = 11.sp, color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderColor,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("user_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Bot Token
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = TelegramConfigManager.sanitizeBotToken(it) },
                    label = { Text("Telegram Bot Token", fontSize = 12.sp) },
                    placeholder = { Text("123456789:ABCdefGh...", fontSize = 11.sp, color = TextSecondary) },
                    singleLine = true,
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
                    modifier = Modifier.fillMaxWidth().testTag("user_token_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Chat ID
                OutlinedTextField(
                    value = chatId,
                    onValueChange = { chatId = TelegramConfigManager.sanitizeChatId(it) },
                    label = { Text("Telegram Chat ID", fontSize = 12.sp) },
                    placeholder = { Text("e.g. -100123456789", fontSize = 11.sp, color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderColor,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("user_chat_id_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Role Selector Chips
                Text("Account Purpose:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    roles.forEach { role ->
                        val isSelected = selectedRole == role
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else CosmicGlass,
                            border = BorderStroke(1.dp, if (isSelected) NeonCyan else CardBorderColor),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRole = role }
                        ) {
                            Text(
                                text = role,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color Picker Swatches
                Text("Avatar Color:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HumanAvatarColors.forEach { hex ->
                        val isSelected = selectedColor == hex
                        val color = parseHexColor(hex)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) Color.White else CardBorderColor,
                                    CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CardBorderColor)
                    ) {
                        Text("CANCEL", color = TextSecondary, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val finalName = name.trim().ifEmpty { "Vault Profile" }
                            onSave(finalName, token.trim(), chatId.trim(), selectedColor, selectedRole, true)
                        },
                        modifier = Modifier.weight(1.3f).testTag("save_user_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CosmicBackground, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CHECK & SAVE", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

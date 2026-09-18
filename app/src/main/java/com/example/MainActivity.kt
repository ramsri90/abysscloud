package com.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.work.WorkInfo
import com.example.data.SyncState
import com.example.ui.MediaItem
import com.example.data.VaultItem
import com.example.ui.VaultViewModel
import com.example.ui.VaultViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import java.io.File
import java.text.DecimalFormat
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.fragment.app.FragmentActivity
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this, VaultViewModelFactory(this))[VaultViewModel::class.java]

        val imageLoader = coil.ImageLoader.Builder(this)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.35)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.15)
                    .build()
            }
            .components {
                add(coil.decode.VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .allowHardware(true)
            .respectCacheHeaders(false)
            .build()
        coil.Coil.setImageLoader(imageLoader)

        setContent {
            val darkModeMode by viewModel.darkModeMode.collectAsStateWithLifecycle()
            val isSystemInDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDark = when (darkModeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDark
            }

            MyApplicationTheme(darkTheme = isDark) {
                AbyssCloudApp(viewModel = viewModel, isDarkTheme = isDark)
            }
        }
    }
}

// Dynamic Tech Stack Themes
data class AppThemeColors(
    val darkBackground: Color,
    val darkSlate: Color,
    val darkGlass: Color,
    val lightBackground: Color,
    val lightSlate: Color,
    val lightGlass: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val secondaryDark: Color,
    val secondaryLight: Color,
    val accent: Color,
    val error: Color,
    val highlight: Color,
    val themeName: String
)

val ThemesList = listOf(
    // Theme 0: Translucent Glass (Modern iOS/macOS Aesthetic with Deep Navy Slate & Telegram Blue / Mint Green)
    AppThemeColors(
        darkBackground = Color(0xFF070B19), // Pitch-black cosmic abyss with subtle dark neon-blue undertone
        darkSlate = Color(0xFF12162E),      // Deep modern space slate
        darkGlass = Color(0x9912162E),      // Translucent blurred dark glass
        lightBackground = Color(0xFFEDF1FD),// Soft frosted ice-blue
        lightSlate = Color(0xFFFFFFFF),
        lightGlass = Color(0xCCFFFFFF),
        primaryDark = Color(0xFF20C8FF),     // Neon Cyan/Sky Blue from Logo
        primaryLight = Color(0xFF3857FF),    // Vibrant Electric Blue from Logo
        secondaryDark = Color(0xFFD21FF0),   // Vibrant Neon Magenta/Pink from Logo
        secondaryLight = Color(0xFF6850FF),  // Vibrant Purple from Logo
        accent = Color(0xFF20C8FF),          // Neon Cyan Accent
        error = Color(0xFFFF3B30),           // Coral Red
        highlight = Color(0xFF6850FF),       // Vibrant Purple highlight
        themeName = "Abyss Electric"
    ),
    // Theme 1: AMOLED Abyss (True OLED Black & Abyss Neon Accents)
    AppThemeColors(
        darkBackground = Color(0xFF000000), // True 100% OLED Pitch Black
        darkSlate = Color(0xFF0D0E1A),      // Deep AMOLED container with subtle blue undertone
        darkGlass = Color(0x990D0E1A),      // Translucent deep AMOLED glass
        lightBackground = Color(0xFFFAFAFC),
        lightSlate = Color(0xFFFFFFFF),
        lightGlass = Color(0xFFF0F0F4),
        primaryDark = Color(0xFF20C8FF),     // Neon Cyan/Sky Blue from Logo
        primaryLight = Color(0xFF3857FF),    // Vibrant Electric Blue from Logo
        secondaryDark = Color(0xFFD21FF0),   // Vibrant Neon Magenta/Pink from Logo
        secondaryLight = Color(0xFF6850FF),  // Vibrant Purple from Logo
        accent = Color(0xFF20C8FF),          // Neon Cyan Accent
        error = Color(0xFFFF355E),           // Neon Red Alert
        highlight = Color(0xFF6850FF),       // Vibrant Purple Highlight
        themeName = "AMOLED Abyss"
    ),
    // Theme 2: AMOLED White Neon (Ultra Dark Pitch Black & Crisp Neon White Accent)
    AppThemeColors(
        darkBackground = Color(0xFF000000), // True 100% OLED Pitch Black
        darkSlate = Color(0xFF0D0D0E),      // Deep AMOLED black container
        darkGlass = Color(0xFF161618),      // Deep AMOLED glass card
        lightBackground = Color(0xFFFAFAFC),
        lightSlate = Color(0xFFFFFFFF),
        lightGlass = Color(0xFFF0F0F4),
        primaryDark = Color(0xFFFFFFFF),     // Ultra Crisp White Neon
        primaryLight = Color(0xFF000000),    // Deep Black
        secondaryDark = Color(0xFFE2E8F0),   // Platinum White Neon
        secondaryLight = Color(0xFF334155),  // Slate
        accent = Color(0xFFFFFFFF),          // White Neon Accent
        error = Color(0xFFFF355E),           // Neon Red Alert
        highlight = Color(0xFFF8FAFC),       // Radiant White Highlight
        themeName = "AMOLED White Neon"
    ),
    // Theme 3: AMOLED Neon Red (Ultra Dark OLED Pitch Black & Electric Neon Red Accent)
    AppThemeColors(
        darkBackground = Color(0xFF000000), // True 100% OLED Pitch Black
        darkSlate = Color(0xFF0A0A0C),      // Deep AMOLED slate container
        darkGlass = Color(0xFF141416),      // Glass card with pitch black blend
        lightBackground = Color(0xFFFAF7F7),
        lightSlate = Color(0xFFFFFFFF),
        lightGlass = Color(0xFFF5EEEE),
        primaryDark = Color(0xFFFF1E42),     // Electric Vibrant Neon Red
        primaryLight = Color(0xFFD6002A),    // Crimson Red
        secondaryDark = Color(0xFFFF4D6D),   // Neon Coral Rose
        secondaryLight = Color(0xFF9E0D2A),  // Deep Wine
        accent = Color(0xFFFF1744),          // Pure Neon Red
        error = Color(0xFFFF334B),           // Neon Red Alert
        highlight = Color(0xFFFF5252),       // Radiant Red Accent
        themeName = "AMOLED Neon Red"
    ),
    // Theme 4: Warm Terracotta (Earthy Human Clay / Warm Linen)
    AppThemeColors(
        darkBackground = Color(0xFF181615),
        darkSlate = Color(0xFF221F1D),
        darkGlass = Color(0xFF2C2724),
        lightBackground = Color(0xFFFAF7F2),
        lightSlate = Color(0xFFFFFFFF),
        lightGlass = Color(0xFFF3EFEA),
        primaryDark = Color(0xFFE07A5F),
        primaryLight = Color(0xFFC85A32),
        secondaryDark = Color(0xFFDDA18E),
        secondaryLight = Color(0xFF8D5B4C),
        accent = Color(0xFF2E7D32),
        error = Color(0xFFC2410C),
        highlight = Color(0xFFD97706),
        themeName = "Warm Terracotta"
    ),
    // Theme 5: Sage Botanical (Deep Herbal Sage / Fresh Almond)
    AppThemeColors(
        darkBackground = Color(0xFF141815),
        darkSlate = Color(0xFF1B211D),
        darkGlass = Color(0xFF242C27),
        lightBackground = Color(0xFFF5F7F4),
        lightSlate = Color(0xFFFFFFFF),
        lightGlass = Color(0xFFEAEFEA),
        primaryDark = Color(0xFF6EA284),
        primaryLight = Color(0xFF3E6B52),
        secondaryDark = Color(0xFF97BDB0),
        secondaryLight = Color(0xFF5B7B68),
        accent = Color(0xFF2E7D32),
        error = Color(0xFFC2410C),
        highlight = Color(0xFFB45309),
        themeName = "Sage Botanical"
    ),
    // Theme 6: Sand & Amber (Desert Sandstone / Warm Amber)
    AppThemeColors(
        darkBackground = Color(0xFF161412),
        darkSlate = Color(0xFF211D1A),
        darkGlass = Color(0xFF2D2723),
        lightBackground = Color(0xFFFAF8F5),
        lightSlate = Color(0xFFFFFFFF),
        lightGlass = Color(0xFFF3EDE4),
        primaryDark = Color(0xFFF59E0B),
        primaryLight = Color(0xFFB45309),
        secondaryDark = Color(0xFFFCD34D),
        secondaryLight = Color(0xFF92400E),
        accent = Color(0xFF3B6B55),
        error = Color(0xFFC2410C),
        highlight = Color(0xFFD97706),
        themeName = "Sand & Amber"
    ),
    // Theme 7: Warm Espresso (Roasted Espresso / Cashmere Cream)
    AppThemeColors(
        darkBackground = Color(0xFF171413),
        darkSlate = Color(0xFF231F1D),
        darkGlass = Color(0xFF302A27),
        lightBackground = Color(0xFFFAF8F6),
        lightSlate = Color(0xFFFFFFFF),
        lightGlass = Color(0xFFF1ECE7),
        primaryDark = Color(0xFFD7CCC8),
        primaryLight = Color(0xFF5D4037),
        secondaryDark = Color(0xFFBCAAA4),
        secondaryLight = Color(0xFF8D6E63),
        accent = Color(0xFFC85A32),
        error = Color(0xFFC2410C),
        highlight = Color(0xFFD97706),
        themeName = "Warm Espresso"
    )
)

var currentThemeIndexState = androidx.compose.runtime.mutableStateOf(0)
var currentIsDarkThemeState = androidx.compose.runtime.mutableStateOf(true)

val isAppInDarkTheme: Boolean get() = currentIsDarkThemeState.value

val CosmicBackground: Color
    get() {
        val theme = ThemesList.getOrElse(currentThemeIndexState.value) { ThemesList[0] }
        return if (isAppInDarkTheme) theme.darkBackground else theme.lightBackground
    }

val CosmicSlate: Color
    get() {
        val theme = ThemesList.getOrElse(currentThemeIndexState.value) { ThemesList[0] }
        return if (isAppInDarkTheme) theme.darkSlate else theme.lightSlate
    }

val CosmicGlass: Color
    get() {
        val theme = ThemesList.getOrElse(currentThemeIndexState.value) { ThemesList[0] }
        return if (isAppInDarkTheme) theme.darkGlass else theme.lightGlass
    }

val NeonCyan: Color
    get() {
        val theme = ThemesList.getOrElse(currentThemeIndexState.value) { ThemesList[0] }
        return if (isAppInDarkTheme) theme.primaryDark else theme.primaryLight
    }

val NeonPurple: Color
    get() {
        val theme = ThemesList.getOrElse(currentThemeIndexState.value) { ThemesList[0] }
        return if (isAppInDarkTheme) theme.secondaryDark else theme.secondaryLight
    }

val NeonGreen: Color
    get() {
        val theme = ThemesList.getOrElse(currentThemeIndexState.value) { ThemesList[0] }
        return if (theme.themeName.contains("AMOLED", ignoreCase = true)) Color(0xFF00E676) else theme.accent
    }

val SoftCoral: Color
    get() = ThemesList.getOrElse(currentThemeIndexState.value) { ThemesList[0] }.error

val TextGold: Color
    get() {
        val theme = ThemesList.getOrElse(currentThemeIndexState.value) { ThemesList[0] }
        return if (theme.themeName.contains("AMOLED", ignoreCase = true)) Color(0xFFFFD54F) else theme.highlight
    }

val TextPrimary: Color
    get() {
        val theme = ThemesList.getOrElse(currentThemeIndexState.value) { ThemesList[0] }
        return if (isAppInDarkTheme) {
            if (theme.themeName.contains("AMOLED", ignoreCase = true)) Color(0xFFFFFFFF)
            else if (theme.themeName.contains("Translucent Glass", ignoreCase = true)) Color(0xFFF8FAFC)
            else Color(0xFFFAF7F2)
        } else {
            if (theme.themeName.contains("Translucent Glass", ignoreCase = true)) Color(0xFF0F172A)
            else Color(0xFF1E1B18)
        }
    }

val TextSecondary: Color
    get() {
        val theme = ThemesList.getOrElse(currentThemeIndexState.value) { ThemesList[0] }
        return if (isAppInDarkTheme) {
            if (theme.themeName.contains("AMOLED", ignoreCase = true)) Color(0xFFA0A0A5)
            else if (theme.themeName.contains("Translucent Glass", ignoreCase = true)) Color(0xFF94A3B8)
            else Color(0xFFA8A19B)
        } else {
            if (theme.themeName.contains("Translucent Glass", ignoreCase = true)) Color(0xFF64748B)
            else Color(0xFF6E6761)
        }
    }

val CardBorderColor: Color
    get() {
        val theme = ThemesList.getOrElse(currentThemeIndexState.value) { ThemesList[0] }
        return if (isAppInDarkTheme) {
            if (theme.themeName.contains("AMOLED", ignoreCase = true)) Color(0xFF261014)
            else if (theme.themeName.contains("Translucent Glass", ignoreCase = true)) Color(0x3338BDF8)
            else Color(0xFF3D3733)
        } else {
            if (theme.themeName.contains("Translucent Glass", ignoreCase = true)) Color(0x330284C7)
            else Color(0xFFE5DFD7)
        }
    }


@Composable
fun AbyssCloudApp(viewModel: VaultViewModel, isDarkTheme: Boolean = true) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("dashboard") }
    val themeIndex by viewModel.themeIndex.collectAsStateWithLifecycle()

    androidx.compose.runtime.SideEffect {
        currentThemeIndexState.value = themeIndex
        currentIsDarkThemeState.value = isDarkTheme
    }

    var showApp by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var authRetryTrigger by remember { mutableIntStateOf(0) }

    val authenticateWithBiometrics = rememberUpdatedState {
        val biometricManager = androidx.biometric.BiometricManager.from(context)
        val authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or 
            androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            
        when (biometricManager.canAuthenticate(authenticators)) {
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
                val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Vault Security")
                    .setSubtitle("Authenticate to access Abyss Cloud")
                    .setAllowedAuthenticators(authenticators)
                    .build()

                val activity = context as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    val biometricPrompt = androidx.biometric.BiometricPrompt(
                        activity, executor,
                        object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                val friendlyMsg = when (errorCode) {
                                    androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED,
                                    androidx.biometric.BiometricPrompt.ERROR_CANCELED,
                                    androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON ->
                                        "Fingerprint prompt was closed. Tap 'TRY NOW' to unlock."
                                    androidx.biometric.BiometricPrompt.ERROR_LOCKOUT,
                                    androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                                        "Biometric attempts exceeded. Unlock using your device PIN or password."
                                    else -> "Authentication required: $errString"
                                }
                                authError = friendlyMsg
                            }

                            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                authError = null
                                showApp = true
                            }

                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                authError = "Fingerprint not recognized. Tap 'TRY NOW' to try again."
                            }
                        })
                    biometricPrompt.authenticate(promptInfo)
                } else {
                    showApp = true
                }
            }
            else -> {
                showApp = true
            }
        }
    }

    LaunchedEffect(authRetryTrigger) {
        if (!showApp) {
            authenticateWithBiometrics.value()
        }
    }

    if (!showApp) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground)
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = CosmicSlate),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.12f))
                            .border(1.5.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint Security",
                            tint = NeonCyan,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Abyss Cloud Vault Locked",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = authError ?: "Biometric or device screen lock authentication is required to access your media vault.",
                        fontSize = 13.sp,
                        color = if (authError != null) SoftCoral else TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            authError = null
                            authRetryTrigger++
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_try_now_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = CosmicBackground
                        )
                    ) {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TRY NOW",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
        return
    }

    LaunchedEffect(themeIndex, isDarkTheme) {
        currentThemeIndexState.value = themeIndex
        currentIsDarkThemeState.value = isDarkTheme
    }

    var showSetupDialog by remember { mutableStateOf(!viewModel.configManager.isConfigured()) }
    var showMultiUserManager by remember { mutableStateOf(false) }

    if (showSetupDialog) {
        com.example.ui.SetupDialog(viewModel) {
            showSetupDialog = false
        }
    }

    if (showMultiUserManager) {
        com.example.ui.MultiUserManagerDialog(viewModel) {
            showMultiUserManager = false
        }
    }

    Scaffold(
        topBar = {
            val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
            val isFetchingFromTelegram by viewModel.isFetchingFromTelegram.collectAsStateWithLifecycle()
            val isFreeingStorage by viewModel.isFreeingStorage.collectAsStateWithLifecycle()
            val avatarColor = com.example.ui.parseHexColor(activeUser.avatarColorHex)

            val infiniteTransition = rememberInfiniteTransition(label = "HeaderSyncSpinner")
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "HeaderSpinnerRotation"
            )

            Surface(
                color = CosmicSlate,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, CardBorderColor))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isFetchingFromTelegram) NeonCyan.copy(alpha = 0.25f) else NeonCyan.copy(alpha = 0.15f))
                                .border(
                                    1.dp, 
                                    if (isFetchingFromTelegram) NeonCyan else NeonCyan.copy(alpha = 0.3f), 
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFetchingFromTelegram) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = "Syncing",
                                    tint = NeonCyan,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(rotationAngle)
                                )
                            } else {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_abyss_logo),
                                    contentDescription = "Abyss Cloud Logo",
                                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Abyss Cloud",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.3.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isFetchingFromTelegram) "Re-syncing Telegram..." else "Private Cloud Vault",
                                    color = if (isFetchingFromTelegram) NeonCyan else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isFetchingFromTelegram) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (isFetchingFromTelegram) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(10.dp),
                                        strokeWidth = 1.2.dp,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }

                    // Active User Profile Pill (1-tap to switch profiles or check credentials)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CosmicGlass,
                        border = BorderStroke(1.dp, if (activeUser.isVerified) NeonGreen.copy(alpha = 0.5f) else CardBorderColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showMultiUserManager = true }
                            .testTag("user_profile_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeUser.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeUser.name,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 100.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if (activeUser.isVerified) NeonGreen else TextGold)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Account",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = CosmicGlass,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NavigationBarItem(
                            selected = currentTab == "dashboard",
                            onClick = { currentTab = "dashboard" },
                            icon = { Icon(Icons.Default.PermMedia, contentDescription = "Media") },
                            label = { Text("Media", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = NeonCyan.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier.testTag("nav_photos")
                        )
                        NavigationBarItem(
                            selected = currentTab == "device_sync",
                            onClick = { currentTab = "device_sync" },
                            icon = { Icon(Icons.Default.Sync, contentDescription = "Device Sync") },
                            label = { Text("Device Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = NeonCyan.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier.testTag("nav_device_sync")
                        )
                        NavigationBarItem(
                            selected = currentTab == "queue",
                            onClick = { currentTab = "queue" },
                            icon = { Icon(Icons.Default.List, contentDescription = "Tasks") },
                            label = { Text("Sync Queue", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = NeonCyan.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier.testTag("nav_queue")
                        )
                        NavigationBarItem(
                            selected = currentTab == "settings",
                            onClick = { currentTab = "settings" },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = NeonCyan.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        },
        containerColor = CosmicBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isAppInDarkTheme) {
                        Brush.verticalGradient(
                            colors = listOf(
                                CosmicBackground,
                                CosmicSlate,
                                CosmicBackground
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                CosmicBackground,
                                CosmicGlass,
                                CosmicBackground
                            )
                        )
                    }
                )
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "dashboard" -> DashboardScreen(viewModel)
                "device_sync" -> DeviceSyncScreen(viewModel)
                "queue" -> QueueScreen(viewModel)
                "restore" -> RestoreScreen(viewModel)
                "settings" -> SettingsScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: VaultViewModel) {
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val syncedCount by viewModel.syncedCount.collectAsStateWithLifecycle()
    val restoringFileId by viewModel.isRestoringFile.collectAsStateWithLifecycle()
    val isFetchingFromTelegram by viewModel.isFetchingFromTelegram.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingUris by remember { mutableStateOf<List<Uri>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFolderFilter by remember { mutableStateOf("All") }
    var activePreviewImage by remember { mutableStateOf<VaultItem?>(null) }
    var showMoveDialogForSelection by remember { mutableStateOf(false) }

    val selectedItems = remember { androidx.compose.runtime.mutableStateListOf<VaultItem>() }
    val selectionMode = selectedItems.isNotEmpty()

    androidx.activity.compose.BackHandler(enabled = selectionMode) {
        selectedItems.clear()
    }

    val scannedItems by viewModel.scannedMediaItems.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= 33) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(context, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (hasPermission) {
            viewModel.scanDeviceStorage()
        }
    }

    val mergedItems = remember(items, scannedItems) {
        val dbItemsByPath = items.associateBy { it.localPath }
        val dbItemsByFilename = items.groupBy { it.filename.lowercase() }
        val dbItemsByBaseName = items.groupBy { it.filename.substringBeforeLast('.').lowercase() }

        val matchedDbIds = mutableSetOf<String>()
        val resultList = mutableListOf<VaultItem>()

        // 1. Map physical device scanned items to their respective database uploading/synced state (1:1 representation)
        scannedItems.forEach { scanned ->
            // High-precision fallback search to match device file with backup database record
            val scannedBase = scanned.displayName.substringBeforeLast('.').lowercase()
            val dbItem = dbItemsByPath[scanned.uri.toString()]
                ?: dbItemsByPath[scanned.displayName]
                ?: dbItemsByFilename[scanned.displayName.lowercase()]?.firstOrNull()
                ?: dbItemsByBaseName[scannedBase]?.firstOrNull()

            if (dbItem != null) {
                matchedDbIds.add(dbItem.id)
                val finalFolder = if (dbItem.folder == "General" || dbItem.folder.isBlank() || dbItem.folder == "Manual Upload") {
                    if (scanned.bucketName.isNotEmpty()) scanned.bucketName else "General"
                } else {
                    dbItem.folder
                }
                
                resultList.add(
                    dbItem.copy(
                        folder = finalFolder,
                        localPath = scanned.uri.toString(),
                        createdAt = scanned.dateAdded * 1000 // Always use physical device media date as the absolute truth!
                    )
                )
            } else {
                // Completely dynamic local-only placeholder
                resultList.add(
                    VaultItem(
                        id = "scanned_${scanned.id}",
                        localPath = scanned.uri.toString(),
                        filename = scanned.displayName,
                        fileSize = scanned.size,
                        mimeType = scanned.mimeType,
                        syncState = SyncState.PENDING,
                        createdAt = scanned.dateAdded * 1000,
                        folder = scanned.bucketName,
                        tags = ""
                    )
                )
            }
        }

        // 2. Safely append any cloud-only items (database items whose physical files are deleted/not present on device)
        items.forEach { dbItem ->
            if (!matchedDbIds.contains(dbItem.id)) {
                val effectiveCreatedAt = if (dbItem.createdAt > 0L && dbItem.createdAt < System.currentTimeMillis() - 86400000L * 2) {
                    dbItem.createdAt
                } else {
                    val parsed = viewModel.extractDateFromFilename(dbItem.filename)
                    if (parsed != null && parsed > 0L) parsed else dbItem.createdAt
                }
                resultList.add(if (effectiveCreatedAt != dbItem.createdAt) dbItem.copy(createdAt = effectiveCreatedAt) else dbItem)
            }
        }

        resultList
    }

    val folders = remember(mergedItems) {
        val uniqueFolders = mergedItems.map { it.folder }.distinct().toMutableList()
        if (!uniqueFolders.contains("General")) {
            uniqueFolders.add(0, "General")
        }
        val hasUploading = mergedItems.any { it.syncState == SyncState.UPLOADING }
        if (hasUploading) {
            uniqueFolders.add(0, "Uploading")
        }
        uniqueFolders
    }

    val filteredItems = remember(mergedItems, selectedFolderFilter, searchQuery) {
        mergedItems.filter { item ->
            val matchesFolder = when (selectedFolderFilter) {
                "All" -> item.localPath.isNotBlank() || item.syncState == SyncState.SYNCED
                "Photos" -> item.mimeType.startsWith("image/") && (item.localPath.isNotBlank() || item.syncState == SyncState.SYNCED)
                "Videos" -> item.mimeType.startsWith("video/") && (item.localPath.isNotBlank() || item.syncState == SyncState.SYNCED)
                "Synced" -> item.syncState == SyncState.SYNCED
                "Uploading" -> item.syncState == SyncState.UPLOADING
                "Screenshots" -> item.mimeType.startsWith("image/") && (item.filename.contains("Screenshot", ignoreCase = true) || item.folder.equals("Screenshots", ignoreCase = true)) && (item.localPath.isNotBlank() || item.syncState == SyncState.SYNCED)
                else -> item.folder.equals(selectedFolderFilter, ignoreCase = true) && (item.localPath.isNotBlank() || item.syncState == SyncState.SYNCED)
            }
            val matchesSearch = searchQuery.isEmpty() || 
                item.filename.contains(searchQuery, ignoreCase = true) || 
                item.tags.contains(searchQuery, ignoreCase = true)
            matchesFolder && matchesSearch
        }.sortedByDescending { if (it.createdAt > 0) it.createdAt else 0L }
    }

    var cameraPhotoFile by remember { mutableStateOf<File?>(null) }
    var cameraVideoFile by remember { mutableStateOf<File?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            if (!viewModel.configManager.isConfigured()) {
                Toast.makeText(context, "Please set up your Telegram credentials first!", Toast.LENGTH_LONG).show()
            } else {
                val contentResolver = context.contentResolver
                val validUris = mutableListOf<Uri>()
                var oversizedCount = 0
                var unsupportedCount = 0

                uris.forEach { uri ->
                    var size = 0L
                    var name = ""
                    var mime = contentResolver.getType(uri) ?: ""
                    try {
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst()) {
                                if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                                if (nameIdx != -1) name = cursor.getString(nameIdx) ?: ""
                            }
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val isImage = mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
                    val isVideo = mime.startsWith("video/") || ext in listOf("mp4", "mkv", "webm", "3gp", "mov", "avi")

                    if (!isImage && !isVideo) {
                        unsupportedCount++
                    } else if (size > 50L * 1024L * 1024L) {
                        oversizedCount++
                    } else {
                        validUris.add(uri)
                    }
                }

                if (oversizedCount > 0 && unsupportedCount > 0) {
                    Toast.makeText(context, "Skipped $oversizedCount file(s) > 50MB and $unsupportedCount unsupported file(s). App supports images and videos below 50 MB only.", Toast.LENGTH_LONG).show()
                } else if (oversizedCount > 0) {
                    Toast.makeText(context, "Skipped $oversizedCount video/file(s) exceeding 50 MB. Only media below 50 MB is supported.", Toast.LENGTH_LONG).show()
                } else if (unsupportedCount > 0) {
                    Toast.makeText(context, "Skipped $unsupportedCount unsupported file(s). Only images and videos are supported.", Toast.LENGTH_LONG).show()
                }

                if (validUris.isNotEmpty()) {
                    pendingUris = validUris
                }
            }
        }
    }

    var isStackView by remember { mutableStateOf(false) }
    var itemPendingDelete by remember { mutableStateOf<VaultItem?>(null) }
    var showDeleteDialogForSelection by remember { mutableStateOf(false) }
    var pendingBatchDeleteItems by remember { mutableStateOf<List<VaultItem>>(emptyList()) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            if (pendingBatchDeleteItems.isNotEmpty()) {
                viewModel.onLocalFilesDeletedByPermission(pendingBatchDeleteItems)
            }
            Toast.makeText(context, "Local copy deleted from device", Toast.LENGTH_SHORT).show()
        }
        pendingBatchDeleteItems = emptyList()
    }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { uri ->
                pendingUris = listOf(uri)
            }
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            val file = cameraVideoFile
            if (file != null && file.exists()) {
                val size = file.length()
                if (size > 50L * 1024L * 1024L) {
                    file.delete()
                    Toast.makeText(context, "Recorded video exceeds 50 MB limit (${formatFileSize(size)}). Only videos below 50 MB are supported.", Toast.LENGTH_LONG).show()
                } else {
                    cameraUri?.let { uri ->
                        pendingUris = listOf(uri)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isFetchingFromTelegram,
            onRefresh = {
                viewModel.fetchFromTelegramGroup { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                viewModel.scanDeviceStorage()
            },
            modifier = Modifier.fillMaxSize().testTag("gallery_pull_to_refresh"),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = rememberPullToRefreshState(),
                    isRefreshing = isFetchingFromTelegram,
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = NeonCyan,
                    containerColor = CosmicGlass
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
            // Selection overlay or regular header
            androidx.compose.animation.AnimatedVisibility(visible = selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedItems.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close selection", tint = TextPrimary)
                        }
                        Text(
                            "${selectedItems.size} Selected",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row {
                        IconButton(onClick = { showMoveDialogForSelection = true }) {
                            Icon(Icons.Default.Folder, contentDescription = "Move batch", tint = TextPrimary)
                        }
                        IconButton(onClick = {
                            if (selectedItems.isNotEmpty()) {
                                showDeleteDialogForSelection = true
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete batch", tint = SoftCoral)
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = !selectionMode) {
                // Google Photos Inspired Top App Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    val isFetchingFromTelegram by viewModel.isFetchingFromTelegram.collectAsStateWithLifecycle()
                    val isFreeingStorage by viewModel.isFreeingStorage.collectAsStateWithLifecycle()
                    var pendingFreeSpaceItem by remember { mutableStateOf<VaultItem?>(null) }
                    var showSearchDialog by remember { mutableStateOf(false) }
                    
                    val deleteLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult()
                    ) { result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                            pendingFreeSpaceItem?.let { item ->
                                viewModel.onLocalFileDeletedByPermission(item)
                                Toast.makeText(context, "Local copy deleted from device", Toast.LENGTH_SHORT).show()
                            }
                        }
                        pendingFreeSpaceItem = null
                    }

                    // Rearranged Sleek Action Toolbar: Search Pill + Quick Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Media Search Bar Pill
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(CosmicGlass)
                                .border(1.dp, if (searchQuery.isNotEmpty()) NeonCyan else CardBorderColor, RoundedCornerShape(22.dp))
                                .clickable { showSearchDialog = true }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (searchQuery.isNotEmpty()) NeonCyan else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (searchQuery.isNotEmpty()) searchQuery else "Search media, tags...",
                                    color = if (searchQuery.isNotEmpty()) TextPrimary else TextSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = SoftCoral, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // Fetch from Telegram Group Action
                        IconButton(
                            onClick = {
                                viewModel.fetchFromTelegramGroup { success, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(CosmicGlass, CircleShape)
                                .border(1.dp, if (isFetchingFromTelegram) NeonGreen else CardBorderColor, CircleShape)
                                .testTag("fetch_telegram_btn")
                        ) {
                            if (isFetchingFromTelegram) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = NeonGreen,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Fetch Telegram Group",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Free Storage Action
                        IconButton(
                            onClick = {
                                viewModel.freeUpDeviceStorage(
                                    onNeedPermission = { intentSender, itemsToClear ->
                                        pendingBatchDeleteItems = itemsToClear
                                        deleteLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
                                    },
                                    onComplete = { count, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !isFreeingStorage,
                            modifier = Modifier
                                .size(40.dp)
                                .background(CosmicGlass, CircleShape)
                                .border(1.dp, if (isFreeingStorage) NeonPurple else CardBorderColor, CircleShape)
                                .testTag("free_storage_btn")
                        ) {
                            if (isFreeingStorage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = NeonPurple,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = "Free Storage",
                                    tint = NeonPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Theme Mode Toggle
                        val darkModeMode by viewModel.darkModeMode.collectAsStateWithLifecycle()
                        IconButton(
                            onClick = {
                                val nextMode = (darkModeMode + 1) % 3
                                viewModel.setDarkModeMode(nextMode)
                                val msg = when (nextMode) {
                                    1 -> "Theme Mode: Light"
                                    2 -> "Theme Mode: Dark"
                                    else -> "Theme Mode: System Default"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(CosmicGlass, CircleShape)
                                .border(1.dp, CardBorderColor, CircleShape)
                                .testTag("theme_toggle_btn")
                        ) {
                            Icon(
                                imageVector = when (darkModeMode) {
                                    1 -> Icons.Default.WbSunny
                                    2 -> Icons.Default.NightsStay
                                    else -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = "Toggle Theme Mode",
                                tint = TextGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (showSearchDialog) {
                        var tempSearchInput by remember { mutableStateOf(searchQuery) }
                        AlertDialog(
                            onDismissRequest = { showSearchDialog = false },
                            containerColor = CosmicSlate,
                            title = {
                                Text(
                                    "SEARCH MEDIA",
                                    color = NeonCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        "Enter media or keyword to search:",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    OutlinedTextField(
                                        value = tempSearchInput,
                                        onValueChange = { tempSearchInput = it },
                                        placeholder = { Text("e.g. vacation, video, selfie, sunset...", color = TextSecondary, fontSize = 12.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonCyan,
                                            unfocusedBorderColor = CardBorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("search_dialog_input")
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        searchQuery = tempSearchInput
                                        showSearchDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Text("Apply", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            },
                            dismissButton = {
                                Row {
                                    if (searchQuery.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                searchQuery = ""
                                                tempSearchInput = ""
                                                showSearchDialog = false
                                            }
                                        ) {
                                            Text("Clear", color = SoftCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    TextButton(onClick = { showSearchDialog = false }) {
                                        Text("Cancel", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Apple-Style Translucent Glass Segmented Control Categories Selector
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val specialCategories = listOf(
                    "All" to "All Media",
                    "Photos" to "Photos",
                    "Videos" to "Videos",
                    "Synced" to "Synced",
                    "Screenshots" to "Screenshots"
                )
                
                items(specialCategories) { (filterId, label) ->
                    val isSelected = selectedFolderFilter == filterId
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else CosmicGlass,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NeonCyan else CardBorderColor
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedFolderFilter = filterId }
                            .testTag("filter_$filterId")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (filterId == "Synced") NeonGreen else NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = label,
                                color = if (isSelected) (if (filterId == "Synced") NeonGreen else NeonCyan) else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                items(folders) { folder ->
                    val isSelected = selectedFolderFilter == folder
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else CosmicGlass,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NeonCyan else CardBorderColor
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedFolderFilter = folder }
                            .testTag("filter_folder_$folder")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = folder,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Modern Cloud Backup Progress Banner
            if (totalCount > 0) {
                val percent = if (totalCount > 0) (syncedCount * 100 / totalCount) else 0
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicGlass)
                        .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "Cloud Status",
                                tint = if (percent == 100) NeonGreen else NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "CLOUD PROTECTION STATUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (percent == 100) NeonGreen else TextPrimary
                            )
                        }
                        Text(
                            text = "$syncedCount / $totalCount backed up ($percent%)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (percent == 100) NeonGreen else NeonCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Custom Sleek Linear Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = (percent.toFloat() / 100f).coerceIn(0f, 1f))
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = if (percent == 100) {
                                            listOf(NeonGreen, NeonGreen.copy(alpha = 0.8f))
                                        } else {
                                            listOf(NeonCyan, NeonPurple)
                                        }
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid/Stack Section Title, Count & View Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildString {
                            if (selectedFolderFilter != "All") {
                                append("Folder: $selectedFolderFilter")
                            }
                            if (searchQuery.isNotEmpty()) {
                                if (isNotEmpty()) {
                                    append(" • ")
                                }
                                append("Search: \"$searchQuery\"")
                            }
                            if (isEmpty()) {
                                append("All Photos & Media")
                            }
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.clickable(enabled = searchQuery.isNotEmpty()) { searchQuery = "" }
                    )
                    Text(
                        text = "${filteredItems.size} items",
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                }

                // Stack vs Grid Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmicGlass)
                        .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isStackView) NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { isStackView = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ViewStream,
                                contentDescription = "Stack View",
                                tint = if (isStackView) NeonCyan else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Stack",
                                fontSize = 11.sp,
                                fontWeight = if (isStackView) FontWeight.Bold else FontWeight.Normal,
                                color = if (isStackView) NeonCyan else TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isStackView) NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { isStackView = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = "Grid View",
                                tint = if (!isStackView) NeonCyan else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Grid",
                                fontSize = 11.sp,
                                fontWeight = if (!isStackView) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isStackView) NeonCyan else TextSecondary
                            )
                        }
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmptyStateView { filePickerLauncher.launch(arrayOf("image/*", "video/*")) }
                }
            } else if (isStackView) {
                // Stack View: High-visibility stacked stream of media cards
                val groupedItems = remember(filteredItems) {
                    val sdf = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault())
                    val cache = mutableMapOf<Long, String>()
                    filteredItems.groupBy { item ->
                        val dayIndex = item.createdAt / 86400000L
                        cache.getOrPut(dayIndex) {
                            sdf.format(java.util.Date(item.createdAt))
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    groupedItems.forEach { (dateGroup, itemsInGroup) ->
                        item(key = "header_$dateGroup") {
                            Text(
                                text = dateGroup.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(itemsInGroup, key = { it.id }) { item ->
                            val isSelected = selectedItems.contains(item)
                            VaultItemStackedCard(
                                item = item,
                                viewModel = viewModel,
                                onRetry = { viewModel.retryUpload(item) },
                                onDelete = { itemPendingDelete = item },
                                onRestore = {
                                    viewModel.downloadAndRestoreFile(item) { success, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                },
                                isRestoring = restoringFileId == item.id,
                                onImagePreviewClick = { activePreviewImage = item },
                                onShare = { shareVaultItem(context, item, viewModel) },
                                selectionMode = selectionMode,
                                selected = isSelected,
                                onSelectClick = {
                                    if (isSelected) selectedItems.remove(item) else selectedItems.add(item)
                                },
                                onLongPress = {
                                    if (!selectionMode) selectedItems.add(item)
                                }
                            )
                        }
                    }
                }
            } else {
                val groupedItems = remember(filteredItems) {
                    val sdf = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault())
                    val cache = mutableMapOf<Long, String>()
                    filteredItems.groupBy { item ->
                        val dayIndex = item.createdAt / 86400000L
                        cache.getOrPut(dayIndex) {
                            sdf.format(java.util.Date(item.createdAt))
                        }
                    }
                }

                // Modern Google Photos Inspired Date-wise Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    groupedItems.forEach { (dateGroup, itemsInDay) ->
                        item(span = { GridItemSpan(3) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, top = 14.dp, bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateGroup.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                
                                val unbackedUp = itemsInDay.filter { it.syncState != SyncState.SYNCED }
                                if (unbackedUp.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(NeonCyan.copy(alpha = 0.15f))
                                            .border(0.5.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                unbackedUp.forEach { item ->
                                                    viewModel.retryUpload(item)
                                                }
                                                Toast.makeText(context, "Safeguarding ${unbackedUp.size} items from $dateGroup", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "BACKUP DAY",
                                            color = NeonCyan,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                        items(itemsInDay, key = { it.id }) { item ->
                            val isSelected = selectedItems.contains(item)
                            ModernGalleryGridItem(
                                item = item,
                                viewModel = viewModel,
                                selected = isSelected,
                                selectionMode = selectionMode,
                                onSelectClick = {
                                    if (isSelected) {
                                        selectedItems.remove(item)
                                    } else {
                                        selectedItems.add(item)
                                    }
                                },
                                onLongPress = {
                                    if (!selectionMode) {
                                        selectedItems.add(item)
                                    }
                                },
                                onPreviewClick = { activePreviewImage = item }
                            )
                        }
                    }
                }
            }
        }
    }

        // Add File Floating Button
        var isFabExpanded by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(visible = isFabExpanded) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            isFabExpanded = false
                            val televaultDir = File(context.filesDir, "televault").apply { mkdirs() }
                            val tempFile = File(televaultDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                            cameraPhotoFile = tempFile
                            cameraUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                            cameraUri?.let { cameraLauncher.launch(it) }
                        },
                        containerColor = CosmicGlass,
                        contentColor = NeonCyan,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo")
                    }
                    FloatingActionButton(
                        onClick = {
                            isFabExpanded = false
                            val televaultDir = File(context.filesDir, "televault").apply { mkdirs() }
                            val tempFile = File(televaultDir, "camera_video_${System.currentTimeMillis()}.mp4")
                            cameraVideoFile = tempFile
                            cameraUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                            cameraUri?.let { videoLauncher.launch(it) }
                        },
                        containerColor = CosmicGlass,
                        contentColor = NeonCyan,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Record Video")
                    }
                    FloatingActionButton(
                        onClick = {
                            isFabExpanded = false
                            filePickerLauncher.launch(arrayOf("image/*", "video/*"))
                        },
                        containerColor = CosmicGlass,
                        contentColor = NeonCyan,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Upload Media (<50MB)")
                    }
                }
            }

            FloatingActionButton(
                onClick = { isFabExpanded = !isFabExpanded },
                containerColor = if (isFabExpanded) NeonPurple else NeonCyan,
                contentColor = CosmicBackground,
                modifier = Modifier.testTag("vault_fab")
            ) {
                Icon(if (isFabExpanded) Icons.Default.Close else Icons.Default.Add, contentDescription = "Add Menu")
            }
        }
    }

    // Modal Add Setup
    pendingUris?.let { uris ->
        if (uris.size == 1) {
            AddVaultItemDialog(
                uri = uris[0],
                folders = folders,
                onDismiss = { pendingUris = null },
                onConfirm = { customName, folder ->
                    viewModel.addVaultItem(uris[0], customName, folder)
                    pendingUris = null
                    Toast.makeText(context, "Added to folder '$folder'. Processing registry entry...", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            AddMultipleVaultItemsDialog(
                uris = uris,
                folders = folders,
                onDismiss = { pendingUris = null },
                onConfirm = { folder ->
                    uris.forEach { uri ->
                        viewModel.addVaultItem(uri, null, folder)
                    }
                    pendingUris = null
                    Toast.makeText(context, "Added ${uris.size} items to folder '$folder'. Processing registry entries...", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Modal Image Preview
    if (showMoveDialogForSelection) {
        MoveMultipleItemsDialog(
            itemCount = selectedItems.size,
            folders = folders,
            onDismiss = { showMoveDialogForSelection = false },
            onConfirm = { newFolder ->
                selectedItems.forEach { item ->
                    viewModel.moveItemToFolder(item, newFolder)
                }
                showMoveDialogForSelection = false
                selectedItems.clear()
                Toast.makeText(context, "Items moved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    activePreviewImage?.let { item ->
        var showMoveDialog by remember { mutableStateOf(false) }
        var pendingDeleteVaultItem by remember { mutableStateOf<VaultItem?>(null) }
        val deleteLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                pendingDeleteVaultItem?.let { itemToDelete ->
                    viewModel.onLocalFileDeletedByPermission(itemToDelete)
                    Toast.makeText(context, "Local copy deleted from device", Toast.LENGTH_SHORT).show()
                }
            }
            pendingDeleteVaultItem = null
            activePreviewImage = null
        }

        var isRestoringActiveItem by remember { mutableStateOf(false) }

        FullscreenPhotoViewerDialog(
            item = item,
            allGalleryItems = filteredItems,
            viewModel = viewModel,
            onSelectItem = { newActiveItem ->
                activePreviewImage = newActiveItem
            },
            onDismiss = { activePreviewImage = null },
            onShare = {
                shareVaultItem(context, item, viewModel)
            },
            onDelete = {
                itemPendingDelete = item
            },
            onMoveClick = {
                showMoveDialog = true
            },
            onDeleteLocalCopy = {
                pendingDeleteVaultItem = item
                viewModel.deleteLocalCopyFromDevice(
                    item = item,
                    onNeedPermission = { intentSender ->
                        deleteLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
                    },
                    onSuccess = {
                        Toast.makeText(context, "Local copy deleted! File is safely backed up in Telegram Cloud.", Toast.LENGTH_LONG).show()
                        activePreviewImage = item.copy(localPath = "")
                    }
                )
            },
            onRestore = {
                isRestoringActiveItem = true
                viewModel.downloadAndRestoreFile(
                    item = item,
                    onSuccessWithItem = { restoredItem ->
                        activePreviewImage = restoredItem
                    },
                    onResult = { success, msg ->
                        isRestoringActiveItem = false
                        Toast.makeText(context, msg, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                    }
                )
            },
            isRestoring = isRestoringActiveItem
        )

        if (showMoveDialog) {
            MoveFolderDialog(
                item = item,
                folders = folders,
                onDismiss = { showMoveDialog = false },
                onConfirm = { newFolder ->
                    viewModel.moveItemToFolder(item, newFolder)
                    showMoveDialog = false
                    activePreviewImage = item.copy(folder = newFolder)
                    Toast.makeText(context, "Moved to folder '$newFolder'", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    if (itemPendingDelete != null) {
        val targetItem = itemPendingDelete!!
        DeleteOptionsDialog(
            itemCount = 1,
            onDismiss = { itemPendingDelete = null },
            onDeleteDeviceOnly = {
                pendingBatchDeleteItems = listOf(targetItem)
                viewModel.deleteLocalCopyFromDevice(
                    item = targetItem,
                    onNeedPermission = { intentSender ->
                        deleteLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
                    },
                    onSuccess = {
                        Toast.makeText(context, "Local copy deleted from device! Kept in cloud.", Toast.LENGTH_SHORT).show()
                    }
                )
                if (activePreviewImage?.id == targetItem.id) activePreviewImage = null
                itemPendingDelete = null
            },
            onDeleteCloudOnly = {
                viewModel.deleteItemFromVaultOnly(targetItem)
                if (activePreviewImage?.id == targetItem.id) activePreviewImage = null
                itemPendingDelete = null
                Toast.makeText(context, "Removed from Cloud & Vault", Toast.LENGTH_SHORT).show()
            },
            onDeleteEverywhere = {
                pendingBatchDeleteItems = listOf(targetItem)
                viewModel.deleteItem(targetItem)
                if (activePreviewImage?.id == targetItem.id) activePreviewImage = null
                itemPendingDelete = null
                Toast.makeText(context, "Deleted everywhere", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showDeleteDialogForSelection) {
        val itemsToDelete = selectedItems.toList()
        DeleteOptionsDialog(
            itemCount = itemsToDelete.size,
            onDismiss = { showDeleteDialogForSelection = false },
            onDeleteDeviceOnly = {
                pendingBatchDeleteItems = itemsToDelete
                viewModel.deleteMultipleFromDeviceOnly(
                    items = itemsToDelete,
                    onNeedPermission = { intentSender ->
                        deleteLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
                    },
                    onSuccess = { count ->
                        Toast.makeText(context, "Deleted $count files from device", Toast.LENGTH_SHORT).show()
                    }
                )
                selectedItems.clear()
                showDeleteDialogForSelection = false
            },
            onDeleteCloudOnly = {
                viewModel.deleteMultipleFromVaultOnly(itemsToDelete)
                selectedItems.clear()
                showDeleteDialogForSelection = false
                Toast.makeText(context, "Removed ${itemsToDelete.size} items from Cloud & Vault", Toast.LENGTH_SHORT).show()
            },
            onDeleteEverywhere = {
                pendingBatchDeleteItems = itemsToDelete
                viewModel.deleteMultipleEverywhere(
                    items = itemsToDelete,
                    onNeedPermission = { intentSender ->
                        deleteLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
                    },
                    onSuccess = {
                        Toast.makeText(context, "Deleted ${itemsToDelete.size} items everywhere", Toast.LENGTH_SHORT).show()
                    }
                )
                selectedItems.clear()
                showDeleteDialogForSelection = false
            }
        )
    }
}

@Composable
fun AddMultipleVaultItemsDialog(
    uris: List<Uri>,
    folders: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (selectedFolder: String) -> Unit
) {
    val context = LocalContext.current
    var selectedFolder by remember { mutableStateOf("General") }
    var customFolderInput by remember { mutableStateOf("") }
    var showCustomFolderField by remember { mutableStateOf(false) }
    var totalSizeText by remember { mutableStateOf("Analyzing total size...") }

    LaunchedEffect(uris) {
        val resolver = context.contentResolver
        var totalSize: Long = 0
        uris.forEach { uri ->
            try {
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (sizeIdx != -1) totalSize += cursor.getLong(sizeIdx)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        totalSizeText = if (totalSize > 0L) formatFileSize(totalSize) else "Unknown size"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VAULT BULK CONFIRMATION",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        },
        containerColor = CosmicSlate,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Configure sorting and details for your ${uris.size} selected local-first encrypted files.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Text(
                    text = "Total Files: ${uris.size}",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Total Size: $totalSizeText",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                HorizontalDivider(color = CardBorderColor)

                // Folder Header
                Text(
                    text = "SORT INTO FOLDER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextGold
                )

                // Dropdown or list of available folder chips
                Column {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        items(folders) { folder ->
                            val isSelected = selectedFolder == folder && !showCustomFolderField
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (isSelected) NeonCyan else CardBorderColor, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedFolder = folder
                                        showCustomFolderField = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = folder, color = if (isSelected) NeonCyan else TextSecondary, fontSize = 12.sp)
                            }
                        }

                        // Custom Folder Chip
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (showCustomFolderField) NeonPurple.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (showCustomFolderField) NeonPurple else CardBorderColor, RoundedCornerShape(8.dp))
                                    .clickable {
                                        showCustomFolderField = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = "+ Custom Folder", color = if (showCustomFolderField) NeonPurple else TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    if (showCustomFolderField) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customFolderInput,
                            onValueChange = { customFolderInput = it },
                            label = { Text("New Folder Name", fontSize = 11.sp, color = NeonPurple) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = CardBorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalFolder = if (showCustomFolderField) {
                        customFolderInput.trim().ifEmpty { "General" }
                    } else {
                        selectedFolder
                    }
                    onConfirm(finalFolder)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.testTag("dialog_confirm_btn")
            ) {
                Text("VAULT NOW", color = CosmicBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_cancel_btn")) {
                Text("CANCEL", color = SoftCoral)
            }
        }
    )
}

@Composable
fun AddVaultItemDialog(
    uri: Uri,
    folders: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (customFilename: String, selectedFolder: String) -> Unit
) {
    val context = LocalContext.current
    var filenameInput by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf("General") }
    var customFolderInput by remember { mutableStateOf("") }
    var showCustomFolderField by remember { mutableStateOf(false) }
    var fileSizeText by remember { mutableStateOf("Analyzing size...") }
    var fileSizeBytes by remember { mutableStateOf(0L) }
    var fileMime by remember { mutableStateOf("") }

    LaunchedEffect(uri) {
        val resolver = context.contentResolver
        var size: Long = 0
        var name = "vault_file_${System.currentTimeMillis()}"
        var mime = resolver.getType(uri) ?: ""
        try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx != -1) name = cursor.getString(nameIdx)
                    if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        filenameInput = name
        fileSizeBytes = size
        fileMime = mime
        fileSizeText = if (size > 0L) formatFileSize(size) else "Unknown size"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VAULT CONFIRMATION",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        },
        containerColor = CosmicSlate,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Configure details for your local-first encrypted register entry.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                // Filename TextField
                OutlinedTextField(
                    value = filenameInput,
                    onValueChange = { filenameInput = it },
                    label = { Text("Display Filename", fontSize = 11.sp, color = NeonCyan) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CardBorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Size: $fileSizeText",
                        fontSize = 11.sp,
                        color = if (fileSizeBytes > 50L * 1024L * 1024L) SoftCoral else TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Max: 50 MB",
                        fontSize = 10.sp,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (fileSizeBytes > 50L * 1024L * 1024L) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SoftCoral.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, SoftCoral),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = SoftCoral, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Exceeds 50 MB limit. App supports images and videos below 50 MB only.",
                                color = SoftCoral,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = CardBorderColor)

                // Folder Header
                Text(
                    text = "SORT INTO FOLDER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextGold
                )

                // Dropdown or list of available folder chips
                Column {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        items(folders) { folder ->
                            val isSelected = selectedFolder == folder && !showCustomFolderField
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (isSelected) NeonCyan else CardBorderColor, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedFolder = folder
                                        showCustomFolderField = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = folder, color = if (isSelected) NeonCyan else TextSecondary, fontSize = 12.sp)
                            }
                        }

                        // Custom Folder Chip
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (showCustomFolderField) NeonPurple.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (showCustomFolderField) NeonPurple else CardBorderColor, RoundedCornerShape(8.dp))
                                    .clickable {
                                        showCustomFolderField = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = "+ Custom Folder", color = if (showCustomFolderField) NeonPurple else TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    if (showCustomFolderField) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customFolderInput,
                            onValueChange = { customFolderInput = it },
                            label = { Text("New Folder Name", fontSize = 11.sp, color = NeonPurple) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = CardBorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalFolder = if (showCustomFolderField) {
                        customFolderInput.trim().ifEmpty { "General" }
                    } else {
                        selectedFolder
                    }
                    onConfirm(filenameInput.trim().ifEmpty { "vault_file" }, finalFolder)
                },
                enabled = fileSizeBytes <= 50L * 1024L * 1024L,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    disabledContainerColor = CardBorderColor
                ),
                modifier = Modifier.testTag("dialog_confirm_btn")
            ) {
                Text(
                    text = if (fileSizeBytes > 50L * 1024L * 1024L) "FILE TOO LARGE (>50MB)" else "VAULT NOW",
                    color = if (fileSizeBytes > 50L * 1024L * 1024L) SoftCoral else CosmicBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_cancel_btn")) {
                Text("CANCEL", color = SoftCoral)
            }
        }
    )
}

@Composable
fun DeleteOptionsDialog(
    itemCount: Int,
    onDismiss: () -> Unit,
    onDeleteDeviceOnly: () -> Unit,
    onDeleteCloudOnly: () -> Unit,
    onDeleteEverywhere: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = SoftCoral,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "DELETE OPTIONS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftCoral
                )
            }
        },
        containerColor = CosmicSlate,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (itemCount == 1) 
                        "Choose where to remove this media item from:" 
                    else 
                        "Choose where to remove these $itemCount selected media items from:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // 1. Delete on Device Only
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CosmicGlass,
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDeleteDeviceOnly() }
                        .testTag("delete_opt_device")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Delete on Device",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                "Frees phone storage. Keeps file backed up in Telegram Cloud.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // 2. Delete on Cloud & Vault
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CosmicGlass,
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDeleteCloudOnly() }
                        .testTag("delete_opt_cloud")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Delete on Cloud & Vault",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                "Removes from Cloud & Vault registry. Keeps original file on phone.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // 3. Delete Everywhere
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CosmicGlass,
                    border = BorderStroke(1.dp, SoftCoral.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDeleteEverywhere() }
                        .testTag("delete_opt_everywhere")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SoftCoral.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = SoftCoral, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Delete Everywhere",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftCoral
                            )
                            Text(
                                "Permanently deletes from both device storage and cloud vault.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("delete_opt_cancel")) {
                Text("CANCEL", color = TextSecondary, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {}
    )
}

@Composable
fun rememberLocalExistsState(context: Context, localPath: String): Boolean {
    var exists by remember(localPath) {
        mutableStateOf(
            if (localPath.startsWith("content://")) true
            else if (localPath.isNotEmpty()) {
                val f = File(localPath)
                f.exists() && f.length() > 0
            } else false
        )
    }
    LaunchedEffect(localPath) {
        if (localPath.startsWith("content://")) {
            val uri = Uri.parse(localPath)
            withContext(Dispatchers.IO) {
                val realExists = try {
                    context.contentResolver.openInputStream(uri)?.use { true } ?: false
                } catch (e: Exception) { false }
                if (!realExists) {
                    withContext(Dispatchers.Main) {
                        exists = false
                    }
                }
            }
        }
    }
    return exists
}

@Composable
fun FullscreenPhotoViewerDialog(
    item: VaultItem,
    allGalleryItems: List<VaultItem> = emptyList(),
    viewModel: VaultViewModel? = null,
    onSelectItem: ((VaultItem) -> Unit)? = null,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onMoveClick: () -> Unit,
    onDeleteLocalCopy: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    isRestoring: Boolean = false
) {
    val context = LocalContext.current
    val itemsList = remember(allGalleryItems, item) {
        if (allGalleryItems.isNotEmpty()) allGalleryItems else listOf(item)
    }
    val totalCount = itemsList.size
    val initialIndex = remember(item.id, itemsList) {
        val idx = itemsList.indexOfFirst { it.id == item.id }
        if (idx >= 0) idx else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceAtLeast(0),
        pageCount = { totalCount.coerceAtLeast(1) }
    )

    val currentItem = if (itemsList.isNotEmpty() && pagerState.currentPage in 0 until totalCount) {
        itemsList[pagerState.currentPage]
    } else {
        item
    }

    LaunchedEffect(pagerState.currentPage) {
        if (itemsList.isNotEmpty() && pagerState.currentPage in 0 until totalCount) {
            val pageItem = itemsList[pagerState.currentPage]
            if (pageItem.id != item.id && onSelectItem != null) {
                onSelectItem(pageItem)
            }
        }
    }

    LaunchedEffect(item.id) {
        val targetIdx = itemsList.indexOfFirst { it.id == item.id }
        if (targetIdx >= 0 && targetIdx != pagerState.currentPage) {
            pagerState.scrollToPage(targetIdx)
        }
    }

    var showExifSheet by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(false) }

    val currentLocalUri = remember(currentItem.localPath) {
        if (currentItem.localPath.startsWith("content://")) Uri.parse(currentItem.localPath) else null
    }
    val currentLocalFile = remember(currentItem.localPath) {
        if (currentItem.localPath.isNotEmpty() && !currentItem.localPath.startsWith("content://")) File(currentItem.localPath) else null
    }
    val currentLocalFileExists = rememberLocalExistsState(context, currentItem.localPath)

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.94f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Swipable Media Viewers (Images / Videos move horizontally)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("photo_viewer_pager")
                ) { page ->
                    val pageItem = if (itemsList.isNotEmpty() && page in 0 until totalCount) itemsList[page] else item
                    SingleMediaViewerContent(
                        item = pageItem,
                        viewModel = viewModel,
                        showControls = showControls,
                        onToggleControls = { showControls = !showControls },
                        onDismiss = onDismiss
                    )
                }

                // Apple/VisionOS Style Top Floating Glass Pill Capsule Header + Action Toolbar (Integrated into showControls container)
                AnimatedVisibility(
                    visible = showControls,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .displayCutoutPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top Header Row: File info capsule & Close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(CosmicGlass)
                                    .border(1.dp, CardBorderColor, RoundedCornerShape(30.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    currentItem.syncState == SyncState.SYNCED -> NeonGreen
                                                    currentItem.syncState == SyncState.FAILED -> SoftCoral
                                                    else -> NeonCyan
                                                }
                                            )
                                    )
                                    if (totalCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(NeonCyan.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${pagerState.currentPage + 1}/$totalCount",
                                                color = NeonCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    Text(
                                        text = currentItem.filename,
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Text(
                                        text = "• ${formatFileSize(currentItem.fileSize)}",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Close Button Pill
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(CosmicGlass)
                                    .border(1.dp, CardBorderColor, CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Action Toolbar Pill (Positioned right below the image name)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .background(CosmicSlate.copy(alpha = 0.92f))
                                .border(1.dp, CardBorderColor, RoundedCornerShape(32.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 0. Cloud Safeguard Button (Only visible for dynamic scanned items)
                            if (currentItem.id.startsWith("scanned_") && viewModel != null) {
                                var isUploadingByClick by remember(currentItem.id) { mutableStateOf(false) }
                                IconButton(
                                    onClick = {
                                        isUploadingByClick = true
                                        viewModel.addVaultItem(
                                            uri = Uri.parse(currentItem.localPath),
                                            customFilename = currentItem.filename,
                                            folder = currentItem.folder,
                                            isManualImport = true
                                        )
                                        Toast.makeText(context, "Safeguarding ${currentItem.filename} to Cloud...", Toast.LENGTH_SHORT).show()
                                    },
                                    enabled = !isUploadingByClick,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.2f))
                                        .size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Safeguard to Cloud",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // 1. Copy Cloud Link Button
                            IconButton(
                                onClick = {
                                    val link = if (currentItem.syncState == SyncState.SYNCED && (currentItem.telegramMessageId ?: 0L) > 0L) {
                                        val chatIdClean = (currentItem.telegramChatId ?: viewModel?.configManager?.getChatId() ?: "").replace("-100", "")
                                        "https://t.me/c/$chatIdClean/${currentItem.telegramMessageId}"
                                    } else {
                                        currentItem.telegramFileId ?: "file_${currentItem.id}"
                                    }
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Telegram Cloud Link", link)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Cloud Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = "Copy Link", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }

                            // 2. EXIF & Metadata Lens Toggle Pill Button
                            IconButton(
                                onClick = { showExifSheet = !showExifSheet },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (showExifSheet) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                    .size(38.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "EXIF Metadata", tint = if (showExifSheet) NeonCyan else TextPrimary, modifier = Modifier.size(18.dp))
                            }

                            // 3. Save / Delete Local Storage Pill Button
                            if (currentLocalFileExists && onDeleteLocalCopy != null) {
                                IconButton(
                                    onClick = onDeleteLocalCopy,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Default.CleaningServices, contentDescription = "Free Storage", tint = NeonPurple, modifier = Modifier.size(18.dp))
                                }
                            } else if (!currentLocalFileExists && onRestore != null) {
                                IconButton(
                                    onClick = onRestore,
                                    enabled = !isRestoring,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    if (isRestoring) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.CloudDownload, contentDescription = "Restore Local Copy", tint = NeonCyan, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // 4. View in Telegram Button
                            if (currentItem.syncState == SyncState.SYNCED) {
                                IconButton(
                                    onClick = {
                                        openTelegramMessage(context, currentItem.telegramChatId ?: viewModel?.configManager?.getChatId(), currentItem.telegramMessageId)
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "View in Telegram", tint = Color(0xFF2AABEE), modifier = Modifier.size(18.dp))
                                }
                            }

                            // 5. Share Pill Button
                            IconButton(
                                onClick = onShare,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = NeonGreen, modifier = Modifier.size(18.dp))
                            }

                            // 6. Delete Pill Button
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SoftCoral, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // EXIF & Technical Lens Card Overlay (FIXED Slide-up Glass Sheet)
                AnimatedVisibility(
                    visible = showExifSheet,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .displayCutoutPadding()
                        .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSlate.copy(alpha = 0.96f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Camera, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Text(
                                        "EXIF & METADATA LENS",
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                IconButton(onClick = { showExifSheet = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close lens", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }

                            HorizontalDivider(color = CardBorderColor)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Format / MIME:", color = TextSecondary, fontSize = 11.sp)
                                Text(currentItem.mimeType, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Size:", color = TextSecondary, fontSize = 11.sp)
                                Text(formatFileSize(currentItem.fileSize), color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Folder Tag:", color = TextSecondary, fontSize = 11.sp)
                                Text(currentItem.folder, color = TextGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (currentItem.telegramFileId != null) {
                                Column {
                                    Text("Telegram File ID:", color = TextSecondary, fontSize = 10.sp)
                                    Text(
                                        currentItem.telegramFileId,
                                        color = NeonCyan,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Local Storage:", color = TextSecondary, fontSize = 11.sp)
                                Text(
                                    if (currentLocalFileExists) "Present on Device" else "Cloud Only",
                                    color = if (currentLocalFileExists) NeonGreen else NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Context.findActivity(): android.app.Activity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is android.app.Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleMediaViewerContent(
    item: VaultItem,
    viewModel: VaultViewModel? = null,
    showControls: Boolean = false,
    onToggleControls: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }

    val localUri = remember(item.localPath) {
        if (item.localPath.startsWith("content://")) Uri.parse(item.localPath) else null
    }
    val localFile = remember(item.localPath) {
        if (item.localPath.isNotEmpty() && !item.localPath.startsWith("content://")) File(item.localPath) else null
    }
    val localFileExists = rememberLocalExistsState(context, item.localPath)
    
    val isVideo = item.mimeType.startsWith("video/")

    var cloudStreamUrl by remember(item.id, item.telegramFileId) { mutableStateOf<String?>(null) }
    var isResolvingCloudUrl by remember(item.id, localFileExists) { mutableStateOf(!localFileExists && item.telegramFileId != null && !isVideo) }
    var cloudUrlError by remember(item.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(item.id, item.telegramFileId, localFileExists) {
        if (!localFileExists && item.telegramFileId != null && viewModel != null && !isVideo) {
            isResolvingCloudUrl = true
            cloudUrlError = null
            viewModel.getCloudMediaUrl(item.telegramFileId) { url ->
                isResolvingCloudUrl = false
                if (url != null) {
                    cloudStreamUrl = url
                } else {
                    cloudUrlError = "Unable to connect to Telegram Cloud. Verify bot configuration in Settings."
                }
            }
        }
    }

    var videoPlayUri by remember { mutableStateOf<Uri?>(null) }
    var isBufferingVideo by remember { mutableStateOf(true) }
    var streamProgressPct by remember { mutableFloatStateOf(0f) }
    var streamProgressDesc by remember { mutableStateOf("Initializing video stream...") }
    var streamErrorMsg by remember { mutableStateOf<String?>(null) }
    var streamReloadTrigger by remember { mutableIntStateOf(0) }

    val getPlayableContentUri = remember(context) {
        { file: File ->
            try {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                context.grantUriPermission(context.packageName, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                uri
            } catch (e: Exception) {
                Uri.fromFile(file)
            }
        }
    }

    LaunchedEffect(item.id, streamReloadTrigger, isVideo) {
        if (!isVideo) return@LaunchedEffect

        if (localUri != null && localFileExists) {
            videoPlayUri = localUri
            isBufferingVideo = false
            streamErrorMsg = null
            return@LaunchedEffect
        }
        if (localFile != null && localFileExists) {
            videoPlayUri = getPlayableContentUri(localFile)
            isBufferingVideo = false
            streamErrorMsg = null
            return@LaunchedEffect
        }
        val cacheDir = File(context.cacheDir, "stream_cache").apply { mkdirs() }
        val cachedFile = File(cacheDir, "stream_${item.id}.mp4")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            videoPlayUri = getPlayableContentUri(cachedFile)
            isBufferingVideo = false
            streamErrorMsg = null
            return@LaunchedEffect
        }
        if (item.telegramFileId != null && viewModel != null) {
            isBufferingVideo = true
            streamErrorMsg = null
            streamProgressDesc = "Connecting to Telegram Cloud..."
            viewModel.streamCloudVideo(
                item = item,
                context = context,
                onProgress = { pct, downloaded, total ->
                    streamProgressPct = pct
                    streamProgressDesc = "${(pct * 100).toInt()}% • ${formatFileSize(downloaded)} / ${formatFileSize(total)}"
                },
                onReady = { targetFile ->
                    videoPlayUri = getPlayableContentUri(targetFile)
                    isBufferingVideo = false
                    streamErrorMsg = null
                },
                onError = { err ->
                    isBufferingVideo = false
                    streamErrorMsg = err
                }
            )
        } else {
            isBufferingVideo = false
            streamErrorMsg = "This video is not on this device and has not yet been uploaded to Telegram Cloud."
        }
    }

    // Dynamic Ambient Glow & Physics Gesture State
    var dragOffsetY by remember(item.id) { mutableFloatStateOf(0f) }
    var isPullingDown by remember(item.id) { mutableStateOf(false) }

    val animatedDragY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "drag_y_anim"
    )

    val dragDismissAlpha = (1f - (animatedDragY / 280f)).coerceIn(0.15f, 1f)
    val mediaDragScale = (1f - (animatedDragY / 1200f)).coerceIn(0.75f, 1f)

    // Ambient Halo Breathing Pulsing Effect
    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Halo colors based on media type & item ID
    val ambientPrimaryColor = remember(item.id, isVideo) {
        if (isVideo) NeonPurple else NeonCyan
    }
    val ambientSecondaryColor = remember(item.id) {
        val hash = kotlin.math.abs(item.id.hashCode())
        when (hash % 3) {
            0 -> NeonGreen
            1 -> Color(0xFF2AABEE)
            else -> TextGold
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(dragDismissAlpha)
            .pointerInput(item.id, scale) {
                if (scale <= 1.05f) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffsetY > 70f) {
                                onDismiss()
                            }
                            dragOffsetY = 0f
                            isPullingDown = false
                        },
                        onDragCancel = {
                            dragOffsetY = 0f
                            isPullingDown = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (isPullingDown || (dragAmount > 4f && dragOffsetY == 0f)) {
                                isPullingDown = true
                                dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                change.consume()
                            }
                        }
                    )
                }
            }
    ) {
        // Ambient Color-Glow Background Halo Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = animatedDragY
                    scaleX = mediaDragScale
                    scaleY = mediaDragScale
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ambientPrimaryColor.copy(alpha = 0.28f * pulseAlpha),
                            ambientSecondaryColor.copy(alpha = 0.15f * pulseAlpha),
                            Color.Transparent
                        ),
                        center = Offset.Unspecified,
                        radius = 900f
                    )
                )
        )

        // Main Media Container (With gesture drag offset & pinch zoom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = animatedDragY
                    scaleX = mediaDragScale
                    scaleY = mediaDragScale
                },
            contentAlignment = Alignment.Center
        ) {
                    if (isVideo) {
                        if (videoPlayUri != null) {
                            var videoView by remember { mutableStateOf<android.widget.VideoView?>(null) }
                            var isPlaying by remember { mutableStateOf(false) }
                            var currentPosition by remember { mutableIntStateOf(0) }
                            var duration by remember { mutableIntStateOf(0) }
                            var isPlayerLoading by remember { mutableStateOf(true) }
                            var mediaPlayerInstance by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                            var isFillMode by remember { mutableStateOf(false) }

                            // Gesture feedback state
                            var gestureType by remember { mutableStateOf<String?>(null) }
                            var hudFeedbackText by remember { mutableStateOf<String?>(null) }
                            var hudIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }

                            LaunchedEffect(showControls, isPlaying) {
                                if (showControls && isPlaying) {
                                    kotlinx.coroutines.delay(4000)
                                    onToggleControls()
                                }
                            }

                            LaunchedEffect(gestureType) {
                                if (gestureType == null && hudFeedbackText != null) {
                                    kotlinx.coroutines.delay(800)
                                    hudFeedbackText = null
                                    hudIcon = null
                                }
                            }

                            LaunchedEffect(videoView, isPlaying) {
                                while (isPlaying && videoView != null) {
                                    currentPosition = videoView?.currentPosition ?: 0
                                    kotlinx.coroutines.delay(200)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                                    .pointerInput(item.id) {
                                        detectTapGestures(
                                            onTap = {
                                                onToggleControls()
                                            },
                                            onDoubleTap = { offset ->
                                                val width = size.width.toFloat().coerceAtLeast(1f)
                                                videoView?.let { vv ->
                                                    if (offset.x < width * 0.45f) {
                                                        val newPos = (vv.currentPosition - 10_000).coerceAtLeast(0)
                                                        vv.seekTo(newPos)
                                                        currentPosition = newPos
                                                        hudFeedbackText = "-10s (${formatDuration(newPos)})"
                                                        hudIcon = Icons.Default.FastRewind
                                                        gestureType = "seek"
                                                    } else if (offset.x > width * 0.55f) {
                                                        val newPos = (vv.currentPosition + 10_000).coerceAtMost(duration)
                                                        vv.seekTo(newPos)
                                                        currentPosition = newPos
                                                        hudFeedbackText = "+10s (${formatDuration(newPos)})"
                                                        hudIcon = Icons.Default.FastForward
                                                        gestureType = "seek"
                                                    } else {
                                                        isFillMode = !isFillMode
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                androidx.compose.runtime.key(videoPlayUri) {
                                    AndroidView(
                                        factory = { ctx ->
                                            android.widget.VideoView(ctx).apply {
                                                val finalPlayUri = try {
                                                    if (videoPlayUri?.scheme == "file") {
                                                        val f = File(videoPlayUri?.path ?: "")
                                                        if (f.exists()) {
                                                            getPlayableContentUri(f)
                                                        } else videoPlayUri
                                                    } else videoPlayUri
                                                } catch (e: Exception) {
                                                    videoPlayUri
                                                }
                                                setVideoURI(finalPlayUri)
                                                setOnPreparedListener { mp ->
                                                    mediaPlayerInstance = mp
                                                    duration = mp.duration
                                                    isPlayerLoading = false
                                                    mp.start()
                                                    isPlaying = true
                                                }
                                                setOnCompletionListener {
                                                    isPlaying = false
                                                    currentPosition = duration
                                                    if (!showControls) onToggleControls()
                                                }
                                                setOnErrorListener { _, what, extra ->
                                                    isPlayerLoading = false
                                                    streamErrorMsg = "Playback error ($what). Tap retry to reload stream."
                                                    true
                                                }
                                                videoView = this
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .scale(if (isFillMode) 1.25f else 1.0f)
                                            .align(Alignment.Center)
                                    )

                                    DisposableEffect(videoPlayUri) {
                                        onDispose {
                                            try { videoView?.stopPlayback() } catch (e: Exception) { }
                                        }
                                    }
                                }

                                // Gesture HUD Overlay Card
                                if (gestureType != null || hudFeedbackText != null) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = Color(0xF00A0D14),
                                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                                        shadowElevation = 16.dp,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(24.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            hudIcon?.let { icon ->
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = NeonGreen,
                                                    modifier = Modifier.size(38.dp)
                                                )
                                            }
                                            hudFeedbackText?.let { txt ->
                                                Text(
                                                    text = txt,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isPlayerLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = NeonGreen,
                                            modifier = Modifier.size(44.dp),
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }

                                // Sleek Bottom Floating Dock Pill
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = showControls,
                                    enter = androidx.compose.animation.fadeIn(),
                                    exit = androidx.compose.animation.fadeOut(),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xF20F141C),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                            shadowElevation = 12.dp,
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .navigationBarsPadding()
                                                .displayCutoutPadding()
                                                .padding(bottom = 52.dp, start = 12.dp, end = 12.dp)
                                                .fillMaxWidth()
                                                .height(48.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        videoView?.let { vv ->
                                                            if (vv.isPlaying) {
                                                                vv.pause()
                                                                isPlaying = false
                                                            } else {
                                                                if (vv.currentPosition >= duration && duration > 0) {
                                                                    vv.seekTo(0)
                                                                }
                                                                vv.start()
                                                                isPlaying = true
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                                        tint = NeonGreen,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }

                                                Text(
                                                    text = "${formatDuration(currentPosition)} / ${formatDuration(duration)}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontFamily = FontFamily.Monospace
                                                )

                                                Slider(
                                                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                                    onValueChange = { value ->
                                                        val newPosition = (value * duration).toInt()
                                                        videoView?.seekTo(newPosition)
                                                        currentPosition = newPosition
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = NeonGreen,
                                                        activeTrackColor = NeonGreen,
                                                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                                    ),
                                                    thumb = {
                                                        Box(
                                                            modifier = Modifier
                                                                .offset(y = 1.5.dp)
                                                                .size(12.dp)
                                                                .background(NeonGreen, CircleShape)
                                                        )
                                                    },
                                                    track = { sliderState ->
                                                        val frac = if (sliderState.valueRange.endInclusive > sliderState.valueRange.start) {
                                                            (sliderState.value - sliderState.valueRange.start) / (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                                                        } else 0f
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(24.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(3.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color.White.copy(alpha = 0.25f))
                                                            )
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth(frac.coerceIn(0f, 1f))
                                                                    .height(3.dp)
                                                                    .clip(CircleShape)
                                                                    .background(NeonGreen)
                                                            )
                                                        }
                                                    }
                                                )

                                                IconButton(
                                                    onClick = { isFillMode = !isFillMode },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isFillMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                                        contentDescription = if (isFillMode) "Fit" else "Fullscreen",
                                                        tint = if (isFillMode) NeonGreen else Color.White.copy(alpha = 0.9f),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (isBufferingVideo) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = NeonCyan,
                                        modifier = Modifier.size(44.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = "Buffering Stream from Telegram Cloud",
                                        color = NeonCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    LinearProgressIndicator(
                                        progress = { streamProgressPct },
                                        modifier = Modifier
                                            .fillMaxWidth(0.65f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = NeonCyan,
                                        trackColor = CosmicGlass
                                    )
                                    Text(
                                        text = streamProgressDesc,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CosmicSlate),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SoftCoral.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = SoftCoral, modifier = Modifier.size(40.dp))
                                        Text("Video Streaming Unavailable", color = SoftCoral, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                        Text(streamErrorMsg ?: "Unable to stream video from Telegram.", color = TextSecondary, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { streamReloadTrigger++ },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, tint = CosmicBackground, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("RETRY STREAM", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }

                                            if (item.syncState == SyncState.SYNCED) {
                                                Button(
                                                    onClick = {
                                                        openTelegramMessage(context, item.telegramChatId ?: viewModel?.configManager?.getChatId(), item.telegramMessageId)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE))
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("VIEW IN TELEGRAM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Image (Photo) Canvas with smooth pinch-zoom and gesture drag
                        val imageModel = remember(localUri, localFile, localFileExists, cloudStreamUrl) {
                            when {
                                localUri != null && localFileExists -> localUri
                                localFile != null && localFileExists -> localFile
                                cloudStreamUrl != null -> cloudStreamUrl
                                else -> null
                            }
                        }

                        if (imageModel != null) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "Preview of ${item.filename}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = if (scale > 1f) offset.x else 0f,
                                        translationY = if (scale > 1f) offset.y else 0f
                                    )
                                    .pointerInput(item.id) {
                                        detectTapGestures(
                                            onTap = { onToggleControls() },
                                            onDoubleTap = {
                                                if (scale > 1f) {
                                                    scale = 1f
                                                    offset = Offset.Zero
                                                } else {
                                                    scale = 2.5f
                                                    offset = Offset.Zero
                                                }
                                            }
                                        )
                                    }
                                    .pointerInput(item.id) {
                                        awaitEachGesture {
                                            do {
                                                val event = awaitPointerEvent()
                                                val zoom = event.calculateZoom()
                                                val pan = event.calculatePan()
                                                val pointerCount = event.changes.size

                                                if (pointerCount >= 2) {
                                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                                    scale = newScale
                                                    if (newScale > 1f) {
                                                        offset = Offset(
                                                            x = offset.x + pan.x,
                                                            y = offset.y + pan.y
                                                        )
                                                    } else {
                                                        offset = Offset.Zero
                                                    }
                                                    event.changes.forEach { it.consume() }
                                                } else if (pointerCount == 1 && scale > 1.05f) {
                                                    if (pan != Offset.Zero) {
                                                        offset = Offset(
                                                            x = offset.x + pan.x,
                                                            y = offset.y + pan.y
                                                        )
                                                        event.changes.forEach { it.consume() }
                                                    }
                                                }
                                            } while (event.changes.any { it.pressed })
                                        }
                                    },
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        } else if (isResolvingCloudUrl) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = NeonCyan,
                                        modifier = Modifier.size(40.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = "Loading image from Telegram Cloud...",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Direct viewing without restoring",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CosmicSlate),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, SoftCoral.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = SoftCoral, modifier = Modifier.size(40.dp))
                                        Text("Cloud Stream Unavailable", color = SoftCoral, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                        Text(cloudUrlError ?: "Unable to stream image from Telegram.", color = TextSecondary, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        if (item.telegramFileId != null && viewModel != null) {
                                            Button(
                                                onClick = {
                                                    isResolvingCloudUrl = true
                                                    cloudUrlError = null
                                                    viewModel.getCloudMediaUrl(item.telegramFileId) { url ->
                                                        isResolvingCloudUrl = false
                                                        if (url != null) cloudStreamUrl = url else cloudUrlError = "Failed to reconnect to Telegram."
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, tint = CosmicBackground, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("RETRY STREAM", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

@Composable
fun StatsSection(total: Int, synced: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                )
            )
            .border(1.dp, Color(0x0EFFFFFF), RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("TOTAL STORAGE ITEMS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonPurple, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("$total File(s)", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        
        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(44.dp)
                .background(CardBorderColor)
                .align(Alignment.CenterVertically)
        )

        Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
            Text("BACKUP SYSTEM SYNCED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (total > 0) "${((synced.toFloat() / total.toFloat()) * 100).toInt()}%" else "0%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("($synced/$total)", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun EmptyStateView(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(50))
                .background(NeonCyan.copy(alpha = 0.1f))
                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Empty",
                tint = NeonCyan,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Your Local-First Photos are Empty",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Abyss Cloud allows safeguarding files and photos, keeping them on-device while streaming backups to your secure personal Telegram feed.",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = LocalTextStyle.current.copy(lineHeight = 18.sp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, NeonCyan)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan)
            Spacer(modifier = Modifier.width(6.dp))
            Text("ADD YOUR FIRST PHOTO", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ModernGalleryGridItem(
    item: VaultItem,
    viewModel: VaultViewModel? = null,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onSelectClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onPreviewClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isVideo = item.mimeType.startsWith("video/")
    
    val localUri = remember(item.localPath) {
        if (item.localPath.startsWith("content://")) Uri.parse(item.localPath) else null
    }
    val localFile = remember(item.localPath) {
        if (item.localPath.isNotEmpty() && !item.localPath.startsWith("content://")) File(item.localPath) else null
    }
    val localExists = rememberLocalExistsState(context, item.localPath)

    val scaleAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 0.92f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "grid_item_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scaleAnim)
            .clip(RoundedCornerShape(8.dp))
            .background(CosmicGlass)
            .border(
                width = if (selected) 2.5.dp else if (!localExists && item.syncState == SyncState.SYNCED) 1.dp else 0.5.dp,
                color = if (selected) NeonCyan else if (!localExists && item.syncState == SyncState.SYNCED) NeonCyan.copy(alpha = 0.5f) else CardBorderColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = {
                    if (selectionMode) onSelectClick() else onPreviewClick()
                },
                onLongClick = onLongPress
            )
            .testTag("gallery_grid_item_${item.id}")
    ) {
        var cloudThumbUrl by remember(item.telegramFileId, localExists) { mutableStateOf<String?>(null) }
        LaunchedEffect(item.telegramFileId, localExists) {
            if (!localExists && item.telegramFileId != null && viewModel != null) {
                viewModel.getCloudMediaUrl(item.telegramFileId) { url ->
                    cloudThumbUrl = url
                }
            }
        }

        val cachedStreamFile = remember(item.id, isVideo) {
            if (isVideo) {
                val f = File(File(context.cacheDir, "stream_cache"), "stream_${item.id}.mp4")
                if (f.exists() && f.length() > 0) f else null
            } else null
        }

        val mediaModel = remember(localUri, localFile, localExists, cachedStreamFile, cloudThumbUrl) {
            when {
                localUri != null && localExists -> localUri
                localFile != null && localExists -> localFile
                cachedStreamFile != null -> cachedStreamFile
                cloudThumbUrl != null -> cloudThumbUrl
                else -> null
            }
        }

        if (mediaModel != null) {
            AsyncImage(
                model = mediaModel,
                contentDescription = "Gallery item preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            // Cloud-only placeholder thumbnail
            Box(
                modifier = Modifier.fillMaxSize().background(CosmicGlass),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Cloud,
                        contentDescription = "Cloud stored",
                        tint = NeonCyan.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Selection overlay checkmark (top-left)
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (selected) NeonCyan else Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, if (selected) NeonCyan else Color.White, CircleShape)
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = CosmicBackground,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Discrete Micro Status Indicators (top-right)
        Box(
            modifier = Modifier
                .padding(6.dp)
                .align(Alignment.TopEnd)
        ) {
            when (item.syncState) {
                SyncState.SYNCED -> {
                    if (!localExists) {
                        // Neon Cyan Cloud-only dynamic indicator badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonCyan.copy(alpha = 0.25f))
                                .border(0.5.dp, NeonCyan, RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "Cloud Stored Only",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "CLOUD",
                                    color = NeonCyan,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        // Small floating synced checkmark with subtle background blur
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Synced to Cloud",
                                tint = NeonGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                SyncState.UPLOADING -> {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(3.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            color = NeonCyan,
                            strokeWidth = 1.5.dp
                        )
                    }
                }
                SyncState.FAILED -> {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Sync failed",
                            tint = SoftCoral,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                else -> {
                    // Queued or pending sync
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Pending background sync",
                            tint = Color.LightGray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // Bottom Overlay for video label or file size metadata
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .align(Alignment.BottomStart)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (isVideo) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = formatFileSize(item.fileSize),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Text(
                        text = formatFileSize(item.fileSize),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VaultItemCard(
    item: VaultItem,
    viewModel: VaultViewModel? = null,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    isRestoring: Boolean,
    onImagePreviewClick: () -> Unit = {},
    onShare: () -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectClick: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val isMedia = item.mimeType.startsWith("image/") || item.mimeType.startsWith("video/")
    val isVideo = item.mimeType.startsWith("video/")
    val context = LocalContext.current

    if (isMedia) {
        val backupDate = remember(item.createdAt) {
            val date = java.util.Date(item.createdAt)
            val sdf = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault())
            sdf.format(date)
        }

        val dynamicAspectRatio = remember(item.id, isVideo) {
            if (isVideo) 0.82f
            else {
                val hash = kotlin.math.abs(item.id.hashCode())
                when (hash % 5) {
                    0 -> 0.72f  // Tall portrait 3:4
                    1 -> 1.25f  // Landscape 5:4
                    2 -> 0.88f  // Soft portrait
                    3 -> 1.05f  // Near square
                    else -> 0.80f // Portrait
                }
            }
        }

        var isPressed by remember { mutableStateOf(false) }
        val scaleAnim by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            ),
            label = "press_scale"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(dynamicAspectRatio)
                .scale(scaleAnim)
                .clip(RoundedCornerShape(20.dp))
                .background(CosmicGlass)
                .border(
                    if (selected) 2.5.dp else if (isPressed) 2.dp else 1.dp,
                    if (selected) NeonCyan
                    else if (isPressed) NeonCyan.copy(alpha = 0.8f)
                    else if (item.syncState == SyncState.SYNCED) NeonGreen.copy(alpha = 0.35f)
                    else if (item.syncState == SyncState.FAILED) SoftCoral.copy(alpha = 0.35f)
                    else CardBorderColor,
                    RoundedCornerShape(20.dp)
                )
                .combinedClickable(
                    onClick = {
                        if (selectionMode) onSelectClick() else onImagePreviewClick()
                    },
                    onLongClick = onLongPress
                )
                .testTag("vault_item_${item.id}")
        ) {
            val localUri = remember(item.localPath) {
                if (item.localPath.startsWith("content://")) Uri.parse(item.localPath) else null
            }
            val localFile = remember(item.localPath) {
                if (item.localPath.isNotEmpty() && !item.localPath.startsWith("content://")) File(item.localPath) else null
            }
            val localExists = rememberLocalExistsState(context, item.localPath)

            var cloudThumbUrl by remember(item.telegramFileId, localExists) { mutableStateOf<String?>(null) }
            LaunchedEffect(item.telegramFileId, localExists) {
                if (!localExists && item.telegramFileId != null && viewModel != null) {
                    viewModel.getCloudMediaUrl(item.telegramFileId) { url ->
                        cloudThumbUrl = url
                    }
                }
            }

            val cachedStreamFile = remember(item.id, isVideo) {
                if (isVideo) {
                    val f = File(File(context.cacheDir, "stream_cache"), "stream_${item.id}.mp4")
                    if (f.exists() && f.length() > 0) f else null
                } else null
            }

            val mediaModel = remember(localUri, localFile, localExists, cachedStreamFile, cloudThumbUrl) {
                when {
                    localUri != null && localExists -> localUri
                    localFile != null && localExists -> localFile
                    cachedStreamFile != null -> cachedStreamFile
                    cloudThumbUrl != null -> cloudThumbUrl
                    else -> null
                }
            }

            if (mediaModel != null) {
                // Background Image covering full card with round borders or generic icon for video
                AsyncImage(
                    model = mediaModel,
                    contentDescription = "Media preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                
                if (isVideo) {
                     Box(
                         modifier = Modifier.fillMaxSize(),
                         contentAlignment = Alignment.Center
                     ) {
                         Box(
                             modifier = Modifier
                                 .size(44.dp)
                                 .clip(androidx.compose.foundation.shape.CircleShape)
                                 .background(Color.Black.copy(alpha = 0.6f))
                                 .border(1.dp, Color.White.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape),
                             contentAlignment = Alignment.Center
                         ) {
                             Icon(
                                 imageVector = Icons.Default.PlayArrow,
                                 contentDescription = "Play Video",
                                 tint = Color.White,
                                 modifier = Modifier.size(24.dp)
                             )
                         }
                     }
                }

                // Minimalist Dark Overlay Gradients
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
            } else {
                // Cloud-only placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CosmicGlass),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Cloud,
                            contentDescription = "Cloud Stored",
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isVideo) "Cloud Video" else "Cloud Photo",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Tap to stream directly",
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Top Row: Minimal Status Pill & Actions Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isRestoring -> NeonPurple.copy(alpha = 0.85f)
                                !localExists && item.syncState == SyncState.SYNCED -> NeonCyan.copy(alpha = 0.85f)
                                item.syncState == SyncState.SYNCED -> NeonGreen.copy(alpha = 0.85f)
                                item.syncState == SyncState.FAILED -> SoftCoral.copy(alpha = 0.85f)
                                item.syncState == SyncState.UPLOADING -> NeonCyan.copy(alpha = 0.85f)
                                else -> Color.DarkGray.copy(alpha = 0.85f)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (item.syncState == SyncState.UPLOADING || isRestoring) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = Color.White
                            )
                        } else if (!localExists && item.syncState == SyncState.SYNCED) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Cloud Stored",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        } else if (item.syncState == SyncState.SYNCED) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Synced",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        
                        Text(
                            text = when {
                                isRestoring -> "RESTORING"
                                !localExists && item.syncState == SyncState.SYNCED -> ""
                                item.syncState == SyncState.SYNCED -> ""
                                item.syncState == SyncState.FAILED -> "FAILED"
                                item.syncState == SyncState.UPLOADING -> "UPLOADING"
                                else -> "QUEUED"
                            },
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(CosmicSlate)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share Photo", color = NeonCyan) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = NeonCyan) },
                            onClick = {
                                expanded = false
                                onShare()
                            }
                        )
                        if (item.syncState == SyncState.SYNCED) {
                            DropdownMenuItem(
                                text = { Text("View in Telegram", color = Color(0xFF2AABEE)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Color(0xFF2AABEE)) },
                                onClick = {
                                    expanded = false
                                    openTelegramMessage(context, item.telegramChatId ?: viewModel?.configManager?.getChatId(), item.telegramMessageId)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Restore Photo", color = NeonCyan) },
                                leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = NeonCyan) },
                                onClick = {
                                    expanded = false
                                    onRestore()
                                }
                            )
                        }
                        if (item.syncState == SyncState.FAILED) {
                            DropdownMenuItem(
                                text = { Text("Retry Backup", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White) },
                                onClick = {
                                    expanded = false
                                    onRetry()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete", color = SoftCoral) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = SoftCoral) },
                            onClick = {
                                expanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }


        }
    } else {
        // Standard (non-photo) minimal files card
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicGlass)
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) NeonCyan
                    else if (item.syncState == SyncState.SYNCED) NeonGreen.copy(alpha = 0.15f)
                    else if (item.syncState == SyncState.FAILED) SoftCoral.copy(alpha = 0.15f)
                    else CardBorderColor,
                    RoundedCornerShape(16.dp)
                )
                .combinedClickable(
                    onClick = {
                        if (selectionMode) onSelectClick() else onImagePreviewClick()
                    },
                    onLongClick = onLongPress
                )
                .padding(12.dp)
                .testTag("vault_item_${item.id}")
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when (item.syncState) {
                                    SyncState.SYNCED -> NeonGreen.copy(alpha = 0.12f)
                                    SyncState.FAILED -> SoftCoral.copy(alpha = 0.12f)
                                    else -> NeonCyan.copy(alpha = 0.12f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when {
                            item.mimeType.startsWith("audio/") -> Icons.Default.Lock
                            item.mimeType.startsWith("video/") -> Icons.Default.Lock
                            else -> Icons.Default.Lock
                        }
                        val color = when (item.syncState) {
                            SyncState.SYNCED -> NeonGreen
                            SyncState.FAILED -> SoftCoral
                            else -> NeonCyan
                        }
                        Icon(icon, contentDescription = "File Type", tint = color, modifier = Modifier.size(18.dp))
                    }

                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(CosmicSlate)
                        ) {
                            if (item.syncState == SyncState.SYNCED) {
                                DropdownMenuItem(
                                    text = { Text("View in Telegram", color = Color(0xFF2AABEE)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Color(0xFF2AABEE)) },
                                    onClick = {
                                        expanded = false
                                        openTelegramMessage(context, item.telegramChatId ?: viewModel?.configManager?.getChatId(), item.telegramMessageId)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Restore File", color = NeonCyan) },
                                    leadingIcon = { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = NeonCyan) },
                                    onClick = {
                                        expanded = false
                                        onRestore()
                                    }
                                )
                            }
                            if (item.syncState == SyncState.FAILED) {
                                DropdownMenuItem(
                                    text = { Text("Force Retry Backup", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary) },
                                    onClick = {
                                        expanded = false
                                        onRetry()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Wipe from Storage", color = SoftCoral) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = SoftCoral) },
                                onClick = {
                                    expanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = item.filename,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val backupDate = remember(item.createdAt) {
                    val date = java.util.Date(item.createdAt)
                    val sdf = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault())
                    sdf.format(date)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = backupDate,
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFileSize(item.fileSize),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = item.folder,
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (item.syncState) {
                                SyncState.SYNCED -> NeonGreen.copy(alpha = 0.12f)
                                SyncState.FAILED -> SoftCoral.copy(alpha = 0.12f)
                                SyncState.UPLOADING -> NeonCyan.copy(alpha = 0.12f)
                                else -> Color(0x0EFFFFFF)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (item.syncState == SyncState.UPLOADING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.5.dp,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else if (isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.5.dp,
                            color = NeonPurple
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = when {
                            isRestoring -> "RESTORING..."
                            item.syncState == SyncState.SYNCED -> "BACKED UP"
                            item.syncState == SyncState.FAILED -> "FAILED"
                            item.syncState == SyncState.UPLOADING -> "UPLOADING..."
                            else -> "QUEUED"
                        },
                        color = when {
                            isRestoring -> NeonPurple
                            item.syncState == SyncState.SYNCED -> NeonGreen
                            item.syncState == SyncState.FAILED -> SoftCoral
                            item.syncState == SyncState.UPLOADING -> NeonCyan
                            else -> Color.LightGray
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VaultItemStackedCard(
    item: VaultItem,
    viewModel: VaultViewModel?,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    isRestoring: Boolean,
    onImagePreviewClick: () -> Unit,
    onShare: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectClick: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    val context = LocalContext.current
    val isVideo = item.mimeType.startsWith("video/")

    val localUri = remember(item.localPath) {
        if (item.localPath.startsWith("content://")) Uri.parse(item.localPath) else null
    }
    val localFile = remember(item.localPath) {
        if (item.localPath.isNotEmpty() && !item.localPath.startsWith("content://")) File(item.localPath) else null
    }
    val localExists = rememberLocalExistsState(context, item.localPath)
    var cloudThumbUrl by remember(item.telegramFileId, localExists) { mutableStateOf<String?>(null) }
    LaunchedEffect(item.telegramFileId, localExists) {
        if (!localExists && item.telegramFileId != null && viewModel != null) {
            viewModel.getCloudMediaUrl(item.telegramFileId) { url ->
                cloudThumbUrl = url
            }
        }
    }
    val cachedStreamFile = remember(item.id, isVideo) {
        if (isVideo) {
            val f = File(File(context.cacheDir, "stream_cache"), "stream_${item.id}.mp4")
            if (f.exists() && f.length() > 0) f else null
        } else null
    }

    val mediaModel = remember(localUri, localFile, localExists, cachedStreamFile, cloudThumbUrl) {
        when {
            localUri != null && localExists -> localUri
            localFile != null && localExists -> localFile
            cachedStreamFile != null -> cachedStreamFile
            cloudThumbUrl != null -> cloudThumbUrl
            else -> null
        }
    }

    val backupDate = remember(item.createdAt) {
        val date = java.util.Date(item.createdAt)
        val sdf = java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.getDefault())
        sdf.format(date)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) NeonCyan
                else if (item.syncState == SyncState.SYNCED) NeonGreen.copy(alpha = 0.25f)
                else if (item.syncState == SyncState.FAILED) SoftCoral.copy(alpha = 0.35f)
                else CardBorderColor,
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = {
                    if (selectionMode) onSelectClick() else onImagePreviewClick()
                },
                onLongClick = onLongPress
            )
            .testTag("vault_stacked_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = CosmicSlate),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar on Card: Folder tag, Date, and Sync status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (selectionMode) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (selected) NeonCyan else Color.Transparent)
                                .border(1.5.dp, if (selected) NeonCyan else TextSecondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = CosmicBackground, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Folder tag pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicGlass)
                            .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                            Text(
                                text = item.folder,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = backupDate,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Sync status indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isRestoring -> NeonPurple.copy(alpha = 0.2f)
                                !localExists && item.syncState == SyncState.SYNCED -> NeonCyan.copy(alpha = 0.2f)
                                item.syncState == SyncState.SYNCED -> NeonGreen.copy(alpha = 0.2f)
                                item.syncState == SyncState.FAILED -> SoftCoral.copy(alpha = 0.2f)
                                else -> Color(0x1AFFFFFF)
                            }
                        )
                        .border(
                            0.5.dp,
                            when {
                                isRestoring -> NeonPurple
                                !localExists && item.syncState == SyncState.SYNCED -> NeonCyan
                                item.syncState == SyncState.SYNCED -> NeonGreen
                                item.syncState == SyncState.FAILED -> SoftCoral
                                else -> Color(0x33FFFFFF)
                            },
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when {
                            isRestoring -> "RESTORING..."
                            !localExists && item.syncState == SyncState.SYNCED -> "CLOUD ONLY"
                            item.syncState == SyncState.SYNCED -> "SYNCED TO CLOUD"
                            item.syncState == SyncState.FAILED -> "FAILED"
                            else -> "PENDING SYNC"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            isRestoring -> NeonPurple
                            !localExists && item.syncState == SyncState.SYNCED -> NeonCyan
                            item.syncState == SyncState.SYNCED -> NeonGreen
                            item.syncState == SyncState.FAILED -> SoftCoral
                            else -> TextSecondary
                        }
                    )
                }
            }

            // Large Prominent Media Container for Easy Visibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.Black)
            ) {
                if (mediaModel != null) {
                    AsyncImage(
                        model = mediaModel,
                        contentDescription = "Media preview of ${item.filename}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )

                    if (isVideo) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .border(1.5.dp, NeonCyan, CircleShape)
                                    .clickable { onImagePreviewClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Video Stream",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Video Badge in bottom left
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                                Text("VIDEO • ${formatFileSize(item.fileSize)}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Telegram verification badge in bottom right of video container
                        if (item.syncState == SyncState.SYNCED) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2AABEE).copy(alpha = 0.85f))
                                    .clickable {
                                        openTelegramMessage(context, item.telegramChatId ?: viewModel?.configManager?.getChatId(), item.telegramMessageId)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text("TG MSG", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // Fallback placeholder while resolving cloud URL
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = if (isVideo) "Loading Cloud Video..." else "Loading Cloud Photo...",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Bottom Footer: Filename, Details, Tags and Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.filename,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatFileSize(item.fileSize),
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        if (item.tags.isNotEmpty()) {
                            Text(
                                text = "•  ${item.tags}",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.syncState == SyncState.SYNCED) {
                        IconButton(
                            onClick = {
                                openTelegramMessage(context, item.telegramChatId ?: viewModel?.configManager?.getChatId(), item.telegramMessageId)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "View in Telegram", tint = Color(0xFF2AABEE), modifier = Modifier.size(18.dp))
                        }
                    }

                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }

                    if (!localExists && item.telegramFileId != null) {
                        IconButton(onClick = onRestore, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Download to device", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (item.syncState == SyncState.FAILED) {
                        IconButton(onClick = onRetry, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry upload", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = SoftCoral.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onImagePreviewClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun QueueScreen(viewModel: VaultViewModel) {
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val workInfos by viewModel.uploadProgressFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }

    // Generate ongoing items map based on WorkManager status details with live byte counts
    val activeProgressMap = remember(workInfos) {
        val map = mutableMapOf<String, com.example.data.UploadProgressInfo>()
        workInfos.forEach { info ->
            if (info.state == WorkInfo.State.RUNNING) {
                val progress = info.progress.getInt("PROGRESS", -1)
                val bytesRead = info.progress.getLong("BYTES_READ", 0L)
                val bytesTotal = info.progress.getLong("BYTES_TOTAL", 0L)
                val itemId = info.progress.getString("ITEM_ID")
                if (itemId != null && progress != -1) {
                    map[itemId] = com.example.data.UploadProgressInfo(
                        progress = progress,
                        bytesRead = bytesRead,
                        bytesTotal = bytesTotal
                    )
                }
            }
        }
        map
    }

    val uploadingItems = remember(items, activeProgressMap) {
        items.filter { it.syncState == SyncState.UPLOADING || activeProgressMap.containsKey(it.id) }
            .sortedByDescending { it.createdAt }
    }

    val pendingItems = remember(items, activeProgressMap) {
        items.filter { it.syncState == SyncState.PENDING && !activeProgressMap.containsKey(it.id) }
            .sortedByDescending { it.createdAt }
    }

    val failedItems = remember(items) {
        items.filter { it.syncState == SyncState.FAILED }.sortedByDescending { it.createdAt }
    }

    val syncedItems = remember(items) {
        items.filter { it.syncState == SyncState.SYNCED }.sortedByDescending { it.createdAt }
    }

    val queueItems = remember(uploadingItems, pendingItems, failedItems) {
        uploadingItems + pendingItems + failedItems
    }

    val uploadingCount = uploadingItems.size
    val pendingCount = pendingItems.size
    val failedCount = failedItems.size
    val syncedCount = syncedItems.size
    val totalCount = queueItems.size + syncedCount

    val activeDisplayQueueItems = remember(selectedFilter, queueItems, uploadingItems, pendingItems, failedItems) {
        when (selectedFilter) {
            "UPLOADING" -> uploadingItems
            "QUEUED" -> pendingItems
            "FAILED" -> failedItems
            "SYNCED" -> emptyList()
            else -> queueItems
        }
    }

    val activeDisplaySyncedItems = remember(selectedFilter, syncedItems) {
        when (selectedFilter) {
            "SYNCED" -> syncedItems
            "ALL" -> syncedItems
            else -> emptyList()
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val screenWidth = maxWidth
        val horizontalPadding = if (screenWidth > 600.dp) 24.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 850.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = horizontalPadding, vertical = 16.dp)
        ) {
            Text(
                text = "SYNC PIPELINE STATUS",
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonCyan,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Interactive queue tab engine. Click any status badge to filter items separately.",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Queue Control Buttons (Stop, Restart, Clear Queue)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // STOP BACKUP
                    Button(
                        onClick = {
                            viewModel.stopAllBackupProgress()
                            Toast.makeText(context, "All backup progress stopped!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftCoral.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, SoftCoral),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("stop_backup_btn_queue")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = SoftCoral, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP BACKUP", color = SoftCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // RESTART BACKUP
                    Button(
                        onClick = {
                            viewModel.restartBackup()
                            Toast.makeText(context, "Backup restarted for queue items!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("restart_backup_btn_queue")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Restart", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESTART BACKUP", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // CANCEL & CLEAR QUEUE
                Button(
                    onClick = {
                        viewModel.cancelAndClearQueue()
                        Toast.makeText(context, "Cancelled and cleared all items in sync queue!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftCoral.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, SoftCoral.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("clear_queue_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Queue", tint = SoftCoral, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CANCEL & CLEAR SYNC QUEUE", color = SoftCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Live Pipeline Status Filter Badges / Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ALL Badge
                val isAllSelected = selectedFilter == "ALL"
                Row(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isAllSelected) NeonCyan.copy(alpha = 0.25f) else CosmicGlass)
                        .border(1.dp, if (isAllSelected) NeonCyan else CardBorderColor, RoundedCornerShape(20.dp))
                        .clickable { selectedFilter = "ALL" }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isAllSelected) NeonCyan else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ALL ($totalCount)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAllSelected) NeonCyan else TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Uploading Badge
                val isUploadingSelected = selectedFilter == "UPLOADING"
                Row(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isUploadingSelected) NeonCyan.copy(alpha = 0.25f) else if (uploadingCount > 0) NeonCyan.copy(alpha = 0.1f) else CosmicGlass)
                        .border(1.dp, if (isUploadingSelected) NeonCyan else if (uploadingCount > 0) NeonCyan.copy(alpha = 0.4f) else CardBorderColor, RoundedCornerShape(20.dp))
                        .clickable { selectedFilter = "UPLOADING" }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (uploadingCount > 0) NeonCyan else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "UPLOADING ($uploadingCount)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUploadingSelected || uploadingCount > 0) NeonCyan else TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Queued Badge
                val isQueuedSelected = selectedFilter == "QUEUED"
                Row(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isQueuedSelected) Color.White.copy(alpha = 0.2f) else CosmicGlass)
                        .border(1.dp, if (isQueuedSelected) TextPrimary else CardBorderColor, RoundedCornerShape(20.dp))
                        .clickable { selectedFilter = "QUEUED" }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (pendingCount > 0) Color.Yellow else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "QUEUED ($pendingCount)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isQueuedSelected) TextPrimary else TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Synced Badge
                val isSyncedSelected = selectedFilter == "SYNCED"
                Row(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSyncedSelected) NeonGreen.copy(alpha = 0.25f) else if (syncedCount > 0) NeonGreen.copy(alpha = 0.1f) else CosmicGlass)
                        .border(1.dp, if (isSyncedSelected) NeonGreen else if (syncedCount > 0) NeonGreen.copy(alpha = 0.4f) else CardBorderColor, RoundedCornerShape(20.dp))
                        .clickable { selectedFilter = "SYNCED" }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (syncedCount > 0) NeonGreen else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SYNCED ($syncedCount)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSyncedSelected || syncedCount > 0) NeonGreen else TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Failed Badge
                val isFailedSelected = selectedFilter == "FAILED"
                Row(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isFailedSelected) SoftCoral.copy(alpha = 0.25f) else if (failedCount > 0) SoftCoral.copy(alpha = 0.15f) else CosmicGlass)
                        .border(1.dp, if (isFailedSelected) SoftCoral else if (failedCount > 0) SoftCoral.copy(alpha = 0.4f) else CardBorderColor, RoundedCornerShape(20.dp))
                        .clickable { selectedFilter = "FAILED" }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (failedCount > 0) SoftCoral else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FAILED ($failedCount)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFailedSelected || failedCount > 0) SoftCoral else TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (activeDisplayQueueItems.isEmpty() && activeDisplaySyncedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = when (selectedFilter) {
                                "FAILED" -> Icons.Default.Warning
                                "SYNCED" -> Icons.Default.Check
                                else -> Icons.Default.Check
                            },
                            contentDescription = "Empty Filter Status",
                            tint = when (selectedFilter) {
                                "FAILED" -> SoftCoral
                                "SYNCED" -> NeonGreen
                                else -> NeonGreen
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    when (selectedFilter) {
                                        "FAILED" -> SoftCoral.copy(alpha = 0.15f)
                                        else -> NeonGreen.copy(alpha = 0.15f)
                                    }
                                )
                                .padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedFilter) {
                                "UPLOADING" -> "No media files currently uploading"
                                "QUEUED" -> "No media files pending in queue"
                                "FAILED" -> "No failed backup items"
                                "SYNCED" -> "No cloud-synced files found"
                                else -> "All backups are fully synchronized!"
                            },
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (selectedFilter) {
                                "UPLOADING" -> "Tap 'ALL' or 'QUEUED' to monitor other pipeline states."
                                "QUEUED" -> "All queue tasks have completed dispatching."
                                "FAILED" -> "All pipeline operations are healthy."
                                "SYNCED" -> "Completed uploads will appear here."
                                else -> "No files currently waiting in the upload loop."
                            },
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activeDisplayQueueItems, key = { it.id }) { item ->
                        val progressInfo = activeProgressMap[item.id]
                        val percentageProgress = progressInfo?.progress
                        val bytesRead = progressInfo?.bytesRead ?: 0L
                        val bytesTotal = if ((progressInfo?.bytesTotal ?: 0L) > 0L) progressInfo!!.bytesTotal else item.fileSize

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicGlass)
                                .border(
                                    1.dp,
                                    if (item.syncState == SyncState.UPLOADING) NeonCyan.copy(alpha = 0.5f) else CardBorderColor,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(14.dp)
                                .testTag("queue_item_${item.id}")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Left thumbnail preview + filename details
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val isMedia = item.mimeType.startsWith("image/") || item.mimeType.startsWith("video/")
                                        if (isMedia) {
                                            AsyncImage(
                                                model = item.localPath,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.InsertDriveFile,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.filename,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${formatFileSize(item.fileSize)} • ${item.mimeType}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                // Individual action buttons
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (item.syncState == SyncState.FAILED) {
                                        IconButton(onClick = { viewModel.retryUpload(item) }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = NeonCyan)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteItem(item) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SoftCoral)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Progress state text indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (item.syncState) {
                                        SyncState.UPLOADING -> "UPLOADING TO TELEGRAM CONTAINER..."
                                        SyncState.FAILED -> "BACKUP WORKER TERMINATED (FAILED)"
                                        else -> "QUEUED FOR PIPELINE DISPATCH"
                                    },
                                    fontSize = 9.sp,
                                    color = when (item.syncState) {
                                        SyncState.UPLOADING -> NeonCyan
                                        SyncState.FAILED -> SoftCoral
                                        else -> Color.LightGray
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (item.syncState == SyncState.UPLOADING) {
                                    Text(
                                        text = if (bytesTotal > 0L && bytesRead > 0L) {
                                            "${formatFileSize(bytesRead)} / ${formatFileSize(bytesTotal)} (${percentageProgress ?: 0}%)"
                                        } else {
                                            "${percentageProgress ?: 0}%"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Linear progress indicator
                            if (item.syncState == SyncState.UPLOADING) {
                                val livePrg = percentageProgress ?: 0
                                LinearProgressIndicator(
                                    progress = { livePrg.toFloat() / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = NeonCyan,
                                    trackColor = NeonCyan.copy(alpha = 0.15f)
                                )
                            } else if (item.syncState == SyncState.FAILED) {
                                if (!item.errorMessage.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SoftCoral.copy(alpha = 0.10f))
                                            .border(1.dp, SoftCoral.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Error",
                                            tint = SoftCoral,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.errorMessage,
                                            fontSize = 11.sp,
                                            color = SoftCoral,
                                            style = androidx.compose.ui.text.TextStyle(lineHeight = 15.sp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.retryUpload(item) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = NeonCyan
                                    ),
                                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "RETRY UPLOAD",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                LinearProgressIndicator(
                                    progress = { 0.05f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color.Gray,
                                    trackColor = Color(0x1FFFFFFF)
                                )
                            }
                        }
                    }

                    if (activeDisplaySyncedItems.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(NeonGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RECENTLY SYNCED TO TELEGRAM (${activeDisplaySyncedItems.size})",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonGreen,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        items(activeDisplaySyncedItems, key = { "synced_${it.id}" }) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CosmicGlass)
                                    .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                                    .testTag("synced_item_${item.id}")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.4f))
                                                .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val isMedia = item.mimeType.startsWith("image/") || item.mimeType.startsWith("video/")
                                            if (isMedia) {
                                                AsyncImage(
                                                    model = item.localPath,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.InsertDriveFile,
                                                    contentDescription = null,
                                                    tint = NeonGreen,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.filename,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${formatFileSize(item.fileSize)} • ${item.folder}",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(NeonGreen.copy(alpha = 0.15f))
                                                .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "SYNCED ✓",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonGreen,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteItem(item) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SoftCoral.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        queueItems.forEach {
                            if (it.syncState == SyncState.FAILED) {
                                viewModel.retryUpload(it)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("retry_all_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = CosmicBackground)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RETRY ALL FAILED UPLOADS", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun RestoreScreen(viewModel: VaultViewModel) {
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val restoringId by viewModel.isRestoringFile.collectAsStateWithLifecycle()

    val backupSyncedItems = remember(items) {
        items.filter { it.syncState == SyncState.SYNCED }
    }

    var textSearchId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "TELEGRAM CLOUD RESTORATION",
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NeonCyan,
            letterSpacing = 1.5.sp
        )
        Text(
            text = "Re-download files directly from your Telegram Bot channel",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Custom Restore box
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CosmicGlass),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "EXTERNAL MANIFEST RESTORE",
                    color = NeonPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "Have an isolated Telegram file ID? Enter it below to fetch the binary directly.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                OutlinedTextField(
                    value = textSearchId,
                    onValueChange = { textSearchId = it },
                    label = { Text("Telegram file_id Reference", fontSize = 12.sp) },
                    placeholder = { Text("e.g. BQACAgQAAxkBAAM...", fontSize = 11.sp, color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().testTag("restore_id_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = CardBorderColor,
                        focusedLabelColor = NeonPurple,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (textSearchId.isEmpty()) {
                            Toast.makeText(context, "Please enter a valid file_id first!", Toast.LENGTH_SHORT).show()
                        } else {
                            // Synthesize a temp item to download
                            val synthItem = VaultItem(
                                localPath = "",
                                filename = "External_Restored_File.dat",
                                fileSize = 0,
                                mimeType = "application/octet-stream",
                                telegramFileId = textSearchId,
                                syncState = SyncState.SYNCED
                            )
                            viewModel.downloadAndRestoreFile(synthItem) { success, message ->
                                if (success) {
                                    textSearchId = ""
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = CosmicBackground)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("FETCH AND RESTORE BINARY", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "BACKED UP HISTORY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (backupSyncedItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No items are cataloged. Complete a cloud backup to view restoration links.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(backupSyncedItems, key = { it.id }) { item ->
                    val isDownloading = restoringId == item.id
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicGlass)
                            .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.filename,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "file_id: ${item.telegramFileId?.take(18) ?: ""}...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = NeonCyan
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.downloadAndRestoreFile(item) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, NeonGreen),
                            enabled = !isDownloading
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = NeonGreen)
                            } else {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: VaultViewModel) {
    val context = LocalContext.current
    val isTesting by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val restoringId by viewModel.isRestoringFile.collectAsStateWithLifecycle()

    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val syncedCount by viewModel.syncedCount.collectAsStateWithLifecycle()
    val currentThemeIndex by viewModel.themeIndex.collectAsStateWithLifecycle()

    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    var tokenInput by remember(activeUser.id) { mutableStateOf(activeUser.botToken) }
    var chatInput by remember(activeUser.id) { mutableStateOf(activeUser.chatId) }
    var hideToken by remember { mutableStateOf(true) }

    LaunchedEffect(activeUser.botToken, activeUser.chatId) {
        tokenInput = activeUser.botToken
        chatInput = activeUser.chatId
    }

    val parseAndAutoFill = { input: String ->
        val trimmed = input.trim()
        val parts = trimmed.split(Regex("[,;\\s]+"))
        if (parts.size >= 2) {
            var token: String? = null
            var chatId: String? = null
            for (part in parts) {
                val p = part.trim()
                if (p.contains(":") && p.split(":").firstOrNull()?.all { it.isDigit() } == true) {
                    token = p
                } else if (p.startsWith("-") || p.all { it.isDigit() || it == '-' }) {
                    chatId = p
                }
            }
            if (token != null && chatId != null) {
                tokenInput = token
                chatInput = chatId
                Toast.makeText(context, "Detected combined config. Auto-filled bot token and chat ID!", Toast.LENGTH_SHORT).show()
            } else {
                // If it split but didn't identify both, assign to what makes sense or use input
                if (trimmed.contains(":") && trimmed.split(":").firstOrNull()?.all { it.isDigit() } == true) {
                    tokenInput = trimmed
                } else {
                    tokenInput = trimmed
                }
            }
        } else {
            if (trimmed.contains(":") && trimmed.split(":").firstOrNull()?.all { it.isDigit() } == true) {
                tokenInput = trimmed
            } else if (trimmed.startsWith("-") || (trimmed.isNotEmpty() && trimmed.all { it.isDigit() || it == '-' })) {
                chatInput = trimmed
            } else {
                // Default fallback if we can't classify
                tokenInput = input
            }
        }
    }

    val parseAndAutoFillChat = { input: String ->
        val trimmed = input.trim()
        val parts = trimmed.split(Regex("[,;\\s]+"))
        if (parts.size >= 2) {
            var token: String? = null
            var chatId: String? = null
            for (part in parts) {
                val p = part.trim()
                if (p.contains(":") && p.split(":").firstOrNull()?.all { it.isDigit() } == true) {
                    token = p
                } else if (p.startsWith("-") || p.all { it.isDigit() || it == '-' }) {
                    chatId = p
                }
            }
            if (token != null && chatId != null) {
                tokenInput = token
                chatInput = chatId
                Toast.makeText(context, "Detected combined config. Auto-filled bot token and chat ID!", Toast.LENGTH_SHORT).show()
            } else {
                chatInput = trimmed
            }
        } else {
            if (trimmed.contains(":") && trimmed.split(":").firstOrNull()?.all { it.isDigit() } == true) {
                tokenInput = trimmed
            } else if (trimmed.startsWith("-") || (trimmed.isNotEmpty() && trimmed.all { it.isDigit() || it == '-' })) {
                chatInput = trimmed
            } else {
                chatInput = input
            }
        }
    }

    val users by viewModel.users.collectAsStateWithLifecycle()
    val checkingUserId by viewModel.checkingUserId.collectAsStateWithLifecycle()
    val availableTargets by viewModel.availableUploadTargets.collectAsStateWithLifecycle()
    val isTestingAll by viewModel.isTestingAllBots.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "ABYSS CLOUD SECURE PARAMS",
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonCyan,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Abyss Cloud locks credentials locally in encrypted hardware-backed storage files. No external servers access these except directly to Telegram API endpoints.",
                fontSize = 11.sp,
                color = TextSecondary,
                style = LocalTextStyle.current.copy(lineHeight = 16.sp),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        // Storage statistics overview (moved from photos tab)
        item {
            StatsSection(total = totalCount, synced = syncedCount)
        }

        // Clear Local Cache & Storage Utility Section
        item {
            val cacheSizeFormatted by viewModel.localCacheSizeFormatted.collectAsStateWithLifecycle()
            var isClearing by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                viewModel.refreshCacheSize()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicGlass)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = "Clear Cache",
                                tint = NeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "LOCAL CACHE SIZE",
                                color = NeonPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Temporary stream buffers",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicBackground.copy(alpha = 0.7f))
                            .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cacheSizeFormatted,
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Clearing local cache removes temporary media stream buffers and image thumbnails to free up disk space while keeping your master vault database and cloud backups 100% intact.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    style = LocalTextStyle.current.copy(lineHeight = 15.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        isClearing = true
                        viewModel.clearLocalCache { freedBytes, formattedSize ->
                            isClearing = false
                            Toast.makeText(
                                context,
                                if (freedBytes > 0) "Cleared local cache! Freed $formattedSize." else "Cache is already 100% clean!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !isClearing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_local_cache_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, NeonPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isClearing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NeonPurple,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CLEARING CACHE...", color = NeonPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CLEAR LOCAL CACHE NOW", color = NeonPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Configurable Dynamic App theme selection
        item {
            val darkModeMode by viewModel.darkModeMode.collectAsStateWithLifecycle()
            ThemeSelectorSection(
                currentThemeIndex = currentThemeIndex,
                darkModeMode = darkModeMode,
                onThemeSelected = { viewModel.setThemeIndex(it) },
                onDarkModeSelected = { viewModel.setDarkModeMode(it) }
            )
        }

        // Multi-User Accounts Hub
        item {
            var showUserModal by remember { mutableStateOf(false) }

            if (showUserModal) {
                com.example.ui.MultiUserManagerDialog(viewModel) {
                    showUserModal = false
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicGlass)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "USER PROFILES & CREDENTIALS",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${users.size} Profile${if (users.size > 1) "s" else ""} • Active: ${activeUser.name}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { showUserModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MANAGE", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active User Quick View Card
                val activeAvatarColor = com.example.ui.parseHexColor(activeUser.avatarColorHex)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicBackground.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(activeAvatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    activeUser.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    activeUser.name,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (activeUser.isVerified) "Verified • Ready to sync" else "Unverified • Tap to test",
                                    color = if (activeUser.isVerified) NeonGreen else TextGold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Check it button
                        val isChecking = checkingUserId == activeUser.id
                        Button(
                            onClick = {
                                viewModel.checkUserCredentials(activeUser) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeUser.isVerified) NeonGreen.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, if (activeUser.isVerified) NeonGreen else NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = NeonCyan)
                            } else {
                                Icon(
                                    if (activeUser.isVerified) Icons.Default.CheckCircle else Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = if (activeUser.isVerified) NeonGreen else NeonCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (activeUser.isVerified) "CHECKED" else "CHECK IT",
                                    color = if (activeUser.isVerified) NeonGreen else NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                val hasMultiBots = availableTargets.size >= 2 || users.count { it.botToken.isNotBlank() } >= 2
                if (hasMultiBots) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NeonGreen.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("MULTI-BOT SHARING ACTIVE", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Surface(shape = RoundedCornerShape(4.dp), color = NeonGreen.copy(alpha = 0.2f)) {
                                    Text("${availableTargets.size.coerceAtLeast(2)} BOTS", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Both profile bots share file upload duties in round-robin sequence during backup.",
                                color = TextPrimary,
                                fontSize = 10.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val targetChatId = activeUser.chatId.ifEmpty { viewModel.configManager.getChatId() }
                                    viewModel.verifyAllBots(targetChatId) { passed, total, summary ->
                                        Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = !isTestingAll,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, NeonGreen),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().testTag("verify_all_profiles_button"),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                if (isTestingAll) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = NeonGreen)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("VERIFYING BOTH BOTS...", color = NeonGreen, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("VERIFY BOTH BOTS LIVE", color = NeonGreen, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Configuration card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicGlass)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "TELEGRAM CREDENTIALS",
                    color = TextGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (availableTargets.size >= 2 || users.count { it.botToken.isNotBlank() } >= 2) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonCyan.copy(alpha = 0.1f),
                        border = BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Editing credentials for active profile '${activeUser.name}'. Both connected bots share background file uploads round-robin.",
                                color = TextPrimary,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Bot token Input
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { parseAndAutoFill(it) },
                    label = { Text("Telegram Bot Token", fontSize = 12.sp) },
                    placeholder = { Text("e.g. 123456789:ABCdefGhIJ...", fontSize = 11.sp, color = TextSecondary) },
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
                    modifier = Modifier.fillMaxWidth().testTag("token_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Chat ID Input
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { parseAndAutoFillChat(it) },
                    label = { Text("Telegram Chat ID (Channel/Group ID)", fontSize = 12.sp) },
                    placeholder = { Text("e.g. -100123456789", fontSize = 11.sp, color = TextSecondary) },
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
                                viewModel.testTelegramConnection(cleanToken, cleanChatId) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("test_conn_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, NeonCyan),
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = NeonCyan)
                        } else {
                            Text("TEST CONNECTION", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (tokenInput.isEmpty() || chatInput.isEmpty()) {
                                Toast.makeText(context, "Token or Chat ID can't be blank.", Toast.LENGTH_SHORT).show()
                            } else {
                                val cleanToken = com.example.data.TelegramConfigManager.sanitizeBotToken(tokenInput)
                                val cleanChatId = com.example.data.TelegramConfigManager.sanitizeChatId(chatInput)
                                tokenInput = cleanToken
                                chatInput = cleanChatId
                                viewModel.saveTelegramSettings(cleanToken, cleanChatId)
                                Toast.makeText(context, "Settings securely encrypted and saved!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("save_config_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("SAVE CONFIG", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Multi-Bot Load Balancing Pool Card (Prevents timeouts & 429 rate limits)
        item {
            com.example.ui.MultiBotPoolCard(
                viewModel = viewModel,
                currentChatId = chatInput
            )
        }

        // Automated Sync & Background backup control card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicGlass)
                    .border(1.dp, Color(0x0EFFFFFF), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "AUTOMATED BACKGROUND SYNC",
                    color = TextGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                var autoSyncEnabled by remember { mutableStateOf(viewModel.configManager.isAutoSyncEnabled()) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Background Auto-Backup",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Monitor enrolled folders and securely upload new media automatically over Wi-Fi in the background.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            style = LocalTextStyle.current.copy(lineHeight = 15.sp)
                        )
                    }
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = { checked ->
                            autoSyncEnabled = checked
                            viewModel.configManager.setAutoSyncEnabled(checked)
                            viewModel.setupPeriodicAutoSync()
                            Toast.makeText(context, if (checked) "Auto-Backup Enabled" else "Auto-Backup Disabled", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CardBorderColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CardBorderColor.copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                val isWifiOnlySync by viewModel.isWifiOnlySync.collectAsStateWithLifecycle()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Wi-Fi Only Sync",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isWifiOnlySync) NeonCyan.copy(alpha = 0.15f) else NeonGreen.copy(alpha = 0.15f))
                                    .border(1.dp, if (isWifiOnlySync) NeonCyan.copy(alpha = 0.4f) else NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isWifiOnlySync) "WI-FI ONLY" else "MOBILE DATA + WI-FI",
                                    color = if (isWifiOnlySync) NeonCyan else NeonGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isWifiOnlySync)
                                "Sync pauses on Mobile Data to save cellular bandwidth."
                                else "Sync operates over all network types (Mobile Data & Wi-Fi).",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            style = LocalTextStyle.current.copy(lineHeight = 15.sp)
                        )
                    }
                    Switch(
                        checked = isWifiOnlySync,
                        onCheckedChange = { checked ->
                            viewModel.setWifiOnlySyncEnabled(checked)
                            Toast.makeText(
                                context,
                                if (checked) "Wi-Fi Only Sync Enabled (Saves Mobile Data)" else "Sync Enabled on Mobile Data + Wi-Fi",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = NeonGreen,
                            uncheckedTrackColor = NeonGreen.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("wifi_only_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = CardBorderColor.copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                val currentMediaMode by viewModel.backupMediaType.collectAsStateWithLifecycle()

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Backup Mode Selection",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (currentMediaMode) {
                                    "IMAGES_ONLY" -> "PHOTOS"
                                    "VIDEOS_ONLY" -> "VIDEOS"
                                    else -> "ALL MEDIA"
                                },
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Specify whether background auto-backup should process photos only, videos only, or all media.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        style = LocalTextStyle.current.copy(lineHeight = 15.sp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // ALL MEDIA option
                        val isAll = currentMediaMode == "ALL"
                        Button(
                            onClick = {
                                viewModel.setBackupMediaType("ALL")
                                Toast.makeText(context, "Backup Mode: All Media (Photos & Videos)", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAll) NeonCyan.copy(alpha = 0.25f) else CosmicBackground
                            ),
                            border = BorderStroke(1.dp, if (isAll) NeonCyan else CardBorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("backup_mode_all_btn"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "ALL MEDIA",
                                color = if (isAll) NeonCyan else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }

                        // IMAGES ONLY option
                        val isImages = currentMediaMode == "IMAGES_ONLY"
                        Button(
                            onClick = {
                                viewModel.setBackupMediaType("IMAGES_ONLY")
                                Toast.makeText(context, "Backup Mode: Photos Only", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isImages) NeonGreen.copy(alpha = 0.25f) else CosmicBackground
                            ),
                            border = BorderStroke(1.dp, if (isImages) NeonGreen else CardBorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("backup_mode_images_btn"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "IMAGES ONLY",
                                color = if (isImages) NeonGreen else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }

                        // VIDEOS ONLY option
                        val isVideos = currentMediaMode == "VIDEOS_ONLY"
                        Button(
                            onClick = {
                                viewModel.setBackupMediaType("VIDEOS_ONLY")
                                Toast.makeText(context, "Backup Mode: Videos Only", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVideos) SoftCoral.copy(alpha = 0.25f) else CosmicBackground
                            ),
                            border = BorderStroke(1.dp, if (isVideos) SoftCoral else CardBorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("backup_mode_videos_btn"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "VIDEOS ONLY",
                                color = if (isVideos) SoftCoral else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Manual Sync All Trigger
                Button(
                    onClick = {
                        viewModel.syncAllPending()
                        Toast.makeText(context, "Initiating full backup cascade...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, NeonPurple)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonPurple)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TRIGGER MANUAL BACKUP FOR PENDING FILES", color = NeonPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Cloud Restoration Card (Moved from separate tab into Settings)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicGlass)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "CLOUD RESTORATION",
                    color = NeonPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    "Restore individual files safely from your secure Telegram channel, keeping the navigation bar minimal.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    style = LocalTextStyle.current.copy(lineHeight = 15.sp),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                var showRestoreMenu by remember { mutableStateOf(false) }

                Button(
                    onClick = { showRestoreMenu = !showRestoreMenu },
                    modifier = Modifier.fillMaxWidth().testTag("toggle_restore_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, NeonPurple)
                ) {
                    Icon(
                        imageVector = if (showRestoreMenu) Icons.Default.Close else Icons.Default.ArrowForward,
                        contentDescription = "Toggle Restore",
                        tint = NeonPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (showRestoreMenu) "CLOSE RESTORE HUB" else "OPEN RESTORE HUB",
                        color = NeonPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (showRestoreMenu) {
                    Spacer(modifier = Modifier.height(16.dp))

                    val isFetchingFromTelegram by viewModel.isFetchingFromTelegram.collectAsStateWithLifecycle()

                    Button(
                        onClick = {
                            viewModel.fetchFromTelegramGroup { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("fetch_telegram_restore_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, NeonGreen)
                    ) {
                        if (isFetchingFromTelegram) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NeonGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isFetchingFromTelegram) "FETCHING FROM TELEGRAM..." else "FETCH ALL BACKUPS FROM TELEGRAM GROUP",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    var textSearchId by remember { mutableStateOf("") }
                    val backupSyncedItems = remember(items) {
                        items.filter { it.syncState == SyncState.SYNCED }
                            .sortedByDescending { it.createdAt }
                            .take(15) // Limit history in UI to prevent UI thread from hanging in standard Column
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CosmicBackground.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "EXTERNAL MANIFEST RESTORE",
                                color = NeonPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "Enter an isolated Telegram file_id to restore and download.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                            OutlinedTextField(
                                value = textSearchId,
                                onValueChange = { textSearchId = it },
                                label = { Text("Telegram file_id Reference", fontSize = 11.sp) },
                                placeholder = { Text("e.g. BQACAgQAAxkBAAM...", fontSize = 11.sp, color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth().testTag("restore_id_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonPurple,
                                    unfocusedBorderColor = CardBorderColor,
                                    focusedLabelColor = NeonPurple,
                                    unfocusedLabelColor = TextSecondary,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    if (textSearchId.isEmpty()) {
                                        Toast.makeText(context, "Please enter a valid file_id first!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val synthItem = VaultItem(
                                            localPath = "",
                                            filename = "External_Restored_File.dat",
                                            fileSize = 0,
                                            mimeType = "application/octet-stream",
                                            telegramFileId = textSearchId,
                                            syncState = SyncState.SYNCED
                                        )
                                        viewModel.downloadAndRestoreFile(synthItem) { success, message ->
                                            if (success) {
                                                textSearchId = ""
                                            }
                                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("restore_by_id_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = CosmicBackground, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("FETCH AND RESTORE BINARY", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "RECENT BACKED UP HISTORY (TOP 15)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (backupSyncedItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No items are cataloged in cloud history yet.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            backupSyncedItems.forEach { item ->
                                val isDownloading = restoringId == item.id
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CosmicBackground.copy(alpha = 0.5f))
                                        .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.filename,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "file_id: ${item.telegramFileId?.take(18) ?: ""}...",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = NeonCyan
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.downloadAndRestoreFile(item) { success, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f)),
                                        border = BorderStroke(1.dp, NeonGreen),
                                        enabled = !isDownloading,
                                        modifier = Modifier.testTag("restore_item_btn_${item.id}")
                                    ) {
                                        if (isDownloading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = NeonGreen)
                                        } else {
                                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Guide / Instruction card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicGlass)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "HOW TO GET CREDENTIALS",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    "1. Create a Bot:\nSearch @BotFather in Telegram and send \"/newbot\". Copy the HTTP API token.\n\n" +
                    "2. Setup a Secure Channel:\nCreate a Private Channel or Group and add your newly generated bot as an administrator with permission to post documents.\n\n" +
                    "3. Get Channel Chat ID:\nSend a test message to the channel, then forward it to @userinfobot or fetch the ID via the following API in your browser: https://api.telegram.org/bot<TOKEN>/getUpdates and look for \"chat\":{\"id\": -100xxxxxxxxxx}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    style = LocalTextStyle.current.copy(lineHeight = 16.sp)
                )

                if (viewModel.configManager.isConfigured()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.clearTelegramSettings()
                            tokenInput = ""
                            chatInput = ""
                            Toast.makeText(context, "Cleared configuration!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftCoral)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WIPE SECURE CREDS FROM ENVIRONMENT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceSyncScreen(viewModel: VaultViewModel) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanningDevice.collectAsStateWithLifecycle()
    val scannedItems by viewModel.scannedMediaItems.collectAsStateWithLifecycle()
    val enrolledFolders by viewModel.selectedBackupFolders.collectAsStateWithLifecycle()
    val allVaultItems by viewModel.allItems.collectAsStateWithLifecycle()

    val existingVaultFilenames = remember(allVaultItems) { allVaultItems.map { it.filename }.toSet() }
    val updatedScannedItems = remember(scannedItems, existingVaultFilenames) {
        scannedItems.map { item ->
            val isBackedUp = existingVaultFilenames.contains(item.displayName)
            if (item.isAlreadyBackedUp != isBackedUp) item.copy(isAlreadyBackedUp = isBackedUp) else item
        }
    }

    val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }
    } else {
        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var hasPermissions by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                // On Android 13+, check if we have either full media images/video permission, OR partial user selected permission
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                permissions.all {
                    androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = if (android.os.Build.VERSION.SDK_INT >= 34) {
            results[android.Manifest.permission.READ_MEDIA_IMAGES] == true ||
            results[android.Manifest.permission.READ_MEDIA_VIDEO] == true ||
            results["android.permission.READ_MEDIA_VISUAL_USER_SELECTED"] == true
        } else {
            results.values.all { it }
        }
        if (hasPermissions) {
            viewModel.scanDeviceStorage()
        } else {
            Toast.makeText(context, "Storage permission is required to find folders!", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(hasPermissions) {
        if (hasPermissions && viewModel.scannedMediaItems.value.isEmpty()) {
            viewModel.scanDeviceStorage()
        }
    }

    var exploreFolderDetails by remember { mutableStateOf<String?>(null) } // folder name to show explore dialog

    if (!hasPermissions) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NeonCyan.copy(alpha = 0.1f))
                    .border(2.dp, NeonCyan, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "DISCOVER SYSTEM MEDIA",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Configure custom device sections like Camera images, WhatsApp Media, Screenshots, and Downloads to import selectively or enroll in background auto-backup.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = LocalTextStyle.current.copy(lineHeight = 18.sp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { permissionLauncher.launch(permissions) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "GRANT STORAGE ACCESS",
                    color = CosmicBackground,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    } else {
        val foldersMap = remember(updatedScannedItems) {
            updatedScannedItems.groupBy { it.bucketName }
        }
        var folderSearchQuery by remember { mutableStateOf("") }
        val filteredFoldersMap = remember(foldersMap, folderSearchQuery) {
            if (folderSearchQuery.isBlank()) foldersMap
            else foldersMap.filter { (name, _) -> name.contains(folderSearchQuery.trim(), ignoreCase = true) }
        }
        val allEnrolled = foldersMap.keys.isNotEmpty() && foldersMap.keys.all { enrolledFolders.contains(it) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // Header Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicSlate),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "ENTIRE DEVICE STORAGE",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ALL FOLDERS & MEDIA",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { viewModel.scanDeviceStorage() },
                                modifier = Modifier
                                    .background(NeonCyan.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            ) {
                                Icon(
                                    modifier = Modifier.rotate(if (isScanning) 180f else 0f),
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Scan Storage",
                                    tint = NeonCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Limit indicator badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonCyan.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "All Device Folders • Photos & Videos Detected",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Scans all device folders and system storage recursively for photos and videos. You can enroll all folders in background auto-sync, selectively import media files, or stop backup progress at any time.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            style = LocalTextStyle.current.copy(lineHeight = 15.sp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // All Folders Auto-Sync Toggle Button
                        Button(
                            onClick = {
                                if (allEnrolled) {
                                    viewModel.unenrollAllDeviceFolders()
                                    Toast.makeText(context, "Unenrolled all folders from auto-sync", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.enrollAllDeviceFolders(foldersMap.keys)
                                    Toast.makeText(context, "Enrolled all ${foldersMap.size} device folders in auto-sync!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (allEnrolled) SoftCoral.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, if (allEnrolled) SoftCoral else NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (allEnrolled) Icons.Default.Close else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (allEnrolled) SoftCoral else NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (allEnrolled) "UNENROLL ALL FOLDERS FROM AUTO-SYNC" else "ENROLL ALL FOLDERS IN AUTO-SYNC",
                                color = if (allEnrolled) SoftCoral else NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // STOP COMPLETE BACKUP PROGRESS Button
                        Button(
                            onClick = {
                                viewModel.stopAllBackupProgress()
                                Toast.makeText(context, "All backup progress stopped!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftCoral.copy(alpha = 0.2f)),
                            border = BorderStroke(1.5.dp, SoftCoral),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("stop_backup_btn_devicesync")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = SoftCoral, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "STOP COMPLETE BACKUP PROGRESS",
                                color = SoftCoral,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Stats summary row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CosmicGlass),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("ALL FOLDERS", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${foldersMap.size} (${enrolledFolders.size} synced)", color = TextGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1.5f)
                            .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CosmicGlass),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("MEDIA (<50 MB)", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${scannedItems.size} Image(s) & Video(s)", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Folders Field
            if (foldersMap.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = folderSearchQuery,
                        onValueChange = { folderSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search all device folders...", color = TextSecondary, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (folderSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { folderSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (isScanning && foldersMap.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NeonCyan)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Scanning entire device storage for all folders...",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            } else if (foldersMap.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CosmicGlass)
                            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No media directories discovered on the device.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "DISCOVERED FOLDERS (${filteredFoldersMap.size})",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        if (folderSearchQuery.isNotBlank()) {
                            Text(
                                "Filtered",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                filteredFoldersMap.forEach { (bucketName, itemsList) ->
                    val isAutoBackup = enrolledFolders.contains(bucketName)
                    val photosCount = itemsList.count { it.mimeType.startsWith("image/") }
                    val videosCount = itemsList.count { it.mimeType.startsWith("video/") }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicGlass),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isAutoBackup) NeonCyan.copy(alpha = 0.3f) else CardBorderColor, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                bucketName.contains("camera", ignoreCase = true) || bucketName.contains("dcim", ignoreCase = true) -> Icons.Default.Share
                                                bucketName.contains("whatsapp", ignoreCase = true) || bucketName.contains("telegram", ignoreCase = true) -> Icons.Default.Send
                                                bucketName.contains("screenshot", ignoreCase = true) -> Icons.Default.Build
                                                bucketName.contains("download", ignoreCase = true) -> Icons.Default.ArrowBack
                                                bucketName.contains("movie", ignoreCase = true) || bucketName.contains("video", ignoreCase = true) -> Icons.Default.PlayArrow
                                                else -> Icons.Default.List
                                            },
                                            contentDescription = null,
                                            tint = if (isAutoBackup) NeonCyan else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = bucketName,
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "$photosCount photo(s) • $videosCount video(s) (<50 MB)",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    // Switch for Auto Sync Enroll
                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isAutoBackup) NeonCyan.copy(alpha = 0.15f) else SoftCoral.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, if (isAutoBackup) NeonCyan.copy(alpha = 0.4f) else SoftCoral.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = if (isAutoBackup) "SYNC ACTIVE" else "SYNC OFF",
                                                color = if (isAutoBackup) NeonCyan else SoftCoral,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Switch(
                                            checked = isAutoBackup,
                                            onCheckedChange = {
                                                viewModel.toggleBackupFolder(bucketName)
                                                if (isAutoBackup) {
                                                    Toast.makeText(context, "Turned OFF sync for $bucketName. Photos will not sync.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Turned ON sync for $bucketName. Photos will auto-sync.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = NeonCyan,
                                                checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                                                uncheckedThumbColor = TextSecondary,
                                                uncheckedTrackColor = CardBorderColor
                                            ),
                                            modifier = Modifier.scale(0.85f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { exploreFolderDetails = bucketName },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.15f)),
                                        border = BorderStroke(1.dp, NeonPurple),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.List, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("EXPLORE & SELECT", color = NeonPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }

                                    Button(
                                        onClick = {
                                            val unbackedItems = itemsList.filter { !it.isAlreadyBackedUp }
                                            if (unbackedItems.isEmpty()) {
                                                Toast.makeText(context, "All media in $bucketName is already in your vault!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                unbackedItems.forEach { item ->
                                                    viewModel.addVaultItem(item.uri, item.displayName, bucketName, isManualImport = true)
                                                }
                                                Toast.makeText(context, "Importing ${unbackedItems.size} media file(s) (<50MB) from $bucketName into vault!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonCyan.copy(alpha = 0.15f)
                                        ),
                                        border = BorderStroke(1.dp, NeonCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "IMPORT ALL",
                                            color = NeonCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    exploreFolderDetails?.let { bucketName ->
        val folderItems = updatedScannedItems.filter { it.bucketName == bucketName }
        ExploreFolderDialog(
            folderName = bucketName,
            items = folderItems,
            onDismiss = { exploreFolderDetails = null },
            onImportSelected = { selectedList ->
                selectedList.forEach { item ->
                    viewModel.addVaultItem(item.uri, item.displayName, bucketName, isManualImport = true)
                }
                exploreFolderDetails = null
                Toast.makeText(context, "Queued ${selectedList.size} item(s) for local vault encryption and Telegram backup!", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
fun ExploreFolderDialog(
    folderName: String,
    items: List<MediaItem>,
    onDismiss: () -> Unit,
    onImportSelected: (List<MediaItem>) -> Unit
) {
    val selectedItems = remember { mutableStateListOf<MediaItem>() }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (selectedItems.isNotEmpty()) {
            selectedItems.clear()
        } else {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        containerColor = CosmicSlate,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = folderName.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Text(
                            text = "${items.size} file(s) on device",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = SoftCoral)
                }
            }
        },
        text = {
            var dialogFilter by remember { mutableStateOf("ALL") }
            val filteredDialogItems = remember(items, dialogFilter) {
                when (dialogFilter) {
                    "PHOTOS" -> items.filter { it.mimeType.startsWith("image/") }
                    "VIDEOS" -> items.filter { it.mimeType.startsWith("video/") }
                    "SYNCED" -> items.filter { it.isAlreadyBackedUp }
                    else -> items
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Select photos or videos to import into your secure Abyss Cloud register.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Filter Tabs inside Dialog
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val photosCount = items.count { it.mimeType.startsWith("image/") }
                    val videosCount = items.count { it.mimeType.startsWith("video/") }
                    val syncedCount = items.count { it.isAlreadyBackedUp }

                    listOf(
                        "ALL" to "ALL (${items.size})",
                        "PHOTOS" to "PHOTOS ($photosCount)",
                        "VIDEOS" to "VIDEOS ($videosCount)",
                        "SYNCED" to "SYNCED ☁ ($syncedCount)"
                    ).forEach { (filterKey, label) ->
                        val isSelectedFilter = dialogFilter == filterKey
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelectedFilter) NeonCyan.copy(alpha = 0.25f) else CosmicGlass,
                            border = BorderStroke(1.dp, if (isSelectedFilter) NeonCyan else CardBorderColor),
                            modifier = Modifier
                                .clickable { dialogFilter = filterKey }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelectedFilter) NeonCyan else TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            selectedItems.clear()
                            selectedItems.addAll(filteredDialogItems.filter { !it.isAlreadyBackedUp })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicGlass),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SELECT ALL", color = NeonCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = { selectedItems.clear() },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicGlass),
                        border = BorderStroke(1.dp, CardBorderColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CLEAR SELECTION", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredDialogItems, key = { it.id }) { item ->
                        val isSelected = selectedItems.contains(item)
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = !item.isAlreadyBackedUp) {
                                    if (isSelected) {
                                        selectedItems.remove(item)
                                    } else {
                                        selectedItems.add(item)
                                    }
                                }
                        ) {
                            // Show real image/video preview using Coil
                            AsyncImage(
                                model = item.uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().alpha(if (item.isAlreadyBackedUp) 0.35f else 1.0f),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )

                            // Contrast gradient overlay for label legibility
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.3f),
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )

                            // Videos show a centered play overlay icon
                            if (item.mimeType.startsWith("video/")) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Video file",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Info details overlay
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = item.displayName,
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatFileSize(item.size),
                                    color = Color.LightGray,
                                    fontSize = 7.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }

                            // Status indicator in Top Right - SYNCED WHITE CLOUD BADGE
                            if (item.isAlreadyBackedUp) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Synced White Cloud",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "SYNCED",
                                            color = Color.White,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            } else {
                                // Selection checkbox indicator
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NeonCyan else Color.Black.copy(alpha = 0.3f))
                                        .border(1.dp, Color.White, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = CosmicBackground,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImportSelected(selectedItems.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                enabled = selectedItems.isNotEmpty(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "VAULT SELECTED (${selectedItems.size})",
                    color = CosmicBackground,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = SoftCoral, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

// Convert bytes to clean representation
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

fun log10(x: Double): Double = kotlin.math.log10(x)

fun formatDuration(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun shareVaultItem(context: Context, item: VaultItem, viewModel: VaultViewModel) {
    if (item.localPath.startsWith("content://")) {
        try {
            val uri = Uri.parse(item.localPath)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Media via"))
            return
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share media: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }
    }

    val file = File(item.localPath)
    if (file.exists()) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Media via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } else if (!item.telegramFileId.isNullOrEmpty()) {
        Toast.makeText(context, "Downloading from Telegram backup to share...", Toast.LENGTH_SHORT).show()
        viewModel.downloadAndRestoreFile(item, onSuccessWithItem = { restoredItem ->
            val restoredFile = File(restoredItem.localPath)
            if (restoredFile.exists()) {
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        restoredFile
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = item.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Media via"))
                } catch (e: Exception) {
                    Toast.makeText(context, "Error sharing download: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }) { success, msg ->
            if (!success) {
                Toast.makeText(context, "Failed to download backup: $msg", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        Toast.makeText(context, "Local file not found on device!", Toast.LENGTH_SHORT).show()
    }
}

fun openTelegramMessage(context: Context, chatId: String?, messageId: Long?) {
    val cleanChatId = chatId?.trim()
    val url = when {
        !cleanChatId.isNullOrBlank() && messageId != null && messageId > 0 -> {
            if (cleanChatId.startsWith("-100")) {
                val stripped = cleanChatId.removePrefix("-100")
                "https://t.me/c/$stripped/$messageId"
            } else if (cleanChatId.startsWith("@")) {
                val username = cleanChatId.removePrefix("@")
                "https://t.me/$username/$messageId"
            } else {
                val stripped = cleanChatId.removePrefix("-")
                "https://t.me/c/$stripped/$messageId"
            }
        }
        !cleanChatId.isNullOrBlank() -> {
            if (cleanChatId.startsWith("-100")) {
                val stripped = cleanChatId.removePrefix("-100")
                "https://t.me/c/$stripped"
            } else if (cleanChatId.startsWith("@")) {
                val username = cleanChatId.removePrefix("@")
                "https://t.me/$username"
            } else {
                val stripped = cleanChatId.removePrefix("-")
                "https://t.me/c/$stripped"
            }
        }
        else -> "https://t.me"
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open Telegram: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MoveFolderDialog(
    item: VaultItem,
    folders: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (selectedFolder: String) -> Unit
) {
    var selectedFolder by remember { mutableStateOf(item.folder) }
    var customFolderInput by remember { mutableStateOf("") }
    var showCustomFolderField by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MOVE TO FOLDER",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        },
        containerColor = CosmicSlate,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Select an existing folder of Abyss Cloud, or input a custom destination path.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Text(
                    text = "Current folder: ${item.folder}",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // List of folders
                Column {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        items(folders) { folder ->
                            val isSelected = selectedFolder == folder && !showCustomFolderField
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (isSelected) NeonCyan else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedFolder = folder
                                        showCustomFolderField = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = folder, color = if (isSelected) NeonCyan else Color.LightGray, fontSize = 12.sp)
                            }
                        }

                        // Custom Folder Chip
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (showCustomFolderField) NeonPurple.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (showCustomFolderField) NeonPurple else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        showCustomFolderField = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = "+ Custom...", color = if (showCustomFolderField) NeonPurple else Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    }

                    if (showCustomFolderField) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = customFolderInput,
                            onValueChange = { customFolderInput = it },
                            label = { Text("New Folder Name", fontSize = 11.sp, color = NeonPurple) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalFolder = if (showCustomFolderField) {
                        customFolderInput.trim().ifEmpty { "General" }
                    } else {
                        selectedFolder
                    }
                    onConfirm(finalFolder)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Confirm Move", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
fun MoveMultipleItemsDialog(
    itemCount: Int,
    folders: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (selectedFolder: String) -> Unit
) {
    var selectedFolder by remember { mutableStateOf("General") }
    var customFolderInput by remember { mutableStateOf("") }
    var showCustomFolderField by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MOVE MULTIPLE ITEMS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        },
        containerColor = CosmicSlate,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Select a destination folder for $itemCount items.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                if (showCustomFolderField) {
                    OutlinedTextField(
                        value = customFolderInput,
                        onValueChange = { customFolderInput = it },
                        label = { Text("Custom Folder Name", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_folder_input")
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = selectedFolder, color = Color.White)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray)
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(CosmicSlate).fillMaxWidth(0.6f)
                        ) {
                            folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder, color = Color.White) },
                                    onClick = {
                                        selectedFolder = folder
                                        expanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Add Custom Folder", color = NeonCyan) },
                                onClick = {
                                    showCustomFolderField = true
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalFolder = if (showCustomFolderField && customFolderInput.isNotBlank()) {
                        customFolderInput.trim()
                    } else {
                        selectedFolder
                    }
                    onConfirm(finalFolder)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier.testTag("dialog_confirm_btn")
            ) {
                Text("MOVE", color = CosmicBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_cancel_btn")) {
                Text("CANCEL", color = SoftCoral)
            }
        }
    )
}

@Composable
fun ThemeSelectorSection(
    currentThemeIndex: Int,
    darkModeMode: Int,
    onThemeSelected: (Int) -> Unit,
    onDarkModeSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CosmicGlass)
            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            "APPEARANCE MODE",
            color = NeonCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            "Toggle between crisp Light Theme, immersive Dark Theme, or System Auto.",
            color = TextSecondary,
            fontSize = 11.sp,
            style = LocalTextStyle.current.copy(lineHeight = 15.sp),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modes = listOf(
                Triple(0, "Auto", Icons.Default.BrightnessAuto),
                Triple(1, "Light", Icons.Default.WbSunny),
                Triple(2, "Dark", Icons.Default.NightsStay)
            )

            modes.forEach { (modeVal, label, icon) ->
                val isSelected = darkModeMode == modeVal
                Button(
                    onClick = { onDarkModeSelected(modeVal) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dark_mode_option_$modeVal"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) NeonCyan else CardBorderColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) NeonCyan else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        color = if (isSelected) NeonCyan else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = CardBorderColor)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "DYNAMIC PALETTE",
            color = TextGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            "Select a customized theme styling palette for Abyss Cloud's local-first interfaces.",
            color = TextSecondary,
            fontSize = 11.sp,
            style = LocalTextStyle.current.copy(lineHeight = 15.sp),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(ThemesList) { index, theme ->
                val isSelected = currentThemeIndex == index
                val primary = if (isAppInDarkTheme) theme.primaryDark else theme.primaryLight
                val secondary = if (isAppInDarkTheme) theme.secondaryDark else theme.secondaryLight

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) primary.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) primary else CardBorderColor,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onThemeSelected(index) }
                        .padding(vertical = 10.dp, horizontal = 6.dp)
                ) {
                    // Double color swatch preview
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-4).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(primary)
                                .border(1.dp, CardBorderColor, RoundedCornerShape(50.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(secondary)
                                .border(1.dp, CardBorderColor, RoundedCornerShape(50.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = theme.themeName,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.MainActivityViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TokTipAppEntry()
            }
        }
    }
}

@Composable
fun TokTipAppEntry() {
    val viewModel: MainActivityViewModel = viewModel()
    val currentTab by viewModel.selectedTab.collectAsState()
    val pointMessage by viewModel.pointFloatMessage.collectAsState()
    val showAuthDialog by viewModel.showAuthDialog.collectAsState()

    // Base Scaffold spanning full edge-to-edge area
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            TokTipBottomBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        containerColor = Color(0xFF0C0C14) // Rich cosmic black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()) // Respect navigation safe heights
        ) {
            // Screen router
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith
                            fadeOut(animationSpec = tween(220))
                },
                label = "navigation_tabs"
            ) { tab ->
                when (tab) {
                    "feed" -> VideoFeedScreen(viewModel = viewModel)
                    "wallet" -> WalletAndEarningsScreen(viewModel = viewModel)
                    "add_video" -> UploadModeratorScreen(viewModel = viewModel)
                }
            }

            // High-fidelity Floating Points Toast overlay
            AnimatedVisibility(
                visible = pointMessage != null,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = fadeOut() + slideOutVertically { -it / 2 },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            ) {
                pointMessage?.let { msg ->
                    PointsFlyerNotification(message = msg)
                }
            }

            // Universal Auth popup modal Dialog
            if (showAuthDialog) {
                AuthSheetDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.showAuthDialog.value = false }
                )
            }
        }
    }
}

// Customized Navigation Bar matching M3 specifications
@Composable
fun TokTipBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF131322),
        tonalElevation = 8.dp,
        modifier = Modifier.shadow(20.dp, spotColor = Color.Black)
    ) {
        NavigationBarItem(
            selected = currentTab == "feed",
            onClick = { onTabSelected("feed") },
            label = { Text("Feed", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
            icon = {
                Icon(
                    imageVector = if (currentTab == "feed") Icons.Default.PlayArrow else Icons.Outlined.PlayArrow,
                    contentDescription = "Videos Scroll Feed"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFFD700),
                selectedTextColor = Color(0xFFFFD700),
                indicatorColor = Color(0xFF232338)
            ),
            modifier = Modifier.testTag("nav_feed_tab")
        )

        NavigationBarItem(
            selected = currentTab == "add_video",
            onClick = { onTabSelected("add_video") },
            label = { Text("Publish", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload Creator"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF33D188),
                selectedTextColor = Color(0xFF33D188),
                indicatorColor = Color(0xFF1D322A)
            ),
            modifier = Modifier.testTag("nav_publish_tab")
        )

        NavigationBarItem(
            selected = currentTab == "wallet",
            onClick = { onTabSelected("wallet") },
            label = { Text("Wallet", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountBox,
                    contentDescription = "Wallet Referrals"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF00D2FF),
                selectedTextColor = Color(0xFF00D2FF),
                indicatorColor = Color(0xFF132D42)
            ),
            modifier = Modifier.testTag("nav_wallet_tab")
        )
    }
}

// Float point feedback bubble
@Composable
fun PointsFlyerNotification(message: String) {
    Surface(
        color = Color(0xFFFFD700),
        contentColor = Color.Black,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .border(2.dp, Color.White, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Star",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// Interactive Sign In / Sign Up Composable Dialog card
@Composable
fun AuthSheetDialog(
    viewModel: MainActivityViewModel,
    onDismiss: () -> Unit
) {
    val loginError by viewModel.loginError.collectAsState()
    val registerError by viewModel.registerError.collectAsState()

    var isSignUpMode by remember { mutableStateOf(false) }

    // Input state containers
    var usernameField by remember { mutableStateOf("") }
    var emailField by remember { mutableStateOf("") }
    var passwordField by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF131322),
            border = BorderStroke(1.dp, Color(0xFF00D2FF).copy(alpha = 0.25f)),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Glow lock icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF00D2FF), Color(0xFFFFD700))
                            ), CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Authentication Portal",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (isSignUpMode) "Create Free Account" else "Sign In to TokTip",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = if (isSignUpMode) {
                        "Claim 500 free Tip Coins immediately upon sign up to tip your favorite creators!"
                    } else {
                        "Access your personalized profile dashboard, loyalty points history, and content creator tips"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                // Error feedback system
                val currentErr = if (isSignUpMode) registerError else loginError
                if (currentErr != null) {
                    Surface(
                        color = Color(0xFF351717),
                        border = BorderStroke(1.dp, Color(0xFFE94057)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error icon",
                                tint = Color(0xFFE94057),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = currentErr,
                                color = Color(0xFFE94057),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Input field layouts
                OutlinedTextField(
                    value = usernameField,
                    onValueChange = { usernameField = it },
                    label = { Text("Username", fontSize = 12.sp) },
                    placeholder = { Text("e.g. creative_mind", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00D2FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = Color(0xFF00D2FF),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Icon",
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_username_input")
                )

                if (isSignUpMode) {
                    OutlinedTextField(
                        value = emailField,
                        onValueChange = { emailField = it },
                        label = { Text("Email Address", fontSize = 12.sp) },
                        placeholder = { Text("e.g. user@toktip.app", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00D2FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = Color(0xFF00D2FF),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Email Icon",
                                tint = Color.White.copy(alpha = 0.4f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )
                }

                OutlinedTextField(
                    value = passwordField,
                    onValueChange = { passwordField = it },
                    label = { Text("Password", fontSize = 12.sp) },
                    placeholder = { Text("••••••••", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00D2FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = Color(0xFF00D2FF),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password Lock Icon",
                            tint = Color.White.copy(alpha = 0.4f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Submit Action Button
                Button(
                    onClick = {
                        if (isSignUpMode) {
                            viewModel.signUp(usernameField, emailField, passwordField, onSuccess = {
                                onDismiss()
                            })
                        } else {
                            viewModel.signIn(usernameField, passwordField, onSuccess = {
                                onDismiss()
                            })
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D2FF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auth_submit_action")
                ) {
                    Text(
                        text = if (isSignUpMode) "Sign Up & Get 500 Coins" else "Log In Successfully",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Switch modes toggle
                TextButton(
                    onClick = {
                        isSignUpMode = !isSignUpMode
                        viewModel.loginError.value = null
                        viewModel.registerError.value = null
                    },
                    modifier = Modifier.testTag("auth_toggle_mode_trigger")
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account? Sign In" else "New to TokTip? Create Free Account",
                        color = Color(0xFFFFD700),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip / Browse Videos")
                }
            }
        }
    }
}

// Video Feed Tab Screen combining vertical swipe gesture mechanics and canvas rendering
@Composable
fun VideoFeedScreen(viewModel: MainActivityViewModel) {
    val videos by viewModel.safeVideos.collectAsState()
    val activeIdx by viewModel.activeVideoIndex.collectAsState()
    val stateWallet by viewModel.walletState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val watchSecs by viewModel.watchSeconds.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showTipDialog by remember { mutableStateOf<VideoEntity?>(null) }
    var showCommentsDialog by remember { mutableStateOf<VideoEntity?>(null) }
    var dragAmountY by remember { mutableStateOf(0f) }

    if (videos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                CircularProgressIndicator(color = Color(0xFFFFD700))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Syndicating beautiful visualizer channels...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    val activeVideo = videos.getOrNull(activeIdx) ?: videos.first()

    // Outer Container handling simulated swipe gestures
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragAmountY < -120f) {
                            // Swiped Up -> Next video
                            if (activeIdx < videos.size - 1) {
                                viewModel.changeVideo(activeIdx + 1)
                            } else {
                                Toast
                                    .makeText(context, "You've scrolled to the end!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } else if (dragAmountY > 120f) {
                            // Swiped Down -> Previous video
                            if (activeIdx > 0) {
                                viewModel.changeVideo(activeIdx - 1)
                            }
                        }
                        dragAmountY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAmountY += dragAmount.y
                    }
                )
            }
    ) {
        // High fidelity Animated Video Simulation in background
        VideoArtVisualizer(
            themeType = activeVideo.type,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        // Custom Double Tap overlay to Like video
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeVideo.id) {
                    detectTapGestures(
                        onDoubleTap = {
                            val currentUserName = stateWallet?.currentUserName ?: ""
                            if (currentUserName.isEmpty()) {
                                viewModel.showAuthDialog.value = true
                                Toast.makeText(context, "Please sign up/in to like videos!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.performLike(activeVideo.id, activeVideo.isLiked)
                                Toast
                                    .makeText(context, "♥ Liked this visualizer!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        onTap = {
                            viewModel.togglePlay()
                        }
                    )
                }
        )

        // top status & ticker panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Loyalty watch multiplier indicator
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) Color.Green else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Next bonus in: ${15 - watchSecs}s",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Balance live ticker
                Surface(
                    color = Color(0xFF181829),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF00D2FF).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Wallet Balance",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stateWallet?.coinsBalance ?: 0.0} Coins",
                            color = Color(0xFF00D2FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // play/pause simulation watermark overlay
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center)
                    .shadow(30.dp, CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // vertical action elements aligned on the extreme right
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Circle Creator Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .shadow(8.dp, CircleShape)
                    .border(2.dp, Color(0xFFFFD700), CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
                        ), CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeVideo.creatorName.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Like Action Item
            VerticalFeedActionButton(
                icon = Icons.Default.Favorite,
                tint = if (activeVideo.isLiked) Color(0xFFE94057) else Color.White,
                label = activeVideo.likesCount.toString(),
                onClick = {
                    val currentUserName = stateWallet?.currentUserName ?: ""
                    if (currentUserName.isEmpty()) {
                        viewModel.showAuthDialog.value = true
                        Toast.makeText(context, "Log in to like videos!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.performLike(activeVideo.id, activeVideo.isLiked)
                    }
                },
                tag = "like_button_${activeVideo.id}"
            )

            // Tip Creator Creator Action
            VerticalFeedActionButton(
                icon = Icons.Default.Star,
                tint = Color(0xFFFFD700),
                label = "Tip",
                onClick = {
                    val currentUserName = stateWallet?.currentUserName ?: ""
                    if (currentUserName.isEmpty()) {
                        viewModel.showAuthDialog.value = true
                        Toast.makeText(context, "Log in to tip creators!", Toast.LENGTH_SHORT).show()
                    } else {
                        showTipDialog = activeVideo
                    }
                },
                tag = "tip_button_${activeVideo.id}"
            )

            // Referral Share Button
            VerticalFeedActionButton(
                icon = Icons.Default.Share,
                tint = Color(0xFF00D2FF),
                label = "Share",
                onClick = {
                    val currentUserName = stateWallet?.currentUserName ?: ""
                    if (currentUserName.isEmpty()) {
                        viewModel.showAuthDialog.value = true
                        Toast.makeText(context, "Log in to share and earn bonuses!", Toast.LENGTH_SHORT).show()
                    } else {
                        val refLink = "https://toktip.app/ref/${stateWallet?.referralCode ?: "TOKREF88"}/vid${activeVideo.id}"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("TokTip Referral Link", refLink))
                        
                        Toast.makeText(
                            context,
                            "Referral Link copied with referral code! Generated reward pending.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Simulate custom random referral registration after sharing for rich interactive demonstration!
                        scope.launch {
                            viewModel.repository.triggerReferralSignup(
                                stateWallet?.referralCode ?: "TOKREF88",
                                listOf("Ali_Super", "RameshKumar", "ZaraNez", "TechGeek", "SaraS").random()
                            )
                        }
                    }
                },
                tag = "share_button_${activeVideo.id}"
            )

            // Comments button option
            VerticalFeedActionButton(
                icon = Icons.Default.Email,
                tint = Color(0xFF00FF7F),
                label = "Comment",
                onClick = {
                    showCommentsDialog = activeVideo
                },
                tag = "comment_button_${activeVideo.id}"
            )

            // Up / Down Help selectors (handy placeholder fallback for drag gestures)
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        if (activeIdx > 0) viewModel.changeVideo(activeIdx - 1)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        text = "▲",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = {
                        if (activeIdx < videos.size - 1) viewModel.changeVideo(activeIdx + 1)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        text = "▼",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Overlay of description and policy verification on bottom left
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.8f)
                .padding(start = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "@${activeVideo.creatorName}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.clickable {
                        viewModel.toggleFollowCreator(activeVideo.creatorName)
                    }
                )

                // Follow Badge Button option
                val followsState by viewModel.allFollows.collectAsState(initial = emptyList())
                val isFollowingCreator = followsState.any { it.creatorName.lowercase() == activeVideo.creatorName.lowercase() }
                
                Surface(
                    color = if (isFollowingCreator) Color(0xFF33D188).copy(alpha = 0.2f) else Color(0xFF00D2FF).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (isFollowingCreator) Color(0xFF33D188) else Color(0xFF00D2FF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable {
                        viewModel.toggleFollowCreator(activeVideo.creatorName)
                    }
                ) {
                    Text(
                        text = if (isFollowingCreator) "Following ✓" else "Follow +",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFollowingCreator) Color(0xFF33D188) else Color(0xFF00D2FF),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Tips state summary label
                if (activeVideo.tipsCount > 0) {
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFFFFD700)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "⚡ Tipped ${activeVideo.tipsCount}c",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = activeVideo.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = activeVideo.description,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // AI Moderation shield badge
            Surface(
                color = Color(0xFF1D322A),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF33D188).copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "AI Moderated",
                        tint = Color(0xFF33D188),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Verified Clean: Automated AI Moderation API Approved",
                        fontSize = 10.sp,
                        color = Color(0xFF33D188),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Comments interactive dialog
    if (showCommentsDialog != null) {
        val targetVideo = showCommentsDialog!!
        val commentsFlow = remember(targetVideo.id) { viewModel.getCommentsForVideo(targetVideo.id) }
        val commentsList by commentsFlow.collectAsState(initial = emptyList())
        var newCommentText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCommentsDialog = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0C0C1E),
                border = BorderStroke(1.dp, Color(0xFF00D2FF).copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Comments",
                                tint = Color(0xFF00D2FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Discussion Hub (${commentsList.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = { showCommentsDialog = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Divider line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    // Comments list container
                    Box(modifier = Modifier.weight(1f)) {
                        if (commentsList.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "💬 No comments yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Be the first to share your thoughts!",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(commentsList) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFF00D2FF), Color(0xFF8A2387))
                                                    ), CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.username.take(2).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "@${item.username}",
                                                    color = Color(0xFFFFD700),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Just Now",
                                                    color = Color.White.copy(alpha = 0.3f),
                                                    fontSize = 9.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = item.text,
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 12.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Input Form
                    val guestName = stateWallet?.currentUserName ?: ""
                    if (guestName.isEmpty()) {
                        Surface(
                            color = Color(0xFF1E1412),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Log in to comment and earn loyalty points! ",
                                    color = Color(0xFFFFD700),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Sign In",
                                    color = Color(0xFF00D2FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        viewModel.showAuthDialog.value = true
                                    }
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newCommentText,
                                onValueChange = { newCommentText = it },
                                placeholder = {
                                    Text(
                                        "Add a beautiful comment...",
                                        color = Color.White.copy(alpha = 0.35f),
                                        fontSize = 12.sp
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00D2FF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedContainerColor = Color(0xFF13132B),
                                    unfocusedContainerColor = Color(0xFF13132B)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("comment_input_box")
                            )

                            IconButton(
                                onClick = {
                                    if (newCommentText.trim().isNotEmpty()) {
                                        viewModel.submitComment(targetVideo.id, newCommentText) { success ->
                                            if (success) {
                                                newCommentText = ""
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .background(Color(0xFF00D2FF), CircleShape)
                                    .size(40.dp)
                                    .testTag("comment_submit_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Gift/Tip popover dialog
    if (showTipDialog != null) {
        val targetCreator = showTipDialog!!
        Dialog(onDismissRequest = { showTipDialog = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1A1A2E),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f)),
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Tip Creator Balance",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "How many coins would you like to tip creator @${targetCreator.creatorName}?",
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(5.0, 20.0, 50.0, 100.0).forEach { amount ->
                            Button(
                                onClick = {
                                    viewModel.tipVideoCreator(
                                        targetCreator.id,
                                        targetCreator.creatorName,
                                        amount,
                                        onSuccess = {
                                            showTipDialog = null
                                            Toast.makeText(context, "Succeeded tipping $amount coins!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { errMsg ->
                                            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF26264C),
                                    contentColor = Color(0xFFFFD700)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(amount.toInt().toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showTipDialog = null },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

// Side widget helper component
@Composable
fun VerticalFeedActionButton(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.62f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            style = MaterialTheme.typography.bodySmall.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black,
                    blurRadius = 3f
                )
            )
        )
    }
}

// Visual simulated canvas visualizers matching selected content themes
@Composable
fun VideoArtVisualizer(
    themeType: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scenic_render")
    
    // Smooth infinite progress clocks
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulsingFactor by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = SineCrossingEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsing"
    )

    val wavesTranslation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waves"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        when (themeType) {
            "SUNSET_COAST" -> {
                // Sunset visual background
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE94057),
                            Color(0xFFF27121),
                            Color(0xFF26101E)
                        )
                    )
                )

                // draw big glowing sun in center
                drawCircle(
                    color = Color(0xFFFFE066),
                    radius = 120.dp.toPx() * pulsingFactor,
                    center = Offset(width / 2, height / 2)
                )

                // draw moving ocean wave layers at the bottom
                val path = Path().apply {
                    moveTo(0f, height)
                    lineTo(0f, height * 0.72f)
                    // Generate nice smooth sine wave
                    for (x in 0..width.toInt() step 10) {
                        val relativeX = x + wavesTranslation
                        val y = height * 0.72f + sin(relativeX * 0.008f) * 12.dp.toPx()
                        lineTo(x.toFloat(), y)
                    }
                    lineTo(width, height)
                    close()
                }
                drawPath(path = path, color = Color(0xFF1B0F2A).copy(alpha = 0.82f))
            }

            "NEON_CYBERPUNK" -> {
                // Cyberpunk linear neon grid
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF150024),
                            Color(0xFF1E0E3D),
                            Color(0xFF03001F)
                        )
                    )
                )

                // Radiant neon sun on middle top
                drawCircle(
                    color = Color(0xFFFF007F),
                    radius = 90.dp.toPx(),
                    center = Offset(width / 2, height * 0.38f)
                )

                // Horizontal scanline bands over sun
                for (y in (height * 0.2f).toInt()..(height * 0.5f).toInt() step 24) {
                    drawLine(
                        color = Color(0xFF03001F),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(width, y.toFloat()),
                        strokeWidth = 6.dp.toPx()
                    )
                }

                // Grid perspective lines intersecting toward sun center
                val horizonY = height * 0.55f
                drawLine(
                    color = Color(0xFF00FFFF),
                    start = Offset(0f, horizonY),
                    end = Offset(width, horizonY),
                    strokeWidth = 3.dp.toPx()
                )

                // Draw falling projection grid tracks
                val gridPoints = 8
                for (i in 0..gridPoints) {
                    val startX = (width / gridPoints) * i
                    drawLine(
                        color = Color(0xFF00FFFF).copy(alpha = 0.6f),
                        start = Offset(width / 2, horizonY),
                        end = Offset(startX, height),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Scrolling horizontal grid bars
                for (i in 1..6) {
                    // Calculate logarithmic distance
                    val fraction = i * 0.16f + (wavesTranslation % 200) / 1500f
                    val y = horizonY + (height - horizonY) * fraction
                    drawLine(
                        color = Color(0xFFFF00FF).copy(alpha = 0.7f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            "ABSTRACT_ZEN" -> {
                // Deep midnight meditation backdrop
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF281358),
                            Color(0xFF10072E),
                            Color(0xFF070014)
                        ),
                        center = Offset(width / 2, height / 2)
                    )
                )

                // concentric geometric lotus mandalas
                val mandalas = 12
                val originX = width / 2
                val originY = height / 2
                
                rotate(rotation) {
                    for (i in 1..mandalas) {
                        val baseRadius = (i * 24).dp.toPx() * pulsingFactor
                        drawCircle(
                            color = Color(0xFFA594F9).copy(alpha = 0.42f - (i * 0.03f)),
                            radius = baseRadius,
                            center = Offset(originX, originY),
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Draw visual star anchors
                        for (pts in 0..6) {
                            val radians = Math.toRadians((pts * (360 / 6)).toDouble())
                            val ptX = originX + cos(radians).toFloat() * baseRadius
                            val ptY = originY + sin(radians).toFloat() * baseRadius
                            drawCircle(
                                color = Color(0xFFFFD700).copy(alpha = 0.7f),
                                radius = 2.dp.toPx(),
                                center = Offset(ptX, ptY)
                            )
                        }
                    }
                }
            }

            "COZY_RAIN" -> {
                // Dark rainy clouds
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF14213d), Color(0xFF000000))
                    )
                )

                // Cozy golden street lamp highlights
                drawCircle(
                    color = Color(0xFFfca311).copy(alpha = 0.25f),
                    radius = 160.dp.toPx(),
                    center = Offset(width * 0.2f, height * 0.35f)
                )
                drawCircle(
                    color = Color(0xFFfca311),
                    radius = 20.dp.toPx(),
                    center = Offset(width * 0.2f, height * 0.35f)
                )
                drawLine(
                    color = Color(0xFF333333),
                    start = Offset(width * 0.2f, height * 0.35f),
                    end = Offset(width * 0.2f, height),
                    strokeWidth = 6.dp.toPx()
                )

                // Cascading raindrop vectors
                val rainRows = 25
                for (i in 0..rainRows) {
                    val progressX = (width / rainRows) * i
                    val offsetShift = (wavesTranslation * (1.2f + (i % 3) * 0.2f)) % height
                    val dropLength = 15.dp.toPx()
                    drawLine(
                        color = Color(0xFF90E0EF).copy(alpha = 0.65f),
                        start = Offset(progressX, offsetShift),
                        end = Offset(progressX, offsetShift + dropLength),
                        strokeWidth = 1.2f.dp.toPx()
                    )
                }
            }

            "MATRIX_GRID" -> {
                // Full matrix slate black environment
                drawRect(color = Color(0xFF000000))

                // Falling binary codestreams
                val columns = 16
                for (col in 0..columns) {
                    val progressX = (width / columns) * col
                    val speedScalar = 0.9f + (col % 4) * 0.4f
                    val calculatedY = (wavesTranslation * speedScalar) % height

                    // Render vertical strings
                    for (charIdx in 0..12) {
                        val characterY = calculatedY - (charIdx * 20.dp.toPx())
                        if (characterY in 0f..height) {
                            val intensity = 1f - (charIdx * 0.08f)
                            // Draw simulated binary digits as small glowing green squares
                            drawCircle(
                                color = Color(0xFF00FF00).copy(alpha = intensity.coerceIn(0f, 1f)),
                                radius = 2.dp.toPx() * pulsingFactor,
                                center = Offset(progressX, characterY)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Ease function interpolation
object SineCrossingEasing : Easing {
    override fun transform(fraction: Float): Float {
        return sin(fraction * Math.PI).toFloat()
    }
}

// WALLET & STATISTICS TAB: Displays earnings dashboards, payouts, and copyable referral details
@Composable
fun WalletAndEarningsScreen(viewModel: MainActivityViewModel) {
    val stateWallet by viewModel.walletState.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val context = LocalContext.current

    var redeemInputCode by remember { mutableStateOf("") }
    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // PKR Cashout properties
    var selectedProvider by remember { mutableStateOf("EasyPaisa") }
    var cashoutAccountNo by remember { mutableStateOf("") }
    var cashoutCoinsAmount by remember { mutableStateOf("") }
    var activeInvoiceResult by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090911))
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        // 1. Professional visual home banner image
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF00D2FF).copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("home_professional_banner")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_home_banner),
                        contentDescription = "TokTip Professional Studio",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient overlay to make text pop
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )
                    // Content
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            color = Color(0xFFE94057),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                "PRO STUDIO PORTAL",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            "TokTip PK Monetization Hub",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Convert content rewards directly to PKR • 1 Coin = 5 PKR",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // 2. Profile Welcome / Guest header card
        item {
            val currentUserName = stateWallet?.currentUserName ?: ""
            if (currentUserName.isEmpty()) {
                Surface(
                    color = Color(0xFF1E1412),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .background(Color(0xFF33201B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Guest Avatar",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Guest Browser Mode",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Log in or Sign up free to unlock tipping, posting clips & claim 500 Coin rewards!",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Button(
                            onClick = { viewModel.showAuthDialog.value = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("wallet_guest_login_btn")
                        ) {
                            Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Surface(
                    color = Color(0xFF131F2A),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF00D2FF).copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF00D2FF), Color(0xFF00589F))
                                    ), CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUserName.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "@$currentUserName",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Surface(
                                    color = Color(0xFF33D188).copy(alpha = 0.15f),
                                    contentColor = Color(0xFF33D188),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "CREATOR",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = stateWallet?.currentUserEmail ?: "user@toktip.app",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.logout() },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("wallet_logout_btn")
                        ) {
                            Text("Logout", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 3. Creator Traffic & Monetization Tracker Card (1000 followers / 10000 views)
        item {
            val wallet = stateWallet
            if (wallet != null && wallet.currentUserName.isNotEmpty()) {
                val isMonetized = wallet.isMonetized
                val hasMetFollowers = wallet.followersCount >= 1000
                val hasMetViews = wallet.viewsCount >= 10000
                val requirementsMet = hasMetFollowers || hasMetViews

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isMonetized) Color(0xFF33D188).copy(alpha = 0.4f) else Color(0xFF00D2FF).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "TRAFFIC MONETIZATION TRACKER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Text(
                                    "Goal: 1K Followers OR 10K Views",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Active Status Indicator
                            if (isMonetized) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF33D188), CircleShape)
                                    )
                                    Text(
                                        "PKR APPROVED 🇵🇰",
                                        color = Color(0xFF33D188),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFFFFD700), CircleShape)
                                    )
                                    Text(
                                        "UNMONETIZED",
                                        color = Color(0xFFFFD700),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )

                        // Followers metric Tracker
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Person, "Followers", tint = Color(0xFF00D2FF), modifier = Modifier.size(16.dp))
                                    Text("Follower Traffic", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                                }
                                Text(
                                    "${wallet.followersCount} / 1,000",
                                    color = if (hasMetFollowers) Color(0xFF33D188) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            val followProgress = (wallet.followersCount.toFloat() / 1000f).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { followProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (hasMetFollowers) Color(0xFF33D188) else Color(0xFF00D2FF),
                                trackColor = Color.White.copy(alpha = 0.08f)
                            )
                        }

                        // Views metric Tracker
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.PlayArrow, "Views", tint = Color(0xFFFF4500), modifier = Modifier.size(16.dp))
                                    Text("Reel Playback Views", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                                }
                                Text(
                                    "${wallet.viewsCount} / 10,000",
                                    color = if (hasMetViews) Color(0xFF33D188) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            val viewProgress = (wallet.viewsCount.toFloat() / 10000f).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { viewProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (hasMetViews) Color(0xFF33D188) else Color(0xFFFF6B6B),
                                trackColor = Color.White.copy(alpha = 0.08f)
                            )
                        }

                        // Simulation Booster triggers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.boostFollowers() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("boost_followers_btn"),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text("+180 Followers 📈", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.boostViews() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("boost_views_btn"),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text("+1,250 Views 🚀", fontSize = 11.sp, color = Color(0xFFFB7185), fontWeight = FontWeight.Bold)
                            }
                        }

                        // Activation Option
                        if (!isMonetized) {
                            Button(
                                onClick = {
                                    viewModel.claimMonetization { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = requirementsMet,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF22C55E),
                                    disabledContainerColor = Color(0xFF334155),
                                    contentColor = Color.White,
                                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("claim_monetization_button")
                            ) {
                                Text(
                                    text = if (requirementsMet) "⚡ APPLY & ENABLE PKR MONETIZATION" else "🔒 GOALS PENDING (MEET TO UNLOCK)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Earning Account fully monetized",
                                    color = Color(0xFF33D188),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "Reset Demo Stats",
                                    color = Color.White.copy(alpha = 0.35f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable { viewModel.resetCreatorStats() }
                                        .padding(4.dp)
                                        .testTag("reset_creator_stats")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Live Coin Balance Dashboard Card
        item {
            Surface(
                color = Color(0xFF14142D),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("LOYALTY WATCH POINTS", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${stateWallet?.pointsBalance ?: 0} PTS",
                                color = Color(0xFFFFD700),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Watch Balance",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CREATOR TIPS RECEIVED", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${stateWallet?.creatorEarnings ?: 0.0} Coins",
                                color = Color(0xFF33D188),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column {
                            Text("USER TIP COINS BALANCE", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${stateWallet?.coinsBalance ?: 0.0} Coins",
                                color = Color(0xFF00D2FF),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 5. Digital Pakistan Earning & Cash-out Portal (Only visible when monetized)
        item {
            val wallet = stateWallet
            if (wallet != null && wallet.isMonetized) {
                val totalRemainingCoins = wallet.creatorEarnings + wallet.coinsBalance
                val estimatedPkr = totalRemainingCoins * 5.0 // 1 Coin = 5 PKR

                Surface(
                    color = Color(0xFF071F1E),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF33D188).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🇵🇰", fontSize = 18.sp)
                            Column {
                                Text(
                                    "PAKISTAN RUPEE (PKR) CASH-OUT",
                                    color = Color(0xFF33D188),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    "Official EasyPaisa & JazzCash Gateway",
                                    color = Color.White.copy(alpha = 0.55f),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )

                        // Total Estimated PKR balance
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0E2E2C), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CONVERTIBLE CASH ESTIMATE", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                Text(
                                    "Rs. $estimatedPkr PKR",
                                    color = Color(0xFF10B981),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "1 Coin = 5 Rs.",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Provider Selection Row
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Select Payment Provider", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("EasyPaisa", "JazzCash", "SadaPay", "NayaPay").forEach { provider ->
                                    val isSelected = selectedProvider == provider
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) Color(0xFF10B981) else Color(0xFF0F2624),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color(0xFF34D399) else Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedProvider = provider }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            provider,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Input Account Recipient No
                        OutlinedTextField(
                            value = cashoutAccountNo,
                            onValueChange = { cashoutAccountNo = it },
                            placeholder = { Text("e.g. 03001234567 or IBAN No", fontSize = 11.sp) },
                            label = { Text("Account / Mobile Phone Number", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = Color(0xFF34D399)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cashout_phone_number_box")
                        )

                        // Input Coins Amount to Redeem
                        OutlinedTextField(
                            value = cashoutCoinsAmount,
                            onValueChange = { cashoutCoinsAmount = it },
                            placeholder = { Text("Max Available: $totalRemainingCoins Coins", fontSize = 11.sp) },
                            label = { Text("Exchange Quantity (Coins)", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedLabelColor = Color(0xFF34D399)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cashout_coins_amount_box")
                        )

                        // Submit Button
                        Button(
                            onClick = {
                                val coinsToWithdraw = cashoutCoinsAmount.toDoubleOrNull()
                                if (coinsToWithdraw == null || coinsToWithdraw <= 0) {
                                    Toast.makeText(context, "Please input a valid positive Coin balance!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (cashoutAccountNo.trim().isEmpty()) {
                                    Toast.makeText(context, "Recipient mobile/account is missing!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.processPkrCashout(
                                    coinsToWithdraw,
                                    selectedProvider,
                                    cashoutAccountNo.trim()
                                ) { success, msg ->
                                    if (success) {
                                        activeInvoiceResult = msg
                                        cashoutCoinsAmount = ""
                                        cashoutAccountNo = ""
                                    } else {
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cashout_pkr_submit_btn")
                        ) {
                            Text(
                                "PROCESS PKR CASHOUT NOW 🇵🇰",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // 6. Viral Referral Program Option card
        item {
            Surface(
                color = Color(0xFF132D42).copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF00D2FF).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Viral Referral Program",
                        color = Color(0xFF00D2FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Invite content creators. For each referral copy share, earn immediate bonus points & cashable tip credits!",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0A1926), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("YOUR SHARE CODE", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                            Text(
                                text = stateWallet?.referralCode ?: "TOKREF88",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = {
                                clip.setPrimaryClip(ClipData.newPlainText("Code", stateWallet?.referralCode ?: "TOKREF88"))
                                Toast.makeText(context, "Copied referral code safely!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00D2FF),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Copy", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Friends Invited: ${stateWallet?.referralsCount ?: 0}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 7. Referral claim validation input
        item {
            Surface(
                color = Color(0xFF0F0F1B),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Redeem Creator Reward Code",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = redeemInputCode,
                            onValueChange = { redeemInputCode = it },
                            placeholder = { Text("e.g. TOKREF88", fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF33D188),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("referral_input_field")
                        )

                        Button(
                            onClick = {
                                viewModel.applyReferral(
                                    redeemInputCode,
                                    "User_${(1000..9999).random()}",
                                    onSuccess = {
                                        redeemInputCode = ""
                                        Toast.makeText(context, "Success rewards loaded!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = {
                                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33D188)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("apply_referral_button")
                        ) {
                            Text("Redeem", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 8. Dynamic Ledger list title
        item {
            Text(
                "Loyalty Ledger Activity Log",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Ledger row list rendering
        if (transactions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131326)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No loyalty logs found. Start watching videos to earn!",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(transactions) { tx ->
                LedgerRowItem(tx)
            }
        }
    }

    // PKR Official Cashout invoice printable pop-up
    if (activeInvoiceResult != null) {
        Dialog(onDismissRequest = { activeInvoiceResult = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0A141A),
                border = BorderStroke(1.dp, Color(0xFF33D188).copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Success Green stamp
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFF33D188).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = Color(0xFF33D188),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "TRANSACTION SUCCESSFUL",
                            color = Color(0xFF33D188),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "State Bank of Pakistan Simulated Payout",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    // Details block
                    Text(
                        text = activeInvoiceResult ?: "",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    // Footer receipt meta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tax Withheld: 1% (WHT)", color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp)
                        Text("ID: TX-PKR-${(100000..999999).random()}", color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = { activeInvoiceResult = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33D188)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("close_invoice_btn")
                    ) {
                        Text("Dismiss Receipt", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// Transaction list row element
@Composable
fun LedgerRowItem(tx: TransactionEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_item_${tx.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111122)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconTint = when (tx.type) {
                "WATCH_REWARD" -> Color(0xFFFFD700)
                "TIP_SENT" -> Color(0xFFE94057)
                "TIP_RECEIVED" -> Color(0xFF33D188)
                "REFERRAL_REWARD" -> Color(0xFF00D2FF)
                else -> Color.White
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconTint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (tx.type) {
                        "WATCH_REWARD" -> Icons.Default.Star
                        "TIP_SENT" -> Icons.Default.Favorite
                        "TIP_RECEIVED" -> Icons.Default.Star
                        "REFERRAL_REWARD" -> Icons.Default.Person
                        else -> Icons.Default.AccountBox
                    },
                    contentDescription = tx.type,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.details,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = java.text.SimpleDateFormat("MMM dd, HH:mm:ss").format(java.util.Date(tx.timestamp)),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            val formattedAmt = when (tx.type) {
                "WATCH_REWARD", "REFERRAL_REWARD" -> "+${tx.amount.toInt()}"
                "TIP_SENT" -> "-${tx.amount.toInt()}"
                "TIP_RECEIVED" -> "+${tx.amount.toInt()}"
                else -> "${tx.amount.toInt()}"
            }

            Text(
                text = formattedAmt,
                color = iconTint,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// CREATOR FORM TAB: Hosts content creation models and live automated AI Moderation API validations
@Composable
fun UploadModeratorScreen(viewModel: MainActivityViewModel) {
    val loading by viewModel.moderationLoading.collectAsState()
    val reportResult by viewModel.moderationResult.collectAsState()
    val isApproved by viewModel.moderationSuccess.collectAsState()
    val stateWallet by viewModel.walletState.collectAsState()
    val currentUserName = stateWallet?.currentUserName ?: ""
    val context = LocalContext.current

    var creatorName by remember { mutableStateOf("") }
    LaunchedEffect(currentUserName) {
        if (currentUserName.isNotEmpty()) {
            creatorName = currentUserName
        }
    }

    var title by remember { mutableStateOf("Cozy Rainy Cityscape") }
    var description by remember { mutableStateOf("Chill rain drops falling over neon-soaked urban rooftops.") }
    var tags by remember { mutableStateOf("#rainy #lofi #urban") }
    
    // Choose dynamic visual rendering type
    var selectedTheme by remember { mutableStateOf("COZY_RAIN") }
    val channels = listOf(
        "COZY_RAIN" to "Cozy Lofi Rain",
        "SUNSET_COAST" to "Sunset Coastal",
        "NEON_CYBERPUNK" to "Synthwave Highway",
        "ABSTRACT_ZEN" to "Geometric Zen",
        "MATRIX_GRID" to "Digital Matrix"
    )

    if (currentUserName.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090911))
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF1B1B36), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Studio Locked",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Creator Studio Locked",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = "Sign up or Log in to unlock our safe publishing API! Create original generated abstract loops, get monitored by Gemini AI, and earn tips & coins directly from viewers.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.showAuthDialog.value = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D2FF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp)
                ) {
                    Text("Sign In or Register Now", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090911))
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                "Automated Safe-Publishing API",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Publish new visualizer clips utilizing fully integrated Gemini AI Moderation API checks.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        // Creator form panel
        Surface(
            color = Color(0xFF131326),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                Column {
                    Text("Creator Pen Name", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = creatorName,
                        onValueChange = { creatorName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F0F23),
                            unfocusedContainerColor = Color(0xFF0F0F23),
                            focusedBorderColor = Color(0xFFFFD700)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("creator_name_input")
                    )
                }

                // Video Title
                Column {
                    Text("Clip Title", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F0F23),
                            unfocusedContainerColor = Color(0xFF0F0F23),
                            focusedBorderColor = Color(0xFFFFD700)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("video_title_input")
                    )
                }

                // Description
                Column {
                    Text("Description / Storyline", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F0F23),
                            unfocusedContainerColor = Color(0xFF0F0F23),
                            focusedBorderColor = Color(0xFFFFD700)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("video_description_input")
                    )
                }

                // Tags
                Column {
                    Text("Hashtags", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F0F23),
                            unfocusedContainerColor = Color(0xFF0F0F23),
                            focusedBorderColor = Color(0xFFFFD700)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("video_tags_input")
                    )
                }

                // Scene Renderer Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Select Visualizer Canvas Render", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        channels.forEach { (typeKey, typeLabel) ->
                            val isSelected = selectedTheme == typeKey
                            Surface(
                                color = if (isSelected) Color(0xFFFFD700).copy(alpha = 0.15f) else Color(0xFF0F0F23),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .clickable { selectedTheme = typeKey }
                                    .testTag("theme_btn_$typeKey")
                            ) {
                                Text(
                                    text = typeLabel,
                                    color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Trigger Button
        Button(
            onClick = {
                if (title.isBlank() || description.isBlank() || creatorName.isBlank()) {
                    Toast.makeText(context, "All form fields are required!", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                viewModel.publishNewVideo(
                    title = title,
                    description = description,
                    tags = tags,
                    creatorName = creatorName,
                    theme = selectedTheme,
                    onProcessed = { safe, report ->
                        if (safe) {
                            Toast.makeText(context, "Published! AI Moderation check passed.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Rejected by policy compliance moderation checker!", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("publish_video_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF33D188),
                disabledContainerColor = Color(0xFF1D322A),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Analyzing metadata with Gemini AI API...", color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Publish Verify")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Moderation API & Publish", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        // Live Moderation Verification report status box
        AnimatedVisibility(
            visible = reportResult != null,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = fadeOut()
        ) {
            reportResult?.let { report ->
                val approved = isApproved == true
                Surface(
                    color = if (approved) Color(0xFF162E24) else Color(0xFF351717),
                    border = BorderStroke(1.dp, if (approved) Color(0xFF33D188) else Color(0xFFE94057)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("moderation_report_panel")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (approved) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = "Status icon",
                                tint = if (approved) Color(0xFF33D188) else Color(0xFFE94057),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (approved) "Moderation Check Passed" else "Moderation Violated policy",
                                color = if (approved) Color(0xFF33D188) else Color(0xFFE94057),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = report,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        )

                        if (!approved) {
                            Text(
                                text = "🔒 Warning: To keep the platform safe, this video is blocked from publishing to the general scrolling category. Try rewriting metadata without using malicious triggers.",
                                color = Color.White.copy(alpha = 0.62f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        } else {
                            Text(
                                text = "✨ Instant Sync: Successfully syndicated to local Room persistence. Check out the Video Feed tab to preview!",
                                color = Color.White.copy(alpha = 0.62f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

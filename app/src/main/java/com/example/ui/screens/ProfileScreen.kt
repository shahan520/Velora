@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.VeloraData
import com.example.model.*
import com.example.ui.components.VeloraTopBar
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    user: UserProfile = VeloraData.currentUser,
    isPremium: Boolean,
    onEditProfile: () -> Unit,
    onPremiumClick: () -> Unit,
    onBoostClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSafetyClick: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            VeloraTopBar(
                title = "Profile",
                onNotificationsClick = onNotificationsClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
        ) {
            // User Header Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clickable(onClick = onEditProfile)
                        .testTag("profile_avatar_box")
                ) {
                    AsyncImage(
                        model = user.effectivePrimaryPhoto,
                        contentDescription = user.name,
                        modifier = Modifier
                            .size(78.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, VeloraPink, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    // Camera edit icon indicator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(VeloraPink)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = VeloraTextPrimary
                    )
                    Text(
                        text = "${user.city}, ${user.country}",
                        fontSize = 13.sp,
                        color = VeloraTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Edit Profile",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VeloraPink,
                        modifier = Modifier
                            .clickable(onClick = onEditProfile)
                            .testTag("edit_profile_link")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Premium Membership Banner Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = VeloraPlumDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPremiumClick)
                    .testTag("premium_banner_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(VeloraGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Crown",
                                tint = VeloraGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Velora Premium",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isPremium) "Active Member \u2705" else "Get more features & unlimited likes",
                                fontSize = 12.sp,
                                color = VeloraLavender
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = VeloraGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Menu Items List
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    ProfileMenuItem(
                        icon = Icons.Outlined.Person,
                        title = "My Profile",
                        onClick = onEditProfile,
                        testTag = "menu_my_profile"
                    )
                    ProfileMenuItem(
                        icon = Icons.Outlined.Bolt,
                        title = "Boost & Super Like",
                        onClick = onBoostClick,
                        testTag = "menu_boost"
                    )
                    ProfileMenuItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Notifications",
                        onClick = onNotificationsClick,
                        testTag = "menu_notifications"
                    )
                    ProfileMenuItem(
                        icon = Icons.Outlined.Security,
                        title = "Privacy & Safety",
                        onClick = onSafetyClick,
                        testTag = "menu_privacy"
                    )
                    ProfileMenuItem(
                        icon = Icons.Outlined.HelpOutline,
                        title = "Help & Support",
                        onClick = { /* help support */ },
                        testTag = "menu_help"
                    )
                    ProfileMenuItem(
                        icon = Icons.Outlined.Logout,
                        title = "Log Out",
                        onClick = onLogout,
                        isDestructive = true,
                        testTag = "menu_logout"
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    badgeColor: Color? = null,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isDestructive) VeloraErrorRed else (badgeColor ?: VeloraTextPrimary),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDestructive) VeloraErrorRed else VeloraTextPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = VeloraTextMuted
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = VeloraTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun PremiumSubscriptionScreen(
    plans: List<SubscriptionPlan> = VeloraData.subscriptionPlans,
    isAlreadyPremium: Boolean,
    onSubscribe: (SubscriptionPlan) -> Unit,
    onBack: () -> Unit,
    mfsConfig: com.example.server.MfsMerchantConfig = com.example.server.MfsMerchantConfig(),
    onSubmitMfsPayment: ((method: String, senderPhone: String, trxId: String, plan: SubscriptionPlan) -> Unit)? = null,
    isPaymentProcessing: Boolean = false,
    paymentSuccessDialogVisible: Boolean = false,
    paymentSuccessMessage: String? = null,
    onDismissPaymentSuccess: () -> Unit = {}
) {
    var selectedPlan by remember { mutableStateOf(plans.firstOrNull { it.isPopular } ?: plans.first()) }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    // Update selectedPlan if plans change
    LaunchedEffect(plans) {
        if (plans.isNotEmpty() && plans.none { it.id == selectedPlan.id }) {
            selectedPlan = plans.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Velora Premium", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VeloraPlumDark)
            )
        },
        bottomBar = {
            Surface(
                color = VeloraPlumDark,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (onSubmitMfsPayment != null) {
                                showCheckoutDialog = true
                            } else {
                                onSubscribe(selectedPlan)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("subscribe_now_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
                        shape = RoundedCornerShape(27.dp)
                    ) {
                        Text(
                            text = if (isAlreadyPremium) "Manage Plan" else "Subscribe Now • ৳${selectedPlan.priceBdt} BDT",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(VeloraPlumDark)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(VeloraGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "Crown",
                    tint = VeloraGold,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Velora Premium",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Unlock unlimited likes, VIP features & real matches in Bangladesh",
                fontSize = 13.sp,
                color = VeloraLavender,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bangladeshi MFS Accepted Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = VeloraPlumSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, VeloraPink.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = "MFS",
                            tint = VeloraPink,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bangladeshi Payments Supported",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE2136E)
                        ) {
                            Text(
                                text = "bKash",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF7931E)
                        ) {
                            Text(
                                text = "Nagad",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Benefits Checklist
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = VeloraPlumMedium),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    listOf(
                        "See who liked you",
                        "Unlimited daily likes",
                        "Advanced discovery filters",
                        "Ad-free romantic experience",
                        "Free monthly Profile Boost",
                        "Weekly Super Likes bonus"
                    ).forEach { benefit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = VeloraGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = benefit,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pricing Plans Selector with BDT Currency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                plans.forEach { plan ->
                    val isSelected = selectedPlan.id == plan.id
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedPlan = plan }
                            .testTag("plan_${plan.id}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) VeloraPlumSurface else VeloraPlumMedium,
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            if (isSelected) VeloraPink else Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (plan.savingsPercent > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = VeloraPink,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text(
                                        text = "Save ${plan.savingsPercent}%",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = plan.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "৳${plan.priceBdt}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = VeloraPinkLight
                            )
                            Text(
                                text = "BDT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VeloraLavender
                            )
                        }
                    }
                }
            }
        }
    }

    // Checkout Sheet Modal
    if (showCheckoutDialog) {
        MfsCheckoutDialog(
            plan = selectedPlan,
            mfsConfig = mfsConfig,
            isProcessing = isPaymentProcessing,
            onConfirmPayment = { method, phone, trxId ->
                if (onSubmitMfsPayment != null) {
                    onSubmitMfsPayment(method, phone, trxId, selectedPlan)
                } else {
                    onSubscribe(selectedPlan)
                }
                showCheckoutDialog = false
            },
            onDismiss = { showCheckoutDialog = false }
        )
    }

    // Payment Success Alert
    if (paymentSuccessDialogVisible && !paymentSuccessMessage.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = onDismissPaymentSuccess,
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = VeloraOnlineGreen,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Subscription Activated!", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(paymentSuccessMessage, fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = onDismissPaymentSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
                    modifier = Modifier.testTag("dismiss_payment_success_button")
                ) {
                    Text("Awesome!", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun MfsCheckoutDialog(
    plan: SubscriptionPlan,
    mfsConfig: com.example.server.MfsMerchantConfig,
    isProcessing: Boolean,
    onConfirmPayment: (method: String, senderPhone: String, trxId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMethod by remember {
        mutableStateOf(if (mfsConfig.bKashEnabled) "BKASH" else if (mfsConfig.nagadEnabled) "NAGAD" else "CARD")
    }
    var senderPhone by remember { mutableStateOf("01712-345678") }
    var trxIdInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Checkout", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = VeloraPinkSoft
                ) {
                    Text(
                        text = "৳ ${plan.priceBdt} BDT",
                        color = VeloraPink,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Payment Method",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VeloraTextSecondary
                )

                // Method Selector Chips (bKash, Nagad, Card)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // bKash Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedMethod == "BKASH") (if (isSystemInDarkTheme()) Color(0xFF381525) else Color(0xFFFDE8F1)) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            if (selectedMethod == "BKASH") 2.dp else 1.dp,
                            if (selectedMethod == "BKASH") Color(0xFFE2136E) else VeloraCardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = mfsConfig.bKashEnabled) {
                                selectedMethod = "BKASH"
                            }
                            .testTag("select_method_bkash")
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "bKash",
                                fontWeight = FontWeight.Black,
                                color = if (mfsConfig.bKashEnabled) Color(0xFFE2136E) else Color.Gray,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (mfsConfig.bKashEnabled) "বিকাশ" else "Disabled",
                                fontSize = 10.sp,
                                color = if (mfsConfig.bKashEnabled) Color(0xFFE2136E) else Color.Gray
                            )
                        }
                    }

                    // Nagad Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedMethod == "NAGAD") (if (isSystemInDarkTheme()) Color(0xFF3B200B) else Color(0xFFFEF4E8)) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            if (selectedMethod == "NAGAD") 2.dp else 1.dp,
                            if (selectedMethod == "NAGAD") Color(0xFFF7931E) else VeloraCardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = mfsConfig.nagadEnabled) {
                                selectedMethod = "NAGAD"
                            }
                            .testTag("select_method_nagad")
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Nagad",
                                fontWeight = FontWeight.Black,
                                color = if (mfsConfig.nagadEnabled) Color(0xFFF7931E) else Color.Gray,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (mfsConfig.nagadEnabled) "নগদ" else "Disabled",
                                fontSize = 10.sp,
                                color = if (mfsConfig.nagadEnabled) Color(0xFFF7931E) else Color.Gray
                            )
                        }
                    }

                    // Card Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedMethod == "CARD") (if (isSystemInDarkTheme()) Color(0xFF261947) else VeloraLavenderLight) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            if (selectedMethod == "CARD") 2.dp else 1.dp,
                            if (selectedMethod == "CARD") VeloraViolet else VeloraCardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMethod = "CARD" }
                            .testTag("select_method_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Card",
                                fontWeight = FontWeight.Bold,
                                color = VeloraViolet,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Debit/Credit",
                                fontSize = 10.sp,
                                color = VeloraTextMuted
                            )
                        }
                    }
                }

                // Instructions Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (selectedMethod) {
                        "BKASH" -> if (isSystemInDarkTheme()) Color(0xFF2C1020) else Color(0xFFFDF0F5)
                        "NAGAD" -> if (isSystemInDarkTheme()) Color(0xFF2E1908) else Color(0xFFFFF7ED)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val merchantNum = if (selectedMethod == "BKASH") mfsConfig.bKashMerchantNumber else mfsConfig.nagadMerchantNumber
                        val merchantType = if (selectedMethod == "BKASH") mfsConfig.bKashMerchantType else mfsConfig.nagadMerchantType
                        val methodTitle = if (selectedMethod == "BKASH") "bKash Merchant Payment" else if (selectedMethod == "NAGAD") "Nagad Payment" else "Card Payment"

                        Text(
                            text = methodTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedMethod == "BKASH") Color(0xFFE2136E) else if (selectedMethod == "NAGAD") Color(0xFFF7931E) else VeloraViolet
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (selectedMethod != "CARD") {
                            Text("1. Send ৳${plan.priceBdt} BDT via $selectedMethod ($merchantType)", fontSize = 12.sp)
                            Text("2. Merchant Number: $merchantNum (Counter: ${mfsConfig.merchantCounter})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("3. Enter your account number and TrxID below to automatically activate.", fontSize = 12.sp)
                        } else {
                            Text("Instant card processing in BDT (৳${plan.priceBdt}). Enter reference / TrxID below.", fontSize = 12.sp)
                        }
                    }
                }

                if (!inputError.isNullOrBlank()) {
                    Text(inputError!!, color = Color.Red, fontSize = 12.sp)
                }

                // Phone Input
                OutlinedTextField(
                    value = senderPhone,
                    onValueChange = { senderPhone = it },
                    label = { Text("Sender Phone Number") },
                    placeholder = { Text("01XXXXXXXXX") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mfs_phone_input")
                )

                // TrxID Input
                OutlinedTextField(
                    value = trxIdInput,
                    onValueChange = {
                        trxIdInput = it.uppercase()
                        inputError = null
                    },
                    label = { Text("Transaction ID (TrxID)") },
                    placeholder = { Text("e.g. BKH89X201A") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mfs_trxid_input")
                )

                // Fast Simulation Button for Instant Testing
                OutlinedButton(
                    onClick = {
                        val prefix = if (selectedMethod == "NAGAD") "NGD" else if (selectedMethod == "CARD") "CRD" else "BKH"
                        val randomSuffix = (100000..999999).random().toString()
                        trxIdInput = "$prefix$randomSuffix"
                        inputError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_trxid_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Simulate",
                        tint = VeloraPink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Generate Test TrxID", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (trxIdInput.trim().length < 6) {
                        inputError = "Please enter a valid TrxID with at least 6 characters."
                        return@Button
                    }
                    onConfirmPayment(selectedMethod, senderPhone, trxIdInput)
                },
                enabled = !isProcessing && trxIdInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedMethod == "BKASH") Color(0xFFE2136E) else if (selectedMethod == "NAGAD") Color(0xFFF7931E) else VeloraPink
                ),
                modifier = Modifier.testTag("confirm_mfs_payment_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Confirm & Activate", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_checkout_button")
            ) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun BoostStoreScreen(
    boostMinutes: Int,
    superLikes: Int,
    onActivateBoost: () -> Unit,
    onBuySuperLikes: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Boost & Super Like", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Boost Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(VeloraGoldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = "Boost", tint = VeloraGoldDark)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Profile Boost", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Be seen 10x more for 30 minutes", fontSize = 13.sp, color = VeloraTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (boostMinutes > 0) {
                        Text(
                            text = "⚡ Boost Active: $boostMinutes min remaining",
                            color = VeloraGoldDark,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Button(
                            onClick = onActivateBoost,
                            colors = ButtonDefaults.buttonColors(containerColor = VeloraGold),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("buy_boost_button")
                        ) {
                            Text("Activate Boost • ৳99", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Super Like Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(VeloraLavenderLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Super Like", tint = VeloraViolet)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Super Likes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Remaining: $superLikes Super Likes", fontSize = 13.sp, color = VeloraTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onBuySuperLikes,
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraViolet),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("buy_super_like_button")
                    ) {
                        Text("Get 5 Super Likes • ৳49", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notifications) { notif ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, VeloraCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(VeloraPinkSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (notif.type) {
                                    "MATCH" -> Icons.Default.Favorite
                                    "MESSAGE" -> Icons.Default.ChatBubble
                                    "LIKE" -> Icons.Default.ThumbUp
                                    else -> Icons.Default.Visibility
                                },
                                contentDescription = null,
                                tint = VeloraPink,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = notif.message, fontSize = 13.sp, color = VeloraTextSecondary)
                        }

                        Text(text = notif.timeAgo, fontSize = 11.sp, color = VeloraTextMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun BlockReportScreen(
    user: UserProfile,
    onBlockUser: (UserProfile) -> Unit,
    onReportUser: (UserProfile, String) -> Unit,
    onBack: () -> Unit
) {
    var reportReason by remember { mutableStateOf("Fake profile") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety & Actions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Manage interactions with ${user.name}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VeloraTextPrimary
            )

            // Block Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Block User", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "They won't be able to contact you or see your profile anymore.",
                        fontSize = 13.sp,
                        color = VeloraTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onBlockUser(user) },
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraPassRed),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("confirm_block_button")
                    ) {
                        Text("Block ${user.name}", color = Color.White)
                    }
                }
            }

            // Report Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Report User", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Report inappropriate behavior or policy violations.", fontSize = 13.sp, color = VeloraTextSecondary)

                    Spacer(modifier = Modifier.height(10.dp))

                    listOf("Fake profile", "Harassment", "Inappropriate photo", "Scam or commercial", "Underage").forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = r }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = reportReason == r,
                                onClick = { reportReason = r },
                                colors = RadioButtonDefaults.colors(selectedColor = VeloraPink)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = r, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onReportUser(user, reportReason) },
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraViolet),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("confirm_report_button")
                    ) {
                        Text("Submit Report", color = Color.White)
                    }
                }
            }
        }
    }
}

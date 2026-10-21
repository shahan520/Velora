package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.components.VeloraBrandLogo
import com.example.ui.theme.*

@Composable
fun AdminPanelScreen(
    currentSection: AdminSection,
    onSectionChange: (AdminSection) -> Unit,
    profiles: List<UserProfile>,
    reports: List<UserReport>,
    onToggleSuspendUser: (String) -> Unit,
    onResolveReport: (String, String) -> Unit,
    onExitAdmin: () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                color = VeloraPlumDark,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VeloraBrandLogo(size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Velora Admin",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = VeloraPink
                                ) {
                                    Text(
                                        text = "SUPER ADMIN",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = VeloraPlumSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, VeloraViolet)
                                ) {
                                    Text(
                                        text = "/admin",
                                        color = VeloraPinkLight,
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Manage • Monitor • Grow",
                                fontSize = 11.sp,
                                color = VeloraLavender
                            )
                        }
                    }

                    // Exit to App Button
                    Button(
                        onClick = onExitAdmin,
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraPlumMedium),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("exit_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Log Out Admin",
                            tint = VeloraPinkLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exit /admin",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Horizontal Admin Navigation Tabs
            Surface(
                color = VeloraPlumDark,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AdminNavTab(
                        icon = Icons.Default.Dashboard,
                        label = "Dashboard",
                        isSelected = currentSection == AdminSection.DASHBOARD,
                        onClick = { onSectionChange(AdminSection.DASHBOARD) }
                    )
                    AdminNavTab(
                        icon = Icons.Default.People,
                        label = "Users",
                        isSelected = currentSection == AdminSection.USERS,
                        onClick = { onSectionChange(AdminSection.USERS) }
                    )
                    AdminNavTab(
                        icon = Icons.Default.Report,
                        label = "Reports",
                        isSelected = currentSection == AdminSection.REPORTS,
                        badgeCount = reports.count { it.status == "Pending" },
                        onClick = { onSectionChange(AdminSection.REPORTS) }
                    )
                    AdminNavTab(
                        icon = Icons.Default.Analytics,
                        label = "Analytics",
                        isSelected = currentSection == AdminSection.ANALYTICS,
                        onClick = { onSectionChange(AdminSection.ANALYTICS) }
                    )
                    AdminNavTab(
                        icon = Icons.Default.Paid,
                        label = "Revenue",
                        isSelected = currentSection == AdminSection.SUBSCRIPTIONS,
                        onClick = { onSectionChange(AdminSection.SUBSCRIPTIONS) }
                    )
                    AdminNavTab(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        isSelected = currentSection == AdminSection.SETTINGS,
                        onClick = { onSectionChange(AdminSection.SETTINGS) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(VeloraIvory)
        ) {
            when (currentSection) {
                AdminSection.DASHBOARD -> AdminDashboardSection(profiles, reports)
                AdminSection.USERS -> AdminUsersSection(profiles, onToggleSuspendUser)
                AdminSection.REPORTS -> AdminReportsSection(reports, onResolveReport)
                AdminSection.ANALYTICS -> AdminAnalyticsSection()
                AdminSection.SUBSCRIPTIONS -> AdminSubscriptionsSection()
                AdminSection.SETTINGS -> AdminSettingsSection()
                else -> AdminDashboardSection(profiles, reports)
            }
        }
    }
}

@Composable
private fun AdminNavTab(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) VeloraPink else VeloraLavender,
                modifier = Modifier.size(22.dp)
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(VeloraErrorRed)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) VeloraPink else VeloraLavender
        )
    }
}

@Composable
fun AdminDashboardSection(profiles: List<UserProfile>, reports: List<UserReport>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Dashboard",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VeloraTextPrimary
                )
                Text(
                    text = "Welcome back, Admin! 👋 Here's what's happening with Velora today.",
                    fontSize = 13.sp,
                    color = VeloraTextSecondary
                )
            }
        }

        // 4 KPI Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiMetricCard(
                title = "Total Users",
                value = "48,532",
                trend = "↑ 12%",
                trendPositive = true,
                icon = Icons.Default.People,
                iconTint = VeloraViolet,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Active Users",
                value = "32,487",
                trend = "↑ 18%",
                trendPositive = true,
                icon = Icons.Default.Bolt,
                iconTint = VeloraGold,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiMetricCard(
                title = "Total Matches",
                value = "12,846",
                trend = "↑ 24%",
                trendPositive = true,
                icon = Icons.Default.Favorite,
                iconTint = VeloraPink,
                modifier = Modifier.weight(1f)
            )
            KpiMetricCard(
                title = "Reports",
                value = "${reports.size}",
                trend = "↓ 15%",
                trendPositive = true,
                icon = Icons.Default.ReportProblem,
                iconTint = VeloraErrorRed,
                modifier = Modifier.weight(1f)
            )
        }

        // User Growth Chart
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("User Growth", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Last 7 Days", color = VeloraTextMuted, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Growth Curve Drawing
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    val points = listOf(
                        Offset(0f, h * 0.75f),
                        Offset(w * 0.16f, h * 0.70f),
                        Offset(w * 0.33f, h * 0.60f),
                        Offset(w * 0.50f, h * 0.35f),
                        Offset(w * 0.66f, h * 0.45f),
                        Offset(w * 0.83f, h * 0.20f),
                        Offset(w, h * 0.10f)
                    )

                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val cx = (prev.x + curr.x) / 2
                            cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = VeloraPink,
                        style = Stroke(width = 6f)
                    )

                    points.forEach { pt ->
                        drawCircle(color = VeloraPink, radius = 7f, center = pt)
                        drawCircle(color = Color.White, radius = 3.5f, center = pt)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Oct 5", "Oct 6", "Oct 7", "Oct 8", "Oct 9", "Oct 10", "Oct 12").forEach { day ->
                        Text(day, fontSize = 10.sp, color = VeloraTextMuted)
                    }
                }
            }
        }

        // Location Distribution
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("User Location Distribution", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                LocationProgressBar("Bangladesh 🇧🇩", 42, VeloraPink)
                LocationProgressBar("India 🇮🇳", 18, VeloraViolet)
                LocationProgressBar("United States 🇺🇸", 10, VeloraGold)
                LocationProgressBar("United Kingdom 🇬🇧", 8, VeloraVerifiedBlue)
                LocationProgressBar("Others 🌐", 22, VeloraLavender)
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    trend: String,
    trendPositive: Boolean,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, color = VeloraTextSecondary, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = VeloraTextPrimary)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = trend,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (trendPositive) VeloraOnlineGreen else VeloraErrorRed
            )
        }
    }
}

@Composable
private fun LocationProgressBar(label: String, percentage: Int, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, color = VeloraTextPrimary)
            Text("$percentage%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VeloraTextPrimary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = VeloraPinkSoft
        )
    }
}

@Composable
fun AdminUsersSection(
    profiles: List<UserProfile>,
    onToggleSuspend: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = profiles.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.city.contains(searchQuery, ignoreCase = true) ||
        it.email.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("User Management", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Manage all accounts, verify IDs, or enforce safety suspensions.", fontSize = 12.sp, color = VeloraTextSecondary)

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, email, or city...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_user_search"),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered) { user ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = user.photos.firstOrNull(),
                            contentDescription = user.name,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("(${user.age})", fontSize = 13.sp, color = VeloraTextMuted)
                            }
                            Text("${user.city}, ${user.country}", fontSize = 12.sp, color = VeloraTextSecondary)
                            Text(user.email, fontSize = 11.sp, color = VeloraTextMuted)
                        }

                        // Status Badge & Action
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (user.isSuspended) VeloraErrorRed.copy(alpha = 0.15f) else VeloraOnlineGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (user.isSuspended) "Suspended" else "Active",
                                    color = if (user.isSuspended) VeloraErrorRed else VeloraOnlineGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            TextButton(
                                onClick = { onToggleSuspend(user.id) },
                                modifier = Modifier.testTag("suspend_user_${user.id}")
                            ) {
                                Text(
                                    text = if (user.isSuspended) "Reactivate" else "Suspend",
                                    color = if (user.isSuspended) VeloraOnlineGreen else VeloraErrorRed,
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

@Composable
fun AdminReportsSection(
    reports: List<UserReport>,
    onResolveReport: (String, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf("All") }

    val filtered = when (selectedTab) {
        "Profile" -> reports.filter { it.type == "Profile" }
        "Message" -> reports.filter { it.type == "Message" }
        "Content" -> reports.filter { it.type == "Content" }
        else -> reports
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Reports & Moderation", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Review community violations and apply prompt action.", fontSize = 12.sp, color = VeloraTextSecondary)

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Profile", "Message", "Content").forEach { tab ->
                val isSel = selectedTab == tab
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) VeloraPink else Color.White,
                    modifier = Modifier.clickable { selectedTab = tab }
                ) {
                    Text(
                        text = "$tab Reports",
                        color = if (isSel) Color.White else VeloraTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered) { rep ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = VeloraPinkSoft
                                ) {
                                    Text(
                                        text = rep.type,
                                        color = VeloraPink,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reported: ${rep.reportedUserName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Text(rep.date, fontSize = 11.sp, color = VeloraTextMuted)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Reason: ${rep.reason} (by ${rep.reporterName})",
                            fontSize = 13.sp,
                            color = VeloraTextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status: ${rep.status}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (rep.status) {
                                    "Resolved" -> VeloraOnlineGreen
                                    "Under Review" -> VeloraGoldDark
                                    "Dismissed" -> VeloraTextMuted
                                    else -> VeloraErrorRed
                                }
                            )

                            Row {
                                OutlinedButton(
                                    onClick = { onResolveReport(rep.id, "Dismissed") },
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Dismiss", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { onResolveReport(rep.id, "Resolved") },
                                    colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Resolve", fontSize = 11.sp, color = Color.White)
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
fun AdminAnalyticsSection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Matches & Analytics", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiMetricCard("Match Rate", "8.4%", "↑ 2.1%", true, Icons.Default.TrendingUp, VeloraPink, Modifier.weight(1f))
            KpiMetricCard("Avg Chat Time", "2h 36m", "↑ 10%", true, Icons.Default.Schedule, VeloraViolet, Modifier.weight(1f))
        }

        // Gender breakdown
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Matches by Gender", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("48%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = VeloraViolet)
                        Text("Male", fontSize = 12.sp, color = VeloraTextMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("52%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = VeloraPink)
                        Text("Female", fontSize = 12.sp, color = VeloraTextMuted)
                    }
                }
            }
        }

        // Top Interests
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Popular Interests", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))
                LocationProgressBar("Travel ✈️", 24, VeloraPink)
                LocationProgressBar("Music 🎵", 18, VeloraViolet)
                LocationProgressBar("Sports ⚽", 16, VeloraGold)
                LocationProgressBar("Food 🍜", 14, VeloraVerifiedBlue)
                LocationProgressBar("Fashion 👗", 12, VeloraRose)
            }
        }
    }
}

@Composable
fun AdminSubscriptionsSection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Premium & Subscriptions", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // Total Revenue Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VeloraPlumDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Total Verified Revenue", color = VeloraLavender, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("৳ 5,89,420", color = VeloraGold, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text("↑ 32% this month • Ad-free business model", color = VeloraOnlineGreen, fontSize = 12.sp)
            }
        }

        // Configurable Plans
        Text("Active Plans", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        listOf(
            Triple("Basic Free", "৳0 / forever", "Standard likes, basic filters, ad-free"),
            Triple("Premium", "৳299 / month", "Unlimited likes, see who liked you, 1 boost"),
            Triple("Gold VIP", "৳499 / month", "All premium features + 5 Super likes/week + VIP badge")
        ).forEach { (name, price, desc) ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(desc, fontSize = 12.sp, color = VeloraTextSecondary)
                    }
                    Text(price, fontWeight = FontWeight.Black, color = VeloraPink, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun AdminSettingsSection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("System Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("General App Config", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))

                SettingRow(title = "App Name", value = "Velora")
                SettingRow(title = "Default Country", value = "Bangladesh 🇧🇩")
                SettingRow(title = "Currency", value = "BDT (৳)")
                SettingRow(title = "Timezone", value = "Asia/Dhaka")
                SettingRow(title = "Age Verification", value = "Strict (18+ confirmation)")
                SettingRow(title = "Ad Network", value = "Disabled (100% Ad-Free)")
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick Maintenance Actions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = { /* clear cache */ }, modifier = Modifier.weight(1f)) {
                        Text("Clear Cache", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { /* backup */ },
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraViolet),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Backup DB", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 13.sp, color = VeloraTextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VeloraTextPrimary)
    }
}

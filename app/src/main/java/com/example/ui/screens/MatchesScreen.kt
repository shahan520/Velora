package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.MatchItem
import com.example.ui.components.VeloraTopBar
import com.example.ui.theme.*

@Composable
fun MatchesScreen(
    matches: List<MatchItem>,
    onMatchClick: (MatchItem) -> Unit,
    onFilterClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("All") }

    val filteredMatches = when (selectedTab) {
        "Online" -> matches.filter { it.isOnline }
        "New" -> matches.filter { it.isNew }
        else -> matches
    }

    Scaffold(
        topBar = {
            VeloraTopBar(
                title = "Matches",
                onFilterClick = onFilterClick,
                onNotificationsClick = onNotificationsClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tabs: All, Online, New
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf("All", "Online", "New").forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("match_tab_$tab")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tab,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) VeloraPink else VeloraTextMuted
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(width = 16.dp, height = 3.dp)
                                        .clip(CircleShape)
                                        .background(VeloraPink)
                                )
                            }
                        }
                    }
                }
            }

            // New Matches Horizontal Avatar Row
            val newMatches = matches.filter { it.isNew }
            if (newMatches.isNotEmpty()) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "New Matches (${newMatches.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VeloraTextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(newMatches) { match ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onMatchClick(match) }
                                    .padding(4.dp)
                            ) {
                                Box {
                                    AsyncImage(
                                        model = match.user.photos.firstOrNull(),
                                        contentDescription = match.user.name,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .border(2.5.dp, VeloraPink, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (match.isOnline) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(VeloraOnlineGreen)
                                                .border(2.dp, Color.White, CircleShape)
                                                .align(Alignment.BottomEnd)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = match.user.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VeloraTextPrimary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = VeloraCardBorder
                )
            }

            // Match Conversations List
            if (filteredMatches.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMatches) { match ->
                        MatchCardItem(
                            match = match,
                            onClick = { onMatchClick(match) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matches found in this category",
                        color = VeloraTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchCardItem(
    match: MatchItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("match_item_${match.id}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box {
                AsyncImage(
                    model = match.user.photos.firstOrNull(),
                    contentDescription = match.user.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                if (match.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(VeloraOnlineGreen)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = match.user.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VeloraTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = match.lastMessage,
                    fontSize = 13.sp,
                    color = VeloraTextSecondary,
                    maxLines = 1
                )
            }

            // Time & Status
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (match.isOnline) "Online" else match.lastMessageTime,
                    fontSize = 11.sp,
                    color = if (match.isOnline) VeloraOnlineGreen else VeloraTextMuted,
                    fontWeight = if (match.isOnline) FontWeight.Bold else FontWeight.Normal
                )

                if (match.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(VeloraPink),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = match.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

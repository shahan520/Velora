@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DiscoverScreen(
    profiles: List<UserProfile>,
    currentIndex: Int,
    activeSubTab: String,
    onSubTabSelected: (String) -> Unit,
    onPass: (UserProfile) -> Unit,
    onLike: (UserProfile) -> Unit,
    onSuperLike: (UserProfile) -> Unit,
    onUndoPass: () -> Unit,
    onCardClick: (UserProfile) -> Unit,
    onFilterClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    showMatchDialog: Boolean,
    matchedUser: UserProfile?,
    onDismissMatch: () -> Unit,
    onStartChatWithMatch: (UserProfile) -> Unit
) {
    val currentProfile = if (profiles.isNotEmpty()) profiles[currentIndex % profiles.size] else null

    Scaffold(
        topBar = {
            VeloraTopBar(
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
            // Sub Tabs: Discover, Nearby, Online
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf("Discover", "Nearby", "Online").forEach { tab ->
                    val isSelected = activeSubTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSubTabSelected(tab) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("subtab_$tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tab,
                                fontSize = 14.sp,
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

            if (currentProfile != null) {
                // Dating Profile Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(8.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onCardClick(currentProfile) }
                            .testTag("profile_card_${currentProfile.id}"),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Primary Photo
                            AsyncImage(
                                model = currentProfile.photos.firstOrNull(),
                                contentDescription = "${currentProfile.name}'s photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Gradient Scrim for readable text
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.4f),
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )

                            // Top Info Tag (Online dot)
                            if (currentProfile.isOnline) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(VeloraOnlineGreen)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Online",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Bottom Profile Details Overlay
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomStart)
                                    .padding(20.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${currentProfile.name}, ${currentProfile.age}",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (currentProfile.isVerified) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        VerifiedBadge(size = 20.dp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        tint = VeloraPinkLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${currentProfile.city}, ${currentProfile.country} • ${currentProfile.distanceKm} km away",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Interests Pills
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    currentProfile.interests.take(3).forEach { interest ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = interest,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Bottom Action Buttons (Pass, Super Like, Like, Undo)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo Pass
                    CircularActionButton(
                        icon = Icons.Default.Replay,
                        contentDescription = "Undo Pass",
                        onClick = onUndoPass,
                        size = 46.dp,
                        iconSize = 22.dp,
                        iconTint = VeloraGold,
                        testTag = "action_undo"
                    )

                    // Pass (X)
                    CircularActionButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Pass",
                        onClick = { onPass(currentProfile) },
                        size = 62.dp,
                        iconSize = 30.dp,
                        iconTint = VeloraPassRed,
                        elevation = 8.dp,
                        testTag = "action_pass"
                    )

                    // Super Like (Star)
                    CircularActionButton(
                        icon = Icons.Default.Star,
                        contentDescription = "Super Like",
                        onClick = { onSuperLike(currentProfile) },
                        size = 50.dp,
                        iconSize = 24.dp,
                        iconTint = VeloraSuperLikeStar,
                        testTag = "action_super_like"
                    )

                    // Like (Heart)
                    CircularActionButton(
                        icon = Icons.Default.Favorite,
                        contentDescription = "Like",
                        onClick = { onLike(currentProfile) },
                        size = 62.dp,
                        iconSize = 30.dp,
                        backgroundColor = VeloraPink,
                        iconTint = Color.White,
                        elevation = 8.dp,
                        testTag = "action_like"
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "No more profiles",
                            tint = VeloraPink,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No more profiles nearby!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = VeloraTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try adjusting your distance or age filters.",
                            color = VeloraTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Mutual Match Celebration Dialog
    if (showMatchDialog && matchedUser != null) {
        Dialog(onDismissRequest = onDismissMatch) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = VeloraPlumDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("match_celebration_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "It's a Match! \uD83C\uDF89",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = VeloraPink
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "You and ${matchedUser.name} liked each other!",
                        fontSize = 14.sp,
                        color = VeloraLavender,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Intertwined Profile Pictures with Glowing Heart
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=80",
                            contentDescription = "My photo",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(3.dp, VeloraPink, CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VeloraPink),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Match Heart",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AsyncImage(
                            model = matchedUser.photos.firstOrNull(),
                            contentDescription = "${matchedUser.name}'s photo",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(3.dp, VeloraPink, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { onStartChatWithMatch(matchedUser) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("send_message_match_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send Message", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send a Message",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = onDismissMatch,
                        modifier = Modifier.testTag("keep_swiping_button")
                    ) {
                        Text(
                            text = "Keep Swiping",
                            color = VeloraLavender,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDetailsScreen(
    profile: UserProfile,
    onBack: () -> Unit,
    onLike: () -> Unit,
    onPass: () -> Unit,
    onSuperLike: () -> Unit,
    onReportBlock: () -> Unit
) {
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularActionButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Pass",
                        onClick = onPass,
                        size = 56.dp,
                        iconTint = VeloraPassRed,
                        testTag = "detail_pass"
                    )

                    CircularActionButton(
                        icon = Icons.Default.Star,
                        contentDescription = "Super Like",
                        onClick = onSuperLike,
                        size = 48.dp,
                        iconTint = VeloraSuperLikeStar,
                        testTag = "detail_super_like"
                    )

                    CircularActionButton(
                        icon = Icons.Default.Favorite,
                        contentDescription = "Like",
                        onClick = onLike,
                        size = 56.dp,
                        backgroundColor = VeloraPink,
                        iconTint = Color.White,
                        testTag = "detail_like"
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Photo Gallery Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                AsyncImage(
                    model = profile.photos.getOrNull(selectedPhotoIndex) ?: profile.photos.firstOrNull(),
                    contentDescription = profile.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top navigation bar overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onReportBlock,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("detail_more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color.White
                        )
                    }
                }

                // Thumbnail Strip
                if (profile.photos.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(profile.photos.indices.toList()) { idx ->
                            val isSelected = selectedPhotoIndex == idx
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        2.dp,
                                        if (isSelected) VeloraPink else Color.White,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedPhotoIndex = idx }
                            ) {
                                AsyncImage(
                                    model = profile.photos[idx],
                                    contentDescription = "Thumbnail $idx",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            // Info Body
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${profile.name}, ${profile.age}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = VeloraTextPrimary
                    )
                    if (profile.isVerified) {
                        Spacer(modifier = Modifier.width(8.dp))
                        VerifiedBadge(size = 22.dp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = VeloraPink,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${profile.city}, ${profile.country} • ${profile.distanceKm} km away",
                        color = VeloraTextSecondary,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats row (Profession, Height, Status)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailChip(icon = Icons.Default.School, text = profile.profession)
                    DetailChip(icon = Icons.Default.Straighten, text = profile.height)
                    DetailChip(icon = Icons.Default.FavoriteBorder, text = profile.relationshipStatus)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // About Me
                Text(
                    text = "About me",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VeloraTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = profile.aboutMe,
                    fontSize = 15.sp,
                    color = VeloraTextSecondary,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Interests
                Text(
                    text = "Interests",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VeloraTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    profile.interests.forEach { interest ->
                        InterestChip(text = interest)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = VeloraPinkSoft,
        border = androidx.compose.foundation.BorderStroke(1.dp, VeloraCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = VeloraPink, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, fontSize = 12.sp, color = VeloraTextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SearchFiltersScreen(
    currentFilters: DiscoveryFilters,
    onApply: (DiscoveryFilters) -> Unit,
    onBack: () -> Unit
) {
    var minAge by remember { mutableFloatStateOf(currentFilters.minAge.toFloat()) }
    var maxAge by remember { mutableFloatStateOf(currentFilters.maxAge.toFloat()) }
    var maxDistance by remember { mutableFloatStateOf(currentFilters.maxDistanceKm.toFloat()) }
    var selectedGender by remember { mutableStateOf(currentFilters.gender) }
    var selectedCountry by remember { mutableStateOf(currentFilters.country) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filters", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            minAge = 18f
                            maxAge = 35f
                            maxDistance = 50f
                            selectedGender = "All"
                            selectedCountry = "Any"
                        }
                    ) {
                        Text("Reset", color = VeloraPink, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
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
                            onApply(
                                DiscoveryFilters(
                                    minAge = minAge.toInt(),
                                    maxAge = maxAge.toInt(),
                                    maxDistanceKm = maxDistance.toInt(),
                                    gender = selectedGender,
                                    country = selectedCountry
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("apply_filters_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Text("Apply Filters", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Age Range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Age Range", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${minAge.toInt()} - ${maxAge.toInt()}", color = VeloraPink, fontWeight = FontWeight.Bold)
            }
            RangeSlider(
                value = minAge..maxAge,
                onValueChange = { range ->
                    minAge = range.start
                    maxAge = range.endInclusive
                },
                valueRange = 18f..60f,
                colors = SliderDefaults.colors(thumbColor = VeloraPink, activeTrackColor = VeloraPink)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Distance", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Within ${maxDistance.toInt()} km", color = VeloraPink, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = maxDistance,
                onValueChange = { maxDistance = it },
                valueRange = 5f..150f,
                colors = SliderDefaults.colors(thumbColor = VeloraPink, activeTrackColor = VeloraPink)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Gender
            Text("Gender", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("All", "Women", "Men", "Other").forEach { g ->
                    InterestChip(
                        text = g,
                        isSelected = selectedGender.equals(g, ignoreCase = true),
                        onClick = { selectedGender = g }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Country
            Text("Country", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Any", "Bangladesh 🇧🇩", "India 🇮🇳", "Global 🌐").forEach { c ->
                    InterestChip(
                        text = c,
                        isSelected = selectedCountry == c,
                        onClick = { selectedCountry = c }
                    )
                }
            }
        }
    }
}

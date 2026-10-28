@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.UserProfile
import com.example.service.CloudinaryUploadService
import com.example.ui.theme.*

@Composable
fun EditProfileScreen(
    user: UserProfile,
    isUploading: Boolean,
    uploadProgress: Float,
    uploadStatus: String?,
    lastUploadedUrl: String?,
    onUploadPhotoUri: (Uri) -> Unit,
    onUploadPrimaryPhotoUri: (Uri) -> Unit = {},
    onUploadMultiplePhotos: (List<Uri>) -> Unit = {},
    onUploadSamplePhoto: (String) -> Unit,
    onSetPrimaryPhoto: (String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onSaveProfile: (aboutMe: String, profession: String, city: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var aboutMeText by remember(user.aboutMe) { mutableStateOf(user.aboutMe) }
    var professionText by remember(user.profession) { mutableStateOf(user.profession) }
    var cityText by remember(user.city) { mutableStateOf(user.city) }
    var showSamplePicker by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }

    // Dedicated Primary Photo Picker
    val primaryPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadPrimaryPhotoUri(uri)
        }
    }

    // Multi-photo picker for 4 to 5 Clear Face Photos
    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onUploadMultiplePhotos(uris)
        }
    }

    // Single photo picker fallback
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadPhotoUri(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Profile & Photos",
                        fontWeight = FontWeight.Bold,
                        color = VeloraTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("edit_profile_back_button")
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = VeloraTextPrimary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSaveProfile(aboutMeText, professionText, cityText)
                            onBack()
                        },
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Text(
                            "Done",
                            fontWeight = FontWeight.Bold,
                            color = VeloraPink,
                            fontSize = 16.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Cloudinary CDN & Cloud Firestore Status Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VeloraPlumDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(VeloraPink.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = "Cloud Upload",
                                    tint = VeloraPink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cloudinary CDN & Cloud Firestore",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Cloud: ${CloudinaryUploadService.getEffectiveCloudName()} • Firestore synced",
                                    fontSize = 11.sp,
                                    color = VeloraLavender
                                )
                            }
                        }

                        IconButton(
                            onClick = { showConfigDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Configure Cloudinary",
                                tint = VeloraGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isUploading) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = VeloraPink,
                            trackColor = VeloraPlumMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uploadStatus ?: "Uploading to Cloudinary CDN...",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 1. DEDICATED PRIMARY PROFILE PHOTO SECTION
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(2.dp, VeloraPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("primary_photo_section")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = VeloraPink,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PRIMARY PHOTO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                            Text("Main Profile Picture", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = VeloraTextPrimary)
                        }

                        Button(
                            onClick = {
                                primaryPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            enabled = !isUploading,
                            modifier = Modifier.testTag("change_primary_photo_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change Primary", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, VeloraPink, RoundedCornerShape(16.dp))
                                .clickable {
                                    primaryPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = user.effectivePrimaryPhoto,
                                contentDescription = "Primary Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .background(VeloraPink, RoundedCornerShape(topStart = 8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Your Primary Dating Photo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = VeloraTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "This is the first picture everyone sees on discovery cards, match lists, and video calls.",
                                fontSize = 12.sp,
                                color = VeloraTextSecondary,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "✓ High resolution & clear face required",
                                fontSize = 11.sp,
                                color = VeloraPink,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. CLEAR FACE PHOTOS (4 TO 5 PHOTOS) SECTION
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, VeloraCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("face_photos_section")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Clear Face Photos",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = VeloraTextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (user.photos.size >= 4) VeloraPink.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${user.photos.size} / 5 Photos",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (user.photos.size >= 4) VeloraPink else VeloraTextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (user.photos.size >= 4) "✓ 4-5 clear face photos requirement met" else "Upload 4 to 5 clear face photos for profile verification",
                                fontSize = 12.sp,
                                color = if (user.photos.size >= 4) VeloraPink else VeloraTextSecondary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Quick Samples button
                            OutlinedButton(
                                onClick = { showSamplePicker = !showSamplePicker },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("sample_photos_toggle_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VeloraViolet, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Samples", fontSize = 11.sp, color = VeloraViolet)
                            }

                            // Upload multiple face photos button
                            Button(
                                onClick = {
                                    multiPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                enabled = !isUploading && user.photos.size < 5,
                                modifier = Modifier
                                    .testTag("upload_gallery_photo_button")
                                    .testTag("upload_face_photos_button")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Photos", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Samples Dropdown/Carousel
                    AnimatedVisibility(visible = showSamplePicker) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Tap sample to add to your clear face photos:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VeloraTextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(CloudinaryUploadService.sampleProfilePhotos) { sampleUrl ->
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    onUploadSamplePhoto(sampleUrl)
                                                    showSamplePicker = false
                                                }
                                                .border(1.5.dp, VeloraPinkSoft, RoundedCornerShape(12.dp))
                                        ) {
                                            AsyncImage(
                                                model = sampleUrl,
                                                contentDescription = "Sample avatar",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .size(20.dp)
                                                    .background(VeloraPink, RoundedCornerShape(topStart = 8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "Add",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5 Clear Face Photo Slots
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (slotIndex in 0 until 5) {
                            val photoUrl = user.photos.getOrNull(slotIndex)
                            val isSlotFilled = photoUrl != null
                            val isPrimary = photoUrl != null && photoUrl == user.effectivePrimaryPhoto

                            if (isSlotFilled) {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isPrimary) 1.5.dp else 1.dp,
                                        if (isPrimary) VeloraPink else VeloraCardBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_photo_card_$slotIndex")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        ) {
                                            AsyncImage(
                                                model = photoUrl,
                                                contentDescription = "Face Photo ${slotIndex + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            if (isPrimary) {
                                                Surface(
                                                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                                                    color = VeloraPink,
                                                    modifier = Modifier.align(Alignment.TopStart)
                                                ) {
                                                    Text(
                                                        text = "PRIMARY",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Photo ${slotIndex + 1}: Clear Face",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = VeloraTextPrimary
                                                )
                                                if (isPrimary) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("(Primary)", fontSize = 11.sp, color = VeloraPink, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text(
                                                text = if (photoUrl!!.contains("cloudinary.com")) "Cloudinary CDN Verified" else "Clear Face Photo",
                                                fontSize = 11.sp,
                                                color = VeloraTextSecondary
                                            )
                                            Text(
                                                text = photoUrl,
                                                fontSize = 9.sp,
                                                color = VeloraTextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row {
                                            if (!isPrimary) {
                                                IconButton(
                                                    onClick = { onSetPrimaryPhoto(photoUrl!!) },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .testTag("set_primary_photo_$slotIndex")
                                                ) {
                                                    Icon(
                                                        Icons.Default.StarBorder,
                                                        contentDescription = "Make Primary",
                                                        tint = VeloraGold,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            if (user.photos.size > 1) {
                                                IconButton(
                                                    onClick = { onRemovePhoto(photoUrl!!) },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .testTag("delete_photo_$slotIndex")
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete Photo",
                                                        tint = VeloraErrorRed,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Empty Slot
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, VeloraCardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isUploading) {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                        .testTag("empty_photo_slot_$slotIndex")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add face photo",
                                                tint = VeloraPink,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "Slot ${slotIndex + 1}: Empty Face Photo",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = VeloraTextPrimary
                                            )
                                            Text(
                                                text = "Tap to upload clear face photo (${slotIndex + 1} of 5)",
                                                fontSize = 11.sp,
                                                color = VeloraTextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Profile Information Form
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "About You",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = VeloraTextPrimary
                    )

                    OutlinedTextField(
                        value = aboutMeText,
                        onValueChange = { aboutMeText = it },
                        label = { Text("About Me (Bio)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_aboutme_input"),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = professionText,
                        onValueChange = { professionText = it },
                        label = { Text("Profession / Occupation") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profession_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = cityText,
                        onValueChange = { cityText = it },
                        label = { Text("City, Location") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_city_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            onSaveProfile(aboutMeText, professionText, cityText)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_details_button"),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Save Changes to Firestore",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    // Cloudinary Custom Configuration Dialog
    if (showConfigDialog) {
        var cloudNameInput by remember { mutableStateOf(CloudinaryUploadService.getEffectiveCloudName()) }
        var uploadPresetInput by remember { mutableStateOf(CloudinaryUploadService.getEffectiveUploadPreset()) }

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Cloudinary Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Configure your Cloudinary credentials for unsigned uploads:",
                        fontSize = 13.sp,
                        color = VeloraTextSecondary
                    )
                    OutlinedTextField(
                        value = cloudNameInput,
                        onValueChange = { cloudNameInput = it },
                        label = { Text("Cloud Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uploadPresetInput,
                        onValueChange = { uploadPresetInput = it },
                        label = { Text("Unsigned Upload Preset") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        CloudinaryUploadService.configuredCloudName = cloudNameInput.trim()
                        CloudinaryUploadService.configuredUploadPreset = uploadPresetInput.trim()
                        showConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraPink)
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

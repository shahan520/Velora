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

    // Modern Android Photo Picker (Google Play policy compliant, zero-permission)
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

            // Photos Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Profile Photos (${user.photos.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = VeloraTextPrimary
                    )
                    Text(
                        text = "First photo is your main dating profile picture",
                        fontSize = 12.sp,
                        color = VeloraTextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Quick sample photo button for testing
                    OutlinedButton(
                        onClick = { showSamplePicker = !showSamplePicker },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sample_photos_toggle_button")
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = VeloraViolet,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Samples", fontSize = 12.sp, color = VeloraViolet)
                    }

                    // Gallery photo picker
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraPink),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        enabled = !isUploading,
                        modifier = Modifier.testTag("upload_gallery_photo_button")
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Photo", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Samples Dropdown/Carousel
            AnimatedVisibility(visible = showSamplePicker) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Tap to upload curated dating profile photo to Cloudinary CDN:",
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

            // Current Photos Grid / Cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                user.photos.forEachIndexed { index, photoUrl ->
                    val isPrimary = index == 0
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isPrimary) 2.dp else 1.dp,
                            if (isPrimary) VeloraPink else VeloraCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_photo_card_$index")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Photo ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (isPrimary) {
                                    Surface(
                                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                                        color = VeloraPink,
                                        modifier = Modifier.align(Alignment.TopStart)
                                    ) {
                                        Text(
                                            text = "MAIN",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPrimary) "Primary Photo" else "Photo ${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = VeloraTextPrimary
                                )
                                Text(
                                    text = if (photoUrl.contains("cloudinary.com")) "Cloudinary CDN Hosted" else "CDN Image",
                                    fontSize = 11.sp,
                                    color = VeloraTextSecondary
                                )
                                Text(
                                    text = photoUrl,
                                    fontSize = 10.sp,
                                    color = VeloraTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row {
                                if (!isPrimary) {
                                    IconButton(
                                        onClick = { onSetPrimaryPhoto(photoUrl) },
                                        modifier = Modifier.testTag("set_primary_photo_$index")
                                    ) {
                                        Icon(
                                            Icons.Default.StarBorder,
                                            contentDescription = "Make Primary",
                                            tint = VeloraGold
                                        )
                                    }
                                }
                                if (user.photos.size > 1) {
                                    IconButton(
                                        onClick = { onRemovePhoto(photoUrl) },
                                        modifier = Modifier.testTag("delete_photo_$index")
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Photo",
                                            tint = VeloraErrorRed
                                        )
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

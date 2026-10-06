package com.safetravel.tracker.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.safetravel.tracker.ui.theme.*

/**
 * Sensitive Safety Permission Types with Clean Everyday Language.
 */
enum class SafetyPermissionType(
    val title: String,
    val subtitle: String,
    val description: String,
    val bulletPoints: List<String>,
    val icon: ImageVector,
    val accentColor: Color,
    val confirmText: String
) {
    LOCATION(
        title = "যাত্রার সুরক্ষা ও লাইভ লোকেশন অ্যাক্সেস",
        subtitle = "রিয়েল-টাইম লাইভ ট্র্যাকিং ও সুরক্ষা",
        description = "SafeTravel আপনার নিরাপদ ভ্রমণের জন্য লোকেশন ডাটা ব্যবহার করে। ট্রিপ চলাকালীন রিয়েল-টাইম জিপিএস রুট ট্র্যাক করা, স্পিড মনিটরিং এবং জরুরি গার্ডিয়ানদের সাথে লাইভ অবস্থান শেয়ার করতে এটি ব্যবহৃত হয়।\n\nভ্রমণ শুরু করার পর স্ক্রিন বন্ধ থাকলে বা অন্য অ্যাপে থাকলেও (Background) আপনার নিরাপত্তা রক্ষায় এটি সক্রিয় থাকে। আপনার তথ্য সুরক্ষিত ও সম্পূর্ণ এনক্রিপ্টেড থাকে।",
        bulletPoints = listOf(
            "রিয়েল-টাইম লাইভ রুট ট্র্যাকিং ও গতি পর্যবেক্ষণ",
            "জরুরি SOS ট্রিগারে গার্ডিয়ানদের কাছে লাইভ লোকেশন পাঠানো",
            "ভ্রমণকালে স্ক্রিন লক থাকলেও সুরক্ষামূলক ট্র্যাকিং",
            "সম্পূর্ণ সুরক্ষিত ও এন্ড-টু-এন্ড ডাটা এনক্রিপশন"
        ),
        icon = Icons.Default.LocationOn,
        accentColor = Cyan500,
        confirmText = "অনুমতি দিন"
    ),
    NOTIFICATION(
        title = "জরুরি নোটিফিকেশন ও ট্রিপ আপডেট",
        subtitle = "রিয়েল-টাইম নিরাপত্তা সতর্কতা",
        description = "জরুরি নিরাপত্তা এলার্ট ও বার্তা জানানোর জন্য নোটিফিকেশন অনুমতি প্রয়োজন:\n\n• জরুরি SOS স্টেটাস ও গার্ডিয়ানদের বার্তা\n• চলমান ট্রিপের লাইভ কন্ট্রোল ও ট্র্যাকিং আপডেট\n• নিরাপত্তা বুলেটিন ও সতর্কতা।\n\nআপনি যেকোনো সময় ফোনের সেটিংস থেকে নোটিফিকেশন পরিবর্তন করতে পারবেন।",
        bulletPoints = listOf(
            "জরুরি SOS সতর্কতা ও গার্ডিয়ান মেসেজিং",
            "চলমান ট্রিপের লাইভ কন্ট্রোল ও রুট আপডেট",
            "জরুরি নিরাপত্তা বুলেটিন ও আবহাওয়া সতর্কতা"
        ),
        icon = Icons.Default.Notifications,
        accentColor = Yellow500,
        confirmText = "চালু করুন"
    ),
    AUDIO(
        title = "অডিও ব্ল্যাকবক্স এভিডেন্স রেকর্ডিং",
        subtitle = "জরুরি মুহূর্তে নিরাপত্তা প্রমাণ সংরক্ষণ",
        description = "SafeTravel অডিও ব্ল্যাকবক্সের জন্য মাইক্রোফোন অনুমতি প্রয়োজন। ভ্রমণের সময় ম্যানুয়ালি ব্ল্যাকবক্স চালু করলে বা জরুরি SOS পাঠালে পারিপার্শ্বিক অডিও প্রমাণ হিসেবে রেকর্ড হয়ে আপনার ফোনে ও নিরাপদ ক্লাউডে সংরক্ষিত থাকে।\n\nআপনার সক্রিয় সম্মতি ছাড়া কখনো কোনো অডিও রেকর্ড করা হয় না।",
        bulletPoints = listOf(
            "ব্যবহারকারীর নির্দেশনায় অডিও ব্ল্যাকবক্স রেকর্ডিং",
            "জরুরি পরিস্থিতিতে সুরক্ষামূলক প্রমাণ হিসেবে সংরক্ষণ",
            "সম্পূর্ণ প্রাইভেট এবং সর্বোচ্চ এনক্রিপশনে সুরক্ষিত"
        ),
        icon = Icons.Default.Mic,
        accentColor = Emerald500,
        confirmText = "অনুমতি দিন"
    )
}

/**
 * Single Permission Prominent Disclosure Dialog.
 */
@Composable
fun PermissionDisclosureDialog(
    showDialog: Boolean,
    permissionType: SafetyPermissionType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Slate900.copy(alpha = 0.97f)
                ),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            permissionType.accentColor.copy(alpha = 0.7f),
                            permissionType.accentColor.copy(alpha = 0.15f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(permissionType.accentColor.copy(alpha = 0.12f))
                            .border(1.dp, permissionType.accentColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = permissionType.icon,
                            contentDescription = permissionType.title,
                            tint = permissionType.accentColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = permissionType.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = permissionType.accentColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, permissionType.accentColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = permissionType.subtitle,
                            color = permissionType.accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguriFontFamily,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = permissionType.description,
                        color = Slate300,
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        fontFamily = HindSiliguriFontFamily,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate800.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, Slate700.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        permissionType.bulletPoints.forEach { point ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = permissionType.accentColor,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = point,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    fontFamily = HindSiliguriFontFamily
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Slate600),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300)
                        ) {
                            Text(
                                text = "পরে",
                                fontSize = 13.sp,
                                fontFamily = HindSiliguriFontFamily
                            )
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = permissionType.accentColor
                            )
                        ) {
                            Text(
                                text = permissionType.confirmText,
                                color = if (permissionType == SafetyPermissionType.NOTIFICATION) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Introductory Safety Permissions Wizard Dialog shown smoothly after login/home screen load.
 */
@Composable
fun SafetyPermissionsWizardDialog(
    showDialog: Boolean,
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestLocation: (() -> Unit)? = null,
    onRequestNotification: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Slate900.copy(alpha = 0.98f)
                ),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(Cyan500.copy(alpha = 0.6f), Slate700.copy(alpha = 0.3f))
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Cyan500.copy(alpha = 0.12f))
                            .border(1.dp, Cyan500.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Safety Shield",
                            tint = Cyan500,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "নিরাপত্তা সুরক্ষা পরিচিতি",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "ভ্রমণে আপনার সর্বোচ্চ সুরক্ষায় যেসব ফিচার কাজ করবে",
                        color = Slate400,
                        fontSize = 12.sp,
                        fontFamily = HindSiliguriFontFamily,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location Card
                    PermissionStatusCard(
                        title = "লোকেশন ও লাইভ ট্র্যাকিং",
                        description = "ভ্রমণকালে আপনার লাইভ রুট মনিটরিং, পথ নিরাপত্তা যাচাই এবং জরুরি বিপদে গার্ডিয়ানদের কাছে তাৎক্ষণিক লাইভ অবস্থান পৌঁছানোর জন্য এটি প্রয়োজন।",
                        isGranted = hasLocationPermission,
                        icon = Icons.Default.LocationOn,
                        accentColor = Cyan500,
                        onGrant = null
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Notification Card
                    PermissionStatusCard(
                        title = "জরুরি নোটিফিকেশন",
                        description = "ট্রিপ চলাকালীন নিরাপত্তা স্ট্যাটাস, গার্ডিয়ানদের সতর্কতা এবং জরুরি SOS অ্যালার্ট তাৎক্ষণিকভাবে আপনার স্ক্রিনে পৌঁছে দেওয়ার জন্য প্রয়োজন।",
                        isGranted = hasNotificationPermission,
                        icon = Icons.Default.Notifications,
                        accentColor = Yellow500,
                        onGrant = null
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Audio Blackbox Info Card
                    PermissionStatusCard(
                        title = "নিরাপত্তা অডিও ব্ল্যাকবক্স",
                        description = "জরুরি পরিস্থিতিতে আপনার সম্মতি নিয়ে ব্যাকগ্রাউন্ডে পারিপার্শ্বিক অডিও প্রমাণ এনক্রিপ্ট করে ক্লাউডে সংরক্ষণের সুবিধা রয়েছে।",
                        isGranted = null,
                        icon = Icons.Default.Mic,
                        accentColor = Emerald500,
                        onGrant = null
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Single informative acknowledgment button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Cyan500
                        )
                    ) {
                        Text(
                            text = "ঠিক আছে",
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Location Wizard Dialog triggered when user taps location-based quick actions
 * (e.g. Nearby Thana, Fire Station, Hospitals, Weather) without granting location.
 */
@Composable
fun ActionLocationRequiredWizardDialog(
    showDialog: Boolean,
    actionTitle: String,
    onGrantLocation: () -> Unit,
    onContinueWithoutLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.98f)),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(Cyan500.copy(alpha = 0.65f), Slate700.copy(alpha = 0.3f))
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Cyan500.copy(alpha = 0.12f))
                            .border(1.dp, Cyan500.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location Access",
                            tint = Cyan500,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "নিকটস্থ $actionTitle দেখতে লোকেশন প্রয়োজন",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Yellow500.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Yellow500.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "দূরত্ব ও নিকটস্থ সেবা ফিল্টারিং",
                            color = Yellow500,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = HindSiliguriFontFamily,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "আপনার বর্তমান অবস্থান ছাড়া আপনার সবচেয়ে কাছের $actionTitle কোনটি তা শনাক্ত করা ও সঠিক দূরত্ব নির্ধারণ করা সম্ভব নয়।\n\nআপনি কি লোকেশন অ্যাক্সেস দিয়ে নিকটস্থ তালিকা দেখতে চান?",
                        color = Slate300,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = HindSiliguriFontFamily,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(0.9f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Slate600),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300)
                        ) {
                            Text(
                                text = "বাতিল",
                                fontSize = 12.sp,
                                fontFamily = HindSiliguriFontFamily
                            )
                        }

                        OutlinedButton(
                            onClick = onContinueWithoutLocation,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Slate600),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300)
                        ) {
                            Text(
                                text = "অনুমতি ছাড়া",
                                fontSize = 12.sp,
                                fontFamily = HindSiliguriFontFamily,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = onGrantLocation,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan500)
                        ) {
                            Text(
                                text = "অনুমতি দিন",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = HindSiliguriFontFamily,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    isGranted: Boolean?,
    icon: ImageVector,
    accentColor: Color,
    onGrant: (() -> Unit)?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Slate800.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (isGranted == true) Emerald500.copy(alpha = 0.4f) else Slate700.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = HindSiliguriFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val (badgeText, badgeColor, badgeBg) = when (isGranted) {
                        true -> Triple("সক্রিয়", Emerald500, Emerald500.copy(alpha = 0.15f))
                        false -> Triple("প্রয়োজন", Yellow500, Yellow500.copy(alpha = 0.15f))
                        else -> Triple("ঐচ্ছিক", Slate300, Slate500.copy(alpha = 0.2f))
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg,
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = HindSiliguriFontFamily,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = description,
                    color = Slate400,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = HindSiliguriFontFamily
                )
            }

            if (isGranted == false && onGrant != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onGrant,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Grant",
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

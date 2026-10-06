package com.safetravel.tracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.safetravel.tracker.R
import com.safetravel.tracker.supabase.SupabaseSafetyNews
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailScreen(
    news: SupabaseSafetyNews,
    onBack: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val context = LocalContext.current
    
    // Header Alpha based on scroll
    val headerAlpha by remember {
        derivedStateOf {
            val firstItemIndex = scrollState.firstVisibleItemIndex
            val firstItemOffset = scrollState.firstVisibleItemScrollOffset
            if (firstItemIndex > 0) 1f
            else (firstItemOffset.toFloat() / 500f).coerceIn(0f, 1f)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Slate900)) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. HERO IMAGE WITH PARALLAX
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .graphicsLayer {
                            translationY = scrollState.firstVisibleItemScrollOffset.toFloat() / 2.5f
                            alpha = (1f - (scrollState.firstVisibleItemScrollOffset.toFloat() / 800f)).coerceIn(0f, 1f)
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(news.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_launcher_background)
                    )
                    
                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Slate900.copy(alpha = 0.5f),
                                        Slate900
                                    )
                                )
                            )
                    )

                    // Category Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        color = getCategoryColor(news.category).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, getCategoryColor(news.category))
                    ) {
                        Text(
                            text = news.category.uppercase(),
                            color = getCategoryColor(news.category),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 2. CONTENT AREA
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate900)
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Title
                    Text(
                        text = news.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 25.sp,
                        fontFamily = news.title.appFontFamily
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Meta Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatNewsTimestamp(news.publishedAt ?: news.createdAt),
                            color = Slate400,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${calculateReadTime(news.content ?: "")} min read",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Description (Lead)
                    Text(
                        text = news.description,
                        color = Slate300,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp,
                        fontFamily = news.description.appFontFamily,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(listOf(Slate700, Color.Transparent)),
                                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                            )
                            .padding(start = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Main Content
                    Text(
                        text = news.content ?: "No detailed content available for this report.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.5.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = (news.content ?: "").appFontFamily
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        // 3. READING PROGRESS BAR
        val progress by remember {
            derivedStateOf {
                val totalItems = scrollState.layoutInfo.totalItemsCount
                if (totalItems <= 0) 0f
                else {
                    val current = scrollState.firstVisibleItemIndex.toFloat()
                    (current / (totalItems - 1)).coerceIn(0f, 1f)
                }
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(2.dp)
                .background(Color(0xFF10B981))
                .align(Alignment.TopStart)
        )
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "traffic" -> Color(0xFFF59E0B) // Amber
        "emergency" -> Color(0xFFEF4444) // Red
        "weather" -> Color(0xFF3B82F6) // Blue
        else -> Color(0xFF10B981) // Emerald
    }
}

private fun calculateReadTime(content: String): Int {
    val wordsPerMinute = 200
    val words = content.split("\\s+".toRegex()).size
    return (words / wordsPerMinute).coerceAtLeast(1)
}

private fun formatNewsTimestamp(rawTimestamp: String?): String {
    if (rawTimestamp.isNullOrBlank()) return "Recent update"
    val trimmed = rawTimestamp.trim()
    // If it's already a relative or human-readable format (e.g. "15 minutes ago", "Just now"), return as is
    if (!trimmed.first().isDigit()) return trimmed
    return DateTimeUtils.formatBstDateTime(trimmed, "dd MMM yyyy, hh:mm a").ifBlank { trimmed }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun NewsDetailPreview() {
    val mockNews = SupabaseSafetyNews(
        id = "1",
        title = "Critical Traffic Alert: Dhaka-Chittagong Highway Blocked",
        description = "A major multi-vehicle collision has occurred near the Gazipur bypass, causing a total standstill in both directions.",
        content = """
            Local authorities report that the incident occurred around 8:45 AM this morning. Emergency services are currently on the scene working to clear the wreckage and assist those involved.
            
            Witnesses suggest that heavy morning fog and high speed were contributing factors. Two cargo trucks and several passenger vehicles were caught in the pile-up.
            
            Travelers are strongly advised to avoid this route for the next 4-6 hours. Alternative routes through the bypass roads are also experiencing significant congestion as traffic is being diverted. 
            
            Safety Tips for Diverted Traffic:
            1. Keep your headlights on for visibility.
            2. Maintain a safe following distance.
            3. Follow instructions from traffic police on site.
            
            Updates will be provided as soon as the clearance operations make progress. Stay safe and plan your travel accordingly.
        """.trimIndent(),
        category = "Traffic",
        imageUrl = "https://images.unsplash.com/photo-1545147418-4f1e6b63edc0",
        publishedAt = "15 minutes ago"
    )

    SafeTravelTheme {
        NewsDetailScreen(news = mockNews, onBack = {})
    }
}

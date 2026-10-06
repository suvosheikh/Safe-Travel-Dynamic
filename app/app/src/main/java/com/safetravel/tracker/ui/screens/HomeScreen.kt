package com.safetravel.tracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.safetravel.tracker.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safetravel.tracker.supabase.SupabaseHomepageBanner
import com.safetravel.tracker.supabase.SupabaseSafetyNews
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.ui.components.*
import com.safetravel.tracker.viewmodel.SafeTravelViewModel

@Composable
fun HomeScreen(vm: SafeTravelViewModel, onNewsClick: (SupabaseSafetyNews) -> Unit, onActionClick: (String) -> Unit) {
    val banners by vm.homepageBanners.collectAsState()
    val newsList by vm.safetyNews.collectAsState()
    val context = LocalContext.current

    // Trigger pre-fetching when data changes
    LaunchedEffect(banners, newsList) {
        vm.prefetchImages(context)
    }

    HomeScreenContent(
        banners = banners,
        newsList = newsList,
        onNewsClick = onNewsClick,
        onActionClick = onActionClick
    )
}

@Composable
fun HomeScreenContent(
    banners: List<SupabaseHomepageBanner>,
    newsList: List<SupabaseSafetyNews>,
    onNewsClick: (SupabaseSafetyNews) -> Unit,
    onActionClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp) // Space for bottom bar
    ) {
        // 1. HERO BANNER (16:9)
        if (banners.isNotEmpty()) {
            item(key = "hero_banners") {
                BannerSection(banners)
            }
        }

        // 3. QUICK ACTION GRID
        item(key = "quick_actions_grid") {
            QuickActionGrid(onActionClick)
        }

        // 4. LATEST SAFETY NEWS
        if (newsList.isNotEmpty()) {
            item(key = "safety_news_header") {
                NewsSectionHeader()
            }

            items(newsList, key = { it.id }) { news ->
                NewsCard(news, onNewsClick)
            }
        }
    }
}

@Composable
fun BannerSection(banners: List<com.safetravel.tracker.supabase.SupabaseHomepageBanner>) {
    if (banners.isEmpty()) return
    var currentPage by remember { mutableIntStateOf(0) }
    
    // Auto-slide effect every 8 seconds
    LaunchedEffect(banners.size) {
        while (true) {
            delay(8000)
            if (banners.isNotEmpty()) {
                currentPage = (currentPage + 1) % banners.size
            }
        }
    }

    val safeIndex = if (currentPage < banners.size) currentPage else 0
    val currentBanner = banners[safeIndex]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 2.dp)
    ) {
        // Fixed Card Frame with reduced height (2.1:1 ratio)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.1f / 1f)
                .glassmorphic(shape = RoundedCornerShape(20.dp), shadowElevation = 3.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = currentBanner,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(1000)) + scaleIn(initialScale = 1.05f, animationSpec = tween(1000)))
                        .togetherWith(fadeOut(animationSpec = tween(1000)))
                },
                label = "bannerAnimation"
            ) { banner ->
                HeroContent(banner)
            }
        }

        // Indicators placed inside the image frame (bottom-center)
        if (banners.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(banners.size) { iteration ->
                    val isSelected = currentPage == iteration
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 14.dp else 5.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(if (isSelected) GlassColors.PrimaryOrange else Color.White.copy(alpha = 0.5f))
                            .clickable { currentPage = iteration }
                    )
                }
            }
        }
    }
}

@Composable
fun HeroContent(banner: com.safetravel.tracker.supabase.SupabaseHomepageBanner) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(banner.imageUrl)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(shimmerBrush())
                )
            },
            error = {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        )
        
        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = banner.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Read More", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// HeroBanner function removed and replaced by HeroContent inside BannerSection


@Composable
fun QuickActionGrid(onActionClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Quick Action Grid",
            color = GlassColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 4 Columns Grid
        val items = listOf(
            GridItem("Thana", Icons.Default.LocationOn, Color(0xFF10B981)),
            GridItem("Emergency Contacts", Icons.Default.Phone, Color(0xFF3B82F6)),
            GridItem("Safety Tips", Icons.Default.Lock, Color(0xFFF59E0B)),
            GridItem("Nearby Hospitals", Icons.Default.LocalHospital, Color(0xFFEF4444)),
            GridItem("Fire Service", Icons.Default.LocalFireDepartment, Color(0xFFF97316)),
            GridItem("Blood Bank", Icons.Default.WaterDrop, Color(0xFFDC2626)),
            GridItem("Traffic Alerts", Icons.Default.Traffic, Color(0xFFFFCC00)),
            GridItem("Weather Alerts", Icons.Default.Cloud, Color(0xFF60A5FA))
        )

        val rows = items.chunked(4)
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .height(IntrinsicSize.Min), // Forces all items in row to have same height as the tallest one
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { item ->
                    ActionCard(item, modifier = Modifier.weight(1f), onClick = { onActionClick(item.label) })
                }
            }
        }
    }
}

@Composable
fun ActionCard(item: GridItem, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .glassmorphic(shape = RoundedCornerShape(20.dp), shadowElevation = 3.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp) // Slightly smaller icon box
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.label,
                color = GlassColors.TextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
                maxLines = 2,
                softWrap = true
            )
        }
    }
}

@Composable
fun NewsSectionHeader() {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "লাইভ আপডেট",
            color = Color(0xFF06B6D4),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = HindSiliguriFontFamily
        )
        Text(
            text = "Latest Safety News",
            color = GlassColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NewsCard(news: com.safetravel.tracker.supabase.SupabaseSafetyNews, onClick: (SupabaseSafetyNews) -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .glassmorphic(shape = RoundedCornerShape(24.dp), shadowElevation = 4.dp)
            .clickable { onClick(news) }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(news.imageUrl)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(shimmerBrush())
                    )
                },
                error = {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_background),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = news.title,
                    color = GlassColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = news.description,
                    color = GlassColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Read More",
                    color = GlassColors.PrimaryOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onClick(news) }
                )
            }
        }
    }
}

@Composable
fun shimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1000f
): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.White.copy(alpha = 0.3f),
            Color.White.copy(alpha = 0.7f),
            Color.White.copy(alpha = 0.3f),
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(800), repeatMode = RepeatMode.Restart
            ), label = "shimmer"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset.Zero
        )
    }
}

data class GridItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val mockBanners = listOf(
        SupabaseHomepageBanner(
            id = "1",
            title = "Campaign: Stay Safe on Roads",
            imageUrl = "https://images.unsplash.com/photo-1545147418-4f1e6b63edc0",
            isActive = true
        ),
        SupabaseHomepageBanner(
            id = "2",
            title = "Emergency Protocol Training",
            imageUrl = "https://images.unsplash.com/photo-1517404215738-15263e9f9178",
            isActive = true
        )
    )

    val mockNews = listOf(
        SupabaseSafetyNews(
            id = "1",
            title = "Traffic Alert: Dhaka-Chittagong Highway",
            description = "Severe congestion reported near Gazipur due to construction works. Expect delays of up to 2 hours."
        )
    )

    SafeTravelTheme {
        HomeScreenContent(
            banners = mockBanners,
            newsList = mockNews,
            onNewsClick = {},
            onActionClick = {}
        )
    }
}


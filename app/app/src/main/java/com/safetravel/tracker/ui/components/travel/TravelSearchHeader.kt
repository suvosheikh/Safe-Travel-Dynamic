// File: /app/src/main/java/com/safetravel/tracker/ui/components/travel/TravelSearchHeader.kt
package com.safetravel.tracker.ui.components.travel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.ui.theme.*

import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun TravelSearchHeaderPreview() {
    SafeTravelTheme {
        TravelSearchHeader(
            destination = "",
            onDestinationChange = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelSearchHeader(
    destination: String,
    onDestinationChange: (String) -> Unit,
    onSearchClicked: () -> Unit = {},
    suggestions: List<com.safetravel.tracker.viewmodel.GeocodeSuggestion> = emptyList(),
    onSuggestionSelected: (com.safetravel.tracker.viewmodel.GeocodeSuggestion) -> Unit = {},
    isSearching: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.8f)),
        colors = CardDefaults.cardColors(containerColor = Slate800)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Emerald400, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAFE JOURNEY TRACKER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PremiumGold,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Emerald400.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isSearching) "SEARCHING..." else "GPS PASSIVE ENCRYPTED",
                        color = Emerald400,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp
                    )
                }
            }

            // Route search input container (Only Destination / Search criteria)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate900, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val displayValue = remember(destination) {
                    if (destination.contains(" [") && destination.contains("]")) {
                        destination.substringBefore(" [")
                    } else {
                        destination
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Destination Point Icon",
                        tint = Red500,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = displayValue,
                        onValueChange = onDestinationChange,
                        placeholder = { Text("Search your destination or landmark...", color = Slate300.copy(alpha = 0.7f), fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { onSearchClicked() }
                        ),
                        trailingIcon = {
                            IconButton(onClick = onSearchClicked) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Perform geocoding query",
                                    tint = Emerald400,
                                    modifier = Modifier.size(20.dp)
                                    )
                            }
                        }
                    )
                }
            }

            // Suggestions List Overlay View
            if (suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Slate700.copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "SELECT SEARCH RESULT Landmark:",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PremiumGold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate900, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    suggestions.forEach { sug ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSuggestionSelected(sug) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Landmark point",
                                tint = Emerald400,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (sug.name.contains(",")) sug.name.substringBefore(",") else sug.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (sug.name.contains(",")) {
                                    Text(
                                        text = sug.name.substringAfter(",").trim(),
                                        color = Slate300,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Select option",
                                tint = Emerald400.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

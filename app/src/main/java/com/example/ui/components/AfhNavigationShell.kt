package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.AfhStrings
import com.example.core.AppLanguage
import com.example.model.Conversation
import com.example.model.UserProfile
import com.example.ui.theme.AfhCyan
import com.example.ui.theme.AfhElectricBlue
import com.example.ui.theme.AfhTurquoise

enum class AfhDestination {
    CHAT, EXPLORE, SETTINGS, ADMIN
}

@Composable
fun AfhSidebarContent(
    language: AppLanguage,
    currentDestination: AfhDestination,
    activeConversationId: String?,
    conversations: List<Conversation>,
    searchQuery: String,
    favoritesOnly: Boolean,
    profile: UserProfile?,
    isAdmin: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onNewChat: () -> Unit,
    onSelectConversation: (Conversation) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onToggleFavorite: (Conversation) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onNavigate: (AfhDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AfhStrings.get(language)
    var renamingConv by remember { mutableStateOf<Conversation?>(null) }

    val filteredConversations = remember(conversations, searchQuery, favoritesOnly) {
        conversations.filter { conv ->
            val matchesQuery = searchQuery.isBlank() ||
                conv.title.contains(searchQuery, ignoreCase = true) ||
                conv.lastMessagePreview.contains(searchQuery, ignoreCase = true)
            val matchesFav = !favoritesOnly || conv.favorite
            matchesQuery && matchesFav
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AfhHorizontalLogo(
                iconSize = 34.dp,
                showSlogan = true,
                sloganText = strings.slogan,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Button(
                onClick = onNewChat,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AfhElectricBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sidebar_new_chat_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.newChat, fontWeight = FontWeight.SemiBold)
            }

            // Primary navigation destinations
            NavigationDrawerItem(
                label = { Text(strings.navChat) },
                selected = currentDestination == AfhDestination.CHAT,
                onClick = { onNavigate(AfhDestination.CHAT) },
                icon = { Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null) },
                modifier = Modifier.testTag("nav_chat")
            )
            NavigationDrawerItem(
                label = { Text(strings.navExplore) },
                selected = currentDestination == AfhDestination.EXPLORE,
                onClick = { onNavigate(AfhDestination.EXPLORE) },
                icon = { Icon(Icons.Rounded.Explore, contentDescription = null) },
                modifier = Modifier.testTag("nav_explore")
            )
            NavigationDrawerItem(
                label = { Text(strings.navSettings) },
                selected = currentDestination == AfhDestination.SETTINGS,
                onClick = { onNavigate(AfhDestination.SETTINGS) },
                icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                modifier = Modifier.testTag("nav_settings")
            )
            if (isAdmin) {
                NavigationDrawerItem(
                    label = { Text(strings.navAdmin) },
                    selected = currentDestination == AfhDestination.ADMIN,
                    onClick = { onNavigate(AfhDestination.ADMIN) },
                    icon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = AfhTurquoise) },
                    modifier = Modifier.testTag("nav_admin")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            // Search + Favorites
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(strings.searchConversations, style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sidebar_search_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.recentHistory,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = onToggleFavoritesFilter,
                    label = { Text(strings.favorites, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (favoritesOnly) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    modifier = Modifier.testTag("sidebar_favorites_chip")
                )
            }

            // Conversations list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filteredConversations.isEmpty()) {
                    item {
                        Text(
                            text = strings.noConversationsYet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    items(filteredConversations, key = { it.id }) { conv ->
                        val isSelected = conv.id == activeConversationId && currentDestination == AfhDestination.CHAT
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) AfhElectricBlue.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) BorderStroke(1.dp, AfhCyan.copy(alpha = 0.45f)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectConversation(conv) }
                                .testTag("conversation_item_${conv.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = conv.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (conv.lastMessagePreview.isNotBlank()) {
                                        Text(
                                            text = conv.lastMessagePreview,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onToggleFavorite(conv) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (conv.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (conv.favorite) AfhCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { renamingConv = conv },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = strings.renameConversation,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteConversation(conv.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = strings.deleteConversation,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            // User Profile Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(AfhDestination.SETTINGS) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!profile?.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile?.avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = AfhElectricBlue.copy(alpha = 0.25f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (profile?.name?.firstOrNull() ?: 'A').uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = AfhCyan
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.name ?: "AFH User",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${profile?.dailyRequestsUsed ?: 0}/${profile?.dailyLimit ?: 50} today",
                        style = MaterialTheme.typography.labelMedium,
                        color = AfhTurquoise
                    )
                }
            }
        }
    }

    renamingConv?.let { conv ->
        var newTitle by remember(conv) { mutableStateOf(conv.title) }
        AlertDialog(
            onDismissRequest = { renamingConv = null },
            title = { Text(strings.renameConversation) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onRenameConversation(conv.id, newTitle.trim())
                        }
                        renamingConv = null
                    }
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { renamingConv = null }) { Text(strings.cancel) }
            }
        )
    }
}

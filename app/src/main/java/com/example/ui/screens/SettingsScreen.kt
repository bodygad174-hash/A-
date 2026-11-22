package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.AfhStrings
import com.example.core.AppLanguage
import com.example.model.AfhModelSpec
import com.example.model.UserProfile
import com.example.ui.components.AfhGlassCard
import com.example.ui.theme.AfhCyan
import com.example.ui.theme.AfhElectricBlue
import com.example.ui.theme.AfhTurquoise

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    profile: UserProfile?,
    models: List<AfhModelSpec>,
    language: AppLanguage,
    themeMode: String,
    onUpdateName: (String) -> Unit,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateTheme: (String) -> Unit,
    onUpdateDefaultModel: (String) -> Unit,
    onToggleStreaming: (Boolean) -> Unit,
    onClearAllConversations: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AfhStrings.get(language)
    val isArabic = language == AppLanguage.AR

    var editedName by remember(profile?.name) { mutableStateOf(profile?.name ?: "") }
    var confirmClearChats by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
            )
        }

        // 1. Profile Section
        item {
            AfhGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = strings.profileSection,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AfhCyan
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (!profile?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profile?.avatarUrl,
                                contentDescription = "User avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = AfhElectricBlue.copy(alpha = 0.2f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = "Profile",
                                    tint = AfhCyan,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.name ?: "AFH User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = profile?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AfhTurquoise.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = (profile?.role ?: "user").uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = AfhTurquoise,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text(if (isArabic) "الاسم المعروض" else "Display Name") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_name_input")
                        )
                        Button(
                            onClick = { onUpdateName(editedName.trim()) },
                            enabled = editedName.isNotBlank() && editedName.trim() != profile?.name,
                            modifier = Modifier.testTag("settings_save_name_button")
                        ) {
                            Text(strings.save)
                        }
                    }
                }
            }
        }

        // 2. Appearance & Language
        item {
            AfhGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = strings.appearanceSection,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AfhCyan
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "dark" to strings.themeDark,
                            "light" to strings.themeLight,
                            "system" to strings.themeSystem
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = themeMode == key,
                                onClick = { onUpdateTheme(key) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("theme_chip_$key")
                            )
                        }
                    }

                    Text(
                        text = strings.languageSection,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AfhCyan
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = language == AppLanguage.EN,
                            onClick = { onUpdateLanguage(AppLanguage.EN) },
                            label = { Text("English (LTR)") },
                            modifier = Modifier.testTag("lang_chip_en")
                        )
                        FilterChip(
                            selected = language == AppLanguage.AR,
                            onClick = { onUpdateLanguage(AppLanguage.AR) },
                            label = { Text("العربية (RTL)") },
                            modifier = Modifier.testTag("lang_chip_ar")
                        )
                    }
                }
            }
        }

        // 3. Default AFH Model & Chat Preferences
        item {
            AfhGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.defaultModelSection,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AfhCyan
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        models.filter { it.enabled }.forEach { model ->
                            FilterChip(
                                selected = (profile?.defaultAfhModelId ?: "afh_flash") == model.id,
                                onClick = { onUpdateDefaultModel(model.id) },
                                label = { Text(model.localizedName(isArabic)) },
                                modifier = Modifier.testTag("default_model_${model.id}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.streamingResponses,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = profile?.streamingEnabled ?: true,
                            onCheckedChange = onToggleStreaming,
                            modifier = Modifier.testTag("settings_streaming_switch")
                        )
                    }
                }
            }
        }

        // 4. Usage Quotas
        item {
            AfhGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.usageSection,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AfhCyan
                    )
                    val dailyUsed = profile?.dailyRequestsUsed ?: 0L
                    val dailyMax = (profile?.dailyLimit ?: 50L).coerceAtLeast(1L)
                    val monthlyUsed = profile?.monthlyRequestsUsed ?: 0L
                    val monthlyMax = (profile?.monthlyLimit ?: 1000L).coerceAtLeast(1L)

                    Text(
                        text = "${strings.dailyUsage}: $dailyUsed / $dailyMax",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { (dailyUsed.toFloat() / dailyMax.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = AfhCyan
                    )

                    Text(
                        text = "${strings.monthlyUsage}: $monthlyUsed / $monthlyMax",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { (monthlyUsed.toFloat() / monthlyMax.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = AfhTurquoise
                    )
                }
            }
        }

        // 5. Privacy, Data & Account Security
        item {
            AfhGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = AfhTurquoise
                        )
                        Text(
                            text = strings.privacySecuritySection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AfhCyan
                        )
                    }

                    OutlinedButton(
                        onClick = { confirmClearChats = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_all_conversations_button")
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.clearAllConversations)
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.signOut)
                    }

                    Button(
                        onClick = { confirmDeleteAccount = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("delete_account_button")
                    ) {
                        Text(strings.deleteAccount)
                    }
                }
            }
        }
    }

    if (confirmClearChats) {
        AlertDialog(
            onDismissRequest = { confirmClearChats = false },
            title = { Text(strings.clearAllConversations) },
            text = {
                Text(
                    if (isArabic) "سيتم حذف جميع محادثاتك ورسائلك نهائياً."
                    else "This permanently deletes all your AFH AI conversations and messages."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmClearChats = false
                        onClearAllConversations()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearChats = false }) { Text(strings.cancel) }
            }
        )
    }

    if (confirmDeleteAccount) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAccount = false },
            title = { Text(strings.deleteAccount) },
            text = {
                Text(
                    if (isArabic) "هل أنت متأكد من حذف حسابك وجميع بياناتك وتسجيل الخروج؟"
                    else "This deletes your AFH AI profile, conversations, and signs you out."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteAccount = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAccount = false }) { Text(strings.cancel) }
            }
        )
    }
}

package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.core.AfhStrings
import com.example.core.AppLanguage
import com.example.data.AuthManager
import com.example.ui.components.AfhGlassCard
import com.example.ui.components.AfhLogoMark
import com.example.ui.theme.AfhCyan
import com.example.ui.theme.AfhElectricBlue
import com.example.ui.theme.AfhSoftPurple
import com.example.ui.theme.AfhTurquoise

@Composable
fun AuthScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val strings = AfhStrings.get(language)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AfhElectricBlue.copy(alpha = 0.22f),
                            AfhSoftPurple.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding)
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Language quick toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilterChip(
                        selected = language == AppLanguage.AR,
                        onClick = {
                            onLanguageChange(if (language == AppLanguage.EN) AppLanguage.AR else AppLanguage.EN)
                        },
                        label = {
                            Text(if (language == AppLanguage.EN) "العربية" else "English")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = "Switch language",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("auth_language_toggle")
                    )
                }

                AfhLogoMark(size = 76.dp, animated = true)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.brandName,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = strings.slogan,
                        style = MaterialTheme.typography.titleMedium,
                        color = AfhCyan,
                        textAlign = TextAlign.Center
                    )
                }

                // Uploaded brand artwork preview
                Image(
                    painter = painterResource(id = R.drawable.afh_hero_artwork),
                    contentDescription = "AFH AI Neural Brand Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .clip(RoundedCornerShape(24.dp))
                )

                AfhGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strings.heroHeadline,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = strings.signInSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                AuthManager.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AfhElectricBlue,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.4.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(strings.signingIn)
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.signInWithGoogle,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            AuthSecurityBadge(
                                icon = Icons.Rounded.Lock,
                                label = if (language == AppLanguage.AR) "تشفير كامل" else "Zero-Trust Sync"
                            )
                            AuthSecurityBadge(
                                icon = Icons.Rounded.Shield,
                                label = if (language == AppLanguage.AR) "٧ نماذج ذكية" else "7 AFH Models"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthSecurityBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AfhTurquoise,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

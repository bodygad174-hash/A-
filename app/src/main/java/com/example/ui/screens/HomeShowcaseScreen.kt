package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AfhStrings
import com.example.core.AppLanguage
import com.example.model.AfhModelSpec
import com.example.ui.components.AfhGlassCard
import com.example.ui.components.AfhLogoMark
import com.example.ui.components.afhModelAccentColor
import com.example.ui.components.afhModelIcon
import com.example.ui.theme.AfhCyan
import com.example.ui.theme.AfhElectricBlue
import com.example.ui.theme.AfhSoftPurple
import com.example.ui.theme.AfhTurquoise

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeShowcaseScreen(
    language: AppLanguage,
    models: List<AfhModelSpec>,
    onStartChatting: () -> Unit,
    onSelectModelAndChat: (AfhModelSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AfhStrings.get(language)
    val isArabic = language == AppLanguage.AR

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Section
        item {
            AfhGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    AfhElectricBlue.copy(alpha = 0.16f),
                                    AfhSoftPurple.copy(alpha = 0.10f),
                                    AfhCyan.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AfhLogoMark(size = 52.dp, animated = true)
                        Column {
                            Text(
                                text = strings.brandName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.slogan,
                                style = MaterialTheme.typography.labelLarge,
                                color = AfhCyan
                            )
                        }
                    }

                    Text(
                        text = strings.heroHeadline,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = strings.heroSubtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Image(
                        painter = painterResource(id = R.drawable.afh_hero_artwork),
                        contentDescription = "AFH AI Platform Visual",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onStartChatting,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AfhElectricBlue),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("hero_start_chatting_button")
                        ) {
                            Text(strings.startChatting)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                models.firstOrNull { it.enabled }?.let(onSelectModelAndChat)
                                    ?: onStartChatting()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("hero_explore_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = AfhCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.exploreAfhAi)
                        }
                    }
                }
            }
        }

        // Model Showcase Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isArabic) "منظومة نماذج AFH AI" else "AFH AI Model Suite",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                models.forEach { model ->
                    val accent = afhModelAccentColor(model.iconKey)
                    AfhGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = model.enabled) { onSelectModelAndChat(model) }
                            .testTag("showcase_model_${model.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = accent.copy(alpha = 0.16f),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = afhModelIcon(model.iconKey),
                                        contentDescription = model.localizedName(isArabic),
                                        tint = accent,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = model.localizedName(isArabic),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (model.enabled) AfhTurquoise.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (!model.enabled) strings.unavailableBadge
                                            else when (model.tierType) {
                                                "free" -> strings.freeTierBadge
                                                "limited" -> strings.limitedTierBadge
                                                else -> strings.proTierBadge
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (model.enabled) AfhTurquoise else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = model.localizedDescription(isArabic),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    model.capabilities.forEach { cap ->
                                        AssistChip(
                                            onClick = { if (model.enabled) onSelectModelAndChat(model) },
                                            label = {
                                                Text(
                                                    text = cap,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Platform Features + How It Works + Privacy & Security
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.platformFeaturesTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                FeatureRowCard(
                    icon = Icons.Rounded.Layers,
                    title = if (isArabic) "واجهة واحدة موحدة" else "Unified Intelligence Workspace",
                    description = if (isArabic)
                        "تبديل سلس بين نماذج AFH مع الحفاظ على سياق المحادثة والملفات."
                    else "Switch effortlessly across AFH Flash, Pro, Code, Study, Creative, Vision, and Research inside one identity."
                )
                FeatureRowCard(
                    icon = Icons.Rounded.Speed,
                    title = if (isArabic) "بث فوري للردود" else "Real-Time Streaming & Free-First Limits",
                    description = if (isArabic)
                        "استجابات فورية مع عدادات استهلاك واضحة وشفافة."
                    else "Low-latency streaming responses with transparent daily & monthly usage quotas."
                )
                FeatureRowCard(
                    icon = Icons.Rounded.Devices,
                    title = if (isArabic) "مزامنة عبر الأجهزة" else "Cloud History & Bilingual RTL",
                    description = if (isArabic)
                        "مزامنة كاملة للمحادثات ودعم كامل للغتين العربية والإنجليزية."
                    else "Persistent cloud conversations, favorites, search, and native Arabic RTL + English LTR."
                )
            }
        }

        item {
            AfhGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.howItWorksTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isArabic)
                            "١. اختر نموذج AFH المناسب لمهمتك\n٢. أرفق صوراً أو مستندات أو اكتب مباشرة\n٣. استعرض الردود الفورية واحفظ محادثاتك"
                        else "1. Choose the AFH model tailored to your task\n2. Attach photos, code, or PDFs when supported\n3. Stream real-time answers and sync history securely",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            AfhGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = AfhTurquoise
                        )
                        Text(
                            text = strings.privacySectionTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isArabic)
                            "تُحفظ بياناتك ومحادثاتك بعزل تام لكل مستخدم عبر قواعد أمان صارمة، بدون مفاتيح API مكشوفة في الواجهة."
                        else "Your conversations are isolated per account with zero-trust database rules, App Check attestation, and no client-side API secrets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Brand Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = AfhCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "AFH AI — ${strings.slogan}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRowCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    AfhGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AfhElectricBlue.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AfhCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

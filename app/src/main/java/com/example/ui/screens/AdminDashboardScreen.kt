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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AfhStrings
import com.example.core.AppLanguage
import com.example.model.AdminProviderMapping
import com.example.model.AfhCatalogDefaults
import com.example.model.AfhModelSpec
import com.example.model.ProviderSetting
import com.example.model.UsageLog
import com.example.model.UserProfile
import com.example.ui.components.AfhGlassCard
import com.example.ui.theme.AfhCyan
import com.example.ui.theme.AfhElectricBlue
import com.example.ui.theme.AfhSoftPurple
import com.example.ui.theme.AfhTurquoise
import com.example.ui.theme.AfhWarning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    language: AppLanguage,
    users: List<UserProfile>,
    models: List<AfhModelSpec>,
    mappings: Map<String, AdminProviderMapping>,
    providers: List<ProviderSetting>,
    usageLogs: List<UsageLog>,
    totalUserConversations: Int,
    feedbackMessage: String?,
    onDismissFeedback: () -> Unit,
    onUpdateModel: (AfhModelSpec) -> Unit,
    onUpdateMapping: (AdminProviderMapping) -> Unit,
    onUpdateProvider: (ProviderSetting) -> Unit,
    onUpdateUser: (targetUserId: String, status: String, role: String, dailyLimit: Long, monthlyLimit: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AfhStrings.get(language)
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingMapping by remember { mutableStateOf<AdminProviderMapping?>(null) }
    var editingModelLimits by remember { mutableStateOf<AfhModelSpec?>(null) }

    val tabs = listOf(
        "Overview",
        "Models & Routing",
        "Providers",
        "Users & Logs"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 900.dp)
        ) {
            Text(
                text = strings.adminDashboardTitle,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Private technical routing, provider management, quotas & monitoring",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (feedbackMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AfhTurquoise.copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(feedbackMessage, color = AfhTurquoise, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onDismissFeedback) { Text("OK") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label, maxLines = 1) },
                        modifier = Modifier.testTag("admin_tab_$idx")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // KPI Overview
                        item {
                            val totalReq = usageLogs.size
                            val errorCount = usageLogs.count { it.status == "error" }
                            val errorRatePct = if (totalReq > 0) (errorCount * 100f / totalReq) else 0f
                            val activeUsersCount = users.count { it.status == "active" }

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AdminKpiCard(strings.totalUsers, "${users.size}", AfhCyan, Modifier.weight(1f))
                                AdminKpiCard(strings.activeUsers, "$activeUsersCount", AfhTurquoise, Modifier.weight(1f))
                                AdminKpiCard(strings.totalConversations, "$totalUserConversations", AfhElectricBlue, Modifier.weight(1f))
                                AdminKpiCard(strings.apiRequests, "$totalReq", AfhSoftPurple, Modifier.weight(1f))
                                AdminKpiCard(strings.errorRate, String.format("%.1f%%", errorRatePct), AfhWarning, Modifier.weight(1f))
                            }
                        }

                        // Model Usage Breakdown
                        item {
                            AfhGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Model Usage Distribution",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val total = usageLogs.size.coerceAtLeast(1)
                                    models.forEach { model ->
                                        val count = usageLogs.count { it.afhModelId == model.id }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(model.afhModelName, style = MaterialTheme.typography.bodyMedium)
                                            Text("$count calls", style = MaterialTheme.typography.labelMedium, color = AfhCyan)
                                        }
                                        LinearProgressIndicator(
                                            progress = { count.toFloat() / total.toFloat() },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = AfhElectricBlue
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Models & Private Provider Mappings
                        items(models, key = { it.id }) { model ->
                            val mapping = mappings[model.id]
                                ?: AfhCatalogDefaults.defaultMappings[model.id]
                                ?: AdminProviderMapping(afhModelId = model.id)

                            AfhGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = model.afhModelName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Route: ${mapping.providerIdentifier} -> ${mapping.underlyingModelId}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = AfhCyan
                                            )
                                        }
                                        Switch(
                                            checked = model.enabled,
                                            onCheckedChange = { enabled ->
                                                onUpdateModel(model.copy(enabled = enabled))
                                            },
                                            modifier = Modifier.testTag("admin_toggle_${model.id}")
                                        )
                                    }
                                    Text(
                                        text = "Quotas: ${model.dailyLimit}/day • ${model.monthlyLimit}/month • Tier: ${model.tierType}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { editingMapping = mapping },
                                            modifier = Modifier.testTag("admin_edit_route_${model.id}")
                                        ) {
                                            Text("Configure Provider Route")
                                        }
                                        OutlinedButton(
                                            onClick = { editingModelLimits = model },
                                            modifier = Modifier.testTag("admin_edit_limits_${model.id}")
                                        ) {
                                            Text("Edit Limits")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Provider Adapter Connection Status
                        val displayProviders = providers.ifEmpty { AfhCatalogDefaults.defaultProviders }
                        items(displayProviders, key = { it.providerIdentifier }) { provider ->
                            AfhGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = provider.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "ID: ${provider.providerIdentifier} • Mode: ${provider.configMode} • Status: ${provider.status}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (provider.status == "connected") AfhTurquoise else AfhWarning
                                            )
                                        }
                                        Switch(
                                            checked = provider.enabled,
                                            onCheckedChange = { on ->
                                                val newStatus = if (on && provider.configMode == "firebase_app_check") "connected"
                                                else if (on) "unconfigured" else "disabled"
                                                onUpdateProvider(provider.copy(enabled = on, status = newStatus))
                                            }
                                        )
                                    }
                                    Text(
                                        text = provider.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    3 -> {
                        // User Management
                        item {
                            Text(
                                text = strings.userManagement,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(users, key = { it.userId }) { user ->
                            AfhGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(user.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "Usage: ${user.dailyRequestsUsed}/${user.dailyLimit} daily • Role: ${user.role} • Status: ${user.status}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (user.isSuspended) MaterialTheme.colorScheme.error else AfhCyan
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val nextStatus = if (user.isSuspended) "active" else "suspended"
                                                onUpdateUser(user.userId, nextStatus, user.role, user.dailyLimit, user.monthlyLimit)
                                            }
                                        ) {
                                            Text(if (user.isSuspended) "Activate" else "Suspend")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val nextRole = if (user.isAdminRole) "user" else "admin"
                                                onUpdateUser(user.userId, user.status, nextRole, user.dailyLimit, user.monthlyLimit)
                                            }
                                        ) {
                                            Text(if (user.isAdminRole) "Demote" else "Make Admin")
                                        }
                                    }
                                }
                            }
                        }

                        // Error & Request Monitoring
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.errorMonitoring,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(usageLogs.take(25)) { log ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${log.afhModelId} • ${log.status.uppercase()}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (log.status == "success") AfhTurquoise else MaterialTheme.colorScheme.error
                                        )
                                        if (!log.errorCode.isNullOrBlank()) {
                                            Text(
                                                text = log.errorCode,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${log.latencyMs} ms",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AfhCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingMapping?.let { mapping ->
        var providerId by remember(mapping) { mutableStateOf(mapping.providerIdentifier) }
        var underlyingModel by remember(mapping) { mutableStateOf(mapping.underlyingModelId) }
        var systemPrompt by remember(mapping) { mutableStateOf(mapping.systemPrompt) }

        AlertDialog(
            onDismissRequest = { editingMapping = null },
            title = { Text("Configure Route: ${mapping.afhModelId}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("google_gemini", "openai", "anthropic", "deepseek", "opensource").forEach { pid ->
                            FilterChip(
                                selected = providerId == pid,
                                onClick = { providerId = pid },
                                label = { Text(pid) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = underlyingModel,
                        onValueChange = { underlyingModel = it },
                        label = { Text("Underlying Model ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = { Text("System Prompt") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateMapping(
                            mapping.copy(
                                providerIdentifier = providerId,
                                underlyingModelId = underlyingModel.trim(),
                                systemPrompt = systemPrompt.trim()
                            )
                        )
                        editingMapping = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingMapping = null }) { Text("Cancel") }
            }
        )
    }

    editingModelLimits?.let { model ->
        var dailyStr by remember(model) { mutableStateOf(model.dailyLimit.toString()) }
        var monthlyStr by remember(model) { mutableStateOf(model.monthlyLimit.toString()) }
        var tier by remember(model) { mutableStateOf(model.tierType) }

        AlertDialog(
            onDismissRequest = { editingModelLimits = null },
            title = { Text("Edit Limits: ${model.afhModelName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("free", "limited", "pro").forEach { t ->
                            FilterChip(
                                selected = tier == t,
                                onClick = { tier = t },
                                label = { Text(t.uppercase()) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = dailyStr,
                        onValueChange = { dailyStr = it.filter(Char::isDigit) },
                        label = { Text("Daily Limit") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = monthlyStr,
                        onValueChange = { monthlyStr = it.filter(Char::isDigit) },
                        label = { Text("Monthly Limit") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateModel(
                            model.copy(
                                tierType = tier,
                                dailyLimit = dailyStr.toLongOrNull()?.coerceIn(1L, 100000L) ?: model.dailyLimit,
                                monthlyLimit = monthlyStr.toLongOrNull()?.coerceIn(1L, 1000000L) ?: model.monthlyLimit
                            )
                        )
                        editingModelLimits = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingModelLimits = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminKpiCard(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    AfhGlassCard(modifier = modifier.widthIn(min = 135.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

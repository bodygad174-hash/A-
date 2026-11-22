package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ai.AfhAspectRatioOption
import com.example.core.AfhStrings
import com.example.core.AppLanguage
import com.example.model.AfhCatalogDefaults
import com.example.model.AfhModelSpec
import com.example.model.ChatMessage
import com.example.model.PendingAttachment
import com.example.model.UiState
import com.example.ui.components.AfhGlassCard
import com.example.ui.components.AfhLogoMark
import com.example.ui.components.MarkdownMessageView
import com.example.ui.components.afhModelAccentColor
import com.example.ui.components.afhModelIcon
import com.example.ui.theme.AfhCyan
import com.example.ui.theme.AfhElectricBlue
import com.example.ui.theme.AfhSoftPurple
import com.example.ui.theme.AfhTurquoise
import com.example.viewmodel.ChatComposerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    language: AppLanguage,
    models: List<AfhModelSpec>,
    messagesState: UiState<List<ChatMessage>>,
    composerState: ChatComposerState,
    snackbarHostState: SnackbarHostState,
    onSelectModel: (AfhModelSpec) -> Unit,
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onStopGeneration: () -> Unit,
    onRegenerateResponse: (ChatMessage) -> Unit,
    onEditAndResend: (ChatMessage, String) -> Unit,
    onAddAttachment: (PendingAttachment) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onToggleVisualStudio: () -> Unit,
    onSelectAspectRatio: (AfhAspectRatioOption) -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AfhStrings.get(language)
    val isArabic = language == AppLanguage.AR
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()

    val activeModel = remember(models, composerState.selectedModelId) {
        models.firstOrNull { it.id == composerState.selectedModelId }
            ?: AfhCatalogDefaults.models.first()
    }
    val modelAccent = afhModelAccentColor(activeModel.iconKey)

    var showModelPicker by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var speakingMessageId by remember { mutableStateOf<String?>(null) }

    // Text-to-Speech engine for "Listen" action on AI messages
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
        }
        engine
    }
    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val attachment = loadImageAttachment(context, uri)
                if (attachment != null) {
                    onAddAttachment(attachment)
                } else {
                    snackbarHostState.showSnackbar("Unable to load selected image.")
                }
            }
        }
    }

    // Zero-permission SAF Document Picker
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val attachment = loadDocumentAttachment(context, uri)
                if (attachment != null) {
                    onAddAttachment(attachment)
                } else {
                    snackbarHostState.showSnackbar("File exceeds 15MB or could not be read.")
                }
            }
        }
    }

    // Speech Recognizer intent
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                val updated = if (composerState.inputText.isBlank()) spoken else "${composerState.inputText} $spoken"
                onInputTextChange(updated)
            }
        }
    }

    val launchVoiceInput = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isArabic) "ar-SA" else "en-US")
        }
        runCatching { voiceLauncher.launch(intent) }.onFailure {
            scope.launch { snackbarHostState.showSnackbar("Voice recognition unavailable on this device.") }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchVoiceInput()
        } else {
            scope.launch { snackbarHostState.showSnackbar(strings.voicePermissionRationale) }
        }
    }

    val messages = (messagesState as? UiState.Success)?.data.orEmpty()

    LaunchedEffect(messages.size, composerState.streamingContent.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Model Selector Header Strip
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = modelAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, modelAccent.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clickable { showModelPicker = true }
                        .testTag("model_selector_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = afhModelIcon(activeModel.iconKey),
                            contentDescription = activeModel.localizedName(isArabic),
                            tint = modelAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = activeModel.localizedName(isArabic),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = "Change model",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (activeModel.supportsImageGen) {
                    FilterChip(
                        selected = composerState.visualStudioMode,
                        onClick = onToggleVisualStudio,
                        label = { Text(strings.generateImageMode) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("visual_studio_toggle")
                    )
                }
            }
        }

        // Model switch compatibility / error banner
        if (composerState.modelSwitchNotice != null) {
            Surface(
                color = AfhCyan.copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = composerState.modelSwitchNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = AfhCyan,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissNotice, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (composerState.errorBanner != null) {
            Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = composerState.errorBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissNotice, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Main Chat Thread or Empty State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when {
                composerState.visualStudioMode && composerState.generatedBitmap != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AfhGlassCard(modifier = Modifier.widthIn(max = 560.dp)) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = composerState.generatedImagePrompt,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Image(
                                    bitmap = composerState.generatedBitmap.asImageBitmap(),
                                    contentDescription = "Generated visual",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                            }
                        }
                    }
                }

                messagesState is UiState.Loading -> {
                    CircularProgressIndicator(color = AfhCyan)
                }

                messagesState is UiState.Error -> {
                    Text(
                        text = messagesState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp)
                    )
                }

                messages.isEmpty() -> {
                    EmptyChatView(
                        language = language,
                        activeModel = activeModel,
                        onSelectStarter = { starter ->
                            onInputTextChange(starter)
                            onSendMessage()
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isStreamingTarget = composerState.isGenerating && msg.id == composerState.streamingMessageId
                            val renderedContent = if (isStreamingTarget) {
                                composerState.streamingContent
                            } else {
                                msg.content
                            }
                            ChatMessageBubble(
                                message = msg,
                                renderedContent = renderedContent,
                                isStreamingTarget = isStreamingTarget,
                                isSpeaking = speakingMessageId == msg.id,
                                models = models,
                                isArabic = isArabic,
                                strings = strings,
                                onCopy = { text ->
                                    clipboard.setText(AnnotatedString(text))
                                    scope.launch { snackbarHostState.showSnackbar(strings.copiedToClipboard) }
                                },
                                onEdit = { editingMessage = msg },
                                onRegenerate = { onRegenerateResponse(msg) },
                                onToggleSpeak = { text ->
                                    if (speakingMessageId == msg.id) {
                                        tts.stop()
                                        speakingMessageId = null
                                    } else if (ttsReady) {
                                        tts.language = if (isArabic) Locale("ar") else Locale.US
                                        tts.speak(text.take(3500), TextToSpeech.QUEUE_FLUSH, null, msg.id)
                                        speakingMessageId = msg.id
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (composerState.visualStudioMode && composerState.isGenerating) {
                AfhGlassCard(modifier = Modifier.padding(24.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = AfhSoftPurple)
                        Text(
                            text = if (isArabic) "جارٍ إنشاء الصورة عبر استوديو AFH (قد يستغرق ٣٠-٦٠ ثانية)..."
                            else "Synthesizing visual in AFH Creative Studio (30-60s)...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Bottom Composer Area
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 860.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Visual Studio aspect ratio selector
                if (composerState.visualStudioMode && activeModel.supportsImageGen) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AfhAspectRatioOption.entries.forEach { ratio ->
                            FilterChip(
                                selected = composerState.selectedAspectRatio == ratio,
                                onClick = { onSelectAspectRatio(ratio) },
                                label = { Text(ratio.label) }
                            )
                        }
                    }
                }

                // Pending Attachments strip
                if (composerState.attachments.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        composerState.attachments.forEach { att ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AfhElectricBlue.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, AfhCyan.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = att.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 140.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove attachment",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onRemoveAttachment(att.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Supported file types indicator for active AFH model
                val supportedText = if (activeModel.supportedFileTypes.isEmpty()) {
                    strings.noAttachmentsSupported
                } else {
                    "${strings.supportedAttachments} ${activeModel.supportedFileTypes.joinToString(", ")}"
                }
                Text(
                    text = supportedText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Composer Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (activeModel.supportsVision && !composerState.visualStudioMode) {
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("attach_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = strings.attachImage,
                                tint = AfhCyan
                            )
                        }
                    }

                    if (activeModel.supportsFiles && !composerState.visualStudioMode) {
                        IconButton(
                            onClick = {
                                docPickerLauncher.launch(
                                    arrayOf("text/*", "application/pdf", "application/json", "*/*")
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("attach_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AttachFile,
                                contentDescription = strings.attachDocument,
                                tint = AfhTurquoise
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) launchVoiceInput()
                            else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("voice_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = strings.voiceInput,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = composerState.inputText,
                        onValueChange = onInputTextChange,
                        placeholder = {
                            Text(
                                if (composerState.visualStudioMode) {
                                    if (isArabic) "صف الصورة التي تريد إنشاءها..." else "Describe the image to create..."
                                } else strings.messagePlaceholder
                            )
                        },
                        maxLines = 5,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AfhCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    if (composerState.isGenerating) {
                        Button(
                            onClick = onStopGeneration,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("stop_generation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = strings.stopGenerating
                            )
                        }
                    } else {
                        Button(
                            onClick = onSendMessage,
                            enabled = composerState.inputText.isNotBlank() || composerState.attachments.isNotEmpty(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = AfhElectricBlue),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("send_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send"
                            )
                        }
                    }
                }
            }
        }
    }

    // AFH Branded Model Selector Sheet (ONLY AFH names, icons, capabilities, limits)
    if (showModelPicker) {
        ModalBottomSheet(onDismissRequest = { showModelPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.modelSelectorTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                models.forEach { model ->
                    val accent = afhModelAccentColor(model.iconKey)
                    val isSelected = model.id == activeModel.id
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) accent.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = model.enabled) {
                                onSelectModel(model)
                                showModelPicker = false
                            }
                            .testTag("select_model_${model.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = afhModelIcon(model.iconKey),
                                contentDescription = model.localizedName(isArabic),
                                tint = if (model.enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = model.localizedName(isArabic),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val badge = if (!model.enabled) strings.unavailableBadge
                                    else when (model.tierType) {
                                        "free" -> strings.freeTierBadge
                                        "limited" -> strings.limitedTierBadge
                                        else -> strings.proTierBadge
                                    }
                                    Text(
                                        text = badge,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (model.enabled) AfhTurquoise else MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    text = model.localizedDescription(isArabic),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${model.capabilities.joinToString(" • ")} (${model.dailyLimit}/day)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Edit user message dialog
    editingMessage?.let { msg ->
        var editedText by remember(msg) { mutableStateOf(msg.content) }
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text(strings.edit) },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedText.isNotBlank()) {
                            onEditAndResend(msg, editedText.trim())
                        }
                        editingMessage = null
                    }
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) { Text(strings.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChatView(
    language: AppLanguage,
    activeModel: AfhModelSpec,
    onSelectStarter: (String) -> Unit
) {
    val strings = AfhStrings.get(language)
    val isArabic = language == AppLanguage.AR
    val starters = if (isArabic) {
        listOf(
            "اشرح الحوسبة السحابية بأسلوب مبسط",
            "اكتب دالة Kotlin لفرز قائمة بكفاءة",
            "ساعدني في صياغة خطة إطلاق منتج تقني",
            "لخّص أهم مبادئ تصميم واجهات المستخدم"
        )
    } else {
        listOf(
            "Explain quantum computing simply",
            "Write a clean Kotlin Coroutines retry helper",
            "Draft a launch strategy for a mobile SaaS product",
            "Review best practices for zero-trust security"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AfhLogoMark(size = 64.dp, animated = true)
        Text(
            text = strings.emptyChatTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${activeModel.localizedName(isArabic)} — ${activeModel.localizedDescription(isArabic)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            starters.forEach { prompt ->
                AssistChip(
                    onClick = { onSelectStarter(prompt) },
                    label = { Text(prompt, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    renderedContent: String,
    isStreamingTarget: Boolean,
    isSpeaking: Boolean,
    models: List<AfhModelSpec>,
    isArabic: Boolean,
    strings: com.example.core.AfhLocalizedStrings,
    onCopy: (String) -> Unit,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit,
    onToggleSpeak: (String) -> Unit
) {
    val isUser = message.role == "user"
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timestampText = message.createdAt?.toDate()?.let { timeFormat.format(it) }
        ?: timeFormat.format(Date())

    val modelLabel = remember(models, message.afhModelId, isArabic) {
        models.firstOrNull { it.id == message.afhModelId }?.localizedName(isArabic) ?: "AFH AI"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 20.dp
            ),
            color = if (isUser) AfhElectricBlue.copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = BorderStroke(
                1.dp,
                if (isUser) AfhCyan.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            modifier = Modifier.widthIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isUser) {
                        AfhLogoMark(size = 18.dp)
                    }
                    Text(
                        text = if (isUser) (if (isArabic) "أنت" else "You") else modelLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isUser) AfhCyan else AfhTurquoise,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• $timestampText",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (message.attachmentNames.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        message.attachmentNames.forEach { name ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (!message.errorMessage.isNullOrBlank()) {
                    Text(
                        text = message.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRegenerate) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.retry)
                    }
                } else if (isStreamingTarget && renderedContent.isBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AfhCyan)
                        Text(
                            text = if (isArabic) "يفكر..." else "Thinking...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (isUser) {
                    Text(
                        text = renderedContent,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    MarkdownMessageView(
                        content = renderedContent,
                        onCopied = { onCopy(renderedContent) }
                    )
                }

                // Message Action Bar
                if (!isStreamingTarget && renderedContent.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onCopy(renderedContent) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = strings.copy,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isUser) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = strings.edit,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onRegenerate,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = strings.regenerate,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onToggleSpeak(renderedContent) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = if (isSpeaking) strings.stopSpeaking else strings.readAloud,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSpeaking) AfhCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun loadImageAttachment(context: Context, uri: Uri): PendingAttachment? =
    withContext(Dispatchers.IO) {
        try {
            val name = queryFileName(context, uri) ?: "image.jpg"
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
            val scaled = scaleBitmapIfNeeded(decoded, maxDimension = 1600)
            PendingAttachment(
                id = UUID.randomUUID().toString(),
                name = name,
                mimeType = mime,
                isImage = true,
                bitmap = scaled
            )
        } catch (_: Exception) {
            null
        }
    }

private suspend fun loadDocumentAttachment(context: Context, uri: Uri): PendingAttachment? =
    withContext(Dispatchers.IO) {
        try {
            val name = queryFileName(context, uri) ?: "document.txt"
            val mime = context.contentResolver.getType(uri) ?: "text/plain"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            if (bytes.size > 15 * 1024 * 1024) return@withContext null
            if (mime == "application/pdf" || name.endsWith(".pdf", ignoreCase = true)) {
                PendingAttachment(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    mimeType = "application/pdf",
                    isImage = false,
                    rawBytes = bytes
                )
            } else {
                val text = bytes.toString(Charsets.UTF_8)
                PendingAttachment(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    mimeType = mime,
                    isImage = false,
                    textContent = text
                )
            }
        } catch (_: Exception) {
            null
        }
    }

private fun queryFileName(context: Context, uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) return cursor.getString(idx)
        }
    }
    return uri.lastPathSegment
}

private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDimension && h <= maxDimension) return bitmap
    val ratio = maxDimension.toFloat() / maxOf(w, h).toFloat()
    val newW = (w * ratio).toInt().coerceAtLeast(1)
    val newH = (h * ratio).toInt().coerceAtLeast(1)
    val resized = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    // Compress slightly via JPEG roundtrip only if extremely large raw bitmap
    if (resized.byteCount > 16 * 1024 * 1024) {
        val baos = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val compressed = baos.toByteArray()
        return BitmapFactory.decodeByteArray(compressed, 0, compressed.size) ?: resized
    }
    return resized
}

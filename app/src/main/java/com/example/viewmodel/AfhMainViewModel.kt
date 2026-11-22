package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.AfhAspectRatioOption
import com.example.ai.AiProviderGateway
import com.example.ai.AiStreamChunk
import com.example.data.AfhFirestoreRepository
import com.example.model.AdminProviderMapping
import com.example.model.AfhCatalogDefaults
import com.example.model.AfhModelSpec
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.PendingAttachment
import com.example.model.ProviderSetting
import com.example.model.UiState
import com.example.model.UsageLog
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatComposerState(
    val inputText: String = "",
    val selectedModelId: String = "afh_flash",
    val activeConversationId: String? = null,
    val isGenerating: Boolean = false,
    val streamingMessageId: String? = null,
    val streamingContent: String = "",
    val modelSwitchNotice: String? = null,
    val errorBanner: String? = null,
    val attachments: List<PendingAttachment> = emptyList(),
    val visualStudioMode: Boolean = false,
    val selectedAspectRatio: AfhAspectRatioOption = AfhAspectRatioOption.SQUARE_1_1,
    val generatedBitmap: Bitmap? = null,
    val generatedImagePrompt: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class AfhMainViewModel(
    private val repository: AfhFirestoreRepository,
    private val aiGateway: AiProviderGateway,
    private val currentUserId: String
) : ViewModel() {

    private var activeGenerationJob: Job? = null

    private val _composerState = MutableStateFlow(ChatComposerState())
    val composerState: StateFlow<ChatComposerState> = _composerState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    private val _adminFeedback = MutableStateFlow<String?>(null)
    val adminFeedback: StateFlow<String?> = _adminFeedback.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            if (profile != null) UiState.Success(profile) else UiState.Loading
        }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val modelsCatalog: StateFlow<List<AfhModelSpec>> = repository.observePublicModels()
        .catch { error ->
            if (error is CancellationException) throw error
            emit(AfhCatalogDefaults.models)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AfhCatalogDefaults.models
        )

    val conversationsState: StateFlow<UiState<List<Conversation>>> = repository.observeConversations(currentUserId)
        .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).message ?: "Failed to load conversations"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val activeConvIdFlow = _composerState.map { it.activeConversationId }

    val messagesState: StateFlow<UiState<List<ChatMessage>>> = activeConvIdFlow
        .flatMapLatest { convId ->
            if (convId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeMessages(currentUserId, convId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val fsError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (fsError == null && error is CancellationException) throw error
                        emit(UiState.Error((fsError ?: error).message ?: "Failed to load messages"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Success(emptyList())
        )

    // Admin streams (active only if user is admin)
    val isAdminUser: Boolean
        get() {
            val profile = (userProfileState.value as? UiState.Success)?.data
            return profile?.isAdminRole == true || repository.isBootstrappedAdmin(Firebase.auth.currentUser)
        }

    val adminUsersState: StateFlow<List<UserProfile>> = userProfileState
        .flatMapLatest { state ->
            val profile = (state as? UiState.Success)?.data
            val allowed = profile?.isAdminRole == true || repository.isBootstrappedAdmin(Firebase.auth.currentUser)
            if (allowed) repository.observeAllUsersAdmin().catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminMappingsState: StateFlow<Map<String, AdminProviderMapping>> = userProfileState
        .flatMapLatest { state ->
            val profile = (state as? UiState.Success)?.data
            val allowed = profile?.isAdminRole == true || repository.isBootstrappedAdmin(Firebase.auth.currentUser)
            if (allowed) {
                repository.observeAdminMappings()
                    .map { list -> list.associateBy { it.afhModelId } }
                    .catch { emit(AfhCatalogDefaults.defaultMappings) }
            } else flowOf(emptyMap())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val providerSettingsState: StateFlow<List<ProviderSetting>> = userProfileState
        .flatMapLatest { state ->
            val profile = (state as? UiState.Success)?.data
            val allowed = profile?.isAdminRole == true || repository.isBootstrappedAdmin(Firebase.auth.currentUser)
            if (allowed) repository.observeProviderSettings().catch { emit(AfhCatalogDefaults.defaultProviders) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val usageLogsState: StateFlow<List<UsageLog>> = userProfileState
        .flatMapLatest { state ->
            val profile = (state as? UiState.Success)?.data
            val allowed = profile?.isAdminRole == true || repository.isBootstrappedAdmin(Firebase.auth.currentUser)
            repository.observeUsageLogs(currentUserId, allowed).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            if (user != null) {
                repository.ensureUserProfile(user).onSuccess { profile ->
                    _composerState.update {
                        if (it.activeConversationId == null) {
                            it.copy(selectedModelId = profile.defaultAfhModelId.ifBlank { "afh_flash" })
                        } else it
                    }
                    if (profile.isAdminRole || repository.isBootstrappedAdmin(user)) {
                        repository.ensureAdminSeedData()
                    }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesFilter() {
        _favoritesOnly.value = !_favoritesOnly.value
    }

    fun updateInputText(text: String) {
        _composerState.update { it.copy(inputText = text) }
    }

    fun dismissNotice() {
        _composerState.update { it.copy(modelSwitchNotice = null, errorBanner = null) }
    }

    fun dismissAdminFeedback() {
        _adminFeedback.value = null
    }

    fun startNewChat() {
        stopGeneration()
        val defaultModel = (userProfileState.value as? UiState.Success)?.data?.defaultAfhModelId ?: "afh_flash"
        _composerState.update {
            ChatComposerState(
                selectedModelId = defaultModel,
                activeConversationId = null
            )
        }
    }

    fun openConversation(conversation: Conversation) {
        stopGeneration()
        _composerState.update {
            it.copy(
                activeConversationId = conversation.id,
                selectedModelId = conversation.selectedAfhModelId.ifBlank { "afh_flash" },
                errorBanner = null,
                modelSwitchNotice = null,
                generatedBitmap = null
            )
        }
    }

    fun selectModel(model: AfhModelSpec, isArabic: Boolean) {
        if (!model.enabled) return
        val prevId = _composerState.value.selectedModelId
        if (prevId == model.id) return

        val prevModel = modelsCatalog.value.firstOrNull { it.id == prevId }
        val activeConvId = _composerState.value.activeConversationId
        val compatibilityNote = if (activeConvId != null && prevModel != null && prevModel.supportsVision && !model.supportsVision) {
            if (isArabic) {
                "تم التبديل إلى ${model.localizedName(true)}. سياق النصوص محفوظ، لكن الصور السابقة لا تُعاد معالجتها بصرياً."
            } else {
                "Switched to ${model.localizedName(false)}. Text context preserved; prior images require a Vision model."
            }
        } else if (activeConvId != null) {
            if (isArabic) {
                "تم التبديل إلى ${model.localizedName(true)} مع الحفاظ على سياق المحادثة."
            } else {
                "Switched to ${model.localizedName(false)}. Conversation context preserved."
            }
        } else null

        // Drop incompatible attachments if new model doesn't support them
        val filteredAttachments = _composerState.value.attachments.filter { att ->
            (att.isImage && model.supportsVision) || (!att.isImage && model.supportsFiles)
        }

        _composerState.update {
            it.copy(
                selectedModelId = model.id,
                attachments = filteredAttachments,
                visualStudioMode = if (model.supportsImageGen) it.visualStudioMode else false,
                modelSwitchNotice = compatibilityNote
            )
        }

        if (activeConvId != null) {
            viewModelScope.launch {
                repository.updateConversation(
                    userId = currentUserId,
                    conversationId = activeConvId,
                    selectedAfhModelId = model.id
                )
            }
        }
    }

    fun toggleVisualStudioMode() {
        _composerState.update {
            it.copy(
                visualStudioMode = !it.visualStudioMode,
                errorBanner = null
            )
        }
    }

    fun selectAspectRatio(ratio: AfhAspectRatioOption) {
        _composerState.update { it.copy(selectedAspectRatio = ratio) }
    }

    fun addAttachment(attachment: PendingAttachment) {
        _composerState.update { state ->
            if (state.attachments.size >= 5) state
            else state.copy(attachments = state.attachments + attachment)
        }
    }

    fun removeAttachment(id: String) {
        _composerState.update { state ->
            state.copy(attachments = state.attachments.filterNot { it.id == id })
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _composerState.update {
            it.copy(
                isGenerating = false,
                streamingMessageId = null,
                streamingContent = ""
            )
        }
    }

    fun sendMessage(overridePrompt: String? = null) {
        val state = _composerState.value
        val prompt = (overridePrompt ?: state.inputText).trim()
        val attachments = state.attachments
        if (prompt.isBlank() && attachments.isEmpty()) return
        if (state.isGenerating) return

        val profile = (userProfileState.value as? UiState.Success)?.data
        if (profile?.isSuspended == true) {
            _composerState.update { it.copy(errorBanner = "Your account has been suspended.") }
            return
        }

        val activeModel = modelsCatalog.value.firstOrNull { it.id == state.selectedModelId }
            ?: AfhCatalogDefaults.models.first()

        if (!activeModel.enabled) {
            _composerState.update { it.copy(errorBanner = "${activeModel.afhModelName} is currently disabled by an administrator.") }
            return
        }

        if (profile != null && (profile.dailyRequestsUsed >= profile.dailyLimit || profile.monthlyRequestsUsed >= profile.monthlyLimit)) {
            _composerState.update {
                it.copy(errorBanner = "Usage quota reached (${profile.dailyRequestsUsed}/${profile.dailyLimit} daily). Wait for reset or contact admin.")
            }
            viewModelScope.launch {
                repository.logUsage(currentUserId, activeModel.id, "quota_exceeded", 0L, "QUOTA_EXCEEDED")
            }
            return
        }

        // Visual Studio image generation branch
        if (state.visualStudioMode && activeModel.supportsImageGen) {
            generateCreativeImage(prompt, state.selectedAspectRatio, activeModel.id)
            return
        }

        val streamingEnabled = profile?.streamingEnabled ?: true
        val historyMessages = (messagesState.value as? UiState.Success)?.data.orEmpty()

        activeGenerationJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _composerState.update {
                it.copy(
                    inputText = if (overridePrompt == null) "" else it.inputText,
                    attachments = emptyList(),
                    isGenerating = true,
                    streamingContent = "",
                    errorBanner = null,
                    generatedBitmap = null
                )
            }

            try {
                val convId = state.activeConversationId ?: run {
                    val title = prompt.ifBlank { attachments.firstOrNull()?.name ?: "New Chat" }
                        .replace("\n", " ")
                        .take(48)
                    val created = repository.createConversation(
                        userId = currentUserId,
                        title = title,
                        selectedAfhModelId = activeModel.id
                    ).getOrThrow()
                    _composerState.update { it.copy(activeConversationId = created) }
                    created
                }

                // Persist user message
                repository.addMessage(
                    userId = currentUserId,
                    conversationId = convId,
                    role = "user",
                    content = prompt.ifBlank { "[Attached ${attachments.size} file(s)]" },
                    afhModelId = activeModel.id,
                    attachmentNames = attachments.map { it.name },
                    attachmentTypes = attachments.map { it.mimeType }
                ).getOrThrow()

                // Create placeholder assistant message
                val assistantMsgId = repository.addMessage(
                    userId = currentUserId,
                    conversationId = convId,
                    role = "assistant",
                    content = "",
                    afhModelId = activeModel.id
                ).getOrThrow()

                _composerState.update { it.copy(streamingMessageId = assistantMsgId) }

                val mapping = aiGateway.resolveMapping(activeModel.id, adminMappingsState.value)
                val adapter = aiGateway.resolveAdapter(mapping.providerIdentifier)

                var finalResponse = ""
                var latency = 0L

                adapter.streamResponse(
                    mapping = mapping,
                    history = historyMessages,
                    userPrompt = prompt.ifBlank { "Analyze the attached content carefully." },
                    attachments = attachments,
                    streamingEnabled = streamingEnabled
                ).collect { chunk ->
                    when (chunk) {
                        is AiStreamChunk.Delta -> {
                            _composerState.update { it.copy(streamingContent = chunk.accumulatedText) }
                        }
                        is AiStreamChunk.Completed -> {
                            finalResponse = chunk.finalText
                            latency = chunk.latencyMs
                        }
                    }
                }

                repository.updateMessage(
                    userId = currentUserId,
                    conversationId = convId,
                    messageId = assistantMsgId,
                    content = finalResponse,
                    afhModelId = activeModel.id
                )

                repository.updateConversation(
                    userId = currentUserId,
                    conversationId = convId,
                    lastMessagePreview = finalResponse.replace("\n", " ").take(120),
                    incrementMessageCountBy = 2L
                )

                repository.incrementUsageCounters(currentUserId)
                repository.logUsage(currentUserId, activeModel.id, "success", latency)

                _composerState.update {
                    it.copy(
                        isGenerating = false,
                        streamingMessageId = null,
                        streamingContent = ""
                    )
                }
            } catch (e: CancellationException) {
                // Save partial content if user stopped streaming mid-way
                val partial = _composerState.value.streamingContent
                val msgId = _composerState.value.streamingMessageId
                val convId = _composerState.value.activeConversationId
                if (partial.isNotBlank() && !msgId.isNullOrBlank() && !convId.isNullOrBlank()) {
                    viewModelScope.launch {
                        repository.updateMessage(currentUserId, convId, msgId, partial, activeModel.id)
                    }
                }
                _composerState.update {
                    it.copy(isGenerating = false, streamingMessageId = null, streamingContent = "")
                }
                throw e
            } catch (e: Exception) {
                Log.e("AfhMainVM", "Failed generating AI response", e)
                val errMsg = e.localizedMessage ?: "Failed to generate response."
                val msgId = _composerState.value.streamingMessageId
                val convId = _composerState.value.activeConversationId
                if (!msgId.isNullOrBlank() && !convId.isNullOrBlank()) {
                    repository.updateMessage(
                        userId = currentUserId,
                        conversationId = convId,
                        messageId = msgId,
                        content = "",
                        afhModelId = activeModel.id,
                        errorMessage = errMsg
                    )
                }
                repository.logUsage(
                    currentUserId,
                    activeModel.id,
                    "error",
                    System.currentTimeMillis() - startTime,
                    errMsg.take(100)
                )
                _composerState.update {
                    it.copy(
                        isGenerating = false,
                        streamingMessageId = null,
                        streamingContent = "",
                        errorBanner = errMsg
                    )
                }
            }
        }
    }

    private fun generateCreativeImage(
        prompt: String,
        aspectRatio: AfhAspectRatioOption,
        modelId: String
    ) {
        activeGenerationJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _composerState.update {
                it.copy(
                    inputText = "",
                    isGenerating = true,
                    errorBanner = null,
                    generatedBitmap = null,
                    generatedImagePrompt = prompt
                )
            }
            try {
                val bitmap = aiGateway.generateVisual(prompt, aspectRatio)
                repository.incrementUsageCounters(currentUserId)
                repository.logUsage(currentUserId, modelId, "success", System.currentTimeMillis() - startTime)
                _composerState.update {
                    it.copy(
                        isGenerating = false,
                        generatedBitmap = bitmap
                    )
                }
            } catch (e: CancellationException) {
                _composerState.update { it.copy(isGenerating = false) }
                throw e
            } catch (e: Exception) {
                Log.e("AfhMainVM", "Visual generation failed", e)
                val err = e.localizedMessage ?: "Visual generation failed."
                repository.logUsage(currentUserId, modelId, "error", System.currentTimeMillis() - startTime, err.take(100))
                _composerState.update {
                    it.copy(isGenerating = false, errorBanner = err)
                }
            }
        }
    }

    fun regenerateLastResponse(assistantMessage: ChatMessage) {
        val messages = (messagesState.value as? UiState.Success)?.data.orEmpty()
        val idx = messages.indexOfFirst { it.id == assistantMessage.id }
        if (idx <= 0) return
        val precedingUser = messages.subList(0, idx).lastOrNull { it.role == "user" } ?: return
        val convId = _composerState.value.activeConversationId ?: return
        val toDelete = messages.drop(idx).map { it.id }

        viewModelScope.launch {
            repository.deleteMessagesAfter(currentUserId, convId, toDelete)
            sendMessage(overridePrompt = precedingUser.content)
        }
    }

    fun editAndResendMessage(userMessage: ChatMessage, newContent: String) {
        val convId = _composerState.value.activeConversationId ?: return
        val messages = (messagesState.value as? UiState.Success)?.data.orEmpty()
        val idx = messages.indexOfFirst { it.id == userMessage.id }
        if (idx < 0) return
        val toDelete = messages.drop(idx).map { it.id }

        viewModelScope.launch {
            repository.deleteMessagesAfter(currentUserId, convId, toDelete)
            sendMessage(overridePrompt = newContent)
        }
    }

    fun renameConversation(conversationId: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateConversation(currentUserId, conversationId, title = newTitle)
        }
    }

    fun toggleFavoriteConversation(conversation: Conversation) {
        viewModelScope.launch {
            repository.updateConversation(
                currentUserId,
                conversation.id,
                favorite = !conversation.favorite
            )
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(currentUserId, conversationId)
            if (_composerState.value.activeConversationId == conversationId) {
                _composerState.update { it.copy(activeConversationId = null) }
            }
        }
    }

    // --- SETTINGS ACTIONS ---
    fun updatePreferences(
        name: String? = null,
        language: String? = null,
        theme: String? = null,
        defaultModelId: String? = null,
        streamingEnabled: Boolean? = null
    ) {
        viewModelScope.launch {
            repository.updateUserPreferences(
                userId = currentUserId,
                name = name,
                preferredLanguage = language,
                preferredTheme = theme,
                defaultAfhModelId = defaultModelId,
                streamingEnabled = streamingEnabled
            )
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            repository.deleteAllConversations(currentUserId)
            _composerState.update { it.copy(activeConversationId = null) }
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAccountData(currentUserId)
            onComplete()
        }
    }

    // --- ADMIN ACTIONS ---
    fun adminUpdateModel(model: AfhModelSpec) {
        viewModelScope.launch {
            repository.adminSaveModelSpec(model)
                .onSuccess { _adminFeedback.value = "Updated ${model.afhModelName}" }
                .onFailure { _adminFeedback.value = it.localizedMessage }
        }
    }

    fun adminUpdateMapping(mapping: AdminProviderMapping) {
        viewModelScope.launch {
            repository.adminSaveProviderMapping(mapping)
                .onSuccess { _adminFeedback.value = "Saved routing for ${mapping.afhModelId}" }
                .onFailure { _adminFeedback.value = it.localizedMessage }
        }
    }

    fun adminUpdateProvider(setting: ProviderSetting) {
        viewModelScope.launch {
            repository.adminSaveProviderSetting(setting)
                .onSuccess { _adminFeedback.value = "Updated ${setting.displayName}" }
                .onFailure { _adminFeedback.value = it.localizedMessage }
        }
    }

    fun adminUpdateUser(
        targetUserId: String,
        status: String,
        role: String,
        dailyLimit: Long,
        monthlyLimit: Long
    ) {
        viewModelScope.launch {
            repository.adminUpdateUserAccount(targetUserId, status, role, dailyLimit, monthlyLimit)
                .onSuccess { _adminFeedback.value = "Updated user account" }
                .onFailure { _adminFeedback.value = it.localizedMessage }
        }
    }

    companion object {
        fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val databaseId = app.getString(com.example.R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AfhMainViewModel(
                    repository = AfhFirestoreRepository(db),
                    aiGateway = AiProviderGateway(),
                    currentUserId = currentUserId
                )
            }
        }
    }
}

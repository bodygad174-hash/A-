package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.AfhStrings
import com.example.core.AppLanguage
import com.example.core.configureAppCheck
import com.example.data.AuthManager
import com.example.model.UiState
import com.example.ui.components.AfhDestination
import com.example.ui.components.AfhHorizontalLogo
import com.example.ui.components.AfhSidebarContent
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeShowcaseScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AfhAiTheme
import com.example.viewmodel.AfhMainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent BEFORE any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AfhAiRoot()
        }
    }
}

@Composable
fun AfhAiRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var guestLanguage by remember { mutableStateOf(AppLanguage.EN) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = { }
            )
        }
    }

    val user = currentUser
    if (user == null) {
        val layoutDir = if (guestLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
            AfhAiTheme(themeMode = "dark", isArabic = guestLanguage == AppLanguage.AR) {
                AuthScreen(
                    language = guestLanguage,
                    onLanguageChange = { guestLanguage = it },
                    onAuthSuccess = { currentUser = Firebase.auth.currentUser }
                )
            }
        }
    } else {
        key(user.uid) {
            AuthenticatedAfhApp(
                currentUserId = user.uid,
                onSignOut = {
                    AuthManager.signOut(credentialManager, scope) {
                        currentUser = null
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedAfhApp(
    currentUserId: String,
    onSignOut: () -> Unit
) {
    val viewModel: AfhMainViewModel = viewModel(
        key = currentUserId,
        factory = AfhMainViewModel.provideFactory(currentUserId)
    )

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val profile = (profileState as? UiState.Success)?.data
    val language = AppLanguage.fromCode(profile?.preferredLanguage)
    val themeMode = profile?.preferredTheme ?: "dark"
    val strings = AfhStrings.get(language)
    val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val models by viewModel.modelsCatalog.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val composerState by viewModel.composerState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val favoritesOnly by viewModel.favoritesOnly.collectAsStateWithLifecycle()

    val adminUsers by viewModel.adminUsersState.collectAsStateWithLifecycle()
    val adminMappings by viewModel.adminMappingsState.collectAsStateWithLifecycle()
    val providerSettings by viewModel.providerSettingsState.collectAsStateWithLifecycle()
    val usageLogs by viewModel.usageLogsState.collectAsStateWithLifecycle()
    val adminFeedback by viewModel.adminFeedback.collectAsStateWithLifecycle()

    val isAdmin = viewModel.isAdminUser
    var currentDestination by remember { mutableStateOf(AfhDestination.CHAT) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (currentDestination != AfhDestination.CHAT) {
        BackHandler { currentDestination = AfhDestination.CHAT }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        AfhAiTheme(themeMode = themeMode, isArabic = language == AppLanguage.AR) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideLayout = maxWidth >= 840.dp

                val sidebarBlock: @Composable (Modifier) -> Unit = { mod ->
                    AfhSidebarContent(
                        language = language,
                        currentDestination = currentDestination,
                        activeConversationId = composerState.activeConversationId,
                        conversations = conversations,
                        searchQuery = searchQuery,
                        favoritesOnly = favoritesOnly,
                        profile = profile,
                        isAdmin = isAdmin,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onToggleFavoritesFilter = viewModel::toggleFavoritesFilter,
                        onNewChat = {
                            viewModel.startNewChat()
                            currentDestination = AfhDestination.CHAT
                            scope.launch { drawerState.close() }
                        },
                        onSelectConversation = { conv ->
                            viewModel.openConversation(conv)
                            currentDestination = AfhDestination.CHAT
                            scope.launch { drawerState.close() }
                        },
                        onRenameConversation = viewModel::renameConversation,
                        onToggleFavorite = viewModel::toggleFavoriteConversation,
                        onDeleteConversation = viewModel::deleteConversation,
                        onNavigate = { dest ->
                            currentDestination = dest
                            scope.launch { drawerState.close() }
                        },
                        modifier = mod
                    )
                }

                val mainScaffold: @Composable () -> Unit = {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    AfhHorizontalLogo(iconSize = 30.dp)
                                },
                                navigationIcon = {
                                    if (!isWideLayout) {
                                        IconButton(
                                            onClick = { scope.launch { drawerState.open() } },
                                            modifier = Modifier.testTag("open_sidebar_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Menu,
                                                contentDescription = "Open menu"
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            viewModel.startNewChat()
                                            currentDestination = AfhDestination.CHAT
                                        },
                                        modifier = Modifier.testTag("topbar_new_chat_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = strings.newChat
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                )
                            )
                        }
                    ) { innerPadding ->
                        val contentModifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)

                        when (currentDestination) {
                            AfhDestination.CHAT -> ChatScreen(
                                language = language,
                                models = models,
                                messagesState = messagesState,
                                composerState = composerState,
                                snackbarHostState = snackbarHostState,
                                onSelectModel = { model -> viewModel.selectModel(model, language == AppLanguage.AR) },
                                onInputTextChange = viewModel::updateInputText,
                                onSendMessage = { viewModel.sendMessage() },
                                onStopGeneration = viewModel::stopGeneration,
                                onRegenerateResponse = viewModel::regenerateLastResponse,
                                onEditAndResend = viewModel::editAndResendMessage,
                                onAddAttachment = viewModel::addAttachment,
                                onRemoveAttachment = viewModel::removeAttachment,
                                onToggleVisualStudio = viewModel::toggleVisualStudioMode,
                                onSelectAspectRatio = viewModel::selectAspectRatio,
                                onDismissNotice = viewModel::dismissNotice,
                                modifier = contentModifier
                            )

                            AfhDestination.EXPLORE -> HomeShowcaseScreen(
                                language = language,
                                models = models,
                                onStartChatting = {
                                    viewModel.startNewChat()
                                    currentDestination = AfhDestination.CHAT
                                },
                                onSelectModelAndChat = { model ->
                                    viewModel.startNewChat()
                                    viewModel.selectModel(model, language == AppLanguage.AR)
                                    currentDestination = AfhDestination.CHAT
                                },
                                modifier = contentModifier
                            )

                            AfhDestination.SETTINGS -> SettingsScreen(
                                profile = profile,
                                models = models,
                                language = language,
                                themeMode = themeMode,
                                onUpdateName = { viewModel.updatePreferences(name = it) },
                                onUpdateLanguage = { lang -> viewModel.updatePreferences(language = lang.code) },
                                onUpdateTheme = { theme -> viewModel.updatePreferences(theme = theme) },
                                onUpdateDefaultModel = { modelId -> viewModel.updatePreferences(defaultModelId = modelId) },
                                onToggleStreaming = { enabled -> viewModel.updatePreferences(streamingEnabled = enabled) },
                                onClearAllConversations = {
                                    viewModel.clearAllConversations()
                                    scope.launch { snackbarHostState.showSnackbar("All conversations cleared.") }
                                },
                                onDeleteAccount = {
                                    viewModel.deleteAccount(onComplete = onSignOut)
                                },
                                onSignOut = onSignOut,
                                modifier = contentModifier
                            )

                            AfhDestination.ADMIN -> if (isAdmin) {
                                AdminDashboardScreen(
                                    language = language,
                                    users = adminUsers,
                                    models = models,
                                    mappings = adminMappings,
                                    providers = providerSettings,
                                    usageLogs = usageLogs,
                                    totalUserConversations = conversations.size,
                                    feedbackMessage = adminFeedback,
                                    onDismissFeedback = viewModel::dismissAdminFeedback,
                                    onUpdateModel = viewModel::adminUpdateModel,
                                    onUpdateMapping = viewModel::adminUpdateMapping,
                                    onUpdateProvider = viewModel::adminUpdateProvider,
                                    onUpdateUser = viewModel::adminUpdateUser,
                                    modifier = contentModifier
                                )
                            } else {
                                currentDestination = AfhDestination.CHAT
                            }
                        }
                    }
                }

                if (isWideLayout) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        sidebarBlock(Modifier.width(300.dp))
                        mainScaffold()
                    }
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                                sidebarBlock(Modifier.fillMaxSize())
                            }
                        },
                        content = mainScaffold
                    )
                }
            }
        }
    }
}

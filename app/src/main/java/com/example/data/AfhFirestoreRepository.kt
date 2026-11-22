package com.example.data

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.AdminProviderMapping
import com.example.model.AfhCatalogDefaults
import com.example.model.AfhModelSpec
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.ProviderSetting
import com.example.model.UsageLog
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AfhFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isBootstrappedAdmin(user: FirebaseUser?): Boolean {
        return user?.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true) && user?.isEmailVerified == true
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<UserProfile> {
        val docPath = "users/${user.uid}"
        return try {
            val docRef = db.collection("users").document(user.uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(userId = user.uid)
                Result.success(existing)
            } else {
                val initialRole = if (isBootstrappedAdmin(user)) "admin" else "user"
                val displayName = (user.displayName?.takeIf { it.isNotBlank() } ?: "AFH User").take(80)
                val email = (user.email ?: "").take(120)
                val avatar = user.photoUrl?.toString()?.take(500)

                val payload = mapOf(
                    "userId" to user.uid,
                    "name" to displayName,
                    "email" to email,
                    "avatarUrl" to avatar,
                    "role" to initialRole,
                    "status" to "active",
                    "dailyRequestsUsed" to 0L,
                    "monthlyRequestsUsed" to 0L,
                    "dailyLimit" to 50L,
                    "monthlyLimit" to 1000L,
                    "preferredLanguage" to "en",
                    "preferredTheme" to "dark",
                    "defaultAfhModelId" to "afh_flash",
                    "streamingEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>

                docRef.set(payload).await()
                val created = UserProfile(
                    userId = user.uid,
                    name = displayName,
                    email = email,
                    avatarUrl = avatar,
                    role = initialRole,
                    status = "active"
                )
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user profile observation" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateUserPreferences(
        userId: String,
        name: String? = null,
        preferredLanguage: String? = null,
        preferredTheme: String? = null,
        defaultAfhModelId: String? = null,
        streamingEnabled: Boolean? = null
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            name?.takeIf { it.isNotBlank() }?.let { updates["name"] = it.take(80) }
            preferredLanguage?.let { updates["preferredLanguage"] = it }
            preferredTheme?.let { updates["preferredTheme"] = it }
            defaultAfhModelId?.let { updates["defaultAfhModelId"] = it.take(64) }
            streamingEnabled?.let { updates["streamingEnabled"] = it }
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementUsageCounters(userId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            db.collection("users").document(uid).update(
                mapOf(
                    "dailyRequestsUsed" to FieldValue.increment(1L),
                    "monthlyRequestsUsed" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/conversations"
        emitAll(
            db.collection("users").document(uid).collection("conversations")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createConversation(
        userId: String,
        title: String,
        selectedAfhModelId: String
    ): Result<String> {
        val path = "users/$userId/conversations"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid).collection("conversations").document()
            val payload = mapOf(
                "userId" to uid,
                "title" to title.ifBlank { "New Chat" }.take(120),
                "selectedAfhModelId" to selectedAfhModelId.take(64),
                "favorite" to false,
                "lastMessagePreview" to "",
                "messageCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateConversation(
        userId: String,
        conversationId: String,
        title: String? = null,
        selectedAfhModelId: String? = null,
        favorite: Boolean? = null,
        lastMessagePreview: String? = null,
        incrementMessageCountBy: Long? = null
    ): Result<Unit> {
        val path = "users/$userId/conversations/$conversationId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(conversationId.isNotBlank())
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            title?.takeIf { it.isNotBlank() }?.let { updates["title"] = it.take(120) }
            selectedAfhModelId?.let { updates["selectedAfhModelId"] = it.take(64) }
            favorite?.let { updates["favorite"] = it }
            lastMessagePreview?.let { updates["lastMessagePreview"] = it.take(240) }
            incrementMessageCountBy?.let { updates["messageCount"] = FieldValue.increment(it) }

            db.collection("users").document(uid)
                .collection("conversations").document(conversationId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(userId: String, conversationId: String): Result<Unit> {
        val path = "users/$userId/conversations/$conversationId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val convRef = db.collection("users").document(uid)
                .collection("conversations").document(conversationId)
            val msgs = convRef.collection("messages").get().await()
            val batch = db.batch()
            for (doc in msgs.documents) {
                batch.delete(doc.reference)
            }
            batch.delete(convRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAllConversations(userId: String): Result<Unit> {
        val path = "users/$userId/conversations"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val convs = db.collection("users").document(uid)
                .collection("conversations").get().await()
            for (conv in convs.documents) {
                deleteConversation(uid, conv.id).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccountData(userId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            deleteAllConversations(uid).getOrThrow()
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeMessages(userId: String, conversationId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/conversations/$conversationId/messages"
        emitAll(
            db.collection("users").document(uid)
                .collection("conversations").document(conversationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addMessage(
        userId: String,
        conversationId: String,
        role: String,
        content: String,
        afhModelId: String,
        attachmentNames: List<String> = emptyList(),
        attachmentTypes: List<String> = emptyList(),
        errorMessage: String? = null
    ): Result<String> {
        val path = "users/$userId/conversations/$conversationId/messages"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("users").document(uid)
                .collection("conversations").document(conversationId)
                .collection("messages").document()
            val payload = mapOf(
                "userId" to uid,
                "conversationId" to conversationId,
                "role" to role,
                "content" to content.take(32000),
                "afhModelId" to afhModelId.take(64),
                "attachmentNames" to attachmentNames.take(5),
                "attachmentTypes" to attachmentTypes.take(5),
                "errorMessage" to errorMessage?.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMessage(
        userId: String,
        conversationId: String,
        messageId: String,
        content: String,
        afhModelId: String,
        errorMessage: String? = null
    ): Result<Unit> {
        val path = "users/$userId/conversations/$conversationId/messages/$messageId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val updates = mutableMapOf<String, Any>(
                "content" to content.take(32000),
                "afhModelId" to afhModelId.take(64),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (errorMessage != null) {
                updates["errorMessage"] = errorMessage.take(500)
            }
            db.collection("users").document(uid)
                .collection("conversations").document(conversationId)
                .collection("messages").document(messageId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessagesAfter(
        userId: String,
        conversationId: String,
        targetMessageIds: List<String>
    ): Result<Unit> {
        val path = "users/$userId/conversations/$conversationId/messages"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val colRef = db.collection("users").document(uid)
                .collection("conversations").document(conversationId)
                .collection("messages")
            val batch = db.batch()
            targetMessageIds.forEach { msgId ->
                if (msgId.isNotBlank()) {
                    batch.delete(colRef.document(msgId))
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- PUBLIC MODEL CATALOG ---
    fun observePublicModels(): Flow<List<AfhModelSpec>> = flow {
        requireUserId()
        val path = "ai_models"
        emitAll(
            db.collection("ai_models")
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    val list = snapshot.toObjects(
                        AfhModelSpec::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    list.ifEmpty { AfhCatalogDefaults.models }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    handleFirestoreError(fsError ?: error, OperationType.LIST, path)
                    emit(AfhCatalogDefaults.models)
                }
        )
    }

    suspend fun logUsage(
        userId: String,
        afhModelId: String,
        status: String,
        latencyMs: Long,
        errorCode: String? = null
    ): Result<Unit> {
        val path = "usage_logs"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = db.collection("usage_logs").document()
            val payload = mapOf(
                "userId" to uid,
                "afhModelId" to afhModelId.take(64),
                "status" to status,
                "latencyMs" to latencyMs.coerceIn(0L, 300000L),
                "errorCode" to errorCode?.take(120),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN OPERATIONS (Strictly guarded by Firestore rules & role) ---
    suspend fun ensureAdminSeedData(): Result<Unit> {
        return try {
            val existingModels = db.collection("ai_models").get().await()
            if (existingModels.isEmpty) {
                val batch = db.batch()
                AfhCatalogDefaults.models.forEach { m ->
                    val ref = db.collection("ai_models").document(m.id)
                    batch.set(
                        ref,
                        mapOf(
                            "afhModelName" to m.afhModelName,
                            "afhModelNameAr" to m.afhModelNameAr,
                            "description" to m.description,
                            "descriptionAr" to m.descriptionAr,
                            "iconKey" to m.iconKey,
                            "capabilities" to m.capabilities,
                            "supportedFileTypes" to m.supportedFileTypes,
                            "tierType" to m.tierType,
                            "enabled" to m.enabled,
                            "dailyLimit" to m.dailyLimit,
                            "monthlyLimit" to m.monthlyLimit,
                            "sortOrder" to m.sortOrder,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                AfhCatalogDefaults.defaultMappings.values.forEach { mapping ->
                    val ref = db.collection("admin_provider_mappings").document(mapping.afhModelId)
                    batch.set(
                        ref,
                        mapOf(
                            "afhModelId" to mapping.afhModelId,
                            "providerIdentifier" to mapping.providerIdentifier,
                            "underlyingModelId" to mapping.underlyingModelId,
                            "systemPrompt" to mapping.systemPrompt,
                            "rateLimitRpm" to mapping.rateLimitRpm,
                            "timeoutSeconds" to mapping.timeoutSeconds,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                AfhCatalogDefaults.defaultProviders.forEach { p ->
                    val ref = db.collection("provider_settings").document(p.providerIdentifier)
                    batch.set(
                        ref,
                        mapOf(
                            "providerIdentifier" to p.providerIdentifier,
                            "displayName" to p.displayName,
                            "enabled" to p.enabled,
                            "configMode" to p.configMode,
                            "status" to p.status,
                            "rateLimitRpm" to p.rateLimitRpm,
                            "notes" to p.notes,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "admin_seed")
            Result.failure(e)
        }
    }

    fun observeAllUsersAdmin(): Flow<List<UserProfile>> = flow {
        requireUserId()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "users")
                    throw error
                }
        )
    }

    fun observeAdminMappings(): Flow<List<AdminProviderMapping>> = flow {
        requireUserId()
        emitAll(
            db.collection("admin_provider_mappings")
                .snapshots()
                .map { it.toObjects(AdminProviderMapping::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "admin_provider_mappings")
                    throw error
                }
        )
    }

    fun observeProviderSettings(): Flow<List<ProviderSetting>> = flow {
        requireUserId()
        emitAll(
            db.collection("provider_settings")
                .snapshots()
                .map { it.toObjects(ProviderSetting::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "provider_settings")
                    throw error
                }
        )
    }

    fun observeUsageLogs(userId: String, isAdmin: Boolean): Flow<List<UsageLog>> = flow {
        val uid = requireUserId()
        val query = if (isAdmin) {
            db.collection("usage_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
        } else {
            db.collection("usage_logs")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(UsageLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    handleFirestoreError(fsError ?: error, OperationType.LIST, "usage_logs")
                    emit(emptyList())
                }
        )
    }

    suspend fun adminSaveModelSpec(model: AfhModelSpec): Result<Unit> {
        val path = "ai_models/${model.id}"
        return try {
            requireUserId()
            db.collection("ai_models").document(model.id).set(
                mapOf(
                    "afhModelName" to model.afhModelName.take(64),
                    "afhModelNameAr" to model.afhModelNameAr.take(64),
                    "description" to model.description.take(300),
                    "descriptionAr" to model.descriptionAr.take(300),
                    "iconKey" to model.iconKey.take(40),
                    "capabilities" to model.capabilities.take(10),
                    "supportedFileTypes" to model.supportedFileTypes.take(10),
                    "tierType" to model.tierType,
                    "enabled" to model.enabled,
                    "dailyLimit" to model.dailyLimit.coerceIn(1L, 100000L),
                    "monthlyLimit" to model.monthlyLimit.coerceIn(1L, 1000000L),
                    "sortOrder" to model.sortOrder.coerceIn(0L, 100L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminSaveProviderMapping(mapping: AdminProviderMapping): Result<Unit> {
        val path = "admin_provider_mappings/${mapping.afhModelId}"
        return try {
            requireUserId()
            db.collection("admin_provider_mappings").document(mapping.afhModelId).set(
                mapOf(
                    "afhModelId" to mapping.afhModelId.take(64),
                    "providerIdentifier" to mapping.providerIdentifier,
                    "underlyingModelId" to mapping.underlyingModelId.take(100),
                    "systemPrompt" to mapping.systemPrompt.take(2000),
                    "rateLimitRpm" to mapping.rateLimitRpm.coerceIn(1L, 10000L),
                    "timeoutSeconds" to mapping.timeoutSeconds.coerceIn(5L, 300L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminSaveProviderSetting(setting: ProviderSetting): Result<Unit> {
        val path = "provider_settings/${setting.providerIdentifier}"
        return try {
            requireUserId()
            db.collection("provider_settings").document(setting.providerIdentifier).set(
                mapOf(
                    "providerIdentifier" to setting.providerIdentifier.take(64),
                    "displayName" to setting.displayName.take(80),
                    "enabled" to setting.enabled,
                    "configMode" to setting.configMode,
                    "status" to setting.status,
                    "rateLimitRpm" to setting.rateLimitRpm.coerceIn(1L, 10000L),
                    "notes" to setting.notes.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateUserAccount(
        targetUserId: String,
        status: String,
        role: String,
        dailyLimit: Long,
        monthlyLimit: Long
    ): Result<Unit> {
        val path = "users/$targetUserId"
        return try {
            requireUserId()
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "status" to status,
                    "role" to role,
                    "dailyLimit" to dailyLimit.coerceIn(1L, 100000L),
                    "monthlyLimit" to monthlyLimit.coerceIn(1L, 1000000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (role == "admin") {
                val targetUser = db.collection("users").document(targetUserId).get().await()
                val email = targetUser.getString("email") ?: "admin@afh.ai"
                db.collection("admins").document(targetUserId).set(
                    mapOf(
                        "email" to email.take(120),
                        "grantedBy" to (auth.currentUser?.uid ?: "admin"),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                db.collection("admins").document(targetUserId).delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "bodygad999@gmail.com"
    }
}

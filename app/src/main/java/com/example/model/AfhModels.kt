package com.example.model

import android.graphics.Bitmap
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val role: String = "user",
    val status: String = "active",
    val dailyRequestsUsed: Long = 0L,
    val monthlyRequestsUsed: Long = 0L,
    val dailyLimit: Long = 50L,
    val monthlyLimit: Long = 1000L,
    val preferredLanguage: String = "en",
    val preferredTheme: String = "dark",
    val defaultAfhModelId: String = "afh_flash",
    val streamingEnabled: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val isAdminRole: Boolean get() = role == "admin"
    val isSuspended: Boolean get() = status == "suspended"
}

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "New Chat",
    val selectedAfhModelId: String = "afh_flash",
    val favorite: Boolean = false,
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val role: String = "user",
    val content: String = "",
    val afhModelId: String = "afh_flash",
    val attachmentNames: List<String> = emptyList(),
    val attachmentTypes: List<String> = emptyList(),
    val errorMessage: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AfhModelSpec(
    @DocumentId val id: String = "",
    val afhModelName: String = "",
    val afhModelNameAr: String = "",
    val description: String = "",
    val descriptionAr: String = "",
    val iconKey: String = "flash",
    val capabilities: List<String> = emptyList(),
    val supportedFileTypes: List<String> = emptyList(),
    val tierType: String = "free",
    val enabled: Boolean = true,
    val dailyLimit: Long = 50L,
    val monthlyLimit: Long = 1000L,
    val sortOrder: Long = 0L,
    val updatedAt: Timestamp? = null
) {
    fun localizedName(isArabic: Boolean): String =
        if (isArabic && afhModelNameAr.isNotBlank()) afhModelNameAr else afhModelName

    fun localizedDescription(isArabic: Boolean): String =
        if (isArabic && descriptionAr.isNotBlank()) descriptionAr else description

    val supportsVision: Boolean get() = capabilities.contains("Vision")
    val supportsFiles: Boolean get() = capabilities.contains("Files") || capabilities.contains("PDF")
    val supportsImageGen: Boolean get() = capabilities.contains("Image Studio")
    val supportsReasoning: Boolean get() = capabilities.contains("Deep Reasoning")
}

@Keep
@IgnoreExtraProperties
data class AdminProviderMapping(
    @DocumentId val id: String = "",
    val afhModelId: String = "",
    val providerIdentifier: String = "google_gemini",
    val underlyingModelId: String = "gemini-3.8-flash",
    val systemPrompt: String = "",
    val rateLimitRpm: Long = 60L,
    val timeoutSeconds: Long = 60L,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProviderSetting(
    @DocumentId val id: String = "",
    val providerIdentifier: String = "",
    val displayName: String = "",
    val enabled: Boolean = false,
    val configMode: String = "server_env",
    val status: String = "unconfigured",
    val rateLimitRpm: Long = 60L,
    val notes: String = "",
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UsageLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val afhModelId: String = "",
    val status: String = "success",
    val latencyMs: Long = 0L,
    val errorCode: String? = null,
    val createdAt: Timestamp? = null
)

data class PendingAttachment(
    val id: String,
    val name: String,
    val mimeType: String,
    val isImage: Boolean,
    val bitmap: Bitmap? = null,
    val textContent: String? = null,
    val rawBytes: ByteArray? = null
)

object AfhCatalogDefaults {
    val models: List<AfhModelSpec> = listOf(
        AfhModelSpec(
            id = "afh_flash",
            afhModelName = "AFH Flash",
            afhModelNameAr = "AFH فلاش",
            description = "Fast answers and everyday assistance.",
            descriptionAr = "إجابات سريعة ومساعدة يومية ذكية.",
            iconKey = "flash",
            capabilities = listOf("Streaming", "Fast Chat", "Vision", "Files"),
            supportedFileTypes = listOf("PNG", "JPG", "TXT", "MD", "PDF"),
            tierType = "free",
            enabled = true,
            dailyLimit = 100L,
            monthlyLimit = 2500L,
            sortOrder = 1L
        ),
        AfhModelSpec(
            id = "afh_pro",
            afhModelName = "AFH Pro",
            afhModelNameAr = "AFH برو",
            description = "Advanced reasoning, analysis, and problem solving.",
            descriptionAr = "تفكير متقدم وتحليل عميق وحل المشكلات المعقدة.",
            iconKey = "pro",
            capabilities = listOf("Streaming", "Deep Reasoning", "Vision", "Files"),
            supportedFileTypes = listOf("PNG", "JPG", "PDF", "TXT", "JSON"),
            tierType = "limited",
            enabled = true,
            dailyLimit = 40L,
            monthlyLimit = 800L,
            sortOrder = 2L
        ),
        AfhModelSpec(
            id = "afh_code",
            afhModelName = "AFH Code",
            afhModelNameAr = "AFH كود",
            description = "Programming, debugging, and software development.",
            descriptionAr = "البرمجة وتصحيح الأخطاء وتطوير البرمجيات.",
            iconKey = "code",
            capabilities = listOf("Streaming", "Code Architecture", "Files"),
            supportedFileTypes = listOf("KT", "PY", "JS", "TS", "JSON", "TXT", "MD"),
            tierType = "free",
            enabled = true,
            dailyLimit = 80L,
            monthlyLimit = 2000L,
            sortOrder = 3L
        ),
        AfhModelSpec(
            id = "afh_study",
            afhModelName = "AFH Study",
            afhModelNameAr = "AFH للدراسة",
            description = "Education, explanations, studying, and homework assistance.",
            descriptionAr = "التعليم والشرح المبسط والمذاكرة وحل الواجبات.",
            iconKey = "study",
            capabilities = listOf("Streaming", "Step-by-Step Tutor", "Vision", "Files"),
            supportedFileTypes = listOf("PNG", "JPG", "PDF", "TXT"),
            tierType = "free",
            enabled = true,
            dailyLimit = 80L,
            monthlyLimit = 2000L,
            sortOrder = 4L
        ),
        AfhModelSpec(
            id = "afh_creative",
            afhModelName = "AFH Creative",
            afhModelNameAr = "AFH الإبداعي",
            description = "Writing, ideas, creativity, and content & visual generation.",
            descriptionAr = "الكتابة والأفكار الإبداعية وتوليد المحتوى والصور.",
            iconKey = "creative",
            capabilities = listOf("Streaming", "Copywriting", "Image Studio", "Vision"),
            supportedFileTypes = listOf("PNG", "JPG", "TXT", "MD"),
            tierType = "limited",
            enabled = true,
            dailyLimit = 30L,
            monthlyLimit = 600L,
            sortOrder = 5L
        ),
        AfhModelSpec(
            id = "afh_vision",
            afhModelName = "AFH Vision",
            afhModelNameAr = "AFH البصري",
            description = "Image understanding and visual analysis, when supported.",
            descriptionAr = "فهم الصور والتحليل البصري المتقدم للمستندات واللقطات.",
            iconKey = "vision",
            capabilities = listOf("Streaming", "Vision", "Visual Q&A", "OCR"),
            supportedFileTypes = listOf("PNG", "JPG", "WEBP"),
            tierType = "free",
            enabled = true,
            dailyLimit = 50L,
            monthlyLimit = 1200L,
            sortOrder = 6L
        ),
        AfhModelSpec(
            id = "afh_research",
            afhModelName = "AFH Research",
            afhModelNameAr = "AFH للأبحاث",
            description = "Research, information analysis, and long-form answers.",
            descriptionAr = "البحث العميق وتحليل المعلومات والإجابات المطولة الموثقة.",
            iconKey = "research",
            capabilities = listOf("Streaming", "Deep Reasoning", "PDF", "Files"),
            supportedFileTypes = listOf("PDF", "TXT", "MD", "CSV", "JSON"),
            tierType = "limited",
            enabled = true,
            dailyLimit = 35L,
            monthlyLimit = 700L,
            sortOrder = 7L
        )
    )

    val defaultMappings: Map<String, AdminProviderMapping> = mapOf(
        "afh_flash" to AdminProviderMapping(
            id = "afh_flash",
            afhModelId = "afh_flash",
            providerIdentifier = "google_gemini",
            underlyingModelId = "gemini-3.8-flash",
            systemPrompt = "You are AFH Flash, the fast everyday assistant of AFH AI. Respond clearly, concisely, and helpfully using clean Markdown. Never mention external AI providers or internal model IDs; identify solely as AFH AI.",
            rateLimitRpm = 60L,
            timeoutSeconds = 45L
        ),
        "afh_pro" to AdminProviderMapping(
            id = "afh_pro",
            afhModelId = "afh_pro",
            providerIdentifier = "google_gemini",
            underlyingModelId = "gemini-3.8-flash",
            systemPrompt = "You are AFH Pro, the advanced reasoning and analytical intelligence of AFH AI. Break down complex problems systematically, verify edge cases, and deliver structured, insightful answers. Never disclose internal model IDs; identify strictly as AFH AI.",
            rateLimitRpm = 30L,
            timeoutSeconds = 60L
        ),
        "afh_code" to AdminProviderMapping(
            id = "afh_code",
            afhModelId = "afh_code",
            providerIdentifier = "google_gemini",
            underlyingModelId = "gemini-3.8-flash",
            systemPrompt = "You are AFH Code, the software engineering and debugging intelligence of AFH AI. Write clean, production-grade code in fenced markdown blocks with language tags, explain root causes clearly, and follow security best practices. Identify strictly as AFH AI.",
            rateLimitRpm = 45L,
            timeoutSeconds = 60L
        ),
        "afh_study" to AdminProviderMapping(
            id = "afh_study",
            afhModelId = "afh_study",
            providerIdentifier = "google_gemini",
            underlyingModelId = "gemini-3.8-flash",
            systemPrompt = "You are AFH Study, the educational tutor of AFH AI. Teach step-by-step with intuitive analogies, clear definitions, and encouraging practice checks. Adapt to the user's language (English or Arabic). Identify strictly as AFH AI.",
            rateLimitRpm = 60L,
            timeoutSeconds = 50L
        ),
        "afh_creative" to AdminProviderMapping(
            id = "afh_creative",
            afhModelId = "afh_creative",
            providerIdentifier = "google_gemini",
            underlyingModelId = "gemini-3.8-flash",
            systemPrompt = "You are AFH Creative, the writing, ideation, and storytelling engine of AFH AI. Craft vivid, original, well-structured prose, scripts, and brand copy tailored to the user's tone. Identify strictly as AFH AI.",
            rateLimitRpm = 40L,
            timeoutSeconds = 55L
        ),
        "afh_vision" to AdminProviderMapping(
            id = "afh_vision",
            afhModelId = "afh_vision",
            providerIdentifier = "google_gemini",
            underlyingModelId = "gemini-3.8-flash",
            systemPrompt = "You are AFH Vision, the visual understanding intelligence of AFH AI. Analyze uploaded images, screenshots, diagrams, and documents accurately and concisely. Identify strictly as AFH AI.",
            rateLimitRpm = 40L,
            timeoutSeconds = 60L
        ),
        "afh_research" to AdminProviderMapping(
            id = "afh_research",
            afhModelId = "afh_research",
            providerIdentifier = "google_gemini",
            underlyingModelId = "gemini-3.8-flash",
            systemPrompt = "You are AFH Research, the deep synthesis and long-form research intelligence of AFH AI. Provide comprehensive, well-organized reports with clear headings, bullet summaries, and balanced perspectives. Identify strictly as AFH AI.",
            rateLimitRpm = 25L,
            timeoutSeconds = 60L
        )
    )

    val defaultProviders: List<ProviderSetting> = listOf(
        ProviderSetting(
            id = "google_gemini",
            providerIdentifier = "google_gemini",
            displayName = "Google Gemini (Firebase AI Logic)",
            enabled = true,
            configMode = "firebase_app_check",
            status = "connected",
            rateLimitRpm = 60L,
            notes = "Active primary adapter via Firebase AI Logic and App Check."
        ),
        ProviderSetting(
            id = "openai",
            providerIdentifier = "openai",
            displayName = "OpenAI API Adapter",
            enabled = false,
            configMode = "server_env",
            status = "unconfigured",
            rateLimitRpm = 30L,
            notes = "Requires server-side OPENAI_API_KEY proxy endpoint."
        ),
        ProviderSetting(
            id = "anthropic",
            providerIdentifier = "anthropic",
            displayName = "Anthropic API Adapter",
            enabled = false,
            configMode = "server_env",
            status = "unconfigured",
            rateLimitRpm = 30L,
            notes = "Requires server-side ANTHROPIC_API_KEY proxy endpoint."
        ),
        ProviderSetting(
            id = "deepseek",
            providerIdentifier = "deepseek",
            displayName = "DeepSeek API Adapter",
            enabled = false,
            configMode = "server_env",
            status = "unconfigured",
            rateLimitRpm = 40L,
            notes = "Requires server-side DEEPSEEK_API_KEY proxy endpoint."
        ),
        ProviderSetting(
            id = "opensource",
            providerIdentifier = "opensource",
            displayName = "Open-Source Inference Adapter",
            enabled = false,
            configMode = "server_env",
            status = "unconfigured",
            rateLimitRpm = 60L,
            notes = "Requires server-side OPEN_SOURCE_INFERENCE_TOKEN gateway."
        )
    )
}

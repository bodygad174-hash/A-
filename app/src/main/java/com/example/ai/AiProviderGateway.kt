package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.AdminProviderMapping
import com.example.model.AfhCatalogDefaults
import com.example.model.ChatMessage
import com.example.model.PendingAttachment
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "AiProviderGateway"

sealed interface AiStreamChunk {
    data class Delta(val accumulatedText: String) : AiStreamChunk
    data class Completed(val finalText: String, val latencyMs: Long) : AiStreamChunk
}

enum class AfhAspectRatioOption(val label: String, val apiRatio: AspectRatio) {
    SQUARE_1_1("1:1", AspectRatio.SQUARE_1x1),
    LANDSCAPE_16_9("16:9", AspectRatio.LANDSCAPE_16x9),
    PORTRAIT_9_16("9:16", AspectRatio.PORTRAIT_9x16),
    LANDSCAPE_4_3("4:3", AspectRatio.LANDSCAPE_4x3),
    PORTRAIT_3_4("3:4", AspectRatio.PORTRAIT_3x4)
}

interface AiProviderAdapter {
    val providerId: String
    fun streamResponse(
        mapping: AdminProviderMapping,
        history: List<ChatMessage>,
        userPrompt: String,
        attachments: List<PendingAttachment>,
        streamingEnabled: Boolean
    ): Flow<AiStreamChunk>

    suspend fun generateVisual(
        prompt: String,
        aspectRatio: AfhAspectRatioOption
    ): Bitmap
}

class FirebaseGeminiAdapter : AiProviderAdapter {
    override val providerId: String = "google_gemini"

    private val defaultModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private fun buildModel(mapping: AdminProviderMapping) =
        if (mapping.underlyingModelId == "gemini-3.8-flash" && mapping.systemPrompt.isBlank()) {
            defaultModel
        } else {
            val safeModelId = mapping.underlyingModelId.ifBlank { "gemini-3.8-flash" }
            Firebase.ai.generativeModel(
                modelName = safeModelId,
                systemInstruction = if (mapping.systemPrompt.isNotBlank()) {
                    content { text(mapping.systemPrompt) }
                } else null
            )
        }

    override fun streamResponse(
        mapping: AdminProviderMapping,
        history: List<ChatMessage>,
        userPrompt: String,
        attachments: List<PendingAttachment>,
        streamingEnabled: Boolean
    ): Flow<AiStreamChunk> = flow {
        val startMs = System.currentTimeMillis()
        val model = buildModel(mapping)
        val timeoutMs = (mapping.timeoutSeconds.coerceIn(15L, 120L)) * 1000L

        val historyContents: List<Content> = history.takeLast(12).map { msg ->
            val role = if (msg.role == "assistant") "model" else "user"
            content(role) {
                text(msg.content.ifBlank { "(attachment)" })
            }
        }

        val userContent = content("user") {
            attachments.forEach { attachment ->
                when {
                    attachment.isImage && attachment.bitmap != null -> {
                        image(attachment.bitmap)
                    }
                    attachment.rawBytes != null && attachment.mimeType == "application/pdf" -> {
                        inlineData(attachment.rawBytes, "application/pdf")
                    }
                    !attachment.textContent.isNullOrBlank() -> {
                        text("\n[Attached File: ${attachment.name}]\n```\n${attachment.textContent.take(16000)}\n```\n")
                    }
                }
            }
            if (userPrompt.isNotBlank()) {
                text(userPrompt)
            }
        }

        val chat = model.startChat(history = historyContents)
        val accumulated = StringBuilder()

        try {
            if (streamingEnabled) {
                withTimeout(timeoutMs) {
                    chat.sendMessageStream(userContent).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            accumulated.append(piece)
                            emit(AiStreamChunk.Delta(accumulated.toString()))
                        }
                    }
                }
            } else {
                val response = withTimeout(timeoutMs) {
                    chat.sendMessage(userContent)
                }
                accumulated.append(response.text.orEmpty())
            }

            val finalStr = accumulated.toString().trim()
            if (finalStr.isBlank()) {
                throw IllegalStateException("AFH AI returned an empty response. Please try again.")
            }
            emit(AiStreamChunk.Completed(finalStr, System.currentTimeMillis() - startMs))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI inference timed out", e)
            throw IllegalStateException("Request timed out after ${timeoutMs / 1000}s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI inference failed", e)
            throw e
        }
    }

    override suspend fun generateVisual(
        prompt: String,
        aspectRatio: AfhAspectRatioOption
    ): Bitmap {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(aspectRatio.apiRatio)
                .setImageSize(ImageSize.SIZE_1K)
                .build()
        }
        val imageModel = Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )
        return try {
            val response = withTimeout(60_000L) {
                imageModel.generateContent(prompt)
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            imagePart?.image
                ?: throw IllegalStateException("No visual image was returned by AFH Creative Studio.")
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Visual generation timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        }
    }
}

class UnconfiguredExternalAdapter(override val providerId: String) : AiProviderAdapter {
    override fun streamResponse(
        mapping: AdminProviderMapping,
        history: List<ChatMessage>,
        userPrompt: String,
        attachments: List<PendingAttachment>,
        streamingEnabled: Boolean
    ): Flow<AiStreamChunk> = flow {
        throw IllegalStateException(
            "Provider '$providerId' requires a configured server-side API key and backend proxy. " +
                "Switch this model's routing to Google Gemini in the AFH Admin Console to use live inference."
        )
    }

    override suspend fun generateVisual(
        prompt: String,
        aspectRatio: AfhAspectRatioOption
    ): Bitmap {
        throw IllegalStateException("Visual generation requires an active AFH Creative Studio backend.")
    }
}

class AiProviderGateway {
    private val geminiAdapter = FirebaseGeminiAdapter()

    fun resolveAdapter(providerIdentifier: String): AiProviderAdapter {
        return when (providerIdentifier) {
            "google_gemini" -> geminiAdapter
            else -> UnconfiguredExternalAdapter(providerIdentifier)
        }
    }

    fun resolveMapping(
        afhModelId: String,
        adminMappings: Map<String, AdminProviderMapping>
    ): AdminProviderMapping {
        return adminMappings[afhModelId]
            ?: AfhCatalogDefaults.defaultMappings[afhModelId]
            ?: AfhCatalogDefaults.defaultMappings.getValue("afh_flash")
    }

    suspend fun generateVisual(prompt: String, aspectRatio: AfhAspectRatioOption): Bitmap {
        return geminiAdapter.generateVisual(prompt, aspectRatio)
    }
}

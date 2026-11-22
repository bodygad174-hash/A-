package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AfhCyan
import com.example.ui.theme.AfhDeepNavy
import com.example.ui.theme.AfhTurquoise
import com.example.ui.theme.JetBrainsMonoFamily

private sealed interface MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
}

@Composable
fun MarkdownMessageView(
    content: String,
    modifier: Modifier = Modifier,
    onCopied: (() -> Unit)? = null
) {
    val blocks = remember(content) { parseMarkdownBlocks(content) }
    val clipboard = LocalClipboardManager.current

    SelectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.CodeBlock -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = AfhDeepNavy,
                            border = BorderStroke(1.dp, AfhCyan.copy(alpha = 0.28f))
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = block.language.ifBlank { "code" }.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AfhCyan,
                                        fontFamily = JetBrainsMonoFamily
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(block.code))
                                            onCopied?.invoke()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy code",
                                            tint = AfhTurquoise,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = highlightCode(block.code),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 12.5.sp,
                                    lineHeight = 19.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                                )
                            }
                        }
                    }

                    is MarkdownBlock.TextBlock -> {
                        val lines = block.text.lines()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            lines.forEach { line ->
                                when {
                                    line.startsWith("### ") -> Text(
                                        text = parseInlineMarkdown(line.removePrefix("### ")),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    line.startsWith("## ") -> Text(
                                        text = parseInlineMarkdown(line.removePrefix("## ")),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    line.startsWith("# ") -> Text(
                                        text = parseInlineMarkdown(line.removePrefix("# ")),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    line.startsWith("- ") || line.startsWith("* ") -> Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "•",
                                            color = AfhCyan,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = parseInlineMarkdown(line.drop(2)),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    line.isBlank() -> Spacer(modifier = Modifier.height(2.dp))
                                    else -> Text(
                                        text = parseInlineMarkdown(line),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    val result = mutableListOf<MarkdownBlock>()
    val lines = raw.lines()
    val textBuffer = StringBuilder()
    val codeBuffer = StringBuilder()
    var inCode = false
    var currentLang = ""

    for (line in lines) {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("```")) {
            if (!inCode) {
                if (textBuffer.isNotBlank()) {
                    result.add(MarkdownBlock.TextBlock(textBuffer.toString().trimEnd()))
                    textBuffer.clear()
                }
                inCode = true
                currentLang = trimmed.removePrefix("```").trim()
                codeBuffer.clear()
            } else {
                result.add(MarkdownBlock.CodeBlock(currentLang, codeBuffer.toString().trimEnd()))
                codeBuffer.clear()
                currentLang = ""
                inCode = false
            }
        } else {
            if (inCode) {
                codeBuffer.appendLine(line)
            } else {
                textBuffer.appendLine(line)
            }
        }
    }
    if (inCode && codeBuffer.isNotEmpty()) {
        result.add(MarkdownBlock.CodeBlock(currentLang, codeBuffer.toString().trimEnd()))
    } else if (textBuffer.isNotBlank()) {
        result.add(MarkdownBlock.TextBlock(textBuffer.toString().trimEnd()))
    }
    return result
}

private fun parseInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text.startsWith("`", i) -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFamily,
                            color = AfhCyan,
                            background = Color(0xFF162340)
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            text.startsWith("*", i) && !text.startsWith("**", i) -> {
                val end = text.indexOf('*', i + 1)
                if (end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}

private fun highlightCode(code: String): AnnotatedString = buildAnnotatedString {
    val keywords = setOf(
        "fun", "val", "var", "class", "object", "interface", "return", "if", "else",
        "when", "for", "while", "import", "package", "suspend", "async", "await",
        "const", "let", "function", "def", "from", "try", "catch", "true", "false", "null"
    )
    val lines = code.lines()
    lines.forEachIndexed { idx, line ->
        val trimmed = line.trimStart()
        if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
            withStyle(SpanStyle(color = Color(0xFF758BB3), fontStyle = FontStyle.Italic)) {
                append(line)
            }
        } else {
            val tokens = line.split(Regex("(?<=\\b)|(?=\\b)"))
            tokens.forEach { token ->
                when {
                    token in keywords -> withStyle(SpanStyle(color = Color(0xFF875BFF), fontWeight = FontWeight.Medium)) {
                        append(token)
                    }
                    token.startsWith("\"") || token.startsWith("'") -> withStyle(SpanStyle(color = AfhTurquoise)) {
                        append(token)
                    }
                    token.toDoubleOrNull() != null -> withStyle(SpanStyle(color = Color(0xFFFFB547))) {
                        append(token)
                    }
                    else -> withStyle(SpanStyle(color = Color(0xFFF5F8FF))) {
                        append(token)
                    }
                }
            }
        }
        if (idx < lines.lastIndex) append("\n")
    }
}

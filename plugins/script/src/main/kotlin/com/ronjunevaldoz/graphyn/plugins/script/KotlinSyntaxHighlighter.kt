package com.ronjunevaldoz.graphyn.plugins.script

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// Catppuccin Mocha
private val COLOR_KEYWORD = Color(0xFFCBA6F7)
private val COLOR_STRING  = Color(0xFFA6E3A1)
private val COLOR_COMMENT = Color(0xFF6C7086)
private val COLOR_NUMBER  = Color(0xFFFAB387)
private val COLOR_TYPE    = Color(0xFF89B4FA)

private val KEYWORDS = setOf(
    "val", "var", "fun", "class", "object", "interface", "import", "package",
    "if", "else", "when", "for", "while", "do", "return", "break", "continue",
    "true", "false", "null", "is", "as", "in", "by", "this", "super",
    "try", "catch", "finally", "throw", "data", "sealed", "companion",
    "override", "open", "abstract", "private", "internal", "protected",
    "suspend", "inline", "operator", "infix", "typealias", "enum", "it",
)

internal object KotlinHighlightTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(highlight(text.text), OffsetMapping.Identity)
}

private fun highlight(code: String): AnnotatedString = buildAnnotatedString {
    append(code)
    var i = 0
    while (i < code.length) {
        when {
            code.startsWith("//", i) -> {
                val end = code.indexOf('\n', i).let { if (it < 0) code.length else it }
                addStyle(SpanStyle(color = COLOR_COMMENT), i, end)
                i = end
            }
            code[i] == '"' -> {
                var j = i + 1
                while (j < code.length && code[j] != '"' && code[j] != '\n') {
                    if (code[j] == '\\') j++
                    j++
                }
                val end = if (j < code.length && code[j] == '"') j + 1 else j
                addStyle(SpanStyle(color = COLOR_STRING), i, end)
                i = end
            }
            code[i].isDigit() -> {
                var j = i + 1
                while (j < code.length && (code[j].isDigit() || code[j] == '.' || code[j] in "LfFdD")) j++
                addStyle(SpanStyle(color = COLOR_NUMBER), i, j)
                i = j
            }
            code[i].isLetter() || code[i] == '_' -> {
                var j = i + 1
                while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                val word = code.substring(i, j)
                when {
                    word in KEYWORDS -> addStyle(SpanStyle(color = COLOR_KEYWORD), i, j)
                    word[0].isUpperCase() -> addStyle(SpanStyle(color = COLOR_TYPE), i, j)
                }
                i = j
            }
            else -> i++
        }
    }
}

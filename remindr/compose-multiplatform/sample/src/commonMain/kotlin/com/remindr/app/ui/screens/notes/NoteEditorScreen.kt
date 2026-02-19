package com.remindr.app.ui.screens.notes

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remindr.app.ui.components.BackHandler
import com.remindr.app.ui.theme.Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    content: String,
    isNewNote: Boolean,
    onContentChange: (String) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(enabled = true, onBack = onBack)
    var editorValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = content,
                selection = TextRange(content.length),
            ),
        )
    }

    LaunchedEffect(content) {
        if (content != editorValue.text) {
            editorValue = TextFieldValue(
                text = content,
                selection = TextRange(content.length),
            )
        }
    }

    Scaffold(
        containerColor = Colors.example5PageBgColor,
        contentColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNewNote) "New note" else "Edit note",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onDone) {
                        Text(
                            text = "Done",
                            color = Colors.example1Selection,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Colors.example5ToolbarColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Colors.example1Selection,
                ),
            )
        },
    ) { paddingValues ->
        TextField(
            value = editorValue,
            onValueChange = { incoming ->
                val formatted = autoContinuePrefix(
                    previous = editorValue,
                    incoming = incoming,
                )
                editorValue = formatted
                onContentChange(formatted.text)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            placeholder = {
                Text(
                    text = "Start typing...",
                    color = Colors.example5TextGreyLight,
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                lineHeight = 24.sp,
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Colors.example5ItemViewBgColor,
                unfocusedContainerColor = Colors.example5ItemViewBgColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.White,
            ),
            shape = RoundedCornerShape(18.dp),
        )
    }
}

private fun autoContinuePrefix(
    previous: TextFieldValue,
    incoming: TextFieldValue,
): TextFieldValue {
    val oldCursor = previous.selection.start
    val newCursor = incoming.selection.start
    val insertedLength = newCursor - oldCursor

    if (incoming.text.length <= previous.text.length || insertedLength <= 0) {
        return incoming
    }
    if (oldCursor < 0 || oldCursor > previous.text.length) {
        return incoming
    }
    if (newCursor < 0 || newCursor > incoming.text.length) {
        return incoming
    }

    val insertedText = incoming.text.substring(oldCursor, newCursor)
    if (insertedText != "\n") {
        return incoming
    }

    val currentLineBeforeCursor = previous.text
        .substring(0, oldCursor)
        .substringAfterLast('\n')
    val prefix = continuationPrefixForLine(currentLineBeforeCursor) ?: return incoming

    val nextText = buildString(incoming.text.length + prefix.length) {
        append(incoming.text, 0, newCursor)
        append(prefix)
        append(incoming.text, newCursor, incoming.text.length)
    }
    val nextCursor = newCursor + prefix.length
    return incoming.copy(
        text = nextText,
        selection = TextRange(nextCursor),
    )
}

private fun continuationPrefixForLine(line: String): String? {
    val indent = line.takeWhile { it == ' ' || it == '\t' }
    val trimmed = line.drop(indent.length)
    if (trimmed.startsWith("- ")) return "$indent- "
    if (trimmed.startsWith("* ")) return "$indent* "
    if (indent.isNotEmpty()) return indent
    return null
}

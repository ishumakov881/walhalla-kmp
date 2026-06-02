package net.example.online.push.ui

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString

/**
 * Как [LetterHtmlText] в старом NotificationMessagesScreen:
 * HTML из `msg.body` → [htmlToAnnotatedString], ссылки открываются через [LocalUriHandler].
 */
@Composable
internal fun PushLetterHtmlText(
    html: String,
    modifier: Modifier = Modifier,
) {
    if (html.isBlank()) return

    val annotated = remember(html) { htmlToAnnotatedString(html) }
    val uriHandler = LocalUriHandler.current

    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()
                ?.let { uriHandler.openUri(it.item) }
        },
    )
}

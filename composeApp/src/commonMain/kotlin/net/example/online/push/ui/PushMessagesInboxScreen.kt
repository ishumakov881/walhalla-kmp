package net.example.online.push.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.example.online.push.PushInbox
import net.example.online.push.PushInbox.InboxEntry

/**
 * Полноэкранный список push (как экран в старом accesspoint): строки, свайп — удаление,
 * тап — текст + «прочитано».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushMessagesInboxScreen(
    onDismiss: () -> Unit,
) {
    val pushCounts by PushInbox.counts.collectAsState()
    val entries = remember(pushCounts) { PushInbox.listInboxEntries() }
    var detailEntry by remember { mutableStateOf<InboxEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var deleteSnackbarJob by remember { mutableStateOf<Job?>(null) }

    fun deleteWithUndo(entry: InboxEntry) {
        val snapshot = entry.snapshot()
        PushInbox.deleteLetter(
            letterId = entry.letter.id,
            privateOwnerUserName = entry.privateOwnerUserName,
        )
        deleteSnackbarJob?.cancel()
        deleteSnackbarJob = scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Сообщение удалено",
                actionLabel = "Отменить",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                PushInbox.restoreLetter(snapshot)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Сообщения") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                    }
                },
            )
        },
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Нет сообщений", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(
                    items = entries,
                    key = { "${it.letter.id}_${it.privateOwnerUserName.orEmpty()}" },
                ) { entry ->
                    key(entry.letter.id, entry.privateOwnerUserName) {
                        SwipeToDeleteContainer(
                            onDelete = { deleteWithUndo(entry) },
                        ) {
                            PushMessageListItem(
                                entry = entry,
                                onClick = {
                                    if (entry.letter.isUnread) {
                                        PushInbox.markLetterRead(
                                            entry.letter.id,
                                            entry.privateOwnerUserName,
                                        )
                                    }
                                    detailEntry = entry
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    detailEntry?.let { e ->
        AlertDialog(
            onDismissRequest = { detailEntry = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = e.letter.title.ifEmpty { "Сообщение" },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { detailEntry = null }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Закрыть",
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = formatPushLetterDateTime(e.letter.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PushLetterHtmlText(html = e.letter.text)
//                    TextButton(
//                        onClick = {
//                            PushInbox.markLetterRead(e.letter.id, e.privateOwnerUserName)
//                            detailEntry = null
//                        },
//                    ) {
//                        Text("Прочитано")
//                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteWithUndo(e)
                        detailEntry = null
                    },
                ) {
                    Text("Удалить сообщение", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = { detailEntry = null }) {
                    Text("Закрыть")
                }
            },
        )
    }
}

/** Как [SwipeToDeleteContainer] в старом NotificationMessagesScreen. */
@Composable
private fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                currentOnDelete()
                false
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
        },
        content = { content() },
    )
}

/**
 * Иконка сообщений в toolbar (как [WebVieToolbar] в appWebView):
 * видна при total > 0, badge — число непрочитанных.
 */
@Composable
fun PushInboxTopBarAction(
    unreadCount: Int,
    totalCount: Int,
    onOpenInbox: () -> Unit,
) {
    if (totalCount <= 0) return

    BadgedBox(
        badge = {
            if (unreadCount > 0) {
                Badge {
                    Text("$unreadCount")
                }
            }
        },
    ) {
        IconButton(onClick = onOpenInbox) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = "Сообщения",
            )
        }
    }
}

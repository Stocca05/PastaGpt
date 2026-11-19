package pt.isel.pdm.droidmentor.ui.screens.activechat // Groups the selected conversation's Room-backed UI.

import androidx.compose.foundation.ExperimentalFoundationApi // Enables long-press combinedClickable on message bubbles.
import androidx.compose.foundation.combinedClickable // Opens the bubble context menu on long-press.
import androidx.compose.foundation.layout.Arrangement // Separates messages and input controls.
import androidx.compose.foundation.layout.Box // Displays loading, empty, error or message content in the same body area.
import androidx.compose.foundation.layout.Column // Places message content above the draft controls.
import androidx.compose.foundation.layout.PaddingValues // Gives the message list consistent spacing.
import androidx.compose.foundation.layout.Row // Aligns role-specific bubbles and draft controls.
import androidx.compose.foundation.layout.consumeWindowInsets // Avoids applying already reserved system insets twice.
import androidx.compose.foundation.layout.fillMaxSize // Uses the available body area.
import androidx.compose.foundation.layout.fillMaxWidth // Gives rows the full conversation width.
import androidx.compose.foundation.layout.imePadding // Keeps the draft above the visible keyboard.
import androidx.compose.foundation.layout.padding // Applies the existing screen and bubble spacing.
import androidx.compose.foundation.layout.size // Sizes the compact waiting indicator.
import androidx.compose.foundation.layout.widthIn // Keeps message bubbles readable on wider screens.
import androidx.compose.foundation.lazy.LazyColumn // Displays the complete saved message list efficiently.
import androidx.compose.foundation.lazy.items // Uses persisted message IDs as stable item keys.
import androidx.compose.foundation.lazy.rememberLazyListState // Keeps scroll position across ordinary recomposition.
import androidx.compose.material3.AlertDialog // Confirms message rewrite and message deletion.
import androidx.compose.material3.Button // Provides explicit send and same-row retry actions.
import androidx.compose.material3.CircularProgressIndicator // Displays loading and in-flight progress.
import androidx.compose.material3.DropdownMenu // Hosts the Edit, Copy and Delete actions on long-press.
import androidx.compose.material3.DropdownMenuItem // Renders one context-menu action.
import androidx.compose.material3.Icon // Displays the TopAppBar rename icon.
import androidx.compose.material3.IconButton // Provides one-tap manual rename in the app bar.
import androidx.compose.material3.MaterialTheme // Supplies bubble colours, shapes and text styles.
import androidx.compose.material3.OutlinedTextField // Displays the controlled in-memory draft.
import androidx.compose.material3.SnackbarHostState // Displays safe local command feedback.
import androidx.compose.material3.Surface // Draws a role-specific message bubble.
import androidx.compose.material3.Text // Displays saved text and resource-backed labels.
import androidx.compose.material3.TextButton // Provides Cancel, Resend and Delete dialog buttons.
import androidx.compose.runtime.Composable // Describes the conversation UI.
import androidx.compose.runtime.LaunchedEffect // Scrolls when the saved message list changes and tracks scroll position.
import androidx.compose.runtime.getValue // Reads lifecycle-aware Compose state delegates.
import androidx.compose.runtime.mutableIntStateOf // Tracks previous message count to distinguish initial restore from new turns.
import androidx.compose.runtime.mutableStateOf // Tracks whether the manual rename dialog is open.
import androidx.compose.runtime.remember // Owns the factory and feedback state for this selected ID.
import androidx.compose.runtime.rememberCoroutineScope // Launches the clipboard-copied snackbar notice.
import androidx.compose.runtime.setValue // Opens and closes the manual rename dialog.
import androidx.compose.runtime.snapshotFlow // Observes LazyList scroll position cleanly for SavedStateHandle.
import androidx.compose.ui.Alignment // Centres loading states and aligns draft controls.
import androidx.compose.ui.Modifier // Describes layout size, padding and keyboard handling.
import androidx.compose.ui.draw.clip // Clips long-press ripple to the message bubble shape.
import androidx.compose.ui.platform.LocalClipboardManager // Copies message text cleanly to the system clipboard.
import androidx.compose.ui.platform.LocalContext // Resolves safe notice strings in suspend event handlers.
import androidx.compose.ui.res.painterResource // Loads the shared local edit vector icon.
import androidx.compose.ui.res.stringResource // Reads all visible action and error labels.
import androidx.compose.ui.text.AnnotatedString // Wraps copied text for the Compose clipboard API.
import androidx.compose.ui.unit.dp // Sizes bubbles and the small waiting indicator.
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Observes immutable state only while this destination is active.
import androidx.lifecycle.viewmodel.compose.viewModel // Uses the selected conversation's manual factory.
import kotlinx.coroutines.flow.distinctUntilChanged // Avoids redundant SavedStateHandle writes while scroll is idle.
import kotlinx.coroutines.flow.filterNotNull // Ignores pre-layout snapshots before Room items appear.
import kotlinx.coroutines.launch // Shows confirmation snackbar when copying a message.
import pt.isel.pdm.droidmentor.R // Supplies message, action and feedback resources.
import pt.isel.pdm.droidmentor.domain.Message // Reads text and error labels only from persisted domain messages.
import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Supplies a safe fallback for inconsistent legacy error metadata.
import pt.isel.pdm.droidmentor.domain.MessageStatus // Distinguishes pending and failed saved turns.
import pt.isel.pdm.droidmentor.domain.Role // Chooses bubble alignment and author labels.
import pt.isel.pdm.droidmentor.ui.components.EventEffect // Collects local feedback with the destination lifecycle.
import pt.isel.pdm.droidmentor.ui.components.RenameChatDialog // Shares the manual title-editing dialog.
import pt.isel.pdm.droidmentor.ui.components.ScreenScaffold // Supplies the saved title and explicit Back action.
import pt.isel.pdm.droidmentor.ui.components.UiEvent // Distinguishes safe local feedback events.
import pt.isel.pdm.droidmentor.ui.components.messageResource // Resolves typed local notices into UI resources.
import pt.isel.pdm.droidmentor.ui.theme.DroidMentorSpacing // Reuses the shared spacing values.

/** Displays Room-owned messages and explicit commands for the local conversation selected by navigation. */
@Composable // Keeps networking and persistence inside the injected ViewModel and repository.
fun ActiveChatScreen( // Receives the local navigation argument and a manually constructed screen model.
    chatId: String, // Identifies the local conversation selected by the graph.
    onBack: () -> Unit, // Returns to the existing history entry.
    model: ActiveChatViewModel = viewModel(key = chatId, factory = remember(chatId) { ActiveChatViewModel.factory(chatId) }), // Uses ID-specific ownership by default and allows an isolated model in device tests.
    onOpenSettings: () -> Unit = {}, // Navigates directly to Settings when no key is configured or an auth failure occurs.
) { // Starts the selected conversation UI without changing production dependency construction.
    val state by model.state.collectAsStateWithLifecycle() // Observes only immutable Room-derived message state.
    val snackbar = remember { SnackbarHostState() } // Owns local feedback for this conversation.
    val context = LocalContext.current // Resolves safe labels without putting Context in the ViewModel.
    val listState = rememberLazyListState( // Restores saved scroll position across process death.
        initialFirstVisibleItemIndex = state.savedScrollIndex, // Uses SavedStateHandle index when recreating the screen.
        initialFirstVisibleItemScrollOffset = state.savedScrollOffset, // Uses SavedStateHandle offset when recreating the screen.
    ) // Ends scroll-state setup.
    var previousMessageCount by remember(chatId) { mutableIntStateOf(-1) } // Distinguishes initial load from newly added turns.
    val clipboard = LocalClipboardManager.current // Copies message text on context-menu Copy.
    val scope = rememberCoroutineScope() // Displays a short snackbar notice after copying.
    var renaming by remember { mutableStateOf(false) } // Controls visibility of the manual rename dialog.
    var editingMessage by remember { mutableStateOf<Message?>(null) } // Tracks the user message currently being rewritten.
    var deletingMessage by remember { mutableStateOf<Message?>(null) } // Tracks the user message awaiting delete confirmation.
    EventEffect(model.events) { event -> // Handles prerequisite or persistence feedback outside the layout.
        if (event is UiEvent.Notice) snackbar.showSnackbar(context.getString(event.notice.messageResource())) // Displays no direct API payload or raw exception text.
    } // Ends local event collection.
    LaunchedEffect(state.loading, state.messages.size) { // Scrolls to the latest turn on new messages while preserving restored scroll on process-death recovery.
        if (state.loading) return@LaunchedEffect // Waits until Room emits the initial message snapshot.
        if (previousMessageCount == -1) { // Handles the first Room emission for this composition.
            previousMessageCount = state.messages.size // Records baseline message count.
            if (state.messages.isNotEmpty()) { // Only scrolls when messages exist.
                if (state.savedScrollIndex > 0 || state.savedScrollOffset > 0) { // Restores saved position after process death or rotation.
                    listState.scrollToItem(state.savedScrollIndex.coerceAtMost(state.messages.lastIndex), state.savedScrollOffset) // Jumps back to the saved position.
                } else { // Opens a fresh conversation at the latest turn.
                    listState.animateScrollToItem(state.messages.lastIndex) // Keeps the latest saved turn in view.
                } // Ends initial scroll choice.
            } // Ends non-empty initial scroll.
        } else if (state.messages.size > previousMessageCount) { // Scrolls smoothly only when new turns arrive.
            previousMessageCount = state.messages.size // Updates baseline message count.
            listState.animateScrollToItem(state.messages.lastIndex) // Brings the newly added turn into view.
        } else { // Tracks deletions or rewrites without forcing a jump.
            previousMessageCount = state.messages.size // Keeps baseline count accurate.
        } // Ends initial-versus-update scroll handling.
    } // Ends saved-message scroll handling.
    LaunchedEffect(listState) { // Persists scroll position into SavedStateHandle once items are laid out.
        snapshotFlow { // Reads visible scroll coordinates safely during Compose snapshots.
            if (listState.layoutInfo.totalItemsCount > 0) { // Ignores empty/loading states so restored coordinates are never overwritten by zero.
                listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset // Captures current scroll position.
            } else null // Waits for items to appear.
        }.filterNotNull().distinctUntilChanged().collect { (index, offset) -> // Persists only changed non-empty positions.
            model.updateScrollPosition(index, offset) // Saves scroll position for process-death survival.
        } // Ends scroll-position collection.
    } // Ends scroll-position persistence effect.
    val currentChat = state.chat // Captures the current conversation so the rename dialog uses a consistent snapshot.
    if (renaming && currentChat != null) { // Displays manual title editing when requested from the TopAppBar.
        RenameChatDialog( // Reuses the shared rename dialog.
            initialTitle = currentChat.title, // Pre-fills the current conversation title.
            onConfirm = { newTitle -> // Saves the new title via RoomChatRepository.
                renaming = false // Dismisses the dialog first.
                model.renameChat(newTitle) // Updates the stored title in Room.
            }, // Ends confirm handler.
            onDismiss = { renaming = false }, // Closes without changing Room.
        ) // Ends rename dialog.
    } // Ends rename dialog check.
    editingMessage?.let { target -> // Displays the rewrite dialog with explicit truncation warning.
        RewriteMessageDialog( // Lets the user review and confirm truncating subsequent turns.
            initialText = target.text, // Pre-fills the original user prompt.
            onConfirm = { newText -> // Truncates and resends through RoomChatRepository.
                editingMessage = null // Dismisses the dialog first.
                model.rewriteMessage(target.id, newText) // Updates the turn and deletes later turns atomically.
            }, // Ends confirm handler.
            onDismiss = { editingMessage = null }, // Closes without modifying history.
        ) // Ends rewrite dialog.
    } // Ends rewrite dialog check.
    deletingMessage?.let { target -> // Confirms deleting a user message and all later turns.
        AlertDialog( // Prevents accidental deletion of subsequent conversation exchanges.
            onDismissRequest = { deletingMessage = null }, // Closes without modifying Room.
            title = { Text(stringResource(R.string.message_delete_dialog_title)) }, // Summarises deletion.
            text = { Text(stringResource(R.string.message_delete_warning)) }, // Warns that later replies will also be removed.
            confirmButton = { // Deletes from target.seq onward.
                TextButton(onClick = { // Confirms tail deletion.
                    deletingMessage = null // Dismisses the dialog first.
                    model.deleteMessage(target.id) // Removes this turn and later turns in Room.
                }) { Text(stringResource(R.string.message_action_delete)) } // Labels confirm button.
            }, // Ends confirm slot.
            dismissButton = { // Cancels without deleting.
                TextButton(onClick = { deletingMessage = null }) { Text(stringResource(R.string.history_delete_cancel)) } // Reuses Cancel label.
            }, // Ends dismiss slot.
        ) // Ends delete message confirmation dialog.
    } // Ends delete message dialog check.
    ScreenScaffold( // Displays saved conversation metadata and manual rename action.
        title = state.chat?.title ?: stringResource(R.string.navigation_active_chat), // Uses the live Room-backed chat title.
        onBack = onBack, // Returns to the previous history destination.
        snackbarHostState = snackbar, // Hosts safe local command feedback.
        actions = { // Offers manual renaming whenever the conversation metadata is loaded.
            if (state.chat != null && !state.working && !state.pending) { // Avoids renaming while a network exchange holds the chat lock.
                IconButton(onClick = { renaming = true }) { // Opens the manual rename dialog.
                    Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.chat_rename_action)) // Labels the action for accessibility.
                } // Ends rename action button.
            } // Ends loaded-chat action check.
        }, // Ends TopAppBar actions.
    ) { padding -> // Displays saved conversation metadata.
        Column( // Keeps the message body above the in-memory draft.
            modifier = Modifier // Builds keyboard and system-inset handling in readable steps.
                .fillMaxSize() // Uses the full body area.
                .padding(padding) // Leaves room for app and system bars.
                .consumeWindowInsets(padding) // Marks the reserved insets as already handled.
                .imePadding(), // Moves input above the keyboard without doubling navigation-bar padding.
        ) { // Displays local loading, error or message state before the draft controls.
            if (!state.connected) { // Keeps the offline notice visible independently of saved history.
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) { // Uses readable theme-provided contrast for the persistent banner.
                    Text(stringResource(R.string.notice_offline), modifier = Modifier.padding(DroidMentorSpacing.medium)) // Explains that new interactions are unavailable while history stays visible.
                } // Ends the offline banner.
            } // Ends connectivity feedback.
            if (!state.loading && (state.settingsError || !state.hasKey || state.hasInvalidKeyFailure)) { // Explains missing, unreadable or rejected key with a direct Settings action.
                val keyNotice = when { // Chooses safe contextual copy for missing vs rejected credentials.
                    state.settingsError -> R.string.notice_settings_error // Explains settings storage could not be read.
                    !state.hasKey -> R.string.notice_missing_key // Explains no API key is configured yet.
                    else -> R.string.notice_rejected_key // Explains a 401/403 authentication failure happened.
                } // Ends contextual key message choice.
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) { // Frames the guidance and direct Settings button clearly.
                    Row( // Places explanation text beside the direct Open Settings shortcut.
                        modifier = Modifier.fillMaxWidth().padding(horizontal = DroidMentorSpacing.medium, vertical = DroidMentorSpacing.small), // Applies comfortable banner padding.
                        horizontalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small), // Separates copy and action button.
                        verticalAlignment = Alignment.CenterVertically, // Centres the action beside wrapping text.
                    ) { // Starts contextual banner row.
                        Text(stringResource(keyNotice), modifier = Modifier.weight(1f)) // Keeps configuration feedback visible while the draft stays editable.
                        TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.action_open_settings)) } // Leads straight to Settings with zero dead ends.
                    } // Ends contextual banner row.
                } // Ends key-status banner container.
            } // Ends key-status feedback.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { // Gives the saved message body the remaining screen height.
                when { // Keeps storage errors and missing conversations distinct from empty messages.
                    state.loading -> CircularProgressIndicator() // Waits for the initial Room snapshots.
                    state.storageError -> Text(stringResource(R.string.chat_read_error)) // Reports a local history query failure.
                    state.chat == null -> Text(stringResource(R.string.notice_not_found)) // Reports a conversation removed from local storage.
                    state.messages.isEmpty() -> Text(stringResource(R.string.chat_empty)) // Displays a new locally saved conversation without fake sample messages.
                    else -> LazyColumn( // Displays all saved messages in the repository's sequence order.
                        modifier = Modifier.fillMaxSize(), // Uses the body area above the input.
                        state = listState, // Shares the list state with automatic scrolling.
                        contentPadding = PaddingValues(DroidMentorSpacing.medium), // Adds space around message bubbles.
                        verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small), // Separates consecutive turns.
                    ) { // Uses stable local message IDs.
                        items(state.messages, key = { it.id }) { message -> // Renders only rows obtained from Room observation.
                            MessageBubble( // Keeps bubble layout separate from screen and command logic.
                                message = message, // Supplies saved text, role, status and error category.
                                showRetry = message.id == state.messages.lastOrNull()?.id, // Displays the action only beside the final saved row.
                                retryEnabled = state.canRetry, // Shares the ViewModel's current key, connection and ownership guard.
                                canEdit = state.canRewrite, // Enables rewrite only when online, configured and idle.
                                canDelete = state.canModifyMessages, // Enables local deletion whenever no request is active.
                                onRetry = model::retry, // Delegates same-row retry validation to the ViewModel and repository.
                                onEdit = { editingMessage = message }, // Opens the prefilled rewrite dialog with truncation warning.
                                onCopy = { // Copies bubble text to clipboard and confirms via snackbar.
                                    clipboard.setText(AnnotatedString(message.text)) // Copies full saved text.
                                    scope.launch { snackbar.showSnackbar(context.getString(R.string.message_copied)) } // Shows brief feedback.
                                }, // Ends copy handler.
                                onDelete = { deletingMessage = message }, // Opens delete confirmation dialog.
                            ) // Ends this saved message bubble.
                        } // Ends the Room-derived message items.
                    } // Ends the saved message list.
                } // Ends local read-state rendering.
            } // Ends the saved message body.
            if (state.working || state.pending) { // Shows ongoing work from the command or saved SENDING row.
                Row(Modifier.padding(DroidMentorSpacing.medium), horizontalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small), verticalAlignment = Alignment.CenterVertically) { // Places a compact indicator beside its label.
                    CircularProgressIndicator(Modifier.size(20.dp)) // Shows waiting without blocking Back navigation.
                    Text(stringResource(R.string.chat_waiting)) // Explains that the saved attempt is still pending.
                } // Ends the waiting indicator.
            } // Ends in-flight feedback.
            Row( // Places an editable draft beside the explicit send action.
                modifier = Modifier.fillMaxWidth().padding(DroidMentorSpacing.medium), // Keeps the input and tap target clear of the screen edges.
                horizontalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small), // Separates input and sending.
                verticalAlignment = Alignment.Bottom, // Aligns Send with the bottom of a multiline draft.
            ) { // Uses ViewModel commands rather than direct network calls.
                OutlinedTextField( // Keeps unsent user text only in memory.
                    value = state.draft, // Displays the controlled draft without replacing it after a rejected command.
                    onValueChange = model::updateDraft, // Routes editing through the ViewModel's command guard.
                    modifier = Modifier.weight(1f), // Leaves space for the Send action.
                    label = { Text(stringResource(R.string.chat_draft_label)) }, // Labels message input.
                    enabled = !state.loading && !state.storageError && state.chat != null && !state.working && !state.pending, // Keeps the draft editable after a failed attempt or while offline.
                    maxLines = 5, // Allows small Kotlin examples and multiline questions without hiding the whole message list.
                ) // Ends in-memory draft input.
                Button(onClick = model::send, enabled = state.canSend) { // Blocks blank, pending and unresolved-failure sends through the shared state guard.
                    Text(stringResource(R.string.chat_send)) // Names the explicit interaction action.
                } // Ends explicit sending.
            } // Ends draft controls.
        } // Ends keyboard-aware conversation layout.
    } // Ends the selected conversation frame.
} // Ends the active chat screen.

@OptIn(ExperimentalFoundationApi::class) // Uses combinedClickable to open the bubble context menu on long-press.
@Composable // Renders one saved row without owning networking or persistence.
private fun MessageBubble( // Receives callbacks from the screen for retry and long-press context actions.
    message: Message, // Supplies role, text, status and error category.
    showRetry: Boolean, // Shows Retry only on the final failed user message.
    retryEnabled: Boolean, // Mirrors current send/retry prerequisite guards.
    canEdit: Boolean, // Enables Edit on user bubbles when online, configured and idle.
    canDelete: Boolean, // Enables Delete on user bubbles when idle.
    onRetry: () -> Unit, // Retries the final failed user row.
    onEdit: () -> Unit, // Opens the rewrite dialog for this user bubble.
    onCopy: () -> Unit, // Copies this bubble's text to the clipboard.
    onDelete: () -> Unit, // Opens confirmation before deleting this turn and later turns.
) { // Starts the role-specific bubble layout.
    val user = message.role == Role.USER // Chooses author alignment and theme colours.
    var menuOpen by remember(message.id) { mutableStateOf(false) } // Controls the long-press context menu for this bubble.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) { // Separates user and mentor sides.
        Box { // Anchors the DropdownMenu directly to this bubble.
            Surface( // Draws one role-specific saved text bubble.
                modifier = Modifier
                    .widthIn(max = 480.dp) // Limits long lines on larger devices while allowing narrow screens to shrink.
                    .clip(MaterialTheme.shapes.medium) // Clips long-press feedback to the bubble corners.
                    .combinedClickable( // Opens the context menu on long-press without interfering with scrolling.
                        onClick = {}, // Leaves ordinary taps harmless.
                        onLongClick = { menuOpen = true }, // Reveals Edit/Copy/Delete on user turns or Copy on model turns.
                    ), // Ends long-press handler.
                shape = MaterialTheme.shapes.medium, // Uses the app's existing rounded shape.
                color = if (user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer, // Uses theme-provided contrast for each participant.
            ) { // Displays saved content and any persisted failure.
                Column(Modifier.padding(DroidMentorSpacing.medium), verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small)) { // Groups author, status, text and error feedback.
                    Row(horizontalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small), verticalAlignment = Alignment.CenterVertically) { // Displays participant alongside per-message status.
                        Text(stringResource(if (user) R.string.chat_you else R.string.chat_mentor), style = MaterialTheme.typography.labelMedium) // Identifies the stored participant.
                        if (user) MessageStatusBadge(message.status) // Renders distinct SENDING, SENT or FAILED indicator for each user bubble.
                    } // Ends bubble header row.
                    Text(message.text) // Displays full saved message text without altering history.
                    if (user && message.status == MessageStatus.FAILED) { // Reads failures from Room rather than a network event.
                        Text( // Translates the saved category into safe readable feedback.
                            text = stringResource((message.errorCategory ?: MessageErrorCategory.UNKNOWN).messageResource()), // Resolves error category string.
                            style = MaterialTheme.typography.bodySmall, // Uses compact explanatory style inside the bubble.
                            color = MaterialTheme.colorScheme.error, // Highlights persisted failure clearly.
                        ) // Ends failure explanation text.
                        if (showRetry) Button(onClick = onRetry, enabled = retryEnabled) { Text(stringResource(R.string.chat_retry)) } // Retries the same final failed row through the repository.
                    } // Ends persisted error content.
                } // Ends bubble content.
            } // Ends the role-specific bubble surface.
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) { // Lists context actions for this message.
                if (user) { // Exposes Edit and Delete only on user-authored messages.
                    DropdownMenuItem( // Starts conversation rewrite from this user turn.
                        text = { Text(stringResource(R.string.message_action_edit)) }, // Labels Edit action.
                        enabled = canEdit, // Blocks editing when offline, unconfigured or sending.
                        onClick = { menuOpen = false; onEdit() }, // Closes menu and opens rewrite dialog.
                    ) // Ends Edit menu item.
                } // Ends user-only Edit check.
                DropdownMenuItem( // Copies message text on both user and model bubbles.
                    text = { Text(stringResource(R.string.message_action_copy)) }, // Labels Copy action.
                    onClick = { menuOpen = false; onCopy() }, // Closes menu and copies to clipboard.
                ) // Ends Copy menu item.
                if (user) { // Exposes tail-safe Delete on user-authored messages.
                    DropdownMenuItem( // Removes this user message and later exchanges.
                        text = { Text(stringResource(R.string.message_action_delete)) }, // Labels Delete action.
                        enabled = canDelete, // Blocks deleting while a send owns the chat.
                        onClick = { menuOpen = false; onDelete() }, // Closes menu and asks for confirmation.
                    ) // Ends Delete menu item.
                } // Ends user-only Delete check.
            } // Ends bubble context menu.
        } // Ends anchored menu container.
    } // Ends saved-row alignment.
} // Ends the message bubble.

@Composable // Displays distinct SENDING, SENT or FAILED indicator inside a user bubble.
private fun MessageStatusBadge(status: MessageStatus) { // Keeps per-message state rendering single-purpose and readable.
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { // Aligns icon/progress with compact text label.
        when (status) { // Renders each saved message status distinctly.
            MessageStatus.SENDING -> { // Shows active wait spinner while the attempt is running.
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp) // Marks in-flight request clearly.
                Text(stringResource(R.string.message_status_sending), style = MaterialTheme.typography.labelSmall) // Explains sending state.
            } // Ends SENDING indicator.
            MessageStatus.SENT -> { // Shows settled confirmation check mark.
                Icon(painter = painterResource(R.drawable.ic_check), contentDescription = stringResource(R.string.message_status_sent), modifier = Modifier.size(14.dp)) // Marks sent turn.
                Text(stringResource(R.string.message_status_sent), style = MaterialTheme.typography.labelSmall) // Displays compact sent label.
            } // Ends SENT indicator.
            MessageStatus.FAILED -> { // Shows error icon and colour highlight for failed turns.
                Icon(painter = painterResource(R.drawable.ic_error), contentDescription = stringResource(R.string.message_status_failed), modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error) // Marks failure visually.
                Text(stringResource(R.string.message_status_failed), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) // Highlights failure status text.
            } // Ends FAILED indicator.
        } // Ends per-status badge switch.
    } // Ends badge layout.
} // Ends MessageStatusBadge.

@Composable // Displays the pre-filled rewrite dialog with an explicit truncation warning.
private fun RewriteMessageDialog( // Requires confirmation before truncating later messages.
    initialText: String, // Pre-fills the original user message text.
    onConfirm: (String) -> Unit, // Delivers edited nonblank text to ActiveChatViewModel.
    onDismiss: () -> Unit, // Cancels rewriting without modifying history.
) { // Starts rewrite dialog.
    var draft by remember(initialText) { mutableStateOf(initialText) } // Holds edited text locally in memory.
    val valid = draft.isNotBlank() // Prevents submitting blank replacement text.
    AlertDialog( // Presents the warning and editable prompt clearly.
        onDismissRequest = onDismiss, // Cancels on outside tap or Back.
        title = { Text(stringResource(R.string.message_edit_dialog_title)) }, // Summarises edit and resend.
        text = { // Groups explicit warning above the prefilled message field.
            Column(verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.medium)) { // Separates warning from input.
                Text(stringResource(R.string.message_edit_warning), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) // Warns clearly that later turns will be deleted.
                OutlinedTextField( // Pre-fills the editable message prompt.
                    value = draft, // Shows current edited draft.
                    onValueChange = { draft = it }, // Updates local draft on keystroke.
                    modifier = Modifier.fillMaxWidth(), // Uses dialog width.
                    label = { Text(stringResource(R.string.message_edit_label)) }, // Uses a distinct label from the bottom draft field.
                    maxLines = 6, // Keeps longer questions readable inside the dialog.
                ) // Ends edited message field.
            } // Ends dialog body.
        }, // Ends dialog text slot.
        confirmButton = { // Confirms truncation and resend.
            TextButton(onClick = { onConfirm(draft) }, enabled = valid) { Text(stringResource(R.string.message_edit_confirm)) } // Labels Resend action.
        }, // Ends confirm slot.
        dismissButton = { // Cancels without changing Room.
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.history_delete_cancel)) } // Reuses Cancel label.
        }, // Ends dismiss slot.
    ) // Ends AlertDialog.
} // Ends rewrite message dialog.

package pt.isel.pdm.droidmentor.ui.screens.activechat // Groups the selected conversation's Room-backed UI.

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
import androidx.compose.material3.Button // Provides explicit send and same-row retry actions.
import androidx.compose.material3.CircularProgressIndicator // Displays loading and in-flight progress.
import androidx.compose.material3.MaterialTheme // Supplies bubble colours, shapes and text styles.
import androidx.compose.material3.OutlinedTextField // Displays the controlled in-memory draft.
import androidx.compose.material3.SnackbarHostState // Displays safe local command feedback.
import androidx.compose.material3.Surface // Draws a role-specific message bubble.
import androidx.compose.material3.Text // Displays saved text and resource-backed labels.
import androidx.compose.runtime.Composable // Describes the conversation UI.
import androidx.compose.runtime.LaunchedEffect // Scrolls when the saved message list changes.
import androidx.compose.runtime.getValue // Reads lifecycle-aware Compose state delegates.
import androidx.compose.runtime.remember // Owns the factory and feedback state for this selected ID.
import androidx.compose.ui.Alignment // Centres loading states and aligns draft controls.
import androidx.compose.ui.Modifier // Describes layout size, padding and keyboard handling.
import androidx.compose.ui.platform.LocalContext // Resolves safe notice strings in suspend event handlers.
import androidx.compose.ui.res.stringResource // Reads all visible action and error labels.
import androidx.compose.ui.unit.dp // Sizes bubbles and the small waiting indicator.
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Observes immutable state only while this destination is active.
import androidx.lifecycle.viewmodel.compose.viewModel // Uses the selected conversation's manual factory.
import pt.isel.pdm.droidmentor.R // Supplies message, action and feedback resources.
import pt.isel.pdm.droidmentor.domain.Message // Reads text and error labels only from persisted domain messages.
import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Supplies a safe fallback for inconsistent legacy error metadata.
import pt.isel.pdm.droidmentor.domain.MessageStatus // Distinguishes pending and failed saved turns.
import pt.isel.pdm.droidmentor.domain.Role // Chooses bubble alignment and author labels.
import pt.isel.pdm.droidmentor.ui.components.EventEffect // Collects local feedback with the destination lifecycle.
import pt.isel.pdm.droidmentor.ui.components.ScreenScaffold // Supplies the saved title and explicit Back action.
import pt.isel.pdm.droidmentor.ui.components.UiEvent // Distinguishes safe local feedback events.
import pt.isel.pdm.droidmentor.ui.components.messageResource // Resolves typed local notices into UI resources.
import pt.isel.pdm.droidmentor.ui.theme.DroidMentorSpacing // Reuses the shared spacing values.

/** Displays Room-owned messages and explicit commands for the local conversation selected by navigation. */
@Composable // Keeps networking and persistence inside the injected ViewModel and repository.
fun ActiveChatScreen(chatId: String, onBack: () -> Unit) { // Receives only the local navigation argument and return event.
    val factory = remember(chatId) { ActiveChatViewModel.factory(chatId) } // Captures the ID in a manually written constructor binding.
    val model: ActiveChatViewModel = viewModel(key = chatId, factory = factory) // Keeps different local IDs separate even if Navigation reuses the top destination owner.
    val state by model.state.collectAsStateWithLifecycle() // Observes only immutable Room-derived message state.
    val snackbar = remember { SnackbarHostState() } // Owns local feedback for this conversation.
    val context = LocalContext.current // Resolves safe labels without putting Context in the ViewModel.
    val listState = rememberLazyListState() // Preserves ordinary list scroll state.
    EventEffect(model.events) { event -> // Handles prerequisite or persistence feedback outside the layout.
        if (event is UiEvent.Notice) snackbar.showSnackbar(context.getString(event.notice.messageResource())) // Displays no direct API payload or raw exception text.
    } // Ends local event collection.
    LaunchedEffect(state.messages.size) { // Reacts only when saved rows are added or removed.
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex) // Keeps the latest saved turn in view without inventing a model reply.
    } // Ends saved-message scroll handling.
    ScreenScaffold(state.chat?.title ?: stringResource(R.string.navigation_active_chat), onBack, snackbar) { padding -> // Displays saved conversation metadata.
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
            if (state.settingsError || !state.hasKey) { // Explains the separate key prerequisite without reading its value.
                Text(stringResource(if (state.settingsError) R.string.notice_settings_error else R.string.notice_missing_key), modifier = Modifier.padding(DroidMentorSpacing.medium)) // Keeps configuration feedback visible while the draft remains editable.
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
                                onRetry = model::retry, // Delegates same-row retry validation to the ViewModel and repository.
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

@Composable // Renders one saved row without owning networking or persistence.
private fun MessageBubble(message: Message, showRetry: Boolean, retryEnabled: Boolean, onRetry: () -> Unit) { // Receives the same-row retry callback from the screen.
    val user = message.role == Role.USER // Chooses author alignment and theme colours.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) { // Separates user and mentor sides.
        Surface( // Draws one role-specific saved text bubble.
            modifier = Modifier.widthIn(max = 480.dp), // Limits long lines on larger devices while allowing narrow screens to shrink.
            shape = MaterialTheme.shapes.medium, // Uses the app's existing rounded shape.
            color = if (user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer, // Uses theme-provided contrast for each participant.
        ) { // Displays saved content and any persisted failure.
            Column(Modifier.padding(DroidMentorSpacing.medium), verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small)) { // Groups author, text and error feedback.
                Text(stringResource(if (user) R.string.chat_you else R.string.chat_mentor), style = MaterialTheme.typography.labelMedium) // Identifies the stored participant.
                Text(message.text) // Displays full saved message text without altering history.
                if (user && message.status == MessageStatus.FAILED) { // Reads failures from Room rather than a network event.
                    Text(stringResource((message.errorCategory ?: MessageErrorCategory.UNKNOWN).messageResource())) // Translates the saved category into safe readable feedback.
                    if (showRetry) Button(onClick = onRetry, enabled = retryEnabled) { Text(stringResource(R.string.chat_retry)) } // Retries the same final failed row through the repository.
                } // Ends persisted error content.
            } // Ends bubble content.
        } // Ends the role-specific bubble surface.
    } // Ends saved-row alignment.
} // Ends the message bubble.

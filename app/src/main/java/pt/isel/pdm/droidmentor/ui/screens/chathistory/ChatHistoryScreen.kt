package pt.isel.pdm.droidmentor.ui.screens.chathistory // Groups the Room-backed conversation list.

import android.text.format.DateUtils // Formats relative saved timestamps using the device locale.
import androidx.compose.foundation.layout.Arrangement // Separates saved cards and empty-state content.
import androidx.compose.foundation.layout.Column // Groups a chat's title, preview and date.
import androidx.compose.foundation.layout.PaddingValues // Reserves list space around cards and above the floating action.
import androidx.compose.foundation.layout.fillMaxSize // Uses the screen's body area.
import androidx.compose.foundation.layout.fillMaxWidth // Gives saved cards a usable tap area.
import androidx.compose.foundation.layout.padding // Applies app bar and card spacing.
import androidx.compose.foundation.layout.size // Sizes the decorative empty-history mark.
import androidx.compose.foundation.lazy.LazyColumn // Displays saved history without laying out all rows at once.
import androidx.compose.foundation.lazy.items // Uses stable local IDs as list keys.
import androidx.compose.material3.Card // Makes each saved conversation a clear selectable item.
import androidx.compose.material3.CircularProgressIndicator // Shows the initial local query.
import androidx.compose.material3.FloatingActionButton // Provides the required new-conversation action.
import androidx.compose.material3.Icon // Reuses the local conversation mark for the empty state.
import androidx.compose.material3.MaterialTheme // Uses the shared typography and colours.
import androidx.compose.material3.SnackbarHostState // Displays safe local command feedback.
import androidx.compose.material3.Text // Displays saved metadata and resource-backed labels.
import androidx.compose.runtime.Composable // Describes the list and its empty state.
import androidx.compose.runtime.getValue // Reads the lifecycle-aware state delegate.
import androidx.compose.runtime.remember // Owns one snackbar state for this screen.
import androidx.compose.ui.Alignment // Centres the empty-state illustration and label.
import androidx.compose.ui.Modifier // Describes list and card layout.
import androidx.compose.ui.platform.LocalContext // Resolves snackbar resource text in event handlers.
import androidx.compose.ui.res.painterResource // Loads the existing vector mark.
import androidx.compose.ui.res.stringResource // Resolves visible navigation and list labels.
import androidx.compose.ui.text.style.TextOverflow // Keeps previews short without altering saved message text.
import androidx.compose.ui.unit.dp // Reserves space above the floating action and sizes the illustration.
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Observes saved summaries while the destination is active.
import androidx.lifecycle.viewmodel.compose.viewModel // Uses the history model's manual factory and screen owner.
import pt.isel.pdm.droidmentor.R // Supplies shared and history-specific resources.
import pt.isel.pdm.droidmentor.ui.components.EventEffect // Consumes navigation and feedback events only while visible.
import pt.isel.pdm.droidmentor.ui.components.ScreenScaffold // Supplies the app bar, Back action, feedback and floating action.
import pt.isel.pdm.droidmentor.ui.components.UiEvent // Distinguishes new-chat navigation from local feedback.
import pt.isel.pdm.droidmentor.ui.components.messageResource // Converts safe typed notices to strings.
import pt.isel.pdm.droidmentor.ui.theme.DroidMentorSpacing // Uses the shared screen spacing.

/** Displays saved conversation summaries and opens only locally created or selected chat IDs. */
@Composable // Contains layout and UI callbacks, while the ViewModel owns local commands.
fun ChatHistoryScreen( // Receives only the navigation actions required by history.
    onOpenChat: (String) -> Unit, // Opens a selected or successfully persisted local conversation.
    onBack: () -> Unit, // Returns to the existing Title entry.
    model: ChatHistoryViewModel = viewModel(factory = ChatHistoryViewModel.Factory), // Resolves the shared repository through the manual container.
) { // Starts saved history content.
    val state by model.state.collectAsStateWithLifecycle() // Displays only the repository's Room-derived list.
    val snackbar = remember { SnackbarHostState() } // Owns safe feedback for this destination.
    val context = LocalContext.current // Resolves notice strings inside suspend event handling.
    val newTitle = stringResource(R.string.history_new_title) // Supplies a translated initial title to the local creation command.
    EventEffect(model.events) { event -> // Handles command completion without inventing a saved row.
        when (event) { // Separates navigation from feedback.
            is UiEvent.OpenChat -> onOpenChat(event.chatId) // Navigates only after the repository acknowledges persistence.
            is UiEvent.Notice -> snackbar.showSnackbar(context.getString(event.notice.messageResource())) // Displays only a safe local failure label.
        } // Ends history event handling.
    } // Ends lifecycle-aware event collection.
    ScreenScaffold( // Supplies the history title and its return path.
        title = stringResource(R.string.navigation_chat_history), // Uses the shared destination label.
        onBack = onBack, // Keeps controller access outside the screen.
        snackbarHostState = snackbar, // Connects command feedback to this frame.
        floatingActionButton = { // Supplies one explicit local creation action.
            FloatingActionButton(onClick = { model.createChat(newTitle) }) { // The ViewModel rejects repeated taps while creation is pending.
                Text(stringResource(if (state.creating) R.string.history_creating else R.string.history_new_chat), modifier = Modifier.padding(DroidMentorSpacing.medium)) // Displays clear action or progress text.
            } // Ends the new-conversation action.
        }, // Ends the floating action slot.
    ) { padding -> // Receives space reserved for the app and system bars.
        when { // Distinguishes initial load, failed read, empty history and a saved list.
            state.loading -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator() } // Shows progress until the first local snapshot.
            state.storageError -> Text(stringResource(R.string.history_read_error), modifier = Modifier.padding(padding).padding(DroidMentorSpacing.large)) // Never disguises unreadable storage as empty history.
            state.chats.isEmpty() -> Column( // Displays a small illustrated empty state without fake sample rows.
                modifier = Modifier.fillMaxSize().padding(padding).padding(DroidMentorSpacing.large), // Respects the app bar and outer screen padding.
                horizontalAlignment = Alignment.CenterHorizontally, // Centres the empty-history message.
                verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.medium, Alignment.CenterVertically), // Separates the decorative mark and readable text.
            ) { // Displays the simple empty-history content.
                Icon(painterResource(R.drawable.ic_mentor), contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) // Reuses a decorative local vector rather than an external image.
                Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.titleMedium) // Explains how to start using saved conversations.
            } // Ends the empty-history layout.
            else -> LazyColumn( // Displays locally saved summaries with stable selection IDs.
                modifier = Modifier.fillMaxSize().padding(padding), // Uses only the body area supplied by the scaffold.
                contentPadding = PaddingValues(start = DroidMentorSpacing.medium, end = DroidMentorSpacing.medium, top = DroidMentorSpacing.medium, bottom = 96.dp), // Keeps the last row clear of the floating action.
                verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.medium), // Separates selectable conversation cards.
            ) { // Adds one card per saved local conversation.
                items(state.chats, key = { it.chat.id }) { summary -> // Preserves list item identity when Room emits a new order.
                    Card(onClick = { onOpenChat(summary.chat.id) }, modifier = Modifier.fillMaxWidth()) { // Passes only the selected Room ID to navigation.
                        Column(Modifier.padding(DroidMentorSpacing.medium), verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small)) { // Groups this chat's saved metadata.
                            Text(summary.chat.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) // Displays the saved title without altering storage.
                            Text(summary.lastMessage?.text ?: stringResource(R.string.history_no_messages), maxLines = 2, overflow = TextOverflow.Ellipsis) // Shows a short preview while keeping full saved text untouched.
                            Text(DateUtils.getRelativeTimeSpanString(summary.chat.updatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(), style = MaterialTheme.typography.bodySmall) // Formats only the persisted recency timestamp.
                        } // Ends the saved metadata layout.
                    } // Ends the selected conversation card.
                } // Ends saved summary items.
            } // Ends the saved history list.
        } // Ends local read-state handling.
    } // Ends the history screen frame.
} // Ends the Room-backed history screen.

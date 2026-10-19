package pt.isel.pdm.droidmentor.ui.screens.settings // Groups the key configuration screen.

import androidx.compose.foundation.layout.Arrangement // Separates status, input and actions.
import androidx.compose.foundation.layout.Column // Uses a simple vertical form.
import androidx.compose.foundation.layout.fillMaxSize // Uses the screen's content area.
import androidx.compose.foundation.layout.fillMaxWidth // Gives the key field and actions a usable width.
import androidx.compose.foundation.layout.padding // Applies reserved and shared screen spacing.
import androidx.compose.foundation.rememberScrollState // Keeps the form's scroll position during recomposition.
import androidx.compose.foundation.text.KeyboardOptions // Selects a password keyboard without autocomplete input suggestions.
import androidx.compose.foundation.verticalScroll // Keeps all actions reachable with a larger font or smaller height.
import androidx.compose.material3.Button // Provides the explicit save action.
import androidx.compose.material3.CircularProgressIndicator // Indicates an initial read or write in progress.
import androidx.compose.material3.OutlinedButton // Provides the explicit clear action.
import androidx.compose.material3.OutlinedTextField // Displays the controlled in-memory key draft.
import androidx.compose.material3.SnackbarHostState // Displays one-shot safe feedback.
import androidx.compose.material3.Text // Displays resource-backed form labels.
import androidx.compose.material3.TextButton // Toggles visibility for newly entered input.
import androidx.compose.runtime.Composable // Describes the form's UI.
import androidx.compose.runtime.getValue // Reads Compose state delegates.
import androidx.compose.runtime.mutableStateOf // Holds the visibility toggle locally in memory.
import androidx.compose.runtime.remember // Retains visibility and snackbar state without saving the key.
import androidx.compose.runtime.setValue // Updates the local visibility toggle.
import androidx.compose.ui.Modifier // Describes form layout.
import androidx.compose.ui.platform.LocalContext // Resolves snackbar strings inside a suspend event handler.
import androidx.compose.ui.res.stringResource // Resolves visible form text.
import androidx.compose.ui.text.input.KeyboardType // Uses password input semantics for the keyboard.
import androidx.compose.ui.text.input.PasswordVisualTransformation // Masks newly typed input by default.
import androidx.compose.ui.text.input.VisualTransformation // Displays the draft only after an explicit visibility toggle.
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Observes state only while the destination is active.
import androidx.lifecycle.viewmodel.compose.viewModel // Obtains the owner-scoped model using its manual factory.
import pt.isel.pdm.droidmentor.R // Supplies form strings and safe notices.
import pt.isel.pdm.droidmentor.ui.components.EventEffect // Collects notices with the screen lifecycle.
import pt.isel.pdm.droidmentor.ui.components.ScreenScaffold // Supplies the title, Back action and snackbar host.
import pt.isel.pdm.droidmentor.ui.components.UiEvent // Distinguishes safe feedback from unrelated event types.
import pt.isel.pdm.droidmentor.ui.components.messageResource // Converts typed feedback into safe Android resources.
import pt.isel.pdm.droidmentor.ui.theme.DroidMentorSpacing // Reuses the shared screen spacing.

/** Displays explicit Preferences DataStore commands without pre-filling or retaining the stored key in UI state. */
@Composable // Keeps layout and events separate from storage operations.
fun SettingsScreen( // Receives navigation and an owner-scoped, manually constructed model.
    onBack: () -> Unit, // Returns through the graph's existing entry.
    model: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory), // Resolves the injected repository through Application extras.
) { // Starts the settings UI.
    val state by model.state.collectAsStateWithLifecycle() // Observes immutable state without directly reading preferences.
    val snackbar = remember { SnackbarHostState() } // Owns feedback for this screen composition.
    val context = LocalContext.current // Resolves safe feedback labels without storing an Activity in a ViewModel.
    var visible by remember(state.showKeyInput) { mutableStateOf(false) } // Masks every newly opened form without persisting its toggle or key.
    EventEffect(model.events) { event -> // Handles one-shot feedback outside the layout.
        if (event is UiEvent.Notice) snackbar.showSnackbar(context.getString(event.notice.messageResource())) // Displays only a typed safe notice.
    } // Ends settings event collection.
    ScreenScaffold(stringResource(R.string.navigation_settings), onBack, snackbar) { padding -> // Provides the form's frame and return path.
        Column( // Arranges one small configuration form.
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(DroidMentorSpacing.large), // Keeps the complete form reachable inside the reserved screen area.
            verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.medium), // Separates fields and tap targets.
        ) { // Displays presence without displaying the stored credential.
            val status = when { // Distinguishes an unknown status from a genuinely missing key.
                state.loading -> R.string.settings_loading // Waits for the stored presence before asking for input.
                state.storageError -> R.string.notice_settings_error // Does not claim the key is missing after a failed read.
                state.configured -> R.string.settings_configured // Confirms the saved key can already be used.
                else -> R.string.settings_not_configured // Requests initial setup only after confirming absence.
            } // Ends the safe status selection.
            Text(stringResource(status)) // Displays no saved credential value.
            if (!state.loading && !state.storageError) { // Explains the action only after reading preferences successfully.
                Text(stringResource(if (state.configured) R.string.settings_configured_hint else R.string.settings_setup_hint)) // Makes clear that a saved key does not need to be entered again.
            } // Ends the settings explanation.
            if (state.configured && !state.editing && !state.loading && !state.storageError) { // Keeps replacement optional for an existing configuration.
                Button(onClick = model::beginEdit, enabled = state.canChangeKey, modifier = Modifier.fillMaxWidth()) { // Opens a new draft without changing the saved key.
                    Text(stringResource(R.string.settings_change)) // Labels the explicit replacement action.
                } // Ends the change-key button.
            } // Ends the configured summary action.
            if (state.showKeyInput) { // Shows an empty input only for initial setup or an explicit replacement.
                OutlinedTextField( // Holds newly typed input only in the ViewModel's in-memory draft.
                    value = state.draft, // Never reads or pre-fills the configured key.
                    onValueChange = model::updateDraft, // Delegates draft changes to the guarded ViewModel event.
                    modifier = Modifier.fillMaxWidth(), // Gives long credential input a usable form width.
                    label = { Text(stringResource(if (state.configured) R.string.settings_new_key_label else R.string.settings_key_label)) }, // Distinguishes a replacement from the already saved key.
                    enabled = !state.saving, // Prevents input races with completed writes.
                    singleLine = true, // Keeps a key as one input line.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // Uses password input semantics.
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), // Masks the draft unless the user explicitly reveals it.
                    trailingIcon = { // Gives the user an explicit visibility action.
                        TextButton(onClick = { visible = !visible }, enabled = !state.saving && state.draft.isNotEmpty()) { // Changes visibility only for newly entered input, never storage.
                            Text(stringResource(if (visible) R.string.settings_hide else R.string.settings_show)) // Labels the next visibility action.
                        } // Ends the visibility button.
                    }, // Ends the password field's toggle.
                ) // Ends new-key input.
                if (state.configured) Text(stringResource(R.string.settings_replace_hint)) // Explains that the existing key stays active until replacement succeeds.
                Button(onClick = model::save, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) { // Uses the ViewModel's guarded save availability.
                    Text(stringResource(if (state.configured) R.string.settings_replace else R.string.settings_save)) // Names the initial save or explicit replacement.
                } // Ends key saving.
                if (state.configured) { // Allows a replacement to be abandoned without losing the stored key.
                    TextButton(onClick = model::cancelEdit, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) { // Discards only unsaved in-memory input.
                        Text(stringResource(R.string.settings_cancel)) // Labels the safe return to the saved status.
                    } // Ends the replacement cancel action.
                } // Ends replacement-only controls.
            } // Ends the optional new-key form.
            if (state.configured && !state.loading && !state.storageError) { // Offers removal only for a known saved key.
                OutlinedButton(onClick = model::clear, enabled = state.canChangeKey, modifier = Modifier.fillMaxWidth()) { // Removes only the configured credential through the repository.
                    Text(stringResource(R.string.settings_clear)) // Names the explicit clear action.
                } // Ends key clearing.
            } // Ends the stored-key removal action.
            if (state.loading || state.saving) CircularProgressIndicator() // Shows storage progress without printing input or exception details.
        } // Ends the scrollable configuration form.
    } // Ends the settings screen frame.
} // Ends the key configuration screen.

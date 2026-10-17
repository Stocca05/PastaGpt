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
    var visible by remember { mutableStateOf(false) } // Starts newly entered input masked and never persists this toggle or key.
    EventEffect(model.events) { event -> // Handles one-shot feedback outside the layout.
        if (event is UiEvent.Notice) snackbar.showSnackbar(context.getString(event.notice.messageResource())) // Displays only a typed safe notice.
    } // Ends settings event collection.
    ScreenScaffold(stringResource(R.string.navigation_settings), onBack, snackbar) { padding -> // Provides the form's frame and return path.
        Column( // Arranges one small configuration form.
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(DroidMentorSpacing.large), // Keeps the complete form reachable inside the reserved screen area.
            verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.medium), // Separates fields and tap targets.
        ) { // Displays presence without displaying the stored credential.
            Text(stringResource(if (state.configured) R.string.settings_configured else R.string.settings_not_configured)) // Shows only whether configuration exists.
            if (state.storageError) Text(stringResource(R.string.notice_settings_error)) // Distinguishes a read failure from an unconfigured key.
            OutlinedTextField( // Holds newly typed input only in the ViewModel's in-memory draft.
                value = state.draft, // Never reads or pre-fills the configured key.
                onValueChange = model::updateDraft, // Delegates draft changes to the guarded ViewModel event.
                modifier = Modifier.fillMaxWidth(), // Gives long credential input a usable form width.
                label = { Text(stringResource(R.string.settings_key_label)) }, // Names the input field.
                enabled = !state.loading && !state.saving, // Prevents input races with initial reads or completed writes.
                singleLine = true, // Keeps a key as one input line.
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // Uses password input semantics.
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), // Masks the draft unless the user explicitly reveals it.
                trailingIcon = { // Gives the user an explicit visibility action.
                    TextButton(onClick = { visible = !visible }) { // Changes visibility only, never storage.
                        Text(stringResource(if (visible) R.string.settings_hide else R.string.settings_show)) // Labels the next visibility action.
                    } // Ends the visibility button.
                }, // Ends the password field's toggle.
            ) // Ends new-key input.
            Button(onClick = model::save, enabled = !state.loading && !state.saving && state.draft.isNotBlank(), modifier = Modifier.fillMaxWidth()) { // Requires nonblank input before the explicit save command.
                Text(stringResource(R.string.settings_save)) // Names the storage action.
            } // Ends key saving.
            OutlinedButton(onClick = model::clear, enabled = !state.loading && !state.saving && state.configured, modifier = Modifier.fillMaxWidth()) { // Removes only the configured credential through the repository.
                Text(stringResource(R.string.settings_clear)) // Names the explicit clear action.
            } // Ends key clearing.
            if (state.loading || state.saving) CircularProgressIndicator() // Shows storage progress without printing input or exception details.
        } // Ends the scrollable configuration form.
    } // Ends the settings screen frame.
} // Ends the key configuration screen.

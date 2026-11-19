package pt.isel.pdm.droidmentor.ui.screens.settings // Groups the key configuration screen.

import androidx.compose.foundation.layout.Arrangement // Separates status card, input card and actions cleanly.
import androidx.compose.foundation.layout.Column // Uses clear vertical form layout.
import androidx.compose.foundation.layout.Row // Aligns status icon beside status headline cleanly.
import androidx.compose.foundation.layout.fillMaxSize // Uses screen content area.
import androidx.compose.foundation.layout.fillMaxWidth // Gives cards and actions full width.
import androidx.compose.foundation.layout.height // Gives primary action buttons comfortable >=48dp touch height.
import androidx.compose.foundation.layout.padding // Applies consistent outer and card padding.
import androidx.compose.foundation.layout.size // Sizes status badge icon neatly.
import androidx.compose.foundation.rememberScrollState // Keeps form scroll position across recomposition.
import androidx.compose.foundation.shape.RoundedCornerShape // Rounds status card, field and buttons smoothly.
import androidx.compose.foundation.text.KeyboardOptions // Uses password keyboard without autocomplete suggestions.
import androidx.compose.foundation.verticalScroll // Keeps form reachable on landscape or large font sizes.
import androidx.compose.material3.Button // Primary save action.
import androidx.compose.material3.CircularProgressIndicator // Shows storage read/write progress.
import androidx.compose.material3.ElevatedCard // Groups key configuration form cleanly.
import androidx.compose.material3.Icon // Displays visual configured/warning status icon.
import androidx.compose.material3.MaterialTheme // Uses shared colour scheme and typography.
import androidx.compose.material3.OutlinedButton // Secondary clear-key action.
import androidx.compose.material3.OutlinedTextField // Controlled in-memory draft input.
import androidx.compose.material3.SnackbarHostState // Hosts safe typed notices.
import androidx.compose.material3.Surface // Colours status card clearly based on configuration state.
import androidx.compose.material3.Text // Displays resource-backed labels.
import androidx.compose.material3.TextButton // Toggles draft visibility or cancels replacement.
import androidx.compose.runtime.Composable // Describes UI declaratively.
import androidx.compose.runtime.getValue // Reads lifecycle state cleanly.
import androidx.compose.runtime.mutableStateOf // Holds transient visibility flag in memory.
import androidx.compose.runtime.remember // Retains UI-only toggles without persisting credentials.
import androidx.compose.runtime.setValue // Toggles draft visibility cleanly.
import androidx.compose.ui.Alignment // Aligns status icon with heading.
import androidx.compose.ui.Modifier // Configures layout and sizes.
import androidx.compose.ui.platform.LocalContext // Resolves snackbar labels in suspend collector.
import androidx.compose.ui.res.painterResource // Loads check/error icons cleanly.
import androidx.compose.ui.res.stringResource // Reads visible labels from strings.xml.
import androidx.compose.ui.text.font.FontWeight // Emphasises status headline clearly.
import androidx.compose.ui.text.input.KeyboardType // Uses password input semantics.
import androidx.compose.ui.text.input.PasswordVisualTransformation // Masks draft input by default.
import androidx.compose.ui.text.input.VisualTransformation // Reveals newly typed input only when explicitly requested.
import androidx.compose.ui.unit.dp // Specifies clean Material 3 sizes.
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Observes state while active.
import androidx.lifecycle.viewmodel.compose.viewModel // Builds manual factory instance.
import pt.isel.pdm.droidmentor.R // Supplies string and drawable resources.
import pt.isel.pdm.droidmentor.ui.components.EventEffect // Collects one-shot notices cleanly.
import pt.isel.pdm.droidmentor.ui.components.ScreenScaffold // Provides top bar, Back action and snackbar host.
import pt.isel.pdm.droidmentor.ui.components.UiEvent // Distinguishes safe UI notices.
import pt.isel.pdm.droidmentor.ui.components.messageResource // Maps notices to safe resources.
import pt.isel.pdm.droidmentor.ui.theme.DroidMentorSpacing // Uses shared spacing constants.

/** Displays polished Preferences DataStore controls without pre-filling or retaining the stored key in UI state. */
@Composable // Keeps layout separate from storage operations.
fun SettingsScreen( // Receives navigation and manually constructed SettingsViewModel.
    onBack: () -> Unit, // Returns through the existing back stack entry.
    model: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory), // Uses manual Application factory.
) { // Starts the settings UI.
    val state by model.state.collectAsStateWithLifecycle() // Observes immutable presence state only.
    val snackbar = remember { SnackbarHostState() } // Owns feedback snackbar state.
    val context = LocalContext.current // Resolves notice resource text.
    var visible by remember(state.showKeyInput) { mutableStateOf(false) } // Masks new key drafts initially.
    EventEffect(model.events) { event -> // Shows one-shot safe feedback.
        if (event is UiEvent.Notice) snackbar.showSnackbar(context.getString(event.notice.messageResource())) // Never reveals credential text.
    } // Ends notice collector.
    ScreenScaffold(stringResource(R.string.navigation_settings), onBack, snackbar) { padding -> // Hosts form inside consistent app scaffold.
        Column( // Stacks status card cleanly above optional key form card.
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(DroidMentorSpacing.large), // Scrollable outer body.
            verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.large), // Separates status card and form card.
        ) { // Displays key status banner card and actions.
            val status = when { // Selects resource string for loading, error, configured or missing state.
                state.loading -> R.string.settings_loading // Reading status.
                state.storageError -> R.string.notice_settings_error // Storage failure.
                state.configured -> R.string.settings_configured // Key already configured.
                else -> R.string.settings_not_configured // No key saved yet.
            } // Ends status selection.
            val bannerBg = when { // Chooses subtle readable Material container colour for status card.
                state.storageError -> MaterialTheme.colorScheme.errorContainer // Highlights storage problem clearly.
                state.configured -> MaterialTheme.colorScheme.primaryContainer // Celebrates configured state calmly.
                else -> MaterialTheme.colorScheme.secondaryContainer // Neutral setup prompt container.
            } // Ends container background choice.
            val bannerFg = when { // Pairs exact contrasting onContainer content colour cleanly.
                state.storageError -> MaterialTheme.colorScheme.onErrorContainer // High contrast on errorContainer.
                state.configured -> MaterialTheme.colorScheme.onPrimaryContainer // High contrast on primaryContainer.
                else -> MaterialTheme.colorScheme.onSecondaryContainer // High contrast on secondaryContainer.
            } // Ends container content colour choice.
            Surface( // Frames credential status inside a clear rounded banner card.
                shape = RoundedCornerShape(20.dp), // Smooth Material 3 corners.
                color = bannerBg, // Uses semantic state container colour.
                contentColor = bannerFg, // Guarantees light/dark readability.
                modifier = Modifier.fillMaxWidth(), // Uses full form width.
            ) { // Displays status icon, headline, explanation and Change button.
                Column(modifier = Modifier.padding(DroidMentorSpacing.large), verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.medium)) { // Stacks status header and hint cleanly.
                    Row(horizontalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small), verticalAlignment = Alignment.CenterVertically) { // Aligns icon and heading.
                        if (!state.loading) { // Shows check or warning icon once loaded.
                            Icon( // Displays state icon clearly.
                                painter = painterResource(if (state.configured && !state.storageError) R.drawable.ic_check else R.drawable.ic_error), // Matches saved vs unconfigured/error state.
                                contentDescription = null, // Decorative beside visible status text.
                                modifier = Modifier.size(22.dp), // Crisp status icon size.
                            ) // Ends status icon.
                        } // Ends loaded icon guard.
                        Text(stringResource(status), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) // Displays safe status headline.
                    } // Ends status header row.
                    if (!state.loading && !state.storageError) { // Explains behaviour once loaded cleanly.
                        Text(stringResource(if (state.configured) R.string.settings_configured_hint else R.string.settings_setup_hint), style = MaterialTheme.typography.bodyMedium) // Concise explanation.
                    } // Ends hint guard.
                    if (state.configured && !state.editing && !state.loading && !state.storageError) { // Offers optional replacement trigger.
                        Button(onClick = model::beginEdit, enabled = state.canChangeKey, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) { // Opens empty replacement draft.
                            Text(stringResource(R.string.settings_change)) // Labels replacement action.
                        } // Ends Change key button.
                    } // Ends configured summary action.
                } // Ends status card content.
            } // Ends status Surface card.
            if (state.showKeyInput) { // Displays key input and save controls inside a lifted card.
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) { // Groups new-key field and save/cancel actions cleanly.
                    Column(modifier = Modifier.padding(DroidMentorSpacing.large), verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.medium)) { // Clean vertical form spacing.
                        OutlinedTextField( // Holds newly typed draft only in memory.
                            value = state.draft, // Never pre-fills saved key.
                            onValueChange = model::updateDraft, // Routes edits through ViewModel guard.
                            modifier = Modifier.fillMaxWidth(), // Full card width.
                            shape = RoundedCornerShape(14.dp), // Modern rounded field corners.
                            label = { Text(stringResource(if (state.configured) R.string.settings_new_key_label else R.string.settings_key_label)) }, // Labels input clearly.
                            enabled = !state.saving, // Disables editing during active save.
                            singleLine = true, // Keeps API key single-line.
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // Password keyboard semantics.
                            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), // Masks input by default.
                            trailingIcon = { // Explicit Show/Hide toggle.
                                TextButton(onClick = { visible = !visible }, enabled = !state.saving && state.draft.isNotEmpty()) { // Toggles masking only on typed draft.
                                    Text(stringResource(if (visible) R.string.settings_hide else R.string.settings_show)) // Labels toggle action.
                                } // Ends toggle button.
                            }, // Ends trailing icon.
                        ) // Ends API key field.
                        if (state.configured) Text(stringResource(R.string.settings_replace_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) // Explains safe replacement rule.
                        Button(onClick = model::save, enabled = state.canSave, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) { // Explicit Save CTA.
                            Text(stringResource(if (state.configured) R.string.settings_replace else R.string.settings_save)) // Labels Save or Save replacement.
                        } // Ends save button.
                        if (state.configured) { // Optional cancel action when replacing an existing key.
                            TextButton(onClick = model::cancelEdit, enabled = !state.saving, modifier = Modifier.fillMaxWidth().height(48.dp)) { // Discards only draft.
                                Text(stringResource(R.string.settings_cancel)) // Labels Cancel cleanly.
                            } // Ends cancel button.
                        } // Ends replacement cancel.
                    } // Ends form Column.
                } // Ends ElevatedCard.
            } // Ends optional key form.
            if (state.configured && !state.loading && !state.storageError) { // Explicit Clear action outside form card.
                OutlinedButton(onClick = model::clear, enabled = state.canChangeKey, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) { // Removes stored credential safely.
                    Text(stringResource(R.string.settings_clear)) // Labels Clear key cleanly.
                } // Ends clear button.
            } // Ends clear section.
            if (state.loading || state.saving) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally)) // Centres loading spinner cleanly.
        } // Ends outer Column.
    } // Ends ScreenScaffold.
} // Ends Settings screen.

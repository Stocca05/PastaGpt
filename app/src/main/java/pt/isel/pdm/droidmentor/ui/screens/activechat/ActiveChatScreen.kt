package pt.isel.pdm.droidmentor.ui.screens.activechat // Groups the selected conversation's Room-backed UI.

import android.Manifest // Declares the CAMERA runtime permission required before launching TakePicture.
import android.app.Activity // Resolves Activity for ActivityCompat.shouldShowRequestPermissionRationale.
import android.content.Context // Unwraps ContextWrapper safely to find the host Activity.
import android.content.ContextWrapper // Supports unwrapping Compose context wrappers cleanly.
import android.content.Intent // Opens App Settings directly when camera permission is permanently denied.
import android.content.pm.PackageManager // Checks whether CAMERA permission is already granted.
import android.net.Uri // Builds the package: URI for App Settings navigation.
import android.provider.Settings // Targets Android Application Details Settings on permanent denial.
import androidx.activity.compose.rememberLauncherForActivityResult // Launches Android Photo Picker without runtime storage permissions (M3-11).
import androidx.activity.result.PickVisualMediaRequest // Requests image-only media from the system Photo Picker.
import androidx.activity.result.contract.ActivityResultContracts // Uses PickVisualMedia, TakePicture and RequestPermission contracts.
import androidx.core.app.ActivityCompat // Inspects whether Android recommends showing a camera permission rationale.
import androidx.core.content.ContextCompat // Checks current CAMERA runtime permission status safely.
import androidx.core.content.FileProvider // Exposes app-private filesDir/images/ target cleanly to the Camera app.
import androidx.compose.foundation.BorderStroke // Frames mentor bubbles and images with subtle outlines.
import androidx.compose.foundation.ExperimentalFoundationApi // Enables long-press combinedClickable on message bubbles.
import androidx.compose.foundation.clickable // Opens full-screen viewer when tapping a message image thumbnail.
import androidx.compose.foundation.combinedClickable // Opens the bubble context menu on long-press.
import androidx.compose.foundation.layout.Arrangement // Separates messages and input controls.
import androidx.compose.foundation.layout.Box // Displays loading, empty, error or message content in the same body area.
import androidx.compose.foundation.layout.Column // Places message content above the draft controls.
import androidx.compose.foundation.layout.PaddingValues // Gives the message list consistent spacing.
import androidx.compose.foundation.layout.Row // Aligns role-specific bubbles and draft controls.
import androidx.compose.foundation.layout.Spacer // Separates composer/empty-state elements neatly.
import androidx.compose.foundation.layout.consumeWindowInsets // Avoids applying already reserved system insets twice.
import androidx.compose.foundation.layout.fillMaxSize // Uses the available body area.
import androidx.compose.foundation.layout.fillMaxWidth // Gives rows the full conversation width.
import androidx.compose.foundation.layout.heightIn // Bounds image thumbnail and button height cleanly.
import androidx.compose.foundation.layout.imePadding // Keeps the draft above the visible keyboard.
import androidx.compose.foundation.layout.padding // Applies the existing screen and bubble spacing.
import androidx.compose.foundation.layout.size // Sizes icons, avatars and waiting indicators.
import androidx.compose.foundation.layout.width // Separates Send icon and label cleanly.
import androidx.compose.foundation.layout.widthIn // Keeps message bubbles readable on wider screens.
import androidx.compose.foundation.lazy.LazyColumn // Displays the complete saved message list efficiently.
import androidx.compose.foundation.lazy.items // Uses persisted message IDs as stable item keys.
import androidx.compose.foundation.lazy.rememberLazyListState // Keeps scroll position across ordinary recomposition.
import androidx.compose.foundation.shape.CircleShape // Rounds empty-state mentor emblem cleanly.
import androidx.compose.foundation.shape.RoundedCornerShape // Shapes modern asymmetric chat bubbles and composer pill.
import androidx.compose.material3.AlertDialog // Confirms message rewrite, delete and camera permission dialogs.
import androidx.compose.material3.Button // Provides explicit send and same-row retry actions.
import androidx.compose.material3.CircularProgressIndicator // Displays loading and in-flight progress.
import androidx.compose.material3.DropdownMenu // Hosts the Edit, Copy and Delete actions on long-press.
import androidx.compose.material3.DropdownMenuItem // Renders one context-menu action.
import androidx.compose.material3.FilledTonalButton // Styles contextual Open Settings CTA calmly inside banner.
import androidx.compose.material3.HorizontalDivider // Separates composer bar cleanly from the message feed.
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
import androidx.compose.runtime.saveable.rememberSaveable // Survives Activity recreation while external camera UI is open.
import androidx.compose.runtime.setValue // Opens and closes the manual rename dialog.
import androidx.compose.runtime.snapshotFlow // Observes LazyList scroll position cleanly for SavedStateHandle.
import androidx.compose.ui.Alignment // Centres loading states and aligns draft controls.
import androidx.compose.ui.Modifier // Describes layout size, padding and keyboard handling.
import androidx.compose.ui.draw.clip // Clips long-press ripple and image thumbnails to rounded shapes.
import androidx.compose.ui.layout.ContentScale // Crops thumbnails cleanly and fits full-screen image previews.
import androidx.compose.ui.platform.LocalClipboardManager // Copies message text cleanly to the system clipboard.
import androidx.compose.ui.platform.LocalContext // Resolves safe labels in suspend event handlers.
import androidx.compose.ui.res.painterResource // Loads shared local vector icons.
import androidx.compose.ui.res.stringResource // Reads all visible action and error labels.
import androidx.compose.ui.text.AnnotatedString // Wraps copied text for the Compose clipboard API.
import androidx.compose.ui.text.font.FontWeight // Emphasises participant badge and empty prompt cleanly.
import androidx.compose.ui.text.style.TextAlign // Centres empty conversation welcome text cleanly.
import androidx.compose.ui.unit.dp // Sizes bubbles and small status indicators.
import androidx.compose.ui.window.Dialog // Displays full-screen image previews cleanly.
import androidx.compose.ui.window.DialogProperties // Enables full-bleed full-screen image preview dialog.
import coil.compose.AsyncImage // Loads app-private images asynchronously in composer and full-screen preview.
import coil.compose.SubcomposeAsyncImage // Shows Coil loading placeholder and missing-file fallback inside bubbles.
import java.io.File // References resolved app-private image files safely.
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
    var fullscreenImageFile by remember { mutableStateOf<File?>(null) } // Displays tapped bubble image in full-screen preview.
    var pendingCapturePath by rememberSaveable { mutableStateOf<String?>(null) } // Preserves relative capture target if Activity recreates during camera flow.
    var showCameraRationale by remember { mutableStateOf(false) } // Shows rationale explanation when Android asks before re-prompting.
    var showCameraPermanentDenial by remember { mutableStateOf(false) } // Guides user to App Settings when CAMERA permission is permanently denied.
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> // Opens the Photo Picker without requesting storage permissions.
        if (uri != null) model.attachGalleryImage(uri.toString()) // Copies picked image into app-private filesDir/images/.
    } // Ends Photo Picker launcher.
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { succeeded -> // Commits captured photo or removes empty target file.
        val capturedPath = pendingCapturePath // Reads saved relative target path.
        pendingCapturePath = null // Consumes one-shot capture target.
        if (capturedPath != null) model.completeCameraCapture(capturedPath, succeeded) // Finalizes or cleans captured image file.
    } // Ends TakePicture launcher.
    val launchCamera: () -> Unit = { // Allocates target in filesDir/images/ and opens camera via FileProvider.
        scope.launch { // Reserves target off main thread cleanly.
            val targetPath = model.prepareCameraCaptureTarget() // Creates relative destination path.
            val targetFile = targetPath?.let(model::resolveAttachmentFile) // Resolves app-private File inside filesDir/images/.
            if (targetPath == null || targetFile == null) { // Handles storage preparation error cleanly.
                snackbar.showSnackbar(context.getString(R.string.notice_image_import_failed)) // Shows safe notice without leaking path.
            } else { // Launches camera with tightly scoped FileProvider content URI.
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile) // Grants temporary URI access only to target file.
                pendingCapturePath = targetPath // Saves target path across Activity recreation.
                cameraLauncher.launch(contentUri) // Opens system Camera app.
            } // Ends camera launch branch.
        } // Ends coroutine scope.
    } // Ends camera launch helper.
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> // Handles runtime CAMERA permission response.
        if (granted) { // Launches camera immediately once permission is granted.
            launchCamera() // Starts FileProvider camera capture.
        } else if (shouldShowCameraRationale(context)) { // Explains why camera is needed when Android allows re-prompting.
            showCameraRationale = true // Shows rationale dialog.
        } else { // Recognizes permanent denial ("Don't ask again") when Android suppresses rationale.
            showCameraPermanentDenial = true // Shows App Settings guidance dialog without dead ends.
        } // Ends permission response handling.
    } // Ends permission request launcher.
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
    if (showCameraRationale) { // Explains why CAMERA access is needed before re-prompting.
        AlertDialog( // Presents clear educational rationale per Android permission best practices.
            onDismissRequest = { showCameraRationale = false }, // Dismisses without requesting permission.
            title = { Text(stringResource(R.string.camera_rationale_title)) }, // Summarizes camera request.
            text = { Text(stringResource(R.string.camera_rationale_body)) }, // Explains how captured photos are attached.
            confirmButton = { // Re-triggers system permission dialog on explicit acceptance.
                TextButton(onClick = { // Requests runtime CAMERA permission.
                    showCameraRationale = false // Dismisses rationale dialog.
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA) // Opens Android runtime prompt.
                }) { Text(stringResource(R.string.camera_rationale_continue)) } // Labels allow button.
            }, // Ends confirm button.
            dismissButton = { // Leaves photo capture ungranted cleanly.
                TextButton(onClick = { showCameraRationale = false }) { Text(stringResource(R.string.history_delete_cancel)) } // Cancels smoothly.
            }, // Ends dismiss button.
        ) // Ends camera rationale dialog.
    } // Ends camera rationale check.
    if (showCameraPermanentDenial) { // Guides the user to App Settings when CAMERA permission was permanently denied.
        AlertDialog( // Avoids dead ends when Android hides the permission prompt.
            onDismissRequest = { showCameraPermanentDenial = false }, // Closes dialog cleanly.
            title = { Text(stringResource(R.string.camera_denied_permanently_title)) }, // Reports permanent denial clearly.
            text = { Text(stringResource(R.string.camera_denied_permanently_body)) }, // Tells the user how to re-enable Camera in Settings.
            confirmButton = { // Navigates directly to this app's Android system Settings page.
                TextButton(onClick = { // Opens Application Details Settings.
                    showCameraPermanentDenial = false // Dismisses dialog first.
                    val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)) // Targets this app's permissions screen.
                    context.startActivity(settingsIntent) // Opens Android App Settings.
                }) { Text(stringResource(R.string.camera_open_app_settings)) } // Labels settings shortcut.
            }, // Ends confirm button.
            dismissButton = { // Dismisses cleanly without opening Settings.
                TextButton(onClick = { showCameraPermanentDenial = false }) { Text(stringResource(R.string.history_delete_cancel)) } // Cancels cleanly.
            }, // Ends dismiss button.
        ) // Ends permanent denial dialog.
    } // Ends permanent denial check.
    fullscreenImageFile?.let { file -> // Shows tapped conversation image full screen (M3-14).
        Dialog( // Uses full-bleed dialog so system Back or Close dismisses smoothly.
            onDismissRequest = { fullscreenImageFile = null }, // Closes preview cleanly.
            properties = DialogProperties(usePlatformDefaultWidth = false), // Expands viewer to full screen width/height.
        ) { // Starts full-screen viewer container.
            Surface(color = MaterialTheme.colorScheme.scrim, modifier = Modifier.fillMaxSize()) { // Darkens background for clear photo inspection.
                Box(Modifier.fillMaxSize()) { // Places image centered beneath explicit Close button.
                    AsyncImage( // Renders full saved image using Coil.
                        model = file, // Loads app-private image file.
                        contentDescription = stringResource(R.string.chat_image_fullscreen_cd), // Accessibility label for full-screen preview.
                        contentScale = ContentScale.Fit, // Preserves original aspect ratio inside screen bounds.
                        modifier = Modifier.fillMaxSize().padding(DroidMentorSpacing.large), // Leaves subtle breathing room.
                    ) // Ends AsyncImage preview.
                    TextButton( // Provides explicit accessible Close button in top-right corner.
                        onClick = { fullscreenImageFile = null }, // Dismisses full-screen viewer.
                        modifier = Modifier.align(Alignment.TopEnd).padding(DroidMentorSpacing.medium), // Anchors Close clearly.
                    ) { Text(stringResource(R.string.chat_image_close_fullscreen), color = MaterialTheme.colorScheme.inverseOnSurface) } // Labels Close clearly.
                } // Ends full-screen Box.
            } // Ends full-screen Surface.
        } // Ends full-screen Dialog.
    } // Ends full-screen image check.
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
            if (!state.connected) { // Keeps the offline notice visible inside a soft rounded pill banner.
                Surface( // Uses explicit light/dark contrast container.
                    color = MaterialTheme.colorScheme.errorContainer, // Error container background.
                    contentColor = MaterialTheme.colorScheme.onErrorContainer, // High-contrast text colour.
                    shape = RoundedCornerShape(14.dp), // Soft rounded banner card.
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DroidMentorSpacing.medium, vertical = 4.dp), // Gives breathing room around edges.
                ) { // Places warning icon beside offline banner message.
                    Row(Modifier.padding(DroidMentorSpacing.medium), horizontalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small), verticalAlignment = Alignment.CenterVertically) { // Aligns icon and notice text.
                        Icon(painterResource(R.drawable.ic_error), contentDescription = null, modifier = Modifier.size(18.dp)) // Visual warning cue.
                        Text(stringResource(R.string.notice_offline), style = MaterialTheme.typography.bodyMedium) // Explains new interactions are unavailable while history stays visible.
                    } // Ends offline banner row.
                } // Ends the offline banner.
            } // Ends connectivity feedback.
            if (!state.loading && (state.settingsError || !state.hasKey || state.hasInvalidKeyFailure)) { // Explains missing, unreadable or rejected key with a direct Settings action.
                val keyNotice = when { // Chooses safe contextual copy for missing vs rejected credentials.
                    state.settingsError -> R.string.notice_settings_error // Explains settings storage could not be read.
                    !state.hasKey -> R.string.notice_missing_key // Explains no API key is configured yet.
                    else -> R.string.notice_rejected_key // Explains a 401/403 authentication failure happened.
                } // Ends contextual key message choice.
                Surface( // Frames the guidance and direct Settings button cleanly.
                    color = MaterialTheme.colorScheme.secondaryContainer, // Soft tonal background.
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer, // Explicit light/dark contrast.
                    shape = RoundedCornerShape(14.dp), // Rounded modern banner card.
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DroidMentorSpacing.medium, vertical = 4.dp), // Inset cleanly from edges.
                ) { // Starts contextual banner row.
                    Row( // Places explanation text beside the direct Open Settings shortcut.
                        modifier = Modifier.fillMaxWidth().padding(horizontal = DroidMentorSpacing.medium, vertical = DroidMentorSpacing.small), // Applies comfortable banner padding.
                        horizontalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small), // Separates copy and action button.
                        verticalAlignment = Alignment.CenterVertically, // Centres the action beside wrapping text.
                    ) { // Starts contextual banner row.
                        Text(stringResource(keyNotice), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium) // Keeps configuration feedback visible while the draft stays editable.
                        FilledTonalButton(onClick = onOpenSettings, shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.action_open_settings)) } // Leads straight to Settings cleanly.
                    } // Ends contextual banner row.
                } // Ends key-status banner container.
            } // Ends key-status feedback.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { // Gives the saved message body the remaining screen height.
                when { // Keeps storage errors and missing conversations distinct from empty messages.
                    state.loading -> CircularProgressIndicator() // Waits for the initial Room snapshots.
                    state.storageError -> Text(stringResource(R.string.chat_read_error)) // Reports a local history query failure.
                    state.chat == null -> Text(stringResource(R.string.notice_not_found)) // Reports a conversation removed from local storage.
                    state.messages.isEmpty() -> Column( // Displays welcoming empty mentor badge and prompt.
                        modifier = Modifier.padding(DroidMentorSpacing.large), // Comfortable padding around prompt.
                        horizontalAlignment = Alignment.CenterHorizontally, // Centres emblem and prompt.
                        verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.medium), // Separates emblem and label.
                    ) { // Starts empty conversation hero.
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) { // Soft mentor avatar circle.
                            Box(contentAlignment = Alignment.Center) { // Centres vector mark.
                                Icon(painterResource(R.drawable.ic_mentor), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp)) // Decorative mentor mark.
                            } // Ends Box.
                        } // Ends circular badge.
                        Text(stringResource(R.string.chat_empty), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) // Displays welcome prompt cleanly.
                    } // Ends welcoming empty conversation layout.
                    else -> LazyColumn( // Displays all saved messages in the repository's sequence order.
                        modifier = Modifier.fillMaxSize(), // Uses the body area above the input.
                        state = listState, // Shares the list state with automatic scrolling.
                        contentPadding = PaddingValues(DroidMentorSpacing.medium), // Adds space around message bubbles.
                        verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small), // Separates consecutive turns.
                    ) { // Uses stable local message IDs.
                        items(state.messages, key = { it.id }) { message -> // Renders only rows obtained from Room observation.
                            MessageBubble( // Keeps bubble layout separate from screen and command logic.
                                message = message, // Supplies saved text, role, status, relative imagePath and error category.
                                showRetry = message.id == state.messages.lastOrNull()?.id, // Displays the action only beside the final saved row.
                                retryEnabled = state.canRetry, // Shares the ViewModel's current key, connection and ownership guard.
                                canEdit = state.canRewrite, // Enables rewrite only when online, configured and idle.
                                canDelete = state.canModifyMessages, // Enables local deletion whenever no request is active.
                                resolveImageFile = model::resolveAttachmentFile, // Resolves relative path inside filesDir/images/.
                                onOpenImage = { fullscreenImageFile = it }, // Opens full-screen image preview on thumbnail tap.
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
            Surface( // Wraps composer bar in a calm tonal container separated from messages cleanly.
                color = MaterialTheme.colorScheme.surfaceContainerLow, // Distinct subtle bottom surface.
                tonalElevation = 2.dp, // Gentle Material 3 lift for input controls.
                modifier = Modifier.fillMaxWidth(), // Spans full screen width.
            ) { // Stacks divider, optional pending attachment bar and composer row.
                Column(Modifier.fillMaxWidth()) { // Organizes divider, preview chip and composer controls.
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) // Crisp divider above composer.
                    state.pendingImagePath?.let { path -> // Displays uncommitted gallery/camera thumbnail bar cleanly.
                        val previewFile = remember(path) { model.resolveAttachmentFile(path) } // Resolves app-private image file.
                        Surface( // Highlights attached image inside rounded secondaryContainer card.
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().padding(start = DroidMentorSpacing.medium, end = DroidMentorSpacing.medium, top = DroidMentorSpacing.small),
                        ) { // Groups thumbnail, attached indicator and explicit removal action.
                            Row( // Aligns thumbnail, label and remove CTA cleanly.
                                modifier = Modifier.fillMaxWidth().padding(horizontal = DroidMentorSpacing.medium, vertical = DroidMentorSpacing.small),
                                horizontalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small),
                                verticalAlignment = Alignment.CenterVertically,
                            ) { // Starts pending attachment row.
                                if (previewFile != null && previewFile.exists()) { // Shows compact Coil thumbnail when file exists.
                                    AsyncImage(
                                        model = previewFile,
                                        contentDescription = stringResource(R.string.chat_image_attachment_cd),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                                    ) // Ends composer thumbnail.
                                } // Ends optional thumbnail check.
                                Text(stringResource(R.string.chat_image_attached), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)) // Confirms attachment cleanly.
                                TextButton(onClick = model::clearPendingImage) { Text(stringResource(R.string.chat_remove_attachment)) } // Deletes uncommitted disk copy and clears state.
                            } // Ends pending attachment row.
                        } // Ends attachment bar container.
                    } // Ends pending attachment check.
                    Row( // Places gallery/camera icons, rounded pill text input and explicit Send CTA together.
                        modifier = Modifier.fillMaxWidth().padding(horizontal = DroidMentorSpacing.small, vertical = DroidMentorSpacing.small),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) { // Uses ViewModel commands rather than direct network calls.
                        val inputEnabled = !state.loading && !state.storageError && state.chat != null && !state.working && !state.pending // Enables composer controls cleanly.
                        IconButton( // Launches Photo Picker without runtime permissions (M3-11).
                            onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            enabled = inputEnabled,
                        ) { // Ends gallery IconButton setup.
                            Icon(painterResource(R.drawable.ic_photo_library), contentDescription = stringResource(R.string.chat_attach_gallery), tint = MaterialTheme.colorScheme.onSurfaceVariant) // Labels icon for accessibility.
                        } // Ends gallery attach button.
                        IconButton( // Checks CAMERA permission, shows rationale when appropriate, or launches TakePicture via FileProvider (M3-12).
                            onClick = { // Evaluates permission state before calling TakePicture.
                                when { // Branches on granted vs rationale vs initial prompt.
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> launchCamera() // Launches camera immediately when already permitted.
                                    shouldShowCameraRationale(context) -> showCameraRationale = true // Shows rationale first if previously denied.
                                    else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA) // Requests runtime permission from Android.
                                } // Ends permission check.
                            }, // Ends camera tap handler.
                            enabled = inputEnabled,
                        ) { // Ends camera IconButton setup.
                            Icon(painterResource(R.drawable.ic_camera), contentDescription = stringResource(R.string.chat_capture_camera), tint = MaterialTheme.colorScheme.onSurfaceVariant) // Labels camera icon for accessibility.
                        } // Ends camera capture button.
                        OutlinedTextField( // Modern rounded pill input kept only in memory and SavedStateHandle.
                            value = state.draft,
                            onValueChange = model::updateDraft,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            label = { Text(stringResource(R.string.chat_draft_label)) },
                            enabled = inputEnabled,
                            maxLines = 5,
                        ) // Ends in-memory draft input.
                        Button( // Rounded pill Send button with paper-plane icon and >=48dp height.
                            onClick = model::send,
                            enabled = state.canSend,
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = DroidMentorSpacing.medium, vertical = 12.dp),
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { // Places paper-plane icon beside Send label.
                            Icon(painterResource(R.drawable.ic_send), contentDescription = null, modifier = Modifier.size(18.dp)) // Decorative send icon.
                            Spacer(Modifier.width(6.dp)) // Small gap before Send text.
                            Text(stringResource(R.string.chat_send)) // Preserves exact "Send" label.
                        } // Ends explicit sending.
                    } // Ends draft controls.
                } // Ends composer Column.
            } // Ends composer Surface container.
        } // Ends keyboard-aware conversation layout.
    } // Ends the selected conversation frame.
} // Ends the active chat screen.

@OptIn(ExperimentalFoundationApi::class) // Uses combinedClickable to open the bubble context menu on long-press.
@Composable // Renders one saved row without owning networking or persistence.
private fun MessageBubble( // Receives callbacks from the screen for retry, image preview and long-press context actions.
    message: Message, // Supplies role, text, status, imagePath and error category.
    showRetry: Boolean, // Shows Retry only on the final failed user message.
    retryEnabled: Boolean, // Mirrors current send/retry prerequisite guards.
    canEdit: Boolean, // Enables Edit on user bubbles when online, configured and idle.
    canDelete: Boolean, // Enables Delete on user bubbles when idle.
    resolveImageFile: (String) -> File?, // Resolves sandbox-safe relative imagePath into private File.
    onOpenImage: (File) -> Unit, // Opens tapped thumbnail in full-screen preview dialog.
    onRetry: () -> Unit, // Retries the final failed user row.
    onEdit: () -> Unit, // Opens the rewrite dialog for this user bubble.
    onCopy: () -> Unit, // Copies this bubble's text to the clipboard.
    onDelete: () -> Unit, // Opens confirmation before deleting this turn and later turns.
) { // Starts the role-specific bubble layout.
    val user = message.role == Role.USER // Chooses author alignment and theme colours.
    var menuOpen by remember(message.id) { mutableStateOf(false) } // Controls the long-press context menu for this bubble.
    val bubbleShape = RoundedCornerShape( // Asymmetric speech-bubble corners give clear visual turn direction.
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (user) 20.dp else 6.dp,
        bottomEnd = if (user) 6.dp else 20.dp,
    ) // Ends chat bubble shape.
    val isFailedUser = user && message.status == MessageStatus.FAILED // Highlights unresolved user failure gently.
    val bubbleColor = when { // Chooses high-contrast container colour by author and state.
        isFailedUser -> MaterialTheme.colorScheme.errorContainer // Highlights failed send cleanly.
        user -> MaterialTheme.colorScheme.primaryContainer // User message accent colour.
        else -> MaterialTheme.colorScheme.surfaceContainerHigh // Clean neutral surface for mentor replies.
    } // Ends bubble colour choice.
    val bubbleContentColor = when { // Pairs contrasting text/icon colour with bubbleColor.
        isFailedUser -> MaterialTheme.colorScheme.onErrorContainer // Contrast on errorContainer.
        user -> MaterialTheme.colorScheme.onPrimaryContainer // Contrast on primaryContainer.
        else -> MaterialTheme.colorScheme.onSurface // Contrast on neutral reply container.
    } // Ends content colour choice.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) { // Separates user and mentor sides.
        Box { // Anchors the DropdownMenu directly to this bubble.
            Surface( // Draws polished asymmetric message bubble with subtle outline on mentor replies.
                modifier = Modifier
                    .widthIn(max = 460.dp) // Limits long lines on tablets while shrinking nicely on phones.
                    .clip(bubbleShape) // Clips long-press ripple to asymmetric bubble corners.
                    .combinedClickable( // Opens context menu on long-press without interfering with scrolling.
                        onClick = {}, // Leaves ordinary bubble taps harmless.
                        onLongClick = { menuOpen = true }, // Reveals Edit/Copy/Delete on user turns or Copy on model turns.
                    ), // Ends long-press handler.
                shape = bubbleShape, // Uses asymmetric rounded bubble shape.
                color = bubbleColor, // Applies role/status surface tint.
                contentColor = bubbleContentColor, // Applies crisp matching foreground colour.
                tonalElevation = if (user) 1.dp else 0.dp, // Lifts user bubbles gently.
                border = if (!user) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null, // Crisp subtle outline on mentor replies.
            ) { // Displays saved content and any persisted failure.
                Column(Modifier.padding(DroidMentorSpacing.medium), verticalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small)) { // Groups author, status, image, text and error feedback.
                    Row(horizontalArrangement = Arrangement.spacedBy(DroidMentorSpacing.small), verticalAlignment = Alignment.CenterVertically) { // Displays participant alongside per-message status.
                        Text(stringResource(if (user) R.string.chat_you else R.string.chat_mentor), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) // Identifies participant strongly.
                        if (user) MessageStatusBadge(message.status) // Renders distinct SENDING, SENT or FAILED indicator for each user bubble.
                    } // Ends bubble header row.
                    message.imagePath?.let { path -> // Displays attached image thumbnail with loading placeholder and missing-file fallback (M3-14).
                        val resolvedFile = remember(path) { resolveImageFile(path) } // Resolves app-private image file cleanly.
                        if (resolvedFile != null && resolvedFile.exists()) { // Renders Coil preview only when file exists on disk.
                            SubcomposeAsyncImage( // Displays loading spinner, cropped thumbnail or missing-file notice.
                                model = resolvedFile, // Loads app-private image file via Coil.
                                contentDescription = stringResource(R.string.chat_image_attachment_cd), // Accessibility label for image preview.
                                contentScale = ContentScale.Crop, // Crops preview cleanly inside bubble bounds.
                                modifier = Modifier
                                    .fillMaxWidth() // Spans bubble width cleanly.
                                    .heightIn(max = 220.dp) // Keeps long images compact in the feed.
                                    .clip(RoundedCornerShape(14.dp)) // Matches modern bubble curvature cleanly.
                                    .clickable { onOpenImage(resolvedFile) }, // Opens full-screen viewer on tap.
                                loading = { // Shows compact loading placeholder while Coil decodes bitmap off main thread.
                                    Box(Modifier.fillMaxWidth().heightIn(min = 100.dp), contentAlignment = Alignment.Center) { // Centers placeholder spinner.
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp)) // Visual loading placeholder.
                                    } // Ends placeholder Box.
                                }, // Ends loading placeholder.
                                error = { // Handles decode failure cleanly without throwing.
                                    Text(stringResource(R.string.chat_image_missing), style = MaterialTheme.typography.bodySmall) // Reports unavailable image safely.
                                }, // Ends Coil error fallback.
                            ) // Ends SubcomposeAsyncImage.
                        } else { // Handles manually cleared or missing image files cleanly.
                            Text(stringResource(R.string.chat_image_missing), style = MaterialTheme.typography.bodySmall) // Shows missing-file notice instead of broken icon.
                        } // Ends missing-file fallback branch.
                    } // Ends optional message image rendering.
                    if (message.text.isNotBlank()) { // Shows message text when non-empty (allows image-only user bubbles cleanly).
                        Text(message.text) // Displays full saved message text without altering history.
                    } // Ends non-blank text rendering.
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

private fun shouldShowCameraRationale(context: Context): Boolean { // Checks ActivityCompat safely even if Context is wrapped.
    val activity = context.findActivity() ?: return false // Falls back safely in non-Activity test environments.
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) // Delegates to platform rationale check.
} // Ends camera rationale helper.

private tailrec fun Context.findActivity(): Activity? = when (this) { // Unwraps Compose ContextWrappers recursively.
    is Activity -> this // Found the host Activity.
    is ContextWrapper -> baseContext.findActivity() // Continues unwrapping outer ContextWrapper.
    else -> null // Reached Application or non-Activity context cleanly.
} // Ends Activity lookup.

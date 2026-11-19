package pt.isel.pdm.droidmentor.ui.components // Shares the simple manual rename dialog between History and Active Chat.

import androidx.compose.foundation.layout.fillMaxWidth // Lets the title input span the dialog width.
import androidx.compose.material3.AlertDialog // Uses standard Material 3 dialog framing.
import androidx.compose.material3.OutlinedTextField // Displays the editable conversation title.
import androidx.compose.material3.Text // Displays dialog labels.
import androidx.compose.material3.TextButton // Provides Cancel and Save buttons.
import androidx.compose.runtime.Composable // Marks the dialog as Compose UI.
import androidx.compose.runtime.getValue // Reads Compose state delegate.
import androidx.compose.runtime.mutableStateOf // Holds the dialog's draft title locally while open.
import androidx.compose.runtime.remember // Preserves the draft while the dialog stays open for this chat.
import androidx.compose.runtime.setValue // Updates the local draft title.
import androidx.compose.ui.Modifier // Applies dialog field width.
import androidx.compose.ui.res.stringResource // Reads translated dialog strings.
import pt.isel.pdm.droidmentor.R // Supplies rename dialog string resources.

/** Reusable confirmation dialog for manually renaming a conversation. */
@Composable // Keeps rename UI uniform across screens without duplicating dialog code.
fun RenameChatDialog( // Receives the starting title and save/dismiss callbacks.
    initialTitle: String, // Pre-fills the current saved title.
    onConfirm: (String) -> Unit, // Delivers the normalized nonblank title to the caller.
    onDismiss: () -> Unit, // Closes the dialog without modifying storage.
) { // Starts the rename dialog.
    var draft by remember(initialTitle) { mutableStateOf(initialTitle) } // Starts with the current conversation title.
    val trimmed = draft.trim() // Validates nonblank input before enabling Save.
    AlertDialog( // Presents a focused single-field dialog.
        onDismissRequest = onDismiss, // Closes cleanly on outside tap or Back.
        title = { Text(stringResource(R.string.chat_rename_dialog_title)) }, // Labels the rename action.
        text = { // Hosts the single-line title input field.
            OutlinedTextField( // Lets the user type a new title.
                value = draft, // Shows the controlled local draft.
                onValueChange = { draft = it }, // Updates the local draft on keystroke.
                modifier = Modifier.fillMaxWidth(), // Uses available dialog width.
                label = { Text(stringResource(R.string.chat_rename_label)) }, // Labels the title input.
                singleLine = true, // Keeps conversation titles on one line.
            ) // Ends title input field.
        }, // Ends dialog body.
        confirmButton = { // Saves the new title when nonblank.
            TextButton(onClick = { onConfirm(trimmed) }, enabled = trimmed.isNotEmpty()) { // Blocks empty titles.
                Text(stringResource(R.string.chat_rename_save)) // Labels the save action.
            } // Ends confirm button.
        }, // Ends confirm slot.
        dismissButton = { // Cancels without saving.
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.history_delete_cancel)) } // Reuses Cancel label.
        }, // Ends dismiss slot.
    ) // Ends AlertDialog.
} // Ends reusable rename dialog.

package pt.isel.pdm.droidmentor.ui.components // Keeps lifecycle-aware event collection in one small UI helper.

import androidx.compose.runtime.Composable // Allows the collector to follow its screen's composition.
import androidx.compose.runtime.LaunchedEffect // Starts and cancels event work with the visible screen.
import androidx.compose.runtime.rememberUpdatedState // Uses the current callback without restarting event collection on recomposition.
import androidx.lifecycle.Lifecycle // Defines the visible STARTED lifecycle state.
import androidx.lifecycle.compose.LocalLifecycleOwner // Uses the navigation destination's owner rather than the Application lifetime.
import androidx.lifecycle.repeatOnLifecycle // Collects one-shot events only while the screen is started.
import kotlinx.coroutines.flow.Flow // Accepts ViewModel events without depending on the channel implementation.

/** Collects one-shot events while a screen is started and cancels collection when that screen leaves. */
@Composable // Owns the side effect without introducing business logic into the layout.
fun <T> EventEffect(events: Flow<T>, onEvent: suspend (T) -> Unit) { // Lets each screen handle navigation or a safe snackbar event.
    val owner = LocalLifecycleOwner.current // Gets the selected destination's lifecycle.
    val currentHandler = rememberUpdatedState(onEvent) // Keeps the latest navigation and snackbar references.
    LaunchedEffect(events, owner) { // Restarts only when the event stream or screen owner changes.
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { // Defers event consumption while the screen is in the background.
            events.collect { currentHandler.value(it) } // Invokes the current screen handler for each one-shot event.
        } // Ends visible-screen collection.
    } // Ends the lifecycle-owned effect.
} // Ends shared event collection.

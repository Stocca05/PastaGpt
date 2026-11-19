package pt.isel.pdm.droidmentor.data.preferences // Groups Preferences DataStore instances for local app state.

import android.content.Context // Supplies the application's private storage directory.
import androidx.datastore.core.DataStore // Exposes the asynchronous Preferences DataStore API.
import androidx.datastore.preferences.core.Preferences // Represents stored preference key-value pairs.
import androidx.datastore.preferences.preferencesDataStore // Creates one cached singleton store per process.

private val Context.activeSessionStore by preferencesDataStore(name = "droidmentor_session") // Kept separate from droidmentor_credentials per V4.

/** Provides the dedicated Preferences DataStore used only for active conversation restoration. */
object ActiveSessionDataStore { // Keeps UI session state completely separate from BYOK credentials.
    /** Returns the singleton session store bound to the application context. */
    fun get(context: Context): DataStore<Preferences> { // Avoids leaking an Activity context.
        return context.applicationContext.activeSessionStore // Reuses the single process-owned DataStore instance.
    } // Ends session store lookup.
} // Ends session DataStore provider.

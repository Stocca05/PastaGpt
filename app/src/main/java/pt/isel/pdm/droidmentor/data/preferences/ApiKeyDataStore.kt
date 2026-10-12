package pt.isel.pdm.droidmentor.data.preferences // Groups the app's Preferences DataStore setup.

import android.content.Context // Provides the app's private storage location.
import androidx.datastore.core.DataStore // Defines the asynchronous storage interface.
import androidx.datastore.preferences.core.Preferences // Represents stored preference values.
import androidx.datastore.preferences.preferencesDataStore // Creates one cached store with background IO.

private val Context.mentorKeyStore by preferencesDataStore(name = "droidmentor_credentials") // Creates one store per process for this file.

/** Provides the dedicated Preferences DataStore used only for the BYOK key. */
object ApiKeyDataStore { // Keeps store creation separate from repository operations.
    /** Returns the same store instance using the application's context. */
    fun get(context: Context): DataStore<Preferences> { // Supplies the store to the manual service locator later.
        return context.applicationContext.mentorKeyStore // Avoids retaining an Activity or creating a second store for the file.
    } // Ends the store lookup.
} // Ends the DataStore provider.

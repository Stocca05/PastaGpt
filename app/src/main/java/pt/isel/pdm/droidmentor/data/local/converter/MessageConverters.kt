package pt.isel.pdm.droidmentor.data.local.converter // Groups conversions between Kotlin and SQLite values.

import androidx.room.TypeConverter // Marks methods Room can call when saving or reading a column.
import pt.isel.pdm.droidmentor.domain.MessageErrorCategory // Provides the known failure labels.
import pt.isel.pdm.droidmentor.domain.MessageStatus // Provides the known exchange states.
import pt.isel.pdm.droidmentor.domain.Role // Provides the known message authors.

/** Converts message enums to text columns and back, preserving null error values. */
class MessageConverters { // Keeps the conversion methods together.
    /** Returns the role name stored in the database. */
    @TypeConverter // Room uses this method when saving a role.
    fun roleToString(role: Role): String { // Receives the role and returns database text.
        return role.name // Stores USER or MODEL without changing its spelling.
    } // Ends the role-to-text conversion.

    /** Reads a role previously written by [roleToString]. */
    @TypeConverter // Room uses this method when reading a role.
    fun stringToRole(value: String): Role { // Receives the saved text and returns its role.
        return Role.valueOf(value) // Finds the enum entry with this exact name.
    } // Ends the text-to-role conversion.

    /** Returns the exchange state name stored in the database. */
    @TypeConverter // Room uses this method when saving a state.
    fun statusToString(status: MessageStatus): String { // Receives the state and returns database text.
        return status.name // Stores SENDING, SENT or FAILED.
    } // Ends the state-to-text conversion.

    /** Reads a state previously written by [statusToString]. */
    @TypeConverter // Room uses this method when reading a state.
    fun stringToStatus(value: String): MessageStatus { // Receives the saved state name.
        return MessageStatus.valueOf(value) // Finds the matching state entry.
    } // Ends the text-to-state conversion.

    /** Returns the failure name, keeping the absence of an error as null. */
    @TypeConverter // Room uses this method when saving an optional error.
    fun errorCategoryToString(category: MessageErrorCategory?): String? { // Accepts and returns nullable values.
        return category?.name // Reads the name only when the category is not null.
    } // Ends the error-to-text conversion.

    /** Reads a failure previously written by [errorCategoryToString], or returns null. */
    @TypeConverter // Room uses this method when reading an optional error.
    fun stringToErrorCategory(value: String?): MessageErrorCategory? { // Receives the nullable saved name.
        if (value == null) { // Checks whether this message has no recorded error.
            return null // Preserves that absence instead of creating an UNKNOWN error.
        } // Ends the null check.
        return MessageErrorCategory.valueOf(value) // Converts a saved error name to its enum entry.
    } // Ends the text-to-error conversion.
} // Ends the converter class.

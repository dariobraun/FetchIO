package de.hsbo.fetch

import com.google.firebase.database.Exclude

data class Item(val name: String, val info: String, var key: String) {

    constructor() : this(
        "", "", ""
    )

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "info" to info,
            "key" to key
        )
    }
}
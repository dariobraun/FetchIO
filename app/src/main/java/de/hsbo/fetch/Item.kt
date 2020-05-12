package de.hsbo.fetch

import com.google.firebase.database.Exclude
import java.io.Serializable

data class Item(val name: String, val info: String, var key: String): Serializable {

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
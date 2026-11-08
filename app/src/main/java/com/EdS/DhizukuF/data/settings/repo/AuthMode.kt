package com.EdS.DhizukuF.data.settings.repo

/** When DhizukuF asks for the screen lock (PIN, pattern, password), fingerprint or face. */
enum class AuthMode {
    /** Never (default). */
    Off,

    /** Before sensitive actions: allowing an app, enabling dangerous commands, activation ... */
    Confirm,

    /** When the app is opened (and after a short time in the background). */
    Entry,

    /** Both of the above. */
    Both;

    val onConfirm: Boolean get() = this == Confirm || this == Both
    val onEntry: Boolean get() = this == Entry || this == Both

    companion object {
        fun parse(name: String?): AuthMode = entries.firstOrNull { it.name == name } ?: Off
    }
}

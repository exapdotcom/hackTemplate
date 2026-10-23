package com.hustle.core.config

data class AppConfig(
    val backendUrl: String,
    val useMocks: Boolean,
    val mockScenario: MockScenario,
) {
    val normalizedBackendUrl: String
        get() = backendUrl
            .trim()
            .ifBlank { "http://10.0.2.2:8080/" }
            .let { if (it.endsWith('/')) it else "$it/" }
}

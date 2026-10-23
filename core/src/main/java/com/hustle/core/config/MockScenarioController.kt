package com.hustle.core.config

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Runtime switch for mock scenarios. It is intentionally not persisted:
 * restarting the app resets the scenario to the value from gradle.properties.
 */
@Singleton
class MockScenarioController @Inject constructor(
    config: AppConfig,
) {
    private val _scenario = MutableStateFlow(config.mockScenario)
    val scenario: StateFlow<MockScenario> = _scenario.asStateFlow()

    fun set(value: MockScenario) {
        _scenario.value = value
    }
}

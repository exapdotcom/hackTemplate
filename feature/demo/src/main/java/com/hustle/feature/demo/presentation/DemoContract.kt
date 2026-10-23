package com.hustle.feature.demo.presentation

import com.hustle.core.config.MockScenario
import com.hustle.core.presentation.UiEffect
import com.hustle.core.presentation.UiEvent
import com.hustle.core.presentation.UiState
import com.hustle.core.presentation.UiText
import com.hustle.feature.demo.domain.DemoItem

data class DemoState(
    val items: List<DemoItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: UiText? = null,
    val isMockMode: Boolean = false,
    val scenario: MockScenario = MockScenario.SUCCESS,
    val scenarios: List<MockScenario> = MockScenario.entries,
) : UiState

sealed interface DemoEvent : UiEvent {
    data object Load : DemoEvent
    data object Refresh : DemoEvent
    data object Retry : DemoEvent
    data class ItemClicked(val item: DemoItem) : DemoEvent
    data class ScenarioSelected(val scenario: MockScenario) : DemoEvent
}

sealed interface DemoEffect : UiEffect {
    data class ShowMessage(val message: UiText) : DemoEffect
}

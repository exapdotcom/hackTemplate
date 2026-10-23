package com.hustle.feature.demo.presentation

import androidx.lifecycle.viewModelScope
import com.hustle.core.config.AppConfig
import com.hustle.core.config.MockScenarioController
import com.hustle.core.network.ApiResult
import com.hustle.core.presentation.BaseViewModel
import com.hustle.core.presentation.UiText
import com.hustle.core.presentation.toUiText
import com.hustle.feature.demo.R
import com.hustle.feature.demo.domain.DemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class DemoViewModel @Inject constructor(
    private val repository: DemoRepository,
    config: AppConfig,
    private val scenarioController: MockScenarioController,
) : BaseViewModel<DemoState, DemoEvent, DemoEffect>(
    initialState = DemoState(
        isMockMode = config.useMocks,
        scenario = scenarioController.scenario.value,
    ),
) {

    private var loadJob: Job? = null

    init {
        load()
    }

    override fun onEvent(event: DemoEvent) {
        when (event) {
            DemoEvent.Load -> load()
            DemoEvent.Refresh -> load(refresh = true)
            DemoEvent.Retry -> load()
            is DemoEvent.ItemClicked -> sendEffect(
                DemoEffect.ShowMessage(
                    UiText.Resource(
                        R.string.demo_item_clicked,
                        args = listOf(event.item.title),
                    ),
                ),
            )

            is DemoEvent.ScenarioSelected -> {
                scenarioController.set(event.scenario)
                updateState {
                    copy(
                        scenario = event.scenario,
                        items = emptyList(),
                        isLoading = true,
                        isRefreshing = false,
                        error = null,
                    )
                }
                load()
            }
        }
    }

    private fun load(refresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            updateState {
                copy(
                    isLoading = !refresh,
                    isRefreshing = refresh,
                    error = null,
                )
            }

            when (val result = repository.getItems()) {
                is ApiResult.Success -> updateState {
                    copy(
                        items = result.data,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                    )
                }

                is ApiResult.Failure -> updateState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = result.error.toUiText(),
                    )
                }
            }
        }
    }
}

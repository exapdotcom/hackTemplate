package com.hustle.feature.demo.data

import com.hustle.core.config.MockScenario
import com.hustle.core.config.MockScenarioController
import com.hustle.core.network.ApiError
import com.hustle.core.network.ApiResult
import com.hustle.feature.demo.domain.DemoItem
import com.hustle.feature.demo.domain.DemoRepository
import javax.inject.Inject
import kotlinx.coroutines.delay

class MockDemoRepository @Inject constructor(
    private val scenarioController: MockScenarioController,
) : DemoRepository {

    override suspend fun getItems(): ApiResult<List<DemoItem>> =
        when (scenarioController.scenario.value) {
            MockScenario.SUCCESS -> {
                delay(600)
                ApiResult.Success(mockItems)
            }

            MockScenario.EMPTY -> {
                delay(400)
                ApiResult.Success(emptyList())
            }

            MockScenario.ERROR -> {
                delay(500)
                ApiResult.Failure(ApiError.Http(503, "Mock backend is unavailable"))
            }

            MockScenario.SLOW -> {
                delay(2_500)
                ApiResult.Success(mockItems)
            }
        }

    private val mockItems: List<DemoItem> = listOf(
        DemoItem(
            id = "1",
            title = "Mock data, real architecture",
            description = "The feature talks to a repository interface, so switching to the backend is one Hilt binding away.",
            tag = "Architecture",
        ),
        DemoItem(
            id = "2",
            title = "State is explicit",
            description = "Loading, empty, error, and content are separate states instead of a pile of booleans.",
            tag = "MVI",
        ),
        DemoItem(
            id = "3",
            title = "No backend required",
            description = "Use the scenario menu to reproduce the states your teammates will see later.",
            tag = "Mocks",
        ),
        DemoItem(
            id = "4",
            title = "Built for the demo",
            description = "The screen is usable on an emulator before the API is ready, then the real implementation takes over.",
            tag = "Demo",
        ),
    )
}

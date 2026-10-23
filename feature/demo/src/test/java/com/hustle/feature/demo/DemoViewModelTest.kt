package com.hustle.feature.demo

import com.hustle.core.config.AppConfig
import com.hustle.core.config.MockScenario
import com.hustle.core.config.MockScenarioController
import com.hustle.core.network.ApiError
import com.hustle.core.network.ApiResult
import com.hustle.feature.demo.domain.DemoItem
import com.hustle.feature.demo.domain.DemoRepository
import com.hustle.feature.demo.presentation.DemoViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `successful load exposes items`() = runTest {
        val item = DemoItem(
            id = "1",
            title = "Item",
            description = "Description",
            tag = "Test",
        )
        val config = testConfig()
        val viewModel = DemoViewModel(
            repository = FakeDemoRepository(ApiResult.Success(listOf(item))),
            config = config,
            scenarioController = MockScenarioController(config),
        )

        advanceUntilIdle()

        assertEquals(listOf(item), viewModel.state.value.items)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `failed load exposes a user-facing error`() = runTest {
        val config = testConfig()
        val viewModel = DemoViewModel(
            repository = FakeDemoRepository(ApiResult.Failure(ApiError.Network)),
            config = config,
            scenarioController = MockScenarioController(config),
        )

        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    private fun testConfig() = AppConfig(
        backendUrl = "http://localhost/",
        useMocks = true,
        mockScenario = MockScenario.SUCCESS,
    )

    private class FakeDemoRepository(
        private val result: ApiResult<List<DemoItem>>,
    ) : DemoRepository {
        override suspend fun getItems(): ApiResult<List<DemoItem>> = result
    }
}

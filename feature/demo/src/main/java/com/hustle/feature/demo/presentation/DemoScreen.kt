package com.hustle.feature.demo.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hustle.core.R as CoreR
import com.hustle.core.config.MockScenario
import com.hustle.core.presentation.CollectEffects
import com.hustle.core.presentation.UiText
import com.hustle.core.presentation.asString
import com.hustle.core.presentation.resolve
import com.hustle.core.ui.components.EmptyView
import com.hustle.core.ui.components.ErrorView
import com.hustle.core.ui.components.LoadingView
import com.hustle.core.ui.theme.HackTemplateTheme
import com.hustle.feature.demo.R
import com.hustle.feature.demo.domain.DemoItem
import kotlinx.coroutines.launch

@Composable
fun DemoScreen(
    isAuthenticated: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: DemoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is DemoEffect.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(effect.message.resolve(context))
            }
        }
    }

    DemoContent(
        state = state,
        isAuthenticated = isAuthenticated,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onSignIn = onSignIn,
        onSignOut = onSignOut,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoContent(
    state: DemoState,
    isAuthenticated: Boolean,
    snackbarHostState: SnackbarHostState,
    onEvent: (DemoEvent) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    var scenarioMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(R.string.demo_title))
                        if (state.isMockMode) {
                            Text(
                                text = stringResource(R.string.demo_mock_mode),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onEvent(DemoEvent.Refresh) },
                        enabled = !state.isLoading && !state.isRefreshing,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(CoreR.string.core_refresh),
                        )
                    }

                    if (state.isMockMode) {
                        Box {
                            IconButton(onClick = { scenarioMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = stringResource(CoreR.string.core_mock_scenario),
                                )
                            }
                            DropdownMenu(
                                expanded = scenarioMenuExpanded,
                                onDismissRequest = { scenarioMenuExpanded = false },
                            ) {
                                state.scenarios.forEach { scenario ->
                                    DropdownMenuItem(
                                        text = { Text(scenario.label()) },
                                        leadingIcon = if (scenario == state.scenario) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Outlined.Check,
                                                    contentDescription = null,
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        onClick = {
                                            scenarioMenuExpanded = false
                                            onEvent(DemoEvent.ScenarioSelected(scenario))
                                        },
                                    )
                                }
                            }
                        }
                    }

                    TextButton(onClick = if (isAuthenticated) onSignOut else onSignIn) {
                        Text(
                            text = stringResource(
                                if (isAuthenticated) CoreR.string.core_sign_out else CoreR.string.core_sign_in,
                            ),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val error = state.error
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> LoadingView(
                    message = stringResource(CoreR.string.core_loading),
                )

                error != null -> ErrorView(
                    message = error,
                    onRetry = { onEvent(DemoEvent.Retry) },
                )

                state.items.isEmpty() -> EmptyView(
                    title = UiText.Resource(R.string.demo_empty_title),
                    message = UiText.Resource(R.string.demo_empty_message),
                    actionLabel = UiText.Resource(CoreR.string.core_refresh),
                    onAction = { onEvent(DemoEvent.Refresh) },
                )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    if (state.isRefreshing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.demo_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(
                            items = state.items,
                            key = { item -> item.id },
                        ) { item ->
                            DemoCard(
                                item = item,
                                onClick = { onEvent(DemoEvent.ItemClicked(item)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoCard(
    item: DemoItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        DemoImage(item)
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = item.tag,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DemoImage(item: DemoItem) {
    if (item.imageUrl != null) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.tag,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun MockScenario.label(): String = stringResource(
    when (this) {
        MockScenario.SUCCESS -> CoreR.string.core_mock_success
        MockScenario.EMPTY -> CoreR.string.core_mock_empty
        MockScenario.ERROR -> CoreR.string.core_mock_error
        MockScenario.SLOW -> CoreR.string.core_mock_slow
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun DemoContentPreview() {
    HackTemplateTheme {
        DemoContent(
            state = DemoState(
                items = listOf(
                    DemoItem(
                        id = "preview",
                        title = "Preview item",
                        description = "The same screen is used with mocks and with the real API.",
                        tag = "Preview",
                    ),
                ),
                isLoading = false,
                isMockMode = true,
            ),
            isAuthenticated = false,
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onSignIn = {},
            onSignOut = {},
        )
    }
}

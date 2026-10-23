package com.hustle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hustle.core.ui.theme.HackTemplateTheme
import com.hustle.feature.auth.AuthRoute
import com.hustle.feature.auth.domain.SessionState
import com.hustle.feature.demo.DemoRoute

@Composable
fun AppRoot(
    viewModel: AppSessionViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()

    when (val state = session) {
        SessionState.Unknown -> SplashScreen()

        SessionState.Authenticated, SessionState.Unauthenticated -> {
            val startDestination = remember {
                if (viewModel.config.useMocks || state == SessionState.Authenticated) {
                    DemoRoute
                } else {
                    AuthRoute
                }
            }

            AppNavHost(
                startDestination = startDestination,
                isAuthenticated = state == SessionState.Authenticated,
                onLogout = viewModel::logout,
            )
        }
    }
}

@Composable
private fun SplashScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.app_starting),
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashPreview() {
    HackTemplateTheme {
        SplashScreen()
    }
}

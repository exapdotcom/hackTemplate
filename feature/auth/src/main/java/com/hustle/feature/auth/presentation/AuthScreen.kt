package com.hustle.feature.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hustle.core.R as CoreR
import com.hustle.core.presentation.CollectEffects
import com.hustle.core.presentation.asString
import com.hustle.core.ui.components.PrimaryButton
import com.hustle.core.ui.theme.HackTemplateTheme
import com.hustle.feature.auth.R

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    onContinueAsGuest: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            AuthEffect.Authenticated -> onAuthenticated()
        }
    }

    AuthContent(
        state = state,
        onEvent = viewModel::onEvent,
        onContinueAsGuest = onContinueAsGuest,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthContent(
    state: AuthState,
    onEvent: (AuthEvent) -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    val isRegister = state.mode == AuthMode.REGISTER
    val title = if (isRegister) R.string.auth_register_title else R.string.auth_login_title
    val subtitle = if (isRegister) R.string.auth_register_subtitle else R.string.auth_login_subtitle
    val action = if (isRegister) R.string.auth_register_action else R.string.auth_login_action

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.auth_title)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Text(
                text = stringResource(title),
                modifier = Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(subtitle),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isRegister) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onEvent(AuthEvent.NameChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.auth_name)) },
                    singleLine = true,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = state.email,
                onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.auth_email)) },
                singleLine = true,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.auth_password)) },
                singleLine = true,
                enabled = !state.isLoading,
                visualTransformation = if (state.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onEvent(AuthEvent.Submit) },
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { onEvent(AuthEvent.TogglePasswordVisibility) },
                    ) {
                        Icon(
                            imageVector = if (state.isPasswordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = stringResource(
                                if (state.isPasswordVisible) {
                                    R.string.auth_hide_password
                                } else {
                                    R.string.auth_show_password
                                },
                            ),
                        )
                    }
                },
            )

            state.error?.let { error ->
                Text(
                    text = error.asString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Start,
                )
            }

            PrimaryButton(
                text = stringResource(action),
                onClick = { onEvent(AuthEvent.Submit) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(52.dp),
                loading = state.isLoading,
            )

            TextButton(
                onClick = {
                    onEvent(
                        AuthEvent.ModeChanged(
                            if (isRegister) AuthMode.LOGIN else AuthMode.REGISTER,
                        ),
                    )
                },
                enabled = !state.isLoading,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = stringResource(
                        if (isRegister) R.string.auth_to_login else R.string.auth_to_register,
                    ),
                )
            }

            OutlinedButton(
                onClick = onContinueAsGuest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(52.dp),
                enabled = !state.isLoading,
            ) {
                Text(text = stringResource(CoreR.string.core_continue_as_guest))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun AuthContentPreview() {
    HackTemplateTheme {
        AuthContent(
            state = AuthState(
                email = "demo@example.com",
                password = "password",
            ),
            onEvent = {},
            onContinueAsGuest = {},
        )
    }
}

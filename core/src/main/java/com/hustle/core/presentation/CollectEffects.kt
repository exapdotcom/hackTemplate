package com.hustle.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> CollectEffects(
    flow: Flow<T>,
    onEffect: (T) -> Unit,
) {
    LaunchedEffect(flow) {
        flow.collect(onEffect)
    }
}

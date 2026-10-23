package com.hustle.core.presentation

/**
 * Marker interfaces for the transparent MVVM + MVI contract used by features.
 * State is rendered by the screen, events go into the ViewModel, effects are
 * one-off actions such as navigation or a snackbar.
 */
interface UiState
interface UiEvent
interface UiEffect

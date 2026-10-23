# Архитектура

## 1. Общая схема

```text
┌────────────────────────────────────────────────────────────┐
│ :app                                                       │
│  MainActivity → AppRoot → AppNavHost                       │
│  AppSessionViewModel (AuthRepository + AppConfig)          │
└───────────────┬───────────────────────────┬────────────────┘
                │                           │
       ┌────────▼─────────┐        ┌────────▼─────────┐
       │ :feature:auth    │        │ :feature:demo    │
       │ AuthRoute        │        │ DemoRoute        │
       │ AuthScreen       │        │ DemoScreen       │
       │ AuthViewModel    │        │ DemoViewModel    │
       │ AuthRepository   │        │ DemoRepository   │
       └────────┬─────────┘        └────────┬─────────┘
                │                           │
                └─────────────┬─────────────┘
                              │
                     ┌────────▼────────┐
                     │ :core           │
                     │ config          │
                     │ network         │
                     │ storage         │
                     │ presentation    │
                     │ ui/theme, ui kit│
                     └─────────────────┘
```

Зависимости направлены только вниз:

- `:app` зависит от `:core`, `:feature:auth`, `:feature:demo`.
- Фичи зависят от `:core` и **не зависят** друг от друга.
- `:core` не зависит ни от `:app`, ни от фич.

## 2. Почему thin modular

Для 36 часов и одного Android-разработчика 13 модулей — это лишняя работа. Четыре модуля дают:

- явную границу «фича не знает о фиче»;
- отдельные namespace и ресурсы;
- возможность быстро удалить/заменить фичу;
- понятные Hilt-модули;
- при этом сборка остаётся быстрой.

Если проект вырастет, `:core` можно распилить на `core:network`, `core:ui`, `core:data`, но **не раньше**, чем появится реальная потребность.

## 3. Presentation: MVVM + MVI-контракт

Каждая фича имеет три типа:

```kotlin
data class FeatureState(...) : UiState

sealed interface FeatureEvent : UiEvent {
    data object Load : FeatureEvent
    data class QueryChanged(val value: String) : FeatureEvent
}

sealed interface FeatureEffect : UiEffect {
    data class ShowMessage(val message: UiText) : FeatureEffect
}
```

`BaseViewModel` из `:core`:

- хранит `StateFlow<S>`;
- отдаёт `Flow<F>` через `Channel`;
- даёт `updateState { copy(...) }`;
- требует реализовать `onEvent`.

Почему так:

- state прозрачный: экран можно отрисовать в Preview и в тесте;
- events явные: не нужно ловить клики по всей иерархии;
- effects не теряются при рекомпозиции, но и не являются частью state;
- ViewModel не знает про Compose и Android UI.

### Правила

- Не храните в state `Context`, `NavController`, `SnackbarHostState`.
- Не отправляйте навигацию через state (`navigateTo: Boolean`) — это effect.
- Не держите `MutableStateFlow` в UI-слое.
- `UiText` позволяет хранить строку как ресурс или как динамический текст, не таща `Context` в ViewModel.

## 4. Data/domain

```text
Domain model        — то, что нужно UI.
DTO                 — то, что приходит от backend.
Repository interface— контракт для ViewModel.
RepositoryImpl      — Retrofit + safeApiCall + mapping.
MockRepository      — сценарии без backend.
```

Пример:

```kotlin
interface DemoRepository {
    suspend fun getItems(): ApiResult<List<DemoItem>>
}
```

`DemoRepositoryImpl`:

```kotlin
safeApiCall { api.getItems() }
    .map { dtos -> dtos.map(DemoItemDto::toDomain) }
```

`MockDemoRepository` реализует тот же интерфейс и возвращает `ApiResult` по выбранному сценарию. ViewModel не знает, mock это или real.

## 5. Hilt

- `HackTemplateApplication` — `@HiltAndroidApp`.
- `MainActivity` — `@AndroidEntryPoint`.
- ViewModel — `@HiltViewModel` + `@Inject constructor`.
- `ConfigModule` даёт `AppConfig`.
- `NetworkModule` даёт `Gson`, `OkHttpClient`, `Retrofit`.
- `StorageModule` биндит `TokenStore`.
- `DemoModule` / `AuthModule` выбирают mock или real repository по `AppConfig.useMocks`.

Ключевой приём:

```kotlin
@Provides
@Singleton
fun provideDemoRepository(
    config: AppConfig,
    real: DemoRepositoryImpl,
    mock: MockDemoRepository,
): DemoRepository = if (config.useMocks) mock else real
```

Обе реализации создаются Hilt-ом, но наружу отдаётся только одна.

## 6. Сеть и ошибки

```text
Retrofit call
   │
   ▼
safeApiCall { ... }
   │
   ├── Success(data)
   └── Failure(ApiError)
          │
          ▼
     ApiError.toUiText()
          │
          ▼
       UiText в state
```

`ApiError`:

- `Http(code, message)` — non-2xx;
- `Network` — `IOException`;
- `Serialization` — Gson;
- `Unknown` — всё остальное.

`AuthInterceptor` добавляет `Authorization` из `TokenStore.currentToken`. Токен кэшируется в памяти, чтобы не делать `runBlocking` в OkHttp-интерцепторе.

401 сейчас показывается как ошибка. Автоматический refresh — рецепт, потому что контракт backend ещё не согласован.

## 7. Auth flow

```text
AppSessionViewModel
   → AuthRepository.restore()
   → SessionState.Unknown / Authenticated / Unauthenticated
   → AppRoot выбирает стартовый экран
```

- В mock-режиме приложение стартует с Demo, auth доступен по кнопке.
- В real-режиме без токена приложение стартует с Auth.
- «Continue as guest» ведёт в Demo без токена.
- После login AuthScreen отправляет effect `Authenticated`; `:app` навигирует в Demo.
- Logout очищает `TokenStore` и ведёт в Auth.

`AuthRepository` — интерфейс в `feature:auth/domain`; реальная и mock-реализации лежат в `data`.

## 8. Навигация

Type-safe routes:

```kotlin
@Serializable
data object DemoRoute

@Serializable
data object AuthRoute
```

`AppNavHost` — единственное место, где фичи соединяются:

```kotlin
composable<DemoRoute> {
    DemoScreen(
        isAuthenticated = isAuthenticated,
        onSignIn = { navController.navigate(AuthRoute) },
        onSignOut = { ... },
    )
}
```

Фича не принимает `NavController`. Это позволяет:

- тестировать экран без навигации;
- менять flow в одном месте;
- не плодить god-navigation;
- переиспользовать экран в другой фиче.

## 9. Конфигурация

```text
gradle.properties      local.properties      CI secrets
       │                      │                  │
       └──────────────┬───────┴──────────────────┘
                      ▼
          core/build.gradle.kts
                      ▼
          BuildConfig (BACKEND_URL, USE_MOCKS, MOCK_SCENARIO)
                      ▼
                 AppConfig
```

`local.properties` имеет приоритет над `gradle.properties`, но не коммитится.

## 10. Ошибки и границы, которые важно не повторять

Из аудита старых проектов:

- не делать god-navigation модуль;
- не передавать presentation-модели в data/domain;
- не использовать `runBlocking` в интерцепторах;
- не логировать BODY;
- не хранить токены в обычных логах;
- не включать cleartext в release;
- не добавлять мёртвые модули;
- не делать `fallbackToDestructiveMigration` без понимания;
- не хардкодить URL и ключи;
- не смешивать mock и real внутри ViewModel.

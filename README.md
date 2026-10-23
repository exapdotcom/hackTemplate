# Hack Template — Android

Рабочий шаблон Android-приложения для хакатона на **36 часов**, когда Android делает один человек, а backend и web — тиммейты. Приложение запускается и показывает сценарий **без backend** благодаря мокам, а затем переключается на реальный REST API одной настройкой.

## Быстрый старт

1. Откройте проект в Android Studio (JDK 17+).
2. Скопируйте `local.properties.example` в `local.properties` и при необходимости поправьте значения. Файл `local.properties` не коммитится.
3. Запустите `app` на эмуляторе или устройстве.

По умолчанию включён mock-режим:

```properties
hack.backendUrl=http://10.0.2.2:8080/
hack.useMocks=true
hack.mockScenario=SUCCESS
```

`10.0.2.2` — это хост-машина с точки зрения Android-эмулятора. Для реального устройства укажите IP компьютера в локальной сети.

## Стек

| Слой | Решение |
|---|---|
| Язык / UI | Kotlin, Jetpack Compose, Material 3 |
| Архитектура | Тонкая модульность: `app` + `core` + feature-модули |
| Presentation | MVVM + MVI-контракт: `State / Event / Effect` |
| DI | Hilt + KSP |
| Сеть | Retrofit + OkHttp + Gson |
| Навигация | App-level `NavHost` + type-safe route contracts фич |
| Хранение | DataStore для токена; Room — рецепт, не в базе |
| Изображения | Coil |
| SDK | minSdk 26, compileSdk/targetSdk 36 |
| CI | GitHub Actions: тесты, lint, debug APK артефактом |

## Модули

```text
:app                 — точка входа, тема, AppRoot, NavHost, session state
:core                — config, network, storage, MVI base, UI kit, theme
:feature:demo        — демонстрационная фича с mock/real repository
:feature:auth        — login / registration / logout + mock auth
build-logic          — convention plugins
```

Правила:

- `:app` знает обо всех фичах и связывает их через `NavHost`.
- Фича **не знает** о другой фиче и не принимает `NavController`.
- Фича общается с внешним миром через callbacks (`onSignIn`, `onSignOut`, `onAuthenticated`).
- `:core` не знает о фичах.
- Domain-слой есть, но **без use cases**. Если появится повторно используемая логика — добавьте use case в `domain` и вызовите его из ViewModel.

## Как устроена фича

Пример `:feature:demo`:

```text
feature/demo/
  DemoRoute.kt                  @Serializable data object DemoRoute
  data/                         Api, DTO, real repository, mock repository, Hilt module
  domain/                       DemoItem, DemoRepository
  presentation/                 DemoContract, DemoViewModel, DemoScreen
```

Контракт:

```kotlin
data class DemoState(...) : UiState
sealed interface DemoEvent : UiEvent
sealed interface DemoEffect : UiEffect
```

- `State` — обычный immutable data class, экран просто рендерит его.
- `Event` — действия пользователя, попадают в `DemoViewModel.onEvent`.
- `Effect` — одноразовые события: snackbar, навигация. Не храните их в state.
- `BaseViewModel` находится в `:core/presentation`.

## Mock-режим

Моки — это **не offline**. Это подмена repository фейковыми данными. Они не сохраняются между запусками.

В `:feature:demo` реализованы сценарии:

- `SUCCESS` — список;
- `EMPTY` — пустой список;
- `ERROR` — ошибка сервера;
- `SLOW` — задержка 2.5 секунды.

Сценарий можно переключить прямо в приложении: меню `⋮` в top bar на экране Demo. Стартовое значение задаётся в `hack.mockScenario`.

## Конфигурация и секреты

- `gradle.properties` — безопасные дефолты, которые можно коммитить.
- `local.properties` — локальные override-значения, не коммитится.
- CI — GitHub Secrets.

Значения попадают в `com.hustle.core.BuildConfig`:

```kotlin
AppConfig(
    backendUrl = BuildConfig.BACKEND_URL,
    useMocks = BuildConfig.USE_MOCKS,
    mockScenario = ...,
)
```

Не кладите в git `*.jks`, `*.keystore`, `google-services.json`, реальные API-ключи и токены. `local.properties.example` — единственный файл с примером без секретов.

## Auth

`feature:auth` включён в шаблон:

- login и registration по email/password;
- logout;
- mock-auth: подходит любой валидный email и пароль от 6 символов;
- токен сохраняется в DataStore через `TokenStore`;
- `AuthInterceptor` добавляет `Authorization: Bearer <token>`;
- `SessionState` в `:app` решает, показывать Auth или Demo.

Google Sign-In в v1 **не подключён** — это отдельный рецепт в `docs/RECIPES.md`. Для него нужны OAuth client ID и SHA-1, что на 36-часовом хакатоне легко съедает время.

## Backend contract

Шаблон ожидает:

```text
POST /auth/login      { "email", "password" } -> { "accessToken", "refreshToken"? }
POST /auth/register   { "name", "email", "password" } -> { "accessToken", "refreshToken"? }
GET  /demo/items      -> [ { "id", "title", "description", "tag", "imageUrl"? } ]
```

Подробнее — `docs/BACKEND_CONTRACT.md`. Если контракт тиммейтов отличается, поправьте DTO/endpoint в `feature:auth/data` и `feature:demo/data`, а не UI.

## Команды

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew :app:installDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Как добавить новую фичу

1. Создайте модуль `:feature:profile` с `build.gradle.kts`:
   ```kotlin
   plugins { id("hack.android.feature") }
   android { namespace = "com.hustle.feature.profile" }
   dependencies {
       implementation(project(":core"))
       // Compose / lifecycle / Hilt navigation — как в :feature:demo
   }
   ```
2. Добавьте `include(":feature:profile")` в `settings.gradle.kts`.
3. Создайте `@Serializable data object ProfileRoute` (или data class с аргументами).
4. Сделайте `ProfileContract`, `ProfileViewModel`, `ProfileScreen`.
5. Добавьте `composable<ProfileRoute> { ... }` в `AppNavHost`.
6. Передайте навигационные callbacks из `:app`, а не `NavController` внутрь фичи.

Для второго экрана с аргументом используйте type-safe route:

```kotlin
@Serializable
data class ProfileDetails(val userId: String)

composable<ProfileDetails> { entry ->
    val route = entry.toRoute<ProfileDetails>()
    ProfileDetailsScreen(userId = route.userId, ...)
}
```

## Переименование проекта

Скрипт меняет package, applicationId, namespace, app label, rootProject.name и физические директории пакетов:

```powershell
# сначала посмотреть, что изменится
.\scripts\rename-project.ps1 -NewName "MyHack" -NewPackage "com.example.myhack"

# применить
.\scripts\rename-project.ps1 -NewName "MyHack" -NewPackage "com.example.myhack" -Apply
```

Перед запуском сделайте commit или копию проекта. Скрипт не трогает `research/` и `scripts/`.

## CI

`.github/workflows/android.yml` на push/PR:

1. ставит JDK 17 и Android SDK;
2. запускает `testDebugUnitTest lintDebug assembleDebug`;
3. загружает `app-debug.apk` как artifact.

Release signing, Play Store и instrumented tests в базовый шаблон не входят.

## Безопасность

- Логи сети — только `BASIC`, без тел запросов/ответов и токенов.
- Cleartext HTTP разрешён только в debug-манифесте.
- В release нет `usesCleartextTraffic`.
- Токен в DataStore **не зашифрован**. Для production замените реализацию `TokenStore` на encrypted/Keystore-вариант, не меняя фичи.
- Не логируйте `Authorization`, пароли и ответы auth.
- Разрешение `INTERNET` — единственное в базовом шаблоне.

## Что дальше

- `docs/ARCHITECTURE.md` — подробная схема и границы.
- `docs/RECIPES.md` — Room, Paging, WebSocket, upload, Google Sign-In, refresh, encrypted storage.
- `docs/BACKEND_CONTRACT.md` — контракт для тиммейтов.
- `AGENTS.md` — правила для AI-агентов и новых участников.
- `research/` — исследование старых проектов и обоснование решений.

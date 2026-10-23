# AGENTS.md

Инструкции для AI-агентов и людей, которые работают с этим шаблоном.

## Контекст

- Это Android-шаблон для хакатона, не production-приложение.
- Android-разработчик обычно один; backend и web делают другие люди.
- Главная цель — быстро добавить фичу и показать её без backend.
- Не меняйте `research/` без просьбы: это историческое исследование.

## Команды

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew :app:installDebug
```

Перед завершением задачи запустите хотя бы `testDebugUnitTest` и `assembleDebug`. Если меняли manifest, ресурсы или настройки — также `lintDebug`.

## Архитектурные правила

1. **Фича не знает о другой фиче.** Навигацию связывает только `:app`.
2. **Не передавайте `NavController` в feature-модуль.** Используйте callbacks.
3. **State / Event / Effect.** Новый экран делайте через `BaseViewModel` и контракт.
4. **State — immutable data class.** Не храните `Context`, `NavController`, `SnackbarHostState`.
5. **Effect — только одноразовые действия.** Snackbar и навигация, не часть state.
6. **Domain без use cases.** Use case добавляйте только при реальной повторно используемой логике.
7. **Repository интерфейс в `domain`, реализация в `data`.** UI не должен знать про Retrofit/DTO.
8. **DTO не выходят за пределы `data`.** Маппинг в domain-модель обязателен.
9. **Hilt-биндинги — в `di` модуля.** Не создавайте зависимости вручную в Composable.
10. **Convention plugins — в `build-logic`.** Не копируйте `compileSdk`, `minSdk`, Java/Kotlin настройки в каждый модуль.

## UI

- Только Compose + Material 3.
- Dynamic color выключен: тема должна выглядеть одинаково на всех устройствах.
- Общие состояния (`Loading`, `Error`, `Empty`) — из `:core/ui/components`.
- Строки — в `res/values/strings.xml` соответствующего модуля; не хардкодьте пользовательский текст в Composable.
- Preview обязателен для нового экрана, если это возможно.
- Не добавляйте тяжёлые библиотеки без необходимости.

## Сеть и ошибки

- Все вызовы Retrofit оборачивайте в `safeApiCall`.
- Ошибки маппятся в `ApiError`; UI получает `UiText`, не exception.
- В ViewModel не должно быть `try/catch` вокруг каждого запроса — используйте repository + `ApiResult`.
- Логи: только `BASIC`. Никаких тел ответов, заголовков `Authorization`, паролей и токенов.
- Cleartext HTTP — только debug.
- 401 показывайте как ошибку/истёкшую сессию. Автоматический refresh — рецепт, пока контракт не согласован.

## Конфигурация

- Секреты и локальные значения — в `local.properties` (не коммитится) или CI Secrets.
- `gradle.properties` — только безопасные дефолты.
- Новые настройки добавляйте в `AppConfig` и `ConfigModule`, если они нужны во время выполнения.
- Не коммитьте `local.properties`, `*.jks`, `*.keystore`, `google-services.json`, реальные ключи.

## Моки

- Мок — это реализация repository, а не `if (useMocks)` внутри ViewModel.
- Сценарии: `SUCCESS`, `EMPTY`, `ERROR`, `SLOW`.
- Моки не сохраняют данные между запусками.
- Если нужен offline/cache — это Room-рецепт, не мок.

## Тесты

- 3–5 реальных unit-тестов лучше, чем 100 сгенерированных заглушек.
- Тестируйте ViewModel-контракт и маппинг ошибок.
- Для ViewModel используйте `MainDispatcherRule` и `kotlinx-coroutines-test`.
- Не добавляйте instrumented-тесты в базовый шаблон без просьбы.

## Навигация

- Type-safe routes: `@Serializable data object` или `@Serializable data class`.
- Аргументы передавайте через route, не через глобальные синглтоны.
- `composable<Route>` и `entry.toRoute<Route>()`.
- Очищайте back stack через `popUpTo(navController.graph.id) { inclusive = true }`.

## Документация

- Пользовательские инструкции — в `README.md`.
- Контракт backend — в `docs/BACKEND_CONTRACT.md`.
- Рецепты (Room/Paging/WebSocket/upload/Google) — в `docs/RECIPES.md`.
- Решения и их обоснование — в `research/DECISIONS.md`.

## Чего не делать

- Не тащить Room, Paging, WebSocket, upload, Google Sign-In в базовый шаблон.
- Не добавлять kapt: используйте KSP.
- Не использовать `runBlocking` в UI/сетевых интерцепторах.
- Не логировать токены и тела запросов.
- Не добавлять `MANAGE_EXTERNAL_STORAGE` и другие широкие permissions.
- Не делать god-navigation модуль.
- Не копировать старые проекты целиком: только проверенные идеи.

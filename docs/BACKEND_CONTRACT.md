# Контракт backend для Android

Этот файл — точка договорённости с backend-тиммейтами. Android-приложение уже умеет работать без backend в mock-режиме, но для интеграции нужен стабильный контракт.

## 1. Base URL

```text
http://10.0.2.2:8080/     # эмулятор → хост-машина
https://api.example.com/  # реальный сервер
```

Base URL задаётся в `local.properties`:

```properties
hack.backendUrl=http://10.0.2.2:8080/
```

Важно: **завершающий слэш обязателен** (шаблон добавляет его сам, если забыли).

## 2. Авторизация

### POST `/auth/login`

Request:

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

Response `200`:

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "optional-refresh-token"
}
```

Android также принимает alternate-имена:

- `accessToken`, `access_token`, `token`;
- `refreshToken`, `refresh_token`.

Но лучше использовать camelCase.

### POST `/auth/register`

Request:

```json
{
  "name": "Ada Lovelace",
  "email": "ada@example.com",
  "password": "secret123"
}
```

Response — такой же, как у login.

### Ошибки auth

```json
{
  "message": "Invalid credentials"
}
```

Android не показывает `message` пользователю напрямую: он маппит статус в `ApiError` и локализованный текст. Backend всё равно должен возвращать осмысленный статус:

| Код | Значение |
|---|---|
| 400 | validation error |
| 401 | wrong credentials |
| 409 | email already exists |
| 500 | server error |

## 3. Demo

### GET `/demo/items`

Response `200`:

```json
[
  {
    "id": "1",
    "title": "Mock data, real architecture",
    "description": "The feature talks to a repository interface.",
    "tag": "Architecture",
    "imageUrl": "https://example.com/image.jpg"
  }
]
```

Поля:

| Поле | Тип | Обязательное | Примечание |
|---|---|---|---|
| `id` | string | да | стабильный ключ списка |
| `title` | string | да | заголовок |
| `description` | string | нет | по умолчанию пустая строка |
| `tag` | string | нет | по умолчанию `Demo` |
| `imageUrl` | string / null | нет | поддерживается также `image_url` |

## 4. Авторизация запросов

Android добавляет:

```http
Authorization: Bearer <accessToken>
```

Если сервер вернул `401`:

- Android показывает ошибку «сессия истекла»;
- автоматический refresh в базовом шаблоне **не включён**;
- для refresh нужен отдельный endpoint, например `POST /auth/refresh` с `{ "refreshToken": "..." }`, и реализация `Authenticator` (см. `docs/RECIPES.md`).

## 5. Формат ошибок

Рекомендуемый:

```json
{
  "message": "Human-readable message",
  "code": "VALIDATION_ERROR"
}
```

Android сейчас читает только HTTP-код, тело сохраняется в `ApiError.Http.message` для отладки, но **не показывается** в UI. Это сознательно: не доверяем серверному тексту и не светим внутренние детали.

## 6. CORS / web

Android не требует CORS. Если backend и web живут на одном домене, настройте CORS для web отдельно.

## 7. Что нужно от backend-тиммейтов

1. OpenAPI/Swagger или хотя бы этот файл с реальными путями.
2. Примеры ответов login/register.
3. Согласование `401` и refresh.
4. Стабильный `id` в demo-списке.
5. Понятные HTTP-коды, а не `200 { "error": ... }`.

Если контракт отличается, правьте:

- `feature/auth/data/AuthApi.kt`
- `feature/auth/data/AuthDto.kt`
- `feature/demo/data/DemoApi.kt`
- `feature/demo/data/DemoItemDto.kt`

UI и ViewModel при этом не меняются.

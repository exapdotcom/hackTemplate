# Рецепты

В базовый шаблон **не входят** Room, Paging, WebSocket, upload и Google Sign-In. Это сделано специально: на 36-часовом хакатоне они чаще мешают, чем помогают. Ниже — проверенные рецепты, если задача действительно требует.

## 1. Room / offline cache

### Когда нужен

- Показать список без сети.
- Сохранить историю/черновики между запусками.
- Синхронизировать данные с сервером.

### Когда не нужен

- Нужны просто фейковые данные для демо → это моки.
- Нужно сохранить токен/настройку → DataStore.
- Нужно показать один экран, который всегда онлайн.

### Зависимости

Добавьте в `gradle/libs.versions.toml`:

```toml
room = "2.6.1"
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
```

В feature-модуле:

```kotlin
dependencies {
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
```

### Код

```kotlin
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val updatedAt: Long,
)

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY title")
    fun observeAll(): Flow<List<ItemEntity>>

    @Upsert
    suspend fun upsertAll(items: List<ItemEntity>)
}

@Database(
    entities = [ItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
```

Hilt:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
            .build()
}
```

### Правила

- Не используйте `fallbackToDestructiveMigration()` в проекте, который переживёт хакатон.
- Для v1 достаточно одной миграции, но `exportSchema = true` и схема в git — дешёвая страховка.
- Repository должен быть offline-first: сначала отдать кэш, потом обновить из сети.
- DTO → Entity → Domain. Не тащите Entity в UI.
- Не смешивайте Room и моки в одном ViewModel: mock — это реализация repository.

## 2. Paging 3

### Когда нужен

- Список действительно большой/бесконечный.
- Есть `/items?page=...` или cursor.

### Когда не нужен

- 10–50 элементов: обычный `LazyColumn` проще.

### Зависимости

```kotlin
implementation("androidx.paging:paging-runtime:3.3.6")
implementation("androidx.paging:paging-compose:3.3.6")
```

### PagingSource

```kotlin
class ItemPagingSource(
    private val api: ItemApi,
) : PagingSource<Int, Item>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Item> {
        val page = params.key ?: 0
        return try {
            val response = api.getItems(page = page)
            LoadResult.Page(
                data = response.items.map(ItemDto::toDomain),
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (response.items.isEmpty()) null else page + 1,
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Item>): Int? =
        state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(position)?.nextKey?.minus(1)
        }
}
```

UI:

```kotlin
val items = viewModel.pager.collectAsLazyPagingItems()

LazyColumn {
    items(items.itemCount) { index ->
        items[index]?.let { ItemRow(it) }
    }
}
```

Не превращайте `PagingData` в обычный `StateFlow<List<T>>` — потеряете преимущества.

## 3. WebSocket

### Зависимости

Уже есть OkHttp. Добавьте только если нужен STOMP/протокол — соответствующую библиотеку.

### Простой Flow

```kotlin
fun OkHttpClient.events(url: String): Flow<String> = callbackFlow {
    val request = Request.Builder().url(url).build()
    val socket = newWebSocket(request, object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            trySend(text)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            close(t)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            close()
        }
    })

    awaitClose { socket.close(1000, "screen closed") }
}
```

### Правила

- Не держите WebSocket в ViewModel дольше, чем живёт экран, если канал не нужен приложению.
- Для reconnect сделайте отдельный слой, а не `while(true)` в UI.
- Не логируйте payload: там могут быть токены и персональные данные.
- Для STOMP используйте проверенную библиотеку, не пишите протокол руками.

## 4. Upload файла

### Retrofit

```kotlin
@Multipart
@POST("files")
suspend fun upload(
    @Part file: MultipartBody.Part,
    @Part("description") description: RequestBody?,
): UploadResponse
```

### Получение файла

```kotlin
fun ContentResolver.toMultipart(
    uri: Uri,
    mimeType: String,
    fileName: String,
): MultipartBody.Part {
    val bytes = openInputStream(uri)?.use { it.readBytes() }
        ?: error("Cannot open $uri")
    val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("file", fileName, body)
}
```

### Правила

- Не хардкодьте `file.txt` и не делайте `delay(1500)` вместо реального прогресса.
- Берите имя и MIME из `ContentResolver`, а не из пути.
- Для больших файлов не читайте всё в `ByteArray`; используйте `RequestBody` с потоком.
- Проверяйте размер и тип до отправки.
- Не просите `MANAGE_EXTERNAL_STORAGE`; используйте `ActivityResultContracts.OpenDocument`.

## 5. Google Sign-In

В v1 шаблона **не подключён**. Это рецепт на потом.

### Что нужно

1. Проект в Google Cloud.
2. OAuth client ID типа Android.
3. SHA-1 debug/release ключа.
4. Web client ID для backend-верификации.
5. `google-services.json` (не коммитить, если там есть ключи; для OAuth client ID файл обычно не нужен при Credential Manager).

### Credential Manager

```kotlin
val credentialManager = CredentialManager.create(context)

val request = GetCredentialRequest(
    listOf(
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build(),
    ),
)

val result = credentialManager.getCredential(context, request)
val credential = result.credential as? PublicKeyCredential
val idToken = credential?.authenticationResponseJson
```

Точный API зависит от версии `androidx.credentials`. Не отправляйте `idToken` в свой backend без проверки подписи на сервере.

### Почему не в v1

- Нужен OAuth client ID и SHA-1.
- Нужна серверная проверка токена.
- На эмуляторе бывают проблемы с Play Services.
- Email/password + mock-auth достаточно для демо.

## 6. Encrypted token storage

Сейчас `TokenStore` — обычный DataStore. Для production:

- замените реализацию `TokenStore` на Keystore-wrapped AES;
- не меняйте `AuthRepository` и фичи;
- не храните refresh token в логах;
- добавьте очистку при logout и при `401`;
- рассмотрите `EncryptedSharedPreferences`/Tink, но проверьте актуальность библиотеки.

Пример интерфейса уже готов:

```kotlin
interface TokenStore {
    val token: StateFlow<String?>
    val currentToken: String?
    suspend fun restore()
    suspend fun save(tokens: AuthTokens)
    suspend fun clear()
}
```

## 7. Автоматический refresh токена

Не включайте, пока backend не согласовал endpoint. Когда согласует:

```text
POST /auth/refresh
{ "refreshToken": "..." }
→ { "accessToken": "...", "refreshToken": "..." }
```

Правила:

- Обновляйте токен **один раз** при нескольких параллельных 401 (Mutex).
- Не делайте refresh для самого `/auth/login` и `/auth/refresh`.
- Если refresh не удался — очистите токен и покажите Auth.
- Не используйте `runBlocking` в UI. Refresh должен быть suspend-вызовом в repository/сетевом слое.
- Не логируйте старый и новый токен.

## 8. Deep links

Для type-safe route:

```kotlin
@Serializable
data class Details(val id: String)
```

В `NavHost`:

```kotlin
composable<Details>(
    deepLinks = listOf(
        navDeepLink<Details>(basePath = "https://example.com/details"),
    ),
) { entry ->
    val route = entry.toRoute<Details>()
    DetailsScreen(id = route.id)
}
```

Проверьте, что backend/web отдают корректные ссылки и что id кодируется.

## 9. Feature generator

Пока отложен. Когда появится:

- генерировать `feature/<name>` с `build.gradle.kts`, route, contract, ViewModel, screen, strings, test;
- не генерировать бизнес-логику;
- не добавлять фичу в `NavHost` автоматически без ревью;
- не создавать Room/Paging по умолчанию.

## 10. Push-уведомления

Если понадобятся:

- FCM + `google-services.json`;
- notification permission на Android 13+;
- не отправляйте токены в push;
- deep link из push ведёт в route contract;
- для хакатона проще начать с локального `NotificationManager`, если push не является фичей.

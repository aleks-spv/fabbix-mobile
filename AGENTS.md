# AGENTS.md

This repository currently contains:
Здесь мы разрабатывает андроид приложение, современное на июнь 2026 года, с помощью которого можно было бы смотреть интерфейс zabbix frontend. Аналог IntelliTrend Mobile for Zabbix или Tabbix, но бесплатный, с поддержкой добавления нескольких серверов, с сохранением логина и пароля, с галочкой для игнорирования ssl при подключении. mobile app specially designed for real-time monitoring and access to your Zabbix servers. The app allows its users to access their Zabbix servers from their mobile devices and check the status, manage problems or view key performance metrics. The app is optimized for use in different company structures: It supports both individual users or large companies with multiple users and servers. Вообщем нужно удобное, чтобы на экране телефона нормально было видно дашборды, какие тригеры, какие проблемы.

Development Phases
Phase 1 — Project Scaffold

settings.gradle.kts, root build.gradle.kts, libs.versions.toml
app/build.gradle.kts with all deps (Compose BOM, Hilt, Room, Retrofit, Vico)
AndroidManifest.xml (INTERNET permission)
MyApplication.kt, MainActivity.kt
Material 3 theme (dark + light)
AppNavigation.kt skeleton with placeholder screens
Phase 2 — Data Layer

Room: ServerEntity, ServerDao, AppDatabase
SecurePreferences — wraps EncryptedSharedPreferences for tokens + passwords
ZabbixApiClient — OkHttpClient factory: per-call SSL bypass when ignoreSsl=true
ZabbixApiService — single Retrofit @POST("api_jsonrpc.php") method with JSON-RPC body
All DTOs + mappers to domain models
ServerRepositoryImpl, ZabbixRepositoryImpl
Hilt modules wiring everything
Phase 3 — Server Management

ServerListScreen — swipe-to-delete, tap to select active server
AddEditServerScreen — URL, name, login, password (masked), "Ignore SSL" checkbox, "Test connection" button
ServerSwitcher top-bar component
Phase 4 — Auth & Session

LoginScreen — auto-login if credentials saved; manual login otherwise
Token stored per-server in SecurePreferences
Auto-relogin on user.login 401-equivalent (ZBXE_PERMISSION_DENIED)
Phase 5 — Core Screens

Problems: severity colors (Disaster=red, High=orange, Average=yellow, Warning=blue, Info=grey), filter chip row by severity, pull-to-refresh
Triggers: enabled/disabled toggle-able state
Hosts: status dot (green/red/grey), expandable host detail
Dashboard: scrollable widget grid using Vico for graph widgets
Phase 6 — Polish

Auto-refresh interval (configurable: 30s/1m/5m)
Search/filter bar on Problems and Hosts screens
Notification channel for unacknowledged DISASTER problems
RU + EN string resources

Non-Obvious Decisions (critical for future agents)
JSON-RPC envelope — Zabbix API is a single endpoint POST /api_jsonrpc.php. Every call wraps { jsonrpc, method, params, id, auth }. The auth field is null only for user.login.

Per-server SSL bypass — OkHttpClient must be created per-server. A server with ignoreSsl=true gets a trust-all X509TrustManager + NoopHostnameVerifier. Never use a global bypass. Use a ZabbixApiClientFactory injected via Hilt that takes a Server and returns a ZabbixApiService.

URL normalization — Users forget to add /api_jsonrpc.php. The AddEditServerScreen should strip trailing slashes and the path suffix is appended by Retrofit's @POST.

Password storage — Passwords are NEVER stored in Room plain-text. Only SecurePreferences (Android Keystore-backed) holds them; Room ServerEntity has a hasPassword: Boolean flag.

Active server — Tracked in DataStore<Preferences> (not Room). Switching servers triggers re-auth if the stored token is missing or expired.

Severity enum ordering — Zabbix severity IDs: 0=Not classified, 1=Info, 2=Warning, 3=Average, 4=High, 5=Disaster. The color palette must match these IDs exactly.

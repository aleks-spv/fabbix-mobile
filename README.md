# Fabbix Mobile

Free and open-source Android client for [Zabbix](https://www.zabbix.com/) monitoring servers. A mobile-friendly alternative to IntelliTrend Mobile for Zabbix and Tabbix.

## Features

- **Multi-server support** — add and switch between multiple Zabbix servers
- **Secure credential storage** — passwords stored via Android Keystore (EncryptedSharedPreferences), never in plain text
- **Per-server SSL bypass** — toggle SSL certificate verification for each server independently
- **Overview dashboard** — summary of problems by severity and host availability at a glance
- **Problems** — real-time problem list with severity color-coding, filtering by severity, search, pull-to-refresh, and detailed problem info (host groups, interfaces, tags, event status)
- **Hosts** — host list with availability status, interfaces (IP/DNS), templates, macros, and proxy/server monitoring info
- **Auto-refresh** — configurable refresh interval (30s / 1m / 5m)
- **DISASTER notifications** — background worker alerts for unacknowledged Disaster-level problems
- **Server export/import** — backup and restore server configurations (passwords excluded for security)
- **Dark & Light themes** — Material 3 dynamic theming
- **Localization** — English and Russian

## Screenshots

*Coming soon*

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Networking | Retrofit + OkHttp + Gson |
| Local DB | Room |
| Preferences | DataStore + EncryptedSharedPreferences |
| Charts | Vico |
| Background work | WorkManager |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 |

## Building

```bash
git clone https://github.com/your-username/fabbix-mobile.git
cd fabbix-mobile
./gradlew assembleDebug
APK will be at app/build/outputs/apk/debug/app-debug.apk

Architecture
text
com.fabbixmb.app/
├── data/
│   ├── local/          # Room DB, DAOs, SecurePreferences, AppPreferences
│   ├── remote/         # ZabbixApiService, ZabbixApiClientFactory
│   ├── repository/     # ZabbixRepositoryImpl, ServerRepositoryImpl
│   └── worker/         # DisasterCheckWorker
├── di/                 # Hilt modules (Database, Network, Repository)
├── domain/
│   ├── model/          # Problem, Host, Trigger, Severity, Dashboard
│   └── repository/     # Repository interfaces
├── navigation/         # AppNavigation, Screen routes
├── presentation/
│   ├── common/         # SeverityBadge, UiState
│   ├── hosts/          # HostsScreen, HostsViewModel
│   ├── login/          # LoginScreen, LoginViewModel
│   ├── main/           # MainScreen (bottom nav)
│   ├── overview/       # OverviewScreen, OverviewViewModel
│   ├── problems/       # ProblemsScreen, ProblemsViewModel
│   ├── servers/        # ServerListScreen, AddEditServerScreen
│   ├── settings/       # SettingsScreen, SettingsViewModel
│   └── triggers/       # TriggersScreen, TriggersViewModel
└── theme/              # Color, Type, Theme
Zabbix API Compatibility
The app communicates with Zabbix via JSON-RPC API (/api_jsonrpc.php). Tested with:

Zabbix 6.0 LTS
Zabbix 6.4
Zabbix 7.0 LTS
License
This project is licensed under the MIT License — see the LICENSE file for details.

text

---

# device-kit

KMP module with device/install identifiers and WebApp device info.

Targets: **Android**, **iOS**, **JVM** (desktop).

## What is inside

- `Installation` (`expect/actual`)
  - `deviceId(): String`
  - `installId(): String?`
- `Installations` helper singleton
- `LdsWebAppDeviceInfo` + `provideLdsWebAppDeviceInfo()`
- `toGetDeviceInfoJson()` for JS bridge payload

## LdsWebAppDeviceInfo

Снимок данных устройства и приложения для WebView JS bridge (как `Android.getDeviceInfo()` в appWebView).

### Поля

| Поле | Назначение |
|------|------------|
| `appName` | Имя приложения |
| `appVersionName` | Версия (строка) |
| `appVersionCode` | Версия (число) |
| `installSource` | Источник установки (магазин / `direct`) |
| `deviceModel` | Модель устройства |
| `deviceManufacturer` | Производитель |
| `osVersion` | Версия ОС |
| `deviceId` | `Installations.deviceId()` (см. политику ниже) |
| `locale` | Локаль |
| `timeZone` | Часовой пояс |

`installId` в эту модель **не входит** — только через `Installations.installId()`.

### Откуда берутся значения

**Android** (`provideLdsWebAppDeviceInfo` в `androidMain`):

- `appName`, версии, `installSource` — из `PackageManager` / `applicationInfo`
- `deviceModel`, `deviceManufacturer`, `osVersion` — `Build.*`
- `locale`, `timeZone` — `Locale.getDefault()`, `TimeZone.getDefault()`
- `deviceId` — `Installations.deviceId()`
- Нужен `DeviceInfo.initialize(context)`. Если context не задан — fallback с пустым `deviceId` и дефолтными `appName`/`version*`

**iOS**:

- `appName`, версии — `NSBundle.mainBundle` (`CFBundleDisplayName` / `CFBundleName`, `CFBundleShortVersionString`, `CFBundleVersion`)
- `deviceModel`, `osVersion` — `UIDevice`
- `deviceManufacturer` — `"Apple"`
- `installSource` — `"app_store"`
- `locale` — `preferredLocalizations`
- `timeZone` — `NSTimeZone.localTimeZone`
- `deviceId` — `Installations.deviceId()` (Keychain)

**JVM / Desktop**:

- Статические заглушки: `appName = "Example Desktop"`, `versionName = "1.0"`, `versionCode = 1`, `installSource = "direct"`, `deviceManufacturer = "Desktop"`
- `deviceModel` / `osVersion` — `System.getProperty("os.name")` / `"os.version"`
- `deviceId` — `Installations.deviceId()` (файл в data dir)

### API

```kotlin
val info: LdsWebAppDeviceInfo = provideLdsWebAppDeviceInfo()
val json: String = info.toGetDeviceInfoJson()
```

`toGetDeviceInfoJson()` возвращает JSON с двумя блоками (как в старом `WebAppInterface.getDeviceInfo()`):

```json
{
  "appInfo": {
    "appName": "...",
    "versionName": "...",
    "versionCode": 1,
    "installSource": "..."
  },
  "deviceInfo": {
    "model": "...",
    "device_id": "...",
    "manufacturer": "...",
    "androidVersion": "...",
    "locale": "...",
    "timeZone": "...",
    "nestedData": { "someString": "...", "someNumber": 12345 }
  }
}
```

Поле `androidVersion` в JSON — историческое имя; на iOS/desktop туда попадает `osVersion`.

### Использование в приложении

- **JS bridge** (`composeApp`): `registerLdsWebAppJsHandlers(deviceInfo = …)` — отдельные handlers (`getDeviceId`, `getDeviceModel`, …) и `getDeviceInfo` → `toGetDeviceInfoJson()`
- **Push** (`PushTokenSender`): `User-Agent` из `appVersionName`; `device_id` в form — `Installations.deviceId()`
- Обычно один раз: `val webAppDeviceInfo = remember { provideLdsWebAppDeviceInfo() }` (значения фиксируются на момент первого вызова)

## Android initialization

Before using `Installation` / `provideLdsWebAppDeviceInfo()` on Android, initialize context once:

```kotlin
import dev.walhalla.kmp.device.DeviceInfo

DeviceInfo.initialize(applicationContext)
```

If not initialized, Android code that requires context will fail with:
`DeviceInfo is not initialized`.

## Basic usage

```kotlin
import dev.walhalla.kmp.device.Installations
import dev.walhalla.kmp.device.provideLdsWebAppDeviceInfo

val deviceId = Installations.deviceId()
val installId = Installations.installId()

val info = provideLdsWebAppDeviceInfo()
val json = info.toGetDeviceInfoJson()
```

## Current storage policy

- Android:
  - `deviceId` -> `Settings.Secure.ANDROID_ID`
  - `installId` -> `filesDir/uuid.txt`
- iOS:
  - `deviceId` -> Keychain UUID
  - `installId` -> `NSUserDefaults` UUID
- JVM/Desktop:
  - `deviceId` / `installId` -> local files in app data directory


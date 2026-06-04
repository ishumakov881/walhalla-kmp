# device-kit

KMP module with device/install identifiers and WebApp device info.

Targets: **Android**, **iOS**, **JVM** (desktop).

## What is inside

- `Installation` (`expect/actual`)
  - `deviceId(): String`
  - `installId(): String?`
- `Installations` helper singleton
- `AppDeviceInfo` + `provideLdsWebAppDeviceInfo()`
- `toGetDeviceInfoJson()` for JS bridge payload
- `MACKeeper` — MAC, IP, шлюз (`obtainMacAddress()` → `macAddress`)
- `deviceMacAddress()` — то же MAC одним вызовом (раньше `deviceMacAddressForStats`); внутри вызывает `obtainMacAddress()`. Для stats обычно достаточно `provideLdsWebAppDeviceInfo().mac`
- `connectedWifiFrequencyMhz()` — частота Wi‑Fi для `device/stats` (`freq`, только Android)

## Параметры по платформам

Что можно получить из kit.  
**✅** — заполняется штатно · **⚠** — часто пусто / заглушка · **❌** — не используется (`""` / `null`).

| Параметр | API | Android | iOS | JVM (desktop) |
|----------|-----|---------|-----|----------------|
| **Идентификаторы** |
| `deviceId` | `Installations.deviceId()` | ✅ `ANDROID_ID` | ✅ Keychain UUID | ✅ файл в data dir |
| `installId` | `Installations.installId()` | ✅ `filesDir/uuid.txt` | ✅ `NSUserDefaults` | ✅ файл в data dir |
| **Приложение** (`AppDeviceInfo`) |
| `appName` | `provideLdsWebAppDeviceInfo()` | ✅ `PackageManager` | ✅ `NSBundle` | ⚠ `"Example Desktop"` |
| `appVersionName` | ↑ | ✅ | ✅ | ⚠ `"1.0"` |
| `appVersionCode` | ↑ | ✅ | ✅ | ⚠ `1` |
| `installSource` | ↑ | ✅ installer / `direct` | ⚠ `"app_store"` | ⚠ `"direct"` |
| **Устройство / ОС** |
| `deviceModel` | ↑ | ✅ `Build.MODEL` | ✅ `UIDevice.model` | ✅ `os.name` |
| `deviceManufacturer` | ↑ | ✅ `Build.MANUFACTURER` | ⚠ `"Apple"` | ⚠ `"Desktop"` |
| `osVersion` | ↑ | ✅ `Build.VERSION.RELEASE` | ✅ `systemVersion` | ✅ `os.version` |
| `locale` | ↑ | ✅ | ✅ | ✅ |
| `timeZone` | ↑ | ✅ | ✅ | ✅ |
| **device/stats** (в `AppDeviceInfo`, ldsonline `StatsSendWorker`) |
| `sdk` | ↑ | ✅ `SDK_INT` | ⚠ = `osVersion` | ❌ `""` |
| `board` | ↑ | ✅ `Build.BOARD` | ❌ | ❌ |
| `brand` | ↑ | ✅ `Build.BRAND` | ⚠ `"Apple"` | ⚠ `"Desktop"` |
| `statsDevice` | ↑ | ✅ `MANUFACTURER MODEL` | ✅ `UIDevice.model` | ✅ `os.name` |
| `hardware` | ↑ | ✅ `Build.HARDWARE` | ❌ | ❌ |
| `mac` | ↑ (через `MACKeeper`) | ⚠ Wi‑Fi / `wlan*` | ⚠ часто `02:00:00:00:00:00` | ⚠ интерфейс / заглушка |
| `wifiFrequencyMhz` | ↑ | ✅ при Wi‑Fi | ❌ `null` | ❌ `null` |
| **Сеть** (`MACKeeper`, отдельно от snapshot) |
| `ipAddress` | `MACKeeper.ipAddress` после `obtainMacAddress()` | ✅ Wi‑Fi | ✅ `getifaddrs` | ✅ |
| `defaultGateway` | `MACKeeper.defaultGateway` | ✅ DHCP | ❌ | ⚠ ОС-зависимо |
| `macAddress` | `MACKeeper.macAddress` | ⚠ см. `mac` | ⚠ см. `mac` | ⚠ см. `mac` |
| `deviceMacAddress()` | top-level (≈ `macAddress` после `obtainMacAddress()`) | ⚠ | ⚠ | ⚠ |
| `is5GHzBandSupported()` | `MACKeeper` | ✅ API 21+ | ❌ `false` | ❌ `false` |
| `connectedWifiFrequencyMhz()` | top-level | ✅ | ❌ | ❌ |
| **JS bridge** |
| JSON `getDeviceInfo` | `toGetDeviceInfoJson()` | ✅ | ✅ | ✅ |

Для stats в приложении: `release` = `osVersion`, поле формы `device` = `statsDevice`, `version` = `appVersionName` (см. `provideDeviceStatsPlatformFields` в composeApp).

## LdsWebAppDeviceInfo (`AppDeviceInfo`)

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
| `sdk` | SDK / версия ОС для stats |
| `board` | `Build.BOARD` (Android) |
| `brand` | `Build.BRAND` / `"Apple"` |
| `statsDevice` | Поле `device` в `device/stats` |
| `hardware` | `Build.HARDWARE` (Android) |
| `mac` | MAC после `MACKeeper.obtainMacAddress()` |
| `wifiFrequencyMhz` | MHz Wi‑Fi (`freq` в stats, Android) |

`installId` в эту модель **не входит** — только через `Installations.installId()`.

При каждом `provideLdsWebAppDeviceInfo()` на всех платформах вызывается `MACKeeper.obtainMacAddress()` — актуальный `mac` в snapshot.

### Откуда берутся значения

**Android** (`provideLdsWebAppDeviceInfo` в `androidMain`):

- `appName`, версии, `installSource` — из `PackageManager` / `applicationInfo`
- `deviceModel`, `deviceManufacturer`, `osVersion` — `Build.*`
- `locale`, `timeZone` — `Locale.getDefault()`, `TimeZone.getDefault()`
- `deviceId` — `Installations.deviceId()`
- `sdk`, `board`, `brand`, `statsDevice`, `hardware`, `mac`, `wifiFrequencyMhz` — `Build.*` + `MACKeeper` + `connectedWifiFrequencyMhz()`
- Нужен `DeviceInfo.initialize(context)`. Если context не задан — fallback с пустым `deviceId` и дефолтными `appName`/`version*`

**iOS**:

- `appName`, версии — `NSBundle.mainBundle` (`CFBundleDisplayName` / `CFBundleName`, `CFBundleShortVersionString`, `CFBundleVersion`)
- `deviceModel`, `osVersion` — `UIDevice`
- `deviceManufacturer` — `"Apple"`
- `installSource` — `"app_store"`
- `locale` — `preferredLocalizations`
- `timeZone` — `NSTimeZone.localTimeZone`
- `deviceId` — `Installations.deviceId()` (Keychain)
- stats-поля: `sdk` = `systemVersion`, `brand` = `"Apple"`, `statsDevice` = `model`, `mac` через `MACKeeper`

**JVM / Desktop**:

- Статические заглушки: `appName = "Example Desktop"`, `versionName = "1.0"`, `versionCode = 1`, `installSource = "direct"`, `deviceManufacturer = "Desktop"`
- `deviceModel` / `osVersion` — `System.getProperty("os.name")` / `"os.version"`
- `deviceId` — `Installations.deviceId()` (файл в data dir)
- stats-поля: частично заглушки, `mac` через `MACKeeper` / `NetworkInterface`

### API

```kotlin
val info: AppDeviceInfo = provideLdsWebAppDeviceInfo()
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
- **Stats** (`device/stats`): поля из `AppDeviceInfo` (`sdk`, `board`, `brand`, `statsDevice`, `mac`, `wifiFrequencyMhz`, …)
- Обычно один раз: `val webAppDeviceInfo = remember { provideLdsWebAppDeviceInfo() }` (значения фиксируются на момент первого вызова; для stats лучше вызывать `provideLdsWebAppDeviceInfo()` заново)

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

// MAC отдельно (если не нужен полный snapshot):
val mac = deviceMacAddress()  // было: deviceMacAddressForStats()
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


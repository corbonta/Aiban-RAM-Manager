# Aiban RAM Manager

**A root-enabled RAM monitoring and Android process management application.**

Aiban RAM Manager is an Android utility built with Kotlin and Jetpack Compose. It provides RAM monitoring, process inspection, background application cleanup, application freezing, and gaming profiles.

> **Project status:** Early development — version 1.0.

## Features

### RAM Monitoring
- Real-time RAM usage monitoring.
- Reads total and available memory from `/proc/meminfo`.
- Live memory chart covering the latest 5 minutes.
- Optional persistent RAM history.
- Stores up to 1,440 history samples (approximately 12 hours at 30-second intervals).

### Process Management
- Lists running application processes.
- Displays process RSS memory usage.
- Groups multiple processes belonging to the same application.
- Reads Android process importance through `oom_score_adj`.
- Supports force-stopping selected applications.
- Identifies protected and potentially risky applications.

### Background Cleanup
- Root-based background application cleanup.
- Selects eligible background processes using `oom_score_adj`.
- Excludes protected applications, system applications, and the selected game.
- Skips cleanup when process importance cannot be determined.
- Estimates the change in available RAM after cleanup.

**Note:** The reported memory change is an estimate based on available RAM before and after cleanup. It is not a guarantee of memory reclaimed from individual applications.

### Application Freezing
- Freeze applications using Android package management commands.
- Restore frozen applications.
- View disabled applications.
- Supports individual and bulk unfreezing.
- Protects critical system packages from freezing.

### Game Booster
- Select a game.
- Configure individual game profiles.
- Optional background cleanup before launching a game.
- Optional Do Not Disturb activation.
- Optional automatic game launching.
- Restores the previous Do Not Disturb state when returning to the application.

## Root Access

Aiban RAM Manager uses [libsu](https://github.com/topjohnwu/libsu) to execute privileged shell commands.

Root access is required for advanced features, including:
- Process inspection through privileged shell commands.
- Force-stopping applications.
- Background cleanup.
- Application freezing and unfreezing.

RAM monitoring itself does not require root access.

Compatible root management environments may include Magisk and KernelSU, depending on device and ROM compatibility.

## Technology Stack

| Technology | Usage |
|---|---|
| Kotlin | Main programming language |
| Jetpack Compose | User interface |
| Material 3 | UI components |
| AndroidX | Android application libraries |
| libsu 6.0.0 | Root shell integration |
| Gradle Kotlin DSL | Build configuration |
| Java 11 compatibility | Compilation target |

## Requirements

| Requirement | Version |
|---|---|
| Minimum Android | Android 8.0 (API 26) |
| Compile SDK | 37 |
| Target SDK | 37 |
| Java compatibility | 11 |
| Android Studio | Recent version with SDK 37 support |

Some advanced features require a rooted Android device.

## Build from Source

### 1. Clone the repository

```bash
git clone https://github.com/corbonta/Aiban-RAM-Manager.git
cd Aiban-RAM-Manager
```

### 2. Open the project

Open the project directory in Android Studio.

Allow Gradle to synchronize and download the required dependencies.

### 3. Build the debug APK

```bash
./gradlew assembleDebug
```

The generated APK will normally be located at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 4. Install using ADB

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Permissions and Safety

Aiban RAM Manager uses Android package visibility to inspect installed applications.

It also requests notification policy access when the user enables Do Not Disturb functionality.

Root operations can affect system stability. Freezing or stopping important applications may interrupt services or cause unexpected behavior.

Always review the application and understand the effects of privileged operations before granting root access.

## Screenshots

Screenshots will be added as the interface and project documentation mature.

## Roadmap

Planned development directions:

- [ ] Improve RAM monitoring and historical statistics.
- [ ] Refine process inspection and cleanup reporting.
- [ ] Expand device and ROM compatibility.
- [ ] Improve application freezing safeguards.
- [ ] Enhance Game Booster profiles.
- [ ] Add more documentation and screenshots.
- [ ] Expand testing across Android devices.

Roadmap items are development goals, not currently guaranteed features.

## Project Links

- **GitHub:** https://github.com/corbonta/Aiban-RAM-Manager
- **Telegram Channel:** https://t.me/aibanrammanager

## Developer

**cansorwy aka corbonta**

## License

This project is licensed under the Apache License 2.0.

See the [LICENSE](LICENSE) file for details.

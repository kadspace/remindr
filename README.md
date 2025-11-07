# Remindr Compose Multiplatform Sample

This project demonstrates a Kotlin Multiplatform setup that runs on Android and the web (via Compose for Web/Wasm). The UI replicates the "Example3" agenda page from the [Kizitonwose Calendar](https://github.com/kizitonwose/Calendar) samples, providing a weekly agenda view with mock events.

## Requirements

- JDK 17 or newer
- Android Studio Hedgehog (or newer) for running the Android target

## Building & Running

### Android

1. Import the project into Android Studio.
2. Select the `composeApp` configuration.
3. Run on an emulator or device running Android 7.0 (API 24) or newer.

### Web (Wasm)

From the project root, use a locally installed Gradle distribution:

```bash
gradle wasmJsBrowserDevelopmentRun
```

Gradle starts a local development server. Open the printed URL (usually `http://localhost:8080`) to interact with the agenda view in the browser.

## Project Structure

- `composeApp`: Shared Compose Multiplatform code with platform-specific entry points for Android and Web.
  - `commonMain`: UI implementation shared by both platforms.
  - `androidMain`: Android `Activity` hosting the shared `App` composable.
  - `wasmJsMain`: Browser bootstrap code using Compose Wasm.


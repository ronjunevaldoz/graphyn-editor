# app/androidApp

Android entry point for the Graphyn demo app.

## Running

```bash
./gradlew :app:androidApp:installDebug
```

Or open the root project in Android Studio and run the `androidApp` configuration.

## Build

```bash
./gradlew :app:androidApp:assembleDebug   # debug APK
./gradlew :app:androidApp:assembleRelease # release APK (requires signing config)
```

# app/desktopApp

Desktop (JVM) entry point for the Graphyn demo app.

## Running

```bash
./gradlew :app:desktopApp:run
```

## Packaging

```bash
./gradlew :app:desktopApp:packageDistributionForCurrentOS
```

Produces a native installer (`.dmg` on macOS, `.msi` on Windows, `.deb`/`.rpm` on Linux).

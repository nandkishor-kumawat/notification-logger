# Notification Logger

An Android app that logs every notification **locally**, lets you browse, filter, export and clean them up, and never sends data anywhere (the app has no `INTERNET` permission).

## Build

```
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:testDebugUnitTest      # JVM unit tests
./gradlew :app:connectedDebugAndroidTest   # instrumented tests (needs a device)
```

`connectedDebugAndroidTest` uninstalls the app from the device afterwards, which wipes its data and notification access. To keep them, install the test APK and run it yourself:

```
./gradlew :app:installDebug :app:installDebugAndroidTest
adb shell am instrument -w in.i8n.notificationlogger.test/androidx.test.runner.AndroidJUnitRunner
adb uninstall in.i8n.notificationlogger.test
```

## Versioning

`MAJOR.MINOR.PATCH` is set at the top of `app/build.gradle.kts`. `versionCode = MAJOR*10000 + MINOR*100 + PATCH`, so each part must stay below 100 and every release increases the code.

## Release build

1. Create a keystore once and keep it (and its passwords) safe; losing it means you can never update the app under the same signature:
   ```
   keytool -genkeypair -v -keystore notificationlogger.jks -alias release -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Create `keystore.properties` at the repo root (it is git-ignored, as are `*.jks`):
   ```
   storeFile=/absolute/path/to/notificationlogger.jks
   storePassword=...
   keyAlias=release
   keyPassword=...
   ```
3. Build:
   ```
   ./gradlew :app:assembleRelease   # app/build/outputs/apk/release/app-release.apk
   ./gradlew :app:bundleRelease     # app/build/outputs/bundle/release/app-release.aab
   ```
   Without `keystore.properties` the release outputs are unsigned.

## Database changes

Room exports its schema to `app/schemas/` on every build; commit those files. When you change an entity, bump `AppDatabase.version`, add a `Migration`, and extend `MigrationTest`.

## Troubleshooting: logging stops after a while

Some phone makers (Xiaomi, Samsung, OnePlus, Huawei, Oppo, Vivo…) aggressively stop background apps, which can disconnect the notification listener. The app asks Android to reconnect it, but if notifications still go missing:

- Settings → Apps → Notification Logger → Battery → choose **Unrestricted** / **Don't optimize**.
- On Xiaomi/Oppo/Vivo/Huawei also enable **Autostart** for the app, and lock it in the recent-apps screen.
- Check that notification access is still on (Settings → Notifications → Device & app notifications).
- See https://dontkillmyapp.com for per-brand steps.

## More

- [Privacy policy](app/src/main/assets/privacy-policy.md)
- [Manual test matrix](docs/manual-test-matrix.md)
- [Distribution notes](docs/distribution.md)
- [Store listing (en-US)](docs/store-listing.md)

## License

Notification Logger is free software, licensed under the [GNU General Public License v3.0](LICENSE). You may use, study, share and modify it; modified versions you distribute must also be released under the GPL-3.0 with their source.

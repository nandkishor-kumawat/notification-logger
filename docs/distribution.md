# Distribution

Pick one or more. All start from the signed release APK/AAB (see README).

## Sideload (simplest)
Share the signed APK. On Android 13+ users must allow **restricted settings** for notification access (the app explains this). No review needed.

## F-Droid
Fits well (GPL-3.0 licensed, see `LICENSE`): no internet permission, no proprietary dependencies, no tracking. Requires the source to be public, a tagged release, and a metadata file in the F-Droid data repository. F-Droid builds and signs the app itself unless you use reproducible builds.

## Google Play
- Upload the **AAB**; enrol in Play App Signing.
- **Privacy policy URL** is required: publish `privacy-policy.md` at a public URL.
- **Data safety form:** the app collects and shares no data and has no network permission. Declare exactly that.
- Notification access is a sensitive capability: the app's core purpose is reading notifications, so describe that clearly in the listing and in-app (the permission card does). Check Google's current policy for apps that use a notification listener before submitting.
- The app does **not** use `QUERY_ALL_PACKAGES`. It resolves names/icons of launchable apps through a `<queries>` entry, so no special permission declaration is needed. Apps with no launcher icon (system services) show their package name.
- Target API level must meet Play's current requirement; this app targets the latest.

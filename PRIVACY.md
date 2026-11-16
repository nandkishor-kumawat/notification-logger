# Privacy Policy – Notification Logger

_Last updated: 2026-11-14_

Notification Logger keeps a local history of the notifications your phone shows. This page explains what it does with that data.

## Your data stays on your device

- The app has **no internet permission**. It cannot send anything to a server, and it contains no analytics, advertising or crash-reporting code.
- Logged notifications (app name, title, text, progress and time) are stored in a database on your phone only.
- Backups are disabled (`allowBackup=false`), so the log is not copied to cloud backups or to a new device automatically.

## What the app reads

The app uses Android's **notification access**, which you grant in system settings. With it, the app reads each notification's content as it is posted. You decide which apps are logged in _Filter settings_, and you can revoke access at any time in system settings.

## Exporting

Exporting to a text file only happens when you choose it. You pick where the file is saved through Android's file picker. What you do with that file afterwards is up to you.

## Protecting the log

- **Require authentication** asks for your fingerprint, face, PIN, pattern or password before the log is shown, and locks again shortly after you leave the app.
- While authentication is enabled, the app's window is hidden from the recent-apps screen and from screenshots.

## Deleting your data

- _Clear history_ removes the log of one app or of all apps.
- _Auto-delete old notifications_ removes entries older than the period you choose.
- Uninstalling the app removes all of its data.

## Notification actions

If you enable _Open notification on tap_, the app keeps each notification's tap action in memory only (never on disk) so you can open the original app screen. It is lost when the app process ends.

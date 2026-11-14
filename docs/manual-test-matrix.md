# Manual test matrix

Run before each release. Tick per device; note the Android version and device.

**Devices to cover:** Android 8 (API 26, the minimum), 10, 13, 14, 15; one Pixel/stock device; one aggressive-battery OEM phone (Xiaomi, Samsung, OnePlus…).

| # | Check | Expected |
|---|-------|----------|
| 1 | Fresh install, open app | Permission card shown, no crash |
| 2 | Tap Allow access, grant in settings, return | Card disappears |
| 3 | Sideloaded install on Android 13+ | Restricted-settings hint shown; "Allow restricted settings" in App info unblocks the switch |
| 4 | Receive a notification | It appears in the list within a second or two |
| 5 | Download/progress notification | One row per change; versions linked (Notification updates dialog) |
| 6 | Same notification posted repeatedly | Duplicates grouped; Hide duplicates shows one |
| 7 | Filter settings: exclude an app, then whitelist mode | Excluded app no longer logged; set is kept when switching mode |
| 8 | Long-press a card | Text copied |
| 9 | Enable "Open notification on tap", tap a recent entry | Original app screen opens; old entries say "not available anymore" |
| 10 | Export to text file, open the file | Correct entries, order and format |
| 11 | Auto-delete 1 day with old data | Old rows removed |
| 12 | Clear history (one app / all) | Confirm dialog, then removed |
| 13 | Require authentication on, leave app, return | Prompt after ~0.5 s; recents thumbnail blank |
| 14 | Turn authentication off | Asks for authentication first |
| 15 | Rotate phone / change font size to largest | No lock-out, no clipped text |
| 16 | TalkBack on | Menu, lists and dialogs are readable, checkboxes announce state |
| 17 | Reboot phone | Logging resumes without opening the app |
| 18 | Leave phone idle several hours (OEM phone) | Notifications still logged; if not, follow the battery tip in README |
| 19 | Update over a previous version | Data and settings kept |

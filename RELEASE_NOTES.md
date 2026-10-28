## DhizukuF v1.5.3 (11) — DRAFT

> Remove the DRAFT marker when tagging.

Hardening, log settings UX, and string coverage after 1.5.2.

### What's fixed

- **App management**: enabling an app clears **Block**.
- **Logs UI**: level / categories / size disabled while logging is off; **Export** and **Clear** disabled when the journal is empty; Clear still works with logging off if data remains.
- **i18n**: every locale has the full **272** string keys. Logs settings strings are localized for German, French, Spanish, Polish, Italian, Portuguese (PT/BR), Japanese, Chinese (CN/TW). Remaining filled keys use English until a native translation is added. Ukrainian and Russian already had Logs; missing keys completed.
- **AppIconCache**, **MyDhizukuProvider**, **README** screenshots layout.

### Good to know

- No dish protocol change. Rebuild dex only if `dish-client/` changed.
- dish console messages stay English by design.

### Install

1. Install the APK (and Hub if needed).
2. Keep Device Owner.
3. Export dish files only if the client dex changed.

## DhizukuF v1.5.2 (10)

DhizukuF is a fork of [Dhizuku](https://github.com/iamr0s/Dhizuku) that shares Android **Device Owner** permissions with
other apps and adds **dish**, a Device Owner console for any terminal.

Bugfix release after 1.5.1: dish client dex aligned with source, null-safety, log export lock, launcher switch retry, and cleaner deactivate.

### What's fixed

- **dish dex in the APK** matches `dish-client/src` (client 1.1: split classes, stronger handshake, `;` chains, `--help` / `--version`).
- Activation **Retry** is null-safe; binder wrapper fails cleanly if the DPM service binder is missing.
- dish: Binder **transaction too large** prints a clear error (no useless reconnect).
- **Logs export** holds the store lock for the whole write.
- **Customization**: launcher name/icon switch retries once if the system does not confirm it.
- **Deactivate** clears only the active owner mode (profile *or* device) and refreshes state.
- Account / app icon fallbacks without force-unwrap.
- Dead code cleanup in the Dhizuku API service; English comments in prefs helpers.

### Good to know

- **Export dish files again** after updating so Termux / adb get the new `dish` + `dish_dhizukuf.dex`.
- Full list: [CHANGELOG.md](CHANGELOG.md). dish guide: [docs/DISH.md](docs/DISH.md).

### Install

1. Install the APK (and Hub APK if you use the Compatibility Hub).
2. Keep DhizukuF as Device Owner.
3. Open **Terminal (dish)** → **Export dish files** if you use the console.

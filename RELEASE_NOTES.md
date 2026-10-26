## DhizukuF v1.5.1 (9)

DhizukuF is a fork of [Dhizuku](https://github.com/iamr0s/Dhizuku) that shares Android **Device Owner** permissions with
other apps and adds **dish**, a Device Owner console for any terminal.

This is a **dish client** release: the console utility is reworked for stability and compatibility. App features of 1.5.0 (Customization, Logs) are unchanged.

### What's new (dish client 1.1)

- **Clearer client code** - split into `DishMain`, `DishShell`, `DishConnection`, `DishProtocol` (same protocol 1 as the app).
- **Stronger handshake** - API-aware `IActivityManager` order (prefers `broadcastIntentWithFeature` on Android 11+), looks up `activity` and `activity_task`, retries with a longer wait, checks the binder with `pingBinder`, reconnects once after a dead binder.
- **CLI** - `dish -c ""` exits 0; `dish --help` / `--version` work without talking to the app; several commands in one line with `;` (`dish -c "status ; device"`).
- **Interactive** - `!n` (nth history line), `reconnect`, `client-version`; history capped at 100 entries.
- **Tokenize** - bare `;` is a separator; inside double quotes only `"`, `\`, `$` and the backtick are escaped (shell-like).
- **Tuning** - env `DISH_DEBUG`, `DISH_CONNECT_ATTEMPTS`, `DISH_CONNECT_TIMEOUT` for slow devices / TV boxes.
- **Docs** - [docs/DISH.md](docs/DISH.md) describes the client, builtins and environment.

### Good to know

- After installing this build, **export dish files again** from the Terminal (dish) card so the terminal gets the new `dish` script and `dish_dhizukuf.dex`. An old dex on disk keeps the previous client.
- Full list of changes: [CHANGELOG.md](CHANGELOG.md). dish guide: [docs/DISH.md](docs/DISH.md).

### Install

1. Install the APK (or the Hub APK if you use the Compatibility Hub).
2. Keep DhizukuF as Device Owner (or Profile Owner where supported).
3. Export dish files again if you use the terminal console.

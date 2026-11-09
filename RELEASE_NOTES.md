## DhizukuF v1.8.0 (14) — DRAFT

### Highlights

- **Correct three-mode administration support**: Device Administrator, Profile Owner and
  Device Owner are now gated by what Android really offers — a new `AdminModes` check shows
  the owner-granting cards (Dhizuku / Shizuku / ADB) only where owner provisioning exists,
  keeps the Administrator card everywhere, and documents the per-version limits
  (Android 9+ deprecated admin policies, Android 14+ headless-system-user requirements).
  See [docs/ADMINISTRATOR.md](docs/ADMINISTRATOR.md).
- **Minimum SDK is now API 24 (Android 7.0)** — required by the Compose and
  androidx.navigation libraries; Android 4.4–6.0 cannot run this UI and is not supported.
- **Administrator can also be granted through Shizuku/Dhizuku**: the Activate page offers
  *Administrator* as a third owner type — the selected app receives the plain device-admin
  right (`setActiveAdmin`, verified before success is reported).

### Fixed

- The dish card no longer shows the red "needs owner" warning in Administrator mode.
- `dish help` `~` markers now list only commands a device admin can actually use.
- The Administrator card hides itself once the right is active.
- Crash when the daemon service stopped on old Android (`stopForeground` overload).
- `setActiveAdmin` falls back to the older two-argument signature on old Android builds.

## DhizukuF v1.7.0 (13)

### Highlights

- **Synced with Dhizuku**: Dhizuku-API updated to **2.7.0** (Dhizuku-SERVER_API 0.2.0). API clients now receive lock-task mode events.
- **Administrator mode**: a third administration state next to Device Owner and Profile Owner. It is the standard Android device administrator right — activated from the new home card via the system toggle, no root or ADB needed. It is the most limited mode: dish and the Dhizuku API now work in Administrator mode and expose exactly the calls a device admin is allowed to make; owner-only commands still refuse. `dish help` marks admin-capable commands with `~` and `dish status` shows the Administrator state; `dish_dhizukuf.dex` was rebuilt. Password-monitor callbacks are logged as security alerts. The status card shows "You are Administrator"; strings are translated in all locales. See [docs/ADMINISTRATOR.md](docs/ADMINISTRATOR.md).
- **Stronger dangerous-call blocking**: `transferOwnership`, `clearDeviceOwner`, `clearProfileOwner`, `removeActiveAdmin`, `forceRemoveActiveAdmin`, `setDeviceOwner`, `setProfileOwner`, `setActiveAdmin` and similar ownership calls are blocked for the Dhizuku API and `dish api` until *Enable dangerous commands* is switched on.

### Fixed

- Security page syntax error that broke compilation.
- Settings and Security cards now appear in Administrator mode too.
- Missing `ui.theme.plus` import on the Activate and App management pages.

### Install

1. Install the APK (and Hub if needed).
2. Keep Device Owner — or try the new **Administrator** card for the limited mode.
3. Export the updated dish files — the bundled `dish_dhizukuf.dex` changed (Administrator-aware help/status).

## DhizukuF v1.6.0 (12)

### Highlights

- **Material 3 launcher icon** (Customization): original glyph by [BinTianqi](https://github.com/BinTianqi) from [Dhizuku discussion #318](https://github.com/iamr0s/Dhizuku/discussions/318), dark background, adaptive safe-zone.
- **Service notification**: tap opens the app; Settings → Service notification for quiet mode.
- **Open-source licenses** under *Activation by DhizukuF* (Shizuku API: **MIT** — [RikkaApps/Shizuku-API](https://github.com/RikkaApps/Shizuku-API)).
- **Enable dangerous commands ON** unlocks all gated dish actions (including `wipe` / full `api`) and the Dhizuku API.
- Logs UI polish; permission dialog hardening; Allow clears Block; dish batch exit codes.

### Install

1. Install the APK (and Hub if needed).
2. Keep Device Owner.
3. Optional: Customization → Icon → Material 3 (re-add home shortcut if the launcher caches the old icon).
4. Export dish files only if the client dex changed.

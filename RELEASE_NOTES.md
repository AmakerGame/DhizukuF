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

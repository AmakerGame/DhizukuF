## DhizukuF v1.8.0 (14)

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
- **dish works in Administrator mode** with exactly the commands a device admin may use;
  owner-only commands still refuse with a clear explanation.

### Fixed

- Activation through Shizuku/Dhizuku now **verifies the real system state** — "granted" is
  shown only when DevicePolicyManager actually reports the right; void/odd return values of
  private APIs no longer cause false success or false failure.
- Private `setDeviceOwner`/`setProfileOwner` are now probed across all known signatures
  instead of guessing by Android version — works on more OEM builds.
- The activation toast names only the requested, actually-achieved state and stays silent
  on failure or when the app already held the right.
- The dish card no longer shows the red "needs owner" warning in Administrator mode.
- `dish help` `~` markers now list only commands a device admin can actually use.
- The Administrator card hides itself once the right is active.
- Crash when the daemon service stopped on old Android (`stopForeground` overload).
- `setActiveAdmin` falls back to the older two-argument signature on old Android builds.

### Install

1. Install the APK (and Hub if needed).
2. Keep Device Owner — or try the new **Administrator** card for the limited mode.
3. Export the updated dish files — the bundled `dish_dhizukuf.dex` changed.

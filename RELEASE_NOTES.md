## DhizukuF v2.0.0 (16)

### Security

- **Warning before Device Owner activation through Shizuku**: this path skips the "no other users"
  check of ADB, and Android deletes a Private Space (with all its data) when a Device Owner is set.
  DhizukuF now warns, shows how many profiles it found, and asks you to confirm.
- **Tapjacking protection**: the permission dialog ignores touches that pass through another app's
  overlay (and hides overlays on Android 12+), so an overlay cannot trick you into pressing *Allow*.

### Fixed

- **Logs** reload reliably when new entries are written.

- **Correct role in the status message** in 15 languages — "Device Owner", "Profile Owner" or
  "Administrator" is shown instead of a fixed "Owner".
- **Parallel uninstalls / hub installs** no longer time out: each installer result now has its own
  `PendingIntent`.
- **dish**: `uninstall` always reports the real result; an interrupted approval wait answers "denied".
- **Changelogs and release page**: stale upstream changelogs removed, the release page shows only
  the notes of this version.

### New

- **Device Owner via QR code**: scan the code from [docs/ADMINISTRATOR.md](docs/ADMINISTRATOR.md) on the
  setup screen of a factory-reset device (tap 6 times on the Welcome screen).

### Install

1. Install the APK (and Hub if needed) — or provision a new device with the QR code.
2. Keep Device Owner — or try the **Administrator** card for the limited mode.

## DhizukuF v1.9.0 (15)

### Security

- **Enable dangerous commands now asks for the screen lock** when *Require device
  authentication* is set to *Only on confirmation* or *Both* — the same confirmation as the
  permission dialog's Allow/Refuse/Block. Switching the option off still needs nothing.
- **dish `id`/`whoami`** shows the real app label instead of raw package names.
- **Installer-result spoofing fixed**: the hub install and `dish uninstall` now require an
  unguessable token in the result broadcast — on Android < 13 the receiver cannot be marked
  not-exported, so another app could previously fake the result.

### Fixed

- **dish performance**: repeat commands reuse the terminal label stored in the dish registry —
  no PackageManager lookups on every call.
- **Shizuku activation on Android 12+**: `user_setup_complete` is bypassed during owner
  provisioning (ported from upstream) — works on already-setup devices; a stuck flag self-heals.
- **Service notification off really means off**: the daemon service is not started at all —
  no persistent notification; the API and dish still start on demand.
- **Docs**: minimum Android corrected to 7.0 (API 24); the dangerous-command table lists every
  gated form; `SECURITY.md` matches the actual `dish api` behavior.

### Install

1. Install the APK (and Hub if needed).
2. Keep Device Owner — or try the **Administrator** card for the limited mode.

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

## DhizukuF v1.6.0 (12)

### Highlights

- **Material 3 launcher icon** in Customization (any app name).  
  Icon design for adaptive icons; original discussion credit: [BinTianqi](https://github.com/BinTianqi) — [Dhizuku #318](https://github.com/iamr0s/Dhizuku/discussions/318).
- **Enable dangerous commands ON** unlocks all gated **dish** actions (including `wipe` and full `api`) and the Dhizuku API. Off keeps the previous block lists.
- **Service notification**: tap opens the app; Settings → **Service notification** for quiet mode (Android still requires a minimal FGS indicator).
- **Open-source licenses** on the home screen (under *Activation by DhizukuF*; reorder with the pencil) and a licenses page (Shizuku API / Provider: **MIT**, [RikkaApps/Shizuku-API](https://github.com/RikkaApps/Shizuku-API)).
- Logs UI: export/clear only when non-empty; level/categories/size disabled while logging is off.
- Permission dialog: main-thread UI state; safer uid checks (API 31+).
- App management: Allow clears Block.
- dish `batch` keeps the highest exit code under `--keep-going`.

### Credits

- Material-style icon discussion: [BinTianqi](https://github.com/BinTianqi) — [iamr0s/Dhizuku#318](https://github.com/iamr0s/Dhizuku/discussions/318).
- Upstream Dhizuku by [iamr0s](https://github.com/iamr0s/Dhizuku).

### Install

1. Install the APK (and Hub if needed).
2. Keep Device Owner.
3. Optional: Customization → Icon → Material 3.
4. Export dish files only if the client dex changed.

### Notes

- dish console messages stay English by design.
- Full changelog: [CHANGELOG.md](CHANGELOG.md).

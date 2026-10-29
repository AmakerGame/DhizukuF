## DhizukuF v1.6.0 (12) — DRAFT

> Remove the DRAFT marker when tagging.

### Highlights

- **Service notification**: tap opens the app; Settings toggle to quiet mode (Android still needs a minimal FGS indicator).
- **Open-source licenses** card on the home screen (reorder with the pencil) and a licenses page (synced idea from upstream).

- **Material 3 launcher icon** in **Customization** (with any app name).  
  Icon by **[BinTianqi](https://github.com/BinTianqi)** from the upstream discussion  
  [iamr0s/Dhizuku#318](https://github.com/iamr0s/Dhizuku/discussions/318).
- **Enable dangerous commands ON** unlocks all gated **dish** actions (including `wipe` and full `api`) and the **Dhizuku API**. Off keeps the previous block lists.
- Logs UI polish (export/clear only when non-empty; categories/level disabled when logging is off).
- Permission dialog hardening; App management Allow clears Block; icon cache / provider safety.

### Credits

- Material-style icon: [BinTianqi](https://github.com/BinTianqi) — [Dhizuku discussion #318](https://github.com/iamr0s/Dhizuku/discussions/318).

### Install

1. Install the APK (and Hub if needed).
2. Keep Device Owner.
3. Optional: **Customization → Icon → Material 3**.
4. Export dish files only if the client dex changed.

## DhizukuF v1.1.0-pre1 (2) — pre-release

A pre-release for testing. Please report problems in [Issues](https://github.com/AmakerGame/DhizukuF/issues).

### What's new

- **"How to use dish" dialog** — tap the *Terminal (dish)* card to see what dish is and how to start it:
  dish is not a terminal inside the app, it is a command-line tool. Export the files, then run
  `dish` in a terminal app, best of all in **Termux**. The dialog has an *Export dish files* button.
- **Device Owner / Profile Owner aware** — `dish status` shows the mode. In Profile Owner mode,
  commands that need the full Device Owner (`reboot`, `adb`, `timezone`, `private-dns`, ...) say so
  clearly; `dish help` marks them with `*` and `dish help COMMAND` has a `Needs:` line.
- **dish card always visible** — it explains the owner mode: not an owner yet, or Profile Owner.
- **Translations** — every missing string is now translated into all languages of the app (Manchu
  excepted); Hausa is new. Translations are not natively reviewed — corrections are welcome.

- **Security hardening** — no app backups of the approval database, hardened dish entry points
  (malformed requests, request floods, dialog spam), blocked destructive calls and protection of
  DhizukuF itself from `hide` / `suspend` / `clear-data`. See [SECURITY.md](SECURITY.md).

- **Enable dangerous commands** (off by default) — risky actions (reboot, clearing app data, writing
  settings, changing permissions, direct `dish api` calls, ...) need this switch, for dish and the Dhizuku API.
- **Fixes** — blocked apps stay blocked after a restart or an app update; an app that changes its signature
  loses access immediately.

- **Android TV** — TV launcher banner, remote / D-pad navigation with a focus frame, and an export fallback
  for boxes without a folder picker.
- **Text fix** — the activation card says "Activation by DhizukuF"; its description still refers to other
  Dhizuku implementations.
- **Least privilege** — unused storage permissions and an overly broad FileProvider were removed.

- **TV remote fix** — *Export dish files*, *View command* and other buttons inside cards can be selected with
  the remote, and links no longer crash devices without a browser.

### Notes

- Existing dish users: export the dish files again to get the latest launcher and client.
- Commands that change device-wide state need DhizukuF to be the **Device Owner**; Profile Owner
  support is limited.

### Install

1. Download `DhizukuF-v1.1.0-pre1.apk` below and install it (application ID `com.EdS.DhizukuF`).
2. Make DhizukuF the Device Owner — see the
   [Dhizuku activation tutorial](https://github.com/iamr0s/Dhizuku/discussions/19).
   Set the device up **without accounts**, or remove them first.
3. Tap **Terminal (dish)** for instructions, then follow [docs/DISH.md](docs/DISH.md).

Requires Android 8.0 or newer.

Full list of changes: [CHANGELOG.md](CHANGELOG.md).

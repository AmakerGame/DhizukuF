## What does this change?

<!-- One or two sentences. Link the issue: "Fixes #123". -->

## Area

- [ ] App / UI (screens, dialogs, texts)
- [ ] Android TV / D-pad / remote
- [ ] dish (server commands in `dish/`, client in `dish-client/`)
- [ ] Permission dialog / approval of apps / Dhizuku API
- [ ] Hub (`hub/`)
- [ ] Build, CI, release, documentation

## Checked

- [ ] It builds (`./gradlew assembleDebug`, CI is green).
- [ ] UI changes: tried with a **remote control (D-pad)**; every button can be reached, shows the focus frame and can be pressed with OK. The Back key works.
- [ ] dish changes: a command ends with `OK: ...` or `NOT DONE: ...`, a risky action is behind the *Dangerous commands* switch, `dish help` and [docs/DISH.md](../docs/DISH.md) are updated.
- [ ] If the dish client (`dish-client/`) changed, the dex was rebuilt (see *Rebuilding the dex* in docs/DISH.md).
- [ ] New texts are in `strings.xml` (Ukrainian too, if you can).
- [ ] [CHANGELOG.md](../CHANGELOG.md) has a line for it.

## Tested on

<!-- Device, SoC, Android version, Device Owner or Profile Owner, touch or remote. -->

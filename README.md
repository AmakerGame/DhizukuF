# DhizukuF

[![Downloads](https://img.shields.io/github/downloads/AmakerGame/DhizukuF/total?label=Downloads)](https://github.com/AmakerGame/DhizukuF/releases)
[![Latest](https://img.shields.io/github/v/release/AmakerGame/DhizukuF?label=Latest)](https://github.com/AmakerGame/DhizukuF/releases/latest)
[![License](https://img.shields.io/github/license/AmakerGame/DhizukuF?label=License)](LICENSE)
[![Build](https://img.shields.io/github/actions/workflow/status/AmakerGame/DhizukuF/build.yml?label=Build)](https://github.com/AmakerGame/DhizukuF/actions)

**DhizukuF** is a fork of [Dhizuku](https://github.com/iamr0s/Dhizuku) — a tool that shares
Android **Device Owner** permissions with other apps. On top of everything Dhizuku does, DhizukuF
adds **`dish`**, a Device Owner console you can use from any terminal (Termux, `adb shell`, any shell
app), the same way `rish` works for Shizuku.

| | |
|-|-|
| Package | `com.EdS.DhizukuF` |
| Version | see [`version.properties`](version.properties) |
| Android | 8.0 and newer |
| License | [GNU GPL v3](LICENSE) |

> [!NOTE]
> This is an unofficial fork. It is not affiliated with the original Dhizuku project or its author.
> The original README is kept as [docs/README_ORIGINAL.md](docs/README_ORIGINAL.md).

## What is added

Compared to upstream Dhizuku (full list in [CHANGELOG.md](CHANGELOG.md)):

- **dish terminal** — run Device Owner commands from any shell. Export the two files from the
  *Terminal (dish)* card in the app and use them anywhere. See [docs/DISH.md](docs/DISH.md).
- **Around 50 dish commands** — apps (hide, suspend, block uninstall, permissions, clear data),
  restrictions, camera, screen capture, keyguard, ADB, private DNS, settings and more.
- **Direct API** — `dish api METHOD ARGS...` calls any public `DevicePolicyManager` method by name
  (and other system managers), with typed arguments and no rebuild.
- **Clear answers** — every command ends with `OK: ...` or `NOT DONE: ...` and the reason; changes
  are verified by reading the value back.
- **Device Owner / Profile Owner aware** — dish works in both modes; commands that need the full
  Device Owner say so clearly in Profile Owner mode (see [docs/DISH.md](docs/DISH.md#device-owner-and-profile-owner)).
- **Safe start** — dish checks access before the prompt appears. If dish is switched off, DhizukuF
  is not the Device Owner, or you refuse the request, the console does not start.
- **Enable dish** switch in Settings, next to **Enable Dhizuku**. The two are independent.
- **Approval window for terminals** — a terminal shows up in *App management* as `Termux (dish)`
  and asks for permission; optional *Advanced confirmation window* with a Block button and the
  *Show dish* label.
- **User management layout fix** — long user names are no longer squeezed into a one-letter column.

## Quick start

1. Make DhizukuF the Device Owner. The upstream guide still applies:
   [Dhizuku activation tutorial](https://github.com/iamr0s/Dhizuku/discussions/19).

   > [!IMPORTANT]
   > Set the device up **without any accounts**, or remove them before activating.
2. Open DhizukuF → **Terminal (dish)** → **Export dish files**. You get `dish` and
   `dish_dhizukuf.dex`; keep them together.
3. Start it from a terminal:

   ```sh
   # Termux
   cp /sdcard/<folder>/dish* $PREFIX/bin/ && chmod +x $PREFIX/bin/dish
   dish
   ```

   ```text
   dish$ help
   dish$ hide com.example.app
   OK: com.example.app is now hidden
   dish$ restriction add no_install_apps
   OK: restriction no_install_apps is now active
   dish$ api setCameraDisabled true
   OK: dpm.setCameraDisabled called (no return value)
   ```
4. The first run opens an approval window in DhizukuF. Allow it once; later you can block or revoke
   it in **App management**.

More commands, the `api` syntax and rebuild steps: [docs/DISH.md](docs/DISH.md).

## Download

Builds are published on the [Releases page](https://github.com/AmakerGame/DhizukuF/releases).

## Building

```sh
./gradlew assembleRelease
```

The dish client (`dish-client/`) is plain Java without dependencies. The prebuilt
`app/src/main/assets/dish_dhizukuf.dex` can be rebuilt with `dish-client/build.sh`
(see [docs/DISH.md](docs/DISH.md#rebuilding-the-dex)).

## Repository layout

| Path | What is inside |
|-|-|
| `app/` | The Android app (Kotlin, Jetpack Compose) |
| `app/src/main/java/com/EdS/DhizukuF/dish/` | dish server side: commands, engine, approval, registry |
| `app/src/main/assets/` | `dish` launcher script and `dish_dhizukuf.dex` client |
| `dish-client/` | Java source of the dish client and its build script |
| `hidden-api/` | Hidden Android API stubs |
| `docs/` | Documentation, original README, translations, terms and privacy |

## Documentation

- [dish guide](docs/DISH.md)
- [Changelog](CHANGELOG.md)
- [Original Dhizuku README](docs/README_ORIGINAL.md)
- [Terms of Use](docs/TERMS.md) · [Privacy Policy](docs/PRIVACY.md)

## Maintainer

DhizukuF is maintained by [@AmakerGame](https://github.com/AmakerGame).
Bug reports and ideas: [open an issue](https://github.com/AmakerGame/DhizukuF/issues).

## Credits

- [Dhizuku](https://github.com/iamr0s/Dhizuku) by iamr0s and contributors — the base of this fork.
- [Dhizuku-API](https://github.com/iamr0s/Dhizuku-API) for app developers.
- The dish idea follows [Shizuku](https://github.com/RikkaApps/Shizuku)'s `rish`.

## License

DhizukuF is licensed under the [**GNU General Public License v3**](LICENSE), like the original project.

# DhizukuF

[![Downloads](https://img.shields.io/github/downloads/AmakerGame/DhizukuF/total?label=Downloads)](https://github.com/AmakerGame/DhizukuF/releases)
[![Latest](https://img.shields.io/github/v/release/AmakerGame/DhizukuF?label=Latest)](https://github.com/AmakerGame/DhizukuF/releases/latest)
[![License](https://img.shields.io/github/license/AmakerGame/DhizukuF?label=License)](LICENSE)
[![Build](https://img.shields.io/github/actions/workflow/status/AmakerGame/DhizukuF/build.yml?label=Build)](https://github.com/AmakerGame/DhizukuF/actions)

<a href="https://dhizukuf.en.uptodown.com/android" title="Download DhizukuF">
  <img src="https://stc.utdstc.com/img/mediakit/download-aao-small.png" alt="Download DhizukuF">
</a>

**DhizukuF** is a fork of [Dhizuku](https://github.com/iamr0s/Dhizuku) that shares Android **Device Owner** permissions with other apps. It also adds **`dish`** — a Device Owner shell for Termux, `adb shell`, and other terminals (similar to `rish` for Shizuku).

| | |
| --- | --- |
| Package | `com.EdS.DhizukuF` |
| Version | See [`version.properties`](version.properties) |
| Android | 8.0+ |
| License | [GNU GPL v3](LICENSE) |

> [!NOTE]
> Unofficial fork — not affiliated with the original Dhizuku project.
> Upstream README: [docs/README_ORIGINAL.md](docs/README_ORIGINAL.md).

## Screenshots

<p align="center">
  <img src="screenshots/ScreenshotDhizukuF.png" width="160" alt="Home" />
  <img src="screenshots/ScreenshotSecurity.png" width="160" alt="Security" />
  <img src="screenshots/ScreenshotCustomization.png" width="160" alt="Customization" />
  <img src="screenshots/ScreenshotEditDhizukuF.png" width="160" alt="Edit home layout" />
  <img src="screenshots/ScreenshotLogs.png" width="160" alt="Logs" />
</p>

## Features

- **dish** — Device Owner commands from any shell ([docs/DISH.md](docs/DISH.md))
- **App control** — hide, suspend, permissions, restrictions, and more
- **Security** — whitelist, dangerous-commands switch, optional biometrics
- **Customization** — theme, app name/icon, home card order
- **Android TV** — banner and D-pad friendly UI

Full history: [CHANGELOG.md](CHANGELOG.md).

## Download

| Source | Link |
| --- | --- |
| **Uptodown** | [dhizukuf.en.uptodown.com](https://dhizukuf.en.uptodown.com/android) |
| **GitHub Releases** | [Latest](https://github.com/AmakerGame/DhizukuF/releases/latest) |

## Quick start

1. Make DhizukuF the **Device Owner** (or Profile Owner where supported).
2. Open **Terminal (dish)** → **Export dish files**.
3. Run `dish` / `help` from Termux or `adb shell`.
4. Approve the first connection in DhizukuF if asked.

More detail: [docs/DISH.md](docs/DISH.md).

## Documentation

| Topic | Doc |
| --- | --- |
| dish terminal | [docs/DISH.md](docs/DISH.md) |
| Building from source | [docs/BUILDING.md](docs/BUILDING.md) |
| Customization | [docs/CUSTOMIZATION.md](docs/CUSTOMIZATION.md) |
| Security & logs | [docs/LOGS.md](docs/LOGS.md) |
| Hub (compat) | [docs/HUB.md](docs/HUB.md) |
| Privacy | [docs/PRIVACY.md](docs/PRIVACY.md) |
| Terms | [docs/TERMS.md](docs/TERMS.md) |
| Donate | [docs/DONATE.md](docs/DONATE.md) |

## Links

- [Original Dhizuku](https://github.com/iamr0s/Dhizuku)
- [Dhizuku API](https://github.com/iamr0s/Dhizuku-API)

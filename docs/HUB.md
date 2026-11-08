# Dhizuku Compatibility Hub

Apps written for the **original Dhizuku** look for the package `com.rosan.dhizuku`. DhizukuF has another package
(`com.EdS.DhizukuF`), so those apps cannot find it. The **hub** is a tiny hidden app with the original package name that
forwards the Dhizuku API to DhizukuF, so such apps work with DhizukuF again.

The idea of the hub is by [@Jozinho-netizen](https://github.com/Jozinho-netizen).

## Install

**DhizukuF > Settings > Dhizuku Compatibility Hub > Install.**

- The hub APK is built together with DhizukuF and carried inside it, so nothing is downloaded and the APK cannot be
  swapped on the way. It is installed silently when DhizukuF is the Device Owner (in Profile Owner mode Android asks
  you to confirm).
- DhizukuF may close after the install. Open it again; the status then reads *Installed and up to date*.
- The hub has no launcher icon. It shows only in the system list of apps.
- To remove it use **Remove** in the same place.

## Same key, same version

- The hub is signed with **the same key** as DhizukuF (the module reads the same keystore).
- The hub has **the same version** as DhizukuF: both read `version.properties`. When DhizukuF is updated the hub is
  older, the card says *Update available* and **Update** installs the matching hub.
- If another app with the package `com.rosan.dhizuku` is installed (the original Dhizuku), Android will not let the
  hub in. The card then shows **Replace**: a dialog names the app (*Warning: <name> (com.rosan.dhizuku) will be
  removed*) and asks *Yes, remove and install* / *Cancel*. Only after *Yes* is the original removed and the hub
  installed. If the original is the current Device Owner, Android refuses to remove it: hand the ownership over first.
  The hub is never installed over, or removed from, an app with a different signature without that confirmation.

## How it works

| Part | What the hub does |
|-|-|
| Provider `com.rosan.dhizuku.dhizuku_server.provider` and `com.rosan.dhizuku.server.provider` | passes a call (method, argument and extras with the client's Binder) to DhizukuF unchanged and returns DhizukuF's answer, which holds DhizukuF's service Binder |
| Activity for `com.rosan.dhizuku.action.request.permission` and `...REQUEST_DHIZUKU_PERMISSION` | opens DhizukuF's own permission dialog with the same extras (client uid and listener) and closes |
| Permission `com.rosan.dhizuku.permission.API` | declared like the original so clients can use it |

After the first call the client talks to DhizukuF's service **directly**, so DhizukuF sees the real calling app of every
request and the approvals in *App management* work as before. The hub only trusts a DhizukuF signed with its own key.

## Limits and what is not verified

- **What was checked.** The contract was compared with the source of the Dhizuku client library (Dhizuku-API).
  Clients built with a library from June 2024 or later ask Android which app is the owner and then look for
  `<owner package>.dhizuku_server.provider` and `<owner package>.action.REQUEST_DHIZUKU_PERMISSION`. DhizukuF declares
  both itself, so such clients work **without** the hub; for them the hub only satisfies an "is Dhizuku installed" check.
  Clients built with an **older** library hard-code `com.rosan.dhizuku`: finding the server and asking for permission
  work through the hub (they send their data in the extra `bundle`, which the hub passes on).
- **The limit.** Those older clients also pass the official admin component `com.rosan.dhizuku/.server.DhizukuDAReceiver`
  to `DevicePolicyManager`. The owner is DhizukuF, so Android refuses such a call ("no active admin"). The hub does not
  rewrite it, so an old client can connect and be approved but its Device Owner calls fail until the app is updated to a
  newer Dhizuku-API. Test with a real app: *App management* should list it after it asks for permission.
- Features that run client code inside the Dhizuku process (user services) are not forwarded.
- The Dhizuku API switches in DhizukuF (*Settings > Enable DhizukuF*, *Security > whitelist mode / dangerous commands*) apply to hub
  clients exactly as to direct ones.
- The hub needs Android 8.0 or newer, like DhizukuF.

## Build

`:hub` is a normal Gradle module; `:app` depends on `:hub:assembleRelease` and copies the APK to its assets
(`dhizuku_compat_hub.apk`). `./gradlew assembleRelease` builds both. The release workflow also attaches the hub APK
(`DhizukuF-Hub-<tag>.apk`) for a manual install.

# dish — Device Owner console for DhizukuF

`dish` runs Device Owner commands from any terminal (Termux, `adb shell`, any shell app),
the same way `rish` works for Shizuku.

1. In DhizukuF (it must be the Device Owner) open the **Terminal (dish)** card → **Export dish files**
   and pick a folder. Two files are written: `dish` and `dish_dhizukuf.dex`.
2. Put both files together where your terminal can run scripts:
   - Termux: `cp /sdcard/<folder>/dish* $PREFIX/bin/ && chmod +x $PREFIX/bin/dish`
   - adb: `adb push dish dish_dhizukuf.dex /data/local/tmp/` then `adb shell sh /data/local/tmp/dish`
   - anything else: `sh /path/to/dish`
3. Run `dish` (interactive), `dish help`, or `dish -c "status"`.
4. The first run opens DhizukuF's permission dialog for the terminal app, exactly like other apps.
   Once approved the terminal appears in the app list, where it can be blocked or revoked.

Examples: `dish status`, `dish hide com.example.app`, `dish restriction add no_install_apps`,
`dish camera disable`, `dish settings put global stay_on_while_plugged_in 3`,
`dish call --list`, `dish call setLockTaskPackages com.example.a,com.example.b`.

Destructive calls (wipe, removing the owner, password reset) are blocked in `dish call`.

## Rebuilding the dex

The client is plain Java in `dish-client/src` (no dependencies). The prebuilt dex lives in
`app/src/main/assets/dish_dhizukuf.dex`. To rebuild it after editing the client:

```sh
apt install openjdk-21-jdk-headless dalvik-exchange   # or set DX=/path/to/dx
./dish-client/build.sh
```

Keep the protocol in `dish-client/.../DishMain.java` in sync with `DishProtocol.kt`.

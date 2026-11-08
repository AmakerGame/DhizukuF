# Customization

Requested in [#2](https://github.com/AmakerGame/DhizukuF/issues/2): change the name and icon of the app, remember them for
future beta / release updates, and an AMOLED theme.

*Home > Customization* changes how DhizukuF looks. Nothing here changes what DhizukuF does, and **the package name
(`com.EdS.DhizukuF`) never changes**, so updates, approved apps and the Device Owner are not affected.

## Theme

*System* (default), *Light*, *Dark* and **AMOLED**: the dark theme with a pure black background (cards stay a little
lighter, so they can still be told apart), which saves power on an AMOLED screen. *Dynamic colors* takes the colors
from the wallpaper (Android 12 and newer). The change applies at once to every screen and dialog.

## App name and icon

Two independent choices: **the name** that the launcher shows, and **the icon**. Changing one keeps the other.

| Name | Icon |
| --- | --- |
| DhizukuF (default) | **DhizukuF**: the new icon (default) |
| Dhizuku | **Dhizuku**: the icon of the original project; it stays in the app, can be chosen at any time and cannot be deleted |
| Profile Owner | |
| Device Owner | |

Why a list and not a text field: Android cannot rename or redraw the launcher entry of an installed app, so everything
the launcher can show has to be in the app ahead of time. The manifest declares **8 launcher entries** (4 names x 2
icons, `activity-alias`), each with its own name, icon and **TV banner**. Choosing a name or an icon switches on the
right entry and switches off the others: the new one is switched on first, so there is always one entry and the app can
never disappear. The launcher may need a few seconds to refresh. The screen shows "Launcher entry in use now", read from
the system, and says so when the system did not switch.

- The default entry (DhizukuF + new icon) keeps the **old component name** of the launcher activity, so an icon that an
  earlier version left on your home screen keeps working after the update. (After you choose another name or icon,
  that old home screen icon stops working: add the new one from the app drawer.)
- Your choice is also shown in the title of the home screen.

### On a TV

A TV launcher shows the wide **banner** of an app, taken from the app itself. Every one of the 8 entries has its banner,
and the Customization screen shows the banner of the name and icon you pick. After a change the TV launcher may need a
few seconds, or a restart of the launcher or of the device, to refresh. Text fields can be left with the Up / Down keys.

## Remember for updates

*Remember the name and icon for updates* (on by default): when DhizukuF is updated (beta or release), the chosen name and
icon are kept, and put back at the start of the app if the system did not keep them. With the option **off**, the first
start after an update returns the name and the icon to the default ones. (After a reinstall nothing is kept: Android
removes the data of the app.)

## Editing the home screen

The **pencil** at the top right of the home screen, left of the X, switches the home screen to editing mode:
every card has arrows (move up / down) and an eye (hide / show). *Reset the order and show all* is the first item.
Any card can be hidden, including Settings; the pencil always stays, so a hidden card can always be brought back.
Cards that DhizukuF does not show at the moment (for example Shizuku while DhizukuF is the owner) keep their place and
are not listed. Changes show at once and the order is kept for the next start. D-pad friendly: the arrows and the eye are
normal buttons.

# Customization

*Home > Customization* changes how DhizukuF looks. Nothing here changes what DhizukuF does, and **the package name
(`com.EdS.DhizukuF`) never changes**, so updates, approved apps and the Device Owner are not affected.

## Theme

*System* (default), *Light*, *Dark* and **AMOLED**: the dark theme with a pure black background (cards stay a little
lighter, so they can still be told apart), which saves power on an AMOLED screen. *Dynamic colors* takes the colors
from the wallpaper (Android 12 and newer). The change applies at once to every screen and dialog.

## App name and icon

| Choice | What it does |
| --- | --- |
| **DhizukuF** (default) | The new icon. |
| **Dhizuku** | The icon (and the name "Dhizuku") of the original project. It stays in the app: it can be chosen at any time and cannot be deleted. |
| **My icon 1..8** | A picture of your own (*Add an icon*, any image; the middle square is used). It can be chosen and deleted; the two built-in icons cannot. |

How it works, and why it is done this way: Android cannot rename or redraw the launcher entry of an installed app.

- The two built-in icons are two launcher entries of the app (`activity-alias` in the manifest, each with its icon,
  name and **TV banner**). Choosing one switches on that entry and switches off the other, so there is always one
  entry and the app can never disappear. The launcher may need a few seconds to refresh.
- A picture of your own cannot be put into the manifest, so choosing it asks the launcher to add a **shortcut** with
  your picture and your *App name* (the system shows its own confirmation). The normal DhizukuF entry stays. The
  picture and the name are also used inside DhizukuF (the title of the home screen). A TV box has no pinned
  shortcuts: there the picture is used inside DhizukuF only, and the screen says so.
- Deleting a picture removes the file and switches its shortcut off (a shortcut on the home screen can only be
  disabled by an app; you can remove it by hand).
- *App name*: shown in the title of DhizukuF and on the shortcut of your own picture. Blank means "DhizukuF". The two
  built-in entries keep their own names (DhizukuF and Dhizuku).

## Remember for updates

*Remember the name and icon for updates* (on by default): when DhizukuF is updated (beta or release), the chosen
built-in icon, your name and your pictures are kept, and put back at the start of the app if the system did not keep
them. With the option **off**, the first start after an update returns the name and the icon to the default ones.
(After a reinstall nothing is kept: Android removes the data of the app.)

## Editing the home screen

The **pencil** at the top right of the home screen, left of the X, switches the home screen to editing mode:
every card has arrows (move up / down) and an eye (hide / show). *Reset the order and show all* is the first item.
Any card can be hidden, including Settings; the pencil always stays, so a hidden card can always be brought back.
Cards that DhizukuF does not show at the moment (for example Shizuku while DhizukuF is the owner) keep their place and
are not listed. The order is kept for the next start. D-pad friendly: the arrows and the eye are normal buttons.

## Privacy

The pictures are saved in the private storage of DhizukuF (192 x 192 px PNG) and never leave the device.

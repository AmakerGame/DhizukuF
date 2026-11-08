# Logs

DhizukuF can keep a journal of what happens: when the service starts, which app asked for access, what was allowed
or refused, which commands apps ran and what came of them, and the errors of DhizukuF itself. It helps to find out
why an app does not work and to write a good bug report.

**The journal is off by default.** While it is off nothing is saved: no file is created and no background thread runs.

## Switching it on

*Settings > Logs* (a row at the end of the Settings list):

| Option | What it does |
| --- | --- |
| **Enable logs** | Off by default. On: the **Logs** card appears on the home screen, above *Terminal (dish)*, and events start to be saved. Off: the card disappears and nothing new is saved. The entries that are already saved stay until you clear them. |
| **Log level** | *Errors only*, *Warnings*, *Normal* (default), *Debug*. An event is saved only when its level is at least the chosen one. |
| **Log categories** | Switches for *Service events*, *Permission events*, *App requests*, *Commands*, *Errors* and *Debug information*. |
| **Maximum log size** | 1, **5** (default), 10 or 25 MB. When the limit is reached the oldest entries are deleted (down to about 80 % of the limit). A smaller limit is applied at once. |
| **Export logs** | Saves a plain text file, for example `dhizukuf-logs-2026-10-18.txt`. On a TV box without a file dialog the file goes to `Android/data/com.EdS.DhizukuF/files/` and the toast shows the path. |
| **Clear logs** | Deletes every saved entry (asks first). Available only while logging is on. |

## The Logs screen

*Search* is at the top. It looks at the text of an entry, the app name and package, the type of the event, the
command and the result; several words must all be found. Below are the entries, **newest first**: time, type of the
event, the app (name and package) when the event concerns another app, a short description, and the command or the
result when there is one. Tap (or press OK on) an entry to see everything that is known: exact time, type, level,
category, app, package, UID, message, command, result, error and details. A field that is not known is not shown.
*Copy* puts the entry on the clipboard; the share icon exports the whole file.

## What is recorded

| Category | Events |
| --- | --- |
| Service events | DhizukuF service started / stopped, Device Owner status changed, Dhizuku and dish access switched on or off |
| App requests | An app requested Dhizuku access (permission dialog, dish terminal), app connected, app disconnected |
| Permission events | Permission granted, denied (also in *App management* and when an API call is refused, with the reason), app blocked |
| Commands | Command from app (Dhizuku API call or dish command), completed, failed, blocked (needs *Enable dangerous commands*) |
| Errors | Exceptions of DhizukuF; when logging is on, a crash of DhizukuF is written just before the process ends |
| Debug information | Passed permission checks, the owner status at start |

## Privacy

- The journal stays on the device (in the private storage of DhizukuF). It is shared only when **you** export it.
- A command is recorded as its first words only (`settings put global`, `api setApplicationHidden`), never the
  values behind them. The result is recorded only when it is dish's own status line (`OK: ...` / `NOT DONE: ...`),
  because other output can hold values of the device. Parcels of API calls are never recorded, only the name of the
  method (`IDevicePolicyManager.setApplicationHidden`).
- Names of accounts, e-mail addresses, passwords and the like are never written. The app name and package of the app
  that made a request are recorded, because that is the point of the journal.
- Every field has a length limit, so one entry cannot grow without bounds.
- The export header carries the version of DhizukuF, the Android version and the device model, as a bug report needs.
  Look through the file before you attach it to a public issue.

## How it works (for contributors)

`LogManager` (`logging/`) is the only writer. `log()` first reads the cached configuration and returns at once when
the event would not be saved. Accepted events go into a bounded queue (1000; a full queue drops events instead of
growing the memory) and a writer thread saves them in batches; the thread starts with the first event and ends after
20 idle seconds. The file (`files/logs/dhizukuf.log` in device protected storage, so it also works before the first
unlock) has one tab-separated, escaped line per entry (`LogCodec`). `LogStore` appends, trims by streaming (never
loads the file) and reads the newest entries; the screen keeps at most the newest 30 000 in memory and filters on a
background thread after a short typing delay.

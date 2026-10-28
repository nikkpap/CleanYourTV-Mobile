# CleanYourTV Mobile

A phone app that cleans up Android TV and Google TV over Wi-Fi, puts on **CleanYourTV's home screen** and sets a
TV up from a **profile** saved with [CleanYourTV for Windows](https://github.com/nikkpap/CleanYourTV-App).
No computer needed. Nothing is uninstalled, a backup is saved first, and **Restore** puts everything back.

It is a fork of [TidyTV](https://github.com/jolan94/tidytv) by jolan94 (MIT), which does the clean-up, the remote,
Send files, Check my TV, Free up space and Block ads. CleanYourTV Mobile adds:

- **CleanYourTV's home screen**: cleanyourtv's Apple TV style launcher with CleanYourTV's patch: the TV's Live TV
  app as a blue "Live TV" tile, the tuner's channels as a row above the apps (hide and reorder them, kept across
  reboots), the remote's TV button opening Live TV, and a hold-OK menu on every tile (Open, Move, Uninstall).
- **Seven more looks**: Ellada, Aegean night, Meteora dusk, Olympus mist, Neon grid, OLED black and Full black,
  drawn in code (`themes/make_themes.py`).
- **My profile**: pick a `.cytv`, or a `.zip` saved with *Save profile + apps*, on the phone. It installs the
  apps, turns off the same apps (every one still goes through the safety checks), puts the look on and sets
  Private DNS, animation speed and the screensaver.
- **One undo list with the PC app**: both apps keep the list on the TV itself, so Restore works from either one.

## Install

Download `CleanYourTV-Mobile-<version>.apk` from [Releases](https://github.com/nikkpap/CleanYourTV-Mobile/releases)
on your Android phone (Android 8 or newer) and open it. It installs next to TidyTV, not over it.

## Build

GitHub Actions builds it on every push (`.github/workflows/build.yml`): the home screen from
`upstream/cleanyourtv/launcher` with `tools/patch_launcher.py`, then the app. Locally:

```bash
git clone --recurse-submodules https://github.com/nikkpap/CleanYourTV-Mobile
python3 tools/patch_launcher.py /tmp/launcher-copy --row on --row-top --menu on --show-live-tv   # on a copy
./gradlew :app:assembleDebug
```

## Credits

TidyTV (jolan94, MIT), cleanyourtv (Gathin, MIT), tv-detox (Ansub Khan, MIT), Kadb (Apache 2.0). See NOTICE.
MIT licence, Copyright (c) 2026 Nikolaos K. Paridis for the CleanYourTV parts.

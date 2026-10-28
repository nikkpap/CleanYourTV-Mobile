# Themes

Seven themes for the CleanYourTV launcher (`com.gathin.tvhome`). Every wallpaper is drawn in code by
`make_themes.py` (Python 3, numpy and Pillow): there are no photos, so there are no licences to worry about.
`python3 make_themes.py out` draws them again. The `screenshot.jpg` in each folder is a mock-up of Home over
the wallpaper, not a screenshot from a TV.

| Theme | Look |
|---|---|
| `aegean-night/` | Aegean at night: the moon over the sea, Cycladic houses and a chapel with a blue dome. Glass dock, round buttons, focus glow in the app's colour |
| `meteora-dusk/` | Meteora at dusk: rock towers, a monastery and mist under a violet and orange sky. Cinematic top shelf with the app's artwork, glass dock |
| `olympus-mist/` | Olympus before dawn: mountain ridges with mist, snow on the far peaks. Calm: 70% dock, thin rim focus, names under every app |
| `neon-grid/` | Synthwave: a striped sunset, mountains with a cyan rim and a pink neon grid. Lively effects, glass dock |
| `oled-black/` | Pure black for OLED: no wallpaper or glows, one gear for Inputs/Wi-Fi/Settings, thin rim focus, calm motion. The lightest for weak boxes |
| `full-black/` | Black everywhere with no wallpaper or glows: cinematic shelf without artwork, white focus, calm motion, round buttons, App library and the CPU/memory line |
| `ellada/` | The three Greek scenes by time of day (Olympus morning, Meteora evening, Aegean night), with the place's name under the buttons |

**In the app:** Themes → Add themes… and pick `CleanYourTV-themes.zip` (or copy the folders into
`Documents\CleanYourTV\themes`). They show first in the gallery, marked *mine*. Pick one and press Apply.

**With the toolkit:** `TV_HOST=<tv ip> tools/theme.sh themes/aegean-night`.

No theme sets `dock_apps`, so the dock you chose stays as it is.

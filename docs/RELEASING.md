# Releasing TidyTV

TidyTV ships on Google Play (`com.appfactory.tidytv`) with fastlane, the same way as Essence and Flofocus. A signed APK also goes on GitHub releases for phones without Play, and the website deploys separately. Every store command goes through winkle-guard, and the owner approves it.

Lanes (see `fastlane/Fastfile`) run with `bundle exec fastlane android <lane>`:

- `deploy_internal`: builds the bundle (`./gradlew :app:bundleRelease`) and uploads it to the internal track.
- `deploy_production`: builds and uploads to production. Needs `CONFIRM=1`.
- `promote_to_production`: moves the internal build to production. Needs `CONFIRM=1`.
- `metadata_upload`: uploads the listing text from `fastlane/metadata/android/en-IN/`. Add `version_code:N` to include that version's changelog, or `validate_only:true` to only check it.
- `upload_screenshots`: replaces the store screenshots with `images/phoneScreenshots/*`.
- `release_testing track:alpha|beta`: copies the production build to closed (alpha) or open (beta) testing. Needs `CONFIRM=1`.
- `verify`: reads back the version codes on every track.

## A release

1. **Tests:** run `./gradlew test` and `./gradlew recordRoborazziDebug`, look at the changed screenshots, and walk the Demo TV on the emulator. When a real TV or phone is also connected over adb, set `ANDROID_SERIAL=emulator-5554` first: `installDebug` installs on every connected device.
2. **Version:** bump `versionCode` and `versionName` in `app/build.gradle.kts` and `VERSION` in `tools/build-pages.py`.
3. **Changelog:** write `fastlane/metadata/android/en-IN/changelogs/<versionCode>.txt`, 500 characters or fewer.
4. **Commit and push.**
5. **Play:**
   - Safer route: run `deploy_internal`, install from Play's internal track and check it, then `CONFIRM=1 bundle exec fastlane android promote_to_production`.
   - Direct route: `CONFIRM=1 bundle exec fastlane android deploy_production`.
   - Then run `metadata_upload` and `upload_screenshots` if the listing changed, and `verify`.
6. **GitHub release:** attach the signed APK twice: as `TidyTV-<version>.apk`, and as **`TidyTV.apk`**. The site's "Download APK" button and `/tidytv.apk` point to `releases/latest/download/TidyTV.apk`, so that file name must be on every release:
   ```sh
   ./gradlew :app:assembleRelease
   cp app/build/outputs/apk/release/app-release.apk dist/TidyTV-X.Y.Z.apk
   cp app/build/outputs/apk/release/app-release.apk /tmp/TidyTV.apk
   gh release create vX.Y.Z dist/TidyTV-X.Y.Z.apk /tmp/TidyTV.apk --title "TidyTV X.Y.Z: ..." --notes-file notes.md
   ```
7. **Site:** run `python3 tools/build-pages.py`, then `tools/deploy-site.sh`. The site carries no APK; it links to Play and to GitHub.

## Store screenshots

The store screenshots are made with the editor in `tools/playstore-shots/`:

1. Capture raw 1080×2400 screens from the emulator (Demo TV, demo-mode status bar), then put them in `tools/playstore-shots/public/screenshots/android/phone/en/01–07.png`.
2. Run the editor (`bun --cwd tools/playstore-shots dev`) and click **Export bundle**.
3. Copy the seven 1080×1920 PNGs to `fastlane/metadata/android/en-IN/images/phoneScreenshots/1–7_en-IN.png`.

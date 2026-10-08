# BurnMap 9 v0.3 — engineering validation

Validated on 2026-10-08. Debug-signed owner-testing build; clinical/anatomical acceptance remains separate.

| Gate | Result |
|---|---|
| Build and both APK assemblies | PASS |
| Calculation unit tests | 12 passed |
| HONOR DNY-NX9, Android 16/API 36 instrumentation | 26 passed |
| Lint | 0 errors, 15 non-blocking warnings |
| Welcome and native splash share launcher resource | PASS |
| Exact bottom credit, portrait and landscape | PASS |
| Tap-to-skip and calculator navigation/regressions | PASS |
| Built, tested, installed and delivered APK identity | Matching SHA-256 |

- APK: `BurnMap9-v0.3-debug.apk`
- Package: `com.drsherif.ruleofnines`
- Version: 0.3, versionCode 3
- SHA-256: `621068c975c1a1a1f8698badf4aeba9ee11f388478f5222fb45e9d183c3b1d9b`

The [v0.3-debug prerelease](https://github.com/sherifrad/burnmap9-android/releases/tag/v0.3-debug) targets validated source commit `eab2adc50aba0a02db068023572631c0b76e24f3`. Download the [APK](https://github.com/sherifrad/burnmap9-android/releases/download/v0.3-debug/BurnMap9-v0.3-debug.apk) and [checksum](https://github.com/sherifrad/burnmap9-android/releases/download/v0.3-debug/BurnMap9-v0.3-debug.apk.sha256) into the same directory, then verify:

```sh
shasum -a 256 -c BurnMap9-v0.3-debug.apk.sha256
```

The welcome screen reuses the actual manifest launcher XML. Android 12+ launch-theme configuration refers to that same icon, verified by resolving the live activity theme on Honor. The exact lower-edge text is **Made with ❤️ by Omar El-gazzar and Sol**. Portrait and landscape screenshots were visually inspected; automated checks confirm the credit lies within the lower safe area.

The welcome automatically advances after 1.2 seconds and supports immediate tap-to-skip. Saveable session state prevents replay when resuming or rotating the calculator. Existing calculator tests wait for this real entry flow, then run their original drawing, analysis, lifecycle and integrity assertions. No test checks were suppressed.

All seven previously AGY-reviewed clinical/graphics/concurrency implementation files remain unchanged. No dependency, patient-data, map, weighting or calculation changes were made.

Only the debug APK and its checksum are published as release assets. Raw device reports, serials, machine paths, screenshots and signing material remain local under ignored `outputs/`. No production or clinical release is asserted. See [README](../README.md) for the reproducible harness.

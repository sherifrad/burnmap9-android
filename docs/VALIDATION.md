# BurnMap 9 v0.2 — engineering validation

Validated on 2026-10-08. This is a debug-signed owner-testing build. Schematic anatomy and clinical suitability still require owner/clinician acceptance.

| Gate | Result |
|---|---|
| Kotlin/Compose build and both APK assemblies | PASS |
| Calculation unit tests | 12 passed |
| HONOR DNY-NX9, Android 16/API 36 instrumentation | 25 passed |
| Lint | 0 errors, 12 non-blocking warnings |
| Packaged adaptive icon and Android 13 monochrome layer | PASS |
| Application label and OEM launcher rendering | PASS |
| Delivered, tested and installed APK identity | Matching SHA-256 |

APK filename: `BurnMap9-v0.2-debug.apk`
Application ID: `com.drsherif.ruleofnines`
Version: 0.2, versionCode 2
SHA-256: `18b0a6836e35102994ce7e9cd65737ef200f419eab63c1d9d562c107c459dc5b`

The v0.2 change adds native icon resources and branding, and increments the version. The seven previously AGY-reviewed clinical, graphics and concurrency implementation files remain byte-for-byte unchanged. No dependencies were added.

The 25 phone tests cover real tap/drag drawing, independent front/back views, 50.5/49.5/100 totals, exact map/mask support, alpha thresholds, invalid inputs, overlapping/outside paint, immutable snapshots, cancellation, stale results, rotation, background/resume, clear confirmation, regional-list scrolling, landscape usability and canvas recomposition. The additional branding test inflates the actual compiled adaptive XML, verifies the monochrome layer and brand colors, and separately renders Honor's themed launcher drawable.

Raw XML reports, device identifiers, machine-specific paths, logs, screenshots and debug APKs are intentionally local under ignored `outputs/`. The public icon preview contains only generated branding artwork.

Build and phone-test commands are documented in the [README](../README.md). APK signing uses a local debug key; no signing material is included in the repository. No production or clinical release is asserted.

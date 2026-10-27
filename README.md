# CapCut Combined Experimental 0.2.0

**A compiled Morphe `.mpp` bundle, not just source.** Built and applied to the exact supplied APK. Android device behavior remains untested. This supersedes the earlier uncompiled candidate/build-kit ZIPs.

## Input and compatibility
- Package: `com.lemon.lvoverseas`
- Version: `19.9.0`, versionCode `19900002`
- Input SHA-256: `3c74a9941237451953fe4ed1abe477b7413d11d11a4b5b45a17441afd30f2c38`
- Compiled against Morphe Patcher **2.0.1**. Use a Manager/Desktop release compatible with this patcher API. Android Manager import was not exercised here.
- Patch-level guard rejects other package/version/versionCode combinations. Full input hash is additionally enforced by the desktop test script, not by the patch itself.

## Downloads and test evidence

- [Compiled experimental module](artifacts/capcut-combined-0.2.0-experimental.mpp)
- [Detailed test report](TEST-REPORT.md)
- [Test logs](logs/)
- Source is included in this repository under `src/`.

This repository contains experimental modifications, not an official CapCut or Morphe release.

## Import and select
1. Back up local drafts before changing installed builds. Re-signed APKs cannot normally update an official-signed installation; do not uninstall without protecting drafts.
2. Import `capcut-combined-0.2.0-experimental.mpp` as a local patch source using your Morphe version's source/file import UI. The release ZIP must be extracted first.
3. Choose the original APK matching the hash above.
4. Select ONE preset from THIS bundle. Avoid simultaneously selecting equivalent patches from a different source.
   - **CapCut Combined - core** (default): all ten functional/privacy/region patches, no native replacement, no slimming.
   - **CapCut Combined - experimental native fix** (opt-in): core plus the new main native interceptor passthrough. This is the candidate intended to address the remaining notice path. It may fail native registration and crash.
5. Leave both slimming patches off initially. Build and sign in Morphe, then test the delayed-notice scenario on your phone.

Presets use dependency composition, so shared patches execute once within this bundle. The separate UI-only candidate from the prior kit is intentionally NOT included: upstream already applies that renderer hook.

## Contents: 13 individual patches plus 2 presets
Twelve upstream CapCut patches are included, with attribution preserved:
- Block Telemetry & Trackers
- Bypass Effects Region Restriction
- Bypass GeoBlock Region Restriction
- Bypass Signature Check
- Bypass Unsafe Package Modal
- Redirect API to v2 Boot
- Remove Watermark
- Unlock Premium
- Unlock Premium Templates
- Unlock Ultra HD Export
- Locale Assets Slimmer (optional)
- Native Bloat Slimmer (optional; speech trimming stays false)

New individual patch: **Experimental main security interceptor passthrough**. Both presets also enforce the build guard. Individual patches are off by default; the core preset selects its dependencies automatically.

## What has actually passed
- Kotlin compilation, Android D8 compilation, and JVM bundle loading.
- Core preset application and APK assembly.
- Experimental native preset application and APK assembly.
- All 15 named entries selected together, with default option values: **15 successful, 0 failed; 0 fingerprint failures, 0 smali compile errors, 0 skipped steps**. This does NOT mean speech-removal=true was tested.
- Each output: 40 root DEX files with valid headers/checksums, 325,310 unique classes, no duplicate class definitions.
- Register-index checks across the modified DEX: 56,066 instructions for core; 56,071 for native/all. Not a full ART verifier.
- Expected interceptor native flag/body and security-dialog early return checked in output DEX.
- Three assembled test APKs signed and signature-verified for Android API 23, 28 and 36. This is cryptographic verification, NOT installation testing.
- Core/native outputs retain all 345 native libraries and all 60 locale JSON files byte-for-byte.
- Full optional-slimming run removes 59 locale JSON files and zeroes the two AppLovin crash-reporter libraries; speech engines remain unchanged.
- ZIP CRC and 16 KB alignment of nonempty uncompressed native entries checked.
- Negative test: versionCode changed to 19900003 was rejected with no output APK.

## Limits and risks
No phone/emulator launch, delayed-popup test, login, template download, export, battery or performance test was possible here. The native-registration risk remains unresolved. Suppressing a warning does not establish security or make a server accept the client. Paid/cloud permissions remain server-controlled. Broad telemetry and endpoint modifications may break online features.

The signed core/native test APKs were about 333.27 MB versus the 322.46 MB original. This is NOT a demonstrated storage or performance optimization. Optional slimming produced about 332.83 MB. Keep it off unless you accept the feature risks.

The deliverable is the module plus source and test evidence, not redistribution of CapCut's proprietary APK. You generate your own signed APK through Morphe or the included desktop script. Do not upload test signing keys to a public repository.

## Build without GitHub Packages credentials
Requires Linux, JDK 21, Python 3, git and network access to public Maven/JitPack/source repositories.

```sh
export JAVA_HOME=/path/to/jdk21
bash tools/build.sh
bash tools/test-apk.sh /path/to/original.apk core
bash tools/test-apk.sh /path/to/original.apk native
bash tools/test-apk.sh /path/to/original.apk all
```

`build.sh` builds the public Morphe Patcher 2.0.1 source into local Maven, then uses a standalone Kotlin/D8 build. It does not require the authenticated Morphe Gradle plugin. `test-apk.sh` generates signed test APKs under `.cache/test-results` with a local TEST key. Approximately 2 GB RAM was sufficient here after separating patching and signing processes. A clean rebuild may resolve artifacts from configured public repositories; byte-for-byte reproducibility is not claimed.

## Provenance and changes
- Community source: https://github.com/kveld9/kveld-extra-morphe-patches
- Pinned community commit: `f68c15ff6701c2ba425e7bc30d4e4eafc3814944`.
- Patcher source: https://github.com/MorpheApp/morphe-patcher
- Pinned patcher commit: `c3363f717344c74fb3f160f8c788022981d3110f` (2.0.1).
- Added two combined presets, package/version/versionCode guard, native replacement candidate and output audits.
- Corrected earlier native-candidate API usage: the actual patcher requires replacing the method via a new mutable method, not assigning an immutable implementation property.
- Moved guard into its own Kotlin file to eliminate a static-initialization dependency cycle found during testing.
- Changed upstream named individual defaults to false to make preset selection clear.
- Kept upstream functional hook bodies; no claim those premium/region features are newly invented here.
- Adapted upstream test runner for separate-process signing and corrected BKS/PKCS12 handling in its fallback.
- Source files, LICENSE and NOTICE included; upstream GPL terms apply.

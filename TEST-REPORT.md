# Test report — CapCut Combined 0.2.0-experimental

## Deliverable
A compiled `.mpp` containing Android DEX and JVM classes. The same bundle was used for all positive application tests. It loads through Morphe Patcher 2.0.1. It is **not a runtime-verified security-notice fix**.

## Test input
CapCut 19.9.0 / 19900002, com.lemon.lvoverseas.
SHA-256: `3c74a9941237451953fe4ed1abe477b7413d11d11a4b5b45a17441afd30f2c38`.
The fresh download matched the previously inspected APK exactly.

## Results
| Check | Core preset | Core + native candidate | All named entries/default options |
|---|---|---|---|
| Apply patches and dependencies | PASS | PASS | PASS: 15/15 |
| Fingerprint failures | 0 | 0 | 0 |
| Reported smali compile errors | 0 | 0 | 0 |
| Skipped steps | 0 | 0 | 0 |
| Assemble APK | PASS | PASS | PASS |
| Sign APK in separate process | PASS | PASS | PASS |
| Verify APK signature for API 23/28/36 | PASS | PASS | PASS |
| DEX header/SHA1/Adler32 | 40/40 | 40/40 | 40/40 |
| Unique classes | 325,310 | 325,310 | 325,310 |
| Duplicate classes | 0 | 0 | 0 |
| Modified-DEX instructions register-checked | 56,066 | 56,071 | 56,071 |
| Main interceptor native flag | Retained | Cleared; exact passthrough body checked | Cleared; exact passthrough body checked |
| Dialog renderer early return | Confirmed | Confirmed | Confirmed |
| Native libraries preserved unchanged | 345/345 | 345/345 | 343/345; two AppLovin libraries intentionally zeroed |
| Locale JSON files retained | 60/60 | 60/60 | 1/60; 59 intentionally removed |
| Speech engines | Preserved | Preserved | Preserved (trimSpeechEngines=false) |
| ZIP CRC | PASS | PASS | PASS |
| Nonempty uncompressed native entries 16 KB aligned | PASS | PASS | PASS |
| Phone launch / delayed notice / editing / export | NOT TESTED | NOT TESTED | NOT TESTED |

The core preset's application log counts one selected top-level preset; its ten functional patches execute as dependencies. The full-selection log counts 15 named entries, including the two presets; this is not a claim of 15 independent new features.

## Negative compatibility test
A copy of the input was changed only to versionCode 19900003 (its signature was consequently no longer valid). For this deliberate fixture, desktop digest enforcement was disabled so the patch's own versionCode guard could be exercised. The guard rejected it, the test process exited with failure, and no output APK was produced. The original input was not changed.

## Defects found and corrected during this session
- Replaced authenticated Gradle-plugin dependency with a standalone Kotlin/D8 build and a public-source local build of Morphe Patcher 2.0.1.
- Corrected native-method mutation against the real patcher API; previous source-only candidate did not compile as written.
- Removed a Kotlin static-initialization cycle caused by putting the shared guard and aggregate presets in one file.
- Adapted D8 CLI input to a `.jar` temporary filename while retaining `.mpp` output.
- Corrected test-runner BKS/PKCS12 mismatch.
- Split signing into a separate process after combined patching/signing exceeded sandbox memory.

## Important interpretation
These tests demonstrate compilation, successful matching/application, assembly and limited structural/cryptographic validity. They do not prove that Android ART accepts every path, that native registration succeeds, that CapCut's server accepts requests, that premium/cloud functions work, or that the delayed warning disappears.

Core/native signed test output size was 333,272,044 bytes; original size was 322,464,224 bytes. Optional slimming output was 332,829,204 bytes. No performance or size optimization is claimed. Signatures use a local experimental key, not CapCut's official signing identity. Generated proprietary test APKs and private keys are not included in this module distribution.

The separate UI-only candidate was not merged because its hook already exists in upstream Bypass Signature Check. Destructive slimming and the native candidate remain opt-in. This release supersedes earlier source-only kits.

## Evidence
`logs/compile.log`, the three `*-application.log` files, `*-signature.log`, `*-dex-audit.log`, `*-archive-audit.json`, and `wrong-version-negative.log` contain the relevant output. This repository contains source, license, build scripts and audit tools.

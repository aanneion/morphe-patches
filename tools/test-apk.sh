#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 21}"
export PATH="$JAVA_HOME/bin:$PATH"
APK=$(realpath "${1:?Usage: test-apk.sh original.apk [core|native|all]}")
PROFILE=${2:-core}
EXPECTED=3c74a9941237451953fe4ed1abe477b7413d11d11a4b5b45a17441afd30f2c38
ACTUAL=$(sha256sum "$APK" | cut -d' ' -f1)
[ "$EXPECTED" = "$ACTUAL" ] || { echo 'Rejected: wrong input SHA-256'; exit 2; }
[ -f build/runtime-classpath.txt ] || { echo 'Run tools/build.sh first'; exit 2; }
CP=$(cat build/runtime-classpath.txt)
mkdir -p .cache/test-results
TESTDIR="$ROOT/.cache/test-results"
EXTRA=()
case "$PROFILE" in
  core) EXTRA+=('-DpatchName=CapCut Combined - core');;
  native) EXTRA+=('-DpatchName=CapCut Combined - experimental native fix');;
  all) ;;
  *) echo 'Profile must be core, native, or all'; exit 2;;
esac
java -Xmx850m -DtargetApp=capcut "-DtargetApk=$APK" "${EXTRA[@]}" -DapkDigestEnforce=true -DunsignedOnly=true "-DoutputApk=$TESTDIR/$PROFILE-unsigned.apk" -cp "$CP" util.PatchExecutionTestKt 2>&1 | tee "$TESTDIR/$PROFILE-application.log"
# Separate process avoids retaining the patcher's DEX data while signing a large APK.
java -Xmx600m -cp "$CP" util.SignAndVerifyKt "$TESTDIR/$PROFILE-unsigned.apk" "$TESTDIR/$PROFILE-signed.apk" "$TESTDIR/experimental-test.bks" 2>&1 | tee "$TESTDIR/$PROFILE-signature.log"
mkdir -p build/audit
javac -cp "$CP" -d build/audit tools/DexAudit.java
EXPECT_NATIVE=false
[ "$PROFILE" = core ] && EXPECT_NATIVE=true
java -Xmx512m -cp "build/audit:$CP" DexAudit "$TESTDIR/$PROFILE-signed.apk" "$EXPECT_NATIVE" | tee "$TESTDIR/$PROFILE-dex-audit.log"
SLIM=preserve
[ "$PROFILE" = all ] && SLIM=slim
python3 tools/archive_audit.py "$APK" "$TESTDIR/$PROFILE-signed.apk" "$SLIM" | tee "$TESTDIR/$PROFILE-archive-audit.json"
echo 'Structural/signature tests passed. Phone runtime behavior remains UNTESTED.'
echo 'The generated signing key is a test key, not suitable for a public production release.'

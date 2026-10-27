#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
: "${JAVA_HOME:?Set JAVA_HOME to a full JDK 21 installation}"
export PATH="$JAVA_HOME/bin:$PATH"
PATCHER_COMMIT=c3363f717344c74fb3f160f8c788022981d3110f
mkdir -p .cache
if [ ! -d .cache/morphe-patcher/.git ]; then
  git clone https://github.com/MorpheApp/morphe-patcher.git .cache/morphe-patcher
fi
git -C .cache/morphe-patcher checkout --detach "$PATCHER_COMMIT"
# Disable signing of LOCAL Maven publication only. APK signatures are unrelated.
python3 - <<'PY'
from pathlib import Path
p=Path('.cache/morphe-patcher/build.gradle.kts')
s=p.read_text()
marker='\nsigning {'
if marker in s: p.write_text(s[:s.rfind(marker)])
PY
(cd .cache/morphe-patcher && bash gradlew publishToMavenLocal --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx512m -Pkotlin.compiler.execution.strategy=in-process)
bash gradlew clean buildAndroid writeClasspath --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx384m -Pkotlin.compiler.execution.strategy=in-process
mkdir -p artifacts
cp build/libs/*.mpp artifacts/
(cd artifacts && sha256sum *.mpp > SHA256SUMS)
printf '\nBundle saved under %s/artifacts/\n' "$ROOT"

#!/usr/bin/env bash
# Short wrapper: "./psc execution file.ps" instead of the full gradlew line.
# Runs the installed distribution, so there is no Gradle overhead and the exit
# code is the program's own rather than a BUILD FAILED wrapper.
# Rebuild after changing compiler code: ./gradlew :app:installDist
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
dist="$root/app/build/install/printscript/bin/printscript"

if [ ! -x "$dist" ]; then
  echo "Distribution not built. Run: ./gradlew :app:installDist" >&2
  exit 3
fi

export JAVA_HOME="${PRINTSCRIPT_JDK:-C:\\Users\\agusr\\.jdks\\corretto-25.0.2}"
exec "$dist" "$@"

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

if [ -n "${PRINTSCRIPT_JDK:-}" ]; then
  export JAVA_HOME="$PRINTSCRIPT_JDK"
elif [ -z "${JAVA_HOME:-}" ] && ! command -v java >/dev/null 2>&1; then
  echo "No Java found. Set JAVA_HOME or PRINTSCRIPT_JDK to a JDK 21 or newer." >&2
  exit 3
fi

exec "$dist" "$@"

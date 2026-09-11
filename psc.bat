@echo off
rem Short wrapper: "ps execution file.ps" instead of the full gradlew line.
rem Runs the installed distribution, so there is no Gradle overhead and the
rem exit code is the program's own rather than a BUILD FAILED wrapper.
rem Rebuild after changing compiler code: gradlew :app:installDist

set "DIST=%~dp0app\build\install\printscript\bin\printscript.bat"

if not exist "%DIST%" (
  echo Distribution not built. Run: gradlew :app:installDist
  exit /b 3
)

if defined PRINTSCRIPT_JDK set "JAVA_HOME=%PRINTSCRIPT_JDK%"
if defined JAVA_HOME goto :run

where java >nul 2>&1
if errorlevel 1 (
  echo No Java found. Set JAVA_HOME or PRINTSCRIPT_JDK to a JDK 21 or newer.
  exit /b 3
)

:run
call "%DIST%" %*

@echo off
rem Short wrapper: "ps execution file.ps" instead of the full gradlew line.
rem Runs the installed distribution, so there is no Gradle overhead and the
rem exit code is the program's own rather than a BUILD FAILED wrapper.
rem Rebuild after changing compiler code: gradlew :app:installDist

if not defined PRINTSCRIPT_JDK set "PRINTSCRIPT_JDK=C:\Users\agusr\.jdks\corretto-25.0.2"

set "DIST=%~dp0app\build\install\printscript\bin\printscript.bat"

if not exist "%DIST%" (
  echo Distribution not built. Run: gradlew :app:installDist
  exit /b 3
)

set "JAVA_HOME=%PRINTSCRIPT_JDK%"
call "%DIST%" %*

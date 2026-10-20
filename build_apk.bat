@echo off
setlocal enabledelayedexpansion
echo ========================================================
echo   Building Saathi Debug APK for Mobile Testing
echo ========================================================

:: Detect JDK 17 / JDK 21 / Android Studio JBR
set "JAVA_HOME="
if exist "C:\Program Files\Microsoft\jdk-17*" (
    for /d %%D in ("C:\Program Files\Microsoft\jdk-17*") do set "JAVA_HOME=%%D"
)
if "%JAVA_HOME%"=="" if exist "C:\Program Files\Eclipse Adoptium\jdk-17*" (
    for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do set "JAVA_HOME=%%D"
)
if "%JAVA_HOME%"=="" if exist "C:\Program Files\Android\Android Studio\jbr" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
)

if "%JAVA_HOME%"=="" (
    echo Error: No suitable Java runtime found.
    pause
    exit /b 1
)

echo Using JAVA_HOME: %JAVA_HOME%
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0"
call gradlew.bat assembleDebug

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo ========================================================
    echo  SUCCESS! APK generated at:
    echo  app\build\outputs\apk\debug\app-debug.apk
    echo ========================================================
) else (
    echo.
    echo Build failed or incomplete. Check errors above.
)
pause

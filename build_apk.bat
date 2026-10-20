@echo off
echo ========================================================
echo   Building Saathi Debug APK for Mobile Testing
echo ========================================================

set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

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

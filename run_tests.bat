@echo off
echo ========================================================
echo   Running Saathi Unit Tests (Duplicate Pills, Scam Shield)
echo ========================================================

set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

cd /d "%~dp0"
call gradlew.bat testDebugUnitTest --continue

echo ========================================================
echo   Test Execution Finished!
echo ========================================================
pause

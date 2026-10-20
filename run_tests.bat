@echo off
setlocal enabledelayedexpansion
echo ========================================================
echo   Running Saathi Unit Tests (Duplicate Pills, Scam Shield)
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
call gradlew.bat testDebugUnitTest --continue

echo ========================================================
echo   Test Execution Finished!
echo ========================================================
pause

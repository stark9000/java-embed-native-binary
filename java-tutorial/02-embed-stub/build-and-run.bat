@echo off
setlocal
cd /d "%~dp0"

echo ================================================================
echo  Stage 2: embed ..\01-build-exe\target.exe into a Java class
echo ================================================================

if not exist "..\01-build-exe\target.exe" (
    echo ERROR: ..\01-build-exe\target.exe not found.
    echo Run ..\01-build-exe\make-target.bat first.
    goto :end
)

echo.
echo [1/2] Compiling GenerateEmbedded.java ...
javac GenerateEmbedded.java
if errorlevel 1 goto :end

echo.
echo [2/2] Running GenerateEmbedded against target.exe ...
java GenerateEmbedded ..\01-build-exe\target.exe EmbeddedStub.java
if errorlevel 1 goto :end

echo.
echo Wrote EmbeddedStub.java -- target.exe's bytes, base64-encoded as
echo Java string constants. Next: go to ..\03-patch-exe and run
echo build-and-run.bat

:end
pause

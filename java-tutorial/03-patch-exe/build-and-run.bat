@echo off
setlocal
cd /d "%~dp0"

echo ================================================================
echo  Stage 3: decode the embedded stub AND patch a custom icon into
echo           it, in one step
echo ================================================================

if not exist "..\02-embed-stub\EmbeddedStub.java" (
    echo ERROR: ..\02-embed-stub\EmbeddedStub.java not found.
    echo Run ..\02-embed-stub\build-and-run.bat first.
    goto :end
)
if not exist "jna-5.5.0.jar" (
    echo ERROR: jna-5.5.0.jar not found next to this .bat.
    echo Download it from https://github.com/java-native-access/jna/releases
    echo or Maven Central ^(net.java.dev.jna:jna:5.5.0^).
    goto :end
)
if not exist "icon.ico" (
    echo ERROR: icon.ico not found. Put a real multi-frame .ico here
    echo -- the custom icon you want patched in.
    goto :end
)

echo.
echo [1/3] Copying EmbeddedStub.java from Stage 2 ...
copy /y "..\02-embed-stub\EmbeddedStub.java" . >nul

echo.
echo [2/3] Compiling EmbeddedStub.java, IconResourceMini.java, BuildAndPatch.java ...
javac -cp jna-5.5.0.jar EmbeddedStub.java IconResourceMini.java BuildAndPatch.java
if errorlevel 1 goto :end

echo.
echo [3/3] Running BuildAndPatch (decode stub -^> patched.exe -^> patch icon) ...
java -cp ".;jna-5.5.0.jar" BuildAndPatch icon.ico patched.exe
if errorlevel 1 goto :end

echo.
echo Done. patched.exe is target.exe's exact bytes, round-tripped through
echo Java as base64, with your custom icon patched straight in.
echo Open patched.exe in Explorer to see the new icon.

:end
pause

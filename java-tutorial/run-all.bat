@echo off
setlocal EnableExtensions
cd /d "%~dp0"

rem ============================================================================
rem Runs all 3 stages back-to-back:
rem   1. gcc:  target.c + target.rc        -> target.exe
rem   2. Java: embed target.exe as base64  -> EmbeddedStub.java
rem   3. Java: decode the stub AND patch a custom icon into it -> patched.exe
rem ============================================================================

set "MSYS2_ROOT="
if exist "E:\msys64\mingw64\bin\gcc.exe" set "MSYS2_ROOT=E:\msys64"
if exist "C:\msys64\mingw64\bin\gcc.exe" set "MSYS2_ROOT=C:\msys64"
if not defined MSYS2_ROOT (
    echo ERROR: MSYS2 not found at C:\msys64 or E:\msys64.
    goto :end
)
set "BIN64=%MSYS2_ROOT%\mingw64\bin"
set "GCC=%BIN64%\x86_64-w64-mingw32-gcc.exe"
set "WINDRES=%BIN64%\x86_64-w64-mingw32-windres.exe"
if not exist "%GCC%" set "GCC=%BIN64%\gcc.exe"
if not exist "%WINDRES%" set "WINDRES=%BIN64%\windres.exe"
set "PATH=%BIN64%;%PATH%"

if not exist "03-patch-exe\jna-5.5.0.jar" (
    echo ERROR: 03-patch-exe\jna-5.5.0.jar not found.
    echo Download it from https://github.com/java-native-access/jna/releases
    echo or Maven Central ^(net.java.dev.jna:jna:5.5.0^).
    goto :end
)
if not exist "03-patch-exe\icon.ico" (
    echo ERROR: 03-patch-exe\icon.ico not found.
    echo Put a real multi-frame .ico there -- the custom icon to patch in.
    goto :end
)

echo ================================================================
echo  Stage 1: gcc builds target.exe with the bundled default icon
echo ================================================================
pushd 01-build-exe
"%WINDRES%" target.rc -O coff -o target_res.o || goto :fail_pop
"%GCC%" -O2 -Wall -mwindows target.c target_res.o -o target.exe || goto :fail_pop
popd
echo [OK] 01-build-exe\target.exe built

echo.
echo ================================================================
echo  Stage 2: embed target.exe into a Java class
echo ================================================================
pushd 02-embed-stub
javac GenerateEmbedded.java || goto :fail_pop
java GenerateEmbedded ..\01-build-exe\target.exe EmbeddedStub.java || goto :fail_pop
popd
echo [OK] 02-embed-stub\EmbeddedStub.java written

echo.
echo ================================================================
echo  Stage 3: decode the stub AND patch a custom icon into it
echo ================================================================
copy /y 02-embed-stub\EmbeddedStub.java 03-patch-exe\ >nul
pushd 03-patch-exe
javac -cp jna-5.5.0.jar EmbeddedStub.java IconResourceMini.java BuildAndPatch.java || goto :fail_pop
java -cp ".;jna-5.5.0.jar" BuildAndPatch icon.ico patched.exe || goto :fail_pop
popd
echo [OK] 03-patch-exe\patched.exe built

echo.
echo ================================================================
echo  DONE. Run 03-patch-exe\patched.exe -- same working exe as
echo  Stage 1's target.exe, with your custom icon patched in.
echo ================================================================
goto :end

:fail_pop
popd
echo.
echo Pipeline failed - see output above.

:end
pause

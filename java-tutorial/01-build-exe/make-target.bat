@echo off
setlocal EnableExtensions
cd /d "%~dp0"

rem ============================================================================
rem Stage 1: compile target.exe (target.c + target.rc) with MinGW-w64 gcc --
rem the same MinGW-w64 toolchain a real cross-compiled native launcher stub
rem would use.
rem target.rc bakes in default.ico (a plain blue icon, bundled here) at
rem resource ID 1 -- that ID matters later, in Stage 3.
rem ============================================================================

set "MSYS2_ROOT="
if exist "E:\msys64\mingw64\bin\gcc.exe" set "MSYS2_ROOT=E:\msys64"
if exist "C:\msys64\mingw64\bin\gcc.exe" set "MSYS2_ROOT=C:\msys64"
if not defined MSYS2_ROOT (
    echo ERROR: MSYS2 not found at C:\msys64 or E:\msys64.
    echo Edit MSYS2_ROOT in this script if it's installed somewhere else.
    goto :end
)

set "BIN64=%MSYS2_ROOT%\mingw64\bin"
set "GCC=%BIN64%\x86_64-w64-mingw32-gcc.exe"
set "WINDRES=%BIN64%\x86_64-w64-mingw32-windres.exe"
if not exist "%GCC%" set "GCC=%BIN64%\gcc.exe"
if not exist "%WINDRES%" set "WINDRES=%BIN64%\windres.exe"
set "PATH=%BIN64%;%PATH%"

echo [1/2] Compiling target.rc ...
"%WINDRES%" target.rc -O coff -o target_res.o
if errorlevel 1 goto :end

echo [2/2] Compiling target.c + linking target.exe ...
"%GCC%" -O2 -Wall -mwindows target.c target_res.o -o target.exe
if errorlevel 1 goto :end

echo.
echo Built target.exe with the blue default icon at resource ID 1.
echo Next: go to ..\02-embed-stub and run build-and-run.bat

:end
pause

# Reliably Bundling a Native Binary into a Java App

A NetBeans / Ant build-tooling tutorial, with a Windows resource-patching bonus.

Embed a native Windows `.exe` directly into your compiled Java bytecode as
base64 `String` constants — so it's part of the `.class` file's constant
pool and can never silently go missing from a shipped build the way an
Ant-copied resource can. Then, as a bonus, patch that same exe's icon
afterward, entirely from Java, with no recompilation.

Full write-up: **[TUTORIAL.md](TUTORIAL.md)**

## What's here

```
01-build-exe/     gcc/MinGW-w64 builds a real target.exe with a default icon
02-embed-stub/    Java embeds target.exe's bytes as base64 in EmbeddedStub.java
03-patch-exe/     Java decodes the stub, then patches in a custom icon
run-all.bat       runs all three steps back-to-back
TUTORIAL.md       the full step-by-step explanation
```

## Requirements

- **Windows** (steps 1 and 3 use Windows-specific tooling; step 2 alone is
  cross-platform)
- **JDK 8+** (`javac`/`java` on your `PATH`)
- **gcc — specifically MinGW-w64 gcc via MSYS2, not just any gcc.**
  Step 1 needs `x86_64-w64-mingw32-gcc.exe` and `windres.exe` from an
  [MSYS2](https://www.msys2.org/) install. Install MSYS2, then from an
  MSYS2 shell run:
  ```
  pacman -S mingw-w64-x86_64-gcc
  ```
  The build scripts look for MSYS2 at `C:\msys64` or `E:\msys64` by
  default. If yours is installed elsewhere, open `01-build-exe/make-target.bat`
  (and `run-all.bat`) and edit the `MSYS2_ROOT` variable near the top.
  A plain MSYS/MinGW install, WSL's gcc, or MinGW from a different
  distribution will **not** be found automatically — the script checks
  those two exact paths only.
- **[jna-5.5.0.jar](https://github.com/java-native-access/jna/releases)**
  (or `net.java.dev.jna:jna:5.5.0` from Maven Central) — needed for Step 3,
  place it in `03-patch-exe/`
- A real multi-frame `icon.ico` of your own to swap in for Step 3
  (a placeholder is not included — bring your own)

## Quick start

```
git clone <this-repo>
cd java-tutorial
:: put jna-5.5.0.jar and your icon.ico in 03-patch-exe/ first
run-all.bat
```

Or run each stage by hand — see `TUTORIAL.md` for the full breakdown of
what each step does and why.

## Why this exists

Ant's file-copy targets are a second dependency graph tacked onto the
compiler's, and it's easy for the two to drift out of sync — a binary
resource that isn't explicitly declared in `build.xml` just silently
doesn't make it into the jar, no error, no warning. Baking the binary into
the constant pool of a `.java` file sidesteps that entirely: if the class
compiled, the bytes are in the jar, because they *are* the bytecode.

See `TUTORIAL.md` for the full explanation, including the JVM constant-pool
size limits that dictate how the base64 gets chunked, and the PE
resource-section mechanics behind the icon-patching bonus.

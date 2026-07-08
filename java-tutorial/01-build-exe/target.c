#include <windows.h>

/*
 * Minimal target.exe for testing PatchIcon.java against.
 *
 * Deliberately trivial: it just pops a message box so you can visually
 * confirm it's still a working exe after being patched. Compiled with an
 * icon at resource ID 1 (see target.rc) so it matches the ID PatchIcon.java
 * assumes -- meaning patching should cleanly REPLACE this icon rather than
 * add a second unused icon group next to it.
 */
int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance,
                    LPSTR lpCmdLine, int nCmdShow) {
    MessageBoxA(NULL, "target.exe is still working after being patched.",
                "target.exe", MB_OK | MB_ICONINFORMATION);
    return 0;
}

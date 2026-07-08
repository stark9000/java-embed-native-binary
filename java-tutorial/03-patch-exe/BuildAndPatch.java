import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.io.*;
import java.nio.file.*;

/**
 * Stage 3: "patch the original exe with the java stub" -- as a single step.
 *
 * 1. Decode EmbeddedStub.bytes() (the base64'd target.exe from Stage 2)
 *    straight to the output path. This IS target.exe's bytes -- nothing
 *    rebuilt, nothing recompiled, just base64-decoded back to identical bytes.
 * 2. Immediately patch a custom icon into THAT SAME file via
 *    BeginUpdateResource/UpdateResource/EndUpdateResource.
 *
 * There's no separate "decoded but unpatched" file left lying around --
 * from the caller's point of view this is one action: decode the embedded
 * binary and immediately patch it, the way a real "build" step in a
 * native-launcher-wrapping tool would.
 *
 * Requires jna-<version>.jar on the classpath.
 * Usage: java -cp .;jna.jar BuildAndPatch <iconFile> <outputExe>
 */
public class BuildAndPatch {

    interface Kernel32Ext extends StdCallLibrary {
        Kernel32Ext INSTANCE = Native.load("kernel32", Kernel32Ext.class, W32APIOptions.UNICODE_OPTIONS);

        Pointer BeginUpdateResourceW(WString fileName, boolean deleteExistingResources);

        boolean UpdateResourceW(Pointer hUpdate, Pointer type, Pointer name, short language,
                Pointer data, int dataSize);

        boolean EndUpdateResourceW(Pointer hUpdate, boolean discard);
    }

    private static final short LANG_NEUTRAL = 0;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java -cp .;jna.jar BuildAndPatch <iconFile> <outputExe>");
            System.exit(1);
        }
        File iconFile = new File(args[0]);
        File outputExe = new File(args[1]);

        // --- step 1: decode the embedded stub straight to the output path ---
        byte[] stubBytes = EmbeddedStub.bytes();
        Files.write(outputExe.toPath(), stubBytes);
        System.out.println("[1/2] Decoded embedded stub -> " + outputExe.getAbsolutePath()
                + " (" + stubBytes.length + " bytes)");

        // --- step 2: patch the custom icon into that same file ---
        IconResourceMini icon = IconResourceMini.parse(iconFile);

        Pointer hUpdate = Kernel32Ext.INSTANCE.BeginUpdateResourceW(
                new WString(outputExe.getAbsolutePath()), false);
        if (hUpdate == null) {
            throw new IOException("BeginUpdateResource failed for " + outputExe);
        }

        boolean ok = true;
        for (IconResourceMini.Frame frame : icon.frames) {
            ok &= writeResource(hUpdate, IconResourceMini.RT_ICON, frame.id, frame.data);
        }
        // ID 1 matches what target.rc assigned the default icon in Stage 1 --
        // so this replaces it cleanly instead of adding a second icon group.
        ok &= writeResource(hUpdate, IconResourceMini.RT_GROUP_ICON, 1, icon.groupDirectory);

        if (!ok) {
            Kernel32Ext.INSTANCE.EndUpdateResourceW(hUpdate, true); // discard partial write
            throw new IOException("UpdateResource failed while writing icon resources");
        }
        if (!Kernel32Ext.INSTANCE.EndUpdateResourceW(hUpdate, false)) {
            throw new IOException("EndUpdateResource (commit) failed");
        }

        System.out.println("[2/2] Patched icon into " + outputExe.getAbsolutePath()
                + " (" + icon.frames.size() + " frame(s))");
    }

    private static boolean writeResource(Pointer hUpdate, int resourceType, int resourceId, byte[] data) {
        Memory mem = new Memory(data.length);
        mem.write(0, data, 0, data.length);
        return Kernel32Ext.INSTANCE.UpdateResourceW(
                hUpdate,
                new Pointer(resourceType),
                new Pointer(resourceId),
                LANG_NEUTRAL,
                mem,
                data.length);
    }
}

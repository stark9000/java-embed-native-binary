import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/**
 * Parses a standalone .ico into the shape a compiled exe's resource
 * section needs (RT_ICON per frame + one RT_GROUP_ICON directory).
 *
 * A standalone .ico file is one file containing an ICONDIR header followed
 * by one ICONDIRENTRY per frame (each entry points at that frame's image
 * bytes by FILE OFFSET, since it's all one file).
 *
 * Inside a compiled .exe, there's no such thing as a "file offset into the
 * icon" -- each frame becomes its own separate RT_ICON resource, and a
 * single RT_GROUP_ICON resource lists those frames by RESOURCE ID instead.
 * So this class re-packs:
 *
 *   ICONDIR + ICONDIRENTRY[]   (file format, offsets)
 *        -->
 *   GRPICONDIR + GRPICONDIRENTRY[]   (PE resource format, IDs)
 *        + N separate raw frame byte[] blobs (the RT_ICON payloads)
 */
final class IconResourceMini {

    static final int RT_ICON = 3;
    static final int RT_GROUP_ICON = 14;

    static final class Frame {
        final int id;          // the resource ID we're assigning this frame (1, 2, 3, ...)
        final byte[] data;      // raw image bytes (PNG or DIB), unchanged from the .ico
        final int width, height, colorCount, planes, bitCount;

        Frame(int id, byte[] data, int width, int height, int colorCount, int planes, int bitCount) {
            this.id = id;
            this.data = data;
            this.width = width;
            this.height = height;
            this.colorCount = colorCount;
            this.planes = planes;
            this.bitCount = bitCount;
        }
    }

    final List<Frame> frames;
    final byte[] groupDirectory; // the bytes to write as the RT_GROUP_ICON resource

    private IconResourceMini(List<Frame> frames, byte[] groupDirectory) {
        this.frames = frames;
        this.groupDirectory = groupDirectory;
    }

    static IconResourceMini parse(File icoFile) throws IOException {
        byte[] bytes = Files.readAllBytes(icoFile.toPath());
        if (bytes.length < 6) {
            throw new IOException("Not a valid .ico file (too short)");
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        int reserved = buf.getShort(0) & 0xFFFF;
        int type = buf.getShort(2) & 0xFFFF;
        int count = buf.getShort(4) & 0xFFFF;
        if (reserved != 0 || type != 1 || count == 0) {
            throw new IOException("Not a valid .ico file (bad ICONDIR header)");
        }

        List<Frame> frames = new ArrayList<>();
        ByteArrayOutputStream group = new ByteArrayOutputStream();
        writeU16(group, 0);      // reserved
        writeU16(group, 1);      // type = icon
        writeU16(group, count);

        for (int i = 0; i < count; i++) {
            int base = 6 + i * 16;
            int width = buf.get(base) & 0xFF;
            int height = buf.get(base + 1) & 0xFF;
            int colorCount = buf.get(base + 2) & 0xFF;
            int planes = buf.getShort(base + 4) & 0xFFFF;
            int bitCount = buf.getShort(base + 6) & 0xFFFF;
            int bytesInRes = buf.getInt(base + 8);
            int imageOffset = buf.getInt(base + 12);

            byte[] frameData = new byte[bytesInRes];
            System.arraycopy(bytes, imageOffset, frameData, 0, bytesInRes);

            int id = i + 1; // 1-based resource ID for this frame's RT_ICON entry
            frames.add(new Frame(id, frameData, width, height, colorCount, planes, bitCount));

            group.write(width);
            group.write(height);
            group.write(colorCount);
            group.write(0); // reserved
            writeU16(group, planes);
            writeU16(group, bitCount);
            writeU32(group, bytesInRes);
            writeU16(group, id); // <-- this is the field that replaces "file offset"
        }

        return new IconResourceMini(frames, group.toByteArray());
    }

    private static void writeU16(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
    }

    private static void writeU32(ByteArrayOutputStream out, long v) {
        out.write((int) (v & 0xFF));
        out.write((int) ((v >> 8) & 0xFF));
        out.write((int) ((v >> 16) & 0xFF));
        out.write((int) ((v >> 24) & 0xFF));
    }
}

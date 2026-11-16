package br.pucrs.verval.orangehrm.e2e.support;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Gera PNGs válidos com tamanho exato em bytes, para os uploads dos testes (sem arquivos no disco).
 * Mesmo algoritmo de integracao/.../support/Images.java.
 */
public final class Images {

    public static final int ONE_MB = 1_048_576;

    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final int CHUNK_OVERHEAD = 12;

    private Images() {
    }

    public static byte[] pngOfSize(int totalBytes) {
        byte[] ihdr = ByteBuffer.allocate(13).putInt(1).putInt(1)
                .put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0).array();
        byte[] idat = deflate(new byte[] {0, (byte) 0xFF, 0, 0});
        int fixed = SIGNATURE.length + (CHUNK_OVERHEAD + ihdr.length) + (CHUNK_OVERHEAD + idat.length) + CHUNK_OVERHEAD;
        int textLength = totalBytes - fixed - CHUNK_OVERHEAD;
        byte[] text = new byte[textLength];
        byte[] keyword = "Comment".getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(keyword, 0, text, 0, keyword.length);
        Arrays.fill(text, keyword.length + 1, textLength, (byte) 'x');

        ByteArrayOutputStream out = new ByteArrayOutputStream(totalBytes);
        out.writeBytes(SIGNATURE);
        writeChunk(out, "IHDR", ihdr);
        writeChunk(out, "tEXt", text);
        writeChunk(out, "IDAT", idat);
        writeChunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void writeChunk(ByteArrayOutputStream out, String type, byte[] data) {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
        out.writeBytes(typeBytes);
        out.writeBytes(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

    private static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater();
        deflater.setInput(raw);
        deflater.finish();
        byte[] buffer = new byte[64];
        int length = deflater.deflate(buffer);
        deflater.end();
        return Arrays.copyOf(buffer, length);
    }
}

package br.pucrs.verval.orangehrm.api.support;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Gera imagens PNG válidas com tamanho EXATO em bytes (para análise de valor-limite do limite de 1 MB)
 * e monta o anexo no formato aceito pela API (regra Base64Attachment: name, type, size, base64).
 */
public final class Images {

    /** Limite do OrangeHRM: ConfigService::MAX_ATTACHMENT_SIZE. */
    public static final int ONE_MB = 1_048_576;

    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final int CHUNK_OVERHEAD = 12; // tamanho (4) + tipo (4) + CRC (4)

    private Images() {
    }

    /** PNG 1x1 válido, preenchido com um bloco tEXt até ter exatamente {@code totalBytes}. */
    public static byte[] pngOfSize(int totalBytes) {
        byte[] ihdr = ByteBuffer.allocate(13).putInt(1).putInt(1)
                .put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0).array();
        byte[] idat = deflate(new byte[] {0, (byte) 0xFF, 0, 0});

        int fixed = SIGNATURE.length + (CHUNK_OVERHEAD + ihdr.length) + (CHUNK_OVERHEAD + idat.length) + CHUNK_OVERHEAD;
        int textLength = totalBytes - fixed - CHUNK_OVERHEAD;
        if (textLength < 2) {
            throw new IllegalArgumentException("tamanho mínimo: " + (fixed + CHUNK_OVERHEAD + 2) + " bytes");
        }
        byte[] text = new byte[textLength];
        byte[] keyword = "Comment".getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(keyword, 0, text, 0, Math.min(keyword.length, textLength - 1));
        java.util.Arrays.fill(text, Math.min(keyword.length, textLength - 1) + 1, textLength, (byte) 'x');

        ByteArrayOutputStream out = new ByteArrayOutputStream(totalBytes);
        out.writeBytes(SIGNATURE);
        writeChunk(out, "IHDR", ihdr);
        writeChunk(out, "tEXt", text);
        writeChunk(out, "IDAT", idat);
        writeChunk(out, "IEND", new byte[0]);
        byte[] png = out.toByteArray();
        if (png.length != totalBytes) {
            throw new IllegalStateException("PNG com " + png.length + " bytes, esperado " + totalBytes);
        }
        return png;
    }

    public static Map<String, Object> attachment(String name, String type, byte[] content) {
        return attachment(name, type, content, content.length);
    }

    /** Permite declarar um {@code size} diferente do conteúdo real (cliente mentindo o tamanho). */
    public static Map<String, Object> attachment(String name, String type, byte[] content, int declaredSize) {
        Map<String, Object> file = new HashMap<>();
        file.put("name", name);
        file.put("type", type);
        file.put("size", String.valueOf(declaredSize));
        file.put("base64", Base64.getEncoder().encodeToString(content));
        return file;
    }

    public static Map<String, Object> smallPng(String name) {
        return attachment(name, "image/png", pngOfSize(200));
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
        return java.util.Arrays.copyOf(buffer, length);
    }
}

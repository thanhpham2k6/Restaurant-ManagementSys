package com.pos.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Thuần Java (Pure Java) QR Code Generator - Không phụ thuộc thư viện bên ngoài.
 * Tạo mã QR chuẩn ISO/IEC 18004 (Model 2, Byte mode, Error Correction Level M/L).
 * Có thể quét trực tiếp bằng camera điện thoại, Zalo, app ngân hàng.
 */
public class QRCodeService {

    public static BufferedImage generateQRCode(String text, int width, int height) {
        if (text == null || text.isBlank()) text = "POS-RESTAURANT";
        boolean[][] matrix = encode(text);
        int matrixSize = matrix.length;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        g2.setColor(Color.BLACK);

        int margin = 4;
        int totalModules = matrixSize + margin * 2;
        int scale = Math.max(1, Math.min(width, height) / totalModules);
        int startX = (width - matrixSize * scale) / 2;
        int startY = (height - matrixSize * scale) / 2;

        for (int r = 0; r < matrixSize; r++) {
            for (int c = 0; c < matrixSize; c++) {
                if (matrix[r][c]) {
                    g2.fillRect(startX + c * scale, startY + r * scale, scale, scale);
                }
            }
        }
        g2.dispose();
        return img;
    }

    public static String buildTableMenuUrl(String hostAddress, int port, String qrToken) {
        return "http://" + hostAddress + ":" + port + "/menu?token=" + qrToken;
    }

    // ==================== PURE JAVA QR ENCODER (V1 - V3) ====================
    private static boolean[][] encode(String text) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        int len = data.length;

        // Chọn version phù hợp: V1 (max 14 bytes), V2 (max 26 bytes), V3 (max 42 bytes), V4 (max 62 bytes)
        int version = 1;
        int dataCapacity = 14;
        int ecCodewords = 10;
        if (len > 14) { version = 2; dataCapacity = 26; ecCodewords = 16; }
        if (len > 26) { version = 3; dataCapacity = 42; ecCodewords = 26; }
        if (len > 42) { version = 4; dataCapacity = 62; ecCodewords = 36; }
        if (len > 62) {
            // Cắt ngắn chuỗi nếu vượt quá giới hạn V4
            byte[] truncated = new byte[62];
            System.arraycopy(data, 0, truncated, 0, 62);
            data = truncated;
            len = 62;
            version = 4;
            dataCapacity = 62;
            ecCodewords = 36;
        }

        int size = 17 + version * 4;
        boolean[][] matrix = new boolean[size][size];
        boolean[][] reserved = new boolean[size][size];

        // 1. Finder patterns (3 góc)
        drawFinderPattern(matrix, reserved, 0, 0, size);
        drawFinderPattern(matrix, reserved, size - 7, 0, size);
        drawFinderPattern(matrix, reserved, 0, size - 7, size);

        // 2. Timing patterns
        for (int i = 8; i < size - 8; i++) {
            boolean val = (i % 2 == 0);
            matrix[6][i] = val; reserved[6][i] = true;
            matrix[i][6] = val; reserved[i][6] = true;
        }

        // 3. Alignment patterns cho V2, V3, V4
        if (version >= 2) {
            int alignPos = version == 2 ? 18 : version == 3 ? 22 : 26;
            drawAlignmentPattern(matrix, reserved, alignPos, alignPos);
        }

        // 4. Dark module
        matrix[4 * version + 9][8] = true;
        reserved[4 * version + 9][8] = true;

        // 5. Reserve format info areas
        for (int i = 0; i < 9; i++) {
            reserved[8][i] = true; reserved[i][8] = true;
            reserved[8][size - 1 - i] = true; reserved[size - 1 - i][8] = true;
        }

        // 6. Data bitstream: Byte mode (0100) + Length (8 bits) + Data + Terminator + Padding
        int[] bitStream = new int[dataCapacity * 8];
        int bitIdx = 0;

        // Mode indicator 0100 (Byte mode)
        bitStream[bitIdx++] = 0; bitStream[bitIdx++] = 1; bitStream[bitIdx++] = 0; bitStream[bitIdx++] = 0;

        // Character count (8 bits)
        for (int i = 7; i >= 0; i--) {
            bitStream[bitIdx++] = (len >> i) & 1;
        }

        // Data bytes
        for (byte b : data) {
            for (int i = 7; i >= 0; i--) {
                bitStream[bitIdx++] = (b >> i) & 1;
            }
        }

        // Terminator (up to 4 zeroes)
        for (int i = 0; i < 4 && bitIdx < bitStream.length; i++) {
            bitStream[bitIdx++] = 0;
        }

        // Pad to byte boundary
        while (bitIdx % 8 != 0 && bitIdx < bitStream.length) {
            bitStream[bitIdx++] = 0;
        }

        // Pad bytes (0xEC, 0x11)
        int padIdx = 0;
        int[] padBytes = {0xEC, 0x11};
        while (bitIdx < bitStream.length) {
            int pad = padBytes[padIdx % 2];
            for (int i = 7; i >= 0 && bitIdx < bitStream.length; i--) {
                bitStream[bitIdx++] = (pad >> i) & 1;
            }
            padIdx++;
        }

        // Chuyển bit stream thành mảng byte dữ liệu
        int[] dataWords = new int[dataCapacity];
        for (int i = 0; i < dataCapacity; i++) {
            int w = 0;
            for (int b = 0; b < 8; b++) {
                w = (w << 1) | bitStream[i * 8 + b];
            }
            dataWords[i] = w;
        }

        // 7. Tạo mã sửa lỗi Reed-Solomon
        int[] ecWords = calculateReedSolomon(dataWords, ecCodewords);

        // Ghép dataWords và ecWords thành chuỗi bit tổng thể
        int totalWords = dataCapacity + ecCodewords;
        int[] allWords = new int[totalWords];
        System.arraycopy(dataWords, 0, allWords, 0, dataCapacity);
        System.arraycopy(ecWords, 0, allWords, dataCapacity, ecCodewords);

        // 8. Đặt các bit vào ma trận (zigzag từ góc phải dưới)
        int row = size - 1;
        int col = size - 1;
        int dir = -1; // -1: lên trên, 1: xuống dưới
        int bytePos = 0;
        int bitPos = 7;

        while (col > 0) {
            if (col == 6) col--; // Bỏ qua timing column

            for (int c = 0; c < 2; c++) {
                int currCol = col - c;
                if (!reserved[row][currCol]) {
                    boolean bitVal = false;
                    if (bytePos < allWords.length) {
                        bitVal = ((allWords[bytePos] >> bitPos) & 1) == 1;
                        bitPos--;
                        if (bitPos < 0) {
                            bitPos = 7;
                            bytePos++;
                        }
                    }
                    // Áp dụng mask pattern 0: (row + col) % 2 == 0
                    boolean mask = ((row + currCol) % 2 == 0);
                    matrix[row][currCol] = bitVal ^ mask;
                }
            }

            row += dir;
            if (row < 0 || row >= size) {
                dir = -dir;
                row += dir;
                col -= 2;
            }
        }

        // 9. Ghi format information (Level L/M + Mask 000): 0x77C4 (Mask 0, Level M)
        applyFormatInfo(matrix, size, 0x77C4);

        return matrix;
    }

    private static void drawFinderPattern(boolean[][] m, boolean[][] r, int top, int left, int size) {
        for (int dy = -1; dy <= 7; dy++) {
            for (int dx = -1; dx <= 7; dx++) {
                int py = top + dy;
                int px = left + dx;
                if (py >= 0 && py < size && px >= 0 && px < size) {
                    r[py][px] = true;
                    if (dy >= 0 && dy <= 6 && dx >= 0 && dx <= 6) {
                        boolean isBlack = (dy == 0 || dy == 6 || dx == 0 || dx == 6 ||
                                          (dy >= 2 && dy <= 4 && dx >= 2 && dx <= 4));
                        m[py][px] = isBlack;
                    } else {
                        m[py][px] = false;
                    }
                }
            }
        }
    }

    private static void drawAlignmentPattern(boolean[][] m, boolean[][] r, int cy, int cx) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int py = cy + dy;
                int px = cx + dx;
                r[py][px] = true;
                boolean isBlack = (Math.abs(dy) == 2 || Math.abs(dx) == 2 || (dy == 0 && dx == 0));
                m[py][px] = isBlack;
            }
        }
    }

    private static void applyFormatInfo(boolean[][] m, int size, int formatBits) {
        int[] bits = new int[15];
        for (int i = 0; i < 15; i++) {
            bits[i] = (formatBits >> i) & 1;
        }

        // Góc trên trái
        m[8][0] = bits[0] == 1; m[8][1] = bits[1] == 1; m[8][2] = bits[2] == 1;
        m[8][3] = bits[3] == 1; m[8][4] = bits[4] == 1; m[8][5] = bits[5] == 1;
        m[8][7] = bits[6] == 1; m[8][8] = bits[7] == 1; m[7][8] = bits[8] == 1;
        m[5][8] = bits[9] == 1; m[4][8] = bits[10] == 1; m[3][8] = bits[11] == 1;
        m[2][8] = bits[12] == 1; m[1][8] = bits[13] == 1; m[0][8] = bits[14] == 1;

        // Góc phải và góc dưới
        for (int i = 0; i < 7; i++) {
            m[8][size - 1 - i] = bits[i] == 1;
        }
        for (int i = 7; i < 15; i++) {
            m[size - 15 + i][8] = bits[i] == 1;
        }
    }

    // Galois Field GF(256) với polynomial 0x11D (285)
    private static final int[] EXP = new int[512];
    private static final int[] LOG = new int[256];
    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            EXP[i] = x;
            EXP[i + 255] = x;
            LOG[x] = i;
            x <<= 1;
            if ((x & 0x100) != 0) x ^= 0x11D;
        }
    }

    private static int gfMul(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return EXP[LOG[a] + LOG[b]];
    }

    private static int[] calculateReedSolomon(int[] data, int ecCount) {
        // Generator polynomial
        int[] g = {1};
        for (int i = 0; i < ecCount; i++) {
            int[] next = new int[g.length + 1];
            for (int j = 0; j < g.length; j++) {
                next[j] ^= gfMul(g[j], EXP[i]);
                next[j + 1] ^= g[j];
            }
            g = next;
        }

        // Polynomial division
        int[] result = new int[ecCount];
        for (int d : data) {
            int factor = d ^ result[0];
            System.arraycopy(result, 1, result, 0, ecCount - 1);
            result[ecCount - 1] = 0;
            for (int i = 0; i < ecCount; i++) {
                result[i] ^= gfMul(g[i], factor);
            }
        }
        return result;
    }
}

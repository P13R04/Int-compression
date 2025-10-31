package io.compress.intpack;

import java.util.Objects;

/**
 * Low-level bit IO helpers for int[] buffers.
 * Convention: LSB-first inside each 32-bit word (bit 0 = LSB of word 0).
 */
final class BitIO {
    private BitIO() {}

    private static int mask(int bits) {
        return (bits == 32) ? -1 : ((1 << bits) - 1);
    }

    /**
     * Read up to 32 bits starting at bitPos (0 = LSB of word 0), LSB-first.
     */
    static int readBitsLSB(int[] words, int bitPos, int bitLen) {
        Objects.requireNonNull(words);
        if (bitLen == 0) return 0;
        int w = bitPos >>> 5; // /32
        int o = bitPos & 31; // %32
        int v = words[w] >>> o;
        int left = o + bitLen;
        if (left > 32) {
            // bring the high part (may be negative if signed) and shift
            v |= (words[w + 1] << (32 - o));
        }
        return (bitLen == 32) ? v : (v & mask(bitLen));
    }

    /**
     * Write up to 32 bits starting at bitPos (0 = LSB of word 0), LSB-first.
     */
    static void writeBitsLSB(int[] words, int bitPos, int bitLen, int value) {
        if (bitLen == 0) return;
        int w = bitPos >>> 5;
        int o = bitPos & 31;
        int m = mask(bitLen);
        int v = value & m;
        // low part into words[w]
        words[w] = (words[w] & ~(m << o)) | (v << o);
        int left = o + bitLen;
        if (left > 32) {
            int hi = v >>> (32 - o);
            int hiBits = left - 32;
            int hiMask = mask(hiBits);
            words[w + 1] = (words[w + 1] & ~hiMask) | (hi & hiMask);
        }
    }

    // Helpers for NO_CROSSING: operations that fit within a single word.
    static int readBitsInWordLSB(int word, int offset, int bitLen) {
        int m = mask(bitLen);
        return (word >>> offset) & m;
    }

    static int writeBitsInWordLSB(int word, int offset, int bitLen, int value) {
        int m = mask(bitLen);
        int v = value & m;
        return (word & ~(m << offset)) | (v << offset);
    }

    // Optional: MSB-oriented helpers using coordinate conversion
    static int readBitsMSB(int[] words, int bitPos, int bitLen) {
        if (bitLen == 0) return 0;
        int w = bitPos >>> 5;
        int oMSB = bitPos & 31;
        int oLSB = 31 - oMSB - (bitLen - 1);
        int abs = (w << 5) + oLSB;
        return readBitsLSB(words, abs, bitLen);
    }

    static void writeBitsMSB(int[] words, int bitPos, int bitLen, int value) {
        if (bitLen == 0) return;
        int w = bitPos >>> 5;
        int oMSB = bitPos & 31;
        int oLSB = 31 - oMSB - (bitLen - 1);
        int abs = (w << 5) + oLSB;
        writeBitsLSB(words, abs, bitLen, value);
    }
}

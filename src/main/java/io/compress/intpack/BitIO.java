package io.compress.intpack;

import java.util.Objects;

/**
 * Utilitaires bas niveau de lecture/écriture de bits sur des buffers int[].
 * Convention: LSB-first dans chaque entier 32 bits (bit 0 = LSB du mot 0).
 */
final class BitIO {
    private BitIO() {}

    private static int mask(int bits) {
        // Retourne un masque de 'bits' bits à 1 (cas particulier = 32)
        return (bits == 32) ? -1 : ((1 << bits) - 1);
    }

    /**
     * Lecture (LSB-first) d'au plus 32 bits à partir de bitPos (0 = LSB du mot 0).
     * Peut chevaucher 2 entiers consécutifs si (offset+bitLen) > 32.
     */
    static int readBitsLSB(int[] words, int bitPos, int bitLen) {
        Objects.requireNonNull(words);
        if (bitLen == 0) return 0;
        int w = bitPos >>> 5; // /32
        int o = bitPos & 31; // %32
        int v = words[w] >>> o;
        int left = o + bitLen;
        if (left > 32) {
            // Récupère la partie haute provenant du word suivant
            v |= (words[w + 1] << (32 - o));
        }
        return (bitLen == 32) ? v : (v & mask(bitLen));
    }

    /**
     * Écriture (LSB-first) d'au plus 32 bits à partir de bitPos (0 = LSB du mot 0).
     * Peut écrire sur 2 entiers si nécessaire.
     */
    static void writeBitsLSB(int[] words, int bitPos, int bitLen, int value) {
        if (bitLen == 0) return;
        int w = bitPos >>> 5;
        int o = bitPos & 31;
        int m = mask(bitLen);
        int v = value & m;
        // Écrit la partie basse dans words[w]
        words[w] = (words[w] & ~(m << o)) | (v << o);
        int left = o + bitLen;
        if (left > 32) {
            int hi = v >>> (32 - o);
            int hiBits = left - 32;
            int hiMask = mask(hiBits);
            words[w + 1] = (words[w + 1] & ~hiMask) | (hi & hiMask);
        }
    }

    // Helpers pour NO_CROSSING: opérations confinées à un seul entier.
    static int readBitsInWordLSB(int word, int offset, int bitLen) {
        int m = mask(bitLen);
        return (word >>> offset) & m;
    }

    static int writeBitsInWordLSB(int word, int offset, int bitLen, int value) {
        int m = mask(bitLen);
        int v = value & m;
        return (word & ~(m << offset)) | (v << offset);
    }

    // Optionnel: helpers orientés MSB via conversion de coordonnées
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

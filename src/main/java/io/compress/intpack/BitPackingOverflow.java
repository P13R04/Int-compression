package io.compress.intpack;

import java.util.ArrayList;
import java.util.List;


/**
 * Variante OVERFLOW avec zone d'overflow.
 * Idée: choisir kSmall (1..32) qui minimise le total de bits
 *   total = n * (1 + payloadBits) + m * 32
 * où n = nombre d'éléments, m = nombre d'éléments envoyés en overflow,
 * flag = 1 bit par valeur (local/overflow), payload = soit la valeur (locale)
 * soit l'index dans la zone overflow. La zone overflow est stockée en words bruts.
 */
final class BitPackingOverflow extends BitPackingBase {

    BitPackingOverflow(CompressorFactory.Options opts) { super(opts); }

    private static final class Plan {
        int kSmall;
        int payloadBits;
        int[] mapIndex; // -1 => local (value dans payload), >=0 => index dans overflowVals
        List<Integer> overflowVals = new ArrayList<>();
    }

    private Plan plan(int[] src) {
        final int n = src.length;
        Plan best = null;
        long bestBits = Long.MAX_VALUE;

        // Essaye plusieurs k candidats et conserve celui qui minimise total bits
        for (int k = 1; k <= 32; k++) {
            int m = 0;
            for (int v : src) {
                if (v < 0 || v >= (1L << k)) m++; // négatifs → overflow obligatoire
            }
            int bitsIdx = ceilLog2(Math.max(1, m));
            int payloadBits = Math.max(k, bitsIdx);
            long dataBits = (long) n * (1 + payloadBits);
            long overflowBits = (long) m * 32L;
            long total = dataBits + overflowBits;
            if (total < bestBits) {
                bestBits = total;
                best = new Plan();
                best.kSmall = k;
                best.payloadBits = payloadBits;
            }
        }

        // Construit le mapping selon kSmall choisi
        Plan p = best;
        p.mapIndex = new int[n];
        for (int i = 0; i < n; i++) {
            int v = src[i];
            if (v < 0 || v >= (1L << p.kSmall)) {
                p.mapIndex[i] = p.overflowVals.size();
                p.overflowVals.add(v);
            } else {
                p.mapIndex[i] = -1; // local
            }
        }
        return p;
    }


    @Override
    public int[] compress(int[] src) {
        if (src == null) throw new IllegalArgumentException("src null");
        final int n = src.length;
        final Plan p = plan(src);
        final int m = p.overflowVals.size();
    final int dataBits = n * (1 + p.payloadBits); // 1 flag + payload
    final int overflowBits = m * 32; // words d'overflow bruts

        int[] out = allocWithHeader(Headers.HEADER_WORDS, dataBits + overflowBits);
    Headers.write(out, n, CompressionType.OVERFLOW, p.kSmall, 0, p.payloadBits, m);

        // Écrit le flux principal (flag + payload)
        final int base = Headers.HEADER_WORDS;
        int bitPos = 0;
        for (int i = 0; i < n; i++) {
            int idx = p.mapIndex[i];
            if (idx < 0) {
                BitIO.writeBitsLSB(out, (base << 5) + bitPos, 1, 0);
                BitIO.writeBitsLSB(out, (base << 5) + bitPos + 1, p.payloadBits, src[i]);
            } else {
                BitIO.writeBitsLSB(out, (base << 5) + bitPos, 1, 1);
                BitIO.writeBitsLSB(out, (base << 5) + bitPos + 1, p.payloadBits, idx);
            }
            bitPos += 1 + p.payloadBits;
        }

        // Ajoute la zone overflow alignée sur le prochain word
        int overflowBaseBits = (base << 5) + dataBits;
        int overflowBaseWord = (overflowBaseBits + 31) >>> 5;
        for (int i = 0; i < m; i++) {
            out[overflowBaseWord + i] = p.overflowVals.get(i);
        }
        return out;
    }


    @Override
    public void decompress(int[] compressed, int[] dst) {
    int base = Headers.checkMagic(compressed);
    int n = Headers.n(compressed);
    int payloadBits = Headers.extraA(compressed);
        int m = Headers.extraB(compressed);
        if (dst.length < n) throw new IllegalArgumentException("dst trop petit");

        int dataBits = n * (1 + payloadBits);
        int overflowBaseWord = ((base << 5) + dataBits + 31) >>> 5;

        int bitPos = 0;
        for (int i = 0; i < n; i++) {
            int flag = BitIO.readBitsLSB(compressed, (base << 5) + bitPos, 1);
            int payload = BitIO.readBitsLSB(compressed, (base << 5) + bitPos + 1, payloadBits);
            if (flag == 0) {
                dst[i] = payload;
            } else {
                if (payload < 0 || payload >= m) throw new IllegalStateException("idx overflow invalide");
                dst[i] = compressed[overflowBaseWord + payload];
            }
            bitPos += 1 + payloadBits;
        }
    }


    @Override
    public int get(int[] compressed, int index) {
        int base = Headers.checkMagic(compressed);
        int n = Headers.n(compressed);
        if (index < 0 || index >= n) throw new IndexOutOfBoundsException();
        int payloadBits = Headers.extraA(compressed);
        int m = Headers.extraB(compressed);
        int dataBits = n * (1 + payloadBits);
        int overflowBaseWord = ((base << 5) + dataBits + 31) >>> 5;

        int bitPos = index * (1 + payloadBits);
        int flag = BitIO.readBitsLSB(compressed, (base << 5) + bitPos, 1);
        int payload = BitIO.readBitsLSB(compressed, (base << 5) + bitPos + 1, payloadBits);
        if (flag == 0) return payload;
        if (payload < 0 || payload >= m) throw new IllegalStateException("idx overflow invalide");
        return compressed[overflowBaseWord + payload];
    }
}

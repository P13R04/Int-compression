package io.compress.intpack;


/**
 * Variante NO_CROSSING : chaque valeur tient entièrement dans un word.
 * On packe per = floor(32/k) valeurs par word sans chevauchement.
 */
final class BitPackingNoCrossing extends BitPackingBase {
BitPackingNoCrossing(CompressorFactory.Options opts) { super(opts); }


@Override
public int[] compress(int[] src) {
final int n = src.length;
final int k = (opts.k > 0) ? opts.k : computeKAuto(src);
final int per = perWordNoCrossing(k);
final int wordsForData = (n + per - 1) / per; // chaque mot contient per valeurs
int[] out = new int[Headers.HEADER_WORDS + wordsForData];
Headers.write(out, n, CompressionType.NO_CROSSING, k, 0, 0, 0);
int w = Headers.HEADER_WORDS;
int cnt = 0; int off = 0; int cur = 0;
for (int v : src) {
cur = BitIO.writeBitsInWordLSB(cur, off, k, v);
cnt++; off += k;
if (cnt == per) { out[w++] = cur; cnt = 0; off = 0; cur = 0; }
}
if (cnt != 0) out[w] = cur; // dernier mot partiellement rempli
return out;
}


@Override
public void decompress(int[] compressed, int[] dst) {
Headers.checkMagic(compressed);
int n = Headers.n(compressed);
int k = Headers.k(compressed);
int per = perWordNoCrossing(k);
if (dst.length < n) throw new IllegalArgumentException("dst trop petit");
int w = Headers.HEADER_WORDS;
int outIdx = 0;
while (outIdx < n) {
int word = compressed[w++];
for (int i = 0; i < per && outIdx < n; i++) {
int off = i * k;
dst[outIdx++] = BitIO.readBitsInWordLSB(word, off, k);
}
}
}


@Override
public int get(int[] compressed, int index) {
Headers.checkMagic(compressed);
int n = Headers.n(compressed);
if (index < 0 || index >= n) throw new IndexOutOfBoundsException();
int k = Headers.k(compressed);
int per = perWordNoCrossing(k);
int w = Headers.HEADER_WORDS + (index / per);
int off = (index % per) * k;
return BitIO.readBitsInWordLSB(compressed[w], off, k);
}
}

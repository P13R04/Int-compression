package io.compress.intpack;


/**
* Empaquetage autorisant le chevauchement : indexation par position bit i*k.
*/
final class BitPackingCrossing extends BitPackingBase {


BitPackingCrossing(CompressorFactory.Options opts) { super(opts); }


@Override
public int[] compress(int[] src) {
if (src == null) throw new IllegalArgumentException("src null");
final int n = src.length;
final int k = (opts.k > 0) ? opts.k : computeKAuto(src);
final int dataBits = n * k;
int[] out = allocWithHeader(Headers.HEADER_WORDS, dataBits);
Headers.write(out, n, CompressionType.CROSSING, k, 0, 0, 0);
final int base = Headers.HEADER_WORDS;
int bitPos = 0;
for (int v : src) {
BitIO.writeBitsLSB(out, (base << 5) + bitPos, k, v);
bitPos += k;
}
return out;
}


@Override
public void decompress(int[] compressed, int[] dst) {
int base = Headers.checkMagic(compressed);
int n = Headers.n(compressed);
int k = Headers.k(compressed);
if (dst.length < n) throw new IllegalArgumentException("dst trop petit");
int bitPos = 0;
for (int i = 0; i < n; i++) {
dst[i] = BitIO.readBitsLSB(compressed, (base << 5) + bitPos, k);
bitPos += k;
}
}


@Override
public int get(int[] compressed, int index) {
int base = Headers.checkMagic(compressed);
int n = Headers.n(compressed);
if (index < 0 || index >= n) throw new IndexOutOfBoundsException();
int k = Headers.k(compressed);
int bitPos = index * k;
return BitIO.readBitsLSB(compressed, (base << 5) + bitPos, k);
}
}

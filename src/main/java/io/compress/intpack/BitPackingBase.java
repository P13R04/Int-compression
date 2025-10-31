package io.compress.intpack;


/**
* Classe de base avec utilitaires communs : calcul de k, masques, header, etc.
*/
abstract class BitPackingBase implements IntCompressor {
protected final CompressorFactory.Options opts;


protected BitPackingBase(CompressorFactory.Options opts) { this.opts = opts; }


protected static int ceilLog2(int x) {
if (x <= 1) return 0;
int v = x - 1;
int l = 0;
while (v > 0) { v >>>= 1; l++; }
return l;
}


protected static int computeKAuto(int[] src) {
int max = 0;
for (int v : src) if (v > max) max = v;
int k = ceilLog2(max + 1);
if (k <= 0) k = 1;
if (k > 32) k = 32;
return k;
}


protected static int[] allocWithHeader(int headerWords, int dataBits) {
int words = headerWords + ((dataBits + 31) >>> 5);
return new int[words];
}


protected static int perWordNoCrossing(int k) {
return Math.max(1, 32 / Math.max(1, k));
}
}

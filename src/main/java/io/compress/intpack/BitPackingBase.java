package io.compress.intpack;


/**
 * Classe de base avec utilitaires communs.
 * Rôle: calcul de k (bits par valeur), allocation du buffer avec header,
 * et helpers partagés par les variantes (CROSSING / NO_CROSSING / OVERFLOW).
 */
abstract class BitPackingBase implements IntCompressor {
protected final CompressorFactory.Options opts;


protected BitPackingBase(CompressorFactory.Options opts) { this.opts = opts; }


/**
 * ceil(log2(x)) pour x>0. Renvoie 0 pour x<=1.
 */
protected static int ceilLog2(int x) {
if (x <= 1) return 0;
int v = x - 1;
int l = 0;
while (v > 0) { v >>>= 1; l++; }
return l;
}


/**
 * Détermine automatiquement k à partir du max des valeurs.
 * k = ceil(log2(max+1)) borné dans [1,32].
 */
protected static int computeKAuto(int[] src) {
int max = 0;
for (int v : src) if (v > max) max = v;
int k = ceilLog2(max + 1);
if (k <= 0) k = 1;
if (k > 32) k = 32;
return k;
}


/**
 * Alloue un buffer int[] de taille suffisante pour 'headerWords' + data en bits.
 */
protected static int[] allocWithHeader(int headerWords, int dataBits) {
int words = headerWords + ((dataBits + 31) >>> 5);
return new int[words];
}


/**
 * Nombre d'éléments par word en mode NO_CROSSING (pas de chevauchement intra-word).
 */
protected static int perWordNoCrossing(int k) {
return Math.max(1, 32 / Math.max(1, k));
}
}

package io.compress.intpack;


/**
* Point d’entrée unique pour créer un compresseur selon un type.
* Options minimales exposées ici ; on pourra étendre plus tard.
*/
public final class CompressorFactory {


public static class Options {
/** k fixé (bits par valeur). Si <=0, k est calculé automatiquement. */
public int k = 0;
/** Active une estimation simple du k pour OVERFLOW (sinon auto). */
public boolean fixedKForOverflow = false;
/**
* Choix de l’endianess des bits par mot (LSB-first recommandé).
* true = LSB-first (bit 0 = LSB de word 0). false = MSB-first.
*/
public boolean lsbFirst = true;
}


private CompressorFactory() {}


public static IntCompressor create(CompressionType type, Options opts) {
if (opts == null) opts = new Options();
switch (type) {
case CROSSING: return new BitPackingCrossing(opts);
case NO_CROSSING: return new BitPackingNoCrossing(opts);
case OVERFLOW: return new BitPackingOverflow(opts);
default: throw new IllegalArgumentException("Unknown type: " + type);
}
}
}

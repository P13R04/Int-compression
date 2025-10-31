package io.compress.intpack;


/**
* Encodage/décodage du header commun dans le buffer compressé.
* Layout (en int, simple et robuste) :
* [0] magic (0x1B1TPACK ~ « bitpack »),
* [1] n (taille),
* [2] mode (0=CROSSING,1=NO_CROSSING,2=OVERFLOW) | (k << 8) | (flags << 16)
* [3] extraA (ex: payloadBits ou m selon mode)
* [4] extraB (ex: m pour OVERFLOW)
* Data débute à l’index HEADER_WORDS.
*/
final class Headers {
static final int MAGIC = 0x1B17ECA7; // marker for verification (valid hex)
static final int HEADER_WORDS = 5;


static void write(int[] out, int n, CompressionType mode, int k, int flags,
int extraA, int extraB) {
out[0] = MAGIC;
out[1] = n;
int modeVal = (mode == CompressionType.CROSSING) ? 0 :
(mode == CompressionType.NO_CROSSING) ? 1 : 2;
out[2] = (modeVal & 0xFF) | ((k & 0xFF) << 8) | ((flags & 0xFFFF) << 16);
out[3] = extraA;
out[4] = extraB;
}


static int checkMagic(int[] in) {
if (in == null || in.length < HEADER_WORDS || in[0] != MAGIC)
throw new IllegalArgumentException("Invalid compressed buffer or magic");
return HEADER_WORDS;
}


static int n(int[] in) { checkMagic(in); return in[1]; }
static int k(int[] in) { checkMagic(in); return (in[2] >>> 8) & 0xFF; }
static int flags(int[] in) { checkMagic(in); return (in[2] >>> 16) & 0xFFFF; }
static CompressionType mode(int[] in) {
checkMagic(in);
int m = in[2] & 0xFF;
return (m == 0) ? CompressionType.CROSSING : (m == 1 ? CompressionType.NO_CROSSING : CompressionType.OVERFLOW);
}
static int extraA(int[] in) { checkMagic(in); return in[3]; }
static int extraB(int[] in) { checkMagic(in); return in[4]; }
}

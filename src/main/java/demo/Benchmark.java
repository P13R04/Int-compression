package demo;

import io.compress.intpack.*;
import java.util.Random;
import java.util.Locale;

/**
 * Petit programme de bench pour mesurer compress/decompress des trois variantes.
 * Usage: run with java -cp out demo.Benchmark [n]
 */
public class Benchmark {
    public static void main(String[] args) {
        int n = 1_000_000;
        if (args.length > 0) try { n = Integer.parseInt(args[0]); } catch (Exception e) {}
        System.out.println("Benchmark arrays of length " + n);
        int[] data = new int[n];
        Random rnd = new Random(42);
        for (int i = 0; i < n; i++) {
            // mixture: many small values, few large
            int r = rnd.nextInt(1000);
            data[i] = (r < 990) ? rnd.nextInt(128) : (1 << (10 + rnd.nextInt(10)));
        }

        CompressorFactory.Options opts = new CompressorFactory.Options();

        benchType(CompressionType.CROSSING, data, opts);
        benchType(CompressionType.NO_CROSSING, data, opts);
        benchType(CompressionType.OVERFLOW, data, opts);
    }

    static void benchType(CompressionType type, int[] data, CompressorFactory.Options opts) {
        System.out.println("--- " + type + " ---");
        IntCompressor c = CompressorFactory.create(type, opts);

        // warmup
        for (int i = 0; i < 3; i++) {
            int[] comp = c.compress(data);
            int[] out = new int[data.length];
            c.decompress(comp, out);
        }

        // measure compress
        long t0 = System.nanoTime();
        int[] comp = c.compress(data);
        long t1 = System.nanoTime();
        long compressNs = t1 - t0;

        // measure decompress
        int[] out = new int[data.length];
        long t2 = System.nanoTime();
        c.decompress(comp, out);
        long t3 = System.nanoTime();
        long decompressNs = t3 - t2;

    int baseWords = data.length;
    double ratio = (double) comp.length / baseWords;
    double kEff = 32.0 * comp.length / baseWords;
    System.out.printf(
        Locale.ROOT,
        "base_words=%d, compressed words=%d, ratio=%.3f, k_eff(bits/val)=%.3f, compress=%.3f ms, decompress=%.3f ms\n",
        baseWords, comp.length, ratio, kEff, compressNs / 1e6, decompressNs / 1e6);

        // quick verification
        boolean ok = java.util.Arrays.equals(data, out);
        System.out.println("roundtrip ok=" + ok);
    }
}

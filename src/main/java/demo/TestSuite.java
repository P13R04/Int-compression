package demo;

import io.compress.intpack.*;
import java.util.Arrays;
import java.util.Random;

/**
 * Mini test runner autonome (sans JUnit) pour valider les trois variantes
 * sur des cas déterministes, aléatoires et des valeurs négatives (OVERFLOW).
 * L'objectif est d'avoir un feedback rapide en dehors de l'environnement de test.
 */
public class TestSuite {
    public static void main(String[] args) {
        int failures = 0;
        failures += runAllTests();
        if (failures == 0) {
            System.out.println("ALL TESTS PASSED");
            System.exit(0);
        } else {
            System.out.println("FAILED TESTS: " + failures);
            System.exit(2);
        }
    }

    static int runAllTests() {
        int fails = 0;
        CompressorFactory.Options opts = new CompressorFactory.Options();

        System.out.println("== Basic deterministic tests ==");
        int[] data1 = {0,1,2,3,4,5,6,7,15,31,1024,1<<20};
        fails += checkAll(data1, opts) ? 0 : 1;

        System.out.println("== Zeros and max values ==");
        int[] data2 = new int[128];
        Arrays.fill(data2, 0);
        fails += checkAll(data2, opts) ? 0 : 1;
        int[] data3 = new int[128];
        Arrays.fill(data3, Integer.MAX_VALUE >>> 1);
        fails += checkAll(data3, opts) ? 0 : 1;

        System.out.println("== Random tests ==");
        Random rnd = new Random(123456);
        for (int t = 0; t < 10; t++) {
            int n = 100 + rnd.nextInt(900);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(1<<20); // moderate values
            fails += checkAll(a, opts) ? 0 : 1;
        }

    System.out.println("== Tests with negatives (ONLY OVERFLOW) ==");
    int[] neg = { -1, -2, -3, 0, 1, 2, 1024, -100000 }; // expect only OVERFLOW to handle negatives
    System.out.println("-- Testing OVERFLOW n=" + neg.length);
    IntCompressor c = CompressorFactory.create(CompressionType.OVERFLOW, opts);
    int[] compressed = c.compress(neg);
    int[] out = new int[neg.length];
    c.decompress(compressed, out);
    boolean eq = Arrays.equals(neg, out);
    System.out.println("OVERFLOW roundtrip: " + eq + " (compressed words=" + compressed.length + ")");
    if (!eq) fails++;

        return fails;
    }

    static boolean checkAll(int[] data, CompressorFactory.Options opts) {
        boolean ok = true;
        ok &= runForType(CompressionType.CROSSING, data, opts);
        ok &= runForType(CompressionType.NO_CROSSING, data, opts);
        ok &= runForType(CompressionType.OVERFLOW, data, opts);
        return ok;
    }

    static boolean runForType(CompressionType type, int[] data, CompressorFactory.Options opts) {
        System.out.println("-- Testing " + type + " n=" + data.length);
        IntCompressor c = CompressorFactory.create(type, opts);
        int[] compressed = c.compress(data);
        int[] out = new int[data.length];
        c.decompress(compressed, out);
        boolean eq = Arrays.equals(data, out);
        System.out.println(type + " roundtrip: " + eq + " (compressed words=" + compressed.length + ")");
        if (!eq) return false;

        // spot-check get()
        if (data.length > 0) {
            int idx = Math.min(10, data.length-1);
            int gv = c.get(compressed, idx);
            if (gv != data[idx]) {
                System.out.println("get() mismatch at " + idx + ": expected " + data[idx] + " got " + gv);
                return false;
            }
        }
        return true;
    }
}

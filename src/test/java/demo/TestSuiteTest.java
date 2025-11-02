package demo;

import io.compress.intpack.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Random;

public class TestSuiteTest {

    @Test
    public void basicDeterministic() {
        CompressorFactory.Options opts = new CompressorFactory.Options();
        int[] data = {0,1,2,3,4,5,6,7,15,31,1024,1<<20};
        assertAll(
            () -> roundtrip(CompressionType.CROSSING, data, opts),
            () -> roundtrip(CompressionType.NO_CROSSING, data, opts),
            () -> roundtrip(CompressionType.OVERFLOW, data, opts)
        );
    }

    @Test
    public void zerosAndMax() {
        CompressorFactory.Options opts = new CompressorFactory.Options();
        int[] zeros = new int[128];
        Arrays.fill(zeros, 0);
        int[] big = new int[128];
        Arrays.fill(big, Integer.MAX_VALUE >>> 1);
        roundtrip(CompressionType.CROSSING, zeros, opts);
        roundtrip(CompressionType.NO_CROSSING, zeros, opts);
        roundtrip(CompressionType.OVERFLOW, zeros, opts);
        roundtrip(CompressionType.CROSSING, big, opts);
        roundtrip(CompressionType.NO_CROSSING, big, opts);
        roundtrip(CompressionType.OVERFLOW, big, opts);
    }

    @Test
    public void randomSmoke() {
        CompressorFactory.Options opts = new CompressorFactory.Options();
        Random rnd = new Random(123456);
        for (int t = 0; t < 5; t++) {
            int n = 200 + rnd.nextInt(800);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(1<<20);
            roundtrip(CompressionType.CROSSING, a, opts);
            roundtrip(CompressionType.NO_CROSSING, a, opts);
            roundtrip(CompressionType.OVERFLOW, a, opts);
        }
    }

    @Test
    public void overflowHandlesNegatives() {
        CompressorFactory.Options opts = new CompressorFactory.Options();
        int[] neg = { -1, -2, -3, 0, 1, 2, 1024, -100000 };
        roundtrip(CompressionType.OVERFLOW, neg, opts);
    }

    private void roundtrip(CompressionType type, int[] data, CompressorFactory.Options opts) {
        IntCompressor c = CompressorFactory.create(type, opts);
        int[] comp = c.compress(data);
        int[] out = new int[data.length];
        c.decompress(comp, out);
    assertArrayEquals(data, out, "roundtrip en échec pour " + type);
        if (data.length > 0) {
            int idx = Math.min(10, data.length - 1);
            assertEquals(data[idx], c.get(comp, idx));
        }
    }
}

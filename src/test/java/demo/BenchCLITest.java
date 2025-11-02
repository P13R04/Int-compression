package demo;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BenchCLITest {

    private static Path writeTextInts(List<Integer> ints) throws IOException {
        Path p = Files.createTempFile("benchcli-input-", ".txt");
        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            // mix separators: commas, spaces, semicolons
            for (int i = 0; i < ints.size(); i++) {
                int v = ints.get(i);
                if (i % 10 == 0) {
                    w.write(Integer.toString(v));
                    w.write('\n');
                } else if (i % 3 == 0) {
                    w.write(Integer.toString(v));
                    w.write(";");
                } else {
                    w.write(Integer.toString(v));
                    w.write(", ");
                }
            }
        }
        return p;
    }

    private static List<Integer> smallDataset() {
        return java.util.stream.IntStream.range(0, 200)
                .map(i -> (i % 50 == 0) ? (1 << (10 + (i % 10))) : (i % 128))
                .boxed().toList();
    }

    @Test
    public void testTextInputProducesCsvAndAscii() throws Exception {
        List<Integer> ints = smallDataset();
        Path in = writeTextInts(ints);
        Path csv = Files.createTempFile("benchcli-out-", ".csv");
        Path ascii = Files.createTempFile("benchcli-table-", ".txt");

        String[] args = new String[]{
                "--file", in.toString(),
                "--runs", "3",
                "--csv", csv.toString(),
                "--table-out", ascii.toString()
        };
        BenchCLI.main(args);

        assertTrue(Files.exists(csv) && Files.size(csv) > 0, "CSV should be created and non-empty");
        assertTrue(Files.exists(ascii) && Files.size(ascii) > 0, "ASCII table should be created and non-empty");

        List<String> lines = Files.readAllLines(csv);
        assertFalse(lines.isEmpty(), "CSV should have header");
        assertEquals("variant,words,comp_median_ms,comp_iqr_ms,decomp_median_ms,decomp_iqr_ms", lines.get(0));
        String all = Files.readString(ascii);
        assertTrue(all.contains("Variant") && all.contains("words"), "ASCII should contain headers");
        assertTrue(all.contains("CROSSING"));
        assertTrue(all.contains("NO_CROSSING"));
        assertTrue(all.contains("OVERFLOW"));
    }

    @Test
    public void testBinaryInputAlsoWorks() throws Exception {
        // Create a simple binary DataIO-like file: [int count][values...]
        List<Integer> ints = smallDataset();
        Path bin = Files.createTempFile("benchcli-input-", ".bin");
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(bin)))) {
            dos.writeInt(ints.size());
            for (int v : ints) dos.writeInt(v);
        }
        Path csv = Files.createTempFile("benchcli-out-", ".csv");
        String[] args = new String[]{
                "--file", bin.toString(),
                "--runs", "3",
                "--csv", csv.toString()
        };
        BenchCLI.main(args);
        assertTrue(Files.exists(csv) && Files.size(csv) > 0, "CSV should be created for binary input as well");
    }
}

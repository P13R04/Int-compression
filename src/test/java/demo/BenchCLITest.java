package demo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Version sans dépendance JUnit pour éviter les erreurs d'édition dans VS Code.
 * Cette classe peut être exécutée manuellement (main) pour un smoke test du CLI.
 * Les vérifications utilisent des checks simples qui lèvent IllegalStateException.
 */
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

    public static void testTextInputProducesCsvAndAscii() throws Exception {
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

        check(Files.exists(csv) && Files.size(csv) > 0, "CSV should be created and non-empty");
        check(Files.exists(ascii) && Files.size(ascii) > 0, "ASCII table should be created and non-empty");

        List<String> lines = Files.readAllLines(csv);
        check(!lines.isEmpty(), "CSV should have header");
        if (!"variant,base_words,words,ratio,k_eff_bits_per_val,comp_median_ms,comp_iqr_ms,decomp_median_ms,decomp_iqr_ms".equals(lines.get(0))) {
            throw new IllegalStateException("Unexpected CSV header: " + lines.get(0));
        }
        String all = Files.readString(ascii);
        check(all.contains("Variant") && all.contains("words") && all.contains("base_words") && all.contains("k_eff"), "ASCII should contain headers");
        check(all.contains("CROSSING"), "ASCII should contain CROSSING row");
        check(all.contains("NO_CROSSING"), "ASCII should contain NO_CROSSING row");
        check(all.contains("OVERFLOW"), "ASCII should contain OVERFLOW row");
    }

    public static void testBinaryInputAlsoWorks() throws Exception {
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
        check(Files.exists(csv) && Files.size(csv) > 0, "CSV should be created for binary input as well");
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new IllegalStateException(msg);
    }

    public static void main(String[] args) throws Exception {
        // Exécution manuelle des deux scénarios pour un smoke test local
        testTextInputProducesCsvAndAscii();
        testBinaryInputAlsoWorks();
        System.out.println("BenchCLITest manual run: OK");
    }
}

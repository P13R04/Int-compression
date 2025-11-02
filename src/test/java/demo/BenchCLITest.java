package demo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Version sans dépendance JUnit pour éviter des erreurs d'édition dans VS Code.
 * Cette classe peut être exécutée manuellement (main) pour un test rapide du CLI.
 * Les vérifications lèvent IllegalStateException en cas d'échec.
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

    check(Files.exists(csv) && Files.size(csv) > 0, "Le CSV doit être créé et non vide");
    check(Files.exists(ascii) && Files.size(ascii) > 0, "La table ASCII doit être créée et non vide");

        List<String> lines = Files.readAllLines(csv);
    check(!lines.isEmpty(), "Le CSV doit contenir un en-tête");
        if (!"variant,base_words,words,ratio,k_eff_bits_per_val,comp_median_ms,comp_iqr_ms,decomp_median_ms,decomp_iqr_ms".equals(lines.get(0))) {
            throw new IllegalStateException("En-tête CSV inattendu: " + lines.get(0));
        }
        String all = Files.readString(ascii);
    check(all.contains("Variant") && all.contains("words") && all.contains("base_words") && all.contains("k_eff"), "La table ASCII doit contenir les en-têtes");
    check(all.contains("CROSSING"), "La table ASCII doit contenir la ligne CROSSING");
    check(all.contains("NO_CROSSING"), "La table ASCII doit contenir la ligne NO_CROSSING");
    check(all.contains("OVERFLOW"), "La table ASCII doit contenir la ligne OVERFLOW");
    }

    public static void testBinaryInputAlsoWorks() throws Exception {
    // Crée un fichier binaire simple au format DataIO: [int count][values...]
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
    check(Files.exists(csv) && Files.size(csv) > 0, "Le CSV doit aussi être créé pour l'entrée binaire");
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new IllegalStateException(msg);
    }

    public static void main(String[] args) throws Exception {
        // Exécution manuelle des deux scénarios pour un test local rapide
        testTextInputProducesCsvAndAscii();
        testBinaryInputAlsoWorks();
        System.out.println("BenchCLITest exécution manuelle: OK");
    }
}

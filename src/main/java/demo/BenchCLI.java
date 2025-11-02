package demo;

import io.compress.intpack.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CLI de benchmark avec support des fichiers d'entrée et sortie CSV/Table ASCII.
 *
 * Exemples d'usage (commandes conservées en anglais):
 *   - Données générées (taille 100000, 7 runs):
 *       java -cp target/classes demo.BenchCLI --n 100000 --runs 7
 *   - Depuis un fichier texte (ints séparés par espaces/virgule/point-virgule):
 *       java -cp target/classes demo.BenchCLI --file data/input.txt --runs 9 --csv data/out.csv
 *   - Depuis un fichier binaire DataIO (count + ints):
 *       java -cp target/classes demo.BenchCLI --file data/input.bin
 *
 * Options:
 *   --n N               : génère N entiers aléatoires (ignoré si --file présent)
 *   --file <path>       : charge les entiers depuis un fichier (auto-détection binaire vs texte)
 *   --runs R            : nombre de runs chronométrés par variante (défaut 7)
 *   --csv <path>        : écrit les résultats en CSV
 *   --table-out <path>  : écrit le tableau ASCII dans ce fichier
 *   --variants v1,v2    : sous-ensemble (CROSSING,NO_CROSSING,OVERFLOW)
 */
public final class BenchCLI {

    private record Stats(double medianMs, double iqrMs) {}

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = parseArgs(args);
        int runs = parseInt(opts.getOrDefault("--runs", "7"), 7);
        int n = parseInt(opts.getOrDefault("--n", "100000"), 100_000);
        String file = opts.get("--file");
        String csvOut = opts.get("--csv");
        String tableOut = opts.get("--table-out");
        Set<CompressionType> variants = parseVariants(opts.get("--variants"));

        int[] data = (file != null) ? loadIntArrayAuto(Path.of(file)) : generateData(n);
        System.out.println("Input length: " + data.length);

        List<Row> rows = new ArrayList<>();
        for (CompressionType type : variants) {
            Row r = benchVariant(type, data, runs);
            rows.add(r);
        }

    // Légende (descriptions en français ; les noms de colonnes restent tels quels)
    String legend = "Légende: base_words = n (taille originale), words = mots 32 bits compressés, "
        + "ratio = words/base_words, k_eff(bits/val) = 32*words/base_words, IQR = Q3−Q1 (ms)";
        System.out.println(legend);
        printAsciiTable(rows, System.out);
        if (csvOut != null) writeCsv(rows, Path.of(csvOut));
        if (tableOut != null) try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Path.of(tableOut)))) {
            pw.println(legend);
            printAsciiTable(rows, pw);
        }
    }

    private static final class Row {
        final CompressionType type;
        final int baseWords; // taille originale (n)
        final int words;     // taille compressée (en mots int32)
        final Stats comp;    // stats temps de compression (ms)
        final Stats decomp;  // stats temps de décompression (ms)
        Row(CompressionType t, int baseWords, int words, Stats c, Stats d){
            this.type=t; this.baseWords=baseWords; this.words=words; this.comp=c; this.decomp=d;
        }
    }

    private static Row benchVariant(CompressionType type, int[] data, int runs) {
        CompressorFactory.Options opts = new CompressorFactory.Options();
        IntCompressor c = CompressorFactory.create(type, opts);

        // warmup
        for (int i = 0; i < Math.min(3, Math.max(1, runs/3)); i++) {
            int[] comp = c.compress(data);
            int[] out = new int[data.length];
            c.decompress(comp, out);
        }

        List<Double> compMs = new ArrayList<>();
        List<Double> decompMs = new ArrayList<>();
    int words = -1;
        for (int i = 0; i < runs; i++) {
            long t0 = System.nanoTime();
            int[] comp = c.compress(data);
            long t1 = System.nanoTime();
            if (words < 0) words = comp.length;
            long t2 = System.nanoTime();
            int[] out = new int[data.length];
            c.decompress(comp, out);
            long t3 = System.nanoTime();
            // quick check on first iteration
            if (i == 0 && !Arrays.equals(data, out)) {
                throw new IllegalStateException("Roundtrip failed for variant " + type);
            }
            compMs.add((t1 - t0) / 1e6);
            decompMs.add((t3 - t2) / 1e6);
        }
        return new Row(type, data.length, words, summarize(compMs), summarize(decompMs));
    }

    private static Stats summarize(List<Double> values) {
        Collections.sort(values);
        double median = percentile(values, 50);
        double q1 = percentile(values, 25);
        double q3 = percentile(values, 75);
        return new Stats(median, q3 - q1);
    }

    private static double percentile(List<Double> v, double p) {
        if (v.isEmpty()) return Double.NaN;
        double idx = (p / 100.0) * (v.size() - 1);
        int lo = (int) Math.floor(idx);
        int hi = (int) Math.ceil(idx);
        if (lo == hi) return v.get(lo);
        double w = idx - lo;
        return v.get(lo) * (1 - w) + v.get(hi) * w;
    }

    private static void printAsciiTable(List<Row> rows, Appendable out) throws IOException {
        String[] headers = {"Variant", "base_words", "words", "ratio", "k_eff(bits/val)", "comp_med(ms)", "comp_IQR", "decomp_med(ms)", "decomp_IQR"};
        List<String[]> lines = new ArrayList<>();
        lines.add(headers);
        for (Row r : rows) {
            double ratio = (r.baseWords == 0) ? Double.NaN : ((double) r.words) / r.baseWords;
            double kEff = (r.baseWords == 0) ? Double.NaN : 32.0 * r.words / r.baseWords;
            lines.add(new String[]{
                r.type.name(),
                String.valueOf(r.baseWords),
                String.valueOf(r.words),
                fmt(ratio),
                fmt(kEff),
                fmt(r.comp.medianMs()), fmt(r.comp.iqrMs()),
                fmt(r.decomp.medianMs()), fmt(r.decomp.iqrMs())
            });
        }
        int[] w = new int[headers.length];
        for (String[] row : lines) for (int i = 0; i < row.length; i++) w[i] = Math.max(w[i], row[i].length());
        String sep = sep(w);
        out.append(sep);
        for (int i = 0; i < lines.size(); i++) {
            String[] row = lines.get(i);
            out.append("| ");
            for (int c = 0; c < row.length; c++) {
                out.append(pad(row[c], w[c])).append(" | ");
            }
            out.append('\n');
            if (i == 0) out.append(sep);
        }
        out.append(sep);
    }

    private static String fmt(double v) { return String.format(Locale.ROOT, "%.3f", v); }
    private static String pad(String s, int w) { return String.format(Locale.ROOT, "%-"+w+"s", s); }
    private static String sep(int[] w) {
        StringBuilder sb = new StringBuilder();
        sb.append('+');
        for (int x : w) { sb.append("-").append("-".repeat(x)).append("-").append('+'); }
        sb.append('\n');
        return sb.toString();
    }

    private static void writeCsv(List<Row> rows, Path out) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            w.write("variant,base_words,words,ratio,k_eff_bits_per_val,comp_median_ms,comp_iqr_ms,decomp_median_ms,decomp_iqr_ms\n");
            for (Row r : rows) {
                double ratio = (r.baseWords == 0) ? Double.NaN : ((double) r.words) / r.baseWords;
                double kEff = (r.baseWords == 0) ? Double.NaN : 32.0 * r.words / r.baseWords;
                w.write(String.join(",",
                        r.type.name(),
                        Integer.toString(r.baseWords),
                        Integer.toString(r.words),
                        fmt(ratio),
                        fmt(kEff),
                        fmt(r.comp.medianMs()),
                        fmt(r.comp.iqrMs()),
                        fmt(r.decomp.medianMs()),
                        fmt(r.decomp.iqrMs())));
                w.write("\n");
            }
        }
    }

    private static int[] generateData(int n) {
        int[] data = new int[n];
        Random rnd = new Random(42);
        for (int i = 0; i < n; i++) {
            int r = rnd.nextInt(1000);
            data[i] = (r < 990) ? rnd.nextInt(128) : (1 << (10 + rnd.nextInt(10)));
        }
        return data;
    }

    private static int[] loadIntArrayAuto(Path path) throws IOException {
        // Try DataIO binary first
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            dis.mark(12);
            int n = dis.readInt();
            if (n >= 0 && n < (1<<28)) { // plausible length
                int[] arr = new int[n];
                for (int i = 0; i < n; i++) arr[i] = dis.readInt();
                return arr;
            }
        } catch (Exception ignore) {
            // fall through to text parsing
        }
        // Text/CSV parsing: accept numbers separated by whitespace or , ;
        String content = Files.readString(path);
        List<Integer> values = new ArrayList<>();
        Matcher m = Pattern.compile("-?\\d+").matcher(content);
        while (m.find()) values.add(Integer.parseInt(m.group()));
        int[] arr = new int[values.size()];
        for (int i = 0; i < values.size(); i++) arr[i] = values.get(i);
        return arr;
    }

    private static Map<String,String> parseArgs(String[] args) {
        Map<String,String> m = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                if (i+1 < args.length && !args[i+1].startsWith("--")) { m.put(a, args[++i]); }
                else { m.put(a, "true"); }
            }
        }
        return m;
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static Set<CompressionType> parseVariants(String s) {
        if (s == null || s.isBlank()) return EnumSet.allOf(CompressionType.class);
        Set<CompressionType> set = EnumSet.noneOf(CompressionType.class);
        for (String t : s.split(",")) {
            t = t.trim();
            if (!t.isEmpty()) set.add(CompressionType.valueOf(t));
        }
        return set.isEmpty() ? EnumSet.allOf(CompressionType.class) : set;
    }
}

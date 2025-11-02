package demo;

import io.compress.intpack.*;
import java.io.File;

/**
 * CLI simple pour compresser/décompresser des tableaux d'entiers sur disque.
 *
 * Utilisation:
 *  java -cp target/classes demo.SaveExample compress <fichier-entree> <fichier-sortie> [CROSSING|NO_CROSSING|OVERFLOW]
 *  java -cp target/classes demo.SaveExample decompress <fichier-entree> <fichier-sortie>
 *  java -cp target/classes demo.SaveExample roundtrip <fichier-entree> <dossier-sortie> [CROSSING|...]
 */
public class SaveExample {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Utilisation: compress|decompress|roundtrip ...");
            return;
        }

        String cmd = args[0];
        if ("compress".equalsIgnoreCase(cmd)) {
            File in = new File(args[1]);
            File out = new File(args[2]);
            CompressionType type = args.length >= 4 ? CompressionType.valueOf(args[3]) : CompressionType.NO_CROSSING;

            int[] data = loadIntsFlexible(in);
            IntCompressor c = CompressorFactory.create(type, new CompressorFactory.Options());
            int[] comp = c.compress(data);
            DataIO.saveCompressed(out, type, data.length, comp);
            System.out.println("Wrote compressed file: " + out.getAbsolutePath() + " (orig=" + data.length + ", comp=" + comp.length + ")");

        } else if ("decompress".equalsIgnoreCase(cmd)) {
            File in = new File(args[1]);
            File out = new File(args[2]);
            DataIO.Compressed cpl = DataIO.loadCompressed(in);
            CompressionType type = CompressionType.values()[cpl.compressorOrdinal];
            IntCompressor c = CompressorFactory.create(type, new CompressorFactory.Options());
            int[] outArr = new int[cpl.origLength];
            c.decompress(cpl.data, outArr);
            DataIO.saveIntArray(out, outArr);
            System.out.println("Wrote decompressed ints to: " + out.getAbsolutePath());

        } else if ("roundtrip".equalsIgnoreCase(cmd)) {
            File in = new File(args[1]);
            File outDir = new File(args[2]);
            if (!outDir.exists()) outDir.mkdirs();
            CompressionType type = args.length >= 4 ? CompressionType.valueOf(args[3]) : CompressionType.NO_CROSSING;

            int[] data = loadIntsFlexible(in);
            IntCompressor c = CompressorFactory.create(type, new CompressorFactory.Options());
            int[] comp = c.compress(data);
            File compFile = new File(outDir, "comp-" + type + ".bin");
            File origFile = new File(outDir, "orig.bin");
            DataIO.saveCompressed(compFile, type, data.length, comp);
            DataIO.saveIntArray(origFile, data);
            System.out.println("Saved orig -> " + origFile.getAbsolutePath() + ", comp -> " + compFile.getAbsolutePath());

            // Vérification
            DataIO.Compressed read = DataIO.loadCompressed(compFile);
            IntCompressor c2 = CompressorFactory.create(CompressionType.values()[read.compressorOrdinal], new CompressorFactory.Options());
            int[] outArr = new int[read.origLength];
            c2.decompress(read.data, outArr);
            System.out.println("roundtrip ok=" + java.util.Arrays.equals(data, outArr));

        } else {
            System.out.println("Unknown command: " + cmd);
        }
    }

    private static int[] loadIntsFlexible(File f) throws Exception {
        String name = f.getName().toLowerCase();
        if (name.endsWith(".bin")) return DataIO.loadIntArray(f);
        // otherwise treat as text file with one integer per line
        return DataIO.loadIntArrayFromText(f);
    }
}

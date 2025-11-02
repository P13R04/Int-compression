package io.compress.intpack;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Petit utilitaire pour lire/écrire des tableaux d'int et des buffers compressés.
 *
 * Formats binaires (Java Data{In|Out}putStream, big‑endian):
 * - tableau d'int "plain": [int count][int v0]...[int v(count-1)]
 * - fichier compressé: [int compressorOrdinal][int origLength][int compLength][int c0]...[int cN]
 */
public final class DataIO {
    private DataIO() {}

    public static void saveIntArray(File out, int[] arr) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(out)))) {
            dos.writeInt(arr.length);
            for (int v : arr) dos.writeInt(v);
        }
    }

    public static int[] loadIntArray(File in) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(in)))) {
            int n = dis.readInt();
            int[] arr = new int[n];
            for (int i = 0; i < n; i++) arr[i] = dis.readInt();
            return arr;
        }
    }

    public static int[] loadIntArrayFromText(File in) throws IOException {
        List<Integer> list = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(in))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                list.add(Integer.parseInt(line));
            }
        }
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    public static class Compressed {
        public final int compressorOrdinal;
        public final int origLength;
        public final int[] data;

        public Compressed(int compressorOrdinal, int origLength, int[] data) {
            this.compressorOrdinal = compressorOrdinal;
            this.origLength = origLength;
            this.data = data;
        }
    }

    public static void saveCompressed(File out, CompressionType type, int origLength, int[] comp) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(out)))) {
            dos.writeInt(type.ordinal());
            dos.writeInt(origLength);
            dos.writeInt(comp.length);
            for (int v : comp) dos.writeInt(v);
        }
    }

    public static Compressed loadCompressed(File in) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(in)))) {
            int ord = dis.readInt();
            int origLen = dis.readInt();
            int n = dis.readInt();
            int[] comp = new int[n];
            for (int i = 0; i < n; i++) comp[i] = dis.readInt();
            return new Compressed(ord, origLen, comp);
        }
    }
}

# BitPacking Project

This project implements several integer bit-packing compressors (crossing, no-crossing, overflow).

Build & test (Windows PowerShell):

```powershell
# set JAVA_HOME for your session (adjust path to your JDK install)
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
& "$PWD\mvnw.cmd" -DskipTests clean compile
& "$PWD\mvnw.cmd" test
```

Run example main:

```powershell
java -cp target/classes demo.Main
```

Notes:
- Sources are in `src/main/java` and tests in `src/test/java` (standard Maven layout).
- Build output is under `target/`.
- The project is configured to compile with `--release 21` (see `pom.xml`). Use JDK 21 or newer.
# Projet Bit Packing — Compression d'entiers

Ce dépôt contient une petite bibliothèque Java implémentant plusieurs variantes de "bit packing" pour compresser des tableaux d'entiers tout en conservant l'accès direct à l'élément i (méthode `get`).

Variantes implémentées
- `CROSSING` : valeurs empaquetées bit-à-bit, peuvent chevaucher deux mots int.
- `NO_CROSSING` : valeurs contenues entièrement dans un mot (alignement intra-mot).
- `OVERFLOW` : zone principale avec payload + bit drapeau, et zone d'overflow pour les valeurs trop grandes (ou négatives).

Fichiers importants
- `demo/Main.java` : démonstration minimale des trois variantes.
- `demo/TestSuite.java` : petit runner de tests (pas de dépendance externe). Exécute des cas déterministes et aléatoires.
- `demo/Benchmark.java` : bench simple (warmup + mesure d'une passe) pour chaque variante.
- `io/compress/intpack/` : implémentations et utilitaires (BitIO, Headers, BitPacking*).

Compilation et exécution (Windows PowerShell)

```powershell
Set-Location -Path 'C:\Users\Pierre Constantin\Desktop\projet soft engineering'
javac -d out demo\Main.java demo\TestSuite.java demo\Benchmark.java io\compress\intpack\*.java
java -cp out demo.Main
java -cp out demo.TestSuite
java -cp out demo.Benchmark 1000000  # 1M éléments (optionnel)
```

Notes et suite possible
- Les tests sont un runner simple (sans JUnit) pour garder la configuration minimale.
- Le benchmark est volontairement simple; pour des mesures précises, il faut :
  - faire plusieurs répétitions et prendre la médiane
  - isoler le GC et exécuter sur JVM avec option `-Xms -Xmx` contrôlées
  - utiliser `System.nanoTime()` (déjà utilisé) et plusieurs passes
- Améliorations possibles : encodage des entiers signés (zig-zag), tests JUnit, rapport PDF et scripts d'automation.

Auteur: (ajoutez votre nom ici)

Persisting input and compressed data
-----------------------------------

By default the benchmark keeps the original arrays and the compressed buffers only in memory. To save them for later inspection or reuse, you can store the integer arrays (both raw inputs and compressed buffers) as a compact binary file.

Recommended layout (binary int32, big-endian as produced by Java DataOutputStream):
- file = [int count][int v0][int v1]...[int v(count-1)]

Quick Java helper (example API) you can add to your project as `DataIO.java`:

```java
import java.io.*;

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
}
```

Usage examples (PowerShell)

```powershell
# create a folder for persisted data
mkdir data

# run a small demo that writes input and compressed output (adjust paths if you add DataIO usage to demo)
# java -cp target/classes demo.Main  # if modified to save files

# Example (after adding DataIO calls in a small helper):
# java -cp target/classes demo.SaveExample data/input-200k.bin data/comp-overflow.bin
```

Notes
- Files saved in `data/` are ignored by the repository (see `.gitignore`).
- The binary int format is compact and fast to read/write from Java; if you need human-readable files, export CSV instead.
- If you want, I can add `DataIO.java` and a small `demo/SaveExample.java` program that runs the benchmark, saves the input and each compressed buffer under `data/`, and verifies roundtrip on load.

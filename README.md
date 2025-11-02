# Projet BitPacking

Ce projet implémente plusieurs compresseurs d'entiers par bit‑packing (crossing, no‑crossing, overflow).

Build & tests (Windows PowerShell)

```powershell
# Optional: set JAVA_HOME for your session
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'

# Build and run tests
& "$PWD\mvnw.cmd" -DskipTests=false clean test
```

Exécuter la démo interactive (demo.Main)
------------------------------------

`demo.Main` est une petite interface interactive qui affiche une bannière d’auteur (Pierre CONSTANTIN), permet de choisir un tableau d’entrée, exécute toutes les variantes de compresseur, affiche les vérifications de round‑trip et un exemple de `get(i)`, puis attend Entrée avant de quitter.

Exécution en une commande (multi‑plateforme via Maven Wrapper) :

```powershell
# Windows
./mvnw.cmd -q -DskipTests=true compile exec:java@main

# macOS/Linux
./mvnw -q -DskipTests=true compile exec:java@main
```

À quoi s’attendre :
- Saisie soit de `random N` (ex. `random 20000`), soit d’entiers séparés par des espaces.
- Saisie d’un index `i` pour valider l’accès aléatoire via `get(i)`.
- Un récapitulatif par variante : tailles (base_words, words, ratio, k_eff) et temps (comp_med/decomp_med et IQR).
- “Press Enter to exit …” en fin d’exécution.

Notes
- Les sources sont dans `src/main/java` et les tests dans `src/test/java` (standard Maven).
- Les artefacts de build sont sous `target/`.
- Le projet compile avec `--release 21` (voir `pom.xml`). Utiliser JDK 21.

Bench depuis des fichiers (CSV/TXT/BIN)
------------------------------------

Utilisez `demo.BenchCLI` pour lancer des benchmarks avec des jeux de données fournis par fichier.

Formats pris en charge :
- Tableau d’int binaire (format `DataIO`) : `[int count][int v0]..[int v(n-1)]`
- Texte/CSV : entiers séparés par espaces/virgules/points‑virgules (un ou plusieurs par ligne)

Exécution en une commande (compile auto) :

```powershell
# Windows
./mvnw.cmd -q -DskipTests=true compile exec:java@benchcli -Dexec.args="--n 100000 --runs 7"

# macOS/Linux
./mvnw -q -DskipTests=true compile exec:java@benchcli -Dexec.args="--n 100000 --runs 7"

# text/CSV input; write CSV results and ASCII table files
./mvnw[.cmd] -q -DskipTests=true compile exec:java@benchcli -Dexec.args="--file data/input.csv --runs 9 --csv data/results.csv --table-out data/results.txt"

# DataIO binary input
./mvnw[.cmd] -q -DskipTests=true compile exec:java@benchcli -Dexec.args="--file data/input.bin --runs 9"
```

Colonnes de sortie (ASCII et CSV) :
- `base_words` : nombre d'entiers de l'entrée (n)
- `words` : nombre de mots 32 bits dans le buffer compressé
- `ratio` : words / base_words (plus c'est petit, mieux c'est)
- `k_eff` : 32 × ratio (bits utiles par valeur après en‑tête)
- `comp_med(ms)` / `decomp_med(ms)` : médianes des latences sur les runs
- `comp_IQR` / `decomp_IQR` : étendue interquartile (Q3−Q1), en ms

Légende
- Les colonnes `ratio` et `k_eff` mesurent la compacité; les colonnes `*_med` et `*_IQR` mesurent le temps.
- L’IQR est robuste au bruit (latences système, JIT) et complète la médiane.

Fichiers clés
- `src/main/java/demo/Main.java` : démo interactive des trois variantes.
- `src/main/java/demo/Benchmark.java` : micro‑bench simple par variante.
- `src/main/java/demo/BenchCLI.java` : CLI de bench à partir de fichiers (tableau ASCII + CSV).
- `src/main/java/io/compress/intpack/` : implémentations et utilitaires (BitIO, Headers, BitPacking*).

Guide de lecture du code
------------------------

- Vue d’ensemble (API et contrat)
  - `io/compress/intpack/IntCompressor.java` : contrat minimal commun (`compress`, `decompress`, `get`).
  - `io/compress/intpack/Headers.java` : entête auto‑décrivant (magic, n, mode, k, extras), toujours au début du buffer.
  - `io/compress/intpack/CompressorFactory.java` (+ `Options`) : fabrique qui instancie la variante demandée.

- Variantes (logique de compression)
  - `BitPackingCrossing.java` : indices bit‑à‑bit (i*k), les valeurs peuvent chevaucher deux words.
  - `BitPackingNoCrossing.java` : pas de chevauchement; `per = floor(32/k)` valeurs par word.
  - `BitPackingOverflow.java` : flag + payload + zone overflow; choisit un `kSmall` qui minimise les bits totaux.

- I/O et utilitaires bas niveau
  - `BitIO.java` : lecture/écriture de plages de bits LSB‑first (et helpers in‑word / MSB facultatifs).
  - `DataIO.java` : lecture/écriture de tableaux d’int et buffers compressés (formats binaires simples).

- Démos/CLI et tests
  - `demo/Main.java` : interface interactive pour essayer rapidement les variantes (saisie, `get(i)`, timings).
  - `demo/BenchCLI.java` : bench reproductible depuis fichiers (TXT/CSV/BIN) avec médiane/IQR, ASCII + CSV.
  - `demo/Benchmark.java` : micro‑bench minimal par variante.
  - `demo/SaveExample.java` : round‑trip de sauvegarde/lecture sur disque (format `DataIO`).
  - `src/test/java/demo` : tests JUnit et un runner autonome `TestSuite.java`.

- Ordre de lecture recommandé
  1) `IntCompressor` → `Headers` → `CompressorFactory` (contrat et format).
  2) `BitIO` (primitives bit‑à‑bit utilisées partout).
  3) Variantes: `BitPackingNoCrossing` → `BitPackingCrossing` → `BitPackingOverflow`.
  4) Démos/CLI (`Main`, `BenchCLI`, `Benchmark`, `SaveExample`).
  5) Tests (`src/test/java/demo`).

- Points d’attention
  - Convention LSB‑first (bit 0 = LSB du word 0). `get(i)` est en O(1) pour toutes les variantes.
  - `NO_CROSSING` évite les chevauchements au prix de trous potentiels en fin de word.
  - `OVERFLOW` supporte valeurs négatives et très grandes via une zone dédiée (indexée depuis le payload).
  - Pour des entiers signés sans `OVERFLOW`, envisager un encodage ZigZag en amont.

Autres exécutions en une commande
----------------------

```powershell
# Benchmark (argument: N)
./mvnw[.cmd] -q -DskipTests=true compile exec:java@benchmark -Dexec.args="1000000"

# Save/load roundtrip example using DataIO
./mvnw[.cmd] -q -DskipTests=true compile exec:java@saveexample -Dexec.args="data/input-200k.bin data/comp-overflow.bin"
```

Persistance des entrées et des buffers compressés
-----------------------------------

Par défaut, le benchmark garde en mémoire les tableaux d’origine et les buffers compressés. Pour les réutiliser ou les inspecter plus tard, vous pouvez stocker les tableaux d’entiers (entrées brutes et buffers compressés) dans un fichier binaire compact.

Format recommandé (int32 binaire, big‑endian tel que produit par Java DataOutputStream) :
- file = [int count][int v0][int v1]...[int v(count-1)]

Petit utilitaire Java (API d’exemple) à ajouter au projet sous `DataIO.java` :

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

Exemples d’utilisation (PowerShell)

```powershell
# create a folder for persisted data
mkdir data

# run a small demo that writes input and compressed output
# java -cp target/classes demo.Main  # only if you add file-saving in Main

# Example (after adding DataIO calls in a small helper):
# java -cp target/classes demo.SaveExample data/input-200k.bin data/comp-overflow.bin
```

Notes
- Les fichiers enregistrés dans `data/` sont ignorés par le dépôt (voir `.gitignore`).
- Le format binaire int est compact et rapide en Java ; si vous avez besoin de lisible, exportez en CSV.
- Un helper `demo/SaveExample.java` est disponible pour démontrer la sauvegarde/lecture round‑trip via `DataIO`.

Dépannage VS Code (Java)
------------------------------

Si vous voyez une erreur comme “The declared package 'demo' does not match the expected package 'src.main.java.demo'”, cela signifie que le Java Language Server ne reconnaît pas les répertoires source Maven.

Ce dépôt inclut `.vscode/settings.json` avec :
- `java.project.sourcePaths` : `["src/main/java", "src/test/java"]`
- `java.configuration.updateBuildConfiguration` : `automatic`
- `java.import.maven.enabled` : `true`

Étapes pour corriger dans VS Code :
1) Reload Window : Command Palette → “Developer: Reload Window”.
2) Clean Java workspace : Command Palette → “Java: Clean Java Language Server Workspace” (accepter le redémarrage).
3) Vérifier JDK 21 : Command Palette → “Java: Configure Runtime” → sélectionner JDK 21 pour l’espace de travail.
4) Recharger les projets Maven (Explorateur → panneau Maven → “Reload Projects”).
5) S’assurer que le Maven Wrapper est utilisé sous Windows : la clé `maven.executable.path` pointe vers `${workspaceFolder}/mvnw.cmd`.

Après ces étapes, le soulignement rouge sur `package demo;` doit disparaître. Les builds/tests Maven restent la source de vérité.

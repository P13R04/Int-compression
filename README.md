# Projet BitPacking

Compression d'entiers par bit‑packing avec accès direct aux valeurs (CROSSING, NO_CROSSING, OVERFLOW). Le projet se construit et s'exécute avec le Maven Wrapper et un JDK 21, sans autre dépendance.

Build & tests (Windows PowerShell)

```powershell
# Facultatif : définir JAVA_HOME pour la session
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'

# Construire et exécuter les tests
& "$PWD\mvnw.cmd" -DskipTests=false clean test
```

Build & tests (macOS/Linux)

```bash
# Si ce dépôt a été créé sous Windows, vous devrez peut-être rendre le wrapper exécutable une fois :
chmod +x ./mvnw

# Construire et exécuter les tests
./mvnw -DskipTests=false clean test
```

Exécuter la démo interactive (demo.Main)
-------------------------------------

`demo.Main` est une petite interface interactive qui affiche une bannière d’auteur (Pierre CONSTANTIN), permet de choisir un tableau d’entrée, exécute toutes les variantes de compresseur, affiche les vérifications de round‑trip et un exemple de `get(i)`, puis attend Entrée avant de quitter.

Exécution en une commande (multi‑plateforme via Maven Wrapper) :

```powershell
# Windows
./mvnw.cmd -q -DskipTests=true compile exec:java@main

# macOS/Linux
./mvnw -q -DskipTests=true compile exec:java@main
```

À quoi s’attendre :
- Entrer `random N` (ex. `random 20000`) ou une liste d’entiers séparés par des espaces.
- Entrer un index `i` pour tester `get(i)` (accès direct).
- Résumé par variante: tailles (base_words, words, ratio, k_eff) et temps (médiane + IQR).

Guide d'exécution rapide (copier-coller)
---------------------------------------

Démo interactive (choix simple)

```bash
# macOS/Linux
./mvnw -q -DskipTests=true compile exec:java@main
# Quand le terminal affiche "Votre saisie:", tapez par exemple:
#   random 10000
# puis Entrée quand l'indice i est proposé (ou tapez un nombre),
# lisez le récapitulatif, puis appuyez sur Entrée pour quitter.
```

```powershell
# Windows (PowerShell)
./mvnw.cmd -q -DskipTests=true compile exec:java@main
# Au prompt "Votre saisie:", entrez par exemple:
#   random 10000
# puis Entrée quand l'indice i est proposé (ou tapez un nombre),
# lisez le récapitulatif, puis appuyez sur Entrée pour quitter.
```

Démo interactive (non‑interactif, exemple automatique)

```bash
# macOS/Linux: injecte une saisie par défaut (100 valeurs aléatoires) + Entrées
printf "random 100\n\n\n" | ./mvnw -q -DskipTests=true compile exec:java@main
```

Benchmark depuis données générées

```bash
# macOS/Linux
./mvnw -q -DskipTests=true compile exec:java@benchcli -Dexec.args="--n 100000 --runs 5"
```

```powershell
# Windows (PowerShell)
./mvnw.cmd -q -DskipTests=true compile exec:java@benchcli -Dexec.args="--n 100000 --runs 5"
```

Benchmark depuis un fichier texte rapide

```bash
# macOS/Linux
mkdir -p data && echo "1 2 3 4 5 6 7 8 9 10" > data/input.txt
./mvnw -q -DskipTests=true compile exec:java@benchcli -Dexec.args="--file data/input.txt --runs 3 --csv data/out.csv --table-out data/out.txt"
# Consultez ensuite data/out.csv et data/out.txt
```

```powershell
# Windows (PowerShell)
mkdir data
"1 2 3 4 5 6 7 8 9 10" | Out-File -FilePath data/input.txt -Encoding utf8
./mvnw.cmd -q -DskipTests=true compile exec:java@benchcli -Dexec.args="--file data/input.txt --runs 3 --csv data/out.csv --table-out data/out.txt"
# Ouvrez data/out.csv et data/out.txt
```

Micro‑benchmark simple

```bash
# macOS/Linux (N=500000)
./mvnw -q -DskipTests=true compile exec:java@benchmark -Dexec.args="500000"
```

```powershell
# Windows (PowerShell)
./mvnw.cmd -q -DskipTests=true compile exec:java@benchmark -Dexec.args="500000"
```

Notes
- Les sources sont dans `src/main/java` et les tests dans `src/test/java` (standard Maven).
- Les artefacts de build sont sous `target/`.
- Le projet compile avec `--release 21` (voir `pom.xml`). Utiliser JDK 21.

Benchmark depuis des fichiers (CSV/TXT/BIN)
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

# Entrée texte/CSV ; écrit les résultats CSV et un tableau ASCII dans des fichiers
./mvnw[.cmd] -q -DskipTests=true compile exec:java@benchcli -Dexec.args="--file data/input.csv --runs 9 --csv data/results.csv --table-out data/results.txt"

# Entrée binaire DataIO
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
- `src/main/java/demo/Benchmark.java` : micro‑benchmark simple par variante.
- `src/main/java/demo/BenchCLI.java` : CLI de benchmark à partir de fichiers (tableau ASCII + CSV).
- `src/main/java/io/compress/intpack/` : implémentations et utilitaires (BitIO, Headers, BitPacking*).

Guide de lecture du code
-------------------------

- Vue d’ensemble (API et contrat)
  - `io/compress/intpack/IntCompressor.java` : contrat minimal commun (`compress`, `decompress`, `get`).
  - `io/compress/intpack/Headers.java` : entête auto‑décrivant (magic, n, mode, k, extras), toujours au début du buffer.
  - `io/compress/intpack/CompressorFactory.java` (+ `Options`) : fabrique qui instancie la variante demandée.

- Variantes (logique de compression)
  - `BitPackingCrossing.java` : indices bit‑à‑bit (i*k), les valeurs peuvent chevaucher deux words.
  - `BitPackingNoCrossing.java` : pas de chevauchement; `per = floor(32/k)` valeurs par word.
  - `BitPackingOverflow.java` : flag + payload + zone overflow; choisit un `kSmall` qui minimise les bits totaux.

- I/O et utilitaires bas niveau
  - `BitIO.java` : primitives de lecture/écriture de bits (LSB‑first).
  - `DataIO.java` : lecture/écriture de tableaux d’int et buffers compressés (formats binaires simples).

- Démos/CLI et tests
  - `demo/Main.java` : interface interactive pour essayer rapidement les variantes (saisie, `get(i)`, timings).
  - `demo/BenchCLI.java` : benchmark reproductible depuis fichiers (TXT/CSV/BIN) avec médiane/IQR, ASCII + CSV.
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
# Benchmark (argument : N)
./mvnw[.cmd] -q -DskipTests=true compile exec:java@benchmark -Dexec.args="1000000"

# Exemple de sauvegarde/chargement aller‑retour avec DataIO
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
# créer un dossier pour les données persistées
mkdir data

# exécuter une petite démo qui écrit l'entrée et la sortie compressée
# java -cp target/classes demo.Main  # uniquement si vous ajoutez l'enregistrement de fichier dans Main

# Exemple (après avoir ajouté des appels DataIO dans un petit helper) :
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
5) Sous Windows, si besoin, vous pouvez définir `maven.executable.path` vers `${workspaceFolder}/mvnw.cmd` dans vos paramètres utilisateur VS Code.

Après ces étapes, le soulignement rouge sur `package demo;` doit disparaître. Les builds/tests Maven restent la source de vérité.

Notes macOS/JDK récents
-----------------------

- Si vous voyez des avertissements concernant des appels natifs restreints (jansi/hawtjni) lors de l'exécution de Maven avec un JDK très récent (21+), ils sont bénins. Optionnellement, vous pouvez les masquer en exportant:

```bash
export MAVEN_OPTS="--enable-native-access=ALL-UNNAMED"
```

- Si le script `mvnw` affiche `permission denied`, exécutez `chmod +x ./mvnw` une fois (voir ci-dessus).

DéPannage Windows (sans dépendances préinstallées)
-------------------------------------------------

- Installer un JDK 21 (x64). Vérifiez ensuite:

```powershell
java -version
# si nécessaire pour la session courante
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

- Utiliser le Maven Wrapper Windows (`mvnw.cmd`). Ne pas utiliser `mvnw` (Unix):

```powershell
# depuis PowerShell (gère les chemins avec espaces)
& "$PWD\mvnw.cmd" -DskipTests=false clean test

# alternative équivalente
./mvnw.cmd -DskipTests=false clean test
```

- Quoting des arguments pour les exécutions: les exemples donnés fonctionnent sous PowerShell et CMD.

- Encodage console (facultatif): si les accents (é/è/à) s'affichent mal, basculez en UTF-8:

```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null
```

- Premier lancement: le wrapper télécharge Maven automatiquement dans `~\.m2\wrapper`. Si vous êtes derrière un proxy, configurez-le dans `~/.m2/settings.xml`:

```xml
<settings>
  <proxies>
    <proxy>
      <id>corp</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>proxy.example.com</host>
      <port>8080</port>
      <!-- optional: username/password, nonProxyHosts -->
    </proxy>
  </proxies>
</settings>
```

- Avertissements JDK récents (jansi/hawtjni) lors de l'exécution de Maven: bénin. Pour les masquer dans PowerShell:

```powershell
$env:MAVEN_OPTS = '--enable-native-access=ALL-UNNAMED'
```

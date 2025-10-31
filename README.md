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

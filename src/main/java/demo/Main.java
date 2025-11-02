package demo;

// Remarque: ce Main évite toute importation de types du package io.compress.intpack
// afin de limiter les faux positifs de VS Code lorsque le projet n'est pas
// ouvert comme projet Maven. Il utilise la réflexion pour instancier et appeler
// les compresseurs, ce qui supprime les erreurs « cannot be resolved to a type »
// tout en conservant la même exécution via Maven/JDK.

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
	public static void main(String[] args) {
		// Bannière et auteur
		System.out.println("===============================================");
		System.out.println("  Bit Packing Demo — Interface interactive");
		System.out.println("  Auteur: Pierre CONSTANTIN");
		System.out.println("===============================================\n");

		// Aide rapide
		System.out.println("Entrez une liste d'entiers séparés par des virgules/espaces");
		System.out.println("Exemples: 0,1,2,3,15,31,1024  ou  10 20 30 40");
		System.out.println("Vous pouvez aussi taper:   random N   (ex: random 100)");
		System.out.print("\nVotre saisie: ");

		Scanner sc = new Scanner(System.in);
		String line = safeReadLine(sc);
		int[] data = parseInputOrRandom(line);
		if (data.length == 0) {
			// Valeurs par défaut si saisie vide/non reconnue
			data = new int[]{0,1,2,3,4,5,6,7,15,31,1024, 1<<20};
			System.out.println("(Entrée vide/non reconnue) → jeu d'exemple utilisé: " + Arrays.toString(data));
		} else {
			System.out.println("Taille du tableau: " + data.length);
		}

		boolean hasNegative = Arrays.stream(data).anyMatch(x -> x < 0);

		// Prépare les classes réflexives
		Class<?> factoryClass = forNameQuiet("io.compress.intpack.CompressorFactory");
		Class<?> optionsClass = forNameQuiet("io.compress.intpack.CompressorFactory$Options");
		Class<?> compressionTypeClass = forNameQuiet("io.compress.intpack.CompressionType");
		if (factoryClass == null || optionsClass == null || compressionTypeClass == null) {
			System.err.println("[Erreur] Impossible de charger les classes de compression. Assurez-vous de construire avec Maven.");
			waitBeforeExit();
			return;
		}
		Object opts;
		try {
			opts = optionsClass.getConstructor().newInstance();
		} catch (Exception e) {
			System.err.println("[Erreur] Création des options: " + e);
			waitBeforeExit();
			return;
		}

		// Choix d'un indice get(i) à tester
		int defaultIndex = Math.min(10, Math.max(0, data.length/2));
		System.out.print("\nIndice i pour tester get(i) [Entrée=" + defaultIndex + "]: ");
		String idxLine = safeReadLine(sc);
		int getIndex = defaultIndex;
		try {
			if (idxLine != null && !idxLine.isBlank()) {
				getIndex = Integer.parseInt(idxLine.trim());
			}
		} catch (NumberFormatException ignore) { /* garde default */ }
		getIndex = Math.max(0, Math.min(getIndex, Math.max(0, data.length-1)));

		// Exécution sur variantes
		System.out.println("\nExécution…\n");
		if (!hasNegative) {
			runVariantReflect("CROSSING", "CROSSING", factoryClass, optionsClass, compressionTypeClass, opts, data, getIndex);
			runVariantReflect("NO_CROSSING", "NO_CROSSING", factoryClass, optionsClass, compressionTypeClass, opts, data, getIndex);
		} else {
			System.out.println("(Valeurs négatives détectées → CROSSING/NO_CROSSING ignorées; utiliser OVERFLOW)");
		}
		runVariantReflect("OVERFLOW", "OVERFLOW", factoryClass, optionsClass, compressionTypeClass, opts, data, getIndex);

		// Attente avant fin
		System.out.print("\nAppuyez sur Entrée pour quitter… ");
		safeReadLine(sc);
	}

	private static void runVariantReflect(String label,
										  String enumName,
										  Class<?> factoryClass,
										  Class<?> optionsClass,
										  Class<?> compressionTypeClass,
										  Object opts,
										  int[] data,
										  int getIndex) {
		try {
			// CompressionType value
			@SuppressWarnings({"rawtypes","unchecked"})
			Object type = Enum.valueOf((Class) compressionTypeClass, enumName);

			// IntCompressor c = CompressorFactory.create(type, opts)
			Method mCreate = factoryClass.getMethod("create", compressionTypeClass, optionsClass);
			Object c = mCreate.invoke(null, type, opts);

			Method mCompress = c.getClass().getMethod("compress", int[].class);
			Method mDecompress = c.getClass().getMethod("decompress", int[].class, int[].class);
			Method mGet = c.getClass().getMethod("get", int[].class, int.class);

			long t0 = System.nanoTime();
			int[] comp = (int[]) mCompress.invoke(c, (Object) data);
			long t1 = System.nanoTime();
			int[] out = new int[data.length];
			long t2 = System.nanoTime();
			mDecompress.invoke(c, comp, out);
			long t3 = System.nanoTime();

			boolean ok = Arrays.equals(data, out);
			int gi = (data.length == 0) ? 0 : Math.min(getIndex, data.length - 1);
			int gv = (data.length == 0) ? 0 : ((Integer) mGet.invoke(c, comp, gi));
			System.out.printf(Locale.ROOT,
					"%s → ok=%s, words=%d, get(%d)=%d, C=%.3f ms, D=%.3f ms\n",
					label, ok, comp.length, gi, gv, (t1 - t0) / 1e6, (t3 - t2) / 1e6);
		} catch (Exception e) {
			System.err.println("[" + label + "] Erreur d'exécution: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private static String safeReadLine(Scanner sc) {
		try { return sc.nextLine(); } catch (NoSuchElementException e) { return ""; }
	}

	private static Class<?> forNameQuiet(String name) {
		try { return Class.forName(name); } catch (Throwable t) { return null; }
	}

	private static void waitBeforeExit() {
		System.out.print("\nAppuyez sur Entrée pour quitter… ");
		try { System.in.read(); } catch (Exception ignore) {}
	}

	private static int[] parseInputOrRandom(String line) {
		if (line == null) return new int[0];
		String s = line.trim();
		if (s.isEmpty()) return new int[0];
		// Cas "random N"
		Matcher mRand = Pattern.compile("(?i)random\\s+(\\d+)").matcher(s);
		if (mRand.find()) {
			int n = Integer.parseInt(mRand.group(1));
			return generateData(n);
		}
		// Parse entiers (whitespace, virgules, points-virgules)
		Matcher m = Pattern.compile("-?\\d+").matcher(s);
		List<Integer> vals = new ArrayList<>();
		while (m.find()) vals.add(Integer.parseInt(m.group()));
		int[] arr = new int[vals.size()];
		for (int i = 0; i < vals.size(); i++) arr[i] = vals.get(i);
		return arr;
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
}

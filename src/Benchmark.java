import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public final class Benchmark {

    private static final int[] PATTERN_COUNTS = {10, 100, 1000};
    private static final int[] TEXT_LENGTHS = {1000, 10000, 100000};
    private static final int RUNS = 5;
    private static final long SEED = 42L;

    private record Scenario(String name, String alphabet, int minLen, int maxLen) {}

    private static final Scenario[] SCENARIOS = {
            new Scenario("DNA-like (4 letters, length 6-10)", "acgt", 6, 10),
            new Scenario("Lowercase English (26 letters, length 4-8)", "abcdefghijklmnopqrstuvwxyz", 4, 8)
    };

    public static void main(String[] args) throws IOException {
        Random random = new Random(SEED);
        StringBuilder markdown = new StringBuilder();
        StringBuilder csv = new StringBuilder();
        csv.append("scenario,patterns,text_length,matches,ac_nodes,ac_build_ms,ac_search_ms,ac_total_ms,kmp_total_ms,speedup\n");

        for (Scenario scenario : SCENARIOS) {
            markdown.append("### ").append(scenario.name()).append("\n\n");
            markdown.append("| Patterns | Text length | Matches | Trie nodes | AC build (ms) | AC search (ms) | AC total (ms) | KMP x k (ms) | KMP / AC |\n");
            markdown.append("|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");

            for (int patternCount : PATTERN_COUNTS) {
                List<String> patterns = generatePatterns(random, scenario, patternCount);
                for (int textLength : TEXT_LENGTHS) {
                    String text = randomString(random, scenario.alphabet(), textLength);
                    Result r = measure(patterns, text);
                    double speedup = r.kmpTotalMs / r.acTotalMs;

                    markdown.append(String.format(Locale.ROOT,
                            "| %d | %d | %d | %d | %.4f | %.4f | %.4f | %.4f | %.2fx |%n",
                            patternCount, textLength, r.matches, r.nodes,
                            r.acBuildMs, r.acSearchMs, r.acTotalMs, r.kmpTotalMs, speedup));
                    csv.append(String.format(Locale.ROOT,
                            "\"%s\",%d,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.2f%n",
                            scenario.name(), patternCount, textLength, r.matches, r.nodes,
                            r.acBuildMs, r.acSearchMs, r.acTotalMs, r.kmpTotalMs, speedup));
                }
            }
            markdown.append("\n");
        }

        System.out.println(markdown);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get("results.md")))) {
            out.println("# Experimental results");
            out.println();
            out.println("Average of " + RUNS + " runs measured with System.nanoTime() (30 unmeasured warm-up iterations per case).");
            out.println();
            out.print(markdown);
        }
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get("results.csv")))) {
            out.print(csv);
        }
        System.out.println("Saved results.md and results.csv");
    }

    private static final class Result {
        double acBuildMs;
        double acSearchMs;
        double acTotalMs;
        double kmpTotalMs;
        long matches;
        int nodes;
    }

    private static Result measure(List<String> patterns, String text) {
        for (int w = 0; w < 30; w++) {
            runAhoCorasick(patterns, text);
            runKmp(patterns, text);
        }
        long buildSum = 0;
        long searchSum = 0;
        long kmpSum = 0;
        long acMatches = 0;
        long kmpMatches = 0;
        int nodes = 0;

        for (int run = 0; run < RUNS; run++) {
            long t0 = System.nanoTime();
            AhoCorasick ac = new AhoCorasick(patterns);
            long t1 = System.nanoTime();
            acMatches = ac.countMatches(text);
            long t2 = System.nanoTime();
            kmpMatches = runKmp(patterns, text);
            long t3 = System.nanoTime();

            buildSum += t1 - t0;
            searchSum += t2 - t1;
            kmpSum += t3 - t2;
            nodes = ac.nodeCount();
        }

        if (acMatches != kmpMatches) {
            throw new IllegalStateException("Aho-Corasick and KMP disagree: " + acMatches + " vs " + kmpMatches);
        }

        Result r = new Result();
        r.acBuildMs = buildSum / 1e6 / RUNS;
        r.acSearchMs = searchSum / 1e6 / RUNS;
        r.acTotalMs = r.acBuildMs + r.acSearchMs;
        r.kmpTotalMs = kmpSum / 1e6 / RUNS;
        r.matches = acMatches;
        r.nodes = nodes;
        return r;
    }

    private static long runAhoCorasick(List<String> patterns, String text) {
        return new AhoCorasick(patterns).countMatches(text);
    }

    private static long runKmp(List<String> patterns, String text) {
        long total = 0;
        for (String pattern : patterns) {
            total += KMP.count(text, pattern);
        }
        return total;
    }

    private static List<String> generatePatterns(Random random, Scenario scenario, int count) {
        Set<String> unique = new LinkedHashSet<>();
        while (unique.size() < count) {
            int length = scenario.minLen() + random.nextInt(scenario.maxLen() - scenario.minLen() + 1);
            unique.add(randomString(random, scenario.alphabet(), length));
        }
        return new ArrayList<>(unique);
    }

    private static String randomString(Random random, String alphabet, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
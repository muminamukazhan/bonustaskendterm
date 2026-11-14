import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class AhoCorasickTest {

    private static int passed = 0;

    public static void main(String[] args) {
        classicExample();
        overlappingAndNested();
        duplicatePatterns();
        noMatches();
        singleCharacters();
        positionsAreCorrect();
        nonAsciiTextIsHandled();
        invalidInput();
        randomAgainstKmp();
        System.out.println("All tests passed: " + passed);
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("FAILED: " + name);
        }
        passed++;
        System.out.println("PASS  " + name);
    }

    private static void classicExample() {
        AhoCorasick ac = new AhoCorasick(Arrays.asList("he", "she", "his", "hers"));
        long[] counts = ac.occurrenceCounts("ushers");
        check(Arrays.equals(counts, new long[]{1, 1, 0, 1}), "classic example: ushers");
        check(ac.countMatches("ahishers") == 4, "classic example: ahishers");
    }

    private static void overlappingAndNested() {
        AhoCorasick ac = new AhoCorasick(Arrays.asList("a", "aa", "aaa"));
        long[] counts = ac.occurrenceCounts("aaaa");
        check(Arrays.equals(counts, new long[]{4, 3, 2}), "nested patterns in aaaa");
    }

    private static void duplicatePatterns() {
        AhoCorasick ac = new AhoCorasick(Arrays.asList("ab", "ab", "b"));
        long[] counts = ac.occurrenceCounts("abab");
        check(Arrays.equals(counts, new long[]{2, 2, 2}), "duplicate patterns reported separately");
    }

    private static void noMatches() {
        AhoCorasick ac = new AhoCorasick(Arrays.asList("xyz", "qq"));
        check(ac.countMatches("abcabcabc") == 0, "no matches");
        check(ac.countMatches("") == 0, "empty text");
    }

    private static void singleCharacters() {
        AhoCorasick ac = new AhoCorasick(Arrays.asList("a", "b"));
        check(ac.countMatches("abcab") == 4, "single character patterns");
    }

    private static void positionsAreCorrect() {
        AhoCorasick ac = new AhoCorasick(Arrays.asList("abc", "bc", "c"));
        List<AhoCorasick.Match> matches = ac.findAll("xabcx");
        Set<AhoCorasick.Match> expected = new HashSet<>(Arrays.asList(
                new AhoCorasick.Match(0, 1, 4),
                new AhoCorasick.Match(1, 2, 4),
                new AhoCorasick.Match(2, 3, 4)));
        check(matches.size() == 3 && expected.equals(new HashSet<>(matches)), "match positions");
    }

    private static void nonAsciiTextIsHandled() {
        AhoCorasick ac = new AhoCorasick(Arrays.asList("ab"));
        check(ac.countMatches("ab\u0416ab") == 2, "non-ASCII text characters reset the state");
    }

    private static void invalidInput() {
        boolean emptyListRejected = false;
        boolean emptyPatternRejected = false;
        boolean nonAsciiRejected = false;
        try {
            new AhoCorasick(new ArrayList<>());
        } catch (IllegalArgumentException e) {
            emptyListRejected = true;
        }
        try {
            new AhoCorasick(Arrays.asList("ok", ""));
        } catch (IllegalArgumentException e) {
            emptyPatternRejected = true;
        }
        try {
            new AhoCorasick(Arrays.asList("\u0416"));
        } catch (IllegalArgumentException e) {
            nonAsciiRejected = true;
        }
        check(emptyListRejected, "empty pattern list rejected");
        check(emptyPatternRejected, "empty pattern rejected");
        check(nonAsciiRejected, "non-ASCII pattern rejected");
    }

    private static void randomAgainstKmp() {
        Random random = new Random(7);
        for (int round = 0; round < 200; round++) {
            int alphabet = 2 + random.nextInt(3);
            int patternCount = 1 + random.nextInt(15);
            List<String> patterns = new ArrayList<>();
            for (int i = 0; i < patternCount; i++) {
                patterns.add(randomString(random, alphabet, 1 + random.nextInt(6)));
            }
            String text = randomString(random, alphabet, random.nextInt(300));
            AhoCorasick ac = new AhoCorasick(patterns);
            long[] counts = ac.occurrenceCounts(text);
            for (int i = 0; i < patternCount; i++) {
                if (counts[i] != KMP.count(text, patterns.get(i))) {
                    throw new AssertionError("Mismatch with KMP on round " + round);
                }
            }
        }
        check(true, "200 random cases agree with KMP");
    }

    private static String randomString(Random random, int alphabet, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(alphabet)));
        }
        return sb.toString();
    }
}
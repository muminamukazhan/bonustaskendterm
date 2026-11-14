import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AhoCorasick {

    public static final int ALPHABET_SIZE = 128;

    public record Match(int patternIndex, int start, int end) {}

    @FunctionalInterface
    public interface MatchListener {
        void onMatch(int patternIndex, int start, int end);
    }

    private final int[][] go;
    private final int[] fail;
    private final int[] dictLink;
    private final int[] outHead;
    private final int[] outNext;
    private final int[] patternLength;
    private int nodeCount;

    public AhoCorasick(List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            throw new IllegalArgumentException("At least one pattern is required");
        }
        int total = 0;
        for (String p : patterns) {
            if (p == null || p.isEmpty()) {
                throw new IllegalArgumentException("Patterns must be non-empty");
            }
            for (int i = 0; i < p.length(); i++) {
                if (p.charAt(i) >= ALPHABET_SIZE) {
                    throw new IllegalArgumentException("Only ASCII patterns are supported: " + p);
                }
            }
            total += p.length();
        }
        int maxNodes = total + 1;
        go = new int[maxNodes][];
        fail = new int[maxNodes];
        dictLink = new int[maxNodes];
        outHead = new int[maxNodes];
        Arrays.fill(dictLink, -1);
        Arrays.fill(outHead, -1);
        outNext = new int[patterns.size()];
        patternLength = new int[patterns.size()];
        go[0] = newRow();
        nodeCount = 1;

        for (int i = 0; i < patterns.size(); i++) {
            insert(patterns.get(i), i);
        }
        buildLinks();
    }

    private static int[] newRow() {
        int[] row = new int[ALPHABET_SIZE];
        Arrays.fill(row, -1);
        return row;
    }

    private void insert(String pattern, int index) {
        int node = 0;
        for (int i = 0; i < pattern.length(); i++) {
            int c = pattern.charAt(i);
            if (go[node][c] == -1) {
                go[nodeCount] = newRow();
                go[node][c] = nodeCount++;
            }
            node = go[node][c];
        }
        patternLength[index] = pattern.length();
        outNext[index] = outHead[node];
        outHead[node] = index;
    }

    private void buildLinks() {
        int[] queue = new int[nodeCount];
        int head = 0;
        int tail = 0;

        for (int c = 0; c < ALPHABET_SIZE; c++) {
            int v = go[0][c];
            if (v == -1) {
                go[0][c] = 0;
            } else {
                fail[v] = 0;
                dictLink[v] = -1;
                queue[tail++] = v;
            }
        }

        while (head < tail) {
            int u = queue[head++];
            for (int c = 0; c < ALPHABET_SIZE; c++) {
                int v = go[u][c];
                int f = go[fail[u]][c];
                if (v == -1) {
                    go[u][c] = f;
                } else {
                    fail[v] = f;
                    dictLink[v] = outHead[f] != -1 ? f : dictLink[f];
                    queue[tail++] = v;
                }
            }
        }
    }

    public void search(CharSequence text, MatchListener listener) {
        int state = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= ALPHABET_SIZE) {
                state = 0;
                continue;
            }
            state = go[state][ch];
            int node = outHead[state] != -1 ? state : dictLink[state];
            while (node != -1) {
                for (int p = outHead[node]; p != -1; p = outNext[p]) {
                    listener.onMatch(p, i - patternLength[p] + 1, i + 1);
                }
                node = dictLink[node];
            }
        }
    }

    public List<Match> findAll(CharSequence text) {
        List<Match> result = new ArrayList<>();
        search(text, (p, s, e) -> result.add(new Match(p, s, e)));
        return result;
    }

    public long countMatches(CharSequence text) {
        long[] counter = new long[1];
        search(text, (p, s, e) -> counter[0]++);
        return counter[0];
    }

    public long[] occurrenceCounts(CharSequence text) {
        long[] counts = new long[patternLength.length];
        search(text, (p, s, e) -> counts[p]++);
        return counts;
    }

    public int nodeCount() {
        return nodeCount;
    }

    public int patternCount() {
        return patternLength.length;
    }

    public long approximateMemoryBytes() {
        long rows = (long) nodeCount * (ALPHABET_SIZE * 4L + 16);
        long arrays = (long) nodeCount * 4L * 3 + patternLength.length * 8L;
        return rows + arrays;
    }
}
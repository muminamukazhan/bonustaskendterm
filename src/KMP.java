import java.util.ArrayList;
import java.util.List;

public final class KMP {

    private KMP() {}

    public static int[] prefixFunction(String pattern) {
        int[] pi = new int[pattern.length()];
        int k = 0;
        for (int i = 1; i < pattern.length(); i++) {
            while (k > 0 && pattern.charAt(i) != pattern.charAt(k)) {
                k = pi[k - 1];
            }
            if (pattern.charAt(i) == pattern.charAt(k)) {
                k++;
            }
            pi[i] = k;
        }
        return pi;
    }

    public static List<Integer> findAll(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        if (pattern.isEmpty()) {
            return positions;
        }
        int[] pi = prefixFunction(pattern);
        int k = 0;
        for (int i = 0; i < text.length(); i++) {
            while (k > 0 && text.charAt(i) != pattern.charAt(k)) {
                k = pi[k - 1];
            }
            if (text.charAt(i) == pattern.charAt(k)) {
                k++;
            }
            if (k == pattern.length()) {
                positions.add(i - k + 1);
                k = pi[k - 1];
            }
        }
        return positions;
    }

    public static long count(String text, String pattern) {
        if (pattern.isEmpty()) {
            return 0;
        }
        int[] pi = prefixFunction(pattern);
        long matches = 0;
        int k = 0;
        for (int i = 0; i < text.length(); i++) {
            while (k > 0 && text.charAt(i) != pattern.charAt(k)) {
                k = pi[k - 1];
            }
            if (text.charAt(i) == pattern.charAt(k)) {
                k++;
            }
            if (k == pattern.length()) {
                matches++;
                k = pi[k - 1];
            }
        }
        return matches;
    }
}
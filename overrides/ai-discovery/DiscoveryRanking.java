package ir.sinicable.rubikaqueue;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;

/** Deterministic lexical ranking; search engines provide real group identities. */
public final class DiscoveryRanking {
    private DiscoveryRanking() {}

    public static String normalize(String text) {
        String value = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)
            .replace('ي', 'ی').replace('ى', 'ی').replace('ك', 'ک')
            .replace('\u200c', ' ').replace('\u0640', ' ');
        // Treat punctuation as word boundaries, not as part of a match.
        return value.replaceAll("[\\p{M}]", "")
            .replaceAll("[\\p{P}\\p{S}\\p{C}]+", " ")
            .replaceAll("\\s+", " ").trim();
    }

    public static String query(String text) {
        String q = text == null ? "" : text.trim();
        if (q.length() < 2 || q.length() > 300)
            throw new IllegalArgumentException("عبارت باید بین ۲ و ۳۰۰ نویسه باشد");
        return q;
    }

    /**
     * Extract at most eight independent topics. Equivalent Arabic/Persian
     * spellings count as the same topic, retaining the user's first spelling.
     */
    public static String[] terms(String query) {
        String original = query(query);
        String[] parts = original.split("[،,؛;\\n]+");
        if (parts.length == 1) return new String[]{original};
        return combine(new String[0], parts, 8);
    }

    /** Stable bounded union of typed and AI-suggested terms. */
    public static String[] combine(String[] initial, String[] suggested, int limit) {
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();
        append(unique, initial, limit);
        append(unique, suggested, limit);
        return unique.values().toArray(new String[0]);
    }

    private static void append(LinkedHashMap<String, String> unique, String[] candidates, int limit) {
        if (candidates == null || limit <= 0) return;
        for (String raw : candidates) {
            if (unique.size() >= limit) break;
            String candidate = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
            String key = normalize(candidate);
            if (candidate.length() >= 2 && candidate.length() <= 80 && key.length() >= 2) {
                unique.putIfAbsent(key, candidate);
            }
        }
    }

    private static boolean containsPhrase(String haystack, String needle) {
        return !needle.isEmpty() && (" " + haystack + " ").contains(" " + needle + " ");
    }

    public static int score(String query, String name, String keyword, boolean identified) {
        String q = normalize(query), n = normalize(name), k = normalize(keyword);
        if (q.isEmpty()) return 0;
        int score = n.equals(q) ? 60 : containsPhrase(n, q) ? 35 : 0;
        if (k.equals(q)) score += 18;
        else if (containsPhrase(k, q)) score += 9;
        for (String word : q.split(" ")) {
            if (containsPhrase(n, word)) score += 8;
            else if (containsPhrase(k, word)) score += 3;
        }
        // Having a GUID or unrelated keyword alone is not topical evidence.
        if (score == 0) return 0;
        return score + (identified ? 5 : 0);
    }

    public static int scoreAny(String[] terms, String name, String keyword, boolean identified) {
        int best = 0;
        if (terms == null) return best;
        for (String term : terms) best = Math.max(best, score(term, name, keyword, identified));
        return best;
    }

    /**
     * A match against a user-entered topic outranks an AI-only suggestion.
     * The larger score is ordinal rather than a percentage.
     */
    public static int scoreWithSuggestions(String[] typed, String[] suggested,
            String name, String keyword, boolean identified) {
        int direct = scoreAny(typed, name, keyword, identified);
        if (direct > 0) return 200 + Math.min(direct, 199);
        return Math.min(scoreAny(suggested, name, keyword, identified), 199);
    }
}

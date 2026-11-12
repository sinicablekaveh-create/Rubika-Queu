package ir.sinicable.rubikaqueue;

import java.text.Normalizer;
import java.util.Locale;

/** Deterministic lexical ranking; AI expands queries, never invents groups. */
public final class DiscoveryRanking {
    private DiscoveryRanking() {}
    public static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT).replace('ي', 'ی').replace('ك', 'ک')
            .replace('\u200c', ' ').replaceAll("[\\p{M}]", "").replaceAll("\\s+", " ").trim();
    }
    public static String query(String text) {
        String q = text == null ? "" : text.trim();
        if (q.length() < 2 || q.length() > 300) throw new IllegalArgumentException("عبارت باید بین ۲ و ۳۰۰ نویسه باشد");
        return q;
    }
    /** Split a Persian or English topic list into focused searches, without inventing groups. */
    public static String[] terms(String query) {
        String original = query(query);
        java.util.LinkedHashSet<String> terms = new java.util.LinkedHashSet<>();
        String[] parts = original.split("[،,؛;\\n]+");
        if (parts.length > 1) {
            for (String part : parts) {
                String candidate = part.trim().replaceAll("\\s+", " ");
                if (candidate.length() >= 2 && candidate.length() <= 80) {
                    terms.add(candidate);
                    if (terms.size() >= 8) break;
                }
            }
        }
        if (terms.isEmpty()) terms.add(original);
        return terms.toArray(new String[0]);
    }

    /** Rank by the strongest matching search topic, not an entire comma-separated list. */
    public static int scoreAny(String[] terms, String name, String keyword, boolean identified) {
        int best = 0;
        if (terms == null) return best;
        for (String term : terms) best = Math.max(best, score(term, name, keyword, identified));
        return best;
    }

    public static int score(String query, String name, String keyword, boolean identified) {
        String q = normalize(query), n = normalize(name), k = normalize(keyword);
        if (q.isEmpty()) return 0;
        int score = n.equals(q) ? 60 : n.contains(q) ? 35 : 0;
        if (k.equals(q)) score += 18;
        else if (!k.isEmpty() && k.contains(q)) score += 9;
        else if (!k.isEmpty()) score += 4;
        for (String token : q.split(" ")) {
            if (n.contains(token)) score += 8;
            else if (k.contains(token)) score += 3;
        }
        return score + (identified ? 5 : 0);
    }
}

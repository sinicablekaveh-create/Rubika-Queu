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

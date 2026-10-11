package ir.sinicable.rubikaqueue;

public final class DiscoveryRankingTests {
    private static int count;
    private static void check(boolean ok) {
        if (!ok) throw new AssertionError("check " + count);
        count++;
    }
    private static void expectInvalid(String value) {
        try {
            DiscoveryRanking.query(value);
            throw new AssertionError("accepted invalid query: " + value);
        } catch (IllegalArgumentException expected) {
            count++;
        }
    }
    public static void main(String[] args) {
        check(DiscoveryRanking.normalize("كابل يک").equals("کابل یک"));
        check(DiscoveryRanking.normalize(" POWER   Tools ").equals("power tools"));
        check(DiscoveryRanking.normalize("كابل‌- برق").equals("کابل برق"));
        check(DiscoveryRanking.score("برق", "برق", "", false)
                > DiscoveryRanking.score("برق", "ابزار برق", "", false));
        check(DiscoveryRanking.score("برق", "ابزار برق", "", false)
                > DiscoveryRanking.score("برق", "آشپزی", "", false));
        check(DiscoveryRanking.score("کابل", "كابل", "", false) > 0);
        check(DiscoveryRanking.score("", "name", "", true) == 0);
        check(DiscoveryRanking.score(null, null, null, false) == 0);
        check(DiscoveryRanking.score("برق صنعتی", "برق صنعتی", "", false)
                > DiscoveryRanking.score("برق صنعتی", "برق", "", false));
        check(DiscoveryRanking.score("برق", "گروه", "برق", false)
                > DiscoveryRanking.score("برق", "گروه", "سیم‌کشی", false));
        check(DiscoveryRanking.score("برق", "گروه", "سیم‌کشی", false) == 0);
        check(DiscoveryRanking.score("برق", "گروه", "", true) == 0);
        check(DiscoveryRanking.score("برق", "گروه برق", "", true)
                > DiscoveryRanking.score("برق", "گروه برق", "", false));
        check(DiscoveryRanking.score("برق", "ابرقدرت", "", false) == 0);
        check(DiscoveryRanking.score("برق", "کابل-برق", "", false) > 0);
        check(DiscoveryRanking.score("کابل", "کابل‌بندی", "", false) > 0);
        check(DiscoveryRanking.query(" برق ").equals("برق"));
        expectInvalid("");
        expectInvalid("x");
        expectInvalid("x".repeat(301));

        String[] topics = DiscoveryRanking.terms("برق صنعتی، سینی کابل، نردبان کابل");
        check(topics.length == 3);
        check(topics[0].equals("برق صنعتی"));
        check(topics[2].equals("نردبان کابل"));
        check(DiscoveryRanking.scoreAny(topics, "فروش سینی کابل", "", false)
                > DiscoveryRanking.scoreAny(topics, "آشپزی", "", false));
        check(DiscoveryRanking.terms("برق ساختمان").length == 1);
        check(DiscoveryRanking.terms("برق،برق، کابل").length == 2);
        check(DiscoveryRanking.terms("سيني كابل، سینی کابل، نردبان کابل").length == 2);
        check(DiscoveryRanking.terms("a,b").length == 1);
        check(DiscoveryRanking.scoreAny(null, "برق", "", true) == 0);

        String[] combined = DiscoveryRanking.combine(
                new String[]{"سینی کابل", "برق صنعتی"},
                new String[]{"سيني كابل", "لدر کابل", "لدر-کابل", "نردبان کابل"}, 4);
        check(combined.length == 4);
        check(combined[0].equals("سینی کابل"));
        check(combined[2].equals("لدر کابل"));
        check(DiscoveryRanking.combine(new String[0], new String[]{"x", ""}, 8).length == 0);

        String[] typed = {"برق صنعتی"};
        String[] proposed = {"نردبان کابل"};
        check(DiscoveryRanking.scoreWithSuggestions(typed, proposed,
                    "مجموعه برق صنعتی", "", true)
                > DiscoveryRanking.scoreWithSuggestions(typed, proposed,
                    "نردبان کابل", "", true));
        check(DiscoveryRanking.scoreWithSuggestions(typed, proposed,
                    "نردبان کابل", "", true) > 0);
        check(DiscoveryRanking.scoreWithSuggestions(typed, proposed,
                    "آشپزی", "", true) == 0);
        System.out.println("DiscoveryRankingTests: " + count + " checks passed");
    }
}

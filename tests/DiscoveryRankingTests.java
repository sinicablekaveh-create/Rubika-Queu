package ir.sinicable.rubikaqueue;
public final class DiscoveryRankingTests {
    private static int count;
    private static void check(boolean b) { if (!b) throw new AssertionError("check " + count); count++; }
    public static void main(String[] args) {
        check(DiscoveryRanking.normalize("كابل يک").equals("کابل یک"));
        check(DiscoveryRanking.normalize(" POWER   Tools ").equals("power tools"));
        check(DiscoveryRanking.score("برق", "برق", "", false) > DiscoveryRanking.score("برق", "ابزار برق", "", false));
        check(DiscoveryRanking.score("برق", "ابزار برق", "", false) > DiscoveryRanking.score("برق", "آشپزی", "", false));
        check(DiscoveryRanking.score("کابل", "كابل", "", false) > 0);
        check(DiscoveryRanking.score("", "name", "", true) == 0);
        check(DiscoveryRanking.score(null, null, null, false) == 0);
        check(DiscoveryRanking.score("برق صنعتی", "برق صنعتی", "", false) > DiscoveryRanking.score("برق صنعتی", "برق", "", false));
        check(DiscoveryRanking.score("برق", "گروه", "برق", false) > DiscoveryRanking.score("برق", "گروه", "سیم‌کشی", false));
        check(DiscoveryRanking.score("برق", "گروه", "سیم‌کشی", false) > DiscoveryRanking.score("برق", "گروه", "", false));
        check(DiscoveryRanking.score("برق", "گروه", "", true) > DiscoveryRanking.score("برق", "گروه", "", false));
        check(DiscoveryRanking.query(" برق ").equals("برق"));
        for (String q : new String[]{"", "x", "x".repeat(301)}) {
            try { DiscoveryRanking.query(q); throw new AssertionError("accepted invalid query"); }
            catch (IllegalArgumentException expected) { count++; }
        }
        System.out.println("DiscoveryRankingTests: " + count + " checks passed");
    }
}

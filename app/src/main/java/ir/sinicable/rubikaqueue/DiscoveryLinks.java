package ir.sinicable.rubikaqueue;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;

/** Canonical public identities; invite tokens and URL paths remain case sensitive. */
final class DiscoveryLinks {
    static String rubika(String value) {
        try {
            URI original=new URI(value==null?"":value.trim());
            if(original.getRawQuery()!=null||original.getRawFragment()!=null)return "";
        } catch(Exception e){return "";}
        String url = https(value);
        if (url.isEmpty()) return "";
        try {
            URI u = new URI(url);
            if (!"rubika.ir".equals(u.getHost()) || u.getRawQuery() != null || u.getRawFragment() != null) return "";
            String path = u.getRawPath();
            if (!path.matches("/(?:[A-Za-z0-9_-]{3,64}|(?:joinc|joing)/[A-Za-z0-9_-]{3,96})/?")) return "";
            path = path.replaceAll("/$", "");
            String lower = path.substring(1).toLowerCase(Locale.ROOT);
            if (Arrays.asList("install-android", "install-ios", "rules", "faq", "blog", "about",
                    "jobs", "support", "logo", "contests", "botapi", "home", "joinc", "joing").contains(lower)) return "";
            if (!path.startsWith("/joinc/") && !path.startsWith("/joing/")) path = path.toLowerCase(Locale.ROOT);
            return "https://rubika.ir" + path;
        } catch (Exception e) { return ""; }
    }

    static String https(String value) {
        try {
            URI u = new URI(value == null ? "" : value.trim());
            if (!"https".equalsIgnoreCase(u.getScheme()) || u.getHost() == null
                    || u.getUserInfo() != null || u.getPort() != -1) return "";
            return "https://" + u.getHost().toLowerCase(Locale.ROOT)
                    + (u.getRawPath().isEmpty() ? "/" : u.getRawPath())
                    + (u.getRawQuery() == null ? "" : "?" + u.getRawQuery());
        } catch (Exception e) { return ""; }
    }

    static String phone(String input) {
        if (input == null) return "";
        String s = input.trim().replace("\u200e", "").replace("\u200f", "");
        String fa = "۰۱۲۳۴۵۶۷۸۹", ar = "٠١٢٣٤٥٦٧٨٩";
        for (int i = 0; i < 10; i++) s = s.replace(fa.charAt(i), (char)('0'+i)).replace(ar.charAt(i), (char)('0'+i));
        if (!s.matches("\\+?[0-9\\s()\\-./]+")) return "";
        boolean international = s.startsWith("+");
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) { digits = digits.substring(2); international = true; }
        else if (digits.startsWith("0") && digits.length() == 11) { digits = "98" + digits.substring(1); international = true; }
        else if (digits.startsWith("9") && digits.length() == 10) { digits = "98" + digits; international = true; }
        else if (digits.startsWith("98") && digits.length() == 12) international = true;
        if (digits.length() < 7 || digits.length() > 15 || digits.matches("0+")) return "";
        return (international ? "+" : "") + digits;
    }

    static boolean publicKind(String kind) {
        return "public_group".equals(kind) || "public_channel".equals(kind);
    }
    private DiscoveryLinks() { }
}

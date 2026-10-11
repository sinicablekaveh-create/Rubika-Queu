package ir.sinicable.rubikaqueue;

import java.net.URI;

/** Pure-Java validation for user-initiated AI drafting. */
public final class AiPromptPolicy {
    private AiPromptPolicy() {}

    public static String prompt(String input) {
        String text = input == null ? "" : input.trim();
        if (text.isEmpty() || text.length() > 3500) {
            throw new IllegalArgumentException("متن باید بین ۱ و ۳۵۰۰ نویسه باشد");
        }
        return text;
    }

    public static String model(String input) {
        String value = input == null ? "" : input.trim();
        if (!value.matches("[A-Za-z0-9._:/-]{1,80}")) {
            throw new IllegalArgumentException("نام مدل معتبر نیست");
        }
        return value;
    }

    public static String endpoint(String input) {
        if (input == null) throw new IllegalArgumentException("آدرس سرور لازم است");
        String value = input.trim();
        final URI uri;
        try { uri = URI.create(value); }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("آدرس HTTPS معتبر نیست");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getHost().isEmpty() || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty()
                    && !"/".equals(uri.getPath()))
                || uri.getPort() == 0 || uri.getPort() > 65535) {
            throw new IllegalArgumentException("فقط آدرس ریشه سرور HTTPS مجاز است");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

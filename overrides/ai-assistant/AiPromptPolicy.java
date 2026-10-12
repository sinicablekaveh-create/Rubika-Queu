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

    /** Reject errors, placeholders and oversized AI output before returning to sender. */
    public static String suggestion(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isEmpty() || cleaned.length() > 4200) {
            throw new IllegalArgumentException("متن پیشنهادی معتبر یا قابل درج نیست");
        }
        return cleaned;
    }

    public static String model(String input) {
        String value = input == null ? "" : input.trim();
        if (!value.matches("[A-Za-z0-9._:/-]{1,80}")) {
            throw new IllegalArgumentException("نام مدل معتبر نیست");
        }
        return value;
    }

    /** Allow an optional user-supplied bearer token, but never CR/LF or control characters. */
    public static String apiKey(String input) {
        String key = input == null ? "" : input.trim();
        if (key.length() > 512) throw new IllegalArgumentException("کلید API بیش از حد طولانی است");
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c < 33 || c > 126) throw new IllegalArgumentException("کلید API معتبر نیست");
        }
        return key;
    }

    /** Form the standard chat-completions URL without accepting query, credentials or HTTP. */
    public static String chatCompletionsUrl(String input) {
        if (input == null) throw new IllegalArgumentException("آدرس دروازه لازم است");
        String value = input.trim();
        final URI uri;
        try { uri = URI.create(value); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("آدرس HTTPS معتبر نیست"); }
        String path = uri.getRawPath();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getHost().isEmpty() || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null
                || uri.getPort() == 0 || uri.getPort() > 65535
                || (path != null && !path.isEmpty() && !"/".equals(path)
                    && !path.matches("(?:/[A-Za-z0-9._~-]+)+/?"))) {
            throw new IllegalArgumentException("فقط نشانی HTTPS دروازه مجاز است");
        }
        if (path != null) {
            for (String segment : path.split("/")) {
                if (".".equals(segment) || "..".equals(segment)) {
                    throw new IllegalArgumentException("مسیر دروازه معتبر نیست");
                }
            }
        }
        String base = value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
        return base + (base.endsWith("/v1") ? "/chat/completions" : "/v1/chat/completions");
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

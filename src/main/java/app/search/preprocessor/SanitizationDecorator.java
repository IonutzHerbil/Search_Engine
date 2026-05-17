package app.search.preprocessor;

import java.util.regex.Pattern;

public class SanitizationDecorator implements QueryPreProcessor {

    private static final Pattern UNSAFE = Pattern.compile("[^\\w\\s*\":()\\-]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");

    private final QueryPreProcessor delegate;

    public SanitizationDecorator(QueryPreProcessor delegate) {
        this.delegate = delegate;
    }

    @Override
    public String process(String raw) {
        String processed = delegate.process(raw == null ? "" : raw.trim());
        if (processed.isBlank()) return processed;

        String[] tokens = processed.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String token : tokens) {
            if (!result.isEmpty()) result.append(" ");
            if (isQualifier(token) || isOperator(token) || isQuotedPhrase(token)) {
                result.append(token);
            } else {
                result.append(UNSAFE.matcher(token).replaceAll(""));
            }
        }

        return MULTI_SPACE.matcher(result.toString().trim()).replaceAll(" ");
    }

    private boolean isQualifier(String t) {
        String l = t.toLowerCase();
        return l.startsWith("ext:") || l.startsWith("path:") || l.startsWith("name:")
                || l.startsWith("content:") || l.startsWith("color:");
    }

    private boolean isOperator(String t) {
        return t.equalsIgnoreCase("AND") || t.equalsIgnoreCase("OR") || t.equalsIgnoreCase("NOT");
    }

    private boolean isQuotedPhrase(String t) {
        return t.startsWith("\"") && t.endsWith("\"");
    }
}
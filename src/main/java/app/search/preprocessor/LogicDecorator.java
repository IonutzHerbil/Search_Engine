package app.search.preprocessor;

public class LogicDecorator implements QueryPreProcessor {

    private final QueryPreProcessor delegate;

    public LogicDecorator(QueryPreProcessor delegate) {
        this.delegate = delegate;
    }

    @Override
    public String process(String raw) {
        String processed = delegate.process(raw);
        if (processed.isBlank()) return processed;

        String[] tokens = processed.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String token : tokens) {
            if (!result.isEmpty()) result.append(" ");
            if (isQualifier(token) || isOperator(token)
                    || isQuotedPhrase(token) || token.endsWith("*")) {
                result.append(token);
            } else if (token.startsWith("(") && token.endsWith(")")) {
                result.append(wildcardGroup(token));
            } else {
                result.append(token).append("*");
            }
        }

        return result.toString().trim();
    }

    private String wildcardGroup(String group) {
        String inner = group.substring(1, group.length() - 1);
        String[] terms = inner.split("\\s+OR\\s+");
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < terms.length; i++) {
            if (i > 0) sb.append(" OR ");
            String t = terms[i].trim();
            sb.append(t.endsWith("*") ? t : t + "*");
        }
        return sb.append(")").toString();
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
package app.search;

import java.time.Instant;

public record SearchEvent(String query, Instant timestamp) {}

package com.hp.jei_kubejs.sync;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class RecipeSyncRequest {
    private static final Set<String> TYPES = new TreeSet<>();
    private static final Set<String> PREFIXES = new TreeSet<>();

    private RecipeSyncRequest() {
    }

    public static synchronized void clear() {
        TYPES.clear();
        PREFIXES.clear();
    }

    public static synchronized void addType(String type) {
        TYPES.add(type);
    }

    public static synchronized void addPrefix(String prefix) {
        PREFIXES.add(prefix);
    }

    public static synchronized List<String> types() {
        return List.copyOf(TYPES);
    }

    public static synchronized List<String> prefixes() {
        return List.copyOf(PREFIXES);
    }
}

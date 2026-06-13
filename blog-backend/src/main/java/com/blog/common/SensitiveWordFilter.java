package com.blog.common;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SensitiveWordFilter {

    private Map<Character, Object> rootNode = new HashMap<>();
    private boolean initialized = false;

    private static final String REPLACEMENT = "***";
    private static final String LEVEL_KEY = "__level__";

    public void init(List<SensitiveWordEntry> entries) {
        rootNode.clear();
        for (SensitiveWordEntry entry : entries) {
            if (entry.word == null || entry.word.trim().isEmpty()) continue;
            addWord(entry.word.trim().toLowerCase(), entry.level);
        }
        initialized = true;
    }

    private void addWord(String word, int level) {
        Map<Character, Object> current = rootNode;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            Object node = current.get(c);
            if (node == null) {
                Map<Character, Object> newNode = new HashMap<>();
                current.put(c, newNode);
                current = newNode;
            } else {
                current = (Map<Character, Object>) node;
            }
            if (i == word.length() - 1) {
                current.put('\0', level);
            }
        }
    }

    public String filter(String text) {
        if (text == null || text.isEmpty() || !initialized) return text;
        String lower = text.toLowerCase();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < lower.length()) {
            MatchResult match = findMatch(lower, i);
            if (match.length > 0 && match.level == 1) {
                sb.append(REPLACEMENT);
                i += match.length;
            } else if (match.length > 0 && match.level == 2) {
                sb.append(REPLACEMENT);
                i += match.length;
            } else {
                sb.append(text.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    public int getMaxLevel(String text) {
        if (text == null || text.isEmpty() || !initialized) return 0;
        String lower = text.toLowerCase();
        int maxLevel = 0;
        for (int i = 0; i < lower.length(); i++) {
            MatchResult match = findMatch(lower, i);
            if (match.level > maxLevel) maxLevel = match.level;
        }
        return maxLevel;
    }

    public boolean contains(String text) {
        return getMaxLevel(text) > 0;
    }

    private MatchResult findMatch(String text, int start) {
        Map<Character, Object> current = rootNode;
        int matchLen = 0;
        int level = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            Object node = current.get(c);
            if (node == null) break;
            current = (Map<Character, Object>) node;
            matchLen++;
            Object endMarker = current.get('\0');
            if (endMarker != null) {
                level = (int) endMarker;
                return new MatchResult(matchLen, level);
            }
        }
        return new MatchResult(0, 0);
    }

    public boolean isInitialized() {
        return initialized;
    }

    private static class MatchResult {
        final int length;
        final int level;
        MatchResult(int length, int level) {
            this.length = length;
            this.level = level;
        }
    }

    public static class SensitiveWordEntry {
        public final String word;
        public final int level;
        public SensitiveWordEntry(String word, int level) {
            this.word = word;
            this.level = level;
        }
    }
}

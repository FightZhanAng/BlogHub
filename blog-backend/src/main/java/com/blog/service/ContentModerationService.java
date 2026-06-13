package com.blog.service;

public interface ContentModerationService {

    ModerationResult moderateText(String text);

    ModerationResult moderateImage(String imageUrl);

    class ModerationResult {
        private boolean passed;
        private String filteredText;
        private String reason;

        public static ModerationResult pass(String text) {
            ModerationResult r = new ModerationResult();
            r.passed = true;
            r.filteredText = text;
            return r;
        }

        public static ModerationResult replace(String filteredText) {
            ModerationResult r = new ModerationResult();
            r.passed = true;
            r.filteredText = filteredText;
            return r;
        }

        public static ModerationResult reject(String reason) {
            ModerationResult r = new ModerationResult();
            r.passed = false;
            r.reason = reason;
            return r;
        }

        public boolean isPassed() { return passed; }
        public String getFilteredText() { return filteredText; }
        public String getReason() { return reason; }
    }
}

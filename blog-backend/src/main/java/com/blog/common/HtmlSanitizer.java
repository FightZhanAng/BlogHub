package com.blog.common;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * HTML 清洗工具 — 防止 XSS
 */
public class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.basic()
            .addTags("img", "h1", "h2", "h3", "h4", "h5", "h6")
            .addAttributes("img", "src", "alt", "title")
            .addAttributes("a", "href", "title", "target")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("a", "href", "http", "https", "mailto");

    private HtmlSanitizer() {}

    /**
     * 清洗 HTML，只保留安全的标签和属性
     */
    public static String clean(String input) {
        if (input == null || input.isEmpty()) return input;
        return Jsoup.clean(input, SAFELIST);
    }

    /**
     * 严格模式：只保留纯文本，去除所有 HTML
     */
    public static String stripHtml(String input) {
        if (input == null || input.isEmpty()) return input;
        return Jsoup.clean(input, Safelist.none());
    }
}

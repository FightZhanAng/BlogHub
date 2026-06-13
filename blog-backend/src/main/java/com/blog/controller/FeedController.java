package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.entity.Post;
import com.blog.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * RSS 订阅
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "订阅源", description = "RSS和Sitemap订阅")
public class FeedController {

    private final PostService postService;

    @Value("${blog.base-url}")
    private String baseUrl;

    @Operation(summary = "生成Sitemap")
    @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=utf-8")
    public String sitemap() {
        IPage<Post> page = postService.getPublishedPosts(1, 500, null, null, null, null);
        String base = baseUrl;
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        xml.append("  <url><loc>").append(base).append("/</loc><priority>1.0</priority></url>\n");
        xml.append("  <url><loc>").append(base).append("/home</loc><priority>0.9</priority></url>\n");
        xml.append("  <url><loc>").append(base).append("/blog</loc><priority>0.9</priority></url>\n");
        xml.append("  <url><loc>").append(base).append("/archive</loc><priority>0.7</priority></url>\n");
        xml.append("  <url><loc>").append(base).append("/tags</loc><priority>0.7</priority></url>\n");
        xml.append("  <url><loc>").append(base).append("/about</loc><priority>0.5</priority></url>\n");
        for (Post post : page.getRecords()) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(base).append("/blog/").append(post.getSlug()).append("</loc>\n");
            xml.append("    <priority>0.8</priority>\n");
            if (post.getCreatedAt() != null) {
                xml.append("    <lastmod>").append(post.getCreatedAt().toLocalDate()).append("</lastmod>\n");
            }
            xml.append("  </url>\n");
        }
        xml.append("</urlset>\n");
        return xml.toString();
    }

    @Operation(summary = "生成RSS订阅源")
    @GetMapping(value = "/feed.xml", produces = "application/rss+xml;charset=utf-8")
    public String rss() {
        IPage<Post> page = postService.getPublishedPosts(1, 20, null, null, null, null);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        xml.append("  <channel>\n");
        xml.append("    <title>BlogHub</title>\n");
        xml.append("    <link>").append(baseUrl).append("</link>\n");
        xml.append("    <description>BlogHub - 一个技术博客</description>\n");
        xml.append("    <language>zh-CN</language>\n");
        xml.append("    <atom:link href=\"").append(baseUrl).append("/feed.xml\" rel=\"self\" type=\"application/rss+xml\"/>\n");

        for (Post post : page.getRecords()) {
            xml.append("    <item>\n");
            xml.append("      <title>").append(escapeXml(post.getTitle())).append("</title>\n");
            xml.append("      <link>").append(baseUrl).append("/blog/").append(post.getSlug()).append("</link>\n");
            xml.append("      <guid>").append(baseUrl).append("/blog/").append(post.getSlug()).append("</guid>\n");
            xml.append("      <description>").append(escapeXml(post.getDescription())).append("</description>\n");
            if (post.getCreatedAt() != null) {
                ZonedDateTime zdt = post.getCreatedAt().atZone(ZoneId.systemDefault());
                xml.append("      <pubDate>").append(zdt.format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("</pubDate>\n");
            }
            xml.append("    </item>\n");
        }

        xml.append("  </channel>\n");
        xml.append("</rss>\n");
        return xml.toString();
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

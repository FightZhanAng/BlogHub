package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.entity.HotTopic;
import com.blog.mapper.HotTopicMapper;
import com.blog.service.HotTopicService;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotTopicServiceImpl extends ServiceImpl<HotTopicMapper, HotTopic> implements HotTopicService {

    private static final Logger log = LoggerFactory.getLogger(HotTopicServiceImpl.class);

    private static final String API_BASE = "https://60s.viki.moe/v2/";

    private static final Map<String, String> PLATFORMS = new LinkedHashMap<>();
    static {
        PLATFORMS.put("weibo",   API_BASE + "weibo");
        PLATFORMS.put("zhihu",   API_BASE + "zhihu");
        PLATFORMS.put("douyin",  API_BASE + "douyin");
        PLATFORMS.put("bili",    API_BASE + "bili");
        PLATFORMS.put("toutiao", API_BASE + "toutiao");
    }

    private final RestTemplate restTemplate;

    @Override
    @Cacheable(value = "hotTopics", key = "'today'")
    public Map<String, List<HotTopic>> getTodayTopics() {
        LocalDate today = LocalDate.now();
        List<HotTopic> allTopics = list(new LambdaQueryWrapper<HotTopic>()
                .eq(HotTopic::getFetchedAt, today)
                .orderByAsc(HotTopic::getPlatform, HotTopic::getRankNum));

        return allTopics.stream().collect(Collectors.groupingBy(HotTopic::getPlatform));
    }

    @Override
    @Cacheable(value = "hotTopics", key = "'platform:' + #platform")
    public List<HotTopic> getTopicsByPlatform(String platform) {
        return baseMapper.findByPlatformAndDate(platform, LocalDate.now());
    }

    @XxlJob("fetchAllTopics")
    @CacheEvict(value = "hotTopics", allEntries = true)
    @Override
    public void fetchAllTopics() {
        log.info("开始抓取每日热点...");
        XxlJobHelper.log("开始抓取每日热点...");
        fetchGitHubTrending();
        for (Map.Entry<String, String> entry : PLATFORMS.entrySet()) {
            fetchFrom60sApi(entry.getKey(), entry.getValue());
        }
        log.info("每日热点抓取完成");
        XxlJobHelper.log("每日热点抓取完成");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void fetchGitHubTrending() {
        try {
            String url = "https://api.github.com/search/repositories?q=created:>2025-01-01&sort=stars&order=desc&per_page=20";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() == null) return;

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
            if (items == null) return;

            LocalDate today = LocalDate.now();
            remove(new LambdaQueryWrapper<HotTopic>()
                    .eq(HotTopic::getPlatform, "github")
                    .eq(HotTopic::getFetchedAt, today));

            List<HotTopic> topics = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                HotTopic topic = new HotTopic();
                topic.setPlatform("github");
                topic.setTitle(String.valueOf(item.get("full_name")));
                topic.setUrl(String.valueOf(item.get("html_url")));
                topic.setRankNum(i + 1);

                String desc = String.valueOf(item.get("description"));
                long stars = item.get("stargazers_count") != null ? ((Number) item.get("stargazers_count")).longValue() : 0;
                String lang = String.valueOf(item.get("language"));
                topic.setExtra(String.format("⭐ %d | %s | %s", stars, lang, desc.length() > 60 ? desc.substring(0, 60) + "..." : desc));
                topic.setHotValue(String.valueOf(stars));
                topic.setFetchedAt(today);
                topics.add(topic);
            }
            saveBatch(topics);
            log.info("GitHub Trending 抓取完成，共 {} 条", topics.size());
        } catch (Exception e) {
            log.error("GitHub Trending 抓取失败: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchFrom60sApi(String platform, String apiUrl) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(apiUrl, Map.class);
            if (response.getBody() == null) return;

            Object dataObj = response.getBody().get("data");
            if (!(dataObj instanceof List)) return;
            List<Map<String, Object>> items = (List<Map<String, Object>>) dataObj;

            LocalDate today = LocalDate.now();
            remove(new LambdaQueryWrapper<HotTopic>()
                    .eq(HotTopic::getPlatform, platform)
                    .eq(HotTopic::getFetchedAt, today));

            List<HotTopic> topics = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                HotTopic topic = new HotTopic();
                topic.setPlatform(platform);
                topic.setTitle(String.valueOf(item.getOrDefault("title", "")));
                topic.setUrl(String.valueOf(item.getOrDefault("link", "")));

                Object hotVal = item.get("hot_value");
                topic.setHotValue(hotVal != null ? String.valueOf(hotVal) : "");
                topic.setRankNum(i + 1);

                String detail = String.valueOf(item.getOrDefault("detail", ""));
                topic.setExtra(detail.length() > 80 ? detail.substring(0, 80) + "..." : detail);
                topic.setFetchedAt(today);
                topics.add(topic);
            }
            saveBatch(topics);
            log.info("{} 抓取完成，共 {} 条", platform, topics.size());
        } catch (Exception e) {
            log.error("{} 抓取失败: {}", platform, e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void fetchFromUrl(String platform, String apiUrl) {
        fetchFrom60sApi(platform, apiUrl);
    }
}

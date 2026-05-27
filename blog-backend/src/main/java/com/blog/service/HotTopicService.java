package com.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.entity.HotTopic;

import java.util.List;
import java.util.Map;

public interface HotTopicService extends IService<HotTopic> {

    Map<String, List<HotTopic>> getTodayTopics();

    List<HotTopic> getTopicsByPlatform(String platform);

    void fetchAllTopics();

    void fetchGitHubTrending();

    void fetchFromUrl(String platform, String apiUrl);
}

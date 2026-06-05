package com.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * MiMo AI 模型配置
 */
@Configuration
@ConfigurationProperties(prefix = "mimo")
public class MiMoModelConfig {

    private String apiKey;
    private String apiUrl;
    private String defaultModel;
    private List<ModelInfo> models = new ArrayList<>();

    @Data
    public static class ModelInfo {
        private String id;
        private String name;
        private String apiModel;
        private String description;
        private Integer maxTokens;
        private Boolean supportThinking;
        private Boolean supportImage;
        private Double temperature;
        private Double topP;
    }

    public ModelInfo getModel(String modelId) {
        return models.stream()
                .filter(m -> m.getId().equals(modelId))
                .findFirst()
                .orElseGet(() -> models.isEmpty() ? null : models.get(0));
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    public List<ModelInfo> getModels() { return models; }
    public void setModels(List<ModelInfo> models) { this.models = models; }
}

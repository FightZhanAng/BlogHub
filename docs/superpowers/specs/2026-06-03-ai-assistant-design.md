# AI 助手功能设计文档

**创建日期**: 2026-06-03
**状态**: 待审核
**版本**: 1.0

---

## 一、功能概述

为 BlogHub 博客系统添加 AI 助手功能，支持：
- 独立的 AI 助手对话页面
- 文章页面集成 AI 按钮
- 流式输出（逐字显示）
- 多模型切换（MiMo V2.5 Pro / Flash / V2.5）
- 思考模式开关
- 图像输入支持
- 对话历史管理（数据库存储）

---

## 二、技术架构

```
┌─────────────────────────────────────────────────────────┐
│                     前端 (Vue 3)                         │
├─────────────────────────────────────────────────────────┤
│  views/AiAssistant.vue                                   │
│  ├── components/ChatMessage.vue                         │
│  ├── components/StreamingText.vue                       │
│  ├── components/ChatHistory.vue                         │
│  └── components/ImageUploader.vue                       │
│                                                         │
│  views/BlogPost.vue                                     │
│  └── components/AiAssistButton.vue                      │
├─────────────────────────────────────────────────────────┤
│                   composables                           │
│  ├── useAiChat.js (调用后端 API)                        │
│  └── useChatHistory.js (对话历史管理)                   │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   后端 (Spring Boot)                     │
├─────────────────────────────────────────────────────────┤
│  controller/AiChatController.java                       │
│  ├── POST /api/ai/chat (流式输出)                       │
│  ├── POST /api/ai/upload-image (上传图片)               │
│  ├── GET /api/ai/conversations (对话列表)               │
│  ├── GET /api/ai/history/{id} (对话历史)                │
│  └── DELETE /api/ai/history/{id} (删除历史)             │
├─────────────────────────────────────────────────────────┤
│  service/AiChatService.java                             │
│  ├── streamChat(request, emitter)                       │
│  ├── buildRequestBody(request, modelInfo)                │
│  └── saveMessage(conversationId, message)               │
├─────────────────────────────────────────────────────────┤
│  service/ImageService.java                              │
│  ├── uploadImage(file)                                  │
│  └── validateFile(file)                                 │
├─────────────────────────────────────────────────────────┤
│  entity/                                                │
│  ├── AiConversation.java                                │
│  ├── AiMessage.java                                     │
│  └── AiImage.java                                       │
├─────────────────────────────────────────────────────────┤
│  config/                                                │
│  └── MiMoModelConfig.java                               │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│  环境变量 (安全存储)                                     │
│  MIMO_API_KEY=your_key_here                             │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│               MiMo API (Xiaomi)                         │
│  POST https://api.xiaomimimo.com/v1/chat/completions    │
└─────────────────────────────────────────────────────────┘
```

---

## 三、数据库设计

### 3.1 对话表 (ai_conversation)

```sql
CREATE TABLE ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL DEFAULT '新对话',
    article_id BIGINT NULL COMMENT '关联的文章ID（可选）',
    model_id VARCHAR(50) NOT NULL DEFAULT 'mimo-v2.5-pro' COMMENT '使用的模型',
    thinking_enabled BOOLEAN DEFAULT FALSE COMMENT '是否开启思考模式',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_article_id (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话表';
```

### 3.2 消息表 (ai_message)

```sql
CREATE TABLE ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    role ENUM('system', 'user', 'assistant') NOT NULL,
    content TEXT NOT NULL,
    thinking_content TEXT NULL COMMENT '思考过程内容',
    images JSON NULL COMMENT '图片信息（URL引用）',
    tokens_used INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息表';
```

### 3.3 图片表 (ai_image)

```sql
CREATE TABLE ai_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL COMMENT '存储文件名',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    mime_type VARCHAR(50) NOT NULL,
    size BIGINT NOT NULL COMMENT '文件大小（字节）',
    url VARCHAR(500) NOT NULL COMMENT '访问URL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI图片表';
```

---

## 四、配置设计

### 4.1 application.yml

```yaml
mimo:
  api-key: ${MIMO_API_KEY}
  api-url: https://api.xiaomimimo.com/v1/chat/completions
  default-model: mimo-v2.5-pro
  
  models:
    - id: mimo-v2.5-pro
      name: MiMo V2.5 Pro
      description: 强大的通用模型，适合大多数任务
      max-tokens: 4096
      support-thinking: true
      support-image: false
      temperature: 0.7
      top-p: 0.95
      
    - id: mimo-v2.5-flash
      name: MiMo V2.5 Flash
      description: 快速响应，适合简单对话
      max-tokens: 2048
      support-thinking: false
      support-image: false
      temperature: 0.7
      top-p: 0.95
      
    - id: mimo-v2.5
      name: MiMo V2.5
      description: 支持图像理解，多模态能力
      max-tokens: 4096
      support-thinking: true
      support-image: true
      temperature: 0.7
      top-p: 0.95

upload:
  path: ./uploads/ai-images
  max-size: 52428800  # 50MB
```

### 4.2 MiMoModelConfig.java

```java
package com.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

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
    
    // Getters and Setters
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    public List<ModelInfo> getModels() { return models; }
    public void setModels(List<ModelInfo> models) { this.models = models; }
}
```

---

## 五、实体类设计

### 5.1 AiConversation.java

```java
package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_conversation")
public class AiConversation extends BaseEntity {
    
    private Long userId;
    
    private String title;
    
    private Long articleId;
    
    private String modelId;
    
    private Boolean thinkingEnabled;
}
```

### 5.2 AiMessage.java

```java
package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.blog.common.BaseEntity;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_message", autoResultMap = true)
public class AiMessage extends BaseEntity {
    
    private Long conversationId;
    
    private String role;
    
    private String content;
    
    private String thinkingContent;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> images;
    
    private Integer tokensUsed;
}
```

### 5.3 AiImage.java

```java
package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.blog.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_image")
public class AiImage extends BaseEntity {
    
    private Long userId;
    
    private String filename;
    
    private String originalName;
    
    private String mimeType;
    
    private Long size;
    
    private String url;
}
```

---

## 六、DTO 设计

### 6.1 ChatRequest.java

```java
package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
public class ChatRequest {
    
    @NotNull(message = "对话ID不能为空")
    private Long conversationId;
    
    @NotNull(message = "消息内容不能为空")
    private String message;
    
    private String modelId;
    
    private Boolean thinkingEnabled;
    
    private List<String> imageUrls;
    
    private List<String> imageBase64s;
    
    private List<Map<String, String>> history;
}
```

### 6.2 ModelResponse.java

```java
package com.blog.dto;

import lombok.Data;

@Data
public class ModelResponse {
    private String id;
    private String name;
    private String description;
    private Boolean supportThinking;
    private Boolean supportImage;
}
```

### 6.3 ImageUploadResponse.java

```java
package com.blog.dto;

import lombok.Data;

@Data
public class ImageUploadResponse {
    private Long id;
    private String url;
    private String mimeType;
    private Long size;
}
```

### 6.4 CreateConversationRequest.java

```java
package com.blog.dto;

import lombok.Data;

@Data
public class CreateConversationRequest {
    private String title;
    private String modelId;
    private Boolean thinkingEnabled;
    private Long articleId;
}
```

---

## 七、Controller 设计

### AiChatController.java

```java
package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.*;
import com.blog.entity.AiConversation;
import com.blog.entity.AiMessage;
import com.blog.service.AiChatService;
import com.blog.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI 助手", description = "AI 助手相关接口")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final ImageService imageService;
    private final HttpServletRequest request;

    private Long getCurrentUserId() {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                // 解析 token 获取 userId
                // TODO: 实现 token 解析
            }
        } catch (Exception ignored) {}
        return null;
    }

    @GetMapping("/models")
    @Operation(summary = "获取可用模型列表")
    public Result<List<ModelResponse>> getModels() {
        return Result.success(aiChatService.getModels());
    }

    @PostMapping("/chat")
    @Operation(summary = "流式聊天（支持图像）")
    public SseEmitter chat(@RequestBody @Valid ChatRequest request) {
        SseEmitter emitter = new SseEmitter(180000L); // 3分钟超时
        
        CompletableFuture.runAsync(() -> {
            try {
                aiChatService.streamChat(request, emitter);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }

    @PostMapping("/upload-image")
    @Operation(summary = "上传图片")
    public Result<ImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        ImageUploadResponse response = imageService.uploadImage(file, userId);
        return Result.created(response);
    }

    @GetMapping("/conversations")
    @Operation(summary = "获取对话列表")
    public Result<List<AiConversation>> getConversations() {
        Long userId = getCurrentUserId();
        return Result.success(aiChatService.getConversations(userId));
    }

    @PostMapping("/conversations")
    @Operation(summary = "创建新对话")
    public Result<AiConversation> createConversation(
            @RequestBody CreateConversationRequest request) {
        Long userId = getCurrentUserId();
        return Result.created(aiChatService.createConversation(userId, request));
    }

    @GetMapping("/history/{conversationId}")
    @Operation(summary = "获取对话历史")
    public Result<List<AiMessage>> getHistory(@PathVariable Long conversationId) {
        return Result.success(aiChatService.getHistory(conversationId));
    }

    @DeleteMapping("/history/{conversationId}")
    @Operation(summary = "删除对话")
    public Result<Void> deleteConversation(@PathVariable Long conversationId) {
        aiChatService.deleteConversation(conversationId);
        return Result.noContent();
    }
}
```

---

## 八、Service 设计

### 8.1 AiChatService.java

```java
package com.blog.service;

import com.blog.config.MiMoModelConfig;
import com.blog.dto.*;
import com.blog.entity.AiConversation;
import com.blog.entity.AiMessage;
import com.blog.exception.BusinessException;
import com.blog.mapper.AiConversationMapper;
import com.blog.mapper.AiMessageMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final MiMoModelConfig modelConfig;
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 获取可用模型列表
     */
    public List<ModelResponse> getModels() {
        return modelConfig.getModels().stream()
            .map(m -> {
                ModelResponse response = new ModelResponse();
                response.setId(m.getId());
                response.setName(m.getName());
                response.setDescription(m.getDescription());
                response.setSupportThinking(m.getSupportThinking());
                response.setSupportImage(m.getSupportImage());
                return response;
            })
            .toList();
    }

    /**
     * 流式聊天
     */
    public void streamChat(ChatRequest request, SseEmitter emitter) {
        try {
            // 1. 获取对话信息
            AiConversation conversation = conversationMapper.selectById(request.getConversationId());
            if (conversation == null) {
                throw new BusinessException("对话不存在");
            }

            // 2. 确定使用的模型
            String modelId = request.getModelId() != null 
                ? request.getModelId() 
                : conversation.getModelId();
            MiMoModelConfig.ModelInfo modelInfo = modelConfig.getModel(modelId);
            if (modelInfo == null) {
                throw new BusinessException("模型不存在");
            }

            // 3. 验证图像支持
            if (hasImages(request) && !modelInfo.getSupportImage()) {
                throw new BusinessException("当前模型不支持图像输入，请选择 MiMo V2.5 模型");
            }

            // 4. 保存用户消息
            saveMessage(request.getConversationId(), "user", request.getMessage(), request);

            // 5. 构建请求体
            Map<String, Object> requestBody = buildRequestBody(request, modelInfo, conversation);

            // 6. 流式输出
            streamToMiMo(requestBody, request.getConversationId(), emitter);

        } catch (Exception e) {
            log.error("聊天失败", e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(
            ChatRequest request,
            MiMoModelConfig.ModelInfo modelInfo,
            AiConversation conversation) {
        
        List<Map<String, Object>> messages = new ArrayList<>();

        // System Prompt
        messages.add(Map.of(
            "role", "system",
            "content", buildSystemPrompt(request, conversation)
        ));

        // 历史消息
        if (request.getHistory() != null) {
            request.getHistory().forEach(msg -> messages.add(new HashMap<>(msg)));
        }

        // 当前用户消息（可能包含图像）
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        if (hasImages(request)) {
            // 多模态消息：图像 + 文本
            List<Map<String, Object>> content = new ArrayList<>();

            // 添加图像 URL
            if (request.getImageUrls() != null) {
                for (String url : request.getImageUrls()) {
                    content.add(Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", url)
                    ));
                }
            }

            // 添加 Base64 图像
            if (request.getImageBase64s() != null) {
                for (String base64 : request.getImageBase64s()) {
                    content.add(Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", base64)
                    ));
                }
            }

            // 添加文本
            content.add(Map.of(
                "type", "text",
                "text", request.getMessage()
            ));

            userMessage.put("content", content);
        } else {
            // 纯文本消息
            userMessage.put("content", request.getMessage());
        }

        messages.add(userMessage);

        // 构建请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelInfo.getId());
        body.put("messages", messages);
        body.put("stream", true);
        body.put("max_completion_tokens", modelInfo.getMaxTokens());
        body.put("temperature", modelInfo.getTemperature());
        body.put("top_p", modelInfo.getTopP());

        // 思考模式
        Boolean thinkingEnabled = request.getThinkingEnabled() != null
            ? request.getThinkingEnabled()
            : conversation.getThinkingEnabled();

        if (thinkingEnabled && modelInfo.getSupportThinking()) {
            body.put("thinking", Map.of("type", "enabled"));
        } else {
            body.put("thinking", Map.of("type", "disabled"));
        }

        return body;
    }

    /**
     * 构建 System Prompt
     */
    private String buildSystemPrompt(ChatRequest request, AiConversation conversation) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 BlogHub 的 AI 助手，一个专业技术博客的智能助手。\n\n");
        prompt.append("你的特点：\n");
        prompt.append("1. 专注于技术内容，擅长编程、开发、架构等领域\n");
        prompt.append("2. 回答简洁明了，结构清晰\n");
        prompt.append("3. 支持 Markdown 格式输出\n");
        prompt.append("4. 可以根据博客内容提供个性化回答\n");
        prompt.append("5. 如果用户上传了图片，请仔细分析图片内容并给出相关建议\n\n");

        if (conversation.getArticleId() != null) {
            prompt.append("当前文章ID：").append(conversation.getArticleId()).append("\n");
        }

        return prompt.toString();
    }

    /**
     * 流式输出到 MiMo API
     */
    private void streamToMiMo(
            Map<String, Object> requestBody,
            Long conversationId,
            SseEmitter emitter) throws Exception {
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", modelConfig.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        StringBuilder fullContent = new StringBuilder();
        StringBuilder thinkingContent = new StringBuilder<>();

        restTemplate.execute(
            modelConfig.getApiUrl(),
            HttpMethod.POST,
            req -> {
                req.getHeaders().putAll(headers);
                objectMapper.writeValue(req.getBody(), requestBody);
            },
            response -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getBody()))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if (data.equals("[DONE]")) {
                                break;
                            }

                            try {
                                JsonNode jsonNode = objectMapper.readTree(data);
                                JsonNode choice = jsonNode.path("choices").path(0);

                                // 处理普通内容
                                String delta = choice.path("delta").path("content").asText("");
                                if (!delta.isEmpty()) {
                                    fullContent.append(delta);
                                    emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(Map.of("type", "content", "content", delta)));
                                }

                                // 处理思考内容
                                String thinking = choice.path("delta")
                                    .path("reasoning_content").asText("");
                                if (!thinking.isEmpty()) {
                                    thinkingContent.append(thinking);
                                    emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(Map.of("type", "thinking", "content", thinking)));
                                }
                            } catch (Exception e) {
                                log.error("解析响应失败", e);
                            }
                        }
                    }

                    // 保存助手消息
                    saveMessageWithThinking(
                        conversationId,
                        "assistant",
                        fullContent.toString(),
                        thinkingContent.toString()
                    );

                    emitter.send(SseEmitter.event().name("done").data(""));
                    emitter.complete();

                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }
        );
    }

    /**
     * 检查是否包含图像
     */
    private boolean hasImages(ChatRequest request) {
        return (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) ||
               (request.getImageBase64s() != null && !request.getImageBase64s().isEmpty());
    }

    /**
     * 保存消息
     */
    private void saveMessage(Long conversationId, String role, String content, ChatRequest request) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);

        // 保存图片信息
        if (hasImages(request)) {
            Map<String, Object> imagesInfo = new HashMap<>();
            if (request.getImageUrls() != null) {
                imagesInfo.put("urls", request.getImageUrls());
            }
            if (request.getImageBase64s() != null) {
                imagesInfo.put("base64Count", request.getImageBase64s().size());
            }
            message.setImages(imagesInfo);
        }

        messageMapper.insert(message);

        // 更新对话时间
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
    }

    /**
     * 保存带思考内容的消息
     */
    private void saveMessageWithThinking(
            Long conversationId,
            String role,
            String content,
            String thinkingContent) {
        
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setThinkingContent(thinkingContent);
        messageMapper.insert(message);

        // 更新对话时间
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
    }

    /**
     * 获取对话列表
     */
    public List<AiConversation> getConversations(Long userId) {
        return conversationMapper.selectList(
            new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .orderByDesc(AiConversation::getUpdatedAt)
        );
    }

    /**
     * 获取对话历史
     */
    public List<AiMessage> getHistory(Long conversationId) {
        return messageMapper.selectList(
            new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .orderByAsc(AiMessage::getCreatedAt)
        );
    }

    /**
     * 创建对话
     */
    public AiConversation createConversation(Long userId, CreateConversationRequest request) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(request.getTitle() != null ? request.getTitle() : "新对话");
        conversation.setModelId(request.getModelId() != null 
            ? request.getModelId() 
            : modelConfig.getDefaultModel());
        conversation.setThinkingEnabled(request.getThinkingEnabled() != null 
            ? request.getThinkingEnabled() 
            : false);
        conversation.setArticleId(request.getArticleId());
        conversationMapper.insert(conversation);
        return conversation;
    }

    /**
     * 删除对话
     */
    public void deleteConversation(Long conversationId) {
        conversationMapper.deleteById(conversationId);
    }
}
```

### 8.2 ImageService.java

```java
package com.blog.service;

import com.blog.dto.ImageUploadResponse;
import com.blog.entity.AiImage;
import com.blog.exception.BusinessException;
import com.blog.mapper.AiImageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    @Value("${upload.path:./uploads/ai-images}")
    private String uploadPath;

    @Value("${upload.max-size:52428800}")
    private Long maxSize;

    private final AiImageMapper imageMapper;

    /**
     * 上传图片
     */
    public ImageUploadResponse uploadImage(MultipartFile file, Long userId) {
        // 1. 验证文件
        validateFile(file);

        // 2. 生成唯一文件名
        String filename = generateFilename(file);

        // 3. 保存文件
        String url = saveFile(file, filename);

        // 4. 保存到数据库
        AiImage image = new AiImage();
        image.setUserId(userId);
        image.setFilename(filename);
        image.setOriginalName(file.getOriginalFilename());
        image.setMimeType(file.getContentType());
        image.setSize(file.getSize());
        image.setUrl(url);
        imageMapper.insert(image);

        // 5. 返回响应
        ImageUploadResponse response = new ImageUploadResponse();
        response.setId(image.getId());
        response.setUrl(url);
        response.setMimeType(file.getContentType());
        response.setSize(file.getSize());

        return response;
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小不能超过 50MB");
        }

        String contentType = file.getContentType();
        if (!isValidImageType(contentType)) {
            throw new BusinessException("不支持的图片格式，请上传 JPEG, PNG, GIF, WebP 或 BMP 格式");
        }
    }

    /**
     * 验证图片类型
     */
    private boolean isValidImageType(String contentType) {
        return contentType != null && (
            contentType.equals("image/jpeg") ||
            contentType.equals("image/png") ||
            contentType.equals("image/gif") ||
            contentType.equals("image/webp") ||
            contentType.equals("image/bmp")
        );
    }

    /**
     * 生成文件名
     */
    private String generateFilename(MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        return UUID.randomUUID().toString() + ext;
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int lastDot = filename.lastIndexOf('.');
        return lastDot >= 0 ? filename.substring(lastDot) : ".jpg";
    }

    /**
     * 保存文件
     */
    private String saveFile(MultipartFile file, String filename) {
        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path filePath = uploadDir.resolve(filename);
            file.transferTo(filePath.toFile());

            return "/uploads/ai-images/" + filename;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException("文件保存失败", e);
        }
    }
}
```

---

## 九、前端实现

### 9.1 api/aiApi.js

```javascript
import request from './request'

export const aiApi = {
  // 获取可用模型列表
  getModels() {
    return request.get('/api/ai/models')
  },

  // 流式聊天
  chat(data) {
    return fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('blog_token')}`
      },
      body: JSON.stringify(data)
    })
  },

  // 上传图片
  uploadImage(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/api/ai/upload-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 获取对话列表
  getConversations() {
    return request.get('/api/ai/conversations')
  },

  // 创建对话
  createConversation(data) {
    return request.post('/api/ai/conversations', data)
  },

  // 获取对话历史
  getHistory(conversationId) {
    return request.get(`/api/ai/history/${conversationId}`)
  },

  // 删除对话
  deleteConversation(conversationId) {
    return request.delete(`/api/ai/history/${conversationId}`)
  }
}
```

### 9.2 composables/useAiChat.js

```javascript
import { ref } from 'vue'
import { aiApi } from '@/api/aiApi'

export function useAiChat() {
  const isGenerating = ref(false)
  const currentContent = ref('')
  const currentThinking = ref('')

  /**
   * 流式聊天
   */
  async function streamChat(request) {
    isGenerating.value = true
    currentContent.value = ''
    currentThinking.value = ''

    try {
      const response = await aiApi.chat(request)
      const reader = response.body.getReader()
      const decoder = new TextDecoder()

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        const chunk = decoder.decode(value)
        const lines = chunk.split('\n')

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6)
            if (data === '' || data === '[DONE]') break

            try {
              const parsed = JSON.parse(data)
              if (parsed.type === 'content') {
                currentContent.value += parsed.content
              } else if (parsed.type === 'thinking') {
                currentThinking.value += parsed.content
              }
            } catch (e) {
              console.error('解析响应失败:', e)
            }
          }
        }
      }

      return {
        content: currentContent.value,
        thinking: currentThinking.value
      }
    } catch (error) {
      console.error('聊天失败:', error)
      throw error
    } finally {
      isGenerating.value = false
    }
  }

  /**
   * 上传图片
   */
  async function uploadImage(file) {
    const { data } = await aiApi.uploadImage(file)
    return data
  }

  /**
   * 中断生成
   */
  function stopGeneration() {
    // TODO: 实现中断逻辑
    isGenerating.value = false
  }

  return {
    isGenerating,
    currentContent,
    currentThinking,
    streamChat,
    uploadImage,
    stopGeneration
  }
}
```

### 9.3 views/AiAssistant.vue

```vue
<template>
  <div class="ai-assistant">
    <!-- 对话历史侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h3>对话历史</h3>
        <el-button type="primary" @click="createNewConversation">
          <el-icon><Plus /></el-icon> 新对话
        </el-button>
      </div>
      <ChatHistory 
        :conversations="conversations"
        :current-id="currentConversationId"
        @select="selectConversation"
        @delete="deleteConversation"
      />
    </aside>

    <!-- 主对话区域 -->
    <main class="chat-main">
      <!-- 设置栏 -->
      <div class="settings-bar">
        <div class="model-selector">
          <span class="label">模型：</span>
          <el-select v-model="selectedModel" placeholder="选择模型">
            <el-option
              v-for="model in models"
              :key="model.id"
              :label="model.name"
              :value="model.id"
            >
              <div class="model-option">
                <span class="model-name">{{ model.name }}</span>
                <span class="model-desc">{{ model.description }}</span>
                <span v-if="model.supportImage" class="model-badge image">📷 图像</span>
                <span v-if="model.supportThinking" class="model-badge thinking">🧠 思考</span>
              </div>
            </el-option>
          </el-select>
        </div>
        
        <div class="thinking-toggle" v-if="currentModelSupportThinking">
          <span class="label">思考模式：</span>
          <el-switch v-model="thinkingEnabled" />
          <el-tooltip content="开启后AI会先进行深度思考，再给出回答" placement="top">
            <el-icon><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
      </div>

      <!-- 消息区域 -->
      <div class="messages-container" ref="messagesRef">
        <ChatMessage 
          v-for="msg in messages" 
          :key="msg.id"
          :message="msg"
          :show-thinking="thinkingEnabled"
        />
        
        <!-- 流式输出 -->
        <div v-if="isGenerating" class="message assistant">
          <div v-if="currentThinking" class="thinking-block">
            <div class="thinking-header">
              <el-icon><Loading /></el-icon>
              <span>思考中...</span>
            </div>
            <div class="thinking-content">{{ currentThinking }}</div>
          </div>
          <StreamingText :content="currentContent" />
        </div>
      </div>

      <!-- 图片预览 -->
      <div v-if="pendingImages.length > 0" class="image-preview">
        <div v-for="(img, index) in pendingImages" :key="index" class="preview-item">
          <img :src="img.preview" alt="预览" />
          <el-button 
            type="danger" 
            size="small" 
            circle
            @click="removeImage(index)"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <QuickActions @action="handleQuickAction" />
        <div class="input-box">
          <el-tooltip :content="currentModelSupportImage ? '上传图片' : '当前模型不支持图像'" placement="top">
            <el-button 
              @click="triggerImageUpload"
              :disabled="!currentModelSupportImage"
            >
              <el-icon><Picture /></el-icon>
            </el-button>
          </el-tooltip>
          
          <input 
            ref="fileInput"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
            multiple
            style="display: none"
            @change="handleImageUpload"
          />
          
          <el-input 
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            placeholder="输入你的问题... (Ctrl+Enter 发送)"
            @keydown.enter.ctrl="sendMessage"
          />
          
          <el-button 
            type="primary" 
            @click="sendMessage"
            :loading="isGenerating"
            :disabled="!inputMessage.trim() && pendingImages.length === 0"
          >
            {{ isGenerating ? '生成中...' : '发送' }}
          </el-button>
          
          <el-button 
            v-if="isGenerating"
            type="danger" 
            @click="stopGeneration"
          >
            停止
          </el-button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Plus, Close, Loading, Picture, QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { aiApi } from '@/api/aiApi'
import { useAiChat } from '@/composables/useAiChat'
import ChatMessage from '@/components/ChatMessage.vue'
import ChatHistory from '@/components/ChatHistory.vue'
import StreamingText from '@/components/StreamingText.vue'
import QuickActions from '@/components/QuickActions.vue'

const { 
  isGenerating, 
  currentContent, 
  currentThinking,
  streamChat, 
  uploadImage,
  stopGeneration
} = useAiChat()

const models = ref([])
const selectedModel = ref('mimo-v2.5-pro')
const thinkingEnabled = ref(false)

const conversations = ref([])
const currentConversationId = ref(null)
const messages = ref([])
const inputMessage = ref('')
const messagesRef = ref(null)
const fileInput = ref(null)
const pendingImages = ref([])

// 当前模型是否支持图像
const currentModelSupportImage = computed(() => {
  const model = models.value.find(m => m.id === selectedModel.value)
  return model?.supportImage || false
})

// 当前模型是否支持思考
const currentModelSupportThinking = computed(() => {
  const model = models.value.find(m => m.id === selectedModel.value)
  return model?.supportThinking || false
})

// 监听模型切换
watch(selectedModel, (newModel) => {
  const model = models.value.find(m => m.id === newModel)
  if (!model?.supportThinking) {
    thinkingEnabled.value = false
  }
  if (!model?.supportImage) {
    pendingImages.value = []
  }
})

// 加载数据
onMounted(async () => {
  try {
    const [modelsRes, conversationsRes] = await Promise.all([
      aiApi.getModels(),
      aiApi.getConversations()
    ])
    
    models.value = modelsRes.data
    conversations.value = conversationsRes.data
    
    if (conversations.value.length > 0) {
      selectConversation(conversations.value[0].id)
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  }
})

// 触发图片上传
function triggerImageUpload() {
  fileInput.value.click()
}

// 处理图片上传
async function handleImageUpload(event) {
  const files = event.target.files
  if (!files.length) return
  
  for (const file of files) {
    if (file.size > 50 * 1024 * 1024) {
      ElMessage.error('图片大小不能超过 50MB')
      continue
    }
    
    const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp']
    if (!validTypes.includes(file.type)) {
      ElMessage.error('不支持的图片格式')
      continue
    }
    
    const preview = URL.createObjectURL(file)
    pendingImages.value.push({ file, preview })
  }
  
  event.target.value = ''
}

// 移除图片
function removeImage(index) {
  const img = pendingImages.value[index]
  URL.revokeObjectURL(img.preview)
  pendingImages.value.splice(index, 1)
}

// 选择对话
async function selectConversation(id) {
  currentConversationId.value = id
  const conversation = conversations.value.find(c => c.id === id)
  if (conversation) {
    selectedModel.value = conversation.modelId
    thinkingEnabled.value = conversation.thinkingEnabled
  }
  
  const { data } = await aiApi.getHistory(id)
  messages.value = data
  scrollToBottom()
}

// 发送消息
async function sendMessage() {
  if (!inputMessage.value.trim() && pendingImages.value.length === 0) return
  if (isGenerating.value) return

  if (!currentConversationId.value) {
    const { data } = await aiApi.createConversation({
      title: inputMessage.value.slice(0, 50) || '图片对话',
      modelId: selectedModel.value,
      thinkingEnabled: thinkingEnabled.value
    })
    currentConversationId.value = data.id
    conversations.value.unshift(data)
  }

  // 上传图片
  const imageUrls = []
  for (const img of pendingImages.value) {
    try {
      const result = await uploadImage(img.file)
      imageUrls.push(result.url)
    } catch (error) {
      console.error('图片上传失败:', error)
      ElMessage.error('图片上传失败')
    }
  }

  const messageData = {
    conversationId: currentConversationId.value,
    message: inputMessage.value,
    modelId: selectedModel.value,
    thinkingEnabled: thinkingEnabled.value
  }
  
  if (imageUrls.length > 0) {
    messageData.imageUrls = imageUrls
  }

  messages.value.push({
    role: 'user',
    content: inputMessage.value,
    images: imageUrls,
    timestamp: Date.now()
  })

  const userMessage = inputMessage.value
  inputMessage.value = ''
  pendingImages.value = []
  scrollToBottom()

  const result = await streamChat(messageData)

  messages.value.push({
    role: 'assistant',
    content: result.content,
    thinkingContent: result.thinking,
    timestamp: Date.now()
  })

  scrollToBottom()
}

// 创建新对话
async function createNewConversation() {
  const { data } = await aiApi.createConversation({
    title: '新对话',
    modelId: selectedModel.value,
    thinkingEnabled: thinkingEnabled.value
  })
  conversations.value.unshift(data)
  currentConversationId.value = data.id
  messages.value = []
}

// 删除对话
async function deleteConversation(id) {
  await aiApi.deleteConversation(id)
  conversations.value = conversations.value.filter(c => c.id !== id)
  if (currentConversationId.value === id) {
    currentConversationId.value = conversations.value[0]?.id || null
    messages.value = []
  }
}

// 快捷操作
function handleQuickAction(action) {
  const prompts = {
    summarize: '请总结这篇文章的要点',
    explain: '请解释这段代码的含义',
    optimize: '请帮我优化这段写作',
    image: '请描述这张图片的内容'
  }
  inputMessage.value = prompts[action] || ''
}

// 滚动到底部
function scrollToBottom() {
  setTimeout(() => {
    messagesRef.value?.scrollTo(0, messagesRef.value.scrollHeight)
  }, 100)
}
</script>

<style scoped>
.ai-assistant {
  display: flex;
  height: calc(100vh - 60px);
}

.sidebar {
  width: 280px;
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-light);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.settings-bar {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
}

.model-selector,
.thinking-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.model-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.model-name {
  font-weight: 500;
}

.model-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.model-badge {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
}

.model-badge.image {
  background: rgba(64, 158, 255, 0.1);
  color: var(--el-color-primary);
}

.model-badge.thinking {
  background: rgba(230, 162, 60, 0.1);
  color: var(--el-color-warning);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.thinking-block {
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: var(--el-color-warning);
}

.thinking-content {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  white-space: pre-wrap;
}

.image-preview {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-top: 1px solid var(--el-border-color-light);
}

.preview-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
}

.preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-item .el-button {
  position: absolute;
  top: 4px;
  right: 4px;
}

.input-area {
  padding: 16px;
  border-top: 1px solid var(--el-border-color-light);
}

.input-box {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.input-box .el-input {
  flex: 1;
}
</style>
```

---

## 十、路由配置

```javascript
// router/index.js
{
  path: 'ai-assistant',
  name: 'AiAssistant',
  component: () => import('@/views/AiAssistant.vue'),
  meta: { title: 'AI 助手', requiresAuth: true }
}
```

---

## 十一、环境变量

```env
# .env.development
VITE_MIMO_API_KEY=your_mimo_api_key_here

# .env.production
VITE_MIMO_API_KEY=${MIMO_API_KEY}
```

---

## 十二、API 接口汇总

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/ai/models` | GET | 获取可用模型列表 |
| `/api/ai/chat` | POST | 流式聊天（支持图像） |
| `/api/ai/upload-image` | POST | 上传图片 |
| `/api/ai/conversations` | GET | 获取对话列表 |
| `/api/ai/conversations` | POST | 创建新对话 |
| `/api/ai/history/{id}` | GET | 获取对话历史 |
| `/api/ai/history/{id}` | DELETE | 删除对话 |

---

## 十三、注意事项

1. **API_KEY 安全**: 密钥存储在服务器环境变量中，前端无法直接访问
2. **图片大小限制**: 单张图片不超过 50MB
3. **图片格式支持**: JPEG, PNG, GIF, WebP, BMP
4. **思考模式**: 仅 MiMo V2.5 Pro 和 MiMo V2.5 支持
5. **图像输入**: 仅 MiMo V2.5 支持
6. **流式输出**: 使用 SSE (Server-Sent Events) 实现
7. **对话历史**: 存储在数据库中，支持持久化

---

## 十四、待实现功能

- [ ] 对话导出功能
- [ ] 对话分享功能
- [ ] 更多快捷操作
- [ ] 对话重命名
- [ ] 使用量统计

package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.config.MiMoModelConfig;
import com.blog.dto.ChatRequest;
import com.blog.entity.AiConversation;
import com.blog.entity.AiMessage;
import com.blog.exception.BusinessException;
import com.blog.mapper.AiConversationMapper;
import com.blog.mapper.AiMessageMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 聊天核心服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private final MiMoModelConfig mimoConfig;
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    /**
     * 获取可用模型列表
     */
    public List<Map<String, Object>> getModels() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (MiMoModelConfig.ModelInfo model : mimoConfig.getModels()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", model.getId());
            m.put("name", model.getName());
            m.put("apiModel", model.getApiModel());
            m.put("description", model.getDescription());
            m.put("supportThinking", model.getSupportThinking());
            m.put("supportImage", model.getSupportImage());
            result.add(m);
        }
        return result;
    }

    /**
     * 创建对话
     */
    public AiConversation createConversation(Long userId, String title, String modelId, Boolean thinkingEnabled, Long articleId) {
        AiConversation conv = new AiConversation();
        conv.setUserId(userId);
        conv.setTitle(title != null ? title : "新对话");
        conv.setModelId(modelId != null ? modelId : mimoConfig.getDefaultModel());
        conv.setThinkingEnabled(thinkingEnabled != null && thinkingEnabled);
        conv.setArticleId(articleId);
        conversationMapper.insert(conv);
        log.info("创建AI对话: id={}, userId={}", conv.getId(), userId);
        return conv;
    }

    /**
     * 获取用户对话列表
     */
    public List<AiConversation> getConversations(Long userId) {
        return conversationMapper.selectList(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .orderByDesc(AiConversation::getUpdatedAt));
    }

    /**
     * 获取对话历史消息
     */
    public List<AiMessage> getHistory(Long conversationId, Long userId) {
        // 校验归属
        AiConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new BusinessException(404, "对话不存在");
        }
        return messageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByAsc(AiMessage::getCreatedAt));
    }

    /**
     * 删除对话
     */
    public void deleteConversation(Long conversationId, Long userId) {
        AiConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new BusinessException(404, "对话不存在");
        }
        conversationMapper.deleteById(conversationId);
        log.info("删除AI对话: id={}", conversationId);
    }

    /**
     * 流式聊天 — 使用 SSE 推送
     */
    public void streamChat(ChatRequest request, Long userId, SseEmitter emitter) {
        java.net.HttpURLConnection conn = null;
        try {
            MiMoModelConfig.ModelInfo modelInfo = mimoConfig.getModel(request.getModelId());
            if (modelInfo == null) {
                emitter.send(SseEmitter.event().name("error").data("模型不存在"));
                emitter.complete();
                return;
            }

            // 1. 保存用户消息
            AiMessage userMsg = new AiMessage();
            userMsg.setConversationId(request.getConversationId());
            userMsg.setRole("user");
            userMsg.setContent(request.getMessage());
            if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
                Map<String, Object> imgMap = new java.util.HashMap<>();
                imgMap.put("urls", request.getImageUrls());
                userMsg.setImages(imgMap);
            }
            messageMapper.insert(userMsg);

            // 2. 构建请求体
            ObjectNode body = buildRequestBody(request, modelInfo);
            String jsonBody = objectMapper.writeValueAsString(body);
            log.debug("AI 请求体: {}", jsonBody);

            // 3. 使用 HttpURLConnection 真流式调用 MiMo API
            java.net.URL url = new java.net.URL(mimoConfig.getApiUrl());
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("api-key", mimoConfig.getApiKey());
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);
            // 关键：禁用缓冲
            conn.setChunkedStreamingMode(0);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int httpCode = conn.getResponseCode();
            if (httpCode != 200) {
                // 读取错误信息
                java.io.InputStream errStream = conn.getErrorStream();
                String errBody = "";
                if (errStream != null) {
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = errStream.read(buf)) != -1) baos.write(buf, 0, len);
                    errBody = baos.toString("UTF-8");
                }
                log.error("MiMo API 返回 {}: {}", httpCode, errBody);
                emitter.send(SseEmitter.event().name("error").data("API 错误: " + httpCode));
                emitter.complete();
                return;
            }

            // 4. 逐行读取 SSE 流
            StringBuilder contentBuilder = new StringBuilder();
            StringBuilder thinkingBuilder = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    if (!line.startsWith("data: ")) continue;

                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;

                    try {
                        JsonNode json = objectMapper.readTree(data);
                        JsonNode choices = json.get("choices");
                        if (choices != null && choices.size() > 0) {
                            JsonNode delta = choices.get(0).get("delta");
                            if (delta != null) {
                                // 普通内容
                                JsonNode content = delta.get("content");
                                if (content != null && !content.isNull()) {
                                    String text = content.asText();
                                    contentBuilder.append(text);
                                    emitter.send(SseEmitter.event().name("content").data(text));
                                }
                                // 思考内容
                                JsonNode reasoning = delta.get("reasoning_content");
                                if (reasoning != null && !reasoning.isNull()) {
                                    String text = reasoning.asText();
                                    thinkingBuilder.append(text);
                                    emitter.send(SseEmitter.event().name("thinking").data(text));
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            // 5. 保存助手消息
            AiMessage assistantMsg = new AiMessage();
            assistantMsg.setConversationId(request.getConversationId());
            assistantMsg.setRole("assistant");
            assistantMsg.setContent(contentBuilder.toString());
            if (thinkingBuilder.length() > 0) {
                assistantMsg.setThinkingContent(thinkingBuilder.toString());
            }
            messageMapper.insert(assistantMsg);

            // 6. 完成
            emitter.send(SseEmitter.event().name("done").data("ok"));
            emitter.complete();

        } catch (Exception e) {
            log.error("AI 聊天失败: {}", e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event().name("error").data("生成失败: " + e.getMessage()));
                emitter.complete();
            } catch (Exception ignored) {}
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * 构建 OpenAI 兼容的请求体
     */
    private ObjectNode buildRequestBody(ChatRequest request, MiMoModelConfig.ModelInfo modelInfo) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", modelInfo.getApiModel() != null ? modelInfo.getApiModel() : request.getModelId());
        body.put("stream", true);
        body.put("temperature", modelInfo.getTemperature());
        body.put("top_p", modelInfo.getTopP());
        body.put("max_tokens", modelInfo.getMaxTokens());

        // 如果模型支持思考
        if (modelInfo.getSupportThinking() && Boolean.TRUE.equals(request.getThinkingEnabled())) {
            body.put("enable_thinking", true);
        }

        // 构建 messages
        ArrayNode messages = objectMapper.createArrayNode();

        // 历史消息
        if (request.getHistory() != null) {
            for (Map<String, String> msg : request.getHistory()) {
                ObjectNode m = objectMapper.createObjectNode();
                m.put("role", msg.get("role"));
                m.put("content", msg.get("content"));
                messages.add(m);
            }
        }

        // 当前用户消息
        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");

        // 处理图片
        boolean hasImages = false;
        ArrayNode contentArr = null;
        if (modelInfo.getSupportImage()) {
            contentArr = objectMapper.createArrayNode();
            ObjectNode textPart = objectMapper.createObjectNode();
            textPart.put("type", "text");
            textPart.put("text", request.getMessage());
            contentArr.add(textPart);

            // 优先使用 base64
            if (request.getImageBase64s() != null && !request.getImageBase64s().isEmpty()) {
                for (String base64 : request.getImageBase64s()) {
                    ObjectNode imgPart = objectMapper.createObjectNode();
                    imgPart.put("type", "image_url");
                    ObjectNode imgUrl = objectMapper.createObjectNode();
                    imgUrl.put("url", "data:image/jpeg;base64," + base64);
                    imgPart.set("image_url", imgUrl);
                    contentArr.add(imgPart);
                    hasImages = true;
                }
            }
            // 其次使用 URL，读取文件转 base64
            if (!hasImages && request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
                for (String url : request.getImageUrls()) {
                    try {
                        String base64 = convertUrlToBase64(url);
                        if (base64 != null) {
                            ObjectNode imgPart = objectMapper.createObjectNode();
                            imgPart.put("type", "image_url");
                            ObjectNode imgUrl = objectMapper.createObjectNode();
                            imgUrl.put("url", "data:image/jpeg;base64," + base64);
                            imgPart.set("image_url", imgUrl);
                            contentArr.add(imgPart);
                            hasImages = true;
                        }
                    } catch (Exception e) {
                        log.warn("图片转 base64 失败: {}", url, e);
                    }
                }
            }
        }
            userMsg.set("content", contentArr);
        if (hasImages && contentArr != null) {
            userMsg.set("content", contentArr);
        } else {
            userMsg.put("content", request.getMessage());
        }

        messages.add(userMsg);
        body.set("messages", messages);

        return body;
    }

    /**
     * 将 URL/路径转为 base64 字符串
     */
    private String convertUrlToBase64(String url) {
        try {
            java.io.InputStream is;
            if (url.startsWith("http")) {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                is = conn.getInputStream();
            } else {
                // 本地相对路径，如 /uploads/ai-images/xxx.png
                String path = url.startsWith("/") ? url.substring(1) : url;
                java.io.File file = new java.io.File(path);
                if (!file.exists()) {
                    // 尝试从 upload.dir 拼接
                    file = new java.io.File("./" + path);
                }
                if (!file.exists()) {
                    log.warn("图片文件不存在: {}", url);
                    return null;
                }
                is = new java.io.FileInputStream(file);
            }
            byte[] bytes;
            try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
                bytes = baos.toByteArray();
            }
            is.close();
            return java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.warn("图片转 base64 失败: {}", url, e);
            return null;
        }
    }
}

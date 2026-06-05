package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.ChatRequest;
import com.blog.dto.CreateConversationRequest;
import com.blog.dto.ImageUploadResponse;
import com.blog.entity.AiConversation;
import com.blog.entity.AiMessage;
import com.blog.service.AiChatService;
import com.blog.service.AiImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * AI 助手控制器
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI 助手", description = "AI 对话、图片上传、对话管理")
@Slf4j
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiImageService aiImageService;

    @Operation(summary = "获取可用模型列表")
    @GetMapping("/models")
    public Result<List<Map<String, Object>>> getModels() {
        return Result.success(aiChatService.getModels());
    }

    @Operation(summary = "创建新对话")
    @PostMapping("/conversations")
    public Result<AiConversation> createConversation(
            @Valid @RequestBody CreateConversationRequest body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.created(aiChatService.createConversation(
                userId, body.getTitle(), body.getModelId(),
                body.getThinkingEnabled(), body.getArticleId()));
    }

    @Operation(summary = "获取对话列表")
    @GetMapping("/conversations")
    public Result<List<AiConversation>> getConversations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(aiChatService.getConversations(userId));
    }

    @Operation(summary = "获取对话历史")
    @GetMapping("/history/{id}")
    public Result<List<AiMessage>> getHistory(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(aiChatService.getHistory(id, userId));
    }

    @Operation(summary = "删除对话")
    @DeleteMapping("/history/{id}")
    public Result<Void> deleteHistory(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        aiChatService.deleteConversation(id, userId);
        return Result.noContent();
    }

    @Operation(summary = "流式聊天")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequest body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        SseEmitter emitter = new SseEmitter(120000L); // 2 分钟超时
        // 异步执行
        new Thread(() -> aiChatService.streamChat(body, userId, emitter)).start();
        return emitter;
    }

    @Operation(summary = "上传图片")
    @PostMapping("/upload-image")
    public Result<ImageUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws Exception {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(aiImageService.uploadImage(file, userId));
    }
}

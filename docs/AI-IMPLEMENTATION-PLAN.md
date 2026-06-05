# AI 助手功能实现计划

**基于设计文档**: `docs/superpowers/specs/2026-06-03-ai-assistant-design.md`
**AI 服务**: MiMo V2.5 (小米，OpenAI 兼容格式)
**范围**: 全部功能 — 后端 API + 前端对话页面 + 流式输出 + 图像输入 + 对话历史

---

## 实现顺序

### Phase 1: 后端基础设施
1. **SQL 迁移** — 新建 `ai_conversation`、`ai_message`、`ai_image` 三张表
2. **配置类** — `MiMoModelConfig.java` (读取 mimo.* 配置)
3. **application.yml** — 添加 mimo API 配置

### Phase 2: 后端实体 + Mapper
4. **实体类** — `AiConversation.java`、`AiMessage.java`、`AiImage.java` (继承 BaseEntity)
5. **Mapper** — `AiConversationMapper`、`AiMessageMapper`、`AiImageMapper`

### Phase 3: 后端 DTO
6. **DTO** — `ChatRequest`、`CreateConversationRequest`、`ModelResponse`、`ImageUploadResponse`

### Phase 4: 后端 Service
7. **AiChatService** — 核心：构建请求体、调用 MiMo API (SSE 流式)、保存消息
8. **AiImageService** — 图片上传/校验

### Phase 5: 后端 Controller
9. **AiChatController** — REST API:
   - `GET /api/ai/models` — 模型列表
   - `POST /api/ai/chat` — 流式聊天 (SSE)
   - `POST /api/ai/upload-image` — 上传图片
   - `GET /api/ai/conversations` — 对话列表
   - `POST /api/ai/conversations` — 创建对话
   - `GET /api/ai/history/{id}` — 对话历史
   - `DELETE /api/ai/history/{id}` — 删除对话

### Phase 6: 后端配置更新
10. **AuthInterceptor** — 放行 `/api/ai/**` 路由 (AI 助手需登录)
11. **CORS** — 确认 SSE 连接支持

### Phase 7: 前端 API + Composables
12. **aiApi.js** — Axios 模块 (SSE 流式处理)
13. **useAiChat.js** — 流式输出、图片上传、停止生成

### Phase 8: 前端组件
14. **ChatMessage.vue** — 消息气泡 (支持 Markdown 渲染、思考过程展示)
15. **StreamingText.vue** — 流式打字效果
16. **ChatHistory.vue** — 对话历史侧边栏
17. **QuickActions.vue** — 快捷操作按钮
18. **ImageUploader.vue** — 图片预览/上传

### Phase 9: 前端页面
19. **AiAssistant.vue** — 主页面 (布局: 侧边栏 + 设置栏 + 消息区 + 输入区)

### Phase 10: 路由 + 菜单
20. **router/index.js** — 添加 `/ai-assistant` 路由
21. **MainLayout.vue** — 添加 AI 助手导航入口

---

## 关键技术决策

| 决策 | 方案 |
|------|------|
| SSE 流式输出 | 使用 `SseEmitter` + `RestTemplate` 流式读取 MiMo API |
| MiMo API 格式 | OpenAI 兼容 (`/v1/chat/completions`, stream=true) |
| 图片传输 | Base64 编码内联到 messages (MiMo V2.5 支持) |
| 对话历史 | 数据库存储，每次请求发送最近 N 条 |
| 认证 | 复用现有 JWT，AI 接口需要登录 |

## 文件清单

### 新建文件 (后端)
```
blog-backend/sql/migration-ai.sql
blog-backend/src/main/java/com/blog/config/MiMoModelConfig.java
blog-backend/src/main/java/com/blog/entity/AiConversation.java
blog-backend/src/main/java/com/blog/entity/AiMessage.java
blog-backend/src/main/java/com/blog/entity/AiImage.java
blog-backend/src/main/java/com/blog/mapper/AiConversationMapper.java
blog-backend/src/main/java/com/blog/mapper/AiMessageMapper.java
blog-backend/src/main/java/com/blog/mapper/AiImageMapper.java
blog-backend/src/main/java/com/blog/dto/ChatRequest.java
blog-backend/src/main/java/com/blog/dto/CreateConversationRequest.java
blog-backend/src/main/java/com/blog/dto/ModelResponse.java
blog-backend/src/main/java/com/blog/dto/ImageUploadResponse.java
blog-backend/src/main/java/com/blog/service/AiChatService.java
blog-backend/src/main/java/com/blog/service/AiImageService.java
blog-backend/src/main/java/com/blog/controller/AiChatController.java
```

### 新建文件 (前端)
```
vue3-elementplus-starter/src/api/aiApi.js
vue3-elementplus-starter/src/composables/useAiChat.js
vue3-elementplus-starter/src/components/ChatMessage.vue
vue3-elementplus-starter/src/components/StreamingText.vue
vue3-elementplus-starter/src/components/ChatHistory.vue
vue3-elementplus-starter/src/components/QuickActions.vue
vue3-elementplus-starter/src/views/AiAssistant.vue
```

### 修改文件
```
blog-backend/src/main/resources/application.yml          (添加 mimo 配置)
blog-backend/src/main/java/com/blog/config/AuthInterceptor.java (放行 AI 路由)
vue3-elementplus-starter/src/router/index.js              (添加路由)
vue3-elementplus-starter/src/views/MainLayout.vue         (添加导航)
```

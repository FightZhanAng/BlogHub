# 博客项目迭代设计 — UX + 架构 + 安全

> 日期: 2025-05-23  
> 状态: 待实现  
> 涉及范围: blog-backend + vue3-elementplus-starter

---

## 概述

在博客现有功能基础上，从用户体验、代码架构、安全防护三个维度进行系统性改进，三部分互不阻塞，可并行推进。

---

## 1. 用户体验 (UX)

### 1.1 阅读进度条

**方案：** 固定在浏览器窗口顶部 3px 高的渐变细条，随阅读滚动指示百分比。

- 文件：新增 `src/components/ReadingProgress.vue`
- 使用位置：`src/layouts/MainLayout.vue`（全局，所有页面底部阅读量大于一屏时显示）
- 逻辑：
  - `onscroll` 计算 `progress = scrollTop / (scrollHeight - clientHeight) * 100`
  - 宽度用 `transform: scaleX()` + `transform-origin: left` 避免重排
  - 颜色 `#409eff → #337ecc` 渐变
  - 顶部 `position: fixed; top: 0; left: 0; height: 3px; z-index: 9999`
- 进度 < 2% 时隐藏（`opacity: 0`，过渡动画）

### 1.2 图片灯箱

**方案：** 点击文章正文内的 `<img>` 打开全屏遮罩查看大图。

- 文件：新增 `src/components/ImageLightbox.vue`
- 使用位置：`src/views/BlogPost.vue`，挂载后为 `.markdown-body img` 绑定 click 事件
- 组件结构：
  - 全屏固定遮罩（`position: fixed; inset: 0; background: rgba(0,0,0,0.85)`）
  - 居中 `<img>`，`max-width: 90vw; max-height: 90vh; object-fit: contain`
  - 右上角关闭按钮（`X` 图标），底部显示图片原始 alt 文本
- 交互：
  - 点击遮罩 / ESC 键 → 关闭
  - 显示时 `overflow: hidden` 锁 body 滚动
  - 图片加载过程显示 Circle 加载动画
- 依赖：纯 Vue + CSS，无需第三方库

### 1.3 文章目录锚点高亮

**方案：** 滚动时当前阅读的章节标题在 TOC 中自动高亮。

- 文件：重写 `src/components/BlogToc.vue`
- 数据源：从 `.markdown-body h1/h2/h3` 提取（已有）
- 高亮算法：使用 `IntersectionObserver` 代替 scroll 事件监听
  - 为每个标题创建观察目标，`rootMargin: -80px 0px -60% 0px`
  - 进入区域的第一个标题对应的 TOC 项加 `.active` 类
- 点击 TOC 项：`document.querySelector(`#${id}`).scrollIntoView({ behavior: 'smooth' })`
- 样式：`.active` 颜色 `#409eff`、左侧竖条、字体加粗

### 1.4 全文搜索

**方案：** 文章列表页顶部增加搜索框，输入时实时向后端请求过滤。

- 前端：`src/views/BlogList.vue` 顶部增加 `<el-input>` 搜索框
  - 带搜索图标，placeholder "搜索文章..."
  - 300ms 防抖（`lodash-es/debounce` 或手动 `setTimeout`）
  - 输入清除按钮 `clearable`
- 后端：`GET /api/posts` 增加 `keyword` 参数
  - `PostService.getPublishedPosts()` 增加 keyword 搜索
  - 条件：`title LIKE %keyword% OR content LIKE %keyword%`
  - 如果 keyword 为空则返回全部（已有逻辑）
- 无结果时显示 `el-empty` 组件

### 1.5 骨架屏

**方案：** 文章列表和文章详情加载时使用骨架占位。

- 文件：`src/components/SkeletonCard.vue`
- 使用 Element Plus 内置 `<el-skeleton>` 组件
- `BlogList.vue`：加载中时渲染 6 个 `SkeletonCard`
- `BlogPost.vue`：加载中时渲染 1 个 `SkeletonDetail`（标题 + 正文占位）
- 骨架样式：灰色圆角矩形 + 脉冲动画（el-skeleton 自带）

---

## 2. 架构改进

### 2.1 统一 DTO 层

当前命名混乱：部分接口返回 Entity、部分返回 `PostResponse`。

- `PostResponse` → 保留类名，保持 `PostResponse.from()` 风格
- 新建 `UserDTO`，天然不含 `password` 字段，`from()` 方法
- 新建 `CommentDTO`，包含 `postTitle` / `postSlug` 等展示字段
- **关键规则**：Controller 方法签名全部返回 DTO，不再直接暴露 Entity
- 转换方法统一用 `XxxDTO.from(entity)` 静态工厂
- 新建 `CommentDTO`，包含 `postTitle` / `postSlug` 等展示字段
- 转换方法统一用 `XxxDTO.from(entity)` 静态工厂
- Service 层接口和方法签名的返回类型全部改为 DTO

### 2.2 枚举类

- `PostStatus`：`DRAFT(0, "草稿")`, `PUBLISHED(1, "已发布")` + `getCode()` / `getDesc()`
- `UserRole`：`ADMIN("admin", "管理员")`, `USER("user", "普通用户")`
- `ErrorCode`：统一错误码枚举，见 2.3
- MyBatis-Plus 配置 `type-enums-package` 自动转换枚举 <-> 数据库值

### 2.3 全局异常码

```java
public enum ErrorCode {
    POST_NOT_FOUND(40401, "文章不存在"),
    COMMENT_NOT_FOUND(40402, "评论不存在"),
    USER_NOT_FOUND(40403, "用户不存在"),

    FORBIDDEN(40300, "无权操作"),
    LOGIN_REQUIRED(40301, "请先登录"),

    BAD_REQUEST(40000, "请求参数错误"),
    USERNAME_EXISTS(40001, "用户名已存在"),
    INVALID_PASSWORD(40002, "原密码错误"),
    VALIDATION_ERROR(40003, "参数校验失败"),
    ;
}
```

- `BusinessException` 增加 `ErrorCode` 构造方法
- `GlobalExceptionHandler` 从 `ErrorCode` 提取 message 响应给前端

### 2.4 API 文档（Swagger）

- 添加 `springdoc-openapi-ui` 依赖（v1.7.0）
- DTO 字段加 `@Schema(description = "...")`
- Controller 方法加 `@Operation(summary = "...")`
- 生产环境关闭：`springdoc.api-docs.enabled=false`

### 2.5 统一分页请求

- 已有 `PageResult`，再抽取 `PageParam`（page / size / keyword）
- Controller 统一接收 `PageParam` 参数
- Service 统一返回 `PageResult<T>`

### 2.6 前端请求封装

- `composables/useApi.js`：

```js
export function useApi(fn, options = {}) {
  const data = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function execute(...args) {
    loading.value = true
    error.value = null
    try {
      data.value = await fn(...args)
    } catch (e) {
      error.value = e
    } finally {
      loading.value = false
    }
    return data.value
  }

  return { data, loading, error, execute }
}
```

- 逐步替换现有页面中的 try/catch/loading 三板斧模式

### 2.7 后端参数校验完善

- 分组校验：`CreateGroup` / `UpdateGroup`
- 自定义 `@Password` 校验器（大小写 + 数字 + 至少 8 位）
- 自定义 `@EnumString(enumClass = Xxx.class)` 校验器

---

## 3. 安全增强

### 3.1 BCrypt 密码加密

- 依赖：`spring-security-crypto`
- Bean：`@Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }`
- 注册/改密：一律 `passwordEncoder.encode(raw)`
- 登录兼容：先 BCrypt matches → 失败回退 MD5 → 命中则自动升级为 BCrypt 并重新存储

### 3.2 XSS 过滤

- 依赖：`jsoup`（`org.jsoup:jsoup:1.17.2`）
- 工具类 `HtmlSanitizer`：
  ```java
  public static String clean(String input) { return Jsoup.clean(input, Safelist.basic()); }
  ```
- 对 Comment.content、Post.content 输出时调用
- 在 `GlobalExceptionHandler` 或序列化层统一处理

### 3.3 CSRF 说明

当前认证使用 JWT + Authorization Header（非 Cookie），不受 CSRF 影响，只需在文档中说明，无需额外代码。

### 3.4 接口限流

- 自定义注解 `@RateLimit(key = "", max = 5, period = 60)`（单位：秒）
- AOP 切面 + `ConcurrentHashMap<String, AtomicInteger>` 本地计数
- 应用在 `/api/auth/login` 和 `/api/auth/register`
- 超限返回 `429 Too Many Requests`

### 3.5 Token 刷新

- 后端新增 `POST /api/auth/refresh` 接口
  - 接收 Refresh Token → 校验 → 签发新 Access Token
  - Refresh Token 有效期 7 天（当前 JWT 24h）
- 前端 axios 响应拦截器：
  ```js
  // 401 → 调用 /api/auth/refresh → 重放原请求
  instance.interceptors.response.use(null, async error => {
    if (error.response?.status === 401) {
      const newToken = await refreshToken()
      error.config.headers.Authorization = `Bearer ${newToken}`
      return instance(error.config)
    }
  })
  ```

### 3.6 操作日志

- 新增 `sys_log` 表（id / user_id / action / resource / params / ip / created_at）
- `@AuditLog(action = "...", resource = "...")` AOP 注解
- Service 方法执行后自动记录
- 仅对管理员接口生效（`UserRole.ADMIN`）
- 前端日志管理页面 `/logs`（管理员可见），只读展示最近 90 天

---

## 实施顺序

```
Phase 1 — 基础架构（枚举 + 异常码 + DTO + 分页统一）
  → 为后续所有代码提供基础设施

Phase 2 — 安全（BCrypt + XSS + 限流 + Token + 操作日志）
  → 尽早修复安全债

Phase 3 — 前端封装（useApi）
  → 简化后续 UX 功能的编写

Phase 4 — UX 功能（搜索 → 骨架屏 → 目录高亮 → 进度条 → 灯箱）
  → 用户体验改进，按复杂度从低到高

Phase 5 — API 文档（Swagger）
  → 收尾工作
```

const pptxgen = require("pptxgenjs");

const pres = new pptxgen();
pres.layout = "LAYOUT_16x9";
pres.author = "郑占昂";
pres.title = "BlogHub - 全栈技术博客系统";

// ===== 配色方案：Ocean Gradient =====
const C = {
  deep: "1E2761",      // 深海军蓝
  mid: "2C5F8A",       // 中蓝
  accent: "4FC3F7",    // 亮青
  light: "E3F2FD",     // 浅蓝背景
  white: "FFFFFF",
  text: "1E293B",      // 深色文字
  muted: "64748B",     // 灰色辅助文字
  lightMuted: "94A3B8", // 浅灰辅助文字（深色背景用）
  dark: "0F172A",      // 深色背景
};

// ===== 工具函数 =====
const makeShadow = () => ({ type: "outer", blur: 8, offset: 3, angle: 135, color: "000000", opacity: 0.12 });

// ================================================================
// SLIDE 1: 封面
// ================================================================
let s1 = pres.addSlide();
s1.background = { color: C.dark };

// 装饰圆
s1.addShape(pres.shapes.OVAL, { x: 7.5, y: -1.5, w: 4, h: 4, fill: { color: C.mid, transparency: 40 } });
s1.addShape(pres.shapes.OVAL, { x: 8.5, y: 3, w: 2.5, h: 2.5, fill: { color: C.accent, transparency: 50 } });

// B Logo
s1.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: 0.8, y: 1.2, w: 0.8, h: 0.8, fill: { color: C.accent }, rectRadius: 0.15 });
s1.addText("B", { x: 0.8, y: 1.15, w: 0.8, h: 0.85, fontSize: 32, fontFace: "Arial Black", color: C.white, align: "center", valign: "middle", bold: true });

s1.addText("BlogHub", { x: 1.8, y: 1.2, w: 6, h: 0.8, fontSize: 40, fontFace: "Arial Black", color: C.white, bold: true, margin: 0 });

s1.addText("全栈技术博客系统", { x: 0.8, y: 2.4, w: 6, h: 0.5, fontSize: 20, fontFace: "Arial", color: C.accent, margin: 0 });

s1.addText("Vue 3 + Spring Boot + MySQL", { x: 0.8, y: 3.1, w: 6, h: 0.4, fontSize: 14, fontFace: "Arial", color: C.lightMuted, margin: 0 });

s1.addText("郑占昂", { x: 0.8, y: 3.8, w: 3, h: 0.4, fontSize: 14, fontFace: "Arial", color: C.lightMuted, margin: 0 });
s1.addText("2026.05", { x: 0.8, y: 4.2, w: 3, h: 0.4, fontSize: 12, fontFace: "Arial", color: C.lightMuted, margin: 0 });

// ================================================================
// SLIDE 2: 项目概览
// ================================================================
let s2 = pres.addSlide();
s2.background = { color: C.white };

s2.addText("项目概览", { x: 0.8, y: 0.3, w: 8, h: 0.6, fontSize: 32, fontFace: "Arial Black", color: C.deep, bold: true, margin: 0 });
s2.addShape(pres.shapes.RECTANGLE, { x: 0.8, y: 1.05, w: 1.2, h: 0.05, fill: { color: C.accent } });

s2.addText("功能完整的技术博客系统，支持文章管理、评论互动、社交功能、管理后台等全链路博客运营需求", {
  x: 0.8, y: 1.3, w: 8.4, h: 0.6, fontSize: 14, fontFace: "Arial", color: C.muted, margin: 0
});

// 核心数据卡片
const stats = [
  { num: "16", label: "数据表", color: C.accent },
  { num: "78", label: "API 接口", color: C.accent },
  { num: "21", label: "页面组件", color: C.accent },
  { num: "9", label: "核心模块", color: C.accent },
];

stats.forEach((item, i) => {
  const x = 0.8 + i * 2.25;
  s2.addShape(pres.shapes.RECTANGLE, { x, y: 2.2, w: 2.0, h: 1.4, fill: { color: C.white }, shadow: makeShadow() });
  s2.addShape(pres.shapes.RECTANGLE, { x, y: 2.2, w: 2.0, h: 0.06, fill: { color: item.color } });
  s2.addText(item.num, { x, y: 2.4, w: 2.0, h: 0.8, fontSize: 36, fontFace: "Arial Black", color: item.color, align: "center", valign: "middle", bold: true, margin: 0 });
  s2.addText(item.label, { x, y: 3.1, w: 2.0, h: 0.4, fontSize: 13, fontFace: "Arial", color: C.muted, align: "center", margin: 0 });
});

// 技术栈标签
s2.addText("开源协议：MIT   |   GitHub: FightZhanAng/BlogHub   |   Docker 容器化部署", {
  x: 0.8, y: 4.2, w: 8.4, h: 0.4, fontSize: 12, fontFace: "Arial", color: C.muted, align: "center", margin: 0
});

// ================================================================
// SLIDE 3: 技术架构
// ================================================================
let s3 = pres.addSlide();
s3.background = { color: C.white };

s3.addText("技术架构", { x: 0.8, y: 0.3, w: 8, h: 0.6, fontSize: 32, fontFace: "Arial Black", color: C.deep, bold: true, margin: 0 });
s3.addShape(pres.shapes.RECTANGLE, { x: 0.8, y: 0.95, w: 1.2, h: 0.05, fill: { color: C.accent } });

// 前端列
s3.addShape(pres.shapes.RECTANGLE, { x: 0.8, y: 1.4, w: 2.8, h: 3.6, fill: { color: C.light }, shadow: makeShadow() });
s3.addText("前端技术栈", { x: 0.8, y: 1.5, w: 2.8, h: 0.5, fontSize: 16, fontFace: "Arial", color: C.deep, bold: true, align: "center", margin: 0 });

const feStack = ["Vue 3 (Composition API)", "Vite 5 构建工具", "Element Plus 组件库", "Pinia 状态管理", "Vue Router 路由", "Axios 请求封装", "ECharts 数据图表", "Markdown 渲染引擎"];
feStack.forEach((item, i) => {
  s3.addText([
    { text: "● ", options: { color: C.accent, fontSize: 10 } },
    { text: item, options: { color: C.text, fontSize: 12 } }
  ], { x: 1.0, y: 2.1 + i * 0.32, w: 2.5, h: 0.3, margin: 0 });
});

// 后端列
s3.addShape(pres.shapes.RECTANGLE, { x: 3.9, y: 1.4, w: 2.8, h: 3.6, fill: { color: C.light }, shadow: makeShadow() });
s3.addText("后端技术栈", { x: 3.9, y: 1.5, w: 2.8, h: 0.5, fontSize: 16, fontFace: "Arial", color: C.deep, bold: true, align: "center", margin: 0 });

const beStack = ["Spring Boot 2.7", "MyBatis-Plus ORM", "MySQL 8.0 数据库", "JWT 认证 (双 Token)", "BCrypt 密码加密", "jsoup XSS 防护", "Spring AOP 切面", "Swagger API 文档"];
beStack.forEach((item, i) => {
  s3.addText([
    { text: "● ", options: { color: C.mid, fontSize: 10 } },
    { text: item, options: { color: C.text, fontSize: 12 } }
  ], { x: 4.1, y: 2.1 + i * 0.32, w: 2.5, h: 0.3, margin: 0 });
});

// 基础设施列
s3.addShape(pres.shapes.RECTANGLE, { x: 7.0, y: 1.4, w: 2.8, h: 3.6, fill: { color: C.light }, shadow: makeShadow() });
s3.addText("基础设施", { x: 7.0, y: 1.5, w: 2.8, h: 0.5, fontSize: 16, fontFace: "Arial", color: C.deep, bold: true, align: "center", margin: 0 });

const infra = ["Docker Compose 编排", "Nginx 反向代理", "HikariCP 连接池", "Logback 日志系统", "Nginx 静态资源", "HTTPS SSL 证书", "定时任务调度", "CSV 数据导出"];
infra.forEach((item, i) => {
  s3.addText([
    { text: "● ", options: { color: "4FC3F7", fontSize: 10 } },
    { text: item, options: { color: C.text, fontSize: 12 } }
  ], { x: 7.2, y: 2.1 + i * 0.32, w: 2.5, h: 0.3, margin: 0 });
});

// ================================================================
// SLIDE 4: 核心功能模块
// ================================================================
let s4 = pres.addSlide();
s4.background = { color: C.white };

s4.addText("核心功能模块", { x: 0.8, y: 0.3, w: 8, h: 0.6, fontSize: 32, fontFace: "Arial Black", color: C.deep, bold: true, margin: 0 });
s4.addShape(pres.shapes.RECTANGLE, { x: 0.8, y: 0.95, w: 1.2, h: 0.05, fill: { color: C.accent } });

const modules = [
  { icon: "📝", title: "文章系统", desc: "Markdown 编辑器\n版本历史 · 定时发布\nRSS 订阅 · 全文搜索" },
  { icon: "💬", title: "评论系统", desc: "多级嵌套回复\n@提及通知 · 赞/踩\nXSS 防护" },
  { icon: "🏷", title: "标签系统", desc: "自动创建标签\n标签云 · 按标签筛选\n文章关联" },
  { icon: "👤", title: "用户认证", desc: "JWT 双 Token\n登录限流 · BCrypt\n个人中心" },
  { icon: "❤", title: "社交互动", desc: "点赞 · 收藏\n用户关注 · 分享\n排行榜" },
  { icon: "🔔", title: "通知系统", desc: "自动推送通知\n未读角标\n全部已读" },
  { icon: "📊", title: "管理后台", desc: "数据仪表盘\n用户/评论管理\n操作日志审计" },
  { icon: "📷", title: "宝宝相册", desc: "相册管理\n照片/视频上传\n成长时间线" },
];

modules.forEach((m, i) => {
  const col = i % 4;
  const row = Math.floor(i / 4);
  const x = 0.8 + col * 2.25;
  const y = 1.3 + row * 2.0;

  s4.addShape(pres.shapes.RECTANGLE, { x, y, w: 2.0, h: 1.8, fill: { color: C.white }, shadow: makeShadow() });
  s4.addText(m.icon, { x, y: y + 0.15, w: 2.0, h: 0.5, fontSize: 28, align: "center", margin: 0 });
  s4.addText(m.title, { x, y: y + 0.65, w: 2.0, h: 0.35, fontSize: 14, fontFace: "Arial", color: C.deep, bold: true, align: "center", margin: 0 });
  s4.addText(m.desc, { x: x + 0.15, y: y + 1.0, w: 1.7, h: 0.7, fontSize: 10, fontFace: "Arial", color: C.muted, align: "center", margin: 0 });
});

// ================================================================
// SLIDE 5: 文章系统详解
// ================================================================
let s5 = pres.addSlide();
s5.background = { color: C.white };

s5.addText("文章系统", { x: 0.8, y: 0.3, w: 8, h: 0.6, fontSize: 32, fontFace: "Arial Black", color: C.deep, bold: true, margin: 0 });
s5.addShape(pres.shapes.RECTANGLE, { x: 0.8, y: 0.95, w: 1.2, h: 0.05, fill: { color: C.accent } });

// 左侧：功能列表
s5.addText([
  { text: "Markdown 编辑器", options: { bold: true, color: C.deep, fontSize: 14, breakLine: true } },
  { text: "  实时预览 · 图片拖拽上传 · 粘贴上传", options: { color: C.muted, fontSize: 11, breakLine: true } },
  { text: "", options: { fontSize: 8, breakLine: true } },
  { text: "版本历史", options: { bold: true, color: C.deep, fontSize: 14, breakLine: true } },
  { text: "  每次编辑自动快照，最多保留 50 个版本", options: { color: C.muted, fontSize: 11, breakLine: true } },
  { text: "", options: { fontSize: 8, breakLine: true } },
  { text: "定时发布", options: { bold: true, color: C.deep, fontSize: 14, breakLine: true } },
  { text: "  每分钟自动扫描到期文章并发布", options: { color: C.muted, fontSize: 11, breakLine: true } },
  { text: "", options: { fontSize: 8, breakLine: true } },
  { text: "相关推荐", options: { bold: true, color: C.deep, fontSize: 14, breakLine: true } },
  { text: "  基于标签匹配推荐相关文章", options: { color: C.muted, fontSize: 11, breakLine: true } },
  { text: "", options: { fontSize: 8, breakLine: true } },
  { text: "SEO 优化", options: { bold: true, color: C.deep, fontSize: 14, breakLine: true } },
  { text: "  RSS 订阅 · Sitemap · 独立 meta 标签", options: { color: C.muted, fontSize: 11 } },
], { x: 0.8, y: 1.3, w: 4.5, h: 4.0, margin: 0 });

// 右侧：架构图
s5.addShape(pres.shapes.RECTANGLE, { x: 5.8, y: 1.3, w: 3.8, h: 3.8, fill: { color: C.light }, shadow: makeShadow() });
s5.addText("文章数据流", { x: 5.8, y: 1.4, w: 3.8, h: 0.4, fontSize: 14, fontFace: "Arial", color: C.deep, bold: true, align: "center", margin: 0 });

const flow = ["Markdown 编辑器", "↓ 保存", "文章数据库 (posts)", "↓ 定时任务", "定时发布扫描", "↓ 自动发布", "全文搜索索引"];
flow.forEach((item, i) => {
  const isArrow = item.startsWith("↓");
  s5.addText(item, {
    x: 6.2, y: 1.9 + i * 0.42, w: 3.0, h: 0.35,
    fontSize: isArrow ? 11 : 12,
    fontFace: "Arial",
    color: isArrow ? C.accent : C.text,
    align: "center",
    bold: !isArrow,
    margin: 0
  });
});

// ================================================================
// SLIDE 6: 社交与互动
// ================================================================
let s6 = pres.addSlide();
s6.background = { color: C.white };

s6.addText("社交与互动", { x: 0.8, y: 0.3, w: 8, h: 0.6, fontSize: 32, fontFace: "Arial Black", color: C.deep, bold: true, margin: 0 });
s6.addShape(pres.shapes.RECTANGLE, { x: 0.8, y: 0.95, w: 1.2, h: 0.05, fill: { color: C.accent } });

const socialFeatures = [
  { title: "点赞系统", desc: "未登录按 IP 识别\n已登录按用户识别\n首页 TOP10 排行榜", color: C.accent },
  { title: "收藏功能", desc: "一键收藏文章\n个人收藏列表\n随时取消收藏", color: C.accent },
  { title: "用户关注", desc: "关注/取关用户\n粉丝/关注计数\n用户主页展示", color: C.accent },
  { title: "通知中心", desc: "点赞·评论·回复·关注\n@提及自动通知\n未读角标 + 已读", color: C.accent },
];

socialFeatures.forEach((f, i) => {
  const x = 0.8 + i * 2.25;
  s6.addShape(pres.shapes.RECTANGLE, { x, y: 1.4, w: 2.0, h: 2.8, fill: { color: C.white }, shadow: makeShadow() });
  s6.addShape(pres.shapes.RECTANGLE, { x, y: 1.4, w: 2.0, h: 0.06, fill: { color: f.color } });
  s6.addText(f.title, { x, y: 1.6, w: 2.0, h: 0.4, fontSize: 16, fontFace: "Arial", color: C.deep, bold: true, align: "center", margin: 0 });
  s6.addText(f.desc, { x: x + 0.15, y: 2.1, w: 1.7, h: 1.8, fontSize: 12, fontFace: "Arial", color: C.muted, align: "center", margin: 0 });
});

// 分享功能
s6.addText("分享到：微博 · 知乎 · 微信 · 复制链接", { x: 0.8, y: 4.6, w: 8.4, h: 0.4, fontSize: 12, fontFace: "Arial", color: C.muted, align: "center", margin: 0 });

// ================================================================
// SLIDE 7: 数据库设计
// ================================================================
let s7 = pres.addSlide();
s7.background = { color: C.white };

s7.addText("数据库设计", { x: 0.8, y: 0.3, w: 8, h: 0.6, fontSize: 32, fontFace: "Arial Black", color: C.deep, bold: true, margin: 0 });
s7.addShape(pres.shapes.RECTANGLE, { x: 0.8, y: 0.95, w: 1.2, h: 0.05, fill: { color: C.accent } });

s7.addText("MySQL 8.0  |  16 张数据表  |  utf8mb4_unicode_ci 字符集", {
  x: 0.8, y: 1.1, w: 8.4, h: 0.4, fontSize: 13, fontFace: "Arial", color: C.muted, margin: 0
});

const tables = [
  { name: "sys_user", desc: "用户表" },
  { name: "posts", desc: "文章表" },
  { name: "comments", desc: "评论表" },
  { name: "tag", desc: "标签表" },
  { name: "post_tag", desc: "文章标签关联" },
  { name: "likes", desc: "点赞表" },
  { name: "bookmarks", desc: "收藏表" },
  { name: "follows", desc: "关注表" },
  { name: "notifications", desc: "通知表" },
  { name: "sys_log", desc: "操作日志" },
  { name: "post_versions", desc: "版本历史" },
  { name: "series", desc: "文章系列" },
  { name: "daily_views", desc: "阅读统计" },
  { name: "images", desc: "图片记录" },
  { name: "albums", desc: "宝宝相册" },
  { name: "photos", desc: "照片表" },
];

tables.forEach((t, i) => {
  const col = i % 4;
  const row = Math.floor(i / 4);
  const x = 0.8 + col * 2.25;
  const y = 1.7 + row * 0.65;

  s7.addShape(pres.shapes.RECTANGLE, { x, y, w: 2.0, h: 0.55, fill: { color: row === 0 ? C.deep : row === 1 ? C.mid : C.accent } });
  s7.addText([
    { text: t.name, options: { color: C.white, fontSize: 11, bold: true } },
    { text: "  " + t.desc, options: { color: C.white, fontSize: 9 } }
  ], { x, y, w: 2.0, h: 0.55, align: "center", valign: "middle", margin: 0 });
});

// ================================================================
// SLIDE 8: API 设计
// ================================================================
let s8 = pres.addSlide();
s8.background = { color: C.white };

s8.addText("RESTful API 设计", { x: 0.8, y: 0.3, w: 8, h: 0.6, fontSize: 32, fontFace: "Arial Black", color: C.deep, bold: true, margin: 0 });
s8.addShape(pres.shapes.RECTANGLE, { x: 0.8, y: 0.95, w: 1.2, h: 0.05, fill: { color: C.accent } });

s8.addText("78 个 API 端点  |  18 个 Controller  |  统一 Result 返回结构", {
  x: 0.8, y: 1.1, w: 8.4, h: 0.4, fontSize: 13, fontFace: "Arial", color: C.muted, margin: 0
});

// 表格
const apiTable = [
  [{ text: "Controller", options: { bold: true, color: C.white, fill: { color: C.deep } } },
   { text: "端点", options: { bold: true, color: C.white, fill: { color: C.deep } } },
   { text: "功能", options: { bold: true, color: C.white, fill: { color: C.deep } } }],
  ["PostController", "11", "文章 CRUD、搜索、归档、推荐"],
  ["UserController", "11", "用户管理、个人中心、头像上传"],
  ["AlbumController", "10", "相册 CRUD、照片管理、时间线"],
  ["CommentController", "7", "评论 CRUD、赞/踩、统计"],
  ["SocialController", "7", "关注、通知、排行榜"],
  ["SeriesController", "5", "文章系列 CRUD"],
  ["其他 12 个", "27", "标签、认证、统计、导出等"],
];

s8.addTable(apiTable, {
  x: 0.8, y: 1.6, w: 8.4,
  border: { pt: 0.5, color: "E2E8F0" },
  colW: [2.5, 1.0, 4.9],
  rowH: 0.4,
  fontSize: 11,
  fontFace: "Arial",
  color: C.text,
  autoPage: false,
});

// ================================================================
// SLIDE 9: 安全与性能
// ================================================================
let s9 = pres.addSlide();
s9.background = { color: C.dark };

s9.addText("安全与性能", { x: 0.8, y: 0.3, w: 8, h: 0.6, fontSize: 32, fontFace: "Arial Black", color: C.white, bold: true, margin: 0 });
s9.addShape(pres.shapes.RECTANGLE, { x: 0.8, y: 0.95, w: 1.2, h: 0.05, fill: { color: C.accent } });

const securityItems = [
  { title: "JWT 认证", desc: "Access Token (24h)\nRefresh Token (7天)\n自动续期机制" },
  { title: "密码安全", desc: "BCrypt 加密存储\n兼容旧 MD5 自动升级\n加盐哈希" },
  { title: "XSS 防护", desc: "jsoup HTML 清洗\n评论内容过滤\n输入验证" },
  { title: "访问限流", desc: "登录 5次/分钟\n注册 3次/分钟\nAOP 切面实现" },
];

securityItems.forEach((item, i) => {
  const x = 0.8 + i * 2.25;
  s9.addShape(pres.shapes.RECTANGLE, { x, y: 1.3, w: 2.0, h: 2.2, fill: { color: C.deep } });
  s9.addText(item.title, { x, y: 1.45, w: 2.0, h: 0.4, fontSize: 16, fontFace: "Arial", color: C.accent, bold: true, align: "center", margin: 0 });
  s9.addText(item.desc, { x: x + 0.15, y: 1.95, w: 1.7, h: 1.3, fontSize: 12, fontFace: "Arial", color: C.white, align: "center", margin: 0 });
});

// 性能指标
s9.addText("性能优化", { x: 0.8, y: 3.8, w: 8, h: 0.4, fontSize: 18, fontFace: "Arial", color: C.white, bold: true, margin: 0 });

const perfItems = ["HikariCP 连接池（最大20连接）", "MyBatis-Plus 分页插件", "前端 Vite 构建优化", "图片懒加载 + 瀑布流", "Nginx 静态资源缓存", "Docker 容器化部署"];
perfItems.forEach((item, i) => {
  s9.addText([
    { text: "▸ ", options: { color: C.accent, fontSize: 12 } },
    { text: item, options: { color: C.white, fontSize: 13 } }
  ], { x: 0.8 + (i % 3) * 3.0, y: 4.2 + Math.floor(i / 3) * 0.4, w: 2.8, h: 0.35, margin: 0 });
});

// ================================================================
// SLIDE 10: 项目亮点
// ================================================================
let s10 = pres.addSlide();
s10.background = { color: C.white };

s10.addText("项目亮点", { x: 0.8, y: 0.3, w: 8, h: 0.6, fontSize: 32, fontFace: "Arial Black", color: C.deep, bold: true, margin: 0 });
s10.addShape(pres.shapes.RECTANGLE, { x: 0.8, y: 0.95, w: 1.2, h: 0.05, fill: { color: C.accent } });

const highlights = [
  { icon: "🎨", title: "现代化 UI", desc: "Element Plus 组件库\n响应式布局\n深色模式支持" },
  { icon: "📝", title: "完整 Markdown", desc: "实时预览编辑器\n图片拖拽上传\n代码高亮渲染" },
  { icon: "🤖", title: "AI 辅助开发", desc: "Claude Code 集成\nPlaywright 自动化\nCodeGraph 代码分析" },
  { icon: "🚀", title: "一键部署", desc: "Docker Compose 编排\nNginx 反向代理\n环境隔离" },
];

highlights.forEach((h, i) => {
  const x = 0.8 + i * 2.25;
  s10.addShape(pres.shapes.RECTANGLE, { x, y: 1.4, w: 2.0, h: 2.5, fill: { color: C.light }, shadow: makeShadow() });
  s10.addText(h.icon, { x, y: 1.55, w: 2.0, h: 0.5, fontSize: 32, align: "center", margin: 0 });
  s10.addText(h.title, { x, y: 2.1, w: 2.0, h: 0.35, fontSize: 15, fontFace: "Arial", color: C.deep, bold: true, align: "center", margin: 0 });
  s10.addText(h.desc, { x: x + 0.15, y: 2.5, w: 1.7, h: 1.2, fontSize: 11, fontFace: "Arial", color: C.muted, align: "center", margin: 0 });
});

// ================================================================
// SLIDE 11: 谢谢
// ================================================================
let s11 = pres.addSlide();
s11.background = { color: C.dark };

s11.addShape(pres.shapes.OVAL, { x: -1, y: -1, w: 4, h: 4, fill: { color: C.mid, transparency: 40 } });
s11.addShape(pres.shapes.OVAL, { x: 7, y: 2.5, w: 4, h: 4, fill: { color: C.accent, transparency: 50 } });

s11.addText("Thanks", { x: 0.8, y: 1.5, w: 8.4, h: 1.0, fontSize: 48, fontFace: "Arial Black", color: C.white, align: "center", bold: true, margin: 0 });
s11.addText("BlogHub - 全栈技术博客系统", { x: 0.8, y: 2.6, w: 8.4, h: 0.5, fontSize: 18, fontFace: "Arial", color: C.accent, align: "center", margin: 0 });
s11.addText("Vue 3  ·  Spring Boot  ·  MySQL  ·  Docker", { x: 0.8, y: 3.3, w: 8.4, h: 0.4, fontSize: 14, fontFace: "Arial", color: C.lightMuted, align: "center", margin: 0 });
s11.addText("GitHub: FightZhanAng/BlogHub", { x: 0.8, y: 4.2, w: 8.4, h: 0.4, fontSize: 12, fontFace: "Arial", color: C.lightMuted, align: "center", margin: 0 });

// ===== 生成文件 =====
pres.writeFile({ fileName: "D:/Agent/BlogHub-项目介绍.pptx" })
  .then(() => console.log("PPT 生成成功: D:/Agent/BlogHub-项目介绍.pptx"))
  .catch(err => console.error("生成失败:", err));

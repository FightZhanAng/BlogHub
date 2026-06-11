// 统一的 API 基础地址，支持开发和生产环境
// 开发环境通过 Vite proxy 转发，无需完整地址
// 生产环境使用相对路径（nginx 反代）
export const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'
export const STATIC_BASE = import.meta.env.VITE_STATIC_BASE || ''

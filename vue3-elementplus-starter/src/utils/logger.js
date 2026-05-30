/**
 * 前端日志工具
 * - 开发环境：输出到 console（带颜色和时间戳）
 * - 生产环境：可选上报到后端
 */

const isDev = import.meta.env.DEV

const COLORS = {
  info: '#1890ff',
  warn: '#faad14',
  error: '#ff4d4f',
  debug: '#8c8c8c',
}

function timestamp() {
  return new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

function log(level, ...args) {
  if (!isDev && level === 'debug') return

  const color = COLORS[level] || COLORS.info
  const tag = `[${timestamp()}] [${level.toUpperCase()}]`

  if (isDev) {
    console[level === 'error' ? 'error' : level === 'warn' ? 'warn' : 'log'](
      `%c${tag}`,
      `color: ${color}; font-weight: bold`,
      ...args
    )
  }

  // 生产环境可选上报（预留接口）
  if (!isDev && level === 'error') {
    try {
      const payload = {
        level,
        message: args.map(a => (typeof a === 'string' ? a : JSON.stringify(a))).join(' '),
        url: window.location.href,
        timestamp: Date.now(),
      }
      if (navigator.sendBeacon) {
        navigator.sendBeacon('/api/stats/log', JSON.stringify(payload))
      }
    } catch (e) {
      // 静默失败
    }
  }
}

export const logger = {
  info: (...args) => log('info', ...args),
  warn: (...args) => log('warn', ...args),
  error: (...args) => log('error', ...args),
  debug: (...args) => log('debug', ...args),
}

export default logger

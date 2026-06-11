import { ref } from 'vue'

/**
 * 统一 API 请求封装
 * 自动管理 loading / error / data 三态
 *
 * @param {Function} fn 返回 Promise 的异步函数
 * @param {Object}   options
 * @param {boolean}  options.immediate 是否立即执行（默认 false）
 * @param {any}      options.initial   初始 data 值
 * @returns {{ data, loading, error, execute }}
 *
 * @example
 * const { data, loading, error, execute } = useApi(() => request.get('/users'))
 * await execute()  // 手动触发
 *
 * const { data } = useApi(() => request.get('/users'), { immediate: true })
 */
export function useApi(fn, options = {}) {
  const data = ref(options.initial ?? null)
  const loading = ref(false)
  const error = ref(null)

  async function execute(...args) {
    loading.value = true
    error.value = null
    try {
      const result = await fn(...args)
      data.value = result
      return result
    } catch (e) {
      error.value = e
      throw e
    } finally {
      loading.value = false
    }
  }

  if (options.immediate) {
    execute()
  }

  return { data, loading, error, execute }
}

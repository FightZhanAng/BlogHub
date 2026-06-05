import request from './request'

export const aiApi = {
  /** 获取可用模型列表 */
  getModels() {
    return request.get('/ai/models')
  },

  /** 创建新对话 */
  createConversation(data) {
    return request.post('/ai/conversations', data)
  },

  /** 获取对话列表 */
  getConversations() {
    return request.get('/ai/conversations')
  },

  /** 获取对话历史 */
  getHistory(conversationId) {
    return request.get(`/ai/history/${conversationId}`)
  },

  /** 删除对话 */
  deleteConversation(conversationId) {
    return request.delete(`/ai/history/${conversationId}`)
  },

  /**
   * 流式聊天 (SSE)
   * @param {Object} data - { conversationId, message, modelId, thinkingEnabled, imageUrls }
   * @param {Object} callbacks - { onContent, onThinking, onDone, onError }
   * @returns {AbortController} 可用于取消
   */
  streamChat(data, callbacks) {
    const auth = JSON.parse(localStorage.getItem('blog_auth') || '{}')
    const token = auth.token
    const controller = new AbortController()

    ;(async () => {
      try {
        const response = await fetch('/api/ai/chat', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify(data),
          signal: controller.signal
        })

        if (!response.ok) {
          const err = await response.json().catch(() => ({ message: '请求失败' }))
          callbacks.onError?.(err.message || '请求失败')
          return
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })

          // 按空行分割 SSE 事件
          const events = buffer.split('\n\n')
          buffer = events.pop() // 保留最后一个不完整的事件

          for (const eventStr of events) {
            if (!eventStr.trim()) continue

            let eventType = 'message'
            let eventData = ''

            for (const line of eventStr.split('\n')) {
              if (line.startsWith('event:')) {
                eventType = line.substring(6).trim()
              } else if (line.startsWith('data:')) {
                eventData = line.substring(5).trim()
              }
            }

            switch (eventType) {
              case 'content':
                callbacks.onContent?.(eventData)
                break
              case 'thinking':
                callbacks.onThinking?.(eventData)
                break
              case 'done':
                callbacks.onDone?.()
                break
              case 'error':
                callbacks.onError?.(eventData)
                break
            }
          }
        }

        // 处理 buffer 中剩余的事件
        if (buffer.trim()) {
          let eventType = 'message'
          let eventData = ''
          for (const line of buffer.split('\n')) {
            if (line.startsWith('event:')) eventType = line.substring(6).trim()
            else if (line.startsWith('data:')) eventData = line.substring(5).trim()
          }
          if (eventType === 'done') callbacks.onDone?.()
          else if (eventType === 'error') callbacks.onError?.(eventData)
        }

        callbacks.onDone?.()
      } catch (err) {
        if (err.name !== 'AbortError') {
          callbacks.onError?.(err.message)
        }
      }
    })()

    return controller
  },

  /** 上传图片 */
  uploadImage(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/ai/upload-image', formData)
  }
}

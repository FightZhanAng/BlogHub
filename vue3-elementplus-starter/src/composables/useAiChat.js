import { ref } from 'vue'
import { aiApi } from '@/api/aiApi'

/**
 * AI 聊天 composable
 */
export function useAiChat() {
  const isGenerating = ref(false)
  const currentContent = ref('')
  const currentThinking = ref('')
  let abortController = null

  /**
   * 流式发送消息
   */
  async function streamChat(messageData) {
    isGenerating.value = true
    currentContent.value = ''
    currentThinking.value = ''

    return new Promise((resolve, reject) => {
      abortController = aiApi.streamChat(messageData, {
        onContent(text) {
          currentContent.value += text
        },
        onThinking(text) {
          currentThinking.value += text
        },
        onDone() {
          isGenerating.value = false
          resolve({
            content: currentContent.value,
            thinking: currentThinking.value
          })
        },
        onError(msg) {
          isGenerating.value = false
          reject(new Error(msg))
        }
      })
    })
  }

  /**
   * 停止生成
   */
  function stopGeneration() {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    isGenerating.value = false
  }

  /**
   * 上传图片
   */
  async function uploadImage(file) {
    const { data } = await aiApi.uploadImage(file)
    return data
  }

  return {
    isGenerating,
    currentContent,
    currentThinking,
    streamChat,
    stopGeneration,
    uploadImage
  }
}

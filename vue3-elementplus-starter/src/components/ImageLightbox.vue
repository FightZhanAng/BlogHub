<template>
  <Teleport to="body">
    <Transition name="lightbox">
      <div v-if="visible" class="lightbox-overlay" @click.self="close" @keydown.esc="close" tabindex="0">
        <button class="lightbox-close" @click="close">&times;</button>
        <video v-if="isVideo" :src="src" class="lightbox-video" controls autoplay />
        <img v-else :src="src" :alt="alt" class="lightbox-img" />
        <div v-if="alt" class="lightbox-alt">{{ alt }}</div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const emit = defineEmits(['close'])

const visible = ref(false)
const src = ref('')
const alt = ref('')
const isVideo = ref(false)

function open(newSrc, newAlt = '') {
  src.value = newSrc || ''
  alt.value = newAlt
  isVideo.value = /\.(mp4|webm|ogg|mov)(\?|$)/i.test(newSrc || '')
  visible.value = true
}

function close() {
  visible.value = false
  emit('close')
}

function onKeydown(e) {
  if (e.key === 'Escape' && visible.value) close()
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))

defineExpose({ open, close })
</script>

<style scoped>
.lightbox-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  cursor: zoom-out;
}

.lightbox-img {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 4px;
  user-select: none;
}

.lightbox-video {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 4px;
  outline: none;
}

.lightbox-close {
  position: absolute;
  top: 16px;
  right: 20px;
  background: none;
  border: none;
  color: #fff;
  font-size: 36px;
  cursor: pointer;
  opacity: 0.8;
  transition: opacity 0.2s;
  line-height: 1;
}

.lightbox-close:hover { opacity: 1; }

.lightbox-alt {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  background: rgba(0, 0, 0, 0.5);
  padding: 6px 16px;
  border-radius: 20px;
}

.lightbox-enter-active,
.lightbox-leave-active { transition: opacity 0.25s; }
.lightbox-enter-from,
.lightbox-leave-to { opacity: 0; }
</style>

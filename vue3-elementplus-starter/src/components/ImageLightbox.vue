<template>
  <Teleport to="body">
    <Transition name="lightbox">
      <div v-if="visible" class="lightbox-overlay" @click.self="close" @keydown.esc="close" tabindex="0">
        <button class="lightbox-close" @click="close">&times;</button>
        <img v-if="visible" :src="src" :alt="alt" class="lightbox-img" @load="loaded = true" />
        <div v-if="alt" class="lightbox-alt">{{ alt }}</div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  src: String,
  alt: { type: String, default: '' },
})

const emit = defineEmits(['close'])

const visible = ref(false)
const loaded = ref(false)

function open() { visible.value = true; loaded.value = false }
function close() { visible.value = false; loaded.value = false; emit('close') }

function onKeydown(e) {
  if (e.key === 'Escape' && visible.value) close()
}

watch(() => props.src, (val) => {
  if (val) open()
})

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
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

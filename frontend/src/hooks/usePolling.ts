/** 轮询 Hook（规范 §三十：暂不实现 SSE 前先 Polling，默认 5s）。 */
import { onUnmounted, ref } from 'vue'

export function usePolling(fn: () => void | Promise<void>, intervalMs = 5000) {
  const polling = ref(true)
  let timer: ReturnType<typeof setInterval> | null = null

  function start(): void {
    if (timer !== null) return
    polling.value = true
    timer = setInterval(() => {
      void fn()
    }, intervalMs)
  }

  function stop(): void {
    if (timer !== null) {
      clearInterval(timer)
      timer = null
    }
    polling.value = false
  }

  start()
  onUnmounted(stop)
  return { polling, start, stop }
}

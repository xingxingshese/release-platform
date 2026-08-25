/** 异步资源加载 Hook：Loading / Error / Empty / Success / Retry 五态全覆盖（规范 §三十）。 */
import { ref, onMounted, type Ref } from 'vue'
import { ApiError } from '../api/client'

export interface AsyncState<T> {
  data: Ref<T | null>
  loading: Ref<boolean>
  error: Ref<string>
  empty: boolean
  reload: () => Promise<void>
}

export function useAsync<T>(fetcher: () => Promise<T>, immediate = true): AsyncState<T> {
  const data: Ref<T | null> = ref(null)
  const loading = ref(false)
  const error = ref('')
  let firstLoadDone = false

  async function reload(): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      const result = await fetcher()
      data.value = result
      firstLoadDone = true
    } catch (e) {
      error.value = e instanceof ApiError ? `${e.code}: ${e.message}` : String(e)
    } finally {
      loading.value = false
    }
  }

  if (immediate) {
    onMounted(reload)
  }
  return {
    data,
    loading,
    error,
    get empty(): boolean {
      return firstLoadDone && !loading.value && !error.value && isEmpty(data.value)
    },
    reload
  }
}

function isEmpty(v: unknown): boolean {
  if (v == null) return true
  if (Array.isArray(v)) return v.length === 0
  return false
}

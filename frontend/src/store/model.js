import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { modelApi } from '@/api/model'

export const useModelStore = defineStore(
  'model',
  () => {
    const models = ref([])
    const providers = ref([])
    const currentModel = ref(null)  // model code 字符串
    const loading = ref(false)

    async function loadModels() {
      loading.value = true
      try {
        const res = await modelApi.list()
        models.value = res.data || []
        // 默认选第一个
        if (!currentModel.value && models.value.length > 0) {
          currentModel.value = models.value[0].code
        }
      } finally {
        loading.value = false
      }
    }

    async function loadProviders() {
      const res = await modelApi.providers()
      providers.value = res.data || []
    }

    function setCurrentModel(code) {
      currentModel.value = code
    }

    const currentModelObj = computed(() =>
      models.value.find(m => m.code === currentModel.value) || null
    )

    return {
      models, providers, currentModel, currentModelObj, loading,
      loadModels, loadProviders, setCurrentModel
    }
  },
  {
    persist: {
      key: 'minimax-model',
      storage: localStorage,
      paths: ['currentModel']   // 持久化当前选中的模型
    }
  }
)

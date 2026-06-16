<template>
  <div class="m-kg">
    <van-nav-bar title="🕸️ 知识图谱" fixed :border="false" />

    <div class="content">
      <van-search v-model="kw" placeholder="搜索实体..." @search="search" />

      <van-cell-group inset title="实体列表">
        <van-cell
          v-for="e in entities"
          :key="e.id"
          :title="e.name"
          :label="e.entityType + (e.description ? ' · ' + e.description : '')"
          is-link
          @click="selectEntity(e)"
        >
          <template #value>
            <van-tag plain type="primary">{{ e.importance || 5 }}</van-tag>
          </template>
        </van-cell>
        <van-empty v-if="!entities.length" description="暂无数据, 试试搜索" />
      </van-cell-group>

      <van-popup v-model:show="showDetail" position="bottom" round :style="{ height: '70%' }">
        <div v-if="selected" class="detail">
          <h2>{{ selected.name }}</h2>
          <van-tag type="primary">{{ selected.entityType }}</van-tag>
          <p v-if="selected.description">{{ selected.description }}</p>

          <h3>关联 ({{ neighbors.length }})</h3>
          <van-cell
            v-for="(n, i) in neighbors"
            :key="i"
            :title="n.entity.name"
            :label="`${n.hop}跳 · via ${n.via}`"
            :value="n.entity.entityType"
          />
        </div>
      </van-popup>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import axios from 'axios'
import { useUserStore } from '@/store/user'

const API = import.meta.env.VITE_API_BASE || 'http://localhost'
const userStore = useUserStore()

const kw = ref('')
const entities = ref<any[]>([])
const selected = ref<any>(null)
const neighbors = ref<any[]>([])
const showDetail = ref(false)

function auth() {
  return { headers: { Authorization: `Bearer ${userStore.accessToken}` } }
}

async function search() {
  try {
    const { data } = await axios.get(`${API}/api/v1/agent/kg/entities/search`, {
      params: { userId: userStore.profile?.id || 1, keyword: kw.value || 'a', limit: 50 },
      ...auth(),
    })
    entities.value = data.data || []
  } catch (e: any) {
    showToast('搜索失败')
  }
}

async function selectEntity(e: any) {
  selected.value = e
  showDetail.value = true
  try {
    const { data } = await axios.get(
      `${API}/api/v1/agent/kg/entities/${e.id}/neighbors`, auth())
    neighbors.value = data.data || []
  } catch (e: any) {
    neighbors.value = []
  }
}

onMounted(search)
</script>

<style scoped>
.m-kg { min-height: 100vh; background: #f5f7fa; }
.content { padding: 50px 0 60px; }
.detail { padding: 16px; }
.detail h2 { margin: 0 0 8px; }
.detail h3 { margin: 16px 0 8px; font-size: 14px; color: #303133; }
</style>

<template>
  <H5Layout title="项目文档" :show-back="true">
    <div v-if="loading" class="loading-area">
      <a-spin />
    </div>

    <div v-else-if="documents.length === 0" class="empty-area">
      <a-empty description="暂无可下载文档" />
    </div>

    <div v-else class="doc-list">
      <div
        v-for="doc in documents"
        :key="doc.docId"
        class="doc-item"
        @click="onDownload(doc)"
      >
        <div class="doc-icon" :class="`doc-${doc.docType.toLowerCase()}`">
          {{ getDocIcon(doc.docType) }}
        </div>
        <div class="doc-info">
          <div class="doc-name">{{ doc.docName }}</div>
          <div class="doc-meta">
            <span>{{ formatDocType(doc.docType) }}</span>
            <span v-if="doc.uploadTime">{{ doc.uploadTime }}</span>
          </div>
        </div>
        <div class="doc-action">
          <DownloadOutlined />
        </div>
      </div>
    </div>
  </H5Layout>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import H5Layout from '@/layouts/H5Layout.vue'
import { getProjectDocuments } from '@/api/customerPortal'
import type { CustomerDocument } from '@/types/customerPortal'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const documents = ref<CustomerDocument[]>([])

onMounted(async () => {
  const projectId = Number(route.params.projectId)
  if (!projectId) {
    message.error('项目ID缺失')
    router.back()
    return
  }
  loading.value = true
  try {
    documents.value = (await getProjectDocuments(projectId)) || []
  } catch (e: any) {
    console.error('[H5 customer documents]', e)
    if (e?.code === 401 || e?.code === 40301) {
      router.push({
        path: '/h5/customer/login',
        query: { redirect: route.fullPath }
      })
    } else {
      message.error(e?.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
})

function onDownload(doc: CustomerDocument) {
  if (!doc.downloadUrl) {
    message.warning('该文档暂无下载链接')
    return
  }
  message.loading('正在打开下载...', 0.8)
  // 预签名 URL 是直接访问 MinIO 的，新开窗口下载
  window.open(doc.downloadUrl, '_blank')
}

function getDocIcon(type: string): string {
  const map: Record<string, string> = {
    DESIGN: '📐',
    REPORT: '📊',
    CONFIG: '⚙️',
    OTHER: '📄'
  }
  return map[type] || '📄'
}

function formatDocType(type: string): string {
  const map: Record<string, string> = {
    DESIGN: '设计方案',
    REPORT: '测试报告',
    CONFIG: '配置文件',
    OTHER: '其他'
  }
  return map[type] || type
}
</script>

<style scoped>
.loading-area,
.empty-area {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px 0;
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.doc-item {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.15s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.doc-item:active {
  transform: scale(0.98);
}

.doc-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: #f0f5ff;
}

.doc-design {
  background: #e6f4ff;
}

.doc-report {
  background: #f6ffed;
}

.doc-config {
  background: #fff7e6;
}

.doc-other {
  background: #f5f5f5;
}

.doc-info {
  flex: 1;
  min-width: 0;
}

.doc-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 4px;
  word-break: break-all;
}

.doc-meta {
  font-size: 12px;
  color: #888;
  display: flex;
  gap: 12px;
}

.doc-action {
  color: #1677ff;
  font-size: 18px;
}
</style>

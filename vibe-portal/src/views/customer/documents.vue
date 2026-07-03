<template>
  <div class="documents">
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <!-- 项目选择 -->
      <section class="card project-picker">
        <div class="project-picker__label">所属项目</div>
        <div class="project-picker__select" @click="showProjectSheet = true">
          <span class="project-picker__name ellipsis">
            {{ currentProject ? currentProject.projectName : '请选择项目' }}
          </span>
          <van-icon name="arrow-down" size="14" color="#8c8c8c" />
        </div>
      </section>

      <van-search
        v-model="keyword"
        placeholder="搜索文档名称"
        shape="round"
        @search="onSearch"
      />

      <ul class="documents__list">
        <li v-for="doc in filteredList" :key="doc.id" class="doc-item">
          <div class="doc-item__icon" :class="`doc-item__icon--${fileTypeIcon(doc.fileType || '')}`">
            <van-icon :name="fileIconName(doc.fileType || '')" size="22" />
            <span>{{ (doc.fileType || 'file').toUpperCase() }}</span>
          </div>
          <div class="doc-item__body">
            <div class="doc-name ellipsis-2">{{ doc.name }}</div>
            <div class="doc-meta">
              <span v-if="doc.version">v{{ doc.version }}</span>
              <span v-if="doc.size">{{ formatSize(doc.size) }}</span>
              <span v-if="doc.uploadTime">{{ formatDate(doc.uploadTime) }}</span>
            </div>
          </div>
          <van-button
            size="small"
            type="primary"
            plain
            :loading="downloadingId === doc.id"
            @click="onDownload(doc)"
          >
            下载
          </van-button>
        </li>
        <li v-if="!filteredList.length" class="empty">
          <van-empty :description="currentProject ? '暂无可下载文档' : '请先选择项目'" />
        </li>
      </ul>
    </van-pull-refresh>

    <!-- 项目选择 ActionSheet -->
    <van-action-sheet
      v-model:show="showProjectSheet"
      title="选择项目"
      :actions="projectActions"
      close-on-click-action
      cancel-text="取消"
      :round="true"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import dayjs from 'dayjs'
import { getMyProjects, getProjectDocuments, getDocumentDownloadUrl } from '@/api/customer'
import type { CustomerProjectVO, CustomerDocumentVO } from '@/types/api'

const route = useRoute()

const projects = ref<CustomerProjectVO[]>([])
const currentProject = ref<CustomerProjectVO | undefined>()
const list = ref<CustomerDocumentVO[]>([])
const keyword = ref('')
const refreshing = ref(false)
const showProjectSheet = ref(false)
const downloadingId = ref<number | string | undefined>()

const filteredList = computed(() => {
  if (!keyword.value.trim()) return list.value
  const kw = keyword.value.trim().toLowerCase()
  return list.value.filter((d) => d.name.toLowerCase().includes(kw))
})

const projectActions = computed(() =>
  projects.value.map((p) => ({
    name: p.projectName,
    subname: p.projectCode,
    color: currentProject.value?.projectId === p.projectId ? '#1677ff' : '#1a1a1a',
    callback: () => selectProject(p)
  }))
)

/** 拉取项目列表 */
async function fetchProjects() {
  try {
    const data = await getMyProjects()
    projects.value = data || []
    // 优先用路由 query 中的 projectId，否则默认第一个
    const qid = route.query.projectId as string | undefined
    let target: CustomerProjectVO | undefined
    if (qid) target = projects.value.find((p) => String(p.projectId) === qid)
    if (!target && projects.value.length) target = projects.value[0]
    if (target) await selectProject(target)
  } catch (e) {
    // 拦截器已提示
  }
}

async function selectProject(p: CustomerProjectVO) {
  currentProject.value = p
  showProjectSheet.value = false
  await fetchDocuments(p.projectId)
}

async function fetchDocuments(projectId: number | string) {
  try {
    const data = await getProjectDocuments(projectId)
    list.value = data || []
  } catch (e) {
    // 拦截器已提示
  }
}

function onRefresh() {
  const pid = currentProject.value?.projectId
  Promise.all([fetchProjects(), pid ? fetchDocuments(pid) : Promise.resolve()]).finally(() => {
    refreshing.value = false
  })
}

function onSearch() {
  // 前端过滤即可
}

function fileIconName(type: string): string {
  if (['png', 'jpg', 'jpeg'].includes(type)) return 'photo-o'
  return 'description'
}

function fileTypeIcon(type: string): string {
  if (type === 'pdf') return 'pdf'
  if (['doc', 'docx'].includes(type)) return 'doc'
  if (['xls', 'xlsx'].includes(type)) return 'xls'
  return 'other'
}

function formatSize(size: number): string {
  if (size < 1024) return `${size}B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)}KB`
  return `${(size / 1024 / 1024).toFixed(1)}MB`
}

function formatDate(d: string): string {
  return dayjs(d).format('YYYY-MM-DD')
}

async function onDownload(doc: CustomerDocumentVO) {
  if (!currentProject.value) {
    showToast('请先选择项目')
    return
  }
  downloadingId.value = doc.id
  try {
    // 优先使用已有 downloadUrl，否则请求预签名 URL
    let url = doc.downloadUrl
    if (!url) {
      url = await getDocumentDownloadUrl(currentProject.value.projectId, doc.id)
    }
    if (!url) {
      showToast('暂无下载地址')
      return
    }
    // 通过隐藏 a 标签触发下载
    const a = document.createElement('a')
    a.href = url
    a.download = doc.name
    a.target = '_blank'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
  } catch (e) {
    // 拦截器已提示
  } finally {
    downloadingId.value = undefined
  }
}

onMounted(fetchProjects)
</script>

<style lang="scss" scoped>
.documents {
  min-height: 100%;
  background: var(--bg-page);

  &__list {
    padding: 8px 12px;
  }
}

.project-picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  margin: 12px;

  &__label {
    font-size: 13px;
    color: var(--color-text-secondary);
    flex-shrink: 0;
  }

  &__select {
    display: flex;
    align-items: center;
    gap: 6px;
    flex: 1;
    min-width: 0;
    justify-content: flex-end;
  }

  &__name {
    font-size: 14px;
    color: var(--color-text-primary);
    font-weight: 500;
    max-width: 220px;
  }
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);

  &__icon {
    width: 40px;
    height: 48px;
    border-radius: 4px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 10px;
    flex-shrink: 0;

    &--pdf {
      background: #ff4d4f;
    }
    &--doc {
      background: #1677ff;
    }
    &--xls {
      background: #52c41a;
    }
    &--other {
      background: #8c8c8c;
    }
  }

  &__body {
    flex: 1;
    min-width: 0;

    .doc-name {
      font-size: 14px;
      color: var(--color-text-primary);
      line-height: 1.4;
    }
    .doc-meta {
      margin-top: 6px;
      display: flex;
      gap: 8px;
      font-size: 12px;
      color: var(--color-text-secondary);
    }
  }
}

.empty {
  padding: 32px 0;
}
</style>

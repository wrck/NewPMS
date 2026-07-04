<template>
  <H5Layout title="提交交付物" :show-back="true">
    <div v-if="loading" class="loading-area">
      <a-spin />
    </div>

    <div v-else-if="!task" class="empty-area">
      <a-empty description="任务不存在" />
    </div>

    <div v-else class="deliverable-form">
      <!-- 任务信息卡片 -->
      <div class="task-info-card">
        <h3 class="task-title">{{ task.taskName || '-' }}</h3>
        <div class="task-meta" v-if="task.projectName">
          项目：{{ task.projectName }}
        </div>
        <div class="task-meta" v-if="task.taskScope">
          要求：{{ task.taskScope }}
        </div>
        <div class="task-meta" v-if="task.deadline">
          截止日期：{{ task.deadline }}
        </div>
      </div>

      <div class="section-title">
        <span>─── 完成情况 ───</span>
      </div>

      <!-- 上传表单 -->
      <div class="form-card">
        <a-form layout="vertical">
          <a-form-item label="施工照片（必传，至少3张）" required>
            <div class="upload-area">
              <div
                v-for="(photo, idx) in photoList"
                :key="idx"
                class="photo-item"
              >
                <img :src="photo.url" :alt="`photo-${idx + 1}`" />
                <div class="photo-remove" @click="removePhoto(idx)">×</div>
              </div>
              <label v-if="photoList.length < 9" class="photo-add">
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  @change="onPhotoChange"
                  style="display: none"
                />
                <div class="photo-add-inner">
                  <CameraOutlined />
                  <span>拍照/上传</span>
                </div>
              </label>
            </div>
            <div class="upload-tip">
              已选 {{ photoList.length }} 张（最少 3 张）
            </div>
          </a-form-item>

          <a-form-item label="测试记录（必传）" required>
            <label class="file-input">
              <input
                type="file"
                accept=".pdf,.doc,.docx,.xls,.xlsx"
                @change="onTestRecordChange"
                style="display: none"
              />
              <div class="file-input-area">
                <FilePdfOutlined v-if="testRecordFile" />
                <UploadOutlined v-else />
                <span>{{ testRecordFile?.name || '点击上传测试报告' }}</span>
              </div>
            </label>
          </a-form-item>

          <a-form-item label="备注说明">
            <a-textarea
              v-model:value="form.remark"
              placeholder="如有特殊情况请说明（如安装位置调整、客户现场协调等）"
              :rows="4"
              :maxlength="500"
              show-count
            />
          </a-form-item>

          <div class="submit-row">
            <a-button
              size="large"
              block
              :loading="submitting"
              @click="onSubmit"
              type="primary"
            >
              提交交付物
            </a-button>
          </div>
        </a-form>
      </div>
    </div>
  </H5Layout>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CameraOutlined,
  FilePdfOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'
import H5Layout from '@/layouts/H5Layout.vue'
import { getOutsourceTaskDetail, uploadDeliverable } from '@/api/agent'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const submitting = ref(false)
const task = ref<any>(null)

interface PhotoItem {
  url: string
  file?: File
  name: string
}

const photoList = ref<PhotoItem[]>([])
const testRecordFile = ref<File | null>(null)

const form = reactive({
  remark: ''
})

const taskId = route.query.taskId ? Number(route.query.taskId) : 0

onMounted(async () => {
  if (!taskId) {
    message.error('任务ID缺失')
    router.back()
    return
  }
  loading.value = true
  try {
    task.value = await getOutsourceTaskDetail(taskId)
  } catch (e: any) {
    console.error('[H5 agent deliverable-submit]', e)
    if (e?.code === 401 || e?.code === 40301) {
      router.push({
        path: '/h5/agent/login',
        query: { redirect: route.fullPath }
      })
    } else {
      message.error(e?.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
})

function onPhotoChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files) return
  const files = Array.from(input.files)
  const remaining = 9 - photoList.value.length
  const toAdd = files.slice(0, remaining)
  for (const file of toAdd) {
    if (!file.type.startsWith('image/')) {
      message.warning(`${file.name} 不是图片，已跳过`)
      continue
    }
    const url = URL.createObjectURL(file)
    photoList.value.push({
      url,
      file,
      name: file.name
    })
  }
  // 清空 input value 以便重复选择同一文件
  input.value = ''
}

function removePhoto(idx: number) {
  const photo = photoList.value[idx]
  if (photo.url.startsWith('blob:')) {
    URL.revokeObjectURL(photo.url)
  }
  photoList.value.splice(idx, 1)
}

function onTestRecordChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return
  testRecordFile.value = input.files[0]
}

async function onSubmit() {
  if (photoList.value.length < 3) {
    message.warning('请至少上传 3 张施工照片')
    return
  }
  if (!testRecordFile.value) {
    message.warning('请上传测试记录')
    return
  }
  submitting.value = true
  try {
    // 提交施工照片（每张作为一条 PHOTO 类型交付物）
    for (const photo of photoList.value) {
      // 实际场景应先上传到 MinIO 再保存 URL，这里 MVP 演示直接传文件名
      // 后端 uploadDeliverable 接受 OutsourceDeliverable，其中 fileUrl 是必填字段
      // 由于 MVP 后端未提供文件上传接口，这里用 base64 或 blob URL 临时占位
      await uploadDeliverable(taskId, {
        taskId,
        deliverableType: 'PHOTO',
        deliverableName: photo.name,
        fileUrl: photo.url,
        remark: form.remark || undefined
      })
    }
    // 提交测试记录
    await uploadDeliverable(taskId, {
      taskId,
      deliverableType: 'TEST_RECORD',
      deliverableName: testRecordFile.value.name,
      fileUrl: URL.createObjectURL(testRecordFile.value),
      remark: form.remark || undefined
    })

    message.success('交付物已提交')
    router.replace('/h5/agent/workbench')
  } catch (e: any) {
    console.error('[H5 submit deliverable]', e)
    if (e?.code === 401 || e?.code === 40301) {
      message.warning('请先登录')
      router.push({
        path: '/h5/agent/login',
        query: { redirect: route.fullPath }
      })
    } else {
      message.error(e?.message || '提交失败')
    }
  } finally {
    submitting.value = false
  }
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

.task-info-card {
  background: linear-gradient(135deg, #fff7e6 0%, #fff1f0 100%);
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 16px;
  border: 1px solid #ffd591;
}

.task-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px;
}

.task-meta {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
  word-break: break-all;
}

.section-title {
  text-align: center;
  font-size: 13px;
  color: #999;
  margin: 16px 0 12px;
}

.form-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}

.upload-area {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.photo-item {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
}

.photo-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-remove {
  position: absolute;
  top: 0;
  right: 0;
  width: 18px;
  height: 18px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border-radius: 0 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  cursor: pointer;
}

.photo-add {
  width: 72px;
  height: 72px;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.photo-add-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #999;
  font-size: 11px;
  gap: 4px;
}

.photo-add-inner :deep(.anticon) {
  font-size: 20px;
}

.upload-tip {
  font-size: 12px;
  color: #888;
  margin-top: 8px;
}

.file-input {
  display: block;
  cursor: pointer;
}

.file-input-area {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  color: #666;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.file-input-area:hover {
  border-color: #1677ff;
  color: #1677ff;
}

.submit-row {
  margin-top: 16px;
}
</style>

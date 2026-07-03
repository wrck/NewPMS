<template>
  <div class="submit-deliverable">
    <van-form @submit="onSubmit">
      <!-- 任务信息 -->
      <section class="card">
        <div class="section-title">任务信息</div>
        <van-cell-group :border="false">
          <van-cell title="任务" :value="detail?.siteName || '加载中...'" />
          <van-cell title="项目" :value="detail?.projectName || '-'" />
          <van-cell v-if="detail?.progressText" title="完成情况" :value="detail.progressText" />
        </van-cell-group>
      </section>

      <!-- 施工照片（必传，至少 minPhotos 张） -->
      <section class="card">
        <div class="section-title">
          施工照片
          <span class="required">*</span>
          <span class="hint">至少 {{ minPhotos }} 张</span>
        </div>
        <PhotoUpload
          ref="photoRef"
          v-model="form.photos"
          :max-count="20"
          :min-count="minPhotos"
          :auth="'agent'"
          upload-url="/v1/files/upload"
          hint="支持拍照或从相册选择，单张最大 10MB，自动叠加时间水印"
        />
      </section>

      <!-- 测试记录（必传） -->
      <section class="card">
        <div class="section-title">
          测试记录
          <span class="required">*</span>
        </div>
        <FileUpload
          ref="testRecordRef"
          v-model="form.testRecords"
          :multiple="true"
          :max-count="10"
          :auth="'agent'"
          upload-url="/v1/files/upload"
          accept=".pdf,.doc,.docx,.xls,.xlsx,image/*"
          button-text="上传测试记录"
          hint="支持 PDF/Word/Excel/图片，单个最大 20MB"
        />
      </section>

      <!-- 签收单（必传） -->
      <section class="card">
        <div class="section-title">
          签收单
          <span class="required">*</span>
        </div>
        <FileUpload
          ref="receiptRef"
          v-model="form.receipts"
          :multiple="true"
          :max-count="10"
          :auth="'agent'"
          upload-url="/v1/files/upload"
          accept=".pdf,.doc,.docx,image/*"
          button-text="上传签收单"
          hint="支持 PDF/Word/图片，单个最大 20MB"
        />
      </section>

      <!-- 备注 -->
      <section class="card">
        <div class="section-title">备注</div>
        <van-field
          v-model="form.remark"
          type="textarea"
          placeholder="请填写需要说明的事项（如客户现场变更、特殊情况等）"
          rows="3"
          autosize
          maxlength="500"
          show-word-limit
        />
      </section>
    </van-form>

    <!-- 底部操作 -->
    <div class="bottom-action submit-deliverable__action">
      <van-button block type="primary" :loading="submitting" @click="onSubmit">
        提交交付物
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import PhotoUpload from '@/components/PhotoUpload.vue'
import FileUpload from '@/components/FileUpload.vue'
import { getOutsourceTaskDetail, submitDeliverable } from '@/api/agent'
import type { OutsourceTaskDetailVO, SubmitDeliverableParams } from '@/types/api'

const route = useRoute()
const router = useRouter()

const detail = ref<OutsourceTaskDetailVO | undefined>()
const taskId = computed(() => route.params.id as string)
const minPhotos = computed(() => {
  const req = detail.value?.deliverableRequirements?.find((r) => r.type === 'PHOTO')
  return req?.minCount || 3
})

const form = reactive<SubmitDeliverableParams>({
  photos: [],
  testRecords: [],
  receipts: [],
  remark: ''
})

const submitting = ref(false)

const photoRef = ref<InstanceType<typeof PhotoUpload>>()
const testRecordRef = ref<InstanceType<typeof FileUpload>>()
const receiptRef = ref<InstanceType<typeof FileUpload>>()

async function fetchDetail() {
  try {
    detail.value = await getOutsourceTaskDetail(taskId.value)
  } catch (e) {
    // 拦截器已提示，仍允许提交
  }
}

function validate(): boolean {
  // 施工照片校验（数量 + 上传完成）
  if (!photoRef.value?.validate()) {
    return false
  }
  form.photos = photoRef.value.getUrls()
  if (form.photos.length < minPhotos.value) {
    showToast(`施工照片至少 ${minPhotos.value} 张`)
    return false
  }
  // 测试记录校验
  if (!testRecordRef.value?.validate()) {
    return false
  }
  form.testRecords = testRecordRef.value.getUrls()
  if (!form.testRecords.length) {
    showToast('请上传测试记录')
    return false
  }
  // 签收单校验
  if (!receiptRef.value?.validate()) {
    return false
  }
  form.receipts = receiptRef.value.getUrls()
  if (!form.receipts.length) {
    showToast('请上传签收单')
    return false
  }
  return true
}

async function onSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    await submitDeliverable(taskId.value, {
      photos: form.photos,
      testRecords: form.testRecords,
      receipts: form.receipts,
      remark: form.remark || undefined
    })
    showSuccessToast('提交成功')
    setTimeout(() => router.replace(`/agent/tasks/${taskId.value}`), 800)
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

onMounted(fetchDetail)
</script>

<style lang="scss" scoped>
.submit-deliverable {
  padding: 12px;
  min-height: 100%;
  padding-bottom: calc(72px + var(--safe-bottom));

  .section-title {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 15px;
    font-weight: 600;
    margin-bottom: 10px;

    .required {
      color: var(--color-danger);
    }
    .hint {
      margin-left: auto;
      font-size: 12px;
      font-weight: 400;
      color: var(--color-text-secondary);
    }
  }

  &__action {
    display: flex;
    gap: 12px;

    .van-button {
      flex: 1;
    }
  }
}
</style>

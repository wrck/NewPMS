<script setup lang="ts">
/**
 * 反馈悬浮按钮（Task D5.2）
 *
 * 全局右下角 fixed 悬浮按钮，点击弹出反馈提交 Modal：
 *   - 反馈类型（BUG / 建议 / 疑问）
 *   - 标题（必填，≤200）
 *   - 内容描述（≤2000）
 *   - 联系方式（手机号/邮箱，可选）
 *   - 截图上传（FileUpload 组件，可选，最多 4 张）
 *
 * 提交成功后：
 *   - 弹出 success 提示
 *   - 关闭 Modal 并清空表单
 *   - 触发 submitted 事件（供父组件感知）
 *
 * 任何登录用户均可使用（不限管理员）。
 */
import { ref, reactive, computed } from 'vue'
import { message, Modal as AModal } from 'ant-design-vue'
import { MessageOutlined } from '@ant-design/icons-vue'
import FileUpload from '@/components/FileUpload'
import { submitFeedback } from '@/api/feedback'
import type { SysFeedbackDTO, FeedbackType } from '@/api/feedback'

interface Props {
  /** 是否显示悬浮按钮，默认 true（父组件可基于登录状态控制） */
  visible?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  visible: true
})

const emit = defineEmits<{
  (e: 'submitted', id: number): void
}>()

const modalVisible = ref(false)
const submitting = ref(false)

const form = reactive<{
  type: FeedbackType
  title: string
  content: string
  contact: string
  screenshotUrl: string | string[]
}>({
  type: 'BUG',
  title: '',
  content: '',
  contact: '',
  screenshotUrl: []
})

const typeOptions: Array<{ label: string; value: FeedbackType; color: string }> = [
  { label: 'Bug 反馈', value: 'BUG', color: 'red' },
  { label: '功能建议', value: 'SUGGESTION', color: 'blue' },
  { label: '使用疑问', value: 'QUESTION', color: 'gold' }
]

/** 当前选中类型的展示色（按钮徽标） */
const currentTypeColor = computed(
  () => typeOptions.find((o) => o.value === form.type)?.color || 'blue'
)

function openModal() {
  Object.assign(form, {
    type: 'BUG',
    title: '',
    content: '',
    contact: '',
    screenshotUrl: []
  })
  modalVisible.value = true
}

function closeModal() {
  modalVisible.value = false
}

/** 将 string | string[] 统一为逗号分隔字符串（与后端 SysFeedbackDTO.screenshotUrl 字段约定） */
function normalizeScreenshot(v: string | string[]): string {
  if (Array.isArray(v)) return v.filter(Boolean).join(',')
  return v || ''
}

async function handleSubmit() {
  if (!form.title || !form.title.trim()) {
    message.warning('请填写标题')
    return
  }
  if (form.title.length > 200) {
    message.warning('标题长度不能超过 200')
    return
  }
  if (form.content.length > 2000) {
    message.warning('内容长度不能超过 2000')
    return
  }

  submitting.value = true
  try {
    const dto: SysFeedbackDTO = {
      type: form.type,
      title: form.title.trim(),
      content: form.content.trim() || undefined,
      contact: form.contact.trim() || undefined,
      screenshotUrl: normalizeScreenshot(form.screenshotUrl) || undefined
    }
    const id = (await submitFeedback(dto)) as unknown as number
    message.success('反馈已提交，我们会尽快处理并通过站内信回复您')
    emit('submitted', id)
    closeModal()
  } catch (e) {
    console.error('[FeedbackButton] submit failed:', e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <teleport to="body">
    <div v-if="props.visible" class="feedback-fab" @click="openModal">
      <MessageOutlined class="feedback-fab-icon" />
      <span class="feedback-fab-text">反馈</span>
      <span class="feedback-fab-pulse" />
    </div>

    <AModal
      v-model:open="modalVisible"
      title="提交反馈"
      width="640px"
      :confirm-loading="submitting"
      :mask-closable="false"
      ok-text="提交反馈"
      cancel-text="取消"
      @ok="handleSubmit"
    >
      <div class="feedback-modal-body">
        <a-form :model="form" layout="vertical">
          <a-form-item label="反馈类型" name="type" required>
            <a-radio-group v-model:value="form.type" button-style="solid">
              <a-radio-button
                v-for="opt in typeOptions"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </a-radio-button>
            </a-radio-group>
          </a-form-item>

          <a-form-item label="标题" name="title" required>
            <a-input
              v-model:value="form.title"
              placeholder="一句话描述您的问题或建议"
              :max-length="200"
              show-count
            />
          </a-form-item>

          <a-form-item label="详细描述" name="content">
            <a-textarea
              v-model:value="form.content"
              :rows="5"
              placeholder="请详细描述：&#10;- Bug：复现步骤 / 期望结果 / 实际结果&#10;- 建议：使用场景 / 期望效果&#10;- 疑问：具体问题"
              :max-length="2000"
              show-count
            />
          </a-form-item>

          <a-form-item label="截图（可选）" name="screenshotUrl">
            <FileUpload
              v-model="form.screenshotUrl"
              accept="image/*"
              :max-count="4"
              :max-size="10"
              multiple
              list-type="picture-card"
              dir="feedback"
            />
            <div class="form-hint">最多上传 4 张截图，每张不超过 10MB</div>
          </a-form-item>

          <a-form-item label="联系方式（可选）" name="contact">
            <a-input
              v-model:value="form.contact"
              placeholder="手机号或邮箱，便于我们与您联系"
              :max-length="100"
            />
          </a-form-item>
        </a-form>

        <div class="feedback-tip">
          <span class="feedback-tip-dot" :class="`tone-${currentTypeColor}`" />
          <span>提交后将进入反馈中心，管理员处理后会通过站内信通知您。</span>
        </div>
      </div>
    </AModal>
  </teleport>
</template>

<style lang="less" scoped>
.feedback-fab {
  position: fixed;
  right: 32px;
  bottom: 32px;
  z-index: 900;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 44px;
  padding: 0 18px;
  background: linear-gradient(135deg, @brand-primary 0%, saturate(@brand-primary, 15%) 100%);
  color: #fff;
  border-radius: 22px;
  box-shadow: 0 6px 16px rgba(@brand-primary, 0.35);
  cursor: pointer;
  user-select: none;
  transition: all 0.25s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(@brand-primary, 0.5);
  }

  &:active {
    transform: translateY(0);
  }
}

.feedback-fab-icon {
  font-size: 18px;
}

.feedback-fab-text {
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.feedback-fab-pulse {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 22px;
  border: 2px solid @brand-primary;
  opacity: 0;
  pointer-events: none;
  animation: feedback-pulse 2.4s ease-out infinite;
}

@keyframes feedback-pulse {
  0% {
    transform: scale(1);
    opacity: 0.7;
  }
  70% {
    transform: scale(1.15);
    opacity: 0;
  }
  100% {
    transform: scale(1.15);
    opacity: 0;
  }
}

.feedback-modal-body {
  padding: 8px 4px 0;
}

.form-hint {
  margin-top: 4px;
  font-size: 12px;
  color: @text-tertiary;
}

.feedback-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  background: @bg-sub;
  border-radius: 6px;
  font-size: 12px;
  color: @text-secondary;

  &-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;

    &.tone-red {
      background: @status-exception;
    }
    &.tone-blue {
      background: @brand-primary;
    }
    &.tone-gold {
      background: @status-warning;
    }
  }
}

:deep(.ant-form-item) {
  margin-bottom: 16px;
}
</style>

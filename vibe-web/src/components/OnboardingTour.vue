<script setup lang="ts">
/**
 * 新手引导教程（Task 10.2）
 *
 * 自研实现：纯 CSS + JS，不依赖 driver.js / intro.js 等外部库。
 *
 * 5 步引导（按 Spec 顺序）：
 *   ① 查看工作台 - 工作台首页概览
 *   ② 创建项目  - 项目列表页新建项目入口
 *   ③ 派发任务  - 资源调度 / 任务派发
 *   ④ 查看设备  - 设备台账
 *   ⑤ 提交验收  - 交付看板 / 现场作业完成提交
 *
 * 触发逻辑：
 *   - 首次登录（userStore.firstLogin / 无 localStorage 标记）自动触发
 *   - 完成或跳过后写 localStorage `onboarding_tour_done=1`，不再自动触发
 *   - 可通过 appStore.triggerTutorial() 重新打开
 *
 * 集成方式：在 BasicLayout.vue 中 <OnboardingTour ref="..." /> 即可。
 */
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Button } from 'ant-design-vue'
import {
  CloseOutlined,
  LeftOutlined,
  RightOutlined,
  CheckOutlined
} from '@ant-design/icons-vue'

interface TourStep {
  /** 目标元素选择器（多 selector 用逗号分隔，按顺序匹配第一个命中的） */
  selector: string
  /** 步骤标题 */
  title: string
  /** 步骤说明正文 */
  content: string
  /** 高亮区域内边距（像素），默认 8 */
  padding?: number
  /** 跳转路由（可选，进入该步骤前切换路由，以便 selector 命中） */
  route?: string
}

const STORAGE_KEY = 'onboarding_tour_done'

const steps: TourStep[] = [
  {
    selector: '[data-tour="dashboard"], .vibe-page-title, .ant-layout-content',
    title: '① 查看工作台',
    content:
      '欢迎来到 Vibe 交付管理平台！工作台首页按角色展示核心指标、待办事项、我负责的项目与最近动态，是您每天工作的起点。',
    padding: 10
  },
  {
    selector: '[data-tour="create-project"], .page-header-extra .ant-btn-primary, .ant-btn-primary',
    title: '② 创建项目',
    content:
      '进入「项目管理 > 项目列表」，点击右上角「新建项目」按钮即可创建项目。填写客户、产品线、执行模式、PM、计划周期等信息后保存，即可立项。',
    padding: 8
  },
  {
    selector: '[data-tour="dispatch"], .ant-menu-item:nth-of-type(4), .ant-layout-sider',
    title: '③ 派发任务',
    content:
      '进入「资源调度 > 任务派发」，对待派任务选择执行人（自有工程师或代理商），设置计划起止日期，确认派发后任务状态变为 ASSIGNED，工程师会收到通知。',
    padding: 6
  },
  {
    selector: '[data-tour="device-ledger"], .ant-layout-sider .ant-menu li:nth-child(3)',
    title: '④ 查看设备',
    content:
      '进入「设备资产 > 设备台账」可查看设备实例全量信息，支持按 SN / MAC / 状态 / 型号筛选，并支持 Excel 批量导入设备。',
    padding: 6
  },
  {
    selector: '[data-tour="delivery-board"], .ant-layout-sider .ant-menu li:nth-child(6)',
    title: '⑤ 提交验收',
    content:
      '进入「交付管理 > 交付看板」按状态查看工单。进行中工单可点击「标记完成」转交 PM 确认，PM 审核通过后即进入验收环节。完成本步即代表已掌握核心流程，可开始独立作业。',
    padding: 6
  }
]

const visible = ref(false)
const currentStep = ref(0)
const targetRect = ref<DOMRect | null>(null)

const emit = defineEmits<{
  (e: 'finished'): void
  (e: 'skipped'): void
}>()

const totalSteps = steps.length
const isFirst = computed(() => currentStep.value === 0)
const isLast = computed(() => currentStep.value === totalSteps - 1)

/** 步骤气泡位置：默认放在目标元素下方右侧，空间不够时自动调整 */
const popoverStyle = computed(() => {
  if (!targetRect.value) return {}
  const rect = targetRect.value
  const popoverWidth = 360
  const popoverHeight = 220
  const gap = 16

  let left = rect.right - popoverWidth
  if (left < 16) left = 16
  if (left + popoverWidth > window.innerWidth - 16) {
    left = window.innerWidth - popoverWidth - 16
  }

  let top = rect.bottom + gap
  if (top + popoverHeight > window.innerHeight - 16) {
    top = rect.top - popoverHeight - gap
    if (top < 16) top = 16
  }
  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${popoverWidth}px`
  }
})

/** 遮罩层使用 clip-path 挖出目标元素，形成高亮效果 */
const maskStyle = computed(() => {
  if (!targetRect.value) {
    return { clipPath: 'none' }
  }
  const rect = targetRect.value
  const padding = steps[currentStep.value].padding ?? 8
  const x = Math.max(0, rect.left - padding)
  const y = Math.max(0, rect.top - padding)
  const w = rect.width + padding * 2
  const h = rect.height + padding * 2
  return {
    clipPath: `polygon(0 0, 0 100%, ${x}px 100%, ${x}px ${y}px, ${x + w}px ${y}px, ${x + w}px ${y + h}px, ${x}px ${y + h}px, ${x}px 100%, 100% 100%, 100% 0)`
  }
})

function updateTargetRect() {
  if (!visible.value) return
  const step = steps[currentStep.value]
  if (!step) {
    targetRect.value = null
    return
  }
  const el = document.querySelector(step.selector) as HTMLElement | null
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' })
    setTimeout(() => {
      targetRect.value = el.getBoundingClientRect()
    }, 220)
  } else {
    targetRect.value = null
  }
}

function next() {
  if (currentStep.value < totalSteps - 1) {
    currentStep.value++
    nextTick(updateTargetRect)
  } else {
    finish()
  }
}

function prev() {
  if (currentStep.value > 0) {
    currentStep.value--
    nextTick(updateTargetRect)
  }
}

function finish() {
  visible.value = false
  try {
    localStorage.setItem(STORAGE_KEY, '1')
  } catch {
    // localStorage 不可用时忽略
  }
  emit('finished')
}

function skip() {
  visible.value = false
  try {
    localStorage.setItem(STORAGE_KEY, '1')
  } catch {
    // ignore
  }
  emit('skipped')
}

/** 对外暴露：开始教程 */
function start() {
  currentStep.value = 0
  visible.value = true
  nextTick(updateTargetRect)
}

defineExpose({ start, finish, visible })

function onResize() {
  if (visible.value) updateTargetRect()
}

onMounted(() => {
  window.addEventListener('resize', onResize)
  window.addEventListener('scroll', onResize, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('scroll', onResize, true)
})

watch(currentStep, () => {
  nextTick(updateTargetRect)
})
</script>

<template>
  <teleport to="body">
    <div v-if="visible" class="tour-overlay">
      <!-- 遮罩层：用 clip-path 挖出目标元素 -->
      <div class="tour-mask" :style="maskStyle" @click="skip" />

      <!-- 步骤气泡（有目标元素时定位在元素附近） -->
      <div v-if="targetRect" class="tour-popover" :style="popoverStyle">
        <div class="tour-popover-header">
          <span class="tour-step-indicator">步骤 {{ currentStep + 1 }} / {{ totalSteps }}</span>
          <a class="tour-close" title="关闭" @click="skip"><CloseOutlined /></a>
        </div>
        <div class="tour-popover-title">{{ steps[currentStep].title }}</div>
        <div class="tour-popover-content">{{ steps[currentStep].content }}</div>
        <div class="tour-popover-footer">
          <Button size="small" :disabled="isFirst" @click="prev">
            <template #icon><LeftOutlined /></template>
            上一步
          </Button>
          <span class="tour-footer-spacer" />
          <Button v-if="!isLast" type="primary" size="small" @click="next">
            下一步
            <template #icon><RightOutlined /></template>
          </Button>
          <Button v-else type="primary" size="small" @click="finish">
            <template #icon><CheckOutlined /></template>
            完成
          </Button>
        </div>
      </div>

      <!-- 没有目标元素时的中央气泡 -->
      <div v-else class="tour-popover tour-popover-center">
        <div class="tour-popover-header">
          <span class="tour-step-indicator">步骤 {{ currentStep + 1 }} / {{ totalSteps }}</span>
          <a class="tour-close" @click="skip"><CloseOutlined /></a>
        </div>
        <div class="tour-popover-title">{{ steps[currentStep].title }}</div>
        <div class="tour-popover-content">{{ steps[currentStep].content }}</div>
        <div class="tour-popover-footer">
          <Button size="small" :disabled="isFirst" @click="prev">
            <template #icon><LeftOutlined /></template>
            上一步
          </Button>
          <span class="tour-footer-spacer" />
          <Button v-if="!isLast" type="primary" size="small" @click="next">
            下一步
            <template #icon><RightOutlined /></template>
          </Button>
          <Button v-else type="primary" size="small" @click="finish">
            <template #icon><CheckOutlined /></template>
            完成
          </Button>
        </div>
      </div>

      <!-- 底部跳过按钮 -->
      <div class="tour-skip-bar">
        <Button size="small" type="text" @click="skip">跳过教程</Button>
      </div>
    </div>
  </teleport>
</template>

<style lang="less" scoped>
.tour-overlay {
  position: fixed;
  inset: 0;
  z-index: 1100;
  pointer-events: none;
}

.tour-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  pointer-events: auto;
  transition: clip-path 0.25s ease;
}

.tour-popover {
  position: absolute;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.18);
  padding: 16px 20px;
  pointer-events: auto;
  z-index: 1101;
  font-size: 13px;

  &-center {
    top: 50% !important;
    left: 50% !important;
    transform: translate(-50%, -50%);
  }

  &-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  &-title {
    font-size: 15px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.85);
    margin-bottom: 8px;
  }

  &-content {
    color: rgba(0, 0, 0, 0.7);
    line-height: 1.6;
    margin-bottom: 16px;
    min-height: 60px;
  }

  &-footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;
  }
}

.tour-step-indicator {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  background: rgba(0, 0, 0, 0.04);
  padding: 2px 8px;
  border-radius: 10px;
}

.tour-close {
  color: rgba(0, 0, 0, 0.45);
  cursor: pointer;
  padding: 2px;
  &:hover {
    color: rgba(0, 0, 0, 0.85);
  }
}

.tour-footer-spacer {
  flex: 1;
}

.tour-skip-bar {
  position: fixed;
  bottom: 16px;
  right: 16px;
  pointer-events: auto;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 16px;
  padding: 4px 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}
</style>

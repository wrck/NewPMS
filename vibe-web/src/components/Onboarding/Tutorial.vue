<script setup lang="ts">
/**
 * 新手教程组件（Task D4.1）
 *
 * 自研实现：基于 fixed 定位的高亮遮罩 + 步骤气泡，不依赖 driver.js。
 * 5 步引导：
 *   1. 工作台概览
 *   2. 项目管理
 *   3. 设备资产
 *   4. 我的任务
 *   5. 个人中心
 *
 * 触发：
 *   - 首次登录（userStore.firstLogin）自动触发
 *   - 完成后写 localStorage `onboarding_done=1`
 *   - 用户可从右上角 ? 重新打开（removeLocalStorage + 触发）
 */
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Button } from 'ant-design-vue'
import {
  CloseOutlined,
  LeftOutlined,
  RightOutlined,
  CheckOutlined
} from '@ant-design/icons-vue'

interface TutorialStep {
  selector: string
  title: string
  content: string
  /** 高亮区域 padding（像素） */
  padding?: number
}

const STORAGE_KEY = 'onboarding_done'

const steps: TutorialStep[] = [
  {
    selector: '.ant-menu-item-selected, [data-tutorial="dashboard"], .ant-layout-sider',
    title: '① 工作台概览',
    content: '欢迎来到 Vibe 交付管理平台！这里展示项目进度、待办任务、未读消息等关键信息，是您每天工作的起点。',
    padding: 8
  },
  {
    selector: '[data-tutorial="project"], .ant-menu-item:nth-of-type(2), .ant-layout-sider .ant-menu li:nth-child(2)',
    title: '② 项目管理',
    content: '在「项目管理」中创建项目、规划阶段任务、跟踪进度，并与团队成员协作。',
    padding: 6
  },
  {
    selector: '[data-tutorial="device"], .ant-layout-sider .ant-menu li:nth-child(3)',
    title: '③ 设备资产',
    content: '在「设备资产」中维护设备型号库、设备台账、出入库记录与备件库存。',
    padding: 6
  },
  {
    selector: '[data-tutorial="my-tasks"], .ant-layout-sider .ant-menu li:nth-child(1) ul li:nth-child(2)',
    title: '④ 我的任务',
    content: '点击「工作台 → 我的任务」查看分配给您的工作项，包含待办、进行中、已完成任务。',
    padding: 6
  },
  {
    selector: '.app-header .user-menu, .ant-layout-header .ant-avatar',
    title: '⑤ 个人中心',
    content: '点击右上角头像下拉可进入个人中心、修改密码、退出登录。点击 ? 图标可随时重新打开本教程或进入文档中心。',
    padding: 10
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

const popoverStyle = computed(() => {
  if (!targetRect.value) return {}
  const rect = targetRect.value
  // 默认放在目标元素下方右侧，若空间不够则左对齐
  const popoverWidth = 360
  const popoverHeight = 200
  const gap = 16

  let left = rect.right - popoverWidth
  if (left < 16) left = 16
  if (left + popoverWidth > window.innerWidth - 16) {
    left = window.innerWidth - popoverWidth - 16
  }

  let top = rect.bottom + gap
  if (top + popoverHeight > window.innerHeight - 16) {
    // 改为放在上方
    top = rect.top - popoverHeight - gap
    if (top < 16) top = 16
  }
  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${popoverWidth}px`
  }
})

const maskStyle = computed(() => {
  if (!targetRect.value) {
    // 没有目标元素时使用全屏遮罩
    return {
      clipPath: 'none'
    }
  }
  const rect = targetRect.value
  const padding = steps[currentStep.value].padding ?? 8
  const x = Math.max(0, rect.left - padding)
  const y = Math.max(0, rect.top - padding)
  const w = rect.width + padding * 2
  const h = rect.height + padding * 2
  // 使用 clip-path 在遮罩层中挖出一个透明矩形
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
    // 滚动完成后取位置
    setTimeout(() => {
      targetRect.value = el.getBoundingClientRect()
    }, 200)
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
  localStorage.setItem(STORAGE_KEY, '1')
  emit('finished')
}

function skip() {
  visible.value = false
  localStorage.setItem(STORAGE_KEY, '1')
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

// 监听步骤切换时刷新位置
watch(currentStep, () => {
  nextTick(updateTargetRect)
})
</script>

<template>
  <teleport to="body">
    <div v-if="visible" class="tutorial-overlay">
      <!-- 遮罩层：用 clip-path 挖出目标元素 -->
      <div class="tutorial-mask" :style="maskStyle" @click="skip" />

      <!-- 步骤气泡 -->
      <div v-if="targetRect" class="tutorial-popover" :style="popoverStyle">
        <div class="tutorial-popover-header">
          <span class="tutorial-step-indicator">{{ currentStep + 1 }} / {{ totalSteps }}</span>
          <a class="tutorial-close" @click="skip"><CloseOutlined /></a>
        </div>
        <div class="tutorial-popover-title">{{ steps[currentStep].title }}</div>
        <div class="tutorial-popover-content">{{ steps[currentStep].content }}</div>
        <div class="tutorial-popover-footer">
          <Button size="small" :disabled="isFirst" @click="prev">
            <template #icon><LeftOutlined /></template>
            上一步
          </Button>
          <span class="tutorial-footer-spacer" />
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
      <div v-else class="tutorial-popover tutorial-popover-center">
        <div class="tutorial-popover-header">
          <span class="tutorial-step-indicator">{{ currentStep + 1 }} / {{ totalSteps }}</span>
          <a class="tutorial-close" @click="skip"><CloseOutlined /></a>
        </div>
        <div class="tutorial-popover-title">{{ steps[currentStep].title }}</div>
        <div class="tutorial-popover-content">{{ steps[currentStep].content }}</div>
        <div class="tutorial-popover-footer">
          <Button size="small" :disabled="isFirst" @click="prev">
            <template #icon><LeftOutlined /></template>
            上一步
          </Button>
          <span class="tutorial-footer-spacer" />
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
      <div class="tutorial-skip-bar">
        <Button size="small" type="text" @click="skip">跳过教程</Button>
      </div>
    </div>
  </teleport>
</template>

<style lang="less" scoped>
.tutorial-overlay {
  position: fixed;
  inset: 0;
  z-index: 1100;
  pointer-events: none;
}

.tutorial-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  pointer-events: auto;
  transition: clip-path 0.25s ease;
}

.tutorial-popover {
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

.tutorial-step-indicator {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  background: rgba(0, 0, 0, 0.04);
  padding: 2px 8px;
  border-radius: 10px;
}

.tutorial-close {
  color: rgba(0, 0, 0, 0.45);
  cursor: pointer;
  padding: 2px;
  &:hover {
    color: rgba(0, 0, 0, 0.85);
  }
}

.tutorial-footer-spacer {
  flex: 1;
}

.tutorial-skip-bar {
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

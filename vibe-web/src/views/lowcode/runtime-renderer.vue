<script setup lang="ts">
/**
 * 运行时渲染入口页（spec 阶段三 - Task A4.6）
 *
 * 路由：/lowcode/runtime/:bizType/:bizId
 *
 * 功能：
 *   1. 根据 :bizType 查询对应启用中的低代码配置（form/list/tab/relation 任一）
 *   2. 解析 schemaJson 并传给 RuntimeRenderer 渲染
 *   3. bizId 为 0 表示新增态；非 0 表示详情/编辑态（加载初始数据）
 *
 * 后端约定：以 configCode = bizType 作为查找键；若需区分多套配置可改用 bizType + 模板类型。
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer.vue'
import EmptyState from '@/components/EmptyState.vue'
import RuntimeRenderer from '@/components/Lowcode/RuntimeRenderer.vue'
import {
  pageFormConfigs,
  pageListConfigs,
  pageTabConfigs,
  pageRelationConfigs
} from '@/api/lowcode'
import type {
  LowcodeFormConfigVO,
  LowcodeListConfigVO,
  LowcodeTabConfigVO,
  LowcodeRelationConfigVO,
  FormSchema,
  ListSchema,
  TabSchema,
  RelationSchema
} from '@/types/lowcode'
import type { PageResult } from '@/types/api'

const route = useRoute()

/** 路由参数 */
const bizType = computed(() => String(route.params.bizType || ''))
const bizId = computed(() => {
  const raw = route.params.bizId
  if (raw == null || raw === '' || raw === '0') return 0
  return Number(raw)
})
const isCreate = computed(() => bizId.value === 0)

/** 已加载的 schema（任一类型） */
const schema = ref<FormSchema | ListSchema | TabSchema | RelationSchema | null>(null)
const loading = ref(false)
const notFound = ref(false)
const configType = ref<string>('')

/** 依次尝试从 form/list/tab/relation 配置中查找 configCode = bizType 的启用配置 */
async function loadSchemaByBizType() {
  if (!bizType.value) {
    notFound.value = true
    return
  }
  loading.value = true
  try {
    // 1. 查询表单配置（用 keyword 模糊查询后客户端过滤精确匹配 configCode）
    const formRes = (await pageFormConfigs({
      keyword: bizType.value,
      status: 1,
      page: 1,
      size: 50
    })) as unknown as PageResult<LowcodeFormConfigVO>
    const formCfg = formRes?.records?.find((r) => r.configCode === bizType.value)
    if (formCfg) {
      try {
        schema.value = JSON.parse(formCfg.schemaJson) as FormSchema
        configType.value = 'form'
        return
      } catch (e) {
        console.error('[runtime] parse form schema failed:', e)
      }
    }

    // 2. 查询列表配置
    const listRes = (await pageListConfigs({
      keyword: bizType.value,
      status: 1,
      page: 1,
      size: 50
    })) as unknown as PageResult<LowcodeListConfigVO>
    const listCfg = listRes?.records?.find((r) => r.configCode === bizType.value)
    if (listCfg) {
      try {
        schema.value = JSON.parse(listCfg.schemaJson) as ListSchema
        configType.value = 'list'
        return
      } catch (e) {
        console.error('[runtime] parse list schema failed:', e)
      }
    }

    // 3. 查询标签页配置
    const tabRes = (await pageTabConfigs({
      keyword: bizType.value,
      status: 1,
      page: 1,
      size: 50
    })) as unknown as PageResult<LowcodeTabConfigVO>
    const tabCfg = tabRes?.records?.find((r) => r.configCode === bizType.value)
    if (tabCfg) {
      try {
        schema.value = JSON.parse(tabCfg.schemaJson) as TabSchema
        configType.value = 'tab'
        return
      } catch (e) {
        console.error('[runtime] parse tab schema failed:', e)
      }
    }

    // 4. 查询关联页配置
    const relRes = (await pageRelationConfigs({
      keyword: bizType.value,
      status: 1,
      page: 1,
      size: 50
    })) as unknown as PageResult<LowcodeRelationConfigVO>
    const relCfg = relRes?.records?.find((r) => r.configCode === bizType.value)
    if (relCfg) {
      try {
        schema.value = JSON.parse(relCfg.schemaJson) as RelationSchema
        configType.value = 'relation'
        return
      } catch (e) {
        console.error('[runtime] parse relation schema failed:', e)
      }
    }

    notFound.value = true
  } catch (e) {
    console.error('[runtime] load schema failed:', e)
    notFound.value = true
  } finally {
    loading.value = false
  }
}

/** RuntimeRenderer 提交事件回调 */
function handleSubmit(data: Record<string, unknown>) {
  console.log('[runtime] form submit:', data)
  message.success('表单已提交')
}

function handleAction(payload: { type: string; record?: Record<string, unknown> }) {
  console.log('[runtime] action triggered:', payload)
  message.info(`操作 ${payload.type} 已触发`)
}

watch(
  () => route.params.bizType,
  () => {
    loadSchemaByBizType()
  }
)

onMounted(() => {
  loadSchemaByBizType()
})
</script>

<template>
  <PageContainer
    :title="isCreate ? `新增 - ${bizType}` : `详情 - ${bizType}`"
    description="低代码运行时渲染页面"
  >
    <div v-if="loading" class="loading-wrapper">
      <a-spin tip="加载配置中..." />
    </div>

    <EmptyState
      v-else-if="notFound"
      description="未找到匹配的低代码配置，请先在「低代码配置」中创建 configCode 与当前业务类型一致的启用配置"
    />

    <div v-else-if="schema" class="runtime-wrapper">
      <RuntimeRenderer
        :schema="schema"
        :biz-type="bizType"
        :biz-id="bizId"
        :readonly="false"
        @submit="handleSubmit"
        @action="handleAction"
      />
    </div>
  </PageContainer>
</template>

<style lang="less" scoped>
.loading-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 80px 0;
}
.runtime-wrapper {
  background: @bg-container;
  border-radius: 6px;
  padding: 16px;
}
</style>

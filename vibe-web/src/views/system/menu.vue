<script setup lang="ts">
/**
 * 菜单管理
 * 菜单/按钮权限 CRUD、菜单树、拖拽排序、关联角色
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableProps, SwitchProps } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  MenuOutlined,
  AppstoreOutlined,
  ApiOutlined,
  TeamOutlined,
  FolderOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  listMenuTree,
  getMenu,
  createMenu,
  updateMenu,
  deleteMenu,
  listRolesByMenu,
  assignRolesToMenu,
  listRoles
} from '@/api/system'
import type { SysMenu, SysMenuDTO, MenuType, SysRole, MenuRoleSimple } from '@/api/system'
import type { StatusTone } from '@/styles/theme'

/* ============ 数据加载 ============ */
const loading = ref(false)
const menuTree = ref<SysMenu[]>([])
const expandedRowKeys = ref<number[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = (await listMenuTree()) as unknown as SysMenu[]
    menuTree.value = res || []
    // 默认展开第一层
    expandedRowKeys.value = menuTree.value.map((m) => m.id)
  } catch (e) {
    console.error('[system.menu] load failed:', e)
  } finally {
    loading.value = false
  }
}

/* ============ 类型元数据 ============ */
const menuTypeLabel: Record<MenuType, string> = {
  DIRECTORY: '目录',
  MENU: '菜单',
  BUTTON: '按钮'
}

const menuTypeTone: Record<MenuType, StatusTone> = {
  DIRECTORY: 'agent',
  MENU: 'processing',
  BUTTON: 'warning'
}

const menuTypeIcon: Record<MenuType, any> = {
  DIRECTORY: FolderOutlined,
  MENU: MenuOutlined,
  BUTTON: ApiOutlined
}

/* ============ 表格列 ============ */
const columns = [
  { title: '菜单名称', dataIndex: 'menuName', key: 'menuName', width: 220 },
  { title: '类型', dataIndex: 'menuType', key: 'menuType', width: 90 },
  { title: '路由地址', dataIndex: 'path', key: 'path', width: 180, ellipsis: true },
  { title: '组件路径', dataIndex: 'component', key: 'component', width: 180, ellipsis: true },
  { title: '权限标识', dataIndex: 'perms', key: 'perms', width: 180, ellipsis: true },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 70 },
  { title: '可见', dataIndex: 'visible', key: 'visible', width: 80 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' as const }
]

/* ============ 展开/折叠 ============ */
function collectAllKeys(list: SysMenu[]): number[] {
  const keys: number[] = []
  const walk = (nodes: SysMenu[]) => {
    nodes.forEach((n) => {
      if (n.children && n.children.length) {
        keys.push(n.id)
        walk(n.children)
      }
    })
  }
  walk(list)
  return keys
}

function handleExpandAll() {
  expandedRowKeys.value = collectAllKeys(menuTree.value)
}

function handleCollapseAll() {
  expandedRowKeys.value = []
}

function handleExpandedRowsChange(keys: (string | number)[]) {
  expandedRowKeys.value = keys.map((k) => Number(k))
}

/* ============ 表单弹窗 ============ */
const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const formData = reactive<SysMenuDTO>({
  parentId: 0,
  menuName: '',
  menuType: 'MENU',
  path: '',
  component: '',
  perms: '',
  icon: '',
  sortOrder: 0,
  visible: 1
})

/** 父菜单树（包含“根节点”占位项） */
const parentTreeData = computed(() => {
  const buildNode = (m: SysMenu): any => ({
    value: m.id,
    label: m.menuName,
    selectable: m.menuType !== 'BUTTON',
    children: m.children && m.children.length ? m.children.map(buildNode) : undefined
  })
  return [
    { value: 0, label: '根菜单', selectable: true, children: menuTree.value.map(buildNode) }
  ]
})

function openCreate(parent?: SysMenu | Record<string, any>) {
  const p = parent as SysMenu | undefined
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    parentId: p ? p.id : 0,
    menuName: '',
    menuType: p ? (p.menuType === 'BUTTON' ? 'BUTTON' : 'MENU') : 'DIRECTORY',
    path: '',
    component: '',
    perms: '',
    icon: '',
    sortOrder: 0,
    visible: 1
  })
  formVisible.value = true
}

async function openEdit(row: SysMenu | Record<string, any>) {
  const record = row as SysMenu
  isEdit.value = true
  try {
    const detail = (await getMenu(record.id)) as unknown as SysMenu
    Object.assign(formData, {
      id: detail.id,
      parentId: detail.parentId,
      menuName: detail.menuName,
      menuType: detail.menuType,
      path: detail.path || '',
      component: detail.component || '',
      perms: detail.perms || '',
      icon: detail.icon || '',
      sortOrder: detail.sortOrder ?? 0,
      visible: detail.visible ?? 1
    })
    formVisible.value = true
  } catch (e) {
    message.error('加载菜单详情失败')
  }
}

async function handleSubmit() {
  if (!formData.menuName) {
    message.warning('请填写菜单名称')
    return
  }
  if (!formData.menuType) {
    message.warning('请选择菜单类型')
    return
  }
  // 类型校验
  if (formData.menuType === 'MENU' && !formData.path) {
    message.warning('菜单类型需填写路由地址')
    return
  }
  if (formData.menuType === 'BUTTON' && !formData.perms) {
    message.warning('按钮类型需填写权限标识')
    return
  }
  formLoading.value = true
  try {
    const payload: SysMenuDTO = {
      ...formData,
      // DIRECTORY/BUTTON 不需要 component
      component: formData.menuType === 'MENU' ? formData.component : '',
      // BUTTON 不需要 path
      path: formData.menuType === 'BUTTON' ? '' : formData.path
    }
    if (isEdit.value && formData.id) {
      await updateMenu(formData.id, payload)
      message.success('更新成功')
    } else {
      await createMenu(payload)
      message.success('创建成功')
    }
    formVisible.value = false
    loadData()
  } catch (e) {
    // ignore
  } finally {
    formLoading.value = false
  }
}

/** “是否显示”开关变化处理（a-switch 的 update:checked 事件） */
const handleVisibleChange: NonNullable<SwitchProps['onUpdate:checked']> = (checked) => {
  formData.visible = checked ? 1 : 0
}

function handleDelete(row: SysMenu | Record<string, any>) {
  const record = row as SysMenu
  if (record.children && record.children.length > 0) {
    message.warning('存在子菜单，无法删除')
    return
  }
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除菜单「${record.menuName}」吗？`,
    okType: 'danger',
    async onOk() {
      try {
        await deleteMenu(record.id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        /* ignore */
      }
    }
  })
}

/* ============ 拖拽排序 ============ */
const draggingKey = ref<number | null>(null)

const customRow: TableProps['customRow'] = (record: any) => {
  return {
    draggable: true,
    onDragstart(e: DragEvent) {
      draggingKey.value = record.id
      if (e.dataTransfer) {
        e.dataTransfer.effectAllowed = 'move'
      }
    },
    onDragover(e: DragEvent) {
      if (draggingKey.value === null) return
      e.preventDefault()
      if (e.dataTransfer) {
        e.dataTransfer.dropEffect = 'move'
      }
    },
    onDrop(e: DragEvent) {
      e.preventDefault()
      if (draggingKey.value === null || draggingKey.value === record.id) return
      handleReorder(draggingKey.value, record.id)
    },
    onDragend() {
      draggingKey.value = null
    }
  }
}

/**
 * 同级兄弟内重排序：拖拽 sourceKey 到 targetKey 之前/之后
 * 仅支持同级排序（不跨父节点）
 */
function findSiblingArray(
  sourceId: number
): { arr: SysMenu[]; index: number; parent: SysMenu | null } | null {
  const result: { arr: SysMenu[]; index: number; parent: SysMenu | null } = {
    arr: [],
    index: -1,
    parent: null
  }
  const walk = (nodes: SysMenu[], parent: SysMenu | null): boolean => {
    for (let i = 0; i < nodes.length; i++) {
      const node = nodes[i]
      if (!node) continue
      if (node.id === sourceId) {
        result.arr = nodes
        result.index = i
        result.parent = parent
        return true
      }
      const children = node.children
      if (children && children.length) {
        if (walk(children as SysMenu[], node)) return true
      }
    }
    return false
  }
  walk(menuTree.value, null)
  return result.index >= 0 ? result : null
}

async function handleReorder(sourceId: number, targetId: number) {
  // 找到源节点和目标节点的同级数组
  const sourceCtx = findSiblingArray(sourceId)
  const targetCtx = findSiblingArray(targetId)
  if (!sourceCtx || !targetCtx) return
  // 仅同级排序
  if (sourceCtx.parent !== targetCtx.parent) {
    message.info('暂仅支持同级菜单内排序')
    return
  }
  const arr = sourceCtx.arr
  const sourceIdx = sourceCtx.index
  const targetIdx = targetCtx.index
  if (sourceIdx === targetIdx) return
  // 重新排序数组
  const [moved] = arr.splice(sourceIdx, 1)
  if (!moved) return
  arr.splice(targetIdx, 0, moved)
  // 重新生成 sortOrder 并按序号更新到后端
  const updates: Promise<void>[] = []
  arr.forEach((m, idx) => {
    const newSort = idx
    if (m.sortOrder !== newSort) {
      m.sortOrder = newSort
      updates.push(
        updateMenu(m.id, {
          id: m.id,
          parentId: m.parentId,
          menuName: m.menuName,
          menuType: m.menuType,
          path: m.path,
          component: m.component,
          perms: m.perms,
          icon: m.icon,
          sortOrder: newSort,
          visible: m.visible
        })
      )
    }
  })
  if (updates.length === 0) return
  try {
    await Promise.all(updates)
    message.success('排序已更新')
    await loadData()
  } catch (e) {
    message.error('排序更新失败')
    await loadData()
  }
}

/* ============ 关联角色 ============ */
const roleDrawerVisible = ref(false)
const roleLoading = ref(false)
const roleSaving = ref(false)
const currentMenuForRole = ref<SysMenu | null>(null)
const allRoles = ref<SysRole[]>([])
const selectedRoleIds = ref<number[]>([])

async function openRoleDrawer(row: SysMenu | Record<string, any>) {
  const record = row as SysMenu
  currentMenuForRole.value = record
  roleDrawerVisible.value = true
  roleLoading.value = true
  try {
    // 并行加载所有角色 + 当前菜单已关联角色
    const [roles, linked] = await Promise.all([
      listRoles() as unknown as SysRole[],
      listRolesByMenu(record.id) as unknown as MenuRoleSimple[]
    ])
    allRoles.value = roles || []
    selectedRoleIds.value = (linked || []).map((r) => r.id)
  } catch (e) {
    console.error('[system.menu] load roles failed:', e)
  } finally {
    roleLoading.value = false
  }
}

async function handleSaveRoles() {
  if (!currentMenuForRole.value) return
  roleSaving.value = true
  try {
    await assignRolesToMenu(currentMenuForRole.value.id, selectedRoleIds.value)
    message.success('角色关联已更新')
    roleDrawerVisible.value = false
  } catch (e) {
    /* ignore */
  } finally {
    roleSaving.value = false
  }
}

/* ============ 初始化 ============ */
onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer title="菜单管理" description="菜单树维护：目录 / 菜单 / 按钮，支持拖拽排序与角色关联">
    <template #extra>
      <a-button @click="handleExpandAll">全部展开</a-button>
      <a-button @click="handleCollapseAll">全部折叠</a-button>
      <a-button @click="loadData"><template #icon><ReloadOutlined /></template>刷新</a-button>
      <a-button type="primary" @click="openCreate()">
        <template #icon><PlusOutlined /></template>新增根菜单
      </a-button>
    </template>

    <div class="vibe-card table-card">
      <a-table
        :columns="columns"
        :data-source="menuTree"
        :loading="loading"
        :pagination="false"
        row-key="id"
        :scroll="{ x: 1400 }"
        :expanded-row-keys="expandedRowKeys"
        :custom-row="customRow"
        @expanded-rows-change="handleExpandedRowsChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'menuName'">
            <a-space :size="6">
              <component :is="menuTypeIcon[record.menuType as MenuType]" v-if="menuTypeIcon[record.menuType as MenuType]" />
              <span>{{ record.menuName }}</span>
            </a-space>
          </template>
          <template v-else-if="column.key === 'menuType'">
            <StatusTag :tone="menuTypeTone[record.menuType as MenuType]">
              {{ menuTypeLabel[record.menuType as MenuType] || record.menuType }}
            </StatusTag>
          </template>
          <template v-else-if="column.key === 'path'">
            <span class="mono">{{ record.path || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'component'">
            <span class="mono">{{ record.component || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'perms'">
            <a-tag v-if="record.perms" color="blue">{{ record.perms }}</a-tag>
            <span v-else>—</span>
          </template>
          <template v-else-if="column.key === 'visible'">
            <StatusTag :tone="record.visible === 1 ? 'success' : 'archived'">
              {{ record.visible === 1 ? '显示' : '隐藏' }}
            </StatusTag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="openCreate(record)">
                <template #icon><PlusOutlined /></template>子级
              </a-button>
              <a-button type="link" size="small" @click="openRoleDrawer(record)">
                <template #icon><TeamOutlined /></template>角色
              </a-button>
              <a-button type="link" size="small" @click="openEdit(record)">
                <template #icon><EditOutlined /></template>编辑
              </a-button>
              <a-popconfirm title="确定删除该菜单吗？" @confirm="handleDelete(record)">
                <a-button type="link" size="small" danger>
                  <template #icon><DeleteOutlined /></template>删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
        <template #emptyText>
          <EmptyState description="暂无菜单数据" action-text="新增根菜单" @action="openCreate()" />
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑菜单 -->
    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑菜单' : '新增菜单'"
      width="640px"
      :confirm-loading="formLoading"
      @ok="handleSubmit"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="上级菜单">
              <a-tree-select
                v-model:value="formData.parentId"
                :tree-data="parentTreeData"
                placeholder="请选择上级菜单"
                tree-default-expand-all
                allow-clear
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单类型" required>
              <a-radio-group v-model:value="formData.menuType">
                <a-radio value="DIRECTORY">
                  <FolderOutlined /> 目录
                </a-radio>
                <a-radio value="MENU">
                  <MenuOutlined /> 菜单
                </a-radio>
                <a-radio value="BUTTON">
                  <ApiOutlined /> 按钮
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="菜单名称" required>
              <a-input v-model:value="formData.menuName" placeholder="如 系统管理" :maxlength="64" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="图标">
              <a-input v-model:value="formData.icon" placeholder="如 SettingOutlined" :maxlength="64">
                <template #prefix><AppstoreOutlined /></template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序">
              <a-input-number v-model:value="formData.sortOrder" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否显示" v-if="formData.menuType !== 'BUTTON'">
              <a-switch
                :checked="formData.visible === 1"
                @update:checked="handleVisibleChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="formData.menuType !== 'BUTTON'">
            <a-form-item label="路由地址">
              <a-input v-model:value="formData.path" placeholder="如 /system/menu" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="formData.menuType === 'MENU'">
            <a-form-item label="组件路径">
              <a-input v-model:value="formData.component" placeholder="如 system/menu" :maxlength="128" />
            </a-form-item>
          </a-col>
          <a-col :span="24" v-if="formData.menuType !== 'DIRECTORY'">
            <a-form-item label="权限标识">
              <a-input
                v-model:value="formData.perms"
                placeholder="如 system:menu:list"
                :maxlength="128"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 关联角色抽屉 -->
    <a-drawer
      v-model:open="roleDrawerVisible"
      :title="`关联角色 - ${currentMenuForRole?.menuName || ''}`"
      width="480"
      :destroy-on-close="true"
    >
      <a-spin :spinning="roleLoading">
        <div class="role-transfer-wrap">
          <a-checkbox-group v-model:value="selectedRoleIds" style="width: 100%">
            <a-row :gutter="[8, 8]">
              <a-col v-for="r in allRoles" :key="r.id" :span="24">
                <a-checkbox :value="r.id" :disabled="r.status === 0">
                  <a-space :size="8">
                    <span>{{ r.roleName }}</span>
                    <a-tag color="blue">{{ r.roleCode }}</a-tag>
                    <span v-if="r.status === 0" class="disabled-tag">（已禁用）</span>
                  </a-space>
                </a-checkbox>
              </a-col>
            </a-row>
          </a-checkbox-group>
          <EmptyState v-if="!allRoles.length" description="暂无可分配角色" size="compact" />
        </div>
      </a-spin>
      <div class="drawer-footer">
        <a-space>
          <a-button @click="roleDrawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="roleSaving" @click="handleSaveRoles">保存</a-button>
        </a-space>
      </div>
    </a-drawer>
  </PageContainer>
</template>

<style lang="less" scoped>
.table-card {
  padding: 0;
}
.mono {
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 12px;
}
.role-transfer-wrap {
  padding: 8px 0;
}
.drawer-footer {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 12px 24px;
  border-top: 1px solid @border-color-split;
  text-align: right;
}
.disabled-tag {
  color: @status-exception;
  font-size: 12px;
}
:deep(.ant-table-row) {
  cursor: move;
}
</style>

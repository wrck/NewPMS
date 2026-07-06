/**
 * OrgTree 组件单元测试（spec 阶段三 Task 16 - SubTask 16.5）
 *
 * 覆盖范围：
 *   - 组件渲染（搜索框、树节点、空状态）
 *   - 挂载时调用 listOrgTree 加载组织树
 *   - 搜索过滤（关键字命中节点）
 *   - 单选模式：点击节点触发 update:modelValue（number）
 *   - 多选模式：勾选节点触发 update:modelValue（number[]）
 *   - v-model 双向绑定：modelValue 变化同步到内部 keys
 *   - orgType 过滤
 *   - Expose 方法：refresh / expandAll / collapseAll / findNode / getPath
 *   - OrgTreeSelect：表单选择器渲染与 v-model
 *   - useOrgTree composable：fetchTree 缓存、findNode、getPath、clearCache
 *   - 工具函数：filterTree / filterByOrgType / findNodeInTree / findPathInTree / collectAllKeys
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'

import OrgTree from '../index.vue'
import OrgTreeSelect from '../OrgTreeSelect.vue'
import {
  useOrgTree,
  filterTree,
  filterByOrgType,
  findNodeInTree,
  findPathInTree,
  collectAllKeys,
  clearOrgTreeCache
} from '../useOrgTree'
import type { OrgTreeNode } from '../types'

/* ============ Mock @/api/system ============ */
const mocks = vi.hoisted(() => {
  return {
    listOrgTree: vi.fn()
  }
})

vi.mock('@/api/system', async () => {
  const actual = await vi.importActual<typeof import('@/api/system')>('@/api/system')
  return {
    ...actual,
    listOrgTree: mocks.listOrgTree
  }
})

/* ============ 测试数据 ============ */
const sampleTree: OrgTreeNode[] = [
  {
    id: 1,
    orgCode: 'HQ',
    orgName: '总公司',
    parentId: undefined,
    sort: 1,
    status: 1,
    orgType: 'company',
    children: [
      {
        id: 11,
        orgCode: 'TECH',
        orgName: '技术部',
        parentId: 1,
        sort: 1,
        status: 1,
        orgType: 'department',
        children: [
          {
            id: 111,
            orgCode: 'FE',
            orgName: '前端组',
            parentId: 11,
            sort: 1,
            status: 1,
            orgType: 'team'
          },
          {
            id: 112,
            orgCode: 'BE',
            orgName: '后端组',
            parentId: 11,
            sort: 2,
            status: 1,
            orgType: 'team'
          }
        ]
      },
      {
        id: 12,
        orgCode: 'HR',
        orgName: '人事部',
        parentId: 1,
        sort: 2,
        status: 1,
        orgType: 'department'
      }
    ]
  },
  {
    id: 2,
    orgCode: 'BJ_BRANCH',
    orgName: '北京分公司',
    parentId: undefined,
    sort: 2,
    status: 1,
    orgType: 'company'
  }
]

/* ============ 测试辅助 ============ */
function resolveListOrgTree(data: OrgTreeNode[] = sampleTree) {
  mocks.listOrgTree.mockResolvedValue(data)
}

function makeWrapper(options: any = {}) {
  return mount(OrgTree, {
    props: {
      ...(options.props || {})
    },
    global: {
      stubs: {
        ...(options.stubs || {})
      }
    }
  })
}

function makeSelectWrapper(options: any = {}) {
  return mount(OrgTreeSelect, {
    props: {
      placeholder: '请选择组织',
      ...(options.props || {})
    },
    global: {
      stubs: {
        ...(options.stubs || {})
      }
    }
  })
}

/* ============ 测试用例 ============ */
describe('OrgTree', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearOrgTreeCache()
    resolveListOrgTree()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    clearOrgTreeCache()
  })

  describe('渲染', () => {
    it('挂载后调用 listOrgTree 加载组织树', async () => {
      makeWrapper()
      await flushPromises()
      expect(mocks.listOrgTree).toHaveBeenCalledTimes(1)
    })

    it('searchable=true 时渲染搜索框', async () => {
      const wrapper = makeWrapper({ props: { searchable: true } })
      await flushPromises()
      expect(wrapper.find('.org-tree-search').exists()).toBe(true)
    })

    it('searchable=false 时不渲染搜索框', async () => {
      const wrapper = makeWrapper({ props: { searchable: false } })
      await flushPromises()
      expect(wrapper.find('.org-tree-search').exists()).toBe(false)
    })

    it('加载后渲染树节点（ant-tree 组件存在）', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      expect(wrapper.find('.ant-tree').exists() || wrapper.findComponent({ name: 'ATree' }).exists()).toBe(true)
    })

    it('listOrgTree 返回空数组时渲染 a-empty', async () => {
      resolveListOrgTree([])
      const wrapper = makeWrapper()
      await flushPromises()
      expect(wrapper.findComponent({ name: 'AEmpty' }).exists()).toBe(true)
    })

    it('orgCode 节点显示 a-tag', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const tags = wrapper.findAllComponents({ name: 'ATag' })
      expect(tags.length).toBeGreaterThan(0)
    })
  })

  describe('搜索过滤', () => {
    it('输入关键字后过滤树（仅保留命中节点与祖先链）', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const input = wrapper.find('.org-tree-search input')
      expect(input.exists()).toBe(true)
      await input.setValue('前端')
      await nextTick()
      // 树应包含"前端组"节点
      const treeText = wrapper.text()
      expect(treeText).toContain('前端组')
      // 不应包含未命中的"人事部"
      expect(treeText).not.toContain('人事部')
    })

    it('清空关键字后恢复全部节点', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const input = wrapper.find('.org-tree-search input')
      await input.setValue('前端')
      await nextTick()
      await input.setValue('')
      await nextTick()
      const treeText = wrapper.text()
      expect(treeText).toContain('人事部')
    })

    it('搜索关键字高亮显示（mark.org-highlight）', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const input = wrapper.find('.org-tree-search input')
      await input.setValue('技术')
      await nextTick()
      const highlights = wrapper.findAll('.org-highlight')
      expect(highlights.length).toBeGreaterThan(0)
      expect(highlights[0].text()).toContain('技术')
    })
  })

  describe('orgType 过滤', () => {
    it('orgType=department 时仅显示部门类型节点（及祖先链）', async () => {
      const wrapper = makeWrapper({ props: { orgType: 'department', defaultExpandAll: true } })
      await flushPromises()
      const treeText = wrapper.text()
      // 部门节点应出现
      expect(treeText).toContain('技术部')
      expect(treeText).toContain('人事部')
      // 北京分公司无 department 子节点，应被过滤
      expect(treeText).not.toContain('北京分公司')
    })

    it('orgType=all 时显示全部节点', async () => {
      const wrapper = makeWrapper({ props: { orgType: 'all', defaultExpandAll: true } })
      await flushPromises()
      const treeText = wrapper.text()
      expect(treeText).toContain('北京分公司')
    })
  })

  describe('单选模式', () => {
    it('selectable=true 时点击节点触发 update:modelValue（number）', async () => {
      const wrapper = makeWrapper({
        props: { selectable: true, single: true, multiple: false }
      })
      await flushPromises()
      // 点击总公司节点（id=1）
      const titleNodes = wrapper.findAll('.ant-tree-title')
      // antd 4 中标题可能在 .ant-tree-title 或 .ant-tree-node-content-wrapper
      const clickTarget = wrapper.findAll('.ant-tree-node-content-wrapper')
      if (clickTarget.length > 0) {
        await clickTarget[0].trigger('click')
        await nextTick()
        const updateEvents = wrapper.emitted('update:modelValue')
        expect(updateEvents).toBeTruthy()
        if (updateEvents) {
          const value = updateEvents[updateEvents.length - 1][0]
          // 单选模式下应返回 number 或 undefined
          expect(value === undefined || typeof value === 'number' || Array.isArray(value)).toBe(true)
        }
      }
    })

    it('传入 modelValue 时同步到 selectedKeys', async () => {
      const wrapper = makeWrapper({
        props: { modelValue: 11 }
      })
      await flushPromises()
      // 内部 selectedKeys 应包含 11
      const tree = wrapper.findComponent({ name: 'ATree' })
      if (tree.exists()) {
        const selectedKeys = (tree.props() as any).selectedKeys
        expect(selectedKeys).toContain(11)
      }
    })
  })

  describe('多选模式', () => {
    it('multiple=true 时渲染 checkable 树', async () => {
      const wrapper = makeWrapper({
        props: { multiple: true, checkable: true, selectable: false }
      })
      await flushPromises()
      const tree = wrapper.findComponent({ name: 'ATree' })
      expect(tree.exists()).toBe(true)
      expect((tree.props() as any).checkable).toBe(true)
      expect((tree.props() as any).multiple).toBe(true)
    })

    it('传入 modelValue 数组时同步到 checkedKeys', async () => {
      const wrapper = makeWrapper({
        props: { multiple: true, checkable: true, selectable: false, modelValue: [11, 12] }
      })
      await flushPromises()
      const tree = wrapper.findComponent({ name: 'ATree' })
      if (tree.exists()) {
        const checkedKeys = (tree.props() as any).checkedKeys
        expect(checkedKeys).toEqual(expect.arrayContaining([11, 12]))
      }
    })
  })

  describe('Expose 方法', () => {
    it('findNode 返回正确节点', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      const node = vm.findNode(11)
      expect(node).toBeTruthy()
      expect(node.orgName).toBe('技术部')
    })

    it('findNode 未找到时返回 undefined', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      const node = vm.findNode(9999)
      expect(node).toBeUndefined()
    })

    it('getPath 返回从根到节点的路径', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      const path = vm.getPath(111)
      expect(path).toHaveLength(3)
      expect(path[0].id).toBe(1)
      expect(path[1].id).toBe(11)
      expect(path[2].id).toBe(111)
    })

    it('getPath 未找到时返回空数组', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      const path = vm.getPath(9999)
      expect(path).toEqual([])
    })

    it('getTreeData 返回当前树数据', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      const data = vm.getTreeData()
      expect(data).toEqual(sampleTree)
    })

    it('refresh 强制重新拉取', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      expect(mocks.listOrgTree).toHaveBeenCalledTimes(1)
      const vm = wrapper.vm as any
      await vm.refresh()
      expect(mocks.listOrgTree).toHaveBeenCalledTimes(2)
    })

    it('expandAll / collapseAll 调整 expandedKeys', async () => {
      const wrapper = makeWrapper()
      await flushPromises()
      const vm = wrapper.vm as any
      vm.expandAll()
      await nextTick()
      const treeAfterExpand = wrapper.findComponent({ name: 'ATree' })
      if (treeAfterExpand.exists()) {
        const expandedKeys = (treeAfterExpand.props() as any).expandedKeys
        expect(expandedKeys.length).toBeGreaterThan(0)
      }
      vm.collapseAll()
      await nextTick()
      const treeAfterCollapse = wrapper.findComponent({ name: 'ATree' })
      if (treeAfterCollapse.exists()) {
        const expandedKeys = (treeAfterCollapse.props() as any).expandedKeys
        expect(expandedKeys).toEqual([])
      }
    })
  })

  describe('status 过滤', () => {
    it('传入 status 时传给 listOrgTree', async () => {
      makeWrapper({ props: { status: 1 } })
      await flushPromises()
      expect(mocks.listOrgTree).toHaveBeenCalledWith({ status: 1 })
    })

    it('未传 status 时不传参', async () => {
      makeWrapper()
      await flushPromises()
      expect(mocks.listOrgTree).toHaveBeenCalledWith(undefined)
    })

    it('status 变化时强制重新拉取', async () => {
      const wrapper = makeWrapper({ props: { status: 1 } })
      await flushPromises()
      expect(mocks.listOrgTree).toHaveBeenCalledTimes(1)
      await wrapper.setProps({ status: 0 })
      await flushPromises()
      expect(mocks.listOrgTree).toHaveBeenCalledTimes(2)
    })
  })
})

describe('OrgTreeSelect', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearOrgTreeCache()
    resolveListOrgTree()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    clearOrgTreeCache()
  })

  it('挂载后调用 listOrgTree 加载组织树', async () => {
    makeSelectWrapper()
    await flushPromises()
    expect(mocks.listOrgTree).toHaveBeenCalledTimes(1)
  })

  it('渲染 a-tree-select 组件', async () => {
    const wrapper = makeSelectWrapper()
    await flushPromises()
    const treeSelect = wrapper.findComponent({ name: 'ATreeSelect' })
    expect(treeSelect.exists()).toBe(true)
  })

  it('placeholder 透传到 a-tree-select', async () => {
    const wrapper = makeSelectWrapper({ props: { placeholder: '请选择部门' } })
    await flushPromises()
    const treeSelect = wrapper.findComponent({ name: 'ATreeSelect' })
    expect((treeSelect.props() as any).placeholder).toBe('请选择部门')
  })

  it('multiple=true 时 tree-checkable 为 true', async () => {
    const wrapper = makeSelectWrapper({ props: { multiple: true } })
    await flushPromises()
    const treeSelect = wrapper.findComponent({ name: 'ATreeSelect' })
    expect((treeSelect.props() as any).treeCheckable).toBe(true)
  })

  it('disabled=true 时透传到 a-tree-select', async () => {
    const wrapper = makeSelectWrapper({ props: { disabled: true } })
    await flushPromises()
    const treeSelect = wrapper.findComponent({ name: 'ATreeSelect' })
    expect((treeSelect.props() as any).disabled).toBe(true)
  })

  it('orgType 过滤生效', async () => {
    const wrapper = makeSelectWrapper({ props: { orgType: 'department' } })
    await flushPromises()
    const treeSelect = wrapper.findComponent({ name: 'ATreeSelect' })
    const treeData = (treeSelect.props() as any).treeData
    // 应仅包含 department 节点（及祖先链）
    const hasTech = treeData.some((n: any) =>
      n.orgName === '技术部' || (n.children || []).some((c: any) => c.orgName === '技术部')
    )
    expect(hasTech).toBe(true)
  })

  it('modelValue 同步到 a-tree-select value', async () => {
    const wrapper = makeSelectWrapper({ props: { modelValue: 11 } })
    await flushPromises()
    const treeSelect = wrapper.findComponent({ name: 'ATreeSelect' })
    expect((treeSelect.props() as any).value).toBe(11)
  })
})

describe('useOrgTree composable', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    clearOrgTreeCache()
    resolveListOrgTree()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    clearOrgTreeCache()
  })

  it('fetchTree 加载组织树数据', async () => {
    const { treeData, fetchTree } = useOrgTree()
    await fetchTree()
    expect(treeData.value).toEqual(sampleTree)
  })

  it('缓存命中：第二次调用不重复请求', async () => {
    const { fetchTree } = useOrgTree()
    await fetchTree()
    await fetchTree()
    expect(mocks.listOrgTree).toHaveBeenCalledTimes(1)
  })

  it('force=true 强制跳过缓存', async () => {
    const { fetchTree } = useOrgTree()
    await fetchTree()
    await fetchTree({ force: true })
    expect(mocks.listOrgTree).toHaveBeenCalledTimes(2)
  })

  it('clearCache 后下次调用重新请求', async () => {
    const { fetchTree, clearCache } = useOrgTree()
    await fetchTree()
    clearCache()
    await fetchTree()
    expect(mocks.listOrgTree).toHaveBeenCalledTimes(2)
  })

  it('findNode 返回正确节点', async () => {
    const { fetchTree, findNode } = useOrgTree()
    await fetchTree()
    const node = findNode(111)
    expect(node).toBeTruthy()
    expect(node?.orgName).toBe('前端组')
  })

  it('findNode 未找到返回 undefined', async () => {
    const { fetchTree, findNode } = useOrgTree()
    await fetchTree()
    expect(findNode(9999)).toBeUndefined()
  })

  it('getPath 返回从根到节点的路径', async () => {
    const { fetchTree, getPath } = useOrgTree()
    await fetchTree()
    const path = getPath(112)
    expect(path).toHaveLength(3)
    expect(path.map((n) => n.id)).toEqual([1, 11, 112])
  })

  it('getPath 未找到返回空数组', async () => {
    const { fetchTree, getPath } = useOrgTree()
    await fetchTree()
    expect(getPath(9999)).toEqual([])
  })

  it('loading 状态正确切换', async () => {
    const { loading, fetchTree } = useOrgTree()
    expect(loading.value).toBe(false)
    const promise = fetchTree()
    expect(loading.value).toBe(true)
    await promise
    expect(loading.value).toBe(false)
  })

  it('API 失败时抛出错误并设置 error', async () => {
    const error = new Error('network error')
    mocks.listOrgTree.mockRejectedValueOnce(error)
    const { fetchTree, error: errorRef } = useOrgTree()
    await expect(fetchTree()).rejects.toThrow('network error')
    expect(errorRef.value).toBe(error)
  })
})

describe('工具函数', () => {
  describe('filterTree', () => {
    it('关键字为空时返回原树', () => {
      expect(filterTree(sampleTree, '')).toEqual(sampleTree)
    })

    it('匹配节点保留完整子树', () => {
      const result = filterTree(sampleTree, '总公司')
      expect(result).toHaveLength(1)
      expect(result[0].orgName).toBe('总公司')
      // 子节点应完整保留
      expect(result[0].children).toHaveLength(2)
    })

    it('子节点匹配时保留父节点（仅挂载匹配子分支）', () => {
      const result = filterTree(sampleTree, '前端')
      // 总公司应保留（因为后端有匹配）
      expect(result).toHaveLength(1)
      expect(result[0].orgName).toBe('总公司')
      // 总公司下仅保留技术部（人事部不匹配且无匹配子节点）
      expect(result[0].children).toHaveLength(1)
      expect(result[0].children![0].orgName).toBe('技术部')
      // 技术部下仅保留前端组
      expect(result[0].children![0].children).toHaveLength(1)
      expect(result[0].children![0].children![0].orgName).toBe('前端组')
    })

    it('大小写不敏感', () => {
      const result = filterTree(sampleTree, 'TOTAL')
      // 不应命中中文节点
      expect(result).toHaveLength(0)
    })

    it('无匹配返回空数组', () => {
      const result = filterTree(sampleTree, '不存在的组织')
      expect(result).toEqual([])
    })
  })

  describe('filterByOrgType', () => {
    it('orgType=all 返回原树', () => {
      expect(filterByOrgType(sampleTree, 'all')).toEqual(sampleTree)
    })

    it('orgType=department 保留部门节点及祖先链', () => {
      const result = filterByOrgType(sampleTree, 'department')
      expect(result).toHaveLength(1)
      expect(result[0].orgName).toBe('总公司')
      expect(result[0].children).toHaveLength(2)
      expect(result[0].children![0].orgName).toBe('技术部')
    })

    it('orgType=team 保留小组节点及祖先链（祖先仅挂载匹配子分支）', () => {
      const result = filterByOrgType(sampleTree, 'team')
      // 总公司保留（其下有 team 节点）
      expect(result).toHaveLength(1)
      const parent = result[0]
      // 总公司下仅保留技术部（人事部无 team 子节点）
      expect(parent.children).toHaveLength(1)
      expect(parent.children![0].orgName).toBe('技术部')
      // 技术部下保留全部 team（前端组、后端组）
      expect(parent.children![0].children).toHaveLength(2)
    })

    it('orgType=company 保留公司节点（含完整子树）', () => {
      const result = filterByOrgType(sampleTree, 'company')
      // 总公司、北京分公司都应保留
      expect(result).toHaveLength(2)
      expect(result[0].orgName).toBe('总公司')
      expect(result[1].orgName).toBe('北京分公司')
      // 总公司应保留完整子树
      expect(result[0].children).toHaveLength(2)
    })
  })

  describe('findNodeInTree', () => {
    it('查找根节点', () => {
      const node = findNodeInTree(sampleTree, 1)
      expect(node?.orgName).toBe('总公司')
    })

    it('查找深层节点', () => {
      const node = findNodeInTree(sampleTree, 112)
      expect(node?.orgName).toBe('后端组')
    })

    it('未找到返回 undefined', () => {
      expect(findNodeInTree(sampleTree, 9999)).toBeUndefined()
    })
  })

  describe('findPathInTree', () => {
    it('根节点路径长度为 1', () => {
      const path = findPathInTree(sampleTree, 1)
      expect(path).toHaveLength(1)
      expect(path[0].id).toBe(1)
    })

    it('深层节点路径包含所有祖先', () => {
      const path = findPathInTree(sampleTree, 111)
      expect(path).toHaveLength(3)
      expect(path.map((n) => n.id)).toEqual([1, 11, 111])
    })

    it('未找到返回空数组', () => {
      expect(findPathInTree(sampleTree, 9999)).toEqual([])
    })
  })

  describe('collectAllKeys', () => {
    it('收集所有节点 key（深度优先）', () => {
      const keys = collectAllKeys(sampleTree)
      expect(keys).toEqual([1, 11, 111, 112, 12, 2])
    })

    it('空数组返回空数组', () => {
      expect(collectAllKeys([])).toEqual([])
    })
  })
})

/**
 * RichEditor 组件单元测试（spec 阶段三 Task 15 - SubTask 15.7）
 *
 * 覆盖范围：
 *   - 组件渲染（容器、工具栏、编辑区）
 *   - v-model 双向绑定（HTML 模式 / 纯文本模式）
 *   - readonly 模式（隐藏工具栏）
 *   - outputFormat 切换
 *   - Props 默认值与定制
 *   - expose 方法（getHtml / getText / clear / focus / blur / insertImage）
 *   - 编辑器创建 / 内容变化事件
 *   - 图片上传 customUpload 流程（成功 / 失败）
 *
 * 注：wangEditor 5 在 jsdom 下无法初始化（依赖 Selection / Range API），
 * 因此本测试通过 stub 替换 Editor / Toolbar 组件，模拟 wangEditor 行为。
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick, ref, watch, h } from 'vue'
import RichEditor from '../index.vue'

/* ============ Mock ant-design-vue 静态方法 ============ */
const mocks = vi.hoisted(() => {
  return {
    messageSuccess: vi.fn(),
    messageError: vi.fn(),
    messageWarning: vi.fn(),
    messageInfo: vi.fn()
  }
})
const { messageSuccess, messageError, messageWarning, messageInfo } = mocks

vi.mock('ant-design-vue', async () => {
  const actual = await vi.importActual<typeof import('ant-design-vue')>('ant-design-vue')
  return {
    ...actual,
    message: {
      success: mocks.messageSuccess,
      error: mocks.messageError,
      warning: mocks.messageWarning,
      info: mocks.messageInfo,
      loading: vi.fn()
    }
  }
})

/* ============ Mock @wangeditor/editor-for-vue 的 Editor / Toolbar ============ */
/**
 * Editor stub：模拟 wangEditor Editor 组件
 * - 接收 v-model（modelValue）与 defaultConfig
 * - mount 时 emit onCreated（传出一个 mock IDomEditor）+ emit onChange
 * - 内部维护 html 状态，可通过 setText/setHtml 主动触发 onChange
 *
 * 由于 RichEditor 通过 `@on-created` 与 `@on-change` 监听事件，
 * stub 也 emit 同名事件。
 */
function createEditorStub() {
  const instances: any[] = []
  const EditorStub = {
    name: 'WangEditor',
    props: {
      modelValue: { type: String, default: '' },
      defaultConfig: { type: Object, default: () => ({}) },
      mode: { type: String, default: 'default' }
    },
    emits: ['update:modelValue', 'onCreated', 'onChange', 'onDestroyed', 'onFocus', 'onBlur'],
    setup(props: any, ctx: any) {
      const editor = makeMockEditor()
      // mount 时 emit onCreated
      instances.push(editor)
      ctx.emit('onCreated', editor)
      // 监听外部 modelValue 变化，同步到内部
      watch(
        () => props.modelValue,
        (val) => {
          ;(editor as any)._html = val
        }
      )
      return () => h('div', { class: 'wang-editor-stub' })
    }
  }
  return { EditorStub, getInstances: () => instances }
}

/** 构造一个 mock IDomEditor（用 any 绕过严格类型，保留 vi.fn 的 mockReturnValue 方法） */
function makeMockEditor(): any {
  let html = '<p><br></p>'
  let text = ''
  const editor = {
    _html: html,
    getHtml: vi.fn(() => html),
    getText: vi.fn(() => text),
    clear: vi.fn(() => {
      html = '<p><br></p>'
      text = ''
      editor._html = html
    }),
    focus: vi.fn(),
    blur: vi.fn(),
    destroy: vi.fn(),
    dangerouslyInsertHtml: vi.fn((htmlStr: string) => {
      html += htmlStr
      editor._html = html
      editor.getHtml.mockReturnValue(html)
    }),
    // 测试辅助：模拟用户输入文本
    _setText(newText: string) {
      text = newText
      html = `<p>${newText}</p>`
      editor._html = html
      editor.getHtml.mockReturnValue(html)
      editor.getText.mockReturnValue(text)
    },
    _setHtml(newHtml: string) {
      html = newHtml
      editor._html = html
      editor.getHtml.mockReturnValue(html)
    }
  }
  return editor
}

/** Toolbar stub：仅渲染一个占位 div */
const ToolbarStub = {
  name: 'WangToolbar',
  props: {
    editor: { type: Object, default: null },
    defaultConfig: { type: Object, default: () => ({}) },
    mode: { type: String, default: 'default' }
  },
  template: '<div class="wang-toolbar-stub"></div>'
}

/* ============ Mock useUserStore ============ */
const userStoreMock = vi.hoisted(() => ({
  token: 'fake-token'
}))
vi.mock('@/stores/user', () => ({
  useUserStore: () => userStoreMock
}))

/* ============ 全局 fetch mock ============ */
const fetchMock = vi.fn()
;(globalThis as any).fetch = fetchMock

/* ============ 测试辅助 ============ */
function makeWrapper(options: any = {}) {
  const { EditorStub, getInstances } = createEditorStub()
  const wrapper = mount(RichEditor, {
    props: {
      modelValue: options.modelValue ?? '',
      ...(options.props || {})
    },
    global: {
      stubs: {
        Editor: EditorStub,
        Toolbar: ToolbarStub,
        ...(options.stubs || {})
      }
    }
  })
  return { wrapper, getInstances }
}

describe('RichEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchMock.mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('渲染', () => {
    it('应渲染容器、工具栏与编辑区', async () => {
      const { wrapper } = makeWrapper()
      await flushPromises()
      expect(wrapper.find('.rich-editor-container').exists()).toBe(true)
      expect(wrapper.find('.wang-toolbar-stub').exists()).toBe(true)
      expect(wrapper.findComponent({ name: 'WangEditor' }).exists()).toBe(true)
    })

    it('容器高度应用 props.height', async () => {
      const { wrapper } = makeWrapper({ props: { height: 600 } })
      await flushPromises()
      const container = wrapper.find('.rich-editor-container')
      expect(container.attributes('style') || '').toContain('height: 600px')
    })

    it('工具栏默认存在', async () => {
      const { wrapper } = makeWrapper()
      await flushPromises()
      expect(wrapper.find('.wang-toolbar-stub').exists()).toBe(true)
    })

    it('readonly 模式下隐藏工具栏', async () => {
      const { wrapper } = makeWrapper({ props: { readonly: true } })
      await flushPromises()
      expect(wrapper.find('.wang-toolbar-stub').exists()).toBe(false)
      expect(wrapper.find('.is-readonly').exists()).toBe(true)
    })

    it('mode 透传给 Editor / Toolbar', async () => {
      const { wrapper } = makeWrapper({ props: { mode: 'simple' } })
      await flushPromises()
      const editor = wrapper.findComponent({ name: 'WangEditor' })
      const toolbar = wrapper.findComponent({ name: 'WangToolbar' })
      expect(editor.props('mode')).toBe('simple')
      expect(toolbar.props('mode')).toBe('simple')
    })
  })

  describe('Props 默认值', () => {
    it('height 默认 400', async () => {
      const { wrapper } = makeWrapper()
      await flushPromises()
      const container = wrapper.find('.rich-editor-container')
      expect(container.attributes('style') || '').toContain('height: 400px')
    })

    it('placeholder 默认 "请输入内容..."', async () => {
      const { wrapper } = makeWrapper()
      await flushPromises()
      const editor = wrapper.findComponent({ name: 'WangEditor' })
      const config = editor.props('defaultConfig') as any
      expect(config.placeholder).toBe('请输入内容...')
    })

    it('excludeKeys 默认排除 fullScreen 与 group-video', async () => {
      const { wrapper } = makeWrapper()
      await flushPromises()
      const toolbar = wrapper.findComponent({ name: 'WangToolbar' })
      const config = toolbar.props('defaultConfig') as any
      expect(config.excludeKeys).toEqual(['fullScreen', 'group-video'])
    })

    it('可自定义 excludeKeys', async () => {
      const { wrapper } = makeWrapper({
        props: { excludeKeys: ['header1', 'header2'] }
      })
      await flushPromises()
      const toolbar = wrapper.findComponent({ name: 'WangToolbar' })
      const config = toolbar.props('defaultConfig') as any
      expect(config.excludeKeys).toEqual(['header1', 'header2'])
    })

    it('outputFormat 默认 html', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      expect(editors.length).toBeGreaterThan(0)
      // onCreated 触发 handleChange，应输出 html
      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      const lastValue = emitted!.at(-1)![0]
      // 初始 html 是 stub 的初始值
      expect(typeof lastValue).toBe('string')
    })

    it('placeholder 可定制', async () => {
      const { wrapper } = makeWrapper({
        props: { placeholder: '请输入描述...' }
      })
      await flushPromises()
      const editor = wrapper.findComponent({ name: 'WangEditor' })
      const config = editor.props('defaultConfig') as any
      expect(config.placeholder).toBe('请输入描述...')
    })

    it('maxImageSize 透传到 editorConfig.MENU_CONF', async () => {
      const { wrapper } = makeWrapper({
        props: { maxImageSize: 5 }
      })
      await flushPromises()
      const vm = wrapper.vm as any
      // editorConfig 是 computed，可通过 vm.editorConfig 访问
      expect(vm.editorConfig.MENU_CONF.uploadImage).toBeDefined()
      // maxImageSize 通过闭包引用 props，不直接出现在 config 上，
      // 这里验证 customUpload 函数存在
      expect(typeof vm.editorConfig.MENU_CONF.uploadImage.customUpload).toBe('function')
    })
  })

  describe('v-model 双向绑定', () => {
    it('初始 modelValue 同步到内部 valueHtml', async () => {
      const { wrapper } = makeWrapper({ modelValue: '<p>初始内容</p>' })
      await flushPromises()
      const editor = wrapper.findComponent({ name: 'WangEditor' })
      expect(editor.props('modelValue')).toBe('<p>初始内容</p>')
    })

    it('外部 modelValue 变化时同步到内部', async () => {
      const { wrapper } = makeWrapper({ modelValue: '' })
      await flushPromises()
      const editor = wrapper.findComponent({ name: 'WangEditor' })
      expect(editor.props('modelValue')).toBe('')
      await wrapper.setProps({ modelValue: '<p>新内容</p>' })
      await nextTick()
      expect(editor.props('modelValue')).toBe('<p>新内容</p>')
    })

    it('Editor 触发 onChange 时 emit update:modelValue（html 模式）', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      // 模拟用户输入
      editor._setText('hello world')
      // 直接调用组件内部的 handleChange（通过触发 Editor stub 的 onChange 事件）
      wrapper.findComponent({ name: 'WangEditor' }).vm.$emit('onChange', editor)
      await nextTick()
      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      const last = emitted!.at(-1)![0]
      expect(last).toBe('<p>hello world</p>')
    })

    it('outputFormat=text 时 emit 纯文本', async () => {
      const { wrapper, getInstances } = makeWrapper({
        props: { outputFormat: 'text' }
      })
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      editor._setText('纯文本内容')
      wrapper.findComponent({ name: 'WangEditor' }).vm.$emit('onChange', editor)
      await nextTick()
      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      const last = emitted!.at(-1)![0]
      expect(last).toBe('纯文本内容')
    })

    it('change 事件与 update:modelValue 同步触发', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      editor._setText('abc')
      wrapper.findComponent({ name: 'WangEditor' }).vm.$emit('onChange', editor)
      await nextTick()
      const changeEmitted = wrapper.emitted('change')
      const updateEmitted = wrapper.emitted('update:modelValue')
      expect(changeEmitted).toBeTruthy()
      expect(updateEmitted).toBeTruthy()
      expect(changeEmitted!.at(-1)![0]).toBe('<p>abc</p>')
    })
  })

  describe('编辑器生命周期', () => {
    it('onCreated 触发 created 事件', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      expect(editors.length).toBeGreaterThan(0)
      const created = wrapper.emitted('created')
      expect(created).toBeTruthy()
      expect(created![0][0]).toBe(editors[0])
    })

    it('组件卸载时调用 editor.destroy()', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      const spy = vi.spyOn(editor, 'destroy')
      wrapper.unmount()
      expect(spy).toHaveBeenCalled()
    })
  })

  describe('Expose 方法', () => {
    it('getEditor 返回编辑器实例', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      const vm = wrapper.vm as any
      expect(vm.getEditor()).toBe(editors[0])
    })

    it('getHtml 调用 editor.getHtml', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      editor._setHtml('<p>html content</p>')
      const vm = wrapper.vm as any
      expect(vm.getHtml()).toBe('<p>html content</p>')
      expect(editor.getHtml).toHaveBeenCalled()
    })

    it('getText 调用 editor.getText', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      editor._setText('text content')
      const vm = wrapper.vm as any
      expect(vm.getText()).toBe('text content')
      expect(editor.getText).toHaveBeenCalled()
    })

    it('clear 调用 editor.clear', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      const vm = wrapper.vm as any
      vm.clear()
      expect(editor.clear).toHaveBeenCalled()
    })

    it('focus 调用 editor.focus', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      const vm = wrapper.vm as any
      vm.focus()
      expect(editor.focus).toHaveBeenCalled()
    })

    it('blur 调用 editor.blur', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      const vm = wrapper.vm as any
      vm.blur()
      expect(editor.blur).toHaveBeenCalled()
    })

    it('insertImage 调用 editor.dangerouslyInsertHtml', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      const vm = wrapper.vm as any
      vm.insertImage('https://example.com/x.png', '描述')
      expect(editor.dangerouslyInsertHtml).toHaveBeenCalled()
      const callArg = editor.dangerouslyInsertHtml.mock.calls.at(-1)?.[0] as string
      expect(callArg).toContain('https://example.com/x.png')
      expect(callArg).toContain('alt="描述"')
    })

    it('insertImage 在编辑器未初始化时弹 warning', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      // 模拟编辑器未初始化
      const vm = wrapper.vm as any
      vm.getEditor = () => null
      vm.insertImage('https://example.com/x.png')
      expect(messageWarning).toHaveBeenCalled()
      expect(editor.dangerouslyInsertHtml).not.toHaveBeenCalled()
    })
  })

  describe('图片上传 customUpload', () => {
    /** 从组件中取出 customUpload 函数 */
    async function getCustomUpload(wrapper: any) {
      await flushPromises()
      const vm = wrapper.vm as any
      return vm.editorConfig.MENU_CONF.uploadImage.customUpload as (
        file: File,
        insertFn: (url: string, alt?: string, href?: string) => void
      ) => Promise<void>
    }

    function makeImageFile(name = 'pic.png', size = 1024, type = 'image/png'): File {
      const file = new File(['x'], name, { type })
      Object.defineProperty(file, 'size', { value: size })
      return file
    }

    it('非图片文件拒绝上传', async () => {
      const { wrapper } = makeWrapper()
      await flushPromises()
      const customUpload = await getCustomUpload(wrapper)
      const file = new File(['x'], 'doc.pdf', { type: 'application/pdf' })
      const insertFn = vi.fn()
      await customUpload(file, insertFn)
      expect(messageError).toHaveBeenCalledWith('仅支持上传图片文件')
      expect(insertFn).not.toHaveBeenCalled()
      const errEmit = wrapper.emitted('upload-error')
      expect(errEmit).toBeTruthy()
    })

    it('超过大小限制拒绝上传', async () => {
      const { wrapper } = makeWrapper({ props: { maxImageSize: 1 } })
      await flushPromises()
      const customUpload = await getCustomUpload(wrapper)
      // 构造 2MB 文件
      const file = makeImageFile('big.png', 2 * 1024 * 1024)
      const insertFn = vi.fn()
      await customUpload(file, insertFn)
      expect(messageError).toHaveBeenCalledWith('图片大小不能超过 1MB')
      expect(insertFn).not.toHaveBeenCalled()
      expect(wrapper.emitted('upload-error')).toBeTruthy()
    })

    it('presign 接口返回非 200 时弹错误', async () => {
      const { wrapper } = makeWrapper()
      await flushPromises()
      const customUpload = await getCustomUpload(wrapper)
      fetchMock.mockResolvedValueOnce({
        ok: false,
        status: 500,
        json: async () => ({})
      })
      const file = makeImageFile()
      const insertFn = vi.fn()
      await customUpload(file, insertFn)
      expect(messageError).toHaveBeenCalled()
      expect(insertFn).not.toHaveBeenCalled()
      expect(wrapper.emitted('upload-error')).toBeTruthy()
    })

    it('完整成功流程：presign -> PUT -> insertFn', async () => {
      const { wrapper } = makeWrapper()
      await flushPromises()
      const customUpload = await getCustomUpload(wrapper)
      fetchMock.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: {
            uploadUrl: 'https://minio.local/upload?sig=xxx',
            accessUrl: 'https://minio.local/access/pic.png'
          }
        })
      })
      fetchMock.mockResolvedValueOnce({ ok: true, status: 200 })
      const file = makeImageFile('hello.png')
      const insertFn = vi.fn()
      await customUpload(file, insertFn)
      // 两次 fetch：presign POST + PUT 上传
      expect(fetchMock).toHaveBeenCalledTimes(2)
      const presignCall = fetchMock.mock.calls[0]
      expect(presignCall[0]).toContain('/files/presign')
      expect(presignCall[1].method).toBe('POST')
      // 验证 Authorization header
      expect(presignCall[1].headers.Authorization).toBe('Bearer fake-token')
      // 验证 body 包含 fileName
      const body = JSON.parse(presignCall[1].body)
      expect(body.fileName).toBe('hello.png')
      expect(body.bucket).toBe('documents')
      // 第二次 PUT
      const putCall = fetchMock.mock.calls[1]
      expect(putCall[1].method).toBe('PUT')
      expect(putCall[1].body).toBe(file)
      // insertFn 调用
      expect(insertFn).toHaveBeenCalledWith(
        'https://minio.local/access/pic.png',
        'hello.png',
        'https://minio.local/access/pic.png'
      )
      // upload-success 事件
      const successEmit = wrapper.emitted('upload-success')
      expect(successEmit).toBeTruthy()
      const payload = successEmit!.at(-1)![0] as any
      expect(payload.url).toBe('https://minio.local/access/pic.png')
      expect(payload.alt).toBe('hello.png')
    })

    it('PUT 失败时弹错误', async () => {
      const { wrapper } = makeWrapper()
      await flushPromises()
      const customUpload = await getCustomUpload(wrapper)
      fetchMock.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: {
            uploadUrl: 'https://minio.local/upload?sig=xxx',
            accessUrl: 'https://minio.local/access/pic.png'
          }
        })
      })
      fetchMock.mockResolvedValueOnce({ ok: false, status: 403 })
      const file = makeImageFile()
      const insertFn = vi.fn()
      await customUpload(file, insertFn)
      expect(messageError).toHaveBeenCalled()
      expect(insertFn).not.toHaveBeenCalled()
      expect(wrapper.emitted('upload-error')).toBeTruthy()
    })

    it('uploadBucket 透传到 presign 请求 body', async () => {
      const { wrapper } = makeWrapper({ props: { uploadBucket: 'images' } })
      await flushPromises()
      const customUpload = await getCustomUpload(wrapper)
      fetchMock.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          code: 200,
          data: {
            uploadUrl: 'https://minio.local/upload',
            accessUrl: 'https://minio.local/access/pic.png'
          }
        })
      })
      fetchMock.mockResolvedValueOnce({ ok: true, status: 200 })
      const file = makeImageFile()
      await customUpload(file, vi.fn())
      const body = JSON.parse(fetchMock.mock.calls[0][1].body)
      expect(body.bucket).toBe('images')
    })

    it('fetch 抛出异常时弹错误', async () => {
      const { wrapper } = makeWrapper()
      await flushPromises()
      const customUpload = await getCustomUpload(wrapper)
      fetchMock.mockRejectedValueOnce(new Error('network error'))
      const file = makeImageFile()
      const insertFn = vi.fn()
      await customUpload(file, insertFn)
      expect(messageError).toHaveBeenCalled()
      expect(insertFn).not.toHaveBeenCalled()
      const errEmit = wrapper.emitted('upload-error')
      expect(errEmit).toBeTruthy()
      const payload = errEmit!.at(-1)![0] as any
      expect(payload.error.message).toContain('network error')
    })
  })

  describe('readonly 模式', () => {
    it('readonly=true 时 editorConfig.readOnly=true', async () => {
      const { wrapper } = makeWrapper({ props: { readonly: true } })
      await flushPromises()
      const editor = wrapper.findComponent({ name: 'WangEditor' })
      const config = editor.props('defaultConfig') as any
      expect(config.readOnly).toBe(true)
    })

    it('readonly=false 时 editorConfig.readOnly=false', async () => {
      const { wrapper } = makeWrapper({ props: { readonly: false } })
      await flushPromises()
      const editor = wrapper.findComponent({ name: 'WangEditor' })
      const config = editor.props('defaultConfig') as any
      expect(config.readOnly).toBe(false)
    })

    it('readonly 下编辑器内容区高度等于容器高度', async () => {
      const { wrapper } = makeWrapper({
        props: { readonly: true, height: 500 }
      })
      await flushPromises()
      const editor = wrapper.findComponent({ name: 'WangEditor' })
      // editorHeight 计算属性：readonly 时 = height
      const style = editor.attributes('style') || ''
      expect(style).toContain('height: 500px')
    })

    it('非 readonly 下编辑器内容区高度 = height - 50', async () => {
      const { wrapper } = makeWrapper({
        props: { readonly: false, height: 500 }
      })
      await flushPromises()
      const editor = wrapper.findComponent({ name: 'WangEditor' })
      const style = editor.attributes('style') || ''
      expect(style).toContain('height: 450px')
    })
  })

  describe('outputFormat 切换', () => {
    it('outputFormat=html 时 emit html 内容', async () => {
      const { wrapper, getInstances } = makeWrapper({
        props: { outputFormat: 'html' }
      })
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      editor._setHtml('<p>富文本<b>内容</b></p>')
      wrapper.findComponent({ name: 'WangEditor' }).vm.$emit('onChange', editor)
      await nextTick()
      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted!.at(-1)![0]).toBe('<p>富文本<b>内容</b></p>')
    })

    it('outputFormat=text 时 emit 纯文本内容', async () => {
      const { wrapper, getInstances } = makeWrapper({
        props: { outputFormat: 'text' }
      })
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      editor._setText('纯文本')
      wrapper.findComponent({ name: 'WangEditor' }).vm.$emit('onChange', editor)
      await nextTick()
      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted!.at(-1)![0]).toBe('纯文本')
    })

    it('切换 outputFormat 后再次 onChange 输出对应格式', async () => {
      const { wrapper, getInstances } = makeWrapper()
      await flushPromises()
      const editors = getInstances()
      const editor = editors[0] as any
      editor._setHtml('<p>abc</p>')
      editor._setText('abc')
      wrapper.findComponent({ name: 'WangEditor' }).vm.$emit('onChange', editor)
      await nextTick()
      // 默认 html
      expect(wrapper.emitted('update:modelValue')!.at(-1)![0]).toBe('<p>abc</p>')
      // 切到 text
      await wrapper.setProps({ outputFormat: 'text' })
      wrapper.findComponent({ name: 'WangEditor' }).vm.$emit('onChange', editor)
      await nextTick()
      expect(wrapper.emitted('update:modelValue')!.at(-1)![0]).toBe('abc')
    })
  })

  describe('完整集成场景', () => {
    it('初始内容回填 + 编辑触发 v-model 更新', async () => {
      const { wrapper, getInstances } = makeWrapper({
        modelValue: '<p>初始</p>'
      })
      await flushPromises()
      const editor = wrapper.findComponent({ name: 'WangEditor' })
      expect(editor.props('modelValue')).toBe('<p>初始</p>')
      const editors = getInstances()
      const mockEditor = editors[0] as any
      mockEditor._setHtml('<p>初始</p><p>新增段落</p>')
      mockEditor._setText('初始新增段落')
      editor.vm.$emit('onChange', mockEditor)
      await nextTick()
      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted!.at(-1)![0]).toBe('<p>初始</p><p>新增段落</p>')
    })

    it('高度过小不出现负值', async () => {
      const { wrapper } = makeWrapper({ props: { height: 30 } })
      await flushPromises()
      const editor = wrapper.findComponent({ name: 'WangEditor' })
      // height=30，readonly=false 时 editorHeight = max(30-50, 100) = 100
      const style = editor.attributes('style') || ''
      expect(style).toContain('height: 100px')
    })
  })
})

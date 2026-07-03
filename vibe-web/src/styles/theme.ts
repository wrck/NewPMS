/**
 * Ant Design Vue 4.x ConfigProvider 主题配置
 * 按系统设计文档 3.1.2 / 3.1.3 定制品牌主色 / 状态色 / 圆角 / 字号等 token
 * 组件级细节样式（表格表头、菜单选中态等）由 src/styles/global.less 统一处理
 */
import type { ThemeConfig } from 'ant-design-vue/es/config-provider/context'

export const themeConfig: ThemeConfig = {
  token: {
    // 品牌主色（Ant Design 蓝色系）
    colorPrimary: '#1677FF',
    colorPrimaryHover: '#4096FF',
    colorPrimaryActive: '#0958D9',
    // 信息色（与品牌色一致）
    colorInfo: '#1677FF',
    // 状态色
    colorSuccess: '#52C41A',
    colorWarning: '#FAAD14',
    colorError: '#FF4D4F',
    // 链接色
    colorLink: '#1677FF',
    colorLinkHover: '#4096FF',
    colorLinkActive: '#0958D9',
    // 文字色
    colorText: '#1F1F1F',
    colorTextSecondary: '#595959',
    colorTextTertiary: '#8C8C8C',
    colorTextQuaternary: '#BFBFBF',
    colorTextDisabled: '#BFBFBF',
    // 背景色
    colorBgLayout: '#F5F5F5',
    colorBgContainer: '#FFFFFF',
    colorBgElevated: '#FFFFFF',
    // 边框
    colorBorder: '#D9D9D9',
    colorBorderSecondary: '#F0F0F0',
    // 圆角（卡片 8px / 按钮 6px / 标签 4px）
    borderRadius: 6,
    borderRadiusLG: 8,
    borderRadiusSM: 4,
    borderRadiusXS: 2,
    // 字体
    fontFamily:
      '-apple-system, "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif',
    fontSize: 14,
    // 阴影
    boxShadow: '0 1px 2px rgba(0,0,0,0.03), 0 1px 6px rgba(0,0,0,0.06)',
    boxShadowSecondary: '0 3px 6px rgba(0,0,0,0.06), 0 6px 16px rgba(0,0,0,0.08)',
    // 控件高度
    controlHeight: 32,
    controlHeightLG: 40,
    controlHeightSM: 24
  }
  // 组件级 token（表格表头灰底、菜单选中态、Header 高度等）
  // 因 antd-vue 组件 token 类型定义不完整，统一在 src/styles/global.less 中通过 CSS 覆盖实现
}

/** 业务状态色调语义（设计文档 3.1.2 / 3.1.4 状态语义 → 颜色映射） */
export type StatusTone =
  | 'default' // 未开始/待处理 灰
  | 'processing' // 进行中 蓝
  | 'warning' // 待审核/待确认 橙
  | 'success' // 已完成/已通过 绿
  | 'error' // 超期/异常/驳回 红
  | 'pause' // 暂停/挂起 黄
  | 'archived' // 已归档/已取消 灰
  | 'agent' // 代理商代施 紫

export interface StatusColorOption {
  /** 文字色 */
  color: string
  /** 背景色 */
  background: string
  /** 边框色 */
  border: string
}

/** 状态色调 → 颜色映射表（全局统一） */
export const statusColorMap: Record<StatusTone, StatusColorOption> = {
  default: { color: '#8C8C8C', background: '#F5F5F5', border: '#D9D9D9' },
  processing: { color: '#1677FF', background: '#E6F4FF', border: '#91CAFF' },
  warning: { color: '#FAAD14', background: '#FFFBE6', border: '#FFD666' },
  success: { color: '#52C41A', background: '#F6FFED', border: '#B7EB8F' },
  error: { color: '#FF4D4F', background: '#FFF2F0', border: '#FFCCC7' },
  pause: { color: '#FAAD14', background: '#FFFBE6', border: '#FFD666' },
  archived: { color: '#BFBFBF', background: '#F5F5F5', border: '#D9D9D9' },
  agent: { color: '#722ED1', background: '#F9F0FF', border: '#D3ADF7' }
}

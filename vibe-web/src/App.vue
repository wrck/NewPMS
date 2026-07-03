<script setup lang="ts">
/**
 * 应用根组件
 * - 通过 a-config-provider 注入主题 token（主色 #1677FF）
 * - 中文 locale
 */
import { ConfigProvider } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { themeConfig } from '@/styles/theme'
</script>

<template>
  <ConfigProvider :locale="zhCN" :theme="themeConfig">
    <RouterView />
  </ConfigProvider>
</template>

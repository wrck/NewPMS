<script setup lang="ts">
/**
 * 顶部导航栏（设计文档 3.2）
 * height: 56px
 * LOGO + 面包屑导航 + 消息 + 用户头像/名称
 */
import { LayoutHeader } from 'ant-design-vue'
import { MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons-vue'
import Breadcrumb from './Breadcrumb.vue'
import UserMenu from './UserMenu.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<template>
  <LayoutHeader class="app-header">
    <div class="header-left">
      <span class="sider-trigger" @click="appStore.toggleSider()">
        <MenuUnfoldOutlined v-if="appStore.siderCollapsed" />
        <MenuFoldOutlined v-else />
      </span>
      <div class="logo" @click="$router.push('/dashboard')">
        <img src="/favicon.svg" alt="logo" class="logo-img" />
        <span class="logo-text">Vibe 交付管理平台</span>
      </div>
      <Breadcrumb />
    </div>
    <div class="header-right">
      <UserMenu />
    </div>
  </LayoutHeader>
</template>

<style lang="less" scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: @header-height;
  padding: 0 16px 0 0;
  background: @bg-container;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 100%;
  min-width: 0;
}
.sider-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 100%;
  font-size: 18px;
  color: @text-secondary;
  cursor: pointer;
  transition: color 0.2s;
  &:hover {
    color: @brand-primary;
  }
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding-right: 16px;
  border-right: 1px solid #f0f0f0;
  height: 100%;
}
.logo-img {
  width: 28px;
  height: 28px;
}
.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: @text-primary;
  white-space: nowrap;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>

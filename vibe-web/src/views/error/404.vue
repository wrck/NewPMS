<script setup lang="ts">
/**
 * 404 页面
 */
import { useRouter } from 'vue-router'
import { Button } from 'ant-design-vue'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="nf-content">
      <div class="nf-code">404</div>
      <h2 class="nf-title">页面走丢了</h2>
      <p class="nf-desc">抱歉，您访问的页面不存在或已被移除</p>
      <div class="nf-actions">
        <Button type="primary" size="large" @click="router.push('/')">返回首页</Button>
        <Button size="large" @click="router.back()">返回上一页</Button>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100vh;
  background: @bg-page;
}
.nf-content {
  text-align: center;
}
.nf-code {
  font-size: 120px;
  font-weight: 700;
  color: @brand-primary;
  line-height: 1;
  font-family: 'DIN Alternate', sans-serif;
  opacity: 0.8;
}
.nf-title {
  font-size: 24px;
  color: @text-primary;
  margin: 16px 0 8px;
}
.nf-desc {
  font-size: 14px;
  color: @text-tertiary;
  margin: 0 0 32px;
}
.nf-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>

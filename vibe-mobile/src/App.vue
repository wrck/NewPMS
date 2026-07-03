<script setup lang="ts">
/**
 * 根组件
 */
</script>

<template>
  <router-view v-slot="{ Component }">
    <keep-alive :include="['Home', 'Task', 'Field', 'Mine']">
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>

<style scoped></style>

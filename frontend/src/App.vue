<template>
  <ClientTitleBar v-if="showChrome" />
  <ClientIntro v-if="showChrome" />
  <div class="app-frame" :class="{ 'app-frame--client': showChrome }">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import ClientTitleBar from './components/client/ClientTitleBar.vue'
import ClientIntro from './components/client/ClientIntro.vue'
import { isClient } from './utils/clientTheme'

/** 桌面客户端专属 chrome（网页端无 preload 注入，不渲染） */
const showChrome = isClient()
</script>

<style>
/* 客户端固定标题栏：内容整体下移 40px，避免原生窗口按钮悬浮在页面上（下拉显示 bug） */
.app-frame--client {
  padding-top: 40px;
}
</style>

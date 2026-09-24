<template>
  <ClientTitleBar v-if="showChrome" />
  <router-view />
</template>

<script setup lang="ts">
import ClientTitleBar from './components/client/ClientTitleBar.vue'
import { isClient } from './utils/clientTheme'

/** 桌面客户端专属 chrome（网页端无 preload 注入，不渲染） */
const showChrome = isClient()
</script>

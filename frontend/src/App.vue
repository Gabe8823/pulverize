<template>
  <ClientTitleBar v-if="showChrome" />
  <ClientIntro v-if="showChrome" />
  <div class="app-frame" :class="{ 'app-frame--client': showChrome }">
    <router-view v-slot="{ Component }">
      <Transition name="page-fade" mode="out-in">
        <component :is="Component" :key="route.path" />
      </Transition>
    </router-view>
  </div>
</template>

<script setup lang="ts">
import ClientTitleBar from './components/client/ClientTitleBar.vue'
import ClientIntro from './components/client/ClientIntro.vue'
import { isClient } from './utils/clientTheme'
import { useRoute } from 'vue-router'

/** 桌面客户端专属 chrome（网页端无 preload 注入，不渲染） */
const showChrome = isClient()

/** 路由切换过渡：page-fade 定义在 theme.scss（reduced-motion 下已关停） */
const route = useRoute()
</script>

<style>
/* 客户端固定标题栏：内容整体下移 40px，避免原生窗口按钮悬浮在页面上（下拉显示 bug） */
.app-frame--client {
  padding-top: 40px;
}
</style>

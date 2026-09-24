'use strict'
/**
 * 客户端预加载：向页面暴露只读标识，
 * 前端据此渲染「桌面版专属 chrome」（自定义标题栏 / 背景切换），网页端没有该对象。
 * 其余能力一律不暴露（contextIsolation + sandbox 保持开启）。
 */
const { contextBridge } = require('electron')

contextBridge.exposeInMainWorld('runai', {
  isClient: true,
  client: 'electron',
  electron: process.versions.electron,
  chrome: process.versions.chrome
})

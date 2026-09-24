/**
 * ECharts 按需注册中心（去全量引入，显著缩小产物体积）
 * 仅注册本项目用到的图表/组件/渲染器；页面通过 chartInit 创建实例。
 * 类型仍从 'echarts' 以 import type 引入 —— 类型在编译期擦除，不进 bundle。
 */
import { init, use } from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

let registered = false

function ensureRegistered(): void {
  if (registered) return
  use([BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])
  registered = true
}

/** 初始化图表（幂等注册后交给 echarts/core） */
export function chartInit(
  el: HTMLElement,
  theme?: string | object,
): ReturnType<typeof init> {
  ensureRegistered()
  return init(el, theme)
}

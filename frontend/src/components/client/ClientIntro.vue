<template>
  <div v-if="alive" class="intro" :class="{ gone: leaving }" @click="skip">
    <div class="intro-stage">
      <span class="intro-ring ring-a" />
      <span class="intro-ring ring-b" />

      <div class="intro-capsule">
        <span class="cap-half cap-top" />
        <span class="cap-half cap-bottom" />
        <span class="cap-shine" />
      </div>

      <span
        v-for="p in 12"
        :key="p"
        class="intro-dot"
        :style="{ '--a': `${(p - 1) * 30}deg`, '--d': `${90 + ((p * 37) % 80)}px`, '--pd': `${0.62 + (p % 4) * 0.05}s` }"
      />

      <div class="intro-word">
        <span class="intro-word-text">PULVERIZE</span>
        <span class="intro-slash" />
      </div>
      <div class="intro-tag">胶囊变身 · 动力核心上线</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'

/**
 * 桌面客户端开场「变身」动画（假面骑士 ZZZ 风格的胶囊意象）：
 * 胶囊浮现 → 撕裂爆发（粒子+能量环）→ PULVERIZE 字标揭示 → 淡出。
 * 每次启动播放一次；点击可跳过；系统开启「减少动态效果」时不渲染。
 */
const alive = ref(true)
const leaving = ref(false)
const timers: number[] = []

function skip() {
  if (leaving.value) return
  leaving.value = true
  timers.push(window.setTimeout(() => (alive.value = false), 420))
}

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    alive.value = false
    return
  }
  timers.push(window.setTimeout(() => (leaving.value = true), 2050))
  timers.push(window.setTimeout(() => (alive.value = false), 2500))
})

onBeforeUnmount(() => timers.forEach((t) => window.clearTimeout(t)))
</script>

<style scoped>
.intro {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  background:
    radial-gradient(58% 58% at 50% 44%, rgba(227, 6, 19, 0.2), transparent 72%),
    radial-gradient(120% 120% at 50% 50%, #121216 0%, #050507 68%);
  transition: opacity 0.42s ease;
  -webkit-app-region: no-drag;
}
.intro.gone {
  opacity: 0;
  pointer-events: none;
}

.intro-stage {
  position: relative;
  width: min(560px, 88vw);
  height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* ---- 能量环 ---- */
.intro-ring {
  position: absolute;
  left: 50%;
  top: 40%;
  width: 74px;
  height: 74px;
  margin: -37px 0 0 -37px;
  border: 2px solid rgba(227, 6, 19, 0.85);
  border-radius: 50%;
  opacity: 0;
  transform: scale(0.35);
  animation: ring-burst 0.95s cubic-bezier(0.16, 0.84, 0.3, 1) 0.6s forwards;
}
.ring-b {
  border-color: rgba(255, 255, 255, 0.75);
  animation-delay: 0.72s;
}
@keyframes ring-burst {
  0% {
    opacity: 0.95;
    transform: scale(0.35);
  }
  100% {
    opacity: 0;
    transform: scale(7.5);
  }
}

/* ---- 胶囊 ---- */
.intro-capsule {
  position: absolute;
  left: 50%;
  top: 40%;
  width: 158px;
  height: 64px;
  margin: -32px 0 0 -79px;
  animation: cap-in 0.55s cubic-bezier(0.2, 0.9, 0.25, 1.25) both,
    cap-glow 1.1s ease-in-out 0.5s infinite alternate;
}
@keyframes cap-in {
  0% {
    opacity: 0;
    transform: translateY(-56px) scale(0.6) rotate(-6deg);
  }
  70% {
    transform: translateY(4px) scale(1.04) rotate(1deg);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1) rotate(0deg);
  }
}
@keyframes cap-glow {
  from {
    filter: drop-shadow(0 0 10px rgba(227, 6, 19, 0.55));
  }
  to {
    filter: drop-shadow(0 0 26px rgba(227, 6, 19, 0.95));
  }
}
.cap-half {
  position: absolute;
  left: 0;
  width: 100%;
  height: 50%;
  background: linear-gradient(180deg, #ff3040 0%, #e30613 58%, #a90410 100%);
}
.cap-top {
  top: 0;
  border-radius: 999px 999px 6px 6px;
  animation: cap-split-up 0.55s cubic-bezier(0.3, 0, 0.6, 1) 0.66s forwards;
}
.cap-bottom {
  bottom: 0;
  border-radius: 6px 6px 999px 999px;
  background: linear-gradient(180deg, #d30512 0%, #8d030d 100%);
  animation: cap-split-down 0.55s cubic-bezier(0.3, 0, 0.6, 1) 0.7s forwards;
}
@keyframes cap-split-up {
  to {
    transform: translate(-26px, -72px) rotate(-26deg);
    opacity: 0;
  }
}
@keyframes cap-split-down {
  to {
    transform: translate(26px, 72px) rotate(26deg);
    opacity: 0;
  }
}
.cap-shine {
  position: absolute;
  left: 14px;
  top: 7px;
  width: 64px;
  height: 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0));
  animation: cap-fade 0.4s ease 0.66s forwards;
}
@keyframes cap-fade {
  to {
    opacity: 0;
  }
}

/* ---- 爆发粒子 ---- */
.intro-dot {
  position: absolute;
  left: 50%;
  top: 40%;
  width: 7px;
  height: 7px;
  margin: -3.5px 0 0 -3.5px;
  border-radius: 50%;
  background: #ff4a55;
  box-shadow: 0 0 10px rgba(227, 6, 19, 0.9);
  opacity: 0;
  animation: dot-fly 0.85s cubic-bezier(0.2, 0.7, 0.4, 1) var(--pd) forwards;
}
@keyframes dot-fly {
  0% {
    opacity: 1;
    transform: rotate(var(--a)) translateX(6px) scale(1);
  }
  100% {
    opacity: 0;
    transform: rotate(var(--a)) translateX(var(--d)) scale(0.25);
  }
}

/* ---- 字标 ---- */
.intro-word {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(40% + 74px);
  text-align: center;
  overflow: hidden;
}
.intro-word-text {
  display: inline-block;
  font-size: clamp(30px, 7vw, 58px);
  font-weight: 800;
  letter-spacing: 0.3em;
  padding-left: 0.3em; /* 视觉居中：抵消末字符字距 */
  color: #f5f5f7;
  clip-path: inset(0 100% 0 0);
  animation: word-wipe 0.42s cubic-bezier(0.2, 0.7, 0.3, 1) 1.04s forwards;
}
@keyframes word-wipe {
  to {
    clip-path: inset(0 0 0 0);
  }
}
.intro-slash {
  position: absolute;
  left: 8%;
  right: 8%;
  top: 50%;
  height: 3px;
  background: linear-gradient(90deg, transparent, #e30613 22%, #ff5a63 50%, #e30613 78%, transparent);
  transform: translateX(-110%) skewX(-28deg);
  opacity: 0;
  animation: slash-sweep 0.5s cubic-bezier(0.5, 0, 0.3, 1) 1.12s forwards;
}
@keyframes slash-sweep {
  15% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateX(110%) skewX(-28deg);
  }
}
.intro-tag {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(40% + 148px);
  text-align: center;
  font-size: 13px;
  letter-spacing: 0.5em;
  padding-left: 0.5em;
  color: #8f8f97;
  opacity: 0;
  transform: translateY(8px);
  animation: tag-up 0.4s ease 1.38s forwards;
}
@keyframes tag-up {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

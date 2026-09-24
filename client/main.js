'use strict'
/**
 * RunAI 桌面客户端
 * - 内置本地静态服务（加载打包进来的 frontend/dist），端口 5219
 * - /api/* 同源代理到本机后端 127.0.0.1:8080（前端 baseURL='/api' 零改动）
 * - 后端未运行时自动拉起打包的 jar（需要本机 Java 21+），并展示启动等待页
 */
const { app, BrowserWindow, shell } = require('electron')
const http = require('http')
const fs = require('fs')
const path = require('path')
const net = require('net')
const { spawn, execFile } = require('child_process')

const BASE_PORT = 5219
const BACKEND_HOST = '127.0.0.1'
const BACKEND_PORT = 8080

const isPackaged = app.isPackaged
const distDir = isPackaged
  ? path.join(__dirname, 'dist')
  : path.join(__dirname, '..', 'frontend', 'dist')
const backendResDir = isPackaged
  ? path.join(process.resourcesPath, 'backend')
  : path.join(__dirname, 'backend')

let win = null
let backendChild = null
const backendState = { up: false, spawned: false, error: '', lastAttempt: 0, dist: true }

function log() {
  console.log.apply(console, ['[runai]'].concat([].slice.call(arguments)))
}

function isPortUp(port, host) {
  return new Promise(function (resolve) {
    const sock = net.connect({ port: port, host: host })
    let done = false
    function finish(ok) {
      if (done) return
      done = true
      try { sock.destroy() } catch (e) { /* noop */ }
      resolve(ok)
    }
    sock.setTimeout(1200)
    sock.once('connect', function () { finish(true) })
    sock.once('timeout', function () { finish(false) })
    sock.once('error', function () { finish(false) })
  })
}

function findJar() {
  try {
    const files = fs.readdirSync(backendResDir)
    const jar = files.filter(function (f) { return f.endsWith('.jar') })[0]
    return jar ? path.join(backendResDir, jar) : null
  } catch (e) {
    return null
  }
}

function trySpawnBackend() {
  if (backendState.spawned || backendState.up) return
  const jar = findJar()
  if (!jar) {
    backendState.error = '未找到后端 jar（client/backend/*.jar），无法自动启动'
    return
  }
  execFile('java', ['-version'], function (err) {
    if (err) {
      backendState.error = '未检测到 Java 运行环境（需要 Java 21+）'
      return
    }
    backendState.spawned = true
    backendState.error = ''
    log('starting backend:', jar)
    const child = spawn('java', ['-jar', jar], {
      cwd: backendResDir,
      stdio: ['ignore', 'pipe', 'pipe']
    })
    backendChild = child
    child.stdout.on('data', function (d) { log('[backend]', String(d).trim()) })
    child.stderr.on('data', function (d) { log('[backend]', String(d).trim()) })
    child.on('exit', function (code) {
      log('backend exited with code', code)
      backendState.spawned = false
      backendChild = null
      if (!backendState.up) backendState.error = '后端启动失败（退出码 ' + code + '）'
    })
  })
}

async function refreshBackend() {
  backendState.up = await isPortUp(BACKEND_HOST, BACKEND_PORT)
  if (backendState.up) return true
  const now = Date.now()
  if (now - backendState.lastAttempt > 5000) {
    backendState.lastAttempt = now
    trySpawnBackend()
  }
  return false
}

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.json': 'application/json; charset=utf-8',
  '.ico': 'image/x-icon',
  '.txt': 'text/plain; charset=utf-8'
}

function serveFile(res, filePath, code) {
  fs.readFile(filePath, function (err, buf) {
    if (err) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' })
      res.end('Not Found')
      return
    }
    res.writeHead(code || 200, {
      'Content-Type': MIME[path.extname(filePath).toLowerCase()] || 'application/octet-stream',
      'Cache-Control': 'no-cache'
    })
    res.end(buf)
  })
}

function proxyToBackend(req, res) {
  const headers = Object.assign({}, req.headers)
  delete headers.host
  delete headers.connection
  const preq = http.request(
    {
      host: BACKEND_HOST,
      port: BACKEND_PORT,
      path: req.url,
      method: req.method,
      headers: headers
    },
    function (pres) {
      res.writeHead(pres.statusCode, pres.headers)
      pres.pipe(res)
    }
  )
  preq.on('error', function (e) {
    if (res.headersSent) { res.destroy(); return }
    res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' })
    res.end(JSON.stringify({ code: 502, msg: '后端服务未启动：' + e.message }))
  })
  req.pipe(preq)
}

function sendJson(res, obj) {
  res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' })
  res.end(JSON.stringify(obj))
}

function createServer() {
  return http.createServer(function (req, res) {
    const pathname = decodeURIComponent((req.url || '/').split('?')[0])

    if (pathname === '/__health') {
      refreshBackend().then(function (up) {
        sendJson(res, {
          up: up,
          spawned: backendState.spawned,
          error: backendState.error,
          dist: fs.existsSync(path.join(distDir, 'index.html'))
        })
      })
      return
    }

    if (pathname === '/__retry' && req.method === 'POST') {
      backendState.error = ''
      if (backendChild) {
        backendState.spawned = false
        try { backendChild.kill() } catch (e) { /* noop */ }
        backendChild = null
        setTimeout(function () { refreshBackend() }, 1500)
      } else {
        backendState.spawned = false
        refreshBackend()
      }
      sendJson(res, { ok: true })
      return
    }

    if (pathname.indexOf('/api/') === 0) {
      proxyToBackend(req, res)
      return
    }

    const isRoot = pathname === '/' || pathname === '/index.html'
    if (isRoot && !backendState.up) {
      serveFile(res, path.join(__dirname, 'loading.html'), 200)
      return
    }

    const rel = isRoot ? 'index.html' : pathname.slice(1)
    const fp = path.normalize(path.join(distDir, rel))
    if (fp.indexOf(distDir) !== 0) {
      res.writeHead(403)
      res.end()
      return
    }
    fs.stat(fp, function (err, st) {
      if (!err && st.isFile()) {
        serveFile(res, fp)
        return
      }
      serveFile(res, path.join(distDir, 'index.html'))
    })
  })
}

function listenWithFallback(server, port, tries) {
  server.once('error', function (err) {
    if (err && err.code === 'EADDRINUSE' && tries > 0) {
      listenWithFallback(server, port + 1, tries - 1)
    } else {
      app.quit()
    }
  })
  server.listen(port, '127.0.0.1', function () {
    log('local server on http://127.0.0.1:' + port)
    app.whenReady().then(function () {
      createWindow('http://127.0.0.1:' + port + '/')
    })
  })
}

function createWindow(startUrl) {
  win = new BrowserWindow({
    width: 1320,
    height: 880,
    minWidth: 1024,
    minHeight: 680,
    backgroundColor: '#f5f5f7',
    show: false,
    title: 'RunAI · 智能跑步',
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  })
  win.once('ready-to-show', function () { win.show() })
  win.webContents.setWindowOpenHandler(function (details) {
    if (/^https?:/i.test(details.url)) shell.openExternal(details.url)
    return { action: 'deny' }
  })
  win.webContents.on('did-fail-load', function (e, code, desc) {
    log('did-fail-load', code, desc)
  })
  win.loadURL(startUrl)
}

const gotLock = app.requestSingleInstanceLock()
if (!gotLock) {
  app.quit()
} else {
  app.on('second-instance', function () {
    if (win) {
      if (win.isMinimized()) win.restore()
      win.focus()
    }
  })

  app.whenReady().then(function () {
    backendState.dist = fs.existsSync(path.join(distDir, 'index.html'))
    const server = createServer()
    listenWithFallback(server, BASE_PORT, 5)
    refreshBackend()
  })

  app.on('window-all-closed', function () {
    app.quit()
  })

  app.on('will-quit', function () {
    if (backendChild) {
      try { backendChild.kill() } catch (e) { /* noop */ }
      backendChild = null
    }
  })
}

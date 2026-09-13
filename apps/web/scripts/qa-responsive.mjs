import { existsSync } from 'node:fs'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { spawn } from 'node:child_process'

const url = process.env.QA_URL ?? 'http://127.0.0.1:5173/'
const port = 9333
const candidates = [
  process.env.BROWSER_BIN,
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
].filter(Boolean)
const browserPath = candidates.find(existsSync)
if (!browserPath) throw new Error('No se encontró Edge o Chrome para el QA responsive')

const profile = await mkdtemp(join(tmpdir(), 'sdbrewpi-responsive-'))
const browser = spawn(browserPath, [
  '--headless=new',
  '--disable-gpu',
  '--hide-scrollbars',
  `--remote-debugging-port=${port}`,
  `--user-data-dir=${profile}`,
  '--remote-allow-origins=*',
  'about:blank',
], { stdio: 'ignore' })

const delay = milliseconds => new Promise(resolveDelay => setTimeout(resolveDelay, milliseconds))

async function findPage() {
  for (let attempt = 0; attempt < 40; attempt++) {
    try {
      const pages = await fetch(`http://127.0.0.1:${port}/json`).then(response => response.json())
      const page = pages.find(candidate => candidate.type === 'page')
      if (page) return page
    } catch { /* Browser todavía iniciando. */ }
    await delay(100)
  }
  throw new Error('El navegador de QA no respondió')
}

try {
  const page = await findPage()
  const socket = new WebSocket(page.webSocketDebuggerUrl)
  await new Promise((resolveOpen, rejectOpen) => {
    socket.onopen = resolveOpen
    socket.onerror = rejectOpen
  })

  let sequence = 0
  const pending = new Map()
  socket.onmessage = event => {
    const message = JSON.parse(event.data)
    const handler = pending.get(message.id)
    if (!handler) return
    pending.delete(message.id)
    if (message.error) handler.reject(new Error(message.error.message))
    else handler.resolve(message.result)
  }
  const send = (method, params = {}) => new Promise((resolveCommand, rejectCommand) => {
    const id = ++sequence
    pending.set(id, { resolve: resolveCommand, reject: rejectCommand })
    socket.send(JSON.stringify({ id, method, params }))
  })

  await send('Emulation.setDeviceMetricsOverride', {
    width: 390,
    height: 844,
    deviceScaleFactor: 1,
    mobile: true,
  })
  await send('Page.navigate', { url })
  await delay(1800)
  const inspectLayout = async label => {
    const evaluation = await send('Runtime.evaluate', {
      returnByValue: true,
      expression: `(() => {
      const viewportWidth = document.documentElement.clientWidth;
      const offenders = [...document.querySelectorAll('body *')]
        .map(element => ({
          tag: element.tagName.toLowerCase(),
          className: typeof element.className === 'string' ? element.className : '',
          left: Math.round(element.getBoundingClientRect().left),
          right: Math.round(element.getBoundingClientRect().right),
        }))
        .filter(box => box.left < -1 || box.right > viewportWidth + 1)
        .slice(0, 12);
      return {
        label: ${JSON.stringify(label)},
        viewportWidth,
        documentWidth: document.documentElement.scrollWidth,
        offenders,
      };
    })()`,
    })
    return evaluation.result.value
  }
  const plant = await inspectLayout('Mi Planta')
  await send('Runtime.evaluate', {
    expression: `([...document.querySelectorAll('button')].find(button => button.textContent.includes('Agregar bodega'))).click()`,
  })
  await delay(300)
  const warehouseEditor = await inspectLayout('Editor de bodega')
  await send('Runtime.evaluate', {
    expression: `([...document.querySelectorAll('nav button')].find(button => button.textContent.includes('Producción'))).click()`,
  })
  await delay(900)
  const production = await inspectLayout('Producción')
  const results = [plant, warehouseEditor, production]
  console.log(JSON.stringify(results, null, 2))
  socket.close()
  if (results.some(result => result.documentWidth > result.viewportWidth || result.offenders.length > 0)) process.exitCode = 1
} finally {
  browser.kill()
  await delay(300)
  const safeProfile = resolve(profile)
  if (safeProfile.startsWith(resolve(tmpdir()))) await rm(safeProfile, { recursive: true, force: true }).catch(() => {})
}

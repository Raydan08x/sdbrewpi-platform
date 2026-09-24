import { existsSync } from 'node:fs'
import { mkdtemp, rm, writeFile } from 'node:fs/promises'
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
  const login = await inspectLayout('Login')
  const loginGuard = await send('Runtime.evaluate', { returnByValue: true, expression: `(() => {
    if (!document.body.textContent.includes('SDBrewPi Control')) throw new Error('Falta identidad del login');
    if (!document.querySelector('input[autocomplete="username"]')) throw new Error('Falta campo de usuario');
    if (!document.querySelector('input[autocomplete="current-password"]')) throw new Error('Falta campo de contraseña');
    return true;
  })()` })
  if (loginGuard.exceptionDetails) throw new Error('Falló la pantalla de acceso')
  if (process.env.QA_STATIC_PREVIEW === 'true') {
    await send('Runtime.evaluate', { expression: `([...document.querySelectorAll('button')].find(button => button.textContent.includes('Ver demostración'))).click()` })
    await delay(900)
  }
  const plant = await inspectLayout('Mi Planta')
  if (process.env.QA_STATIC_PREVIEW === 'true') {
    const submit = await send('Runtime.evaluate', { expression: `(() => {
      if (!document.body.textContent.includes('Demostración pública')) throw new Error('Falta aviso público');
      document.querySelector('form.plant-profile').requestSubmit();
    })()` })
    if (submit.exceptionDetails) throw new Error('No se pudo verificar el modo público')
    await delay(300)
    const blocked = await send('Runtime.evaluate', { returnByValue: true, expression: `document.body.textContent.includes('los cambios están deshabilitados')` })
    if (!blocked.result.value) throw new Error('La demostración no bloqueó la escritura')
  }
  await send('Runtime.evaluate', {
    expression: `([...document.querySelectorAll('button')].find(button => button.textContent.includes('Agregar bodega'))).click()`,
  })
  await delay(300)
  const warehouseEditor = await inspectLayout('Editor de bodega')
  await send('Runtime.evaluate', { expression: `document.querySelector('[aria-label="Cerrar editor de bodega"]')?.click()` })
  await send('Runtime.evaluate', {
    expression: `([...document.querySelectorAll('nav button')].find(button => button.textContent.includes('Inventarios'))).click()`,
  })
  await delay(700)
  const inventory = await inspectLayout('Inventarios')
  const inventoryGuard = await send('Runtime.evaluate', { returnByValue: true, expression: `(() => {
    if (!document.body.textContent.includes('MALTA-PALE')) throw new Error('Falta el artículo de demostración');
    const create = [...document.querySelectorAll('button')].find(button => button.textContent.includes('Nuevo artículo'));
    if (!create) throw new Error('Falta alta de artículo');
    create.click(); return true;
  })()` })
  if (inventoryGuard.exceptionDetails) throw new Error('Falló el módulo Inventarios')
  await delay(200)
  const inventoryEditor = await inspectLayout('Nuevo artículo de inventario')
  await send('Runtime.evaluate', {
    expression: `([...document.querySelectorAll('nav button')].find(button => button.textContent.includes('Producción'))).click()`,
  })
  await delay(900)
  const production = await inspectLayout('Producción')
  const releaseButton = await send('Runtime.evaluate', { returnByValue: true, expression: `(() => {
    const release = [...document.querySelectorAll('button')].find(button => button.textContent.includes('Liberar orden'));
    if (!release) return { found: false, body: document.body.innerText.slice(0, 1500) };
    release.click();
    return { found: true };
  })()` })
  if (releaseButton.exceptionDetails || !releaseButton.result.value.found) {
    throw new Error(`No se encontró la liberación de orden. Pantalla: ${releaseButton.result.value?.body ?? 'sin contenido'}`)
  }
  await delay(200)
  const orderGuard = await send('Runtime.evaluate', { returnByValue: true, expression: `(() => {
    const form = document.querySelector('.production-orders form');
    if (!form || form.checkValidity()) throw new Error('La orden vacía debe ser inválida');
    return true;
  })()` })
  if (orderGuard.exceptionDetails) throw new Error('Falló validación de liberación de orden')
  const orderEditor = await inspectLayout('Liberación de orden')
  await send('Runtime.evaluate', {
    expression: `([...document.querySelectorAll('button')].find(button => button.textContent.trim() === 'Cancelar'))?.click()`,
  })
  await delay(300)
  const stageGuard = await send('Runtime.evaluate', { returnByValue: true, expression: `(() => {
    const panel = document.querySelector('.production-stage-panel');
    if (!panel) throw new Error('No se encontró el ejecutor del batch record');
    if (!panel.textContent.includes('Armar kit y realizar pesajes')) throw new Error('La primera etapa ejecutable no es el kit de pesajes');
    if (![...panel.querySelectorAll('button')].some(button => button.textContent.includes('Iniciar'))) {
      throw new Error('Falta la acción para iniciar la etapa');
    }
    panel.scrollIntoView({ block: 'start' });
    return true;
  })()` })
  if (stageGuard.exceptionDetails) throw new Error('Falló el ejecutor previo a fermentación')
  const stageExecution = await inspectLayout('Batch record previo a fermentación')
  if (process.env.QA_SCREENSHOT) {
    const capture = await send('Page.captureScreenshot', {format: 'png', captureBeyondViewport: false})
    await writeFile(process.env.QA_SCREENSHOT, Buffer.from(capture.data, 'base64'))
  }
  await send('Runtime.evaluate', {
    expression: `([...document.querySelectorAll('button')].find(button => button.textContent.includes('Fermentación'))).click()`,
  })
  await delay(1200)
  const fermentation = await inspectLayout('Fermentación')
  const fermentationGuard = await send('Runtime.evaluate', { returnByValue: true, expression: `(() => {
    if (document.body.textContent.includes('Nuevo lote')) throw new Error('Fermentación todavía permite crear lotes');
    if (document.body.textContent.includes('Cerrar lote')) throw new Error('Fermentación todavía permite cerrar lotes');
    if (!document.body.textContent.includes('Lotes listos para fermentación')) throw new Error('Falta la bandeja de transferencia');
    return true;
  })()` })
  if (fermentationGuard.exceptionDetails) throw new Error('Falló separación Producción/Fermentación')
  await send('Emulation.setDeviceMetricsOverride', {width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false})
  await delay(200)
  const desktop = await inspectLayout('Fermentación escritorio')
  await send('Runtime.evaluate', {
    expression: `([...document.querySelectorAll('nav button')].find(button => button.textContent.includes('Producción'))).click()`,
  })
  await delay(300)
  await send('Runtime.evaluate', {
    expression: `([...document.querySelectorAll('button')].find(button => button.textContent.includes('Vista general'))).click()`,
  })
  await delay(600)
  const productionDesktop = await inspectLayout('Producción escritorio')
  const desktopStageGuard = await send('Runtime.evaluate', { returnByValue: true, expression: `Boolean(document.querySelector('.production-stage-panel'))` })
  if (!desktopStageGuard.result.value) throw new Error('Falta el batch record en Producción escritorio')
  const results = [login, plant, warehouseEditor, inventory, inventoryEditor, production, orderEditor, stageExecution, fermentation, desktop, productionDesktop]
  console.log(JSON.stringify(results, null, 2))
  socket.close()
  if (results.some(result => result.documentWidth > result.viewportWidth || result.offenders.length > 0)) process.exitCode = 1
} finally {
  browser.kill()
  await delay(300)
  const safeProfile = resolve(profile)
  if (safeProfile.startsWith(resolve(tmpdir()))) await rm(safeProfile, { recursive: true, force: true }).catch(() => {})
}

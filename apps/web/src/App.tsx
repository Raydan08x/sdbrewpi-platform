import { useCallback, useEffect, useState } from 'react'
import { Activity, AlertTriangle, BarChart3, BatteryMedium, Beer, Boxes, Building2, CalendarClock, ChevronRight, CircleGauge, Factory, FlaskConical, Gauge, ListChecks, PackageSearch, Radio, RefreshCw, Signal, Snowflake, Users } from 'lucide-react'
import { getOverview, getProductionOverview, setMode, setSetpoint } from './api'
import type { Batch, ControlMode, Overview, ProductionOverview, Tank } from './types'

const modules = [
  { label: 'Fermentación', icon: FlaskConical, active: true },
  { label: 'Producción', icon: Factory },
  { label: 'Recetas', icon: Beer },
  { label: 'Inventarios', icon: Boxes },
  { label: 'Planeación MRP', icon: BarChart3 },
  { label: 'Compras', icon: PackageSearch },
  { label: 'Clientes CRM', icon: Users },
  { label: 'Administración', icon: Building2 },
]

function TankCard({ tank, batch, busy, onMode, onSetpoint }: { tank: Tank; batch?: Batch; busy: boolean; onMode: (tank: Tank, mode: ControlMode) => void; onSetpoint: (tank: Tank, value: number) => void }) {
  const [draft, setDraft] = useState(tank.setpointC.toFixed(1))
  const delta = tank.productTemperatureC - tank.setpointC
  const fill = Math.max(18, Math.min(92, 48 + delta * 10))
  return <article className="tank-card">
    <div className="card-heading">
      <div><span className="eyebrow">{tank.id}</span><h2>{tank.name}</h2></div>
      <span className={'status ' + (tank.coolingDemand ? 'cooling' : tank.mode.toLowerCase())}><i />{tank.coolingDemand ? 'ENFRIANDO' : tank.mode}</span>
    </div>
    <div className="tank-body">
      <div className="vessel" aria-label={'Temperatura ' + tank.productTemperatureC.toFixed(1) + ' grados'}>
        <div className="vessel-top" />
        <div className="vessel-shell"><div className="liquid" style={{ height: fill + '%' }} /></div>
        <div className="vessel-cone" /><div className="vessel-leg left" /><div className="vessel-leg right" />
        {tank.coolingDemand && <div className="cool-ring"><Snowflake size={17}/></div>}
      </div>
      <div className="readings">
        <div className="primary-reading"><small>TEMP. PRODUCTO</small><strong>{tank.productTemperatureC.toFixed(1)}<sup>°C</sup></strong><span className={delta > .35 ? 'warm' : 'stable'}>{delta > .35 ? `+${delta.toFixed(1)} °C sobre objetivo` : 'Dentro del rango'}</span></div>
        <div className="reading-grid">
          <div><small>DENSIDAD</small><b>{tank.gravity.toFixed(4)}</b><span>SG</span></div>
          <div><small>PILL</small><b>{tank.pillQuality}</b><span>{tank.pillSource} · hace {tank.pillAgeSeconds}s</span></div>
        </div>
        {(tank.pillBatteryPct !== null || tank.pillRssiDbm !== null) && <div className="sensor-meta">
          <span><BatteryMedium size={13}/>{tank.pillBatteryPct ?? '—'}%</span><span><Signal size={13}/>{tank.pillRssiDbm ?? '—'} dBm</span>
        </div>}
      </div>
    </div>
    <div className={'batch-strip ' + (batch ? 'assigned' : 'empty')}>
      <ListChecks size={15}/><div><small>LOTE ACTIVO</small><b>{batch?.code ?? 'Sin lote asignado'}</b></div>
      {batch && <div className="batch-recipe"><span>{batch.recipeName} · v{batch.recipeVersion}</span><small>{batch.volumeL.toFixed(0)} L · fase {batch.currentStep}/{batch.profile.length}</small></div>}
    </div>
    <div className="control-strip">
      <label>Objetivo °C <input type="number" min="0" max="35" step="0.1" value={draft} disabled={busy} onChange={e => setDraft(e.target.value)} /></label>
      <button disabled={busy || Number(draft) === tank.setpointC} onClick={() => onSetpoint(tank, Number(draft))}>Aplicar</button>
      <div className="mode-control" aria-label="Modo de control">
        {(['OFF','MANUAL','AUTO'] as ControlMode[]).map(mode => <button key={mode} className={tank.mode === mode ? 'selected' : ''} disabled={busy || tank.mode === mode} onClick={() => onMode(tank, mode)}>{mode}</button>)}
      </div>
    </div>
  </article>
}

export default function App() {
  const [overview, setOverview] = useState<Overview | null>(null)
  const [production, setProduction] = useState<ProductionOverview | null>(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState('')
  const load = useCallback(async () => {
    try {
      const [fermentation, productionContext] = await Promise.all([getOverview(), getProductionOverview()])
      setOverview(fermentation); setProduction(productionContext); setError('')
    } catch (e) { setError(e instanceof Error ? e.message : 'API no disponible') }
  }, [])
  useEffect(() => {
    const initial = window.setTimeout(load, 0)
    const interval = window.setInterval(load, 2500)
    return () => { window.clearTimeout(initial); window.clearInterval(interval) }
  }, [load])

  const command = async (id: string, action: () => Promise<unknown>) => {
    setBusy(id); setNotice('');
    try { await action(); setError(''); setNotice('Orden aceptada y registrada en simulación'); await load() }
    catch (e) { setError(e instanceof Error ? e.message : 'Error al ejecutar la orden') }
    finally { setBusy('') }
  }

  return <div className="app-shell">
    <aside>
      <div className="brand"><div className="brand-mark"><Beer size={24}/></div><div><b>SDBrewPi</b><span>BREWERY OS</span></div></div>
      <nav>{modules.map(({label,icon:Icon,active}) => <button key={label} className={active ? 'active' : ''} disabled={!active}><Icon size={19}/><span>{label}</span>{active ? <ChevronRight size={16}/> : <small>Próximamente</small>}</button>)}</nav>
      <div className="system-card"><span className="live-dot"/>Sistema local<b>Modo simulación</b><small>Hardware bloqueado</small></div>
    </aside>
    <main>
      <header><div><span className="eyebrow">CONTROL DE PROCESO</span><h1>Fermentación</h1><p>Dos tanques · circuito de frío compartido</p></div><button className="refresh" onClick={load}><RefreshCw size={17}/> Actualizar</button></header>
      <div className="simulation-banner"><AlertTriangle size={20}/><div><b>{overview?.environment === 'LIVE_READ_ONLY' ? 'Telemetría real en modo de solo lectura' : 'Entorno de simulación'}</b><span>Ninguna salida física está habilitada.</span></div></div>
      {error && <div className="message error">{error}</div>}{notice && <div className="message success">{notice}</div>}
      {!overview ? <div className="loading"><RefreshCw className="spin"/>Conectando con el backend…</div> : <>
        <section className={'telemetry-state ' + (overview.telemetry.connected ? 'online' : overview.telemetry.enabled ? 'waiting' : 'disabled')}>
          <Radio size={18}/><div><b>MQTT</b><span>{overview.telemetry.detail}</span></div>
          <small>{overview.telemetry.acceptedMessages} aceptados · {overview.telemetry.rejectedMessages} rechazados</small>
        </section>
        <section className="metrics">
          <div><span><Activity size={18}/>Tanques activos</span><b>{overview.tanks.filter(t => t.mode !== 'OFF').length}<small>/ {overview.tanks.length}</small></b></div>
          <div><span><Snowflake size={18}/>Demandas de frío</span><b>{overview.tanks.filter(t => t.coolingDemand).length}</b></div>
          <div><span><Gauge size={18}/>Bomba</span><b className={overview.chiller.pumpOn ? 'cyan' : ''}>{overview.chiller.pumpOn ? 'ON' : 'OFF'}</b></div>
          <div><span><CircleGauge size={18}/>Depósito</span><b className="muted">Sin sensor</b></div>
        </section>
        <section className="section-title"><div><span className="line"/><h3>FERMENTADORES</h3></div><small>Actualizado {new Date(overview.generatedAt).toLocaleTimeString('es-CO')}</small></section>
        <section className="tank-grid">{overview.tanks.map(tank => <TankCard key={`${tank.id}-${tank.setpointC}`} tank={tank} batch={production?.activeBatches.find(batch => batch.tankId === tank.id)} busy={busy === tank.id} onMode={(t,m) => command(t.id, () => setMode(t,m))} onSetpoint={(t,v) => command(t.id, () => setSetpoint(t,v))}/>)}</section>
        {production && <section className="profile-panel">
          <div className="profile-heading"><div><span className="eyebrow">CONTEXTO DE PRODUCCIÓN</span><h3>Perfiles de fermentación</h3><p>Versiones inmutables para que cada lote conserve el proceso con el que inició.</p></div><span className="demo-badge">DATOS DE DEMOSTRACIÓN</span></div>
          <div className="profile-grid">{production.recipes.map(recipe => <article key={recipe.id}>
            <div className="recipe-title"><div><small>{recipe.code} · V{recipe.version}</small><b>{recipe.name}</b></div><span>{recipe.defaultVolumeL.toFixed(0)} L</span></div>
            <div className="gravity-range"><span>OG <b>{recipe.originalGravity.toFixed(3)}</b></span><i/><span>FG <b>{recipe.targetFinalGravity.toFixed(3)}</b></span></div>
            <ol>{recipe.steps.map(step => <li key={step.order}><span>{step.order}</span><div><b>{step.name}</b><small><CalendarClock size={12}/>{step.durationHours} h · {step.targetTemperatureC.toFixed(1)} °C</small></div></li>)}</ol>
          </article>)}</div>
        </section>}
        <section className="chiller-panel">
          <div className="chiller-icon"><Snowflake size={28}/></div><div><span className="eyebrow">SISTEMA COMPARTIDO</span><h3>Chiller y circulación</h3><p>La temperatura del depósito permanece inhibida hasta instalar y validar su sensor.</p></div>
          <div className="chiller-state"><span>BOMBA <b>{overview.chiller.pumpOn ? 'ENCENDIDA' : 'APAGADA'}</b></span><span>COMPRESOR <b>{overview.chiller.compressorRequest ? 'SOLICITADO' : 'SIN DEMANDA'}</b></span><span>CONTROL <b>{overview.chiller.environment}</b></span></div>
        </section>
      </>}
    </main>
  </div>
}

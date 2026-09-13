import { useCallback, useEffect, useMemo, useState } from 'react'
import { Activity, AlertTriangle, ArrowRight, CalendarClock, CircleGauge, Factory, Gauge, Layers3, ListChecks, PackageCheck, Radio, RefreshCw, Scale, Signal, Snowflake } from 'lucide-react'
import { getOverview, getProductionOverview, setMode, setSetpoint } from './api'
import type { Batch, ControlMode, Overview, ProductionOverview, ProductionStage, Tank } from './types'

const phaseLabels: Record<string, { title: string; description: string }> = {
  PREPARATION: { title: 'Preparación', description: 'Orden, pesajes, limpieza, agua y molienda' },
  MASHING: { title: 'Maceración y separación', description: 'Empaste, descansos, recirculado y lavado' },
  BOILING: { title: 'Cocción', description: 'Hervor, adiciones y whirlpool' },
  COLD_SIDE: { title: 'Lado frío', description: 'Enfriado, transferencia, oxígeno y levadura' },
  CELLAR: { title: 'Bodega fría', description: 'Fermentación, maduración, filtración y gas' },
  PACKAGING: { title: 'Envasado', description: 'Barril, botella o lata según la orden' },
  CLOSEOUT: { title: 'Cierre', description: 'Liberación, inventario terminado y limpieza' },
}

function TankCard({ tank, batch, busy, onMode, onSetpoint }: { tank: Tank; batch?: Batch; busy: boolean; onMode: (tank: Tank, mode: ControlMode) => void; onSetpoint: (tank: Tank, value: number) => void }) {
  const [draft, setDraft] = useState(tank.setpointC)
  const state = tank.mode === 'OFF' ? 'DETENIDO' : tank.coolingDemand ? 'ENFRIANDO' : 'ESTABLE'
  return <article className="tank-card">
    <div className="card-heading"><div><span className="eyebrow">{tank.id}</span><h2>{tank.name}</h2></div><span className={'status ' + (tank.coolingDemand ? 'cooling' : tank.mode.toLowerCase())}><i/>{state}</span></div>
    <div className="tank-body"><div className="vessel"><div className="vessel-top"/><div className="vessel-shell"><div className="liquid" style={{height:'68%'}}/></div><div className="vessel-cone"/><div className="vessel-leg left"/><div className="vessel-leg right"/>{tank.coolingDemand && <div className="cool-ring"><Snowflake size={19}/></div>}</div>
      <div className="readings"><div className="primary-reading"><small>TEMP. PRODUCTO</small><strong>{tank.productTemperatureC.toFixed(1)}<sup>°C</sup></strong><span className={Math.abs(tank.productTemperatureC-tank.setpointC)>.5?'warm':'stable'}>{Math.abs(tank.productTemperatureC-tank.setpointC)>.5?'Fuera del objetivo':'Dentro del rango'}</span></div><div className="reading-grid"><div><small>DENSIDAD</small><b>{tank.gravity.toFixed(4)}</b><span>SG</span></div><div><small>PILL</small><b>{tank.pillQuality}</b><span>{tank.pillId}</span></div></div></div>
    </div>
    <div className={'telemetry-state ' + (tank.pillSource === 'MQTT' ? 'online' : 'disabled')}><Signal size={14}/><div><b>{tank.pillSource}</b><span>{tank.pillReceivedAt ? `recibido hace ${tank.pillAgeSeconds} s` : `muestra simulada · hace ${tank.pillAgeSeconds} s`}</span></div><small>{tank.pillBatteryPct === null ? 'batería pendiente' : `${tank.pillBatteryPct.toFixed(0)}% batería`}{tank.pillRssiDbm === null ? '' : ` · ${tank.pillRssiDbm} dBm`}</small></div>
    <div className={'batch-strip ' + (batch ? 'assigned' : '')}><ListChecks size={15}/><div><small>LOTE ACTIVO</small><b>{batch?.code ?? 'Sin lote asignado'}</b></div>{batch && <div className="batch-recipe"><span>{batch.recipeName} · v{batch.recipeVersion}</span><small>{batch.volumeL.toFixed(0)} L · fase {batch.currentStep}/{batch.profile.length}</small></div>}</div>
    <div className="control-strip"><label>Objetivo °C<input type="number" step="0.1" value={draft} onChange={event=>setDraft(Number(event.target.value))}/></label><button disabled={busy || draft===tank.setpointC} onClick={()=>onSetpoint(tank,draft)}>Aplicar</button><div className="mode-control">{(['OFF','MANUAL','AUTO'] as ControlMode[]).map(mode=><button key={mode} disabled={busy || mode==='AUTO'} className={tank.mode===mode?'selected':''} onClick={()=>onMode(tank,mode)}>{mode}</button>)}</div></div>
  </article>
}

function ProcessMap({ stages, hasActiveBatch, onOpenFermentation }: { stages: ProductionStage[]; hasActiveBatch: boolean; onOpenFermentation: () => void }) {
  const groups = useMemo(() => {
    const grouped = new Map<string, ProductionStage[]>()
    for (const stage of stages) grouped.set(stage.phase, [...(grouped.get(stage.phase) ?? []), stage])
    return [...grouped.entries()]
  }, [stages])
  return <section className="process-map">
    <div className="section-title"><div><span className="line"/><h3>RUTA COMPLETA DEL LOTE</h3></div><small>Catálogo base configurable por receta y orden</small></div>
    <div className="phase-grid">{groups.map(([phase, phaseStages], phaseIndex) => <article className="process-phase" key={phase}>
      <div className="phase-heading"><span>{String(phaseIndex + 1).padStart(2, '0')}</span><div><h2>{phaseLabels[phase]?.title ?? phase}</h2><p>{phaseLabels[phase]?.description}</p></div></div>
      <ol>{phaseStages.map(stage => {
        const fermentation = stage.code === 'FERMENTATION'
        return <li key={stage.code} className={fermentation && hasActiveBatch ? 'active-stage' : ''}>
          <span className="step-index">{stages.findIndex(item => item.code === stage.code) + 1}</span><div><b>{stage.name}</b><p>{stage.description}</p></div>
          <span className={'stage-kind ' + (stage.variant !== 'ALL' ? 'variant' : '')}>{stage.variant !== 'ALL' ? stage.variant : stage.optional ? 'Opcional' : 'Base'}</span>
          {fermentation && <button type="button" onClick={onOpenFermentation}>Abrir control <ArrowRight size={13}/></button>}
        </li>
      })}</ol>
    </article>)}</div>
  </section>
}

export default function ProductionPage() {
  const [section, setSection] = useState<'overview' | 'fermentation'>('overview')
  const [overview, setOverview] = useState<Overview | null>(null)
  const [production, setProduction] = useState<ProductionOverview | null>(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState('')
  const load = useCallback(async () => {
    try {
      const [nextOverview, nextProduction] = await Promise.all([getOverview(), getProductionOverview()])
      setOverview(nextOverview); setProduction(nextProduction); setError('')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'API no disponible') }
  }, [])
  useEffect(() => {
    const initial = window.setTimeout(load, 0)
    const interval = section === 'fermentation' ? window.setInterval(load, 2500) : undefined
    return () => { window.clearTimeout(initial); if (interval) window.clearInterval(interval) }
  }, [load, section])

  const command = async (id: string, action: () => Promise<unknown>) => {
    setBusy(id); setNotice('')
    try { await action(); setNotice('Orden aceptada en simulación'); await load() }
    catch (caught) { setError(caught instanceof Error ? caught.message : 'Error enviando orden') }
    finally { setBusy('') }
  }

  return <main className="production-page">
    <header><div><span className="eyebrow">OPERACIÓN DE CERVECERÍA</span><h1>Producción</h1><p>Ruta completa del lote, desde el kit de pesajes hasta producto terminado</p></div><button className="refresh" onClick={load}><RefreshCw size={17}/> Actualizar</button></header>
    <div className="production-tabs"><button className={section === 'overview' ? 'selected' : ''} onClick={() => setSection('overview')}><Factory size={16}/>Vista general</button><button className={section === 'fermentation' ? 'selected' : ''} onClick={() => setSection('fermentation')}><Snowflake size={16}/>Fermentación</button></div>
    {error && <div className="message error">{error}</div>}{notice && <div className="message success">{notice}</div>}
    {!overview || !production ? <div className="loading"><RefreshCw className="spin"/>Cargando producción…</div> : section === 'overview' ? <>
      <div className="simulation-banner"><AlertTriangle size={20}/><div><b>Mapa operativo en preparación</b><span>Las etapas están registradas; su ejecución y sus consumos todavía no envían órdenes a equipos.</span></div></div>
      <section className="production-metrics">
        <div><Layers3/><span>Etapas registradas<b>{production.processStages.length}</b></span></div>
        <div><Activity/><span>Lotes activos<b>{production.activeBatches.length}</b></span></div>
        <div><Scale/><span>Pasos opcionales<b>{production.processStages.filter(stage => stage.optional).length}</b></span></div>
        <div><PackageCheck/><span>Formatos de envasado<b>3</b></span></div>
      </section>
      <section className="active-production"><div className="section-title"><div><span className="line"/><h3>LOTES EN CURSO</h3></div><small>Datos de simulación</small></div><div className="active-batch-grid">{production.activeBatches.map(batch => <article key={batch.id}><div><small>{batch.code}</small><b>{batch.recipeName}</b></div><span>{batch.volumeL.toFixed(0)} L</span><button onClick={() => setSection('fermentation')}>Fermentación <ArrowRight size={13}/></button></article>)}</div></section>
      <ProcessMap stages={production.processStages} hasActiveBatch={production.activeBatches.length > 0} onOpenFermentation={() => setSection('fermentation')}/>
    </> : <>
      <div className="simulation-banner"><AlertTriangle size={20}/><div><b>{overview.environment === 'LIVE_READ_ONLY' ? 'Telemetría real en modo de solo lectura' : 'Entorno de simulación'}</b><span>Ninguna salida física está habilitada.</span></div></div>
      <section className={'telemetry-state ' + (overview.telemetry.connected ? 'online' : overview.telemetry.enabled ? 'waiting' : 'disabled')}><Radio size={18}/><div><b>MQTT</b><span>{overview.telemetry.detail}</span></div><small>{overview.telemetry.acceptedMessages} aceptados · {overview.telemetry.rejectedMessages} rechazados</small></section>
      <section className="metrics"><div><span><Activity size={18}/>Tanques activos</span><b>{overview.tanks.filter(t => t.mode !== 'OFF').length}<small>/ {overview.tanks.length}</small></b></div><div><span><Snowflake size={18}/>Demandas de frío</span><b>{overview.tanks.filter(t => t.coolingDemand).length}</b></div><div><span><Gauge size={18}/>Bomba</span><b className={overview.chiller.pumpOn ? 'cyan' : ''}>{overview.chiller.pumpOn ? 'ON' : 'OFF'}</b></div><div><span><CircleGauge size={18}/>Depósito</span><b className="muted">Sin sensor</b></div></section>
      <section className="section-title"><div><span className="line"/><h3>FERMENTADORES</h3></div><small>Actualizado {new Date(overview.generatedAt).toLocaleTimeString('es-CO')}</small></section>
      <section className="tank-grid">{overview.tanks.map(tank => <TankCard key={`${tank.id}-${tank.setpointC}`} tank={tank} batch={production.activeBatches.find(batch => batch.tankId === tank.id)} busy={busy === tank.id} onMode={(t,m) => command(t.id, () => setMode(t,m))} onSetpoint={(t,v) => command(t.id, () => setSetpoint(t,v))}/>)}</section>
      <section className="profile-panel"><div className="profile-heading"><div><span className="eyebrow">CONTEXTO DE PRODUCCIÓN</span><h3>Perfiles de fermentación</h3><p>Versiones inmutables para que cada lote conserve el proceso con el que inició.</p></div><span className="demo-badge">DATOS DE DEMOSTRACIÓN</span></div><div className="profile-grid">{production.recipes.map(recipe => <article key={recipe.id}><div className="recipe-title"><div><small>{recipe.code} · V{recipe.version}</small><b>{recipe.name}</b></div><span>{recipe.defaultVolumeL.toFixed(0)} L</span></div><div className="gravity-range"><span>OG <b>{recipe.originalGravity.toFixed(3)}</b></span><i/><span>FG <b>{recipe.targetFinalGravity.toFixed(3)}</b></span></div><ol>{recipe.steps.map(step => <li key={step.order}><span>{step.order}</span><div><b>{step.name}</b><small><CalendarClock size={12}/>{step.durationHours} h · {step.targetTemperatureC.toFixed(1)} °C</small></div></li>)}</ol></article>)}</div></section>
      <section className="chiller-panel"><div className="chiller-icon"><Snowflake size={28}/></div><div><span className="eyebrow">SISTEMA COMPARTIDO</span><h3>Chiller y circulación</h3><p>La temperatura del depósito permanece inhibida hasta instalar y validar su sensor.</p></div><div className="chiller-state"><span>BOMBA <b>{overview.chiller.pumpOn ? 'ENCENDIDA' : 'APAGADA'}</b></span><span>COMPRESOR <b>{overview.chiller.compressorRequest ? 'SOLICITADO' : 'SIN DEMANDA'}</b></span><span>CONTROL <b>{overview.chiller.environment}</b></span></div></section>
    </>}
  </main>
}

import { BatchManagement } from './BatchManagement'
import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { Activity, AlertTriangle, ArrowRight, CalendarClock, CheckCheck, CircleGauge, ClipboardList, Factory, Gauge, Layers3, ListChecks, PackageCheck, Pause, Play, Plus, Radio, RefreshCw, Scale, Signal, Snowflake } from 'lucide-react'
import { acknowledgeAlarm, addBatchEvent, controlProfile, getAlarmHistory, getBatchEvents, getOverview, getProductionOverview, getTankHistory, setMode, setSetpoint } from './api'
import type { AlarmHistory, Batch, BatchEvent, BatchEventInput, ControlMode, FermentationAlarm, FermentationHistory, FermentationMeasurement, Overview, ProductionOverview, ProductionStage, Tank } from './types'

const phaseLabels: Record<string, { title: string; description: string }> = {
  PREPARATION: { title: 'Preparación', description: 'Orden, pesajes, limpieza, agua y molienda' },
  MASHING: { title: 'Maceración y separación', description: 'Empaste, descansos, recirculado y lavado' },
  BOILING: { title: 'Cocción', description: 'Hervor, adiciones y whirlpool' },
  COLD_SIDE: { title: 'Lado frío', description: 'Enfriado, transferencia, oxígeno y levadura' },
  CELLAR: { title: 'Bodega fría', description: 'Fermentación, maduración, filtración y gas' },
  PACKAGING: { title: 'Envasado', description: 'Barril, botella o lata según la orden' },
  CLOSEOUT: { title: 'Cierre', description: 'Liberación, inventario terminado y limpieza' },
}

const eventLabels: Record<string, string> = {
  OPERATOR_NOTE: 'Nota del operador',
  INGREDIENT_ADDITION: 'Adición de ingrediente',
  MEASUREMENT: 'Medición manual',
  SAMPLE: 'Toma de muestra',
  DEVIATION: 'Desviación',
  QUALITY_CHECK: 'Control de calidad',
  SANITATION: 'Limpieza o sanitización',
}

function TrendChart({ samples }: { samples: FermentationMeasurement[] }) {
  const measured = samples.filter(sample => sample.temperatureC !== null || sample.gravity !== null)
  if (measured.length < 2) return <div className="trend-empty">Esperando al menos dos muestras para dibujar la curva.</div>
  const width = 600; const height = 168; const padding = 18
  const values = measured.map(sample => sample.temperatureC).filter((value): value is number => value !== null)
  const gravities = measured.map(sample => sample.gravity).filter((value): value is number => value !== null)
  const range = (items: number[], fallback: [number, number]) => {
    if (!items.length) return fallback
    const min = Math.min(...items); const max = Math.max(...items)
    return min === max ? [min - .5, max + .5] as [number, number] : [min, max] as [number, number]
  }
  const [tempMin, tempMax] = range(values, [0, 1]); const [gravityMin, gravityMax] = range(gravities, [1, 1.1])
  const points = (selector: (sample: FermentationMeasurement) => number | null, min: number, max: number) => measured
    .map((sample, index) => {
      const value = selector(sample); if (value === null) return null
      const x = padding + index * (width - padding * 2) / Math.max(1, measured.length - 1)
      const y = padding + (max - value) * (height - padding * 2) / Math.max(.0001, max - min)
      return `${x.toFixed(1)},${y.toFixed(1)}`
    }).filter(Boolean).join(' ')
  return <div className="trend-chart">
    <div className="trend-legend"><span className="temperature-key">Temperatura {values.at(-1)?.toFixed(1) ?? '—'} °C</span><span className="gravity-key">Gravedad {gravities.at(-1)?.toFixed(4) ?? '—'}</span><small>{measured.length} muestras</small></div>
    <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Curvas de temperatura y gravedad de las últimas 24 horas">
      <path className="grid-line" d={`M${padding} ${height / 2}H${width - padding}`}/>
      {values.length > 1 && <polyline className="temperature-line" points={points(sample => sample.temperatureC, tempMin, tempMax)}/>}
      {gravities.length > 1 && <polyline className="gravity-line" points={points(sample => sample.gravity, gravityMin, gravityMax)}/>}
    </svg>
    <div className="trend-range"><span>{new Date(measured[0].capturedAt).toLocaleTimeString('es-CO', {hour:'2-digit', minute:'2-digit'})}</span><span>{new Date(measured.at(-1)!.capturedAt).toLocaleTimeString('es-CO', {hour:'2-digit', minute:'2-digit'})}</span></div>
  </div>
}

function TankCard({ tank, batch, history, busy, onMode, onSetpoint }: { tank: Tank; batch?: Batch; history?: FermentationHistory; busy: boolean; onMode: (tank: Tank, mode: ControlMode) => void; onSetpoint: (tank: Tank, value: number) => void }) {
  const [draft, setDraft] = useState(tank.setpointC)
  const state = tank.mode === 'OFF' ? 'DETENIDO' : tank.coolingDemand ? 'ENFRIANDO' : 'ESTABLE'
  const sourceState = tank.pillSource === 'SIMULATION' ? 'disabled' : tank.pillQuality === 'GOOD' ? 'online' : 'waiting'
  const profileOwnsControl = batch?.profileState === 'RUNNING'
  return <article className="tank-card">
    <div className="card-heading"><div><span className="eyebrow">{tank.id}</span><h2>{tank.name}</h2></div><span className={'status ' + (tank.coolingDemand ? 'cooling' : tank.mode.toLowerCase())}><i/>{state}</span></div>
    <div className="tank-body"><div className="vessel"><div className="vessel-top"/><div className="vessel-shell"><div className="liquid" style={{height:'68%'}}/></div><div className="vessel-cone"/><div className="vessel-leg left"/><div className="vessel-leg right"/>{tank.coolingDemand && <div className="cool-ring"><Snowflake size={19}/></div>}</div>
      <div className="readings"><div className="primary-reading"><small>TEMP. PRODUCTO</small><strong>{tank.productTemperatureC.toFixed(1)}<sup>°C</sup></strong><span className={Math.abs(tank.productTemperatureC-tank.setpointC)>.5?'warm':'stable'}>{Math.abs(tank.productTemperatureC-tank.setpointC)>.5?'Fuera del objetivo':'Dentro del rango'}</span></div><div className="reading-grid"><div><small>DENSIDAD</small><b>{tank.gravity.toFixed(4)}</b><span>SG</span></div><div><small>SENSOR</small><b>{tank.pillQuality}</b><span>{tank.pillSource === 'PLC_DEMO_SERIAL' ? 'PLC demo' : tank.pillId}</span></div></div></div>
    </div>
    <div className={'telemetry-state ' + sourceState}><Signal size={14}/><div><b>{tank.pillSource === 'PLC_DEMO_SERIAL' ? 'PLC demo · USB serie' : tank.pillSource}</b><span>{tank.pillReceivedAt ? `recibido hace ${tank.pillAgeSeconds} s` : `muestra simulada · hace ${tank.pillAgeSeconds} s`}</span></div><small>{tank.pillBatteryPct === null ? 'batería pendiente' : `${tank.pillBatteryPct.toFixed(0)}% batería`}{tank.pillRssiDbm === null ? '' : ` · ${tank.pillRssiDbm} dBm`}</small></div>
    <div className={'batch-strip ' + (batch ? 'assigned' : '')}><ListChecks size={15}/><div><small>LOTE ACTIVO</small><b>{batch?.code ?? 'Sin lote asignado'}</b></div>{batch && <div className="batch-recipe"><span>{batch.recipeName} · v{batch.recipeVersion}</span><small>{batch.volumeL.toFixed(0)} L · fase {batch.currentStep}/{batch.profile.length}</small></div>}</div>
    <div className="control-strip"><label>Objetivo °C<input type="number" step="0.1" value={draft} disabled={profileOwnsControl} onChange={event=>setDraft(Number(event.target.value))}/></label><button disabled={busy || profileOwnsControl || draft===tank.setpointC} onClick={()=>onSetpoint(tank,draft)}>Aplicar</button><div className="mode-control">{(['OFF','MANUAL','AUTO'] as ControlMode[]).map(mode=><button key={mode} disabled={busy || profileOwnsControl || mode==='AUTO'} className={tank.mode===mode?'selected':''} onClick={()=>onMode(tank,mode)}>{mode}</button>)}</div>{profileOwnsControl && <small className="profile-control-note">El perfil administra el objetivo. Pausa el tiempo para intervenir.</small>}{batch?.profileState === 'PAUSED' && <small className="profile-control-note paused">Intervención manual habilitada; reanudar restaura AUTO y la rampa.</small>}</div>
    <TrendChart samples={history?.samples ?? []}/>
  </article>
}

function ProfileExecutionPanel({ batches, generatedAt, busy, onAction }: { batches: Batch[]; generatedAt: string; busy: string; onAction: (batch: Batch, action: 'start' | 'pause' | 'resume') => void }) {
  if (!batches.length) return <section className="profile-execution empty"><CalendarClock size={20}/><div><b>Sin perfiles asignados</b><span>Inicia un lote en un fermentador para preparar su perfil térmico.</span></div></section>
  return <section className="profile-execution-list">{batches.map(batch => {
    const step = batch.profile[Math.max(0, batch.currentStep - 1)]
    const durationSeconds = Math.max(1, (step?.durationHours ?? 1) * 3600)
    const runningElapsed = batch.stepStartedAt ? Math.max(0, (new Date(generatedAt).getTime() - new Date(batch.stepStartedAt).getTime()) / 1000) : 0
    const elapsed = batch.profileState === 'PAUSED' ? batch.stepElapsedSeconds : runningElapsed
    const progress = batch.profileState === 'COMPLETED' ? 100 : Math.min(100, elapsed * 100 / durationSeconds)
    const label = {NOT_STARTED:'LISTO PARA INICIAR', RUNNING:'EN EJECUCIÓN', PAUSED:'PAUSADO', COMPLETED:'COMPLETADO'}[batch.profileState]
    return <article key={batch.id} className={`profile-execution ${batch.profileState.toLowerCase()}`}>
      <div className="profile-state"><span>{label}</span><b>{batch.code}</b><small>{batch.recipeName} · {batch.tankId}</small></div>
      <div className="active-step"><small>FASE {batch.currentStep}/{batch.profile.length}</small><b>{step?.name ?? 'Perfil finalizado'}</b><span>{step ? `${step.targetTemperatureC.toFixed(1)} °C · ${step.durationHours} h${step.rampRateCPerHour == null ? '' : ` · rampa ${step.rampRateCPerHour.toFixed(2)} °C/h`}` : 'Sin fase activa'}</span><i><em style={{width:`${progress}%`}}/></i></div>
      <div className="profile-actions">
        {batch.profileState === 'NOT_STARTED' && <button disabled={busy === batch.id} onClick={() => onAction(batch, 'start')}><Play size={14}/>Iniciar perfil</button>}
        {batch.profileState === 'RUNNING' && <button disabled={busy === batch.id} onClick={() => onAction(batch, 'pause')}><Pause size={14}/>Pausar tiempo</button>}
        {batch.profileState === 'PAUSED' && <button disabled={busy === batch.id} onClick={() => onAction(batch, 'resume')}><Play size={14}/>Reanudar</button>}
        {batch.profileState === 'COMPLETED' && <span>Último objetivo mantenido</span>}
      </div>
    </article>
  })}</section>
}

function AlarmCenter({ active, history, busy, onAcknowledge }: { active: FermentationAlarm[]; history: AlarmHistory | null; busy: string; onAcknowledge: (alarm: FermentationAlarm, note: string) => Promise<void> }) {
  const [notes, setNotes] = useState<Record<string, string>>({})
  const recent = history?.alarms.slice(0, 8) ?? []
  return <section className={'alarm-center ' + (active.length ? 'has-active' : '')}>
    <div className="alarm-heading"><AlertTriangle size={19}/><div><b>{active.length ? `${active.length} alarma${active.length === 1 ? '' : 's'} activa${active.length === 1 ? '' : 's'}` : 'Sin alarmas activas'}</b><span>Reconocer confirma la revisión; la alarma solo se cierra cuando desaparece su causa.</span></div></div>
    {active.length > 0 && <div className="alarm-list">{active.map(alarm => <article key={alarm.id} className={alarm.severity.toLowerCase()}>
      <span>{alarm.severity === 'CRITICAL' ? 'CRÍTICA' : 'AVISO'}</span><div><b>{alarm.targetId}</b><p>{alarm.message}</p><small>{new Date(alarm.openedAt).toLocaleString('es-CO')}</small></div>
      {alarm.acknowledgedAt ? <div className="alarm-acknowledged"><CheckCheck size={14}/><span>Revisada por {alarm.acknowledgedBy}</span></div> : <div className="alarm-actions"><input aria-label={`Nota para ${alarm.targetId}`} value={notes[alarm.id] ?? ''} maxLength={300} placeholder="Nota opcional" onChange={event => setNotes(current => ({...current, [alarm.id]: event.target.value}))}/><button disabled={busy === `alarm:${alarm.id}`} onClick={async () => { await onAcknowledge(alarm, notes[alarm.id] ?? ''); setNotes(current => ({...current, [alarm.id]: ''})) }}><CheckCheck size={13}/>Reconocer</button></div>}
    </article>)}</div>}
    <div className="alarm-history"><div><b>Historial reciente</b><small>Últimos 7 días</small></div>{recent.length ? <ol>{recent.map(alarm => <li key={alarm.id}><span className={alarm.status.toLowerCase()}>{alarm.status === 'OPEN' ? 'ACTIVA' : 'CERRADA'}</span><div><b>{alarm.targetId} · {alarm.code}</b><small>{alarm.acknowledgedAt ? `Revisada por ${alarm.acknowledgedBy}` : 'Sin reconocimiento'} · {new Date(alarm.openedAt).toLocaleString('es-CO')}</small></div></li>)}</ol> : <p>No hay alarmas registradas en este período.</p>}</div>
  </section>
}

function BatchRecordPanel({ batches, events, busy, onAdd }: { batches: Batch[]; events: Record<string, BatchEvent[]>; busy: string; onAdd: (batchId: string, values: BatchEventInput) => Promise<void> }) {
  const [selectedBatch, setSelectedBatch] = useState(batches[0]?.id ?? '')
  const [eventType, setEventType] = useState('OPERATOR_NOTE')
  const [message, setMessage] = useState('')
  const [materialName, setMaterialName] = useState('')
  const [quantity, setQuantity] = useState('')
  const [unit, setUnit] = useState('kg')
  if (!batches.length) return null
  const batchId = batches.some(batch => batch.id === selectedBatch) ? selectedBatch : batches[0].id
  const selected = batches.find(batch => batch.id === batchId)!
  const addition = eventType === 'INGREDIENT_ADDITION'
  const submit = async (event: FormEvent) => {
    event.preventDefault()
    const values: BatchEventInput = { eventType, message: message.trim() }
    if (addition) Object.assign(values, { materialName: materialName.trim(), quantity: Number(quantity), unit: unit.trim() })
    await onAdd(batchId, values)
    setMessage(''); setMaterialName(''); setQuantity('')
  }
  return <section className="batch-record">
    <div className="batch-record-heading"><div><ClipboardList size={19}/><span><b>Bitácora de fermentación</b><small>Eventos trazables del lote y sus adiciones</small></span></div><select aria-label="Lote de la bitácora" value={batchId} onChange={event => setSelectedBatch(event.target.value)}>{batches.map(batch => <option key={batch.id} value={batch.id}>{batch.code} · {batch.tankId}</option>)}</select></div>
    <div className="batch-record-body"><form onSubmit={submit}><div className="event-form-row"><label>Tipo<select value={eventType} onChange={event => setEventType(event.target.value)}>{Object.entries(eventLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label><label>Fase actual<input value={`${selected.currentStep}. ${selected.profile[selected.currentStep - 1]?.name ?? 'Perfil finalizado'}`} disabled/></label></div>
      {addition && <div className="event-form-row addition"><label>Material<input required maxLength={120} value={materialName} onChange={event => setMaterialName(event.target.value)} placeholder="Ej. Citra"/></label><label>Cantidad<input required type="number" min="0.001" step="0.001" value={quantity} onChange={event => setQuantity(event.target.value)}/></label><label>Unidad<input required maxLength={20} value={unit} onChange={event => setUnit(event.target.value)} placeholder="kg, g, L…"/></label></div>}
      <label>Descripción<textarea required maxLength={300} value={message} onChange={event => setMessage(event.target.value)} placeholder="Qué ocurrió, resultado y observaciones"/></label><button disabled={busy === `event:${batchId}` || !message.trim()}><Plus size={14}/>Registrar evento</button></form>
      <div className="event-timeline">{(events[batchId] ?? []).length ? <ol>{(events[batchId] ?? []).slice(0, 12).map(item => <li key={item.id}><i/><div><span>{eventLabels[item.eventType] ?? item.eventType} · fase {item.stepOrder ?? '—'}</span><b>{item.message}</b>{item.materialName && <small>{item.materialName} · {item.quantity} {item.unit}</small>}<small>{item.actor} · {new Date(item.occurredAt).toLocaleString('es-CO')}</small></div></li>)}</ol> : <div className="event-empty">Aún no hay eventos registrados para este lote.</div>}</div>
    </div>
  </section>
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
  const [histories, setHistories] = useState<Record<string, FermentationHistory>>({})
  const [alarmHistory, setAlarmHistory] = useState<AlarmHistory | null>(null)
  const [batchEvents, setBatchEvents] = useState<Record<string, BatchEvent[]>>({})
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState('')
  const load = useCallback(async () => {
    try {
      const [nextOverview, nextProduction] = await Promise.all([getOverview(), getProductionOverview()])
      const [nextHistories, nextAlarmHistory, nextEventLists] = await Promise.all([
        Promise.all(nextOverview.tanks.map(tank => getTankHistory(tank.id))),
        getAlarmHistory(),
        Promise.all(nextProduction.activeBatches.map(async batch => [batch.id, await getBatchEvents(batch.id)] as const)),
      ])
      setOverview(nextOverview); setProduction(nextProduction)
      setHistories(Object.fromEntries(nextHistories.map(history => [history.tankId, history])))
      setAlarmHistory(nextAlarmHistory); setBatchEvents(Object.fromEntries(nextEventLists)); setError('')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'API no disponible') }
  }, [])
  useEffect(() => {
    const initial = window.setTimeout(load, 0)
    const interval = section === 'fermentation' ? window.setInterval(load, 2500) : undefined
    return () => { window.clearTimeout(initial); if (interval) window.clearInterval(interval) }
  }, [load, section])

  const command = async (id: string, action: () => Promise<unknown>, successMessage = 'Cambio guardado; las salidas físicas siguen deshabilitadas') => {
    setBusy(id); setNotice('')
    try { await action(); setNotice(successMessage); await load() }
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
      <div className="simulation-banner"><AlertTriangle size={20}/><div><b>{overview.environment === 'PLC_DEMO_READ_ONLY' ? 'PLC demo habilitado en modo de solo lectura' : overview.environment === 'LIVE_READ_ONLY' ? 'Telemetría real en modo de solo lectura' : 'Entorno de simulación'}</b><span>Ninguna salida física está habilitada.</span></div></div>
      <section className="source-grid"><div className={'telemetry-state ' + (overview.plcDemo.connected ? 'online' : overview.plcDemo.enabled ? 'waiting' : 'disabled')}><Radio size={18}/><div><b>PLC demo · {overview.plcDemo.port}</b><span>{overview.plcDemo.detail}</span></div><small>{overview.plcDemo.acceptedMessages} muestras · {overview.plcDemo.rejectedLines} rechazadas</small></div><div className={'telemetry-state ' + (overview.telemetry.connected ? 'online' : overview.telemetry.enabled ? 'waiting' : 'disabled')}><Radio size={18}/><div><b>MQTT · Pills</b><span>{overview.telemetry.detail}</span></div><small>{overview.telemetry.acceptedMessages} aceptados · {overview.telemetry.rejectedMessages} rechazados</small></div></section>
      <AlarmCenter active={overview.alarms} history={alarmHistory} busy={busy} onAcknowledge={(alarm, note) => command(`alarm:${alarm.id}`, () => acknowledgeAlarm(alarm, note), 'Alarma reconocida y registrada en la trazabilidad')}/>
      <BatchManagement production={production} tanks={overview.tanks} onChanged={load}/>
      <ProfileExecutionPanel batches={production.activeBatches} generatedAt={overview.generatedAt} busy={busy} onAction={(batch, action) => command(batch.id, () => controlProfile(batch.id, action, batch.revision))}/>
      <BatchRecordPanel batches={production.activeBatches} events={batchEvents} busy={busy} onAdd={(batchId, values) => command(`event:${batchId}`, () => addBatchEvent(batchId, values), 'Evento agregado a la bitácora del lote')}/>
      <section className="metrics"><div><span><Activity size={18}/>Tanques activos</span><b>{overview.tanks.filter(t => t.mode !== 'OFF').length}<small>/ {overview.tanks.length}</small></b></div><div><span><Snowflake size={18}/>Demandas de frío</span><b>{overview.tanks.filter(t => t.coolingDemand).length}</b></div><div><span><Gauge size={18}/>Bomba</span><b className={overview.chiller.pumpOn ? 'cyan' : ''}>{overview.chiller.pumpOn ? 'ON' : 'OFF'}</b></div><div><span><CircleGauge size={18}/>Depósito</span><b className="muted">Sin sensor</b></div></section>
      <section className="section-title"><div><span className="line"/><h3>FERMENTADORES</h3></div><small>Actualizado {new Date(overview.generatedAt).toLocaleTimeString('es-CO')}</small></section>
      <section className="tank-grid">{overview.tanks.map(tank => <TankCard key={`${tank.id}-${tank.setpointC}`} tank={tank} batch={production.activeBatches.find(batch => batch.tankId === tank.id)} history={histories[tank.id]} busy={busy === tank.id} onMode={(t,m) => command(t.id, () => setMode(t,m))} onSetpoint={(t,v) => command(t.id, () => setSetpoint(t,v))}/>)}</section>
      <section className="profile-panel"><div className="profile-heading"><div><span className="eyebrow">CONTEXTO DE PRODUCCIÓN</span><h3>Perfiles de fermentación</h3><p>Versiones inmutables para que cada lote conserve el proceso con el que inició.</p></div><span className="demo-badge">DATOS DE DEMOSTRACIÓN</span></div><div className="profile-grid">{production.recipes.map(recipe => <article key={recipe.id}><div className="recipe-title"><div><small>{recipe.code} · V{recipe.version}</small><b>{recipe.name}</b></div><span>{recipe.defaultVolumeL.toFixed(0)} L</span></div><div className="gravity-range"><span>OG <b>{recipe.originalGravity.toFixed(3)}</b></span><i/><span>FG <b>{recipe.targetFinalGravity.toFixed(3)}</b></span></div><ol>{recipe.steps.map(step => <li key={step.order}><span>{step.order}</span><div><b>{step.name}</b><small><CalendarClock size={12}/>{step.durationHours} h · {step.targetTemperatureC.toFixed(1)} °C{step.rampRateCPerHour == null ? '' : ` · ${step.rampRateCPerHour.toFixed(2)} °C/h`}</small></div></li>)}</ol></article>)}</div></section>
      <section className="chiller-panel"><div className="chiller-icon"><Snowflake size={28}/></div><div><span className="eyebrow">SISTEMA COMPARTIDO</span><h3>Chiller y circulación</h3><p>La temperatura del depósito permanece inhibida hasta instalar y validar su sensor.{overview.environment === 'PLC_DEMO_READ_ONLY' ? ' Los estados siguientes son reportados por la demo.' : ''}</p></div><div className="chiller-state"><span>BOMBA <b>{overview.chiller.pumpOn ? 'ENCENDIDA' : 'APAGADA'}</b></span><span>COMPRESOR <b>{overview.chiller.compressorRequest ? 'SOLICITADO' : 'SIN DEMANDA'}</b></span><span>CONTROL <b>{overview.chiller.environment}</b></span></div></section>
    </>}
  </main>
}


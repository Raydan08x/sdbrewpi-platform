import { useCallback, useEffect, useMemo, useState } from 'react'
import { AlertTriangle, Cable, Check, CircleGauge, Cpu, Factory, FlaskConical, HardDriveDownload, RefreshCw, Save, Snowflake, Waves } from 'lucide-react'
import { getPlantOverview, updatePlantProfile } from './api'
import type { PlantAsset, PlantOverview, PlantProfile } from './types'

type Draft = Omit<PlantProfile, 'id' | 'code' | 'revision' | 'updatedAt'>

const typeLabels: Record<string, string> = {
  CHILLER: 'Sistema de frío', PUMP: 'Bombas', VFD: 'Variadores', POWER: 'Potencia',
  FERMENTER: 'Fermentadores', CONTROLLER: 'Controladores', HMI: 'Pantallas',
  RELAY_MODULE: 'Módulos de salida', SENSOR_GATEWAY: 'Repetidores', SENSOR: 'Sensores',
}

function AssetCard({ asset }: { asset: PlantAsset }) {
  return <article className="asset-card">
    <div className="asset-title"><div><small>{asset.code}</small><b>{asset.name}</b></div><span className={'asset-status ' + asset.status.toLowerCase()}>{asset.status.replace('_', ' ')}</span></div>
    <dl><div><dt>FABRICANTE / MODELO</dt><dd>{asset.manufacturer} · {asset.model}</dd></div>
      {asset.capacityL !== null && <div><dt>CAPACIDAD</dt><dd>{asset.capacityL.toFixed(0)} L</dd></div>}
      <div><dt>CONEXIÓN</dt><dd>{asset.communicationProtocol || 'Pendiente'}</dd></div>
      <div><dt>ELÉCTRICO</dt><dd>{asset.electricalSpec || 'Pendiente'}</dd></div></dl>
    <p>{asset.notes}</p>
  </article>
}

export default function PlantPage() {
  const [overview, setOverview] = useState<PlantOverview | null>(null)
  const [draft, setDraft] = useState<Draft | null>(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [saving, setSaving] = useState(false)
  const load = useCallback(async () => {
    try {
      const next = await getPlantOverview(); setOverview(next)
      setDraft({
        name: next.site.name, companyName: next.site.companyName, legalName: next.site.legalName,
        taxId: next.site.taxId, timezone: next.site.timezone, currency: next.site.currency,
        nominalBatchCapacityL: next.site.nominalBatchCapacityL, plannedFermenters: next.site.plannedFermenters,
        pipingDeadVolumeL: next.site.pipingDeadVolumeL, logoUrl: next.site.logoUrl,
      })
      setError('')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No se pudo cargar la planta') }
  }, [])
  useEffect(() => {
    const initial = window.setTimeout(load, 0)
    return () => window.clearTimeout(initial)
  }, [load])

  const groups = useMemo(() => {
    const result = new Map<string, PlantAsset[]>()
    for (const asset of overview?.assets ?? []) result.set(asset.assetType, [...(result.get(asset.assetType) ?? []), asset])
    return [...result.entries()]
  }, [overview])

  const set = <K extends keyof Draft>(key: K, value: Draft[K]) => setDraft(current => current ? { ...current, [key]: value } : current)
  const save = async () => {
    if (!overview || !draft) return
    setSaving(true); setNotice('')
    try {
      const saved = await updatePlantProfile(overview.site, draft)
      setOverview(current => current ? { ...current, site: saved } : current)
      setNotice('Configuración de planta guardada y registrada en la bitácora')
      setError('')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No se pudo guardar la planta') }
    finally { setSaving(false) }
  }

  return <main className="plant-page">
    <header><div><span className="eyebrow">CONFIGURACIÓN CENTRAL</span><h1>Mi Planta</h1><p>Identidad, capacidades, equipos y preparación de dispositivos</p></div><button className="refresh" onClick={load}><RefreshCw size={17}/> Actualizar</button></header>
    {error && <div className="message error">{error}</div>}{notice && <div className="message success">{notice}</div>}
    {!overview || !draft ? <div className="loading"><RefreshCw className="spin"/>Cargando inventario técnico…</div> : <>
      <section className="plant-metrics">
        <div><Factory/><span>Activos registrados<b>{overview.assets.length}</b></span></div>
        <div><FlaskConical/><span>Fermentadores previstos<b>{overview.site.plannedFermenters}</b></span></div>
        <div><Snowflake/><span>Reserva de frío<b>{overview.assets.find(asset => asset.assetType === 'CHILLER')?.capacityL?.toFixed(0) ?? 'Pendiente'} <small>L</small></b></span></div>
        <div><AlertTriangle/><span>Datos pendientes<b>{overview.assetsNeedingData}</b></span></div>
      </section>

      <section className="plant-layout">
        <form className="plant-profile" onSubmit={event => { event.preventDefault(); void save() }}>
          <div className="panel-heading"><div><span className="eyebrow">SEDE {overview.site.code}</span><h2>Identidad y capacidad</h2></div><div className="plant-logo">{draft.logoUrl ? <img src={draft.logoUrl} alt="Logo de la empresa"/> : <Factory/>}</div></div>
          <div className="form-grid">
            <label>Nombre comercial<input value={draft.companyName} maxLength={160} onChange={event => set('companyName', event.target.value)}/></label>
            <label>Nombre de la sede<input value={draft.name} maxLength={120} onChange={event => set('name', event.target.value)}/></label>
            <label>Razón social<input value={draft.legalName} maxLength={180} onChange={event => set('legalName', event.target.value)}/></label>
            <label>NIT / identificación<input value={draft.taxId} maxLength={40} onChange={event => set('taxId', event.target.value)}/></label>
            <label>Capacidad nominal por lote (L)<input type="number" min="1" step="0.1" value={draft.nominalBatchCapacityL ?? ''} onChange={event => set('nominalBatchCapacityL', event.target.value ? Number(event.target.value) : null)}/></label>
            <label>Fermentadores previstos<input type="number" min="1" max="1000" value={draft.plannedFermenters} onChange={event => set('plannedFermenters', Number(event.target.value))}/></label>
            <label>Remanente de tubería (L)<input type="number" min="0" step="0.1" value={draft.pipingDeadVolumeL} onChange={event => set('pipingDeadVolumeL', Number(event.target.value))}/></label>
            <label>Zona horaria<input value={draft.timezone} onChange={event => set('timezone', event.target.value)}/></label>
            <label>Moneda<input value={draft.currency} maxLength={3} onChange={event => set('currency', event.target.value.toUpperCase())}/></label>
            <label className="wide">URL del logo<input type="url" value={draft.logoUrl} maxLength={500} placeholder="https://…" onChange={event => set('logoUrl', event.target.value)}/></label>
          </div>
          <button className="primary-action" disabled={saving}><Save size={16}/>{saving ? 'Guardando…' : 'Guardar configuración'}</button>
        </form>

        <section className="onboarding-panel">
          <div className="panel-heading"><div><span className="eyebrow">PLUG & PLAY</span><h2>Preparación de dispositivos</h2></div><Cpu/></div>
          <div className="onboarding-state"><i/><div><b>Esperando conexión USB</b><span>{overview.onboarding.detail}</span></div></div>
          <ol>{overview.onboarding.requiredSteps.map((step, index) => <li key={step}><span>{index + 1}</span>{step}</li>)}</ol>
          <div className="firmware-profiles"><small>PERFILES PREPARADOS</small>{overview.onboarding.supportedFirmwareProfiles.map(profile => <span key={profile}><HardDriveDownload size={13}/>{profile}</span>)}</div>
          <button className="scan-action" disabled><Cable size={16}/>Buscar dispositivos</button>
          <p className="safe-note"><Check size={14}/>El registro no habilita relés, motores ni variadores.</p>
        </section>
      </section>

      <section className="asset-register">
        <div className="section-title"><div><span className="line"/><h3>INVENTARIO TÉCNICO</h3></div><small>{overview.assets.length} activos en esta sede</small></div>
        {groups.map(([type, assets]) => <div className="asset-group" key={type}><h4>{typeLabels[type] ?? type}</h4><div className="asset-grid">{assets.map(asset => <AssetCard key={asset.id} asset={asset}/>)}</div></div>)}
      </section>
      <section className="plant-calculation-note"><CircleGauge/><div><b>Base para balances y costos</b><span>La capacidad nominal, el remanente de tubería y las capacidades de cada equipo alimentarán rendimientos, pérdidas, agua, energía y costo por lote.</span></div><Waves/></section>
    </>}
  </main>
}

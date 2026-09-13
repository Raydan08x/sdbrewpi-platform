import { useCallback, useEffect, useMemo, useState } from 'react'
import { AlertTriangle, Cable, Check, CircleGauge, Cpu, Factory, FlaskConical, HardDriveDownload, Pencil, Plus, RefreshCw, Save, Snowflake, Trash2, Waves, X } from 'lucide-react'
import { createPlantAsset, getPlantOverview, retirePlantAsset, updatePlantAsset, updatePlantProfile } from './api'
import type { PlantAsset, PlantAssetInput, PlantOverview, PlantProfile } from './types'
import WarehouseSection from './WarehouseSection'

type Draft = Omit<PlantProfile, 'id' | 'code' | 'revision' | 'updatedAt'>

const typeLabels: Record<string, string> = {
  CHILLER: 'Sistema de frío', PUMP: 'Bombas', VFD: 'Variadores', POWER: 'Potencia',
  FERMENTER: 'Fermentadores', BREWHOUSE: 'Tren de cocción', MILL: 'Molinos', CONTROLLER: 'Controladores',
  HMI: 'Pantallas', RELAY_MODULE: 'Módulos de salida', SENSOR_GATEWAY: 'Repetidores', SENSOR: 'Sensores',
  PACKAGING: 'Envasado', FILTER: 'Filtración', OTHER: 'Otros equipos',
}

const assetTypes = Object.keys(typeLabels)
const assetStatuses = ['DOCUMENTED', 'AVAILABLE', 'NEEDS_DATA', 'OFFLINE', 'WAITING_BATTERY', 'MAINTENANCE', 'OUT_OF_SERVICE']
const emptyAsset: PlantAssetInput = { code: '', assetType: 'OTHER', name: '', manufacturer: '', model: '', capacityL: null, electricalSpec: '', communicationProtocol: '', deviceIdentifier: '', firmwareProfile: '', status: 'NEEDS_DATA', controllable: false, notes: '' }

function AssetCard({ asset, onEdit }: { asset: PlantAsset; onEdit: (asset: PlantAsset) => void }) {
  return <article className="asset-card">
    <div className="asset-title"><div><small>{asset.code}</small><b>{asset.name}</b></div><div className="asset-card-actions"><span className={'asset-status ' + asset.status.toLowerCase()}>{asset.status.replaceAll('_', ' ')}</span><button type="button" aria-label={`Editar ${asset.name}`} onClick={() => onEdit(asset)}><Pencil size={13}/></button></div></div>
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
  const [editingAsset, setEditingAsset] = useState<PlantAsset | null>(null)
  const [assetDraft, setAssetDraft] = useState<PlantAssetInput | null>(null)
  const [assetBusy, setAssetBusy] = useState(false)
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
  const setAsset = <K extends keyof PlantAssetInput>(key: K, value: PlantAssetInput[K]) => setAssetDraft(current => current ? { ...current, [key]: value } : current)
  const startCreateAsset = () => { setEditingAsset(null); setAssetDraft({ ...emptyAsset }); setError(''); setNotice('') }
  const startEditAsset = (asset: PlantAsset) => {
    const { code, assetType, name, manufacturer, model, capacityL, electricalSpec, communicationProtocol, deviceIdentifier, firmwareProfile, status, controllable, notes } = asset
    setEditingAsset(asset)
    setAssetDraft({ code, assetType, name, manufacturer, model, capacityL, electricalSpec, communicationProtocol, deviceIdentifier, firmwareProfile, status, controllable, notes })
    setError(''); setNotice('')
  }
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

  const saveAsset = async () => {
    if (!overview || !assetDraft) return
    setAssetBusy(true); setNotice('')
    try {
      await (editingAsset ? updatePlantAsset(editingAsset, assetDraft) : createPlantAsset(overview.site.id, assetDraft))
      const refreshed = await getPlantOverview()
      setOverview(refreshed); setAssetDraft(null); setEditingAsset(null); setError('')
      setNotice(editingAsset ? 'Equipo actualizado y registrado en la bitácora' : 'Equipo agregado a la planta')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No se pudo guardar el equipo') }
    finally { setAssetBusy(false) }
  }

  const retireAsset = async () => {
    if (!editingAsset || !window.confirm(`¿Retirar ${editingAsset.name} del inventario activo? Su historial se conservará.`)) return
    setAssetBusy(true); setNotice('')
    try {
      await retirePlantAsset(editingAsset)
      const refreshed = await getPlantOverview()
      setOverview(refreshed); setAssetDraft(null); setEditingAsset(null); setError('')
      setNotice('Equipo retirado; su historial permanece registrado')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No se pudo retirar el equipo') }
    finally { setAssetBusy(false) }
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

      {assetDraft && <form className="asset-editor" onSubmit={event => { event.preventDefault(); void saveAsset() }}>
        <div className="panel-heading"><div><span className="eyebrow">INVENTARIO TÉCNICO</span><h2>{editingAsset ? `Editar ${editingAsset.code}` : 'Agregar equipo'}</h2></div><button type="button" className="icon-action" aria-label="Cerrar editor" onClick={() => setAssetDraft(null)}><X size={17}/></button></div>
        <div className="form-grid asset-form-grid">
          <label>Código<input required value={assetDraft.code} maxLength={50} placeholder="EQUIPO-01" onChange={event => setAsset('code', event.target.value.toUpperCase())}/></label>
          <label>Tipo<select value={assetDraft.assetType} onChange={event => setAsset('assetType', event.target.value)}>{assetTypes.map(type => <option key={type} value={type}>{typeLabels[type]}</option>)}</select></label>
          <label>Nombre<input required value={assetDraft.name} maxLength={120} onChange={event => setAsset('name', event.target.value)}/></label>
          <label>Estado<select value={assetDraft.status} onChange={event => setAsset('status', event.target.value)}>{assetStatuses.map(status => <option key={status} value={status}>{status.replaceAll('_', ' ')}</option>)}</select></label>
          <label>Fabricante<input value={assetDraft.manufacturer} maxLength={100} onChange={event => setAsset('manufacturer', event.target.value)}/></label>
          <label>Modelo<input value={assetDraft.model} maxLength={140} onChange={event => setAsset('model', event.target.value)}/></label>
          <label>Capacidad (L)<input type="number" min="0" step="0.1" value={assetDraft.capacityL ?? ''} onChange={event => setAsset('capacityL', event.target.value ? Number(event.target.value) : null)}/></label>
          <label>Identificador del dispositivo<input value={assetDraft.deviceIdentifier} maxLength={160} onChange={event => setAsset('deviceIdentifier', event.target.value)}/></label>
          <label>Especificación eléctrica<input value={assetDraft.electricalSpec} maxLength={300} onChange={event => setAsset('electricalSpec', event.target.value)}/></label>
          <label>Protocolo o conexión<input value={assetDraft.communicationProtocol} maxLength={160} onChange={event => setAsset('communicationProtocol', event.target.value)}/></label>
          <label>Perfil de firmware<input value={assetDraft.firmwareProfile} maxLength={120} onChange={event => setAsset('firmwareProfile', event.target.value)}/></label>
          <label className="check-field"><input type="checkbox" checked={assetDraft.controllable} onChange={event => setAsset('controllable', event.target.checked)}/><span>Equipo controlable</span></label>
          <label className="wide">Notas<textarea value={assetDraft.notes} maxLength={1000} rows={3} onChange={event => setAsset('notes', event.target.value)}/></label>
        </div>
        <div className="asset-editor-actions">
          {editingAsset && <button type="button" className="danger-action" disabled={assetBusy} onClick={() => void retireAsset()}><Trash2 size={15}/>Retirar equipo</button>}
          <button className="primary-action" disabled={assetBusy}><Save size={16}/>{assetBusy ? 'Guardando…' : 'Guardar equipo'}</button>
        </div>
      </form>}

      <section className="asset-register">
        <div className="section-title"><div><span className="line"/><h3>INVENTARIO TÉCNICO</h3></div><div className="section-actions"><small>{overview.assets.length} activos en esta sede</small><button type="button" onClick={startCreateAsset}><Plus size={14}/>Agregar equipo</button></div></div>
        {groups.map(([type, assets]) => <div className="asset-group" key={type}><h4>{typeLabels[type] ?? type}</h4><div className="asset-grid">{assets.map(asset => <AssetCard key={asset.id} asset={asset} onEdit={startEditAsset}/>)}</div></div>)}
      </section>
      <WarehouseSection siteId={overview.site.id} warehouses={overview.warehouses} onChanged={load}/>
      <section className="plant-calculation-note"><CircleGauge/><div><b>Base para balances y costos</b><span>La capacidad nominal, el remanente de tubería y las capacidades de cada equipo alimentarán rendimientos, pérdidas, agua, energía y costo por lote.</span></div><Waves/></section>
    </>}
  </main>
}

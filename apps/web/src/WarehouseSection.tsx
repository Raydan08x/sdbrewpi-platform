import { useState } from 'react'
import { Boxes, MapPin, Pencil, Plus, Save, Snowflake, Trash2, Warehouse, X } from 'lucide-react'
import { createPlantWarehouse, createStorageLocation, retirePlantWarehouse, retireStorageLocation, updatePlantWarehouse, updateStorageLocation } from './api'
import type { PlantStorageLocation, PlantStorageLocationInput, PlantWarehouse, PlantWarehouseInput } from './types'

const purposeLabels: Record<string, string> = {
  GENERAL: 'Uso general',
  RAW_MATERIALS: 'Materias primas',
  PACKAGING: 'Material de empaque',
  PRODUCTION: 'Abastecimiento de producción',
  FINISHED_GOODS: 'Producto terminado',
  CHEMICALS: 'Químicos e insumos de aseo',
  COLD_STORAGE: 'Almacenamiento refrigerado',
  QUARANTINE: 'Cuarentena y calidad',
  MAINTENANCE: 'Mantenimiento y repuestos',
  OTHER: 'Otra finalidad',
}

const categoryLabels: Record<string, string> = {
  RAW_MATERIAL: 'Materia prima',
  PACKAGING_MATERIAL: 'Material de empaque',
  OPERATING_SUPPLY: 'Insumo operativo',
  WORK_IN_PROGRESS: 'Producto en proceso',
  FINISHED_GOOD: 'Producto terminado',
  SPARE_PART: 'Repuesto',
}

const locationTypeLabels: Record<string, string> = {
  ZONE: 'Zona', AISLE: 'Pasillo', RACK: 'Estantería', SHELF: 'Nivel', FLOOR: 'Piso',
  TANK: 'Tanque', ROOM: 'Cuarto', OTHER: 'Otra',
}

const emptyWarehouse: PlantWarehouseInput = {
  code: '', name: '', purpose: 'GENERAL', temperatureControlled: false,
  allowedCategories: ['RAW_MATERIAL'], notes: '',
}

const emptyLocation: PlantStorageLocationInput = { code: '', name: '', locationType: 'ZONE', notes: '' }

interface Props {
  siteId: string
  warehouses: PlantWarehouse[]
  onChanged: () => Promise<void>
}

export default function WarehouseSection({ siteId, warehouses, onChanged }: Props) {
  const [warehouseDraft, setWarehouseDraft] = useState<PlantWarehouseInput | null>(null)
  const [editingWarehouse, setEditingWarehouse] = useState<PlantWarehouse | null>(null)
  const [locationDraft, setLocationDraft] = useState<PlantStorageLocationInput | null>(null)
  const [editingLocation, setEditingLocation] = useState<PlantStorageLocation | null>(null)
  const [locationWarehouse, setLocationWarehouse] = useState<PlantWarehouse | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const setWarehouseValue = <K extends keyof PlantWarehouseInput>(key: K, value: PlantWarehouseInput[K]) =>
    setWarehouseDraft(current => current ? { ...current, [key]: value } : current)
  const setLocationValue = <K extends keyof PlantStorageLocationInput>(key: K, value: PlantStorageLocationInput[K]) =>
    setLocationDraft(current => current ? { ...current, [key]: value } : current)

  const startCreateWarehouse = () => {
    setEditingWarehouse(null); setWarehouseDraft({ ...emptyWarehouse, allowedCategories: [...emptyWarehouse.allowedCategories] })
    setLocationDraft(null); setError(''); setNotice('')
  }
  const startEditWarehouse = (warehouse: PlantWarehouse) => {
    const { code, name, purpose, temperatureControlled, allowedCategories, notes } = warehouse
    setEditingWarehouse(warehouse)
    setWarehouseDraft({ code, name, purpose, temperatureControlled, allowedCategories: [...allowedCategories], notes })
    setLocationDraft(null); setError(''); setNotice('')
  }
  const closeWarehouseEditor = () => { setWarehouseDraft(null); setEditingWarehouse(null) }

  const toggleCategory = (category: string) => {
    if (!warehouseDraft) return
    const selected = warehouseDraft.allowedCategories.includes(category)
    setWarehouseValue('allowedCategories', selected
      ? warehouseDraft.allowedCategories.filter(item => item !== category)
      : [...warehouseDraft.allowedCategories, category])
  }

  const saveWarehouse = async () => {
    if (!warehouseDraft || warehouseDraft.allowedCategories.length === 0) return
    setBusy(true); setError(''); setNotice('')
    try {
      await (editingWarehouse
        ? updatePlantWarehouse(editingWarehouse, warehouseDraft)
        : createPlantWarehouse(siteId, warehouseDraft))
      await onChanged(); closeWarehouseEditor()
      setNotice(editingWarehouse ? 'Bodega actualizada' : 'Bodega registrada')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No se pudo guardar la bodega') }
    finally { setBusy(false) }
  }

  const removeWarehouse = async () => {
    if (!editingWarehouse || !window.confirm(`¿Retirar ${editingWarehouse.name} y sus ubicaciones activas? El historial se conservará.`)) return
    setBusy(true); setError(''); setNotice('')
    try {
      await retirePlantWarehouse(editingWarehouse); await onChanged(); closeWarehouseEditor()
      setNotice('Bodega retirada; el historial permanece registrado')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No se pudo retirar la bodega') }
    finally { setBusy(false) }
  }

  const startCreateLocation = (warehouse: PlantWarehouse) => {
    setLocationWarehouse(warehouse); setEditingLocation(null); setLocationDraft({ ...emptyLocation })
    setWarehouseDraft(null); setError(''); setNotice('')
  }
  const startEditLocation = (warehouse: PlantWarehouse, location: PlantStorageLocation) => {
    const { code, name, locationType, notes } = location
    setLocationWarehouse(warehouse); setEditingLocation(location); setLocationDraft({ code, name, locationType, notes })
    setWarehouseDraft(null); setError(''); setNotice('')
  }
  const closeLocationEditor = () => { setLocationDraft(null); setEditingLocation(null); setLocationWarehouse(null) }

  const saveLocation = async () => {
    if (!locationDraft || !locationWarehouse) return
    setBusy(true); setError(''); setNotice('')
    try {
      await (editingLocation
        ? updateStorageLocation(editingLocation, locationDraft)
        : createStorageLocation(locationWarehouse.id, locationDraft))
      await onChanged(); closeLocationEditor()
      setNotice(editingLocation ? 'Ubicación actualizada' : 'Ubicación registrada')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No se pudo guardar la ubicación') }
    finally { setBusy(false) }
  }

  const removeLocation = async () => {
    if (!editingLocation || !window.confirm(`¿Retirar la ubicación ${editingLocation.name}?`)) return
    setBusy(true); setError(''); setNotice('')
    try {
      await retireStorageLocation(editingLocation); await onChanged(); closeLocationEditor()
      setNotice('Ubicación retirada')
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No se pudo retirar la ubicación') }
    finally { setBusy(false) }
  }

  return <section className="warehouse-section">
    <div className="section-title"><div><span className="line"/><h3>BODEGAS Y UBICACIONES</h3></div><div className="section-actions"><small>{warehouses.length} bodegas configuradas</small><button type="button" onClick={startCreateWarehouse}><Plus size={14}/>Agregar bodega</button></div></div>
    <p className="warehouse-intro">Define dónde puede existir cada categoría de inventario. Las existencias, lotes y movimientos se implementarán en el módulo Inventarios.</p>
    {error && <div className="message error">{error}</div>}{notice && <div className="message success">{notice}</div>}

    {warehouseDraft && <form className="warehouse-editor" onSubmit={event => { event.preventDefault(); void saveWarehouse() }}>
      <div className="panel-heading"><div><span className="eyebrow">CONFIGURACIÓN DE BODEGA</span><h2>{editingWarehouse ? `Editar ${editingWarehouse.code}` : 'Nueva bodega'}</h2></div><button type="button" className="icon-action" aria-label="Cerrar editor de bodega" onClick={closeWarehouseEditor}><X size={17}/></button></div>
      <div className="form-grid warehouse-form-grid">
        <label>Código<input required maxLength={50} placeholder="BOD-PRINCIPAL" value={warehouseDraft.code} onChange={event => setWarehouseValue('code', event.target.value.toUpperCase())}/></label>
        <label>Nombre<input required maxLength={120} value={warehouseDraft.name} onChange={event => setWarehouseValue('name', event.target.value)}/></label>
        <label>Finalidad principal<select value={warehouseDraft.purpose} onChange={event => setWarehouseValue('purpose', event.target.value)}>{Object.entries(purposeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <label className="check-field"><input type="checkbox" checked={warehouseDraft.temperatureControlled} onChange={event => setWarehouseValue('temperatureControlled', event.target.checked)}/><span>Temperatura controlada</span></label>
        <fieldset className="category-selector"><legend>Categorías permitidas</legend>{Object.entries(categoryLabels).map(([value, label]) => <label key={value}><input type="checkbox" checked={warehouseDraft.allowedCategories.includes(value)} onChange={() => toggleCategory(value)}/><span>{label}</span></label>)}</fieldset>
        <label className="wide">Notas<textarea rows={3} maxLength={1000} value={warehouseDraft.notes} onChange={event => setWarehouseValue('notes', event.target.value)}/></label>
      </div>
      {warehouseDraft.allowedCategories.length === 0 && <small className="form-warning">Selecciona al menos una categoría de inventario.</small>}
      <div className="asset-editor-actions">{editingWarehouse && <button type="button" className="danger-action" disabled={busy} onClick={() => void removeWarehouse()}><Trash2 size={15}/>Retirar bodega</button>}<button className="primary-action" disabled={busy || warehouseDraft.allowedCategories.length === 0}><Save size={16}/>{busy ? 'Guardando…' : 'Guardar bodega'}</button></div>
    </form>}

    {locationDraft && locationWarehouse && <form className="warehouse-editor location-editor" onSubmit={event => { event.preventDefault(); void saveLocation() }}>
      <div className="panel-heading"><div><span className="eyebrow">{locationWarehouse.code}</span><h2>{editingLocation ? `Editar ${editingLocation.code}` : 'Nueva ubicación'}</h2></div><button type="button" className="icon-action" aria-label="Cerrar editor de ubicación" onClick={closeLocationEditor}><X size={17}/></button></div>
      <div className="form-grid warehouse-form-grid">
        <label>Código<input required maxLength={50} placeholder="RACK-A" value={locationDraft.code} onChange={event => setLocationValue('code', event.target.value.toUpperCase())}/></label>
        <label>Nombre<input required maxLength={120} value={locationDraft.name} onChange={event => setLocationValue('name', event.target.value)}/></label>
        <label>Tipo<select value={locationDraft.locationType} onChange={event => setLocationValue('locationType', event.target.value)}>{Object.entries(locationTypeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <label className="wide">Notas<textarea rows={3} maxLength={500} value={locationDraft.notes} onChange={event => setLocationValue('notes', event.target.value)}/></label>
      </div>
      <div className="asset-editor-actions">{editingLocation && <button type="button" className="danger-action" disabled={busy} onClick={() => void removeLocation()}><Trash2 size={15}/>Retirar ubicación</button>}<button className="primary-action" disabled={busy}><Save size={16}/>{busy ? 'Guardando…' : 'Guardar ubicación'}</button></div>
    </form>}

    {warehouses.length === 0 ? <div className="warehouse-empty"><Warehouse size={28}/><div><b>Aún no hay bodegas configuradas</b><span>Registra la distribución física antes de crear artículos y existencias.</span></div></div> : <div className="warehouse-grid">{warehouses.map(warehouse => <article className="warehouse-card" key={warehouse.id}>
      <div className="warehouse-heading"><div><small>{warehouse.code}</small><h4>{warehouse.name}</h4><span>{purposeLabels[warehouse.purpose] ?? warehouse.purpose}</span></div><div className="warehouse-heading-actions">{warehouse.temperatureControlled && <span className="cold-badge"><Snowflake size={12}/>Control térmico</span>}<button type="button" aria-label={`Editar ${warehouse.name}`} onClick={() => startEditWarehouse(warehouse)}><Pencil size={14}/></button></div></div>
      <div className="warehouse-categories"><small>CATEGORÍAS ADMITIDAS</small><div>{warehouse.allowedCategories.map(category => <span key={category}><Boxes size={11}/>{categoryLabels[category] ?? category}</span>)}</div></div>
      <div className="location-list"><div><small>UBICACIONES</small><button type="button" onClick={() => startCreateLocation(warehouse)}><Plus size={12}/>Agregar</button></div>{warehouse.locations.length === 0 ? <p>Sin zonas internas registradas.</p> : warehouse.locations.map(location => <button type="button" className="location-row" key={location.id} onClick={() => startEditLocation(warehouse, location)}><MapPin size={13}/><span><b>{location.code}</b>{location.name}</span><small>{locationTypeLabels[location.locationType] ?? location.locationType}</small><Pencil size={12}/></button>)}</div>
      {warehouse.notes && <p className="warehouse-notes">{warehouse.notes}</p>}
    </article>)}</div>}
  </section>
}

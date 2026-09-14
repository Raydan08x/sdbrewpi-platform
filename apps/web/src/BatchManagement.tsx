import { useState, type FormEvent } from 'react'
import { assignFermentation } from './api'
import type { Batch, ProductionOverview, Tank } from './types'

const kindLabel = { TEST: 'Prueba', PILOT: 'Piloto', COMMERCIAL: 'Comercial' }

export function BatchManagement({ production, tanks, onChanged }: {
  production: ProductionOverview; tanks: Tank[]; onChanged: () => Promise<void>
}) {
  const [selected, setSelected] = useState<Batch | null>(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const eligible = production.activeBatches.filter(batch => batch.status === 'READY_FOR_FERMENTATION')
  const freeTanks = tanks.filter(tank => !production.activeBatches.some(batch => batch.status === 'FERMENTING' && batch.tankId === tank.id))

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selected) return
    const data = new FormData(event.currentTarget)
    setBusy(true); setMessage('')
    try {
      await assignFermentation(selected, String(data.get('tank')), Number(data.get('volume')))
      setSelected(null)
      setMessage('Transferencia registrada. El fermentador y la Pill quedaron guardados en el batch record.')
      await onChanged()
    } catch (caught) { setMessage(caught instanceof Error ? caught.message : 'No se pudo asignar la transferencia') }
    finally { setBusy(false) }
  }

  return <section className="batch-management" aria-label="Asignación de lotes fabricados">
    <div className="batch-management-heading"><div><h3>Lotes listos para fermentación</h3><p>Solo aparecen lotes liberados por Producción después de fabricación y enfriado.</p></div><span>{eligible.length} disponibles</span></div>
    {message && <p role="status">{message}</p>}
    {!eligible.length && <p>No hay lotes pendientes de transferencia.</p>}
    <div className="batch-management-list">{eligible.map(batch => <div key={batch.id}>
      <span><b>{batch.code}</b> · {kindLabel[batch.batchKind]} · {batch.productName} · {batch.recipeName} v{batch.recipeVersion}</span>
      <button disabled={busy || !freeTanks.length} onClick={() => setSelected(batch)}>Asignar fermentador</button>
    </div>)}</div>
    {selected && <form onSubmit={submit}><fieldset disabled={busy} className="batch-create-fields"><legend>Transferir {selected.code}</legend>
      <label>Fermentador disponible<select name="tank" required defaultValue=""><option value="" disabled>Selecciona un tanque</option>{freeTanks.map(tank => <option key={tank.id} value={tank.id}>{tank.name} · Pill {tank.pillId}</option>)}</select></label>
      <label>Volumen transferido (L)<input name="volume" type="number" min={1} max={10000} step="0.1" required defaultValue={selected.volumeL} /></label>
      <p>El servidor captura el fermentador, la Pill y la fuente activa como información histórica.</p>
      <div className="batch-buttons"><button type="submit">Confirmar transferencia</button><button type="button" onClick={() => setSelected(null)}>Cancelar</button></div>
    </fieldset></form>}
  </section>
}

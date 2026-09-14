import { useState, type FormEvent } from 'react'
import { markReadyForFermentation, releaseProductionOrder } from './api'
import type { Batch, ProductionOverview } from './types'

const kindLabel = { TEST: 'Prueba técnica', PILOT: 'Lote piloto', COMMERCIAL: 'Lote comercial' }

export function ProductionOrderPanel({ production, onChanged }: { production: ProductionOverview; onChanged: () => Promise<void> }) {
  const [editing, setEditing] = useState(false)
  const [busy, setBusy] = useState('')
  const [message, setMessage] = useState('')
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const data = new FormData(event.currentTarget); setBusy('release'); setMessage('')
    try {
      const batch = await releaseProductionOrder({ recipeVersionId: String(data.get('recipe')), plannedVolumeL: Number(data.get('volume')),
        batchKind: String(data.get('kind')) as Batch['batchKind'], productCode: String(data.get('product')) as Batch['productCode'] })
      setEditing(false); setMessage(`Orden liberada: ${batch.code}. El batch record quedó abierto.`); await onChanged()
    } catch (caught) { setMessage(caught instanceof Error ? caught.message : 'No se pudo liberar la orden') }
    finally { setBusy('') }
  }
  async function ready(batch: Batch) {
    setBusy(batch.id); setMessage('')
    try { await markReadyForFermentation(batch); setMessage(`${batch.code} quedó disponible en Fermentación.`); await onChanged() }
    catch (caught) { setMessage(caught instanceof Error ? caught.message : 'No se pudo liberar el lote hacia fermentación') }
    finally { setBusy('') }
  }
  const released = production.activeBatches.filter(batch => batch.status === 'RELEASED')
  return <section className="batch-management production-orders">
    <div className="batch-management-heading"><div><h3>Órdenes y batch record</h3><p>El código nace al liberar la orden y nunca se reutiliza.</p></div><button onClick={() => setEditing(!editing)}>Liberar orden</button></div>
    {message && <p role="status">{message}</p>}
    {editing && <form onSubmit={submit}><fieldset disabled={busy === 'release'} className="batch-create-fields"><legend>Nueva orden</legend>
      <label>Producto<select name="product" required defaultValue="CERV"><option value="CERV">Cerveza · CERV</option><option value="HSEL">Hard seltzer · HSEL</option></select></label>
      <label>Clase<select name="kind" required defaultValue="COMMERCIAL"><option value="TEST">Prueba · T</option><option value="PILOT">Piloto · P</option><option value="COMMERCIAL">Comercial · L</option></select></label>
      <label>Receta y versión<select name="recipe" required defaultValue=""><option value="" disabled>Selecciona una versión</option>{production.recipes.map(recipe => <option key={recipe.id} value={recipe.id}>{recipe.name} · v{recipe.version}</option>)}</select></label>
      <label>Volumen planeado (L)<input name="volume" type="number" min={1} max={10000} step="0.1" required /></label>
      <div className="batch-buttons"><button type="submit">Liberar y abrir expediente</button><button type="button" onClick={() => setEditing(false)}>Cancelar</button></div>
    </fieldset></form>}
    <div className="batch-management-list">{released.map(batch => <div key={batch.id}><span><b>{batch.code}</b> · {kindLabel[batch.batchKind]} · {batch.productName} · {batch.recipeName}</span><button disabled={!!busy} onClick={() => ready(batch)}>Marcar fabricación lista</button></div>)}</div>
  </section>
}

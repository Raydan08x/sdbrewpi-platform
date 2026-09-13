import { useState, type FormEvent } from 'react'
import { completeBatch, createBatch } from './api'
import type { Batch, ProductionOverview, Tank } from './types'

export function BatchManagement({ production, tanks, onChanged }: {
  production: ProductionOverview; tanks: Tank[]; onChanged: () => Promise<void>
}) {
  const [editing, setEditing] = useState(false)
  const [closing, setClosing] = useState<Batch | null>(null)
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const freeTanks = tanks.filter(tank => !production.activeBatches.some(batch => batch.tankId === tank.id))
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    setBusy(true); setError(''); setNotice('')
    try {
      await createBatch({ code: String(data.get('code')).trim(), recipeVersionId: String(data.get('recipe')),
        tankId: String(data.get('tank')), volumeL: Number(data.get('volume')) })
      setEditing(false); setNotice('Lote asignado. Su perfil queda listo para iniciar desde el panel de ejecución.')
      await onChanged()
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No se pudo crear el lote') }
    finally { setBusy(false) }
  }
  async function close() {
    if (!closing || confirmation !== closing.code) return
    setBusy(true); setError(''); setNotice('')
    try {
      await completeBatch(closing)
      setClosing(null); setConfirmation('')
      setNotice('Lote cerrado y tanque en OFF. No se registra producto terminado ni se confirma una transferencia física.')
      await onChanged()
    } catch (caught) { setError(caught instanceof Error ? caught.message : 'No se pudo cerrar el lote') }
    finally { setBusy(false) }
  }
  return <section className="batch-management" aria-label="Gestión de lotes de fermentación">
    <div className="batch-management-heading"><div><h3>Lotes de fermentación</h3><p>{freeTanks.length} de {tanks.length} tanques disponibles para asignación</p></div>
      <button disabled={busy || !freeTanks.length || !production.recipes.length} onClick={() => { setEditing(!editing); setError(''); setNotice('') }}>Nuevo lote</button></div>
    {!production.recipes.length && <p>Necesitas una versión de receta con perfil antes de asignar un lote.</p>}
    {!freeTanks.length && <p>Todos los tanques tienen lote activo.</p>}
    {error && <p role="alert" className="message error">{error}</p>}
    {notice && <p role="status" className="message success">{notice}</p>}
    {editing && <form onSubmit={submit}>
      <fieldset disabled={busy} className="batch-create-fields"><legend>Asignar un lote sin iniciar el control</legend>
        <label>Código del lote<input name="code" required maxLength={40} pattern=".*\S.*" placeholder="FER-001" /></label>
        <label>Receta y versión<select name="recipe" required defaultValue=""><option value="" disabled>Selecciona una versión</option>{production.recipes.map(recipe => <option key={recipe.id} value={recipe.id}>{recipe.name} · v{recipe.version}</option>)}</select></label>
        <label>Fermentador disponible<select name="tank" required defaultValue=""><option value="" disabled>Selecciona un tanque</option>{freeTanks.map(tank => <option key={tank.id} value={tank.id}>{tank.name}</option>)}</select></label>
        <label>Volumen del lote (L)<input name="volume" type="number" min={1} max={10000} step="0.1" required /></label>
        <div className="batch-buttons"><button type="submit" disabled={!freeTanks.length}>Asignar lote</button><button type="button" onClick={() => setEditing(false)}>Cancelar</button></div>
      </fieldset>
    </form>}
    <div className="batch-management-list">{production.activeBatches.map(batch => <div key={batch.id}><span><b>{batch.code}</b> · {batch.tankId} · {batch.volumeL} L</span><button disabled={busy} onClick={() => { setClosing(batch); setConfirmation(''); setError(''); setNotice('') }}>Cerrar lote {batch.code}</button></div>)}</div>
    {closing && <div className="batch-close-confirm" role="group" aria-label="Confirmar cierre de lote">
      <b>Cerrar {closing.code} en {closing.tankId}</b>
      <p>El control del tanque quedará en OFF y su asignación quedará libre. Se conserva la información del lote. Esta acción también termina un perfil que aún esté en ejecución.</p>
      <label>Escribe {closing.code} para confirmar<input value={confirmation} disabled={busy} onChange={event => setConfirmation(event.target.value)} autoComplete="off" /></label>
      <div className="batch-buttons"><button disabled={busy || confirmation !== closing.code} onClick={close}>Confirmar cierre</button><button disabled={busy} onClick={() => setClosing(null)}>Cancelar cierre</button></div>
    </div>}
  </section>
}

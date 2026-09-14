import { useState, type FormEvent } from 'react'
import { Check, Circle, FastForward, Play } from 'lucide-react'
import { commandProductionStage } from './api'
import type { Batch, BatchStage } from './types'

const finished = (stage: BatchStage) => stage.status === 'COMPLETED' || stage.status === 'SKIPPED'

export function ProductionStagePanel({ batch, onChanged }: { batch: Batch; onChanged: () => Promise<void> }) {
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [notes, setNotes] = useState('')
  const [measuredValue, setMeasuredValue] = useState('')
  const [unit, setUnit] = useState('')
  const stages = batch.executionStages.filter(stage => stage.order <= 180)
  const current = stages.find(stage => !finished(stage))
  const completed = stages.filter(finished).length

  async function action(stage: BatchStage, type: 'START' | 'COMPLETE' | 'SKIP') {
    setBusy(true); setMessage('')
    try {
      await commandProductionStage(batch, stage.code, {
        action: type,
        expectedStageRevision: stage.revision,
        notes: notes.trim(),
        ...(measuredValue === '' ? {} : { measuredValue: Number(measuredValue), unit: unit.trim() }),
      })
      setNotes(''); setMeasuredValue(''); setUnit('')
      setMessage(type === 'START' ? `Etapa iniciada: ${stage.name}` : type === 'SKIP' ? `Etapa omitida: ${stage.name}` : `Etapa completada: ${stage.name}`)
      await onChanged()
    } catch (caught) { setMessage(caught instanceof Error ? caught.message : 'No se pudo actualizar la etapa') }
    finally { setBusy(false) }
  }

  function complete(event: FormEvent) {
    event.preventDefault()
    if (current) void action(current, 'COMPLETE')
  }

  if (!stages.length) return <p>Esta orden todavía no tiene una ruta ejecutable.</p>
  return <div className="production-stage-panel">
    <div className="stage-progress"><span><b>{completed}/{stages.length}</b> etapas resueltas</span><i><em style={{width: `${completed * 100 / stages.length}%`}}/></i></div>
    {current && <article className="current-production-stage">
      <div><small>SIGUIENTE ETAPA · {current.phase}</small><h4>{current.name}</h4><p>{current.description}</p></div>
      {current.status === 'PENDING' ? <div className="stage-actions"><button disabled={busy} onClick={() => action(current, 'START')}><Play size={14}/>Iniciar</button>{current.optional && <button disabled={busy} className="secondary" onClick={() => action(current, 'SKIP')}><FastForward size={14}/>Omitir</button>}</div>
        : <form onSubmit={complete}><label>Observaciones<textarea maxLength={1000} value={notes} onChange={event => setNotes(event.target.value)} placeholder="Resultado, desviaciones y datos relevantes"/></label><div className="stage-measurement"><label>Medición<input type="number" step="0.0001" value={measuredValue} onChange={event => setMeasuredValue(event.target.value)}/></label><label>Unidad<input maxLength={20} required={measuredValue !== ''} value={unit} onChange={event => setUnit(event.target.value)} placeholder="L, °C, SG…"/></label></div><div className="stage-actions"><button disabled={busy}><Check size={14}/>Completar etapa</button>{current.optional && <button type="button" disabled={busy} className="secondary" onClick={() => action(current, 'SKIP')}><FastForward size={14}/>Omitir</button>}</div></form>}
    </article>}
    {message && <p role="status" className="stage-message">{message}</p>}
    <details><summary>Ver expediente de fabricación</summary><ol className="production-stage-list">{stages.map(stage => <li key={stage.code} className={stage.status.toLowerCase()}>{stage.status === 'COMPLETED' ? <Check size={13}/> : stage.status === 'SKIPPED' ? <FastForward size={13}/> : <Circle size={11}/>}<span><b>{stage.name}</b><small>{stage.status === 'COMPLETED' ? `Completada por ${stage.completedBy}` : stage.status === 'SKIPPED' ? `Omitida por ${stage.completedBy}` : stage.status === 'IN_PROGRESS' ? `Iniciada por ${stage.startedBy}` : stage.optional ? 'Opcional' : 'Pendiente'}{stage.measuredValue === null ? '' : ` · ${stage.measuredValue} ${stage.unit}`}</small></span></li>)}</ol></details>
  </div>
}

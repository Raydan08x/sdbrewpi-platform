import type { ControlMode, Overview, ProductionOverview, Tank } from './types'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, { ...init, headers: { 'Content-Type': 'application/json', 'X-Actor': 'local-webapp', ...init?.headers } })
  if (!response.ok) {
    const raw = await response.text()
    const message = (() => {
      try { return (JSON.parse(raw) as { message?: string }).message ?? '' }
      catch { return raw.trim() }
    })()
    throw new Error(message || `No se pudo completar la operación (HTTP ${response.status})`)
  }
  return response.json()
}

export const getOverview = () => request<Overview>('/api/v1/fermentation/overview')
export const getProductionOverview = () => request<ProductionOverview>('/api/v1/production/overview')

export const setMode = (tank: Tank, mode: ControlMode) => request('/api/v1/fermentation/tanks/' + tank.id + '/mode', {
  method: 'PUT', body: JSON.stringify({ mode, expectedRevision: tank.revision })
})

export const setSetpoint = (tank: Tank, setpointC: number) => request('/api/v1/fermentation/tanks/' + tank.id + '/setpoint', {
  method: 'PUT', body: JSON.stringify({ setpointC, expectedRevision: tank.revision })
})

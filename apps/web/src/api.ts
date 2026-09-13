import type { ControlMode, Overview, PlantAsset, PlantAssetInput, PlantOverview, PlantProfile, ProductionOverview, Tank } from './types'

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
export const getPlantOverview = () => request<PlantOverview>('/api/v1/plant/overview')

export const updatePlantProfile = (site: PlantProfile, values: Omit<PlantProfile, 'id' | 'code' | 'revision' | 'updatedAt'>) =>
  request<PlantProfile>('/api/v1/plant/sites/' + site.id, {
    method: 'PUT', body: JSON.stringify({ ...values, expectedRevision: site.revision })
  })

export const createPlantAsset = (siteId: string, values: PlantAssetInput) =>
  request<PlantAsset>(`/api/v1/plant/sites/${siteId}/assets`, { method: 'POST', body: JSON.stringify(values) })

export const updatePlantAsset = (asset: PlantAsset, values: PlantAssetInput) =>
  request<PlantAsset>(`/api/v1/plant/assets/${asset.id}`, {
    method: 'PUT', body: JSON.stringify({ ...values, expectedRevision: asset.revision })
  })

export const retirePlantAsset = (asset: PlantAsset) =>
  request<PlantAsset>(`/api/v1/plant/assets/${asset.id}/retire`, {
    method: 'PUT', body: JSON.stringify({ expectedRevision: asset.revision })
  })

export const setMode = (tank: Tank, mode: ControlMode) => request('/api/v1/fermentation/tanks/' + tank.id + '/mode', {
  method: 'PUT', body: JSON.stringify({ mode, expectedRevision: tank.revision })
})

export const setSetpoint = (tank: Tank, setpointC: number) => request('/api/v1/fermentation/tanks/' + tank.id + '/setpoint', {
  method: 'PUT', body: JSON.stringify({ setpointC, expectedRevision: tank.revision })
})

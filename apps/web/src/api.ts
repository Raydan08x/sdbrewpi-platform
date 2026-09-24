import type { AlarmHistory, AuthSession, AuthUser, Batch, BatchEvent, BatchEventInput, ControlMode, FermentationAlarm, FermentationHistory, InventoryItem, InventoryLot, InventoryMovement, InventoryOverview, Overview, PlantAsset, PlantAssetInput, PlantOverview, PlantProfile, PlantStorageLocation, PlantStorageLocationInput, PlantWarehouse, PlantWarehouseInput, ProductionOverview, Tank } from './types'

const TOKEN_KEY='sdbrewpi.auth.token'
export const getStoredToken=()=>sessionStorage.getItem(TOKEN_KEY)
export const clearStoredToken=()=>sessionStorage.removeItem(TOKEN_KEY)

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  if (import.meta.env.VITE_STATIC_PREVIEW === 'true') {
    if (init?.method && init.method !== 'GET') throw new Error('Vista pública de demostración: los cambios están deshabilitados. Usa el servidor local para operar la planta.')
    const response = await fetch(`${import.meta.env.BASE_URL}preview.json`)
    if (!response.ok) throw new Error('No se pudieron cargar los datos de demostración.')
    const snapshot = await response.json() as Record<string, T>
    const data = snapshot[url.split('?')[0]]
    if (data === undefined) throw new Error('Esta consulta no está incluida en la demostración pública.')
    return data
  }
  const token=getStoredToken()
  const response = await fetch(url, { ...init, headers: { 'Content-Type': 'application/json', 'X-Actor': 'local-webapp', ...(token?{Authorization:`Bearer ${token}`} : {}), ...init?.headers } })
  if (!response.ok) {
    const raw = await response.text()
    const message = (() => {
      try { return (JSON.parse(raw) as { message?: string }).message ?? '' }
      catch { return raw.trim() }
    })()
    throw new Error(message || `No se pudo completar la operación (HTTP ${response.status})`)
  }
  if (response.status === 204) return undefined as T
  return response.json()
}

export const login = async (username:string,password:string) => {
  const session=await request<AuthSession>('/api/v1/auth/login',{method:'POST',body:JSON.stringify({username,password})})
  if(session.token)sessionStorage.setItem(TOKEN_KEY,session.token)
  return session
}
export const getAuthSession = () => request<AuthSession>('/api/v1/auth/session')
export const logout = async () => {try{await request<void>('/api/v1/auth/logout',{method:'POST'})}finally{clearStoredToken()}}
export const registerUser = (values:{username:string;displayName:string;role:string;password:string}) => request<AuthUser>('/api/v1/users',{method:'POST',body:JSON.stringify(values)})

export const getOverview = () => request<Overview>('/api/v1/fermentation/overview')
export const getTankHistory = (tankId: string) => request<FermentationHistory>(`/api/v1/fermentation/tanks/${tankId}/history?hours=24&limit=720`)
export const getAlarmHistory = () => request<AlarmHistory>('/api/v1/fermentation/alarms?hours=168&limit=200')
export const getProductionOverview = () => request<ProductionOverview>('/api/v1/production/overview')
export const getPlantOverview = () => request<PlantOverview>('/api/v1/plant/overview')
export const getInventoryOverview = () => request<InventoryOverview>('/api/v1/inventory/overview')
export const createInventoryItem = (siteId:string, values:{code:string;name:string;category:string;baseUnit:string;trackLots:boolean;minimumStock:number;notes:string}) => request<InventoryItem>(`/api/v1/inventory/sites/${siteId}/items`,{method:'POST',body:JSON.stringify(values)})
export const createInventoryLot = (itemId:string, values:{internalCode:string;supplierLot:string;expiryDate:string|null;qualityStatus:string;notes:string}) => request<InventoryLot>(`/api/v1/inventory/items/${itemId}/lots`,{method:'POST',body:JSON.stringify(values)})
export const createInventoryMovement = (values:{itemId:string;lotId:string|null;warehouseId:string;locationId:string|null;movementType:string;quantity:number;reference:string;notes:string}) => request<InventoryMovement>('/api/v1/inventory/movements',{method:'POST',body:JSON.stringify(values)})

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

export const createPlantWarehouse = (siteId: string, values: PlantWarehouseInput) =>
  request<PlantWarehouse>(`/api/v1/plant/sites/${siteId}/warehouses`, {
    method: 'POST', body: JSON.stringify(values)
  })

export const updatePlantWarehouse = (warehouse: PlantWarehouse, values: PlantWarehouseInput) =>
  request<PlantWarehouse>(`/api/v1/plant/warehouses/${warehouse.id}`, {
    method: 'PUT', body: JSON.stringify({ ...values, expectedRevision: warehouse.revision })
  })

export const retirePlantWarehouse = (warehouse: PlantWarehouse) =>
  request<PlantWarehouse>(`/api/v1/plant/warehouses/${warehouse.id}/retire`, {
    method: 'PUT', body: JSON.stringify({ expectedRevision: warehouse.revision })
  })

export const createStorageLocation = (warehouseId: string, values: PlantStorageLocationInput) =>
  request<PlantStorageLocation>(`/api/v1/plant/warehouses/${warehouseId}/locations`, {
    method: 'POST', body: JSON.stringify(values)
  })

export const updateStorageLocation = (location: PlantStorageLocation, values: PlantStorageLocationInput) =>
  request<PlantStorageLocation>(`/api/v1/plant/locations/${location.id}`, {
    method: 'PUT', body: JSON.stringify({ ...values, expectedRevision: location.revision })
  })

export const retireStorageLocation = (location: PlantStorageLocation) =>
  request<PlantStorageLocation>(`/api/v1/plant/locations/${location.id}/retire`, {
    method: 'PUT', body: JSON.stringify({ expectedRevision: location.revision })
  })

export const setMode = (tank: Tank, mode: ControlMode) => request('/api/v1/fermentation/tanks/' + tank.id + '/mode', {
  method: 'PUT', body: JSON.stringify({ mode, expectedRevision: tank.revision })
})

export const setSetpoint = (tank: Tank, setpointC: number) => request('/api/v1/fermentation/tanks/' + tank.id + '/setpoint', {
  method: 'PUT', body: JSON.stringify({ setpointC, expectedRevision: tank.revision })
})

export const controlProfile = (batchId: string, action: 'start' | 'pause' | 'resume', expectedRevision: number) =>
  request(`/api/v1/production/batches/${batchId}/profile/${action}`, {
    method: 'PUT', body: JSON.stringify({ expectedRevision })
  })

export const acknowledgeAlarm = (alarm: FermentationAlarm, note: string) =>
  request<FermentationAlarm>(`/api/v1/fermentation/alarms/${alarm.id}/acknowledge`, {
    method: 'PUT', body: JSON.stringify({ expectedRevision: alarm.revision, note })
  })

export const getBatchEvents = (batchId: string) =>
  request<BatchEvent[]>(`/api/v1/production/batches/${batchId}/events`)

export const addBatchEvent = (batchId: string, values: BatchEventInput) =>
  request<BatchEvent>(`/api/v1/production/batches/${batchId}/events`, {
    method: 'POST', body: JSON.stringify(values)
  })
export const completeBatch = (batch: { id: string; revision: number }) =>
  request(`/api/v1/production/batches/${batch.id}/complete`, {
    method: 'PUT', body: JSON.stringify({ expectedRevision: batch.revision })
  })

export const releaseProductionOrder = (values: { recipeVersionId: string; plannedVolumeL: number; batchKind: 'TEST' | 'PILOT' | 'COMMERCIAL'; productCode: 'CERV' | 'HSEL' }) =>
  request<Batch>('/api/v1/production/orders/release', { method: 'POST', body: JSON.stringify(values) })

export const commandProductionStage = (batch: Batch, stageCode: string, values: {
  action: 'START' | 'COMPLETE' | 'SKIP'; expectedStageRevision: number; notes?: string;
  measuredValue?: number; unit?: string
}) => request<Batch>(`/api/v1/production/batches/${batch.id}/stages/${stageCode}`, {
  method: 'PUT', body: JSON.stringify({ ...values, expectedBatchRevision: batch.revision })
})

export const assignFermentation = (batch: Batch, tankId: string, transferredVolumeL: number) =>
  request<Batch>(`/api/v1/production/batches/${batch.id}/fermentation-assignment`, {
    method: 'PUT', body: JSON.stringify({ tankId, transferredVolumeL, expectedRevision: batch.revision })
  })

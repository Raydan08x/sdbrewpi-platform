export type ControlMode = 'OFF' | 'MANUAL' | 'AUTO'

export interface Tank {
  id: string
  name: string
  mode: ControlMode
  setpointC: number
  productTemperatureC: number
  gravity: number
  pillId: string
  pillQuality: string
  pillCapturedAt: string
  pillReceivedAt: string | null
  pillAgeSeconds: number
  pillBatteryPct: number | null
  pillRssiDbm: number | null
  pillSource: string
  coolingDemand: boolean
  revision: number
}

export interface Chiller {
  mode: string
  reservoirTemperatureC: number | null
  reservoirQuality: string
  pumpOn: boolean
  compressorRequest: boolean
  environment: string
  hardwareEnabled: boolean
  revision: number
}

export interface Overview {
  environment: string
  generatedAt: string
  tanks: Tank[]
  chiller: Chiller
  telemetry: {
    enabled: boolean
    connected: boolean
    lastMessageAt: string | null
    acceptedMessages: number
    rejectedMessages: number
    detail: string
  }
  plcDemo: {
    enabled: boolean
    connected: boolean
    port: string
    lastMessageAt: string | null
    acceptedMessages: number
    rejectedLines: number
    detail: string
  }
  alarms: FermentationAlarm[]
}

export interface FermentationAlarm {
  id: string
  targetId: string
  code: string
  severity: string
  status: string
  message: string
  source: string
  openedAt: string
  lastSeenAt: string
  clearedAt: string | null
  acknowledgedAt: string | null
  acknowledgedBy: string | null
  acknowledgmentNote: string | null
  revision: number
}

export interface AlarmHistory {
  from: string
  generatedAt: string
  alarms: FermentationAlarm[]
}

export interface FermentationMeasurement {
  capturedAt: string
  receivedAt: string
  temperatureC: number | null
  gravity: number | null
  quality: string
  source: string
}

export interface FermentationHistory {
  tankId: string
  from: string
  generatedAt: string
  samples: FermentationMeasurement[]
}

export interface ProfileStep {
  order: number
  name: string
  targetTemperatureC: number
  durationHours: number
  rampRateCPerHour: number | null
}

export interface Recipe {
  id: string
  code: string
  name: string
  version: number
  originalGravity: number
  targetFinalGravity: number
  defaultVolumeL: number
  notes: string
  createdAt: string
  steps: ProfileStep[]
}

export interface Batch {
  id: string
  code: string
  recipeVersionId: string
  recipeCode: string
  recipeName: string
  recipeVersion: number
  tankId: string
  volumeL: number
  status: string
  currentStep: number
  profileState: 'NOT_STARTED' | 'RUNNING' | 'PAUSED' | 'COMPLETED'
  stepStartedAt: string | null
  stepExpectedCompleteAt: string | null
  stepElapsedSeconds: number
  profileCompletedAt: string | null
  startedAt: string
  expectedCompleteAt: string
  completedAt: string | null
  revision: number
  profile: ProfileStep[]
}

export interface BatchEvent {
  id: string
  occurredAt: string
  eventType: string
  stepOrder: number | null
  actor: string
  message: string
  materialName: string | null
  quantity: number | null
  unit: string | null
}

export interface BatchEventInput {
  eventType: string
  message: string
  materialName?: string
  quantity?: number
  unit?: string
}

export interface ProductionOverview {
  generatedAt: string
  recipes: Recipe[]
  activeBatches: Batch[]
  processStages: ProductionStage[]
}

export interface ProductionStage {
  code: string
  phase: string
  order: number
  name: string
  description: string
  optional: boolean
  variant: string
}

export interface PlantProfile {
  id: string
  code: string
  name: string
  companyName: string
  legalName: string
  taxId: string
  timezone: string
  currency: string
  nominalBatchCapacityL: number | null
  plannedFermenters: number
  pipingDeadVolumeL: number
  logoUrl: string
  revision: number
  updatedAt: string
}

export interface PlantAsset {
  id: string
  siteId: string
  code: string
  assetType: string
  name: string
  manufacturer: string
  model: string
  capacityL: number | null
  electricalSpec: string
  communicationProtocol: string
  deviceIdentifier: string
  firmwareProfile: string
  status: string
  controllable: boolean
  notes: string
  revision: number
  active: boolean
  updatedAt: string
}

export type PlantAssetInput = Omit<PlantAsset, 'id' | 'siteId' | 'revision' | 'active' | 'updatedAt'>

export interface PlantStorageLocation {
  id: string
  warehouseId: string
  code: string
  name: string
  locationType: string
  notes: string
  revision: number
  active: boolean
  updatedAt: string
}

export type PlantStorageLocationInput = Omit<PlantStorageLocation, 'id' | 'warehouseId' | 'revision' | 'active' | 'updatedAt'>

export interface PlantWarehouse {
  id: string
  siteId: string
  code: string
  name: string
  purpose: string
  temperatureControlled: boolean
  notes: string
  allowedCategories: string[]
  locations: PlantStorageLocation[]
  revision: number
  active: boolean
  updatedAt: string
}

export type PlantWarehouseInput = Omit<PlantWarehouse, 'id' | 'siteId' | 'locations' | 'revision' | 'active' | 'updatedAt'>

export interface PlantOverview {
  generatedAt: string
  site: PlantProfile
  assets: PlantAsset[]
  warehouses: PlantWarehouse[]
  onboarding: {
    status: string
    scannerEnabled: boolean
    hardwareOutputsEnabled: boolean
    detail: string
    supportedFirmwareProfiles: string[]
    requiredSteps: string[]
  }
  assetsNeedingData: number
}

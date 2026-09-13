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
}

export interface ProfileStep {
  order: number
  name: string
  targetTemperatureC: number
  durationHours: number
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
  startedAt: string
  expectedCompleteAt: string
  completedAt: string | null
  revision: number
  profile: ProfileStep[]
}

export interface ProductionOverview {
  generatedAt: string
  recipes: Recipe[]
  activeBatches: Batch[]
}

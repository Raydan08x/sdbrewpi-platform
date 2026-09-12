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

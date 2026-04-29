export interface AssessRequest {
  driverAge: number;
  licenseYears: number;
  violationsLast5Yr: number;
  accidentsLast5Yr: number;
  vehicleMake?: string;
  vehicleModel?: string;
  vehicleYear?: number;
  zipCode: string;
  stateCode?: string;
  coverageType: string;
  vehicleCategory?: string;
}

export interface AppliedFactor {
  key: string;
  label: string;
  scoreImpact: number;
  multiplier: number;
}

export interface AssessResponse {
  quoteId: string;
  riskScore: number;
  riskTier: 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH';
  annualPremiumUsd: number;
  monthlyPremiumUsd: number;
  coverageType: string;
  appliedFactors: AppliedFactor[];
  aiExplanation: string;
  createdAt: string;
}

export interface ChatRequest {
  sessionId: string;
  message: string;
  quoteContext?: string;
}

export interface ChatResponse {
  reply: string;
  sessionId: string;
  suggestedActions: string[];
  detectedAction?: string;
  quoteData?: AssessRequest;
}

export type RiskTier = 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH';

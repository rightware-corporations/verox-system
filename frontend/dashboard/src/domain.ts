export type Environment = 'TEST' | 'LIVE';

export type PaymentCoreState =
  | 'PENDING'
  | 'VERIFYING'
  | 'CONFIRMED'
  | 'REVIEW_REQUIRED'
  | 'FAILED'
  | 'EXPIRED';

export type PaymentEffectiveState = PaymentCoreState | 'MANUALLY_ACCEPTED';
export type CheckoutState = 'OPEN' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED';

export type MerchantAccount = {
  merchantId: string;
  merchantName: string;
  environment: Environment;
};

export type MerchantContext = MerchantAccount & {
  displayBrand?: string;
  primaryContact?: string;
};

export type Payment = {
  id: string;
  checkoutSessionId: string;
  externalReference: string;
  status: PaymentCoreState;
  effectiveStatus: PaymentEffectiveState;
  amount: string;
  currency: string;
  provider: string | null;
  confirmedAt: string | null;
  manuallyAcceptedAt: string | null;
};

export type CheckoutSession = {
  id: string;
  paymentId: string;
  externalReference: string;
  status: CheckoutState;
  paymentStatus: PaymentCoreState;
  amount: string;
  currency: string;
  description: string | null;
  checkoutUrl: string;
  expiresAt: string;
};

export type ManualAcceptance = {
  paymentId: string;
  status: 'MANUALLY_ACCEPTED';
  reason: string | null;
  acceptedAt: string;
};

export type OperationalCapability = {
  canUsePilotManualAcceptance: boolean;
  source: 'SERVER_CONFIGURATION' | 'UNAVAILABLE';
};

export type PaymentSemantic = {
  label: string;
  tone: 'neutral' | 'active' | 'success' | 'warning' | 'danger';
  provenance: 'CORE' | 'MANUAL';
};

export function paymentSemantic(status: PaymentEffectiveState): PaymentSemantic {
  switch (status) {
    case 'PENDING': return {label:'Pending verification', tone:'neutral', provenance:'CORE'};
    case 'VERIFYING': return {label:'Verifying', tone:'active', provenance:'CORE'};
    case 'CONFIRMED': return {label:'Confirmed', tone:'success', provenance:'CORE'};
    case 'REVIEW_REQUIRED': return {label:'Review required', tone:'warning', provenance:'CORE'};
    case 'FAILED': return {label:'Failed', tone:'danger', provenance:'CORE'};
    case 'EXPIRED': return {label:'Expired', tone:'neutral', provenance:'CORE'};
    case 'MANUALLY_ACCEPTED': return {label:'Accepted manually', tone:'warning', provenance:'MANUAL'};
  }
}

export function canAcceptManually(payment: Payment, capability: OperationalCapability): boolean {
  if (!capability.canUsePilotManualAcceptance) return false;
  if (payment.effectiveStatus === 'MANUALLY_ACCEPTED') return false;
  return payment.status === 'PENDING' || payment.status === 'REVIEW_REQUIRED';
}

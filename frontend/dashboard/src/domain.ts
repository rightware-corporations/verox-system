export type Environment = 'TEST' | 'LIVE';

export type PaymentCoreState =
  | 'PENDING'
  | 'VERIFYING'
  | 'CONFIRMED'
  | 'REVIEW_REQUIRED'
  | 'FAILED'
  | 'EXPIRED';

export type PaymentEffectiveState = PaymentCoreState | 'MANUALLY_ACCEPTED' | 'MANUALLY_REJECTED';
export type PaymentChannelStatus = 'ACTIVE' | 'INACTIVE';
export type CheckoutState = 'OPEN' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED';

export type OperatorSession = {
  operatorId: string;
  operatorDisplayName: string;
  merchantId: string;
  merchantName: string;
  environment: Environment;
};

export type MerchantAccount = {
  merchantId: string;
  merchantName: string;
  environment: Environment;
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
  manuallyRejectedAt: string | null;
  manualDecisionReason: string | null;
  customerEvidence: CustomerEvidence | null;
};

export type CustomerEvidence = {
  channel: string | null;
  amount: string;
  externalReference: string;
  submittedAt: string;
  message: string;
};

export type PaymentListItem = Payment & {
  description: string | null;
  attentionRequired: boolean;
  createdAt: string;
};

export type PaymentPage = {
  items: PaymentListItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

export type PaymentFeedQuery = {
  status?: PaymentCoreState | PaymentEffectiveState;
  attentionRequired?: boolean;
  page?: number;
  size?: number;
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

export type OperationalCapability = {
  canUsePilotManualAcceptance: boolean;
  source: 'SERVER_CONFIGURATION' | 'UNAVAILABLE';
};

export type ManualRejection = {
  paymentId: string;
  status: 'MANUALLY_REJECTED';
  reason: string | null;
  rejectedAt: string;
};

export type ManualAcceptance = {
  paymentId: string;
  status: 'MANUALLY_ACCEPTED';
  reason: string | null;
  acceptedAt: string;
};

export type PaymentChannel = {
  provider: string;
  displayName: string;
  kind: string;
  status: PaymentChannelStatus;
  recipientDisplay: string | null;
  recipientName: string | null;
  instructions: string | null;
  updatedAt: string;
};

export type PaymentSemantic = {
  label: string;
  tone: 'neutral' | 'active' | 'success' | 'warning' | 'danger';
  provenance: 'CORE' | 'MANUAL';
};

export function paymentSemantic(status: PaymentEffectiveState): PaymentSemantic {
  switch (status) {
    case 'PENDING': return {label:'Aguardando confirmação', tone:'neutral', provenance:'CORE'};
    case 'VERIFYING': return {label:'Em verificação', tone:'active', provenance:'CORE'};
    case 'CONFIRMED': return {label:'Pagamento confirmado', tone:'success', provenance:'CORE'};
    case 'REVIEW_REQUIRED': return {label:'Requer atenção', tone:'warning', provenance:'CORE'};
    case 'FAILED': return {label:'Pagamento não concluído', tone:'danger', provenance:'CORE'};
    case 'EXPIRED': return {label:'Pagamento expirado', tone:'neutral', provenance:'CORE'};
    case 'MANUALLY_ACCEPTED': return {label:'Pagamento confirmado', tone:'success', provenance:'MANUAL'};
    case 'MANUALLY_REJECTED': return {label:'Pagamento não recebido', tone:'danger', provenance:'MANUAL'};
  }
}

export function canRejectManually(payment: Payment): boolean {
  if (payment.effectiveStatus === 'MANUALLY_REJECTED' || payment.effectiveStatus === 'MANUALLY_ACCEPTED') return false;
  if (payment.status === 'CONFIRMED') return false;
  return payment.status === 'PENDING' || payment.status === 'REVIEW_REQUIRED';
}

export function canAcceptManually(payment: Payment): boolean {
  if (payment.effectiveStatus === 'MANUALLY_ACCEPTED') return false;
  if (payment.status === 'CONFIRMED') return false;
  return payment.status === 'PENDING' || payment.status === 'REVIEW_REQUIRED';
}

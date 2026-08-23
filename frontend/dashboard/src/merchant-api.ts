import type {CheckoutSession,ManualAcceptance,MerchantAccount,Payment} from './domain';

export type AccountWire = {
  merchant_id: string;
  merchant_name: string;
  environment: 'TEST' | 'LIVE';
};

export type PaymentWire = {
  id: string;
  checkout_session_id: string;
  external_reference: string;
  status: Payment['status'];
  effective_status: Payment['effectiveStatus'];
  amount: string;
  currency: string;
  provider: string | null;
  confirmed_at: string | null;
  manually_accepted_at: string | null;
};

export type CheckoutSessionWire = {
  id: string;
  payment_id: string;
  external_reference: string;
  status: CheckoutSession['status'];
  payment_status: CheckoutSession['paymentStatus'];
  amount: string;
  currency: string;
  description: string | null;
  checkout_url: string;
  expires_at: string;
};

export type ManualAcceptanceWire = {
  payment_id: string;
  status: 'MANUALLY_ACCEPTED';
  reason: string | null;
  accepted_at: string;
};

export class MerchantApiSecurityBoundaryError extends Error {
  constructor() {
    super('A secure server-owned Merchant API credential boundary is required. The browser must never receive a VEROX Merchant API key.');
    this.name = 'MerchantApiSecurityBoundaryError';
  }
}

export const merchantApiBoundary = {
  available: false as const,
  reason: 'frontend/dashboard is a browser-only Vite application; no BFF, API route, server action or secure proxy is present.',
};

export function mapAccount(dto: AccountWire): MerchantAccount {
  return {merchantId:dto.merchant_id, merchantName:dto.merchant_name, environment:dto.environment};
}

export function mapPayment(dto: PaymentWire): Payment {
  return {
    id:dto.id,
    checkoutSessionId:dto.checkout_session_id,
    externalReference:dto.external_reference,
    status:dto.status,
    effectiveStatus:dto.effective_status,
    amount:dto.amount,
    currency:dto.currency,
    provider:dto.provider,
    confirmedAt:dto.confirmed_at,
    manuallyAcceptedAt:dto.manually_accepted_at,
  };
}

export function mapCheckout(dto: CheckoutSessionWire): CheckoutSession {
  return {
    id:dto.id,
    paymentId:dto.payment_id,
    externalReference:dto.external_reference,
    status:dto.status,
    paymentStatus:dto.payment_status,
    amount:dto.amount,
    currency:dto.currency,
    description:dto.description,
    checkoutUrl:dto.checkout_url,
    expiresAt:dto.expires_at,
  };
}

export function mapManualAcceptance(dto: ManualAcceptanceWire): ManualAcceptance {
  return {paymentId:dto.payment_id,status:dto.status,reason:dto.reason,acceptedAt:dto.accepted_at};
}

function blocked(): never { throw new MerchantApiSecurityBoundaryError(); }

/**
 * These functions define the real Merchant API surface without leaking a secret.
 * They intentionally block until a server-owned integration boundary is added.
 */
export const merchantApi = {
  getAccount: async (): Promise<MerchantAccount> => blocked(),
  getPayment: async (_paymentId:string): Promise<Payment> => blocked(),
  getCheckout: async (_checkoutSessionId:string): Promise<CheckoutSession> => blocked(),
  createCheckout: async (_input:unknown,_idempotencyKey:string): Promise<CheckoutSession> => blocked(),
  acceptManually: async (_paymentId:string,_reason?:string): Promise<ManualAcceptance> => blocked(),
  getManualAcceptance: async (_paymentId:string): Promise<ManualAcceptance> => blocked(),
  configureWebhookEndpoint: async (_url:string): Promise<void> => blocked(),
};

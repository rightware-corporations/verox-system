import type {
  ManualAcceptance,
  ManualRejection,
  MerchantAccount,
  OperatorSession,
  Payment,
  PaymentChannel,
  PaymentCoreState,
  PaymentEffectiveState,
  PaymentFeedQuery,
  PaymentListItem,
  PaymentPage,
} from './domain';

const DEFAULT_BACKEND_ORIGIN = 'https://verox-backend-production.up.railway.app';
export const PLATFORM_BACKEND_ORIGIN = (import.meta.env.VITE_VEROX_BACKEND_BASE_URL || DEFAULT_BACKEND_ORIGIN).replace(/\/$/, '');

export class PlatformApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly retryAfter: string | null;

  constructor(status: number, code: string, message: string, retryAfter: string | null = null) {
    super(message);
    this.name = 'PlatformApiError';
    this.status = status;
    this.code = code;
    this.retryAfter = retryAfter;
  }
}

export class PlatformCsrfUnavailableError extends Error {
  constructor() {
    super('VEROX_CSRF is not readable from this frontend origin. A same-origin deployment/proxy or backend CSRF token delivery adjustment is required before browser mutations can succeed safely.');
    this.name = 'PlatformCsrfUnavailableError';
  }
}

type SessionWire = {
  operator_id: string;
  operator_display_name: string;
  merchant_id: string;
  merchant_name: string;
  environment: 'TEST' | 'LIVE';
  csrf_token?: string | null;
};

type PaymentWire = {
  id: string;
  checkout_session_id: string;
  external_reference: string;
  status: PaymentCoreState;
  effective_status: PaymentEffectiveState;
  amount: string;
  currency: string;
  provider: string | null;
  confirmed_at: string | null;
  manually_accepted_at: string | null;
  manually_rejected_at: string | null;
  manual_decision_reason: string | null;
  customer_evidence: {channel: string | null; amount: string; external_reference: string; submitted_at: string; message: string} | null;
};

type PaymentListItemWire = PaymentWire & {
  description: string | null;
  attention_required: boolean;
  created_at: string;
};

type PaymentPageWire = {
  items: PaymentListItemWire[];
  page: number;
  size: number;
  total_items: number;
  total_pages: number;
};

type ManualRejectionWire = {
  payment_id: string;
  status: 'MANUALLY_REJECTED';
  reason: string | null;
  rejected_at: string;
};

type ManualAcceptanceWire = {
  payment_id: string;
  status: 'MANUALLY_ACCEPTED';
  reason: string | null;
  accepted_at: string;
};

type PaymentChannelWire = {
  provider: string;
  display_name: string;
  kind: string;
  status: 'ACTIVE' | 'INACTIVE';
  recipient_display: string | null;
  recipient_name: string | null;
  instructions: string | null;
  updated_at: string;
};

type ApiErrorWire = {error?: {code?: string; message?: string}};

type RequestOptions = RequestInit & {csrf?: boolean};

let csrfToken: string | null = sessionStorage.getItem('verox_csrf_token');
function readCookie(name: string): string | null {
  const prefix = `${encodeURIComponent(name)}=`;
  for (const entry of document.cookie.split(';')) {
    const cookie = entry.trim();
    if (cookie.startsWith(prefix)) return decodeURIComponent(cookie.slice(prefix.length));
  }
  return null;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');

  if (options.csrf) {
    const csrf = csrfToken || readCookie('VEROX_CSRF');
    if (!csrf) throw new PlatformCsrfUnavailableError();
    headers.set('X-VEROX-CSRF', csrf);
  }

  let response: Response;
  try {
    response = await fetch(`${PLATFORM_BACKEND_ORIGIN}${path}`, {
      ...options,
      headers,
      credentials: 'include',
    });
  } catch {
    throw new PlatformApiError(0, 'NETWORK_ERROR', 'Não foi possível contactar o VEROX Server.');
  }

  if (!response.ok) {
    let body: ApiErrorWire | null = null;
    try { body = await response.json() as ApiErrorWire; } catch { body = null; }
    throw new PlatformApiError(
      response.status,
      body?.error?.code || `HTTP_${response.status}`,
      body?.error?.message || 'O VEROX Server devolveu um erro.',
      response.headers.get('Retry-After'),
    );
  }

  if (response.status === 204 || response.headers.get('content-length') === '0') return undefined as T;
  return response.json() as Promise<T>;
}

function mapSession(dto: SessionWire): OperatorSession {
  return {
    operatorId: dto.operator_id,
    operatorDisplayName: dto.operator_display_name,
    merchantId: dto.merchant_id,
    merchantName: dto.merchant_name,
    environment: dto.environment,
  };
}

function mapPayment(dto: PaymentWire): Payment {
  return {
    id: dto.id,
    checkoutSessionId: dto.checkout_session_id,
    externalReference: dto.external_reference,
    status: dto.status,
    effectiveStatus: dto.effective_status,
    amount: dto.amount,
    currency: dto.currency,
    provider: dto.provider,
    confirmedAt: dto.confirmed_at,
    manuallyAcceptedAt: dto.manually_accepted_at,
    manuallyRejectedAt: dto.manually_rejected_at,
    manualDecisionReason: dto.manual_decision_reason,
    customerEvidence: dto.customer_evidence ? {channel: dto.customer_evidence.channel, amount: dto.customer_evidence.amount, externalReference: dto.customer_evidence.external_reference, submittedAt: dto.customer_evidence.submitted_at, message: dto.customer_evidence.message} : null,
  };
}

function mapPaymentItem(dto: PaymentListItemWire): PaymentListItem {
  return {
    ...mapPayment(dto),
    description: dto.description,
    attentionRequired: dto.attention_required,
    createdAt: dto.created_at,
  };
}

function mapPaymentPage(dto: PaymentPageWire): PaymentPage {
  return {
    items: dto.items.map(mapPaymentItem),
    page: dto.page,
    size: dto.size,
    totalItems: dto.total_items,
    totalPages: dto.total_pages,
  };
}

function mapChannel(dto: PaymentChannelWire): PaymentChannel {
  return {
    provider: dto.provider,
    displayName: dto.display_name,
    kind: dto.kind,
    status: dto.status,
    recipientDisplay: dto.recipient_display,
    recipientName: dto.recipient_name,
    instructions: dto.instructions,
    updatedAt: dto.updated_at,
  };
}

function mapRejection(dto: ManualRejectionWire): ManualRejection { return {paymentId: dto.payment_id, status: dto.status, reason: dto.reason, rejectedAt: dto.rejected_at}; }

function mapAcceptance(dto: ManualAcceptanceWire): ManualAcceptance {
  return {
    paymentId: dto.payment_id,
    status: dto.status,
    reason: dto.reason,
    acceptedAt: dto.accepted_at,
  };
}

export const platformApi = {
  login: async (username: string, password: string): Promise<OperatorSession> => {
    const dto = await request<SessionWire>('/platform/v1/auth/login', {method: 'POST', body: JSON.stringify({username, password})});
    csrfToken = dto.csrf_token || null;
    if (csrfToken) sessionStorage.setItem('verox_csrf_token', csrfToken);
    return mapSession(dto);
  },

  session: async (): Promise<OperatorSession> =>
    mapSession(await request<SessionWire>('/platform/v1/auth/session')),

  logout: async (): Promise<void> => { await request<void>('/platform/v1/auth/logout', {method: 'POST', csrf: true}); csrfToken = null; sessionStorage.removeItem('verox_csrf_token'); },

  account: async (): Promise<MerchantAccount> =>
    mapSession(await request<SessionWire>('/platform/v1/account')),

  payments: async (query: PaymentFeedQuery = {}): Promise<PaymentPage> => {
    const params = new URLSearchParams();
    if (query.status) params.set('status', query.status);
    if (query.attentionRequired !== undefined) params.set('attention_required', String(query.attentionRequired));
    if (query.page !== undefined) params.set('page', String(query.page));
    if (query.size !== undefined) params.set('size', String(query.size));
    const suffix = params.size ? `?${params.toString()}` : '';
    return mapPaymentPage(await request<PaymentPageWire>(`/platform/v1/payments${suffix}`));
  },

  payment: async (paymentId: string): Promise<Payment> =>
    mapPayment(await request<PaymentWire>(`/platform/v1/payments/${encodeURIComponent(paymentId)}`)),

  acceptManually: async (paymentId: string, reason?: string): Promise<ManualAcceptance> =>
    mapAcceptance(await request<ManualAcceptanceWire>(`/platform/v1/payments/${encodeURIComponent(paymentId)}/manual-acceptance`, {
      method: 'POST',
      csrf: true,
      body: JSON.stringify({reason: reason?.trim() || null}),
    })),

  rejectManually: async (paymentId: string, reason?: string): Promise<ManualRejection> =>
    mapRejection(await request<ManualRejectionWire>(`/platform/v1/payments/${encodeURIComponent(paymentId)}/manual-rejection`, {
      method: 'POST', csrf: true, body: JSON.stringify({reason: reason?.trim() || null}),
    })),

  paymentChannels: async (): Promise<PaymentChannel[]> =>
    (await request<PaymentChannelWire[]>('/platform/v1/payment-channels')).map(mapChannel),
};

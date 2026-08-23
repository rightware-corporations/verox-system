export type CheckoutPaymentState = 'PENDING'|'VERIFYING'|'CONFIRMED'|'REVIEW_REQUIRED'|'FAILED'|'EXPIRED';
export type CheckoutEffectivePaymentState = CheckoutPaymentState|'MANUALLY_ACCEPTED';
export type CheckoutState = 'OPEN'|'COMPLETED'|'CANCELLED'|'EXPIRED';

export type HostedPaymentChannel = {
  provider:string;
  displayName:string;
  kind:string;
  enabled:boolean;
  recipientDisplay:string|null;
  recipientName:string|null;
  instructions:string|null;
};

export type HostedCheckout = {
  checkoutSessionId:string;
  paymentId:string;
  merchantDisplayName:string;
  externalReference:string;
  description:string|null;
  amount:string;
  currency:string;
  checkoutStatus:CheckoutState;
  paymentStatus:CheckoutPaymentState;
  effectivePaymentStatus:CheckoutEffectivePaymentState;
  expiresAt:string;
  successUrl:string;
  cancelUrl:string;
  paymentChannels:HostedPaymentChannel[];
};

type HostedPaymentChannelWire={
  provider:string;display_name:string;kind:string;enabled:boolean;
  recipient_display:string|null;recipient_name:string|null;instructions:string|null;
};
type HostedCheckoutWire={
  checkout_session_id:string;payment_id:string;merchant_display_name:string;external_reference:string;
  description:string|null;amount:string;currency:string;checkout_status:CheckoutState;
  payment_status:CheckoutPaymentState;effective_payment_status:CheckoutEffectivePaymentState;
  expires_at:string;success_url:string;cancel_url:string;payment_channels:HostedPaymentChannelWire[];
};
type ApiErrorWire={error?:{code?:string;message?:string}};

const DEFAULT_BACKEND_ORIGIN='https://verox-backend-production.up.railway.app';
export const CHECKOUT_BACKEND_ORIGIN=(import.meta.env.VITE_VEROX_BACKEND_BASE_URL||DEFAULT_BACKEND_ORIGIN).replace(/\/$/,'');

export class CheckoutApiError extends Error{
  readonly status:number;readonly code:string;
  constructor(status:number,code:string,message:string){super(message);this.name='CheckoutApiError';this.status=status;this.code=code}
}

function mapCheckout(dto:HostedCheckoutWire):HostedCheckout{return{
  checkoutSessionId:dto.checkout_session_id,paymentId:dto.payment_id,merchantDisplayName:dto.merchant_display_name,
  externalReference:dto.external_reference,description:dto.description,amount:dto.amount,currency:dto.currency,
  checkoutStatus:dto.checkout_status,paymentStatus:dto.payment_status,effectivePaymentStatus:dto.effective_payment_status,
  expiresAt:dto.expires_at,successUrl:dto.success_url,cancelUrl:dto.cancel_url,
  paymentChannels:dto.payment_channels.map(channel=>({provider:channel.provider,displayName:channel.display_name,kind:channel.kind,
    enabled:channel.enabled,recipientDisplay:channel.recipient_display,recipientName:channel.recipient_name,instructions:channel.instructions}))
}}

async function request<T>(checkoutSessionId:string,capability:string,path='',options:RequestInit={}):Promise<T>{
  const headers=new Headers(options.headers);headers.set('VEROX-Checkout-Capability',capability);
  if(options.body&&!headers.has('Content-Type'))headers.set('Content-Type','application/json');
  let response:Response;try{response=await fetch(`${CHECKOUT_BACKEND_ORIGIN}/public/v1/checkout/${encodeURIComponent(checkoutSessionId)}${path}`,{...options,headers})}
  catch{throw new CheckoutApiError(0,'NETWORK_ERROR','Não foi possível contactar a VEROX. O estado do pagamento não foi alterado.')}
  if(!response.ok){let body:ApiErrorWire|null=null;try{body=await response.json() as ApiErrorWire}catch{body=null}throw new CheckoutApiError(response.status,body?.error?.code||`HTTP_${response.status}`,body?.error?.message||'O checkout não está disponível.')}
  if(response.status===204||response.headers.get('content-length')==='0')return undefined as T;
  return response.json() as Promise<T>;
}

export const checkoutApi={
  bootstrap:async(checkoutSessionId:string,capability:string)=>mapCheckout(await request<HostedCheckoutWire>(checkoutSessionId,capability)),
  submitEvidence:async(checkoutSessionId:string,capability:string,content:string)=>request<unknown>(checkoutSessionId,capability,'/evidence/message',{method:'POST',body:JSON.stringify({content})}),
};

export function readCheckoutContext(){
  const match=window.location.pathname.match(/\/c\/(cs_[A-Za-z0-9_-]+)/);
  const checkoutSessionId=match?.[1]||new URLSearchParams(window.location.search).get('checkout')||'';
  const hash=new URLSearchParams(window.location.hash.replace(/^#/,''));
  const capability=hash.get('vx_capability')||'';
  return{checkoutSessionId,capability};
}

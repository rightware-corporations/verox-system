import type {ApiKeyRecord,ChannelRecord,CheckoutRecord,MerchantProfile,PaymentRecord,ReviewRecord,TeamMember,TimelineEvent,WebhookDeliveryRecord,WebhookEndpointRecord} from './domain';

// FRONTEND FALLBACK DATA ONLY. Replace mechanically with service/query data when the required
// Merchant API collection contracts are available. Never present this module as authoritative LIVE data.
export const fallbackMerchant: MerchantProfile = {id:'mer_demo_001',name:'Mapiko Store',lifecycle:'TEST_READY',environment:'TEST'};

export const fallbackPayments: PaymentRecord[] = [
{id:'pay_demo_84F2',externalReference:'ORD-1024',amount:1500,currency:'MZN',provider:'mpesa',state:'CONFIRMED',verificationState:'VERIFIED',checkoutId:'cs_demo_1024',createdAt:'23 Aug · 01:42',verifiedAt:'23 Aug · 01:44',environment:'TEST'},
{id:'pay_demo_91A7',externalReference:'ORD-1025',amount:850,currency:'MZN',provider:'mpesa',state:'VERIFYING',verificationState:'PROCESSING',checkoutId:'cs_demo_1025',createdAt:'23 Aug · 01:51',environment:'TEST'},
{id:'pay_demo_C310',externalReference:'ORD-1026',amount:2100,currency:'MZN',provider:'emola',state:'REVIEW_REQUIRED',verificationState:'REVIEW_REQUIRED',checkoutId:'cs_demo_1026',createdAt:'23 Aug · 01:54',environment:'TEST'},
];

export const fallbackCheckouts: CheckoutRecord[] = [
{id:'cs_demo_1024',externalReference:'ORD-1024',amount:1500,currency:'MZN',state:'COMPLETED',paymentState:'CONFIRMED',createdAt:'23 Aug · 01:40',expiresAt:'23 Aug · 02:10',paymentId:'pay_demo_84F2',environment:'TEST'},
{id:'cs_demo_1025',externalReference:'ORD-1025',amount:850,currency:'MZN',state:'EVIDENCE_SUBMITTED',paymentState:'VERIFYING',createdAt:'23 Aug · 01:49',expiresAt:'23 Aug · 02:19',paymentId:'pay_demo_91A7',environment:'TEST'},
{id:'cs_demo_1027',externalReference:'ORD-1027',amount:3200,currency:'MZN',state:'OPEN',createdAt:'23 Aug · 02:02',expiresAt:'23 Aug · 02:32',environment:'TEST'},
];

export const fallbackChannels: ChannelRecord[] = [
{provider:'mpesa',name:'M-Pesa',state:'ACTIVE',recipient:'+258 84 ••• ••••',health:'Verification channel connected',lastActivity:'2 min ago',environment:'TEST'},
{provider:'emola',name:'e-Mola',state:'VERIFICATION_REQUIRED',recipient:'+258 87 557 9796 · Owen de Jesus',health:'Backend contract required',environment:'TEST',action:'Requires backend enablement'},
{provider:'millennium-bim',name:'Millennium bim',state:'NOT_CONFIGURED',health:'Destination account not configured',environment:'TEST',action:'Configuration required'},
];

export const fallbackReviews: ReviewRecord[] = [
{paymentId:'pay_demo_C310',externalReference:'ORD-1026',amount:2100,provider:'emola',age:'18 min',state:'REVIEW_REQUIRED',reason:'Verification requires operational attention',environment:'TEST'}
];

export const fallbackTimeline: TimelineEvent[] = [
{id:'1',type:'checkout.created',timestamp:'01:40:08',title:'Checkout created',detail:'Checkout session created for ORD-1024.',relatedId:'cs_demo_1024',state:'neutral'},
{id:'2',type:'payment.started',timestamp:'01:42:11',title:'Payment started',detail:'Customer entered the payment flow.',state:'neutral'},
{id:'3',type:'evidence.received',timestamp:'01:43:58',title:'Evidence received',detail:'Official confirmation evidence submitted to VEROX.',state:'active'},
{id:'4',type:'verification.started',timestamp:'01:43:59',title:'Verification started',detail:'VEROX Server began authoritative verification.',state:'active'},
{id:'5',type:'payment.confirmed',timestamp:'01:44:02',title:'Payment confirmed',detail:'Payment truth changed to CONFIRMED by VEROX Server.',relatedId:'pay_demo_84F2',state:'success'},
{id:'6',type:'webhook.delivered',timestamp:'01:44:03',title:'Webhook delivered',detail:'payment.confirmed delivered successfully · HTTP 200.',relatedId:'evt_demo_104',state:'success'},
];

export const fallbackApiKeys: ApiKeyRecord[] = [
{id:'key_demo_1',name:'Test backend',prefix:'vx_test_',masked:'vx_test_••••••••••••••••4D8F',state:'ACTIVE',createdAt:'12 Aug 2026',lastUsed:'23 Aug 2026 · 01:47',environment:'TEST'}
];
export const fallbackWebhooks: WebhookEndpointRecord[] = [{id:'wh_demo_1',url:'https://merchant.example/webhooks/verox',state:'CONFIGURED',environment:'TEST',events:['payment.confirmed'],lastDelivery:'23 Aug · 01:44'}];
export const fallbackDeliveries: WebhookDeliveryRecord[] = [
{id:'evt_demo_104',event:'payment.confirmed',state:'DELIVERED',httpStatus:200,attempts:1,timestamp:'23 Aug · 01:44',environment:'TEST'},
{id:'evt_demo_103',event:'payment.confirmed',state:'RETRYING',httpStatus:500,attempts:3,timestamp:'23 Aug · 01:31',environment:'TEST'},
];
export const fallbackTeam: TeamMember[] = [
{id:'u1',name:'Merchant Owner',email:'owner@mapiko.example',role:'Owner',status:'ACTIVE',lastActive:'Now'},
{id:'u2',name:'Integration Developer',email:'dev@mapiko.example',role:'Developer',status:'ACTIVE',lastActive:'12 min ago'},
];

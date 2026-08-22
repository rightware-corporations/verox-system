import React, { useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  ArrowLeft,
  Check,
  CheckCircle2,
  ChevronRight,
  CircleAlert,
  Clock3,
  Copy,
  TriangleAlert,
  WifiOff,
} from 'lucide-react';
import './checkout-v2.css';
import './checkout-v2-tune.css';

type Screen = 'ready' | 'instructions' | 'evidence' | 'verifying' | 'confirmed' | 'review' | 'failed' | 'expired' | 'network' | 'invalid';
type ProviderId = 'MPESA' | 'EMOLA' | 'MILLENNIUM_BIM';

type Provider = {
  id: ProviderId;
  name: string;
  kind: string;
  monogram: string;
  enabled: boolean;
  recipient?: string;
  recipientName?: string;
  status: string;
};

const payment = {
  merchant: 'Mapiko Store',
  reference: 'ORD-1024',
  amount: '1 500,00 MZN',
  checkout: 'cs_demo1024',
};

const providers: Provider[] = [
  { id: 'MPESA', name: 'M-Pesa', kind: 'Mobile money', monogram: 'M', enabled: true, recipient: '+258 84 000 0000', status: 'Disponível' },
  { id: 'EMOLA', name: 'e-Mola', kind: 'Mobile money', monogram: 'e', enabled: false, recipient: '+258 87 557 9796', recipientName: 'Owen de Jesus', status: 'Em preparação' },
  { id: 'MILLENNIUM_BIM', name: 'Millennium bim', kind: 'Transferência bancária', monogram: 'bim', enabled: false, status: 'Em preparação' },
];

function Brand({ quiet = false }: { quiet?: boolean }) {
  return <div className={`v2-brand ${quiet ? 'quiet' : ''}`} aria-label="VEROX"><img src="/verox-mark.svg" alt="" aria-hidden="true"/><div className="v2-wordmark"><strong>VEROX</strong>{!quiet && <span>VERIFIED TRUTH.</span>}</div></div>;
}

function AppChrome({ children, screen }: { children: React.ReactNode; screen: Screen }) {
  const stage = useMemo(() => screen === 'ready' || screen === 'instructions' ? 1 : screen === 'evidence' ? 2 : 3, [screen]);
  return <main className="v2-page"><div className="v2-atmosphere" aria-hidden="true"><i/><i/><i/><i/></div><section className="v2-shell">
    <header className="v2-header"><Brand/><span className="v2-secure">Checkout seguro</span></header>
    <div className="v2-steps" aria-label={`Etapa ${stage} de 3`}>{['Pagamento','Comprovativo','Verificação'].map((label,index)=>{const n=index+1;return <div className={`v2-step ${stage===n?'current':''} ${stage>n?'complete':''}`} key={label}><span>{stage>n?<Check size={12}/>:n}</span><b>{label}</b></div>})}</div>
    <div className="v2-content">{children}</div>
    <footer className="v2-footer"><span>Protegido por</span><Brand quiet/></footer>
  </section></main>;
}

function TransactionHero() {
  return <section className="v2-hero"><div className="v2-hero-meta"><span>Pagamento para</span><strong>{payment.reference}</strong></div><h1>{payment.merchant}</h1><div className="v2-money"><strong>1 500,00</strong><span>MZN</span></div><p>Escolha o método de pagamento e siga as instruções apresentadas.</p></section>;
}

function ProviderMark({ provider }: { provider: Provider }) {
  return <span className={`v2-provider-mark ${provider.id.toLowerCase()}`}>{provider.monogram}</span>;
}

function ProviderCard({provider,active,onSelect}:{provider:Provider;active:boolean;onSelect:()=>void}) {
  return <button className={`v2-provider ${active?'active':''} ${!provider.enabled?'disabled':''}`} onClick={onSelect} disabled={!provider.enabled} aria-pressed={active} aria-label={`${provider.name}, ${provider.status}`}>
    <ProviderMark provider={provider}/><span className="v2-provider-copy"><strong>{provider.name}</strong><small>{provider.kind}</small></span><span className={`v2-provider-status ${provider.enabled?'available':'soon'}`}>{provider.status}</span>{provider.enabled&&<ChevronRight size={18}/>} 
  </button>;
}

function StickyAction({children,disabled,onClick}:{children:React.ReactNode;disabled?:boolean;onClick:()=>void}) {
  return <div className="v2-action-dock"><button className="v2-primary" disabled={disabled} onClick={onClick}>{children}<ChevronRight size={18}/></button></div>;
}

function CheckoutReady({onContinue}:{onContinue:()=>void}) {
  const [provider,setProvider]=useState<ProviderId>('MPESA');
  return <><TransactionHero/><section className="v2-section"><div className="v2-section-heading"><div><span className="v2-kicker">Método de pagamento</span><h2>Como pretende pagar?</h2></div><span className="v2-mini-trust">Canais VEROX</span></div><div className="v2-provider-list">{providers.map(p=><ProviderCard key={p.id} provider={p} active={provider===p.id} onSelect={()=>setProvider(p.id)}/>)}</div><div className="v2-note"><p>Os métodos só ficam disponíveis quando o VEROX Server os autoriza para este checkout.</p></div></section><StickyAction onClick={onContinue}>Continuar com M-Pesa</StickyAction></>;
}

function PaymentInstructions({onBack,onPaid}:{onBack:()=>void;onPaid:()=>void}) {
  const provider=providers[0]; const [copied,setCopied]=useState(false);
  const copy=async()=>{try{await navigator.clipboard.writeText(provider.recipient||'');setCopied(true);setTimeout(()=>setCopied(false),1500)}catch{setCopied(false)}};
  return <><Back onClick={onBack}>Voltar</Back><div className="v2-page-heading"><span className="v2-kicker">M-Pesa</span><h1>Faça o pagamento no seu telemóvel</h1><p>Use exatamente os dados apresentados abaixo e regresse à VEROX depois de receber a mensagem oficial.</p></div>
  <section className="v2-pay-card"><div className="v2-pay-card-brand"><ProviderMark provider={provider}/><div><strong>{provider.name}</strong><span>{provider.kind}</span></div></div><div className="v2-pay-card-top"><span>Valor a enviar</span><strong>{payment.amount}</strong></div><div className="v2-recipient"><div><span>Número receptor</span><strong>{provider.recipient}</strong></div><button onClick={copy} aria-label="Copiar número"><Copy size={17}/><span>{copied?'Copiado':'Copiar'}</span></button></div><div className="v2-pay-card-ref"><span>Referência VEROX</span><strong>{payment.reference}</strong></div></section>
  <section className="v2-howto"><h2>Concluir em três passos</h2>{[['01','Envie o valor exato','Envie exatamente 1 500,00 MZN para o número indicado.'],['02','Aguarde a mensagem oficial','Não avance antes de receber a confirmação completa do M-Pesa.'],['03','Regresse à VEROX','No próximo passo irá colar a mensagem oficial completa.']].map(([n,t,d])=><div className="v2-howto-row" key={n}><span>{n}</span><div><strong>{t}</strong><p>{d}</p></div></div>)}</section><div className="v2-critical"><p><strong>Importante:</strong> guarde a mensagem completa. Não altere valores, referência, telefone ou data.</p></div><StickyAction onClick={onPaid}>Já efectuei o pagamento</StickyAction></>;
}

function Evidence({onBack,onSubmit}:{onBack:()=>void;onSubmit:()=>void}) {
  const [value,setValue]=useState(''); const max=4096;
  return <><Back onClick={onBack}>Instruções de pagamento</Back><div className="v2-page-heading"><span className="v2-kicker">Comprovativo</span><h1>Confirmar pagamento</h1><p>Cole abaixo a mensagem completa de confirmação recebida após efectuar o pagamento.</p></div><div className="v2-evidence-guide"><div><strong>Uma mensagem. Sem reconstrução manual.</strong><p>A VEROX envia a evidência completa ao servidor para verificação.</p></div></div><label className="v2-field"><span>Mensagem de confirmação</span><textarea value={value} maxLength={max} onChange={e=>setValue(e.target.value)} placeholder="Cole aqui a mensagem completa de confirmação." autoComplete="off" spellCheck={false}/><div className="v2-field-meta"><small>Não edite nem reconstrua a mensagem.</small><small>{value.length}/{max}</small></div></label><div className="v2-privacy"><span>A evidência é usada apenas para o fluxo de verificação do pagamento.</span></div><StickyAction disabled={!value.trim()} onClick={onSubmit}>Verificar pagamento</StickyAction></>;
}

function VerificationLoader(){return <div className="v2-verification-loader" role="status" aria-label="Verificação em curso"><svg viewBox="0 0 160 120" aria-hidden="true"><path className="vl-frame" d="M33 25h58v70H33z"/><path className="vl-core" d="M61 43l20 12v24L61 91 41 79V55z"/><circle className="vl-node n1" cx="119" cy="30" r="4"/><circle className="vl-node n2" cx="132" cy="57" r="4"/><circle className="vl-node n3" cx="116" cy="89" r="4"/><path className="vl-line l1" d="M91 42l24-10"/><path className="vl-line l2" d="M91 60l36-2"/><path className="vl-line l3" d="M91 79l22 9"/></svg></div>}

function StateScreen({tone,icon,title,headline,body,children,action,onAction}:{tone:string;icon?:React.ReactNode;title:string;headline:string;body:string;children?:React.ReactNode;action?:string;onAction?:()=>void}) {
  return <div className={`v2-state ${tone}`}>{icon&&<div className="v2-state-icon">{icon}</div>}<span className="v2-kicker">{title}</span><h1>{headline}</h1><div className="v2-state-amount">{payment.amount}</div><p className="v2-state-body">{body}</p><div className="v2-state-meta"><div><span>Comerciante</span><strong>{payment.merchant}</strong></div><div><span>Método</span><strong>M-Pesa</strong></div><div><span>Referência</span><strong>{payment.reference}</strong></div></div>{children}{action&&<StickyAction onClick={onAction||(()=>{})}>{action}</StickyAction>}</div>;
}

function Back({onClick,children}:{onClick:()=>void;children:React.ReactNode}) {return <button className="v2-back" onClick={onClick}><ArrowLeft size={16}/>{children}</button>}

function App(){const[screen,setScreen]=useState<Screen>('ready');const[devOpen,setDevOpen]=useState(false);let content:React.ReactNode;switch(screen){
case'ready':content=<CheckoutReady onContinue={()=>setScreen('instructions')}/>;break;
case'instructions':content=<PaymentInstructions onBack={()=>setScreen('ready')} onPaid={()=>setScreen('evidence')}/>;break;
case'evidence':content=<Evidence onBack={()=>setScreen('instructions')} onSubmit={()=>setScreen('verifying')}/>;break;
case'verifying':content=<StateScreen tone="info" icon={<VerificationLoader/>} title="Verificação em curso" headline="Estamos a verificar o seu pagamento" body="Recebemos a confirmação e estamos a validar a transacção."><div className="v2-state-callout info"><span>Não precisa de enviar a mensagem novamente. O pagamento ainda não está confirmado.</span></div></StateScreen>;break;
case'confirmed':content=<StateScreen tone="success" icon={<CheckCircle2/>} title="Verificação concluída" headline="Pagamento confirmado" body="O seu pagamento foi verificado pela VEROX." action={`Voltar à ${payment.merchant}`}><div className="v2-receipt"><span>VEROX verification</span><strong>Confirmed</strong><small>{payment.checkout}</small></div></StateScreen>;break;
case'review':content=<StateScreen tone="warning" icon={<TriangleAlert/>} title="Revisão necessária" headline="Estamos a analisar o seu pagamento" body="Recebemos a informação, mas ainda não é seguro declarar o pagamento confirmado."><div className="v2-state-callout warning"><strong>Não efectue outro pagamento.</strong><span>A transacção pode já ter sido efectuada.</span></div></StateScreen>;break;
case'failed':content=<StateScreen tone="danger" icon={<CircleAlert/>} title="Não verificado" headline="Não foi possível verificar este pagamento" body="A VEROX não tem confirmação suficiente para declarar este pagamento como concluído." action="Tentar novamente" onAction={()=>setScreen('evidence')}/>;break;
case'expired':content=<StateScreen tone="neutral" icon={<Clock3/>} title="Checkout expirado" headline="Este checkout já não está activo" body="A sessão terminou e já não pode receber uma nova submissão de evidência." action="Voltar ao comerciante"/>;break;
case'network':content=<StateScreen tone="neutral" icon={<WifiOff/>} title="Ligação indisponível" headline="Não foi possível contactar a VEROX" body="O estado do seu pagamento não foi alterado. Quando recuperar a ligação, pode tentar novamente." action="Tentar novamente" onAction={()=>setScreen('verifying')}/>;break;
default:content=<StateScreen tone="neutral" icon={<CircleAlert/>} title="Checkout indisponível" headline="Este link não está disponível" body="O checkout pode ser inválido, ter expirado ou já não estar acessível."/>}
return <><AppChrome screen={screen}>{content}</AppChrome>{import.meta.env.DEV&&<div className={`v2-dev ${devOpen?'open':''}`}><button className="v2-dev-trigger" onClick={()=>setDevOpen(v=>!v)}>DEV</button>{devOpen&&<div className="v2-dev-menu"><strong>Preview state</strong>{(['ready','instructions','evidence','verifying','confirmed','review','failed','expired','network','invalid'] as Screen[]).map(s=><button key={s} className={screen===s?'active':''} onClick={()=>{setScreen(s);setDevOpen(false)}}>{s}</button>)}</div>}</div>}</>}

createRoot(document.getElementById('root')!).render(<React.StrictMode><App/></React.StrictMode>);

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
  LoaderCircle,
  LockKeyhole,
  MessageSquareText,
  RotateCw,
  ShieldCheck,
  Smartphone,
  TriangleAlert,
  WifiOff,
} from 'lucide-react';
import './checkout-v2.css';

type Screen =
  | 'ready'
  | 'instructions'
  | 'evidence'
  | 'verifying'
  | 'confirmed'
  | 'review'
  | 'failed'
  | 'expired'
  | 'network'
  | 'invalid';

type Provider = 'MPESA' | 'EMOLA';

const payment = {
  merchant: 'Mapiko Store',
  reference: 'ORD-1024',
  amount: '1 500,00 MZN',
  checkout: 'cs_demo1024',
  recipient: '+258 84 000 0000',
};

const providers = [
  { code: 'MPESA' as Provider, name: 'M-Pesa', monogram: 'M', enabled: true, hint: 'Disponível agora' },
  { code: 'EMOLA' as Provider, name: 'e-Mola', monogram: 'e', enabled: false, hint: 'Em breve' },
];

function Brand({ quiet = false }: { quiet?: boolean }) {
  return (
    <div className={`v2-brand ${quiet ? 'quiet' : ''}`} aria-label="VEROX">
      <img src="/verox-mark.svg" alt="" aria-hidden="true" />
      <div className="v2-wordmark">
        <strong>VEROX</strong>
        {!quiet && <span>VERIFIED TRUTH.</span>}
      </div>
    </div>
  );
}

function AppChrome({ children, screen }: { children: React.ReactNode; screen: Screen }) {
  const stage = useMemo(() => {
    if (screen === 'ready' || screen === 'instructions') return 1;
    if (screen === 'evidence') return 2;
    return 3;
  }, [screen]);

  return (
    <main className="v2-page">
      <div className="v2-atmosphere" aria-hidden="true"><i/><i/><i/></div>
      <section className="v2-shell">
        <header className="v2-header">
          <Brand />
          <div className="v2-secure"><LockKeyhole size={13}/><span>Secure checkout</span></div>
        </header>
        <div className="v2-steps" aria-label={`Etapa ${stage} de 3`}>
          {['Pagamento','Comprovativo','Verificação'].map((label,index) => {
            const n = index + 1;
            return <div className={`v2-step ${stage === n ? 'current' : ''} ${stage > n ? 'complete' : ''}`} key={label}>
              <span>{stage > n ? <Check size={12}/> : n}</span><b>{label}</b>
            </div>;
          })}
        </div>
        <div className="v2-content">{children}</div>
        <footer className="v2-footer">
          <ShieldCheck size={14}/><span>Verificação protegida por</span><Brand quiet />
        </footer>
      </section>
    </main>
  );
}

function TransactionHero({ align = 'center' }: { align?: 'center' | 'left' }) {
  return (
    <div className={`v2-hero ${align}`}>
      <span className="v2-kicker">Pagamento para</span>
      <h1>{payment.merchant}</h1>
      <div className="v2-amount">{payment.amount}</div>
      <div className="v2-reference"><span>Pedido</span><strong>{payment.reference}</strong></div>
    </div>
  );
}

function ProviderCard({provider, active, onSelect}:{provider:(typeof providers)[number],active:boolean,onSelect:()=>void}) {
  return (
    <button
      className={`v2-provider ${active ? 'active' : ''} ${!provider.enabled ? 'disabled' : ''}`}
      onClick={onSelect}
      disabled={!provider.enabled}
      aria-pressed={active}
    >
      <span className={`v2-provider-mark ${provider.code.toLowerCase()}`}>{provider.monogram}</span>
      <span className="v2-provider-copy"><strong>{provider.name}</strong><small>{provider.enabled ? 'Mobile money' : 'Backend integration required'}</small></span>
      <span className={`v2-provider-status ${provider.enabled ? 'available' : 'soon'}`}>{provider.hint}</span>
      {provider.enabled && <ChevronRight size={18}/>} 
    </button>
  );
}

function CheckoutReady({onContinue}:{onContinue:()=>void}) {
  const [provider,setProvider] = useState<Provider>('MPESA');
  return <>
    <TransactionHero />
    <section className="v2-section">
      <div className="v2-section-heading"><div><span className="v2-kicker">Método de pagamento</span><h2>Como pretende pagar?</h2></div><span className="v2-mini-trust"><ShieldCheck size={14}/> Métodos verificados</span></div>
      <div className="v2-provider-list">
        {providers.map(p => <ProviderCard key={p.code} provider={p} active={provider===p.code} onSelect={()=>setProvider(p.code)}/>) }
      </div>
      <div className="v2-note"><CircleAlert size={17}/><p>No MVP atual, apenas M-Pesa está operacional. Outros métodos aparecem somente quando o VEROX Server os disponibilizar.</p></div>
    </section>
    <button className="v2-primary" onClick={onContinue}>Continuar com M-Pesa <ChevronRight size={18}/></button>
  </>;
}

function PaymentInstructions({onBack,onPaid}:{onBack:()=>void,onPaid:()=>void}) {
  const [copied,setCopied] = useState(false);
  const copy = async () => {
    try { await navigator.clipboard.writeText(payment.recipient); setCopied(true); setTimeout(()=>setCopied(false),1600); } catch { setCopied(false); }
  };
  return <>
    <Back onClick={onBack}>Voltar</Back>
    <div className="v2-page-heading"><span className="v2-kicker">M-Pesa</span><h1>Faça o pagamento no seu telemóvel</h1><p>Use os dados abaixo exatamente como apresentados. O VEROX fará a verificação depois.</p></div>
    <section className="v2-pay-card">
      <div className="v2-pay-card-top"><span>Valor a enviar</span><strong>{payment.amount}</strong></div>
      <div className="v2-recipient"><div><span>Número receptor</span><strong>{payment.recipient}</strong></div><button onClick={copy} aria-label="Copiar número"><Copy size={18}/><span>{copied?'Copiado':'Copiar'}</span></button></div>
      <div className="v2-pay-provider"><span className="v2-provider-mark mpesa">M</span><div><strong>M-Pesa</strong><small>Canal de pagamento</small></div></div>
    </section>
    <section className="v2-howto">
      <h2>Depois de abrir o M-Pesa</h2>
      {[['01','Envie o valor exato','Envie exatamente 1 500,00 MZN para o número indicado.'],['02','Aguarde a mensagem oficial','Não avance antes de receber a confirmação completa do M-Pesa.'],['03','Regresse ao VEROX','Vai colar a mensagem oficial no próximo passo.']].map(([n,t,d])=><div className="v2-howto-row" key={n}><span>{n}</span><div><strong>{t}</strong><p>{d}</p></div></div>)}
    </section>
    <div className="v2-critical"><MessageSquareText size={18}/><p>Guarde a mensagem completa. Não altere valores, referência, telefone ou data.</p></div>
    <button className="v2-primary" onClick={onPaid}>Já fiz o pagamento <ChevronRight size={18}/></button>
  </>;
}

function Evidence({onBack,onSubmit}:{onBack:()=>void,onSubmit:()=>void}) {
  const [value,setValue] = useState('');
  const max = 4096;
  return <>
    <Back onClick={onBack}>Instruções de pagamento</Back>
    <div className="v2-page-heading"><span className="v2-kicker">Comprovativo</span><h1>Cole a mensagem completa</h1><p>Use a confirmação oficial recebida depois de efetuar o pagamento.</p></div>
    <div className="v2-evidence-guide"><div className="v2-guide-icon"><MessageSquareText size={21}/></div><div><strong>Uma mensagem. Sem reconstrução manual.</strong><p>O VEROX precisa do conteúdo completo para enviar a evidência ao servidor de forma segura.</p></div></div>
    <label className="v2-field">
      <span>Mensagem de confirmação</span>
      <textarea value={value} maxLength={max} onChange={e=>setValue(e.target.value)} placeholder="Cole aqui a mensagem oficial completa recebida do M-Pesa…" autoComplete="off" spellCheck={false}/>
      <div className="v2-field-meta"><small>Não edite nem remova partes da mensagem.</small><small>{value.length}/{max}</small></div>
    </label>
    <div className="v2-privacy"><LockKeyhole size={15}/><span>A mensagem é usada para verificação do pagamento. Não a guardamos no armazenamento local do browser.</span></div>
    <button className="v2-primary" disabled={!value.trim()} onClick={onSubmit}>Enviar para verificação <ChevronRight size={18}/></button>
  </>;
}

function StateScreen({tone,icon,title,headline,body,children,action,onAction}:{tone:string,icon:React.ReactNode,title:string,headline:string,body:string,children?:React.ReactNode,action?:string,onAction?:()=>void}) {
  return <div className={`v2-state ${tone}`}>
    <div className="v2-state-icon">{icon}</div>
    <span className="v2-kicker">{title}</span>
    <h1>{headline}</h1>
    <div className="v2-state-amount">{payment.amount}</div>
    <p className="v2-state-body">{body}</p>
    <div className="v2-state-meta"><div><span>Merchant</span><strong>{payment.merchant}</strong></div><div><span>Método</span><strong>M-Pesa</strong></div><div><span>Referência</span><strong>{payment.reference}</strong></div></div>
    {children}
    {action && <button className="v2-primary" onClick={onAction}>{action}<ChevronRight size={18}/></button>}
  </div>;
}

function Back({onClick,children}:{onClick:()=>void,children:React.ReactNode}) { return <button className="v2-back" onClick={onClick}><ArrowLeft size={16}/>{children}</button>; }

function App() {
  const [screen,setScreen] = useState<Screen>('ready');
  const [devOpen,setDevOpen] = useState(false);

  let content: React.ReactNode;
  switch(screen){
    case 'ready': content=<CheckoutReady onContinue={()=>setScreen('instructions')}/>; break;
    case 'instructions': content=<PaymentInstructions onBack={()=>setScreen('ready')} onPaid={()=>setScreen('evidence')}/>; break;
    case 'evidence': content=<Evidence onBack={()=>setScreen('instructions')} onSubmit={()=>setScreen('verifying')}/>; break;
    case 'verifying': content=<StateScreen tone="info" icon={<LoaderCircle className="v2-spin"/>} title="Verificação em curso" headline="Estamos a verificar o seu pagamento" body="Recebemos a sua mensagem de confirmação. O pagamento ainda não está confirmado."><div className="v2-state-callout info"><Clock3 size={17}/><span>Não precisa enviar a mensagem novamente. Esta página atualizará quando o servidor determinar o estado.</span></div></StateScreen>; break;
    case 'confirmed': content=<StateScreen tone="success" icon={<CheckCircle2/>} title="Verificação concluída" headline="Pagamento confirmado" body="O VEROX Server verificou o pagamento. Agora é seguro regressar ao merchant." action={`Voltar à ${payment.merchant}`} onAction={()=>{}}><div className="v2-receipt"><span>VEROX verification</span><strong>Confirmed</strong><small>{payment.checkout}</small></div></StateScreen>; break;
    case 'review': content=<StateScreen tone="warning" icon={<TriangleAlert/>} title="Revisão necessária" headline="Estamos a rever o seu pagamento" body="Recebemos a informação, mas ainda não é seguro declarar o pagamento confirmado."><div className="v2-state-callout warning"><TriangleAlert size={17}/><strong>Não faça outro pagamento.</strong><span>A transação pode já ter sido efetuada.</span></div></StateScreen>; break;
    case 'failed': content=<StateScreen tone="danger" icon={<CircleAlert/>} title="Não verificado" headline="Não foi possível verificar este pagamento" body="O VEROX não tem confirmação suficiente para declarar este pagamento como concluído." action="Tentar novamente" onAction={()=>setScreen('evidence')}/>; break;
    case 'expired': content=<StateScreen tone="neutral" icon={<Clock3/>} title="Checkout expirado" headline="Este checkout já não está ativo" body="A sessão terminou e já não pode receber uma nova submissão de evidência." action="Voltar ao merchant" onAction={()=>{}}/>; break;
    case 'network': content=<StateScreen tone="neutral" icon={<WifiOff/>} title="Ligação indisponível" headline="Não conseguimos contactar o VEROX" body="O estado do seu pagamento não foi alterado. Quando recuperar a ligação, pode tentar novamente." action="Tentar novamente" onAction={()=>setScreen('verifying')}/>; break;
    default: content=<StateScreen tone="neutral" icon={<CircleAlert/>} title="Checkout indisponível" headline="Este link não está disponível" body="O checkout pode ser inválido, ter expirado ou já não estar acessível."/>;
  }

  return <>
    <AppChrome screen={screen}>{content}</AppChrome>
    {import.meta.env.DEV && <div className={`v2-dev ${devOpen?'open':''}`}><button className="v2-dev-trigger" onClick={()=>setDevOpen(v=>!v)}>DEV</button>{devOpen&&<div className="v2-dev-menu"><strong>Preview state</strong>{(['ready','instructions','evidence','verifying','confirmed','review','failed','expired','network','invalid'] as Screen[]).map(s=><button key={s} className={screen===s?'active':''} onClick={()=>{setScreen(s);setDevOpen(false)}}>{s}</button>)}</div>}</div>}
  </>;
}

createRoot(document.getElementById('root')!).render(<React.StrictMode><App/></React.StrictMode>);

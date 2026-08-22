import React, { useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { ArrowRight, Check, Copy, LockKeyhole, MessageSquareText, RotateCw, ShieldCheck, TriangleAlert, WifiOff, X } from 'lucide-react';
import { VEROX_LOGO, VEROX_SYMBOL } from './verox-assets';
import './v3.css';

type Screen = 'ready'|'instructions'|'evidence'|'verifying'|'confirmed'|'review'|'failed'|'expired'|'network';

type Rail = {name:string; label:string; enabled:boolean; recipient?:string; note?:string};

const payment = {
  merchant: 'Mapiko Store',
  amount: '1 500,00',
  currency: 'MZN',
  reference: 'ORD-1024',
  checkout: 'cs_demo1024',
};

const rails: Rail[] = [
  {name:'M-Pesa', label:'Mobile money', enabled:true, recipient:'+258 84 000 0000'},
  {name:'e-Mola', label:'Mobile money', enabled:false, note:'Em breve'},
];

function App(){
  const [screen,setScreen]=useState<Screen>('ready');
  const [message,setMessage]=useState('');
  const [dev,setDev]=useState(false);
  const step = useMemo(()=> screen==='ready'||screen==='instructions'?1 : screen==='evidence'?2 : 3,[screen]);

  return <div className="v3-shell">
    <VectorField />
    <main className="v3-frame">
      <Header />
      <Stage step={step}/>
      <section className="v3-content">
        {screen==='ready' && <Ready onNext={()=>setScreen('instructions')}/>} 
        {screen==='instructions' && <Instructions onBack={()=>setScreen('ready')} onNext={()=>setScreen('evidence')}/>} 
        {screen==='evidence' && <Evidence message={message} onChange={setMessage} onBack={()=>setScreen('instructions')} onNext={()=>setScreen('verifying')}/>} 
        {screen==='verifying' && <Verifying/>}
        {screen==='confirmed' && <Confirmed/>}
        {screen==='review' && <Review/>}
        {screen==='failed' && <Failed onRetry={()=>setScreen('evidence')}/>} 
        {screen==='expired' && <Expired/>}
        {screen==='network' && <Network onRetry={()=>setScreen('verifying')}/>} 
      </section>
      <Footer />
      <button className="dev-trigger" onClick={()=>setDev(!dev)}>DEV</button>
      {dev && <DevNav current={screen} setScreen={(s)=>{setScreen(s);setDev(false)}}/>}
    </main>
  </div>
}

function Header(){return <header className="v3-header"><img src={VEROX_LOGO} alt="VEROX"/><div className="secure-mark"><LockKeyhole size={13}/> Secure checkout</div></header>}

function Stage({step}:{step:number}){return <nav className="stage" aria-label="Progresso do checkout">{['Pagamento','Comprovativo','Verificação'].map((label,i)=>{const n=i+1;return <div key={label} className={`stage-item ${n<step?'done':''} ${n===step?'active':''}`}><span>{n<step?<Check size={12}/>:String(n).padStart(2,'0')}</span><strong>{label}</strong></div>})}</nav>}

function TransactionHead({compact=false}:{compact?:boolean}){return <section className={`transaction-head ${compact?'compact':''}`}><div><p>Pagamento para</p><h1>{payment.merchant}</h1><span>Pedido {payment.reference}</span></div><div className="money"><strong>{payment.amount}</strong><em>{payment.currency}</em></div></section>}

function Ready({onNext}:{onNext:()=>void}){return <>
  <TransactionHead/>
  <section className="rail-block"><div className="section-kicker"><span>Método de pagamento</span><small><ShieldCheck size={13}/> Métodos verificados</small></div><h2>Como pretende pagar?</h2>
    <div className="rail-list">{rails.map((r,i)=><button key={r.name} disabled={!r.enabled} className={`rail-row ${r.enabled?'enabled':'disabled'}`} onClick={r.enabled?onNext:undefined}><span className="rail-index">{String(i+1).padStart(2,'0')}</span><span className="rail-name"><strong>{r.name}</strong><small>{r.label}</small></span><span className="rail-state">{r.enabled?'Disponível':r.note}</span>{r.enabled&&<ArrowRight size={17}/>}</button>)}</div>
  </section>
  <aside className="truth-note"><span>VEROX</span><p>O método só avança quando existe suporte operacional no servidor.</p></aside>
  <Primary onClick={onNext}>Continuar com M-Pesa</Primary>
</>}

function Instructions({onBack,onNext}:{onBack:()=>void;onNext:()=>void}){return <>
  <Back onClick={onBack}/>
  <div className="editorial-title"><p>M-Pesa</p><h1>Faça o pagamento<br/>no seu telemóvel.</h1><span>Use os dados abaixo exatamente como apresentados. O VEROX verifica depois.</span></div>
  <section className="payment-terminal"><div className="terminal-grid"><div><small>Valor a enviar</small><strong>{payment.amount} <em>{payment.currency}</em></strong></div><div><small>Número receptor</small><strong>+258 84 000 0000</strong></div></div><button className="copy-btn" onClick={()=>navigator.clipboard?.writeText('+258 84 000 0000')}><Copy size={16}/> Copiar</button><div className="provider-line"><span className="provider-glyph">M</span><div><strong>M-Pesa</strong><small>Canal de pagamento</small></div></div></section>
  <section className="instruction-flow"><h2>Depois de abrir o M-Pesa</h2>{[['01','Envie o valor exato','Envie exatamente 1 500,00 MZN para o número indicado.'],['02','Aguarde a mensagem oficial','Não avance antes de receber a confirmação completa do M-Pesa.'],['03','Regresse ao VEROX','Vai colar a mensagem oficial no próximo passo.']].map(([n,t,d])=><div className="flow-row" key={n}><span>{n}</span><div><strong>{t}</strong><p>{d}</p></div></div>)}</section>
  <aside className="vector-tip"><MessageSquareText size={18}/><p>Guarde a mensagem completa. Não altere valores, referência, telefone ou data.</p></aside>
  <Primary onClick={onNext}>Já fiz o pagamento</Primary>
</>}

function Evidence({message,onChange,onBack,onNext}:{message:string;onChange:(v:string)=>void;onBack:()=>void;onNext:()=>void}){return <>
  <Back onClick={onBack}/>
  <div className="editorial-title"><p>Comprovativo</p><h1>Cole a mensagem<br/>completa.</h1><span>Use a confirmação oficial recebida depois de efetuar o pagamento.</span></div>
  <div className="evidence-principle"><MessageSquareText size={19}/><div><strong>Uma mensagem. Sem reconstrução manual.</strong><p>O VEROX precisa do conteúdo completo para enviar a evidência ao servidor.</p></div></div>
  <label className="message-field"><span>Mensagem de confirmação</span><textarea value={message} maxLength={4096} onChange={e=>onChange(e.target.value)} placeholder="Cole aqui a mensagem oficial completa recebida do M-Pesa…"/><small><span>Não edite nem remova partes da mensagem.</span><b>{message.length}/4096</b></small></label>
  <aside className="privacy"><LockKeyhole size={16}/><p>A mensagem é usada para verificação do pagamento. Não a guardamos no armazenamento local do browser.</p></aside>
  <Primary disabled={!message.trim()} onClick={onNext}>Enviar para verificação</Primary>
</>}

function Verifying(){return <StateLayout tone="info" eyebrow="Verificação em curso" title="Estamos a verificar o seu pagamento." icon={<RotateCw className="spin"/>}>
  <TransactionStrip/><div className="verification-rail"><span className="node done"></span><i></i><span className="node done"></span><i></i><span className="node live"></span></div><p className="state-copy">Recebemos a sua mensagem de confirmação. O pagamento ainda não está confirmado.</p><aside className="info-band">Não precisa enviar a mensagem novamente. Esta página atualizará quando o servidor determinar o estado.</aside>
</StateLayout>}

function Confirmed(){return <StateLayout tone="success" eyebrow="Verificação concluída" title="Pagamento confirmado." icon={<Check/>}>
  <section className="receipt"><div className="receipt-money"><strong>{payment.amount}</strong><span>{payment.currency}</span></div><div className="receipt-grid"><div><small>Merchant</small><b>{payment.merchant}</b></div><div><small>Método</small><b>M-Pesa</b></div><div><small>Referência</small><b>{payment.reference}</b></div><div><small>Checkout</small><b>{payment.checkout}</b></div></div><div className="receipt-seal"><img src={VEROX_SYMBOL}/><span>VEROX VERIFIED</span></div></section><p className="state-copy">O VEROX Server verificou o pagamento. Agora é seguro regressar ao merchant.</p><Primary onClick={()=>{}}>Voltar à Mapiko Store</Primary>
</StateLayout>}

function Review(){return <StateLayout tone="warning" eyebrow="Revisão necessária" title="Estamos a rever o seu pagamento." icon={<TriangleAlert/>}><TransactionStrip/><p className="state-copy">Recebemos a informação, mas ainda não é seguro declarar o pagamento confirmado.</p><aside className="warning-band"><TriangleAlert size={17}/><strong>Não faça outro pagamento.</strong><span>A transação pode já ter sido efetuada.</span></aside></StateLayout>}
function Failed({onRetry}:{onRetry:()=>void}){return <StateLayout tone="danger" eyebrow="Não verificado" title="Não foi possível verificar este pagamento." icon={<X/>}><TransactionStrip/><p className="state-copy">O VEROX não tem confirmação suficiente para declarar este pagamento como concluído.</p><Primary onClick={onRetry}>Tentar novamente</Primary></StateLayout>}
function Expired(){return <StateLayout tone="neutral" eyebrow="Checkout expirado" title="Este checkout já não está ativo." icon={<RotateCw/>}><TransactionStrip/><p className="state-copy">A sessão terminou e já não pode receber uma nova submissão de evidência.</p></StateLayout>}
function Network({onRetry}:{onRetry:()=>void}){return <StateLayout tone="neutral" eyebrow="Ligação indisponível" title="Não conseguimos contactar o VEROX." icon={<WifiOff/>}><TransactionStrip/><p className="state-copy">O estado do seu pagamento não foi alterado. Quando recuperar a ligação, pode tentar novamente.</p><Primary onClick={onRetry}>Tentar novamente</Primary></StateLayout>}

function StateLayout({tone,eyebrow,title,icon,children}:{tone:string;eyebrow:string;title:string;icon:React.ReactNode;children:React.ReactNode}){return <section className={`state-layout ${tone}`}><div className="state-mark">{icon}</div><p className="state-eyebrow">{eyebrow}</p><h1>{title}</h1>{children}</section>}
function TransactionStrip(){return <div className="transaction-strip"><span><small>Merchant</small><b>{payment.merchant}</b></span><span><small>Método</small><b>M-Pesa</b></span><span><small>Referência</small><b>{payment.reference}</b></span></div>}
function Back({onClick}:{onClick:()=>void}){return <button className="back-btn" onClick={onClick}>← Voltar</button>}
function Primary({children,onClick,disabled=false}:{children:React.ReactNode;onClick:()=>void;disabled?:boolean}){return <button className="primary-v3" onClick={onClick} disabled={disabled}><span>{children}</span><ArrowRight size={18}/></button>}
function Footer(){return <footer className="v3-footer"><div><ShieldCheck size={13}/> Verificação protegida por</div><img src={VEROX_SYMBOL} alt=""/><strong>VEROX</strong></footer>}
function DevNav({current,setScreen}:{current:Screen;setScreen:(s:Screen)=>void}){const screens:Screen[]=['ready','instructions','evidence','verifying','confirmed','review','failed','expired','network'];return <div className="dev-panel">{screens.map(s=><button className={current===s?'active':''} onClick={()=>setScreen(s)} key={s}>{s}</button>)}</div>}

function VectorField(){return <svg className="vector-field" viewBox="0 0 1200 1000" aria-hidden="true"><defs><linearGradient id="vg" x1="0" y1="0" x2="1" y2="1"><stop stopColor="#00d1d4"/><stop offset="1" stopColor="#0b6373"/></linearGradient></defs><path d="M80 160 H310 L410 260 H740 L880 120 H1110"/><path d="M60 690 H260 L390 560 H700 L860 720 H1140"/><path d="M180 910 L410 680 H790 L1010 900"/><circle cx="310" cy="160" r="5"/><circle cx="410" cy="260" r="5"/><circle cx="860" cy="720" r="5"/><polygon points="880,100 900,120 880,140 860,120" fill="url(#vg)"/></svg>}

createRoot(document.getElementById('root')!).render(<React.StrictMode><App/></React.StrictMode>)

import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import { ArrowLeft, Check, Copy, LoaderCircle, ShieldCheck, TriangleAlert } from 'lucide-react';
import './styles.css';

type Screen = 'ready' | 'instructions' | 'evidence' | 'verifying' | 'confirmed' | 'review';

const payment = { merchant: 'Mapiko Store', reference: 'ORD-1024', amount: '1 500,00 MZN', provider: 'M-Pesa', recipient: '+258 84 000 0000' };

function Mark() {
  return <div className="mark" aria-hidden="true"><span>V</span></div>;
}

function Header() {
  return <header className="brand"><Mark/><strong>VEROX</strong><span className="brand-line" /></header>;
}

function Summary({ compact = false }: { compact?: boolean }) {
  return <section className={`summary ${compact ? 'compact' : ''}`}>
    <p className="eyebrow">Payment to</p>
    <h1>{payment.merchant}</h1>
    <div className="amount">{payment.amount}</div>
    <p className="reference">{payment.reference}</p>
  </section>;
}

function App() {
  const [screen, setScreen] = useState<Screen>('ready');
  const [evidence, setEvidence] = useState('');

  const back = () => setScreen(screen === 'evidence' ? 'instructions' : 'ready');

  return <main className="page">
    <div className="ambient ambient-a"/><div className="ambient ambient-b"/>
    <section className="checkout-shell">
      <Header />
      <div className="progress" aria-label="Checkout progress"><i className={screen !== 'ready' ? 'done' : 'active'}/><i className={['evidence','verifying','confirmed','review'].includes(screen) ? 'done' : ''}/><i className={['verifying','confirmed','review'].includes(screen) ? 'done' : ''}/></div>

      {screen === 'ready' && <>
        <Summary />
        <div className="method-card"><div className="method-icon">M</div><div><span>Payment method</span><strong>{payment.provider}</strong></div><span className="available">Available</span></div>
        <button className="primary" onClick={() => setScreen('instructions')}>Continue with M-Pesa <span>→</span></button>
      </>}

      {screen === 'instructions' && <>
        <button className="back" onClick={back}><ArrowLeft size={17}/> Back</button>
        <Summary compact />
        <section className="panel"><p className="eyebrow">Pay with M-Pesa</p><h2>Send the exact amount</h2>
          <div className="copy-row"><div><span>Recipient</span><strong>{payment.recipient}</strong></div><button aria-label="Copy recipient"><Copy size={18}/></button></div>
          <ol><li>Open M-Pesa on your phone.</li><li>Send exactly <strong>{payment.amount}</strong>.</li><li>Wait for the official confirmation message.</li><li>Return here to verify the payment.</li></ol>
        </section>
        <button className="primary" onClick={() => setScreen('evidence')}>I’ve made the payment <span>→</span></button>
      </>}

      {screen === 'evidence' && <>
        <button className="back" onClick={back}><ArrowLeft size={17}/> Payment instructions</button>
        <div className="section-head"><p className="eyebrow">Verification</p><h1>Confirm your payment</h1><p>Paste the complete official confirmation message you received after paying.</p></div>
        <label className="evidence-field"><span>Confirmation message</span><textarea value={evidence} onChange={e => setEvidence(e.target.value)} placeholder="Paste the complete confirmation message here"/><small>Keep the message complete and unchanged.</small></label>
        <button className="primary" disabled={!evidence.trim()} onClick={() => setScreen('verifying')}>Verify payment <span>→</span></button>
      </>}

      {screen === 'verifying' && <State icon={<LoaderCircle className="spin"/>} tone="info" title="We’re verifying your payment" text="We received your confirmation message and are checking the payment. This is not yet a confirmation." action="Simulate server result" onAction={() => setScreen('confirmed')} secondary="Needs review" onSecondary={() => setScreen('review')}/>} 

      {screen === 'confirmed' && <State icon={<Check/>} tone="success" title="Payment confirmed" text="Your payment has been verified by VEROX." action={`Return to ${payment.merchant}`} onAction={() => {}}/>}

      {screen === 'review' && <State icon={<TriangleAlert/>} tone="warning" title="We’re reviewing your payment" text="VEROX received your information but cannot safely confirm the payment yet. Do not make another payment." action="Check status again" onAction={() => setScreen('verifying')}/>} 

      <footer><ShieldCheck size={15}/><span>Verification powered by <strong>VEROX</strong></span></footer>
    </section>
  </main>;
}

function State({icon,tone,title,text,action,onAction,secondary,onSecondary}:{icon:React.ReactNode,tone:string,title:string,text:string,action:string,onAction:()=>void,secondary?:string,onSecondary?:()=>void}) {
  return <section className="state"><div className={`state-icon ${tone}`}>{icon}</div><p className="eyebrow">VEROX verification</p><h1>{title}</h1><div className="state-amount">{payment.amount}</div><p>{text}</p><div className="state-summary"><span>{payment.merchant}</span><span>{payment.provider}</span><span>{payment.reference}</span></div><button className="primary" onClick={onAction}>{action}</button>{secondary && <button className="secondary" onClick={onSecondary}>{secondary}</button>}</section>
}

createRoot(document.getElementById('root')!).render(<React.StrictMode><App /></React.StrictMode>);

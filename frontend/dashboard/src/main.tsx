import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, NavLink, Navigate, Route, Routes } from 'react-router-dom';
import {
  Activity,
  Bell,
  ChevronRight,
  CircleHelp,
  Code2,
  CreditCard,
  ExternalLink,
  KeyRound,
  LayoutDashboard,
  Menu,
  Search,
  Settings,
  ShieldCheck,
  WalletCards,
  Webhook,
  X,
} from 'lucide-react';
import './styles.css';

type Metric = {
  label: string;
  value: string;
  helper: string;
};

const previewMetrics: Metric[] = [
  { label: 'Volume verificado', value: '—', helper: 'Aguardando API agregada' },
  { label: 'Pagamentos', value: '—', helper: 'Aguardando endpoint de lista' },
  { label: 'Confirmados', value: '—', helper: 'Fonte: VEROX Server' },
  { label: 'Em revisão', value: '—', helper: 'Sem dados simulados' },
];

const navigation = [
  { to: '/app/overview', label: 'Overview', icon: LayoutDashboard },
  { to: '/app/payments', label: 'Payments', icon: CreditCard },
  { to: '/app/developers', label: 'Developers', icon: Code2 },
  { to: '/app/api-keys', label: 'API Keys', icon: KeyRound },
  { to: '/app/webhooks', label: 'Webhooks', icon: Webhook },
  { to: '/app/settings', label: 'Settings', icon: Settings },
];

function VeroxBrand() {
  return <div className="brand" aria-label="VEROX"><span className="brand-mark" aria-hidden="true">V</span><div><strong>VEROX</strong><small>MERCHANT</small></div></div>;
}

function Sidebar({open,onClose}:{open:boolean;onClose:()=>void}) {
  return <aside className={`sidebar ${open?'open':''}`}>
    <div className="sidebar-head"><VeroxBrand/><button className="mobile-close" onClick={onClose} aria-label="Fechar navegação"><X size={20}/></button></div>
    <div className="merchant-context"><span>Workspace</span><strong>Mapiko Store</strong><small>TEST ENVIRONMENT</small></div>
    <nav className="nav" aria-label="Merchant Platform">{navigation.map(({to,label,icon:Icon})=><NavLink key={to} to={to} onClick={onClose} className={({isActive})=>isActive?'active':''}><Icon size={18}/><span>{label}</span></NavLink>)}</nav>
    <div className="sidebar-foot"><div className="trust"><ShieldCheck size={17}/><div><strong>VEROX Server</strong><span>Payment truth authority</span></div></div><button><CircleHelp size={17}/>Support</button></div>
  </aside>;
}

function Topbar({onMenu}:{onMenu:()=>void}) {
  return <header className="topbar"><button className="menu-btn" onClick={onMenu} aria-label="Abrir navegação"><Menu size={20}/></button><div className="search"><Search size={17}/><input aria-label="Pesquisar" placeholder="Pesquisar pagamento ou referência"/></div><div className="top-actions"><span className="env">TEST</span><button aria-label="Notificações"><Bell size={18}/></button><button className="avatar" aria-label="Conta">MS</button></div></header>;
}

function MetricGrid(){return <section className="metrics" aria-label="Resumo de pagamentos">{previewMetrics.map(m=><article key={m.label}><span>{m.label}</span><strong>{m.value}</strong><small>{m.helper}</small></article>)}</section>}

function Overview(){return <div className="page-stack"><section className="page-intro"><div><span className="eyebrow">MERCHANT OVERVIEW</span><h1>Operação de pagamentos</h1><p>Visibilidade operacional sem ultrapassar a verdade disponível no VEROX Server.</p></div><button className="primary-action"><WalletCards size={17}/>Criar checkout<ExternalLink size={15}/></button></section>
  <div className="contract-banner"><Activity size={18}/><div><strong>Frontend preparado. Dados agregados ainda não existem no contrato atual.</strong><span>Este overview não apresenta métricas fictícias. Os cartões ativam quando o Merchant API disponibilizar lista e agregações.</span></div></div>
  <MetricGrid/>
  <section className="overview-grid"><article className="panel large"><div className="panel-head"><div><span className="eyebrow">PAYMENTS</span><h2>Atividade recente</h2></div><NavLink to="/app/payments">Ver pagamentos <ChevronRight size={15}/></NavLink></div><div className="empty-operational"><div className="empty-symbol"><CreditCard size={24}/></div><strong>Lista de pagamentos ainda não disponível</strong><p>O backend atual permite consultar um pagamento por ID, mas não expõe uma coleção merchant-scoped.</p><code>GET /v1/payments/{'{paymentId}'}</code></div></article>
    <aside className="panel rail"><div className="panel-head"><div><span className="eyebrow">INTEGRATION</span><h2>Estado técnico</h2></div></div><div className="status-row"><span className="dot ok"/><div><strong>Account API</strong><small>GET /v1/account</small></div><b>READY</b></div><div className="status-row"><span className="dot pending"/><div><strong>Payment collection</strong><small>Listagem merchant-scoped</small></div><b>PENDING</b></div><div className="status-row"><span className="dot pending"/><div><strong>Overview metrics</strong><small>Agregações operacionais</small></div><b>PENDING</b></div></aside>
  </section></div>}

function Placeholder({title,description,contract}:{title:string;description:string;contract?:string}){return <div className="page-stack"><section className="page-intro"><div><span className="eyebrow">MERCHANT PLATFORM</span><h1>{title}</h1><p>{description}</p></div></section><article className="panel placeholder"><strong>Surface prepared</strong><p>Esta página entra na próxima sequência de implementação. Nenhuma funcionalidade server-side será simulada como real.</p>{contract&&<code>{contract}</code>}</article></div>}

function AppShell(){const[open,setOpen]=React.useState(false);return <div className="app-shell"><Sidebar open={open} onClose={()=>setOpen(false)}/><div className="workspace"><Topbar onMenu={()=>setOpen(true)}/><main className="content"><Routes><Route path="/app/overview" element={<Overview/>}/><Route path="/app/payments" element={<Placeholder title="Payments" description="Pesquisa, filtros e detalhe de pagamentos merchant-scoped." contract="GET /v1/payments/{paymentId}"/>}/><Route path="/app/developers" element={<Placeholder title="Developers" description="Integração técnica, documentação e estado dos recursos VEROX."/>}/><Route path="/app/api-keys" element={<Placeholder title="API Keys" description="Gestão segura das credenciais de integração do merchant."/>}/><Route path="/app/webhooks" element={<Placeholder title="Webhooks" description="Configuração e saúde da entrega de eventos ao merchant." contract="PUT /v1/webhook-endpoint"/>}/><Route path="/app/settings" element={<Placeholder title="Settings" description="Configuração do workspace, métodos e experiência do merchant."/>}/><Route path="*" element={<Navigate to="/app/overview" replace/>}/></Routes></main></div>{open&&<button className="scrim" aria-label="Fechar navegação" onClick={()=>setOpen(false)}/>}</div>}

createRoot(document.getElementById('root')!).render(<React.StrictMode><BrowserRouter><AppShell/></BrowserRouter></React.StrictMode>);

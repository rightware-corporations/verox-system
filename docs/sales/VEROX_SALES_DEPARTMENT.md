# VEROX Sales & Integration Department

## Purpose

This document is the internal source of truth for the VEROX Sales & Integration Department.

It exists to keep sales, merchant onboarding, commercial communication, objection handling and integration conversations consistent as VEROX grows.

This document is intentionally separate from production architecture and engineering status. Sales must understand the product well enough to sell and integrate it correctly, but must not expose internal verification mechanisms, security controls or implementation details that merchants do not need to know.

---

# 1. Department Mission

The mission of the VEROX Sales & Integration Department is to:

- identify merchants that have a real payment-verification problem;
- understand the merchant's current payment operation before proposing a solution;
- position VEROX as a professional payment verification and trust infrastructure;
- define the merchant's integration requirements clearly;
- remove commercial objections without weakening product standards;
- protect the credibility of VEROX by never promising unsupported behaviour;
- guide each merchant from discovery to technical onboarding and first successful integration;
- preserve product confidentiality and avoid exposing unnecessary internal mechanisms;
- collect recurring objections and turn them into standardized sales cases.

The department does not change VEROX verification rules to close a sale. If a prospect cannot currently satisfy a product requirement, the requirement is explained professionally and the merchant is given a path to become compatible.

---

# 2. VEROX Commercial Positioning

VEROX is a payment verification / payment trust gateway.

For the current product model:

- VEROX does not hold or move the merchant's money;
- the customer pays using the payment method presented by the merchant through VEROX;
- VEROX receives independent payment evidence;
- VEROX verifies the payment according to its internal verification rules;
- once a payment is confirmed, VEROX communicates the payment result to the merchant's system;
- the merchant's own system remains responsible for orders, tickets, inventory, customers and fulfilment.

Commercial communication should focus on the outcome:

> VEROX gives a merchant's software a reliable way to know when an eligible payment has been confirmed.

Do not lead a sales conversation with internal components, parsers, transaction-reference logic, matching algorithms, database rules or security implementation.

---

# 3. Core Sales Principle: Sell the Standard, Not the Limitation

VEROX must not be presented as a collection of technical limitations.

A weak statement is:

> "Our system cannot support that."

The preferred positioning is:

> "For that payment method to be activated in VEROX, this is the required integration standard."

The merchant should understand what is required, why the requirement protects the quality of the payment operation, and what action is needed next.

A product standard must not be weakened merely because a merchant currently operates differently.

---

# 4. Merchant Payment Channel Model

A merchant can have one or more payment channels configured in VEROX.

Examples:

- e-Mola receiving channel;
- M-Pesa receiving channel;
- mKesh receiving channel;
- future supported payment channels.

Conceptually:

```text
Merchant
  ├── e-Mola channel
  ├── M-Pesa channel
  └── mKesh channel
```

A payment method is shown to customers only when the merchant has the corresponding receiving channel configured and approved for use by VEROX.

Examples:

```text
Merchant has e-Mola only
→ Checkout offers e-Mola

Merchant has e-Mola + M-Pesa
→ Checkout offers e-Mola + M-Pesa

Merchant has e-Mola + M-Pesa + mKesh
→ Checkout can offer all three
```

For the current MVP verification policy, automatic confirmation is designed around transfers performed through the same supported payment provider as the merchant's configured receiving channel.

Examples:

```text
e-Mola → e-Mola
M-Pesa → M-Pesa
mKesh  → mKesh
```

Cross-provider transfers must not be sold as automatically verifiable unless VEROX has explicitly approved a deterministic verification method for that route.

This policy is internal. Merchants do not need an explanation of the underlying matching mechanism.

---

# 5. Confidentiality Boundary

Sales may explain:

- what VEROX does;
- what the merchant needs to configure;
- what payment methods are available;
- what the customer must do;
- what states/results the merchant integration can receive;
- what operational requirements exist;
- what the integration sequence is;
- what the merchant needs to provide for onboarding.

Sales should not expose unnecessary internal details such as:

- exact parsing rules;
- exact evidence-correlation rules;
- transaction-reference matching logic;
- candidate selection logic;
- internal thresholds or heuristics;
- database constraints used to prevent replay;
- internal Bridge authentication implementation;
- signing-secret derivation;
- security controls that would make abuse easier if disclosed;
- unpublished anti-fraud or anti-replay logic.

The correct public explanation is usually the product requirement, not the internal mechanism that motivated it.

Important: confidentiality is not the security model. VEROX security must remain strong even if someone understands its general architecture. This rule exists to avoid unnecessary disclosure of implementation-specific mechanisms.

---

# 6. Sales Discovery Process

Before proposing an integration, the salesperson should understand the merchant's actual operation.

Minimum discovery areas:

1. What does the merchant sell?
2. How does the merchant receive payments today?
3. Which mobile-money or payment accounts does the merchant currently own?
4. Which payment methods does the merchant want customers to see?
5. Is the merchant integrating VEROX into an existing system or building a new system?
6. What happens today after a customer claims to have paid?
7. Who currently verifies the payment?
8. What are the main errors, delays or fraud risks in the current process?
9. What event in the merchant's software must happen after payment confirmation?
10. Who is the technical contact for integration?

The purpose is not interrogation. It is to avoid selling the wrong configuration before understanding the merchant's payment reality.

---

# 7. Objection Handling Framework

Every recurring objection should become a formal case in this document.

The VEROX objection framework is:

### Step 1 — Recognize

Show the merchant that the request or concern was understood.

### Step 2 — Reframe

Move the conversation from "why VEROX cannot do X" to "what standard is required to achieve the merchant's desired result safely and consistently".

### Step 3 — Explain the operational consequence

Explain what the merchant gains from following the VEROX standard. Avoid exposing internal mechanisms.

### Step 4 — Present the available path

Do not leave the merchant with a dead end. Explain what can be activated now and what must happen to activate more later.

### Step 5 — Close with a concrete next action

Convert the objection into an operational decision.

Example closing questions:

> "Can we proceed with the e-Mola channel first and add M-Pesa when the receiving channel is available?"

> "Who on your technical team should receive the integration requirements?"

> "Can we schedule the first integration test using the payment method already available?"

---

# 8. Objection Case Registry

Cases use the following identifier format:

```text
CASE-001
CASE-002
CASE-003
...
```

Each case should contain:

- Situation;
- Merchant objection;
- What the merchant is really asking;
- VEROX position;
- Main script;
- Follow-up objections;
- Information safe to disclose;
- Information that remains internal;
- Recommended close;
- Product/engineering follow-up, if required.

A recurring objection must not be handled differently by every salesperson. Once a strong answer is approved, it becomes part of the playbook.

---

# CASE-001 — Merchant Has Only One Receiving Payment Provider

## Situation

A merchant wants VEROX integrated into an existing system.

The merchant currently owns only an e-Mola receiving account, but would like customers using M-Pesa, mKesh or other providers to pay into that same e-Mola account and still have those options presented as normal VEROX payment methods.

## Merchant Objection

Typical forms:

> "I only have e-Mola. Can't my customers just send M-Pesa or mKesh to that number?"

> "I can already receive transfers from another network in my e-Mola account. Why do I need another account?"

> "If you only show e-Mola, I may lose customers who use M-Pesa."

## What the Merchant Is Really Asking

The merchant wants maximum payment-method coverage without having to establish and operate a separate receiving channel for each payment method.

The commercial concern is legitimate: more available payment methods can improve payment accessibility for customers.

However, VEROX must not weaken its operational verification standard merely to make an option appear in checkout.

## VEROX Official Position

To activate a payment method in VEROX, the merchant must have the corresponding receiving channel configured for that payment method.

Therefore:

```text
Merchant has e-Mola
→ e-Mola can be activated

Merchant wants M-Pesa
→ an M-Pesa receiving channel is required

Merchant wants mKesh
→ an mKesh receiving channel is required
```

A cross-provider transfer is not presented as an equivalent substitute for a properly configured VEROX payment channel.

## Main Sales Script

> Percebo. Neste momento vocês possuem o canal e-Mola e, por isso, podemos iniciar a integração do VEROX com e-Mola normalmente.
>
> Para disponibilizarmos também outros métodos, como M-Pesa ou mKesh, precisamos que a empresa possua os respetivos canais receptores configurados para esses métodos.
>
> Isto faz parte do padrão de operação do VEROX: cada opção apresentada ao cliente final deve estar associada ao respetivo meio de pagamento da empresa.
>
> Não precisamos atrasar a integração atual por causa disso. Podemos colocar o e-Mola operacional primeiro. Quando tiverem um canal M-Pesa, adicionamos M-Pesa; e quando tiverem mKesh, adicionamos também esse método.
>
> Assim começam com aquilo que já possuem e mantêm a integração preparada para crescer sem refazer o sistema.

## Follow-up A — "Mas eu consigo receber M-Pesa no meu e-Mola"

Recommended response:

> Sim, existem operações que permitem transferências entre redes diferentes. Mas isso é diferente de termos um método de pagamento configurado e apresentado oficialmente dentro do VEROX.
>
> Quando disponibilizamos M-Pesa como opção, queremos que esse método esteja associado ao respetivo canal M-Pesa da empresa. O mesmo princípio aplica-se ao e-Mola, mKesh e aos restantes meios que forem suportados.
>
> Por isso, não ativamos um método apenas porque uma transferência entre redes pode chegar ao destino. Ativamos o método quando o respetivo canal está corretamente configurado para a operação no VEROX.

Do not explain internal reference or matching behaviour.

## Follow-up B — "Então posso perder clientes que só têm M-Pesa"

Recommended response:

> É uma preocupação válida, e é precisamente por isso que recomendamos adicionar esse canal quando quiserem ampliar as opções de pagamento.
>
> Neste momento temos uma decisão simples: podemos atrasar toda a integração até existirem todos os meios de pagamento, ou podemos colocar já o e-Mola operacional e expandir depois para M-Pesa e mKesh.
>
> A nossa recomendação é começar com o canal que vocês já possuem e deixar a integração preparada para os restantes métodos.

## Follow-up C — "Quero que apareça M-Pesa mesmo sem uma conta M-Pesa"

Recommended response:

> Podemos registar essa necessidade, mas não apresentamos M-Pesa como método oficial enquanto não existir o respetivo canal receptor configurado para a empresa.
>
> É um padrão operacional que aplicamos às integrações VEROX para manter o processo de pagamento consistente para o merchant e para o cliente final.
>
> Assim que o canal M-Pesa estiver disponível, a sua ativação passa a fazer parte da configuração normal da conta.

## Follow-up D — "Por que é obrigatório?"

Recommended response:

> Porque cada método disponibilizado pelo VEROX precisa estar associado ao respetivo canal receptor e devidamente configurado para a operação. É um requisito da plataforma para manter a integridade do processo de confirmação dos pagamentos.

If the merchant requests internal technical details:

> Existem regras internas de validação e segurança que fazem parte da infraestrutura do VEROX e não expomos os mecanismos específicos de verificação. Para a integração, o requisito relevante é que cada método ativo tenha o respetivo canal receptor configurado.

Stop there unless an authorized technical integration discussion requires additional information.

## What Sales Must Not Say

Do not say:

> "You need three accounts because our system cannot match the payments otherwise."

Do not expose explanations about same-provider references, SIMO correlation, internal evidence matching, parser behaviour or candidate selection.

Preferred formulation:

> "To activate three payment methods in VEROX, the company needs the three corresponding receiving channels."

## Recommended Close

> Portanto, para esta primeira integração não precisamos bloquear o projeto. Vamos configurar o e-Mola que vocês já possuem e colocar esse método disponível.
>
> Se quiserem disponibilizar também M-Pesa e mKesh, basta providenciarem os respetivos canais receptores e fazemos a expansão da configuração.
>
> Podemos então avançar nesta primeira etapa com o e-Mola e deixar M-Pesa e mKesh como expansão?

## Internal Engineering Note

Current MVP policy must preserve deterministic automatic confirmation. Cross-provider transfers must not be automatically confirmed merely because value, time or destination appear compatible.

This internal note is not part of the merchant-facing script.

---

# 9. Sales Language Standards

Prefer:

- "requisito de integração";
- "padrão operacional";
- "método disponível";
- "canal receptor";
- "ativar o método";
- "configuração da conta";
- "expandir a integração";
- "confirmação do pagamento";
- "integração segura e consistente".

Avoid unnecessary phrases such as:

- "o nosso algoritmo não consegue";
- "o nosso parser depende de...";
- "a nossa base de dados compara...";
- "usamos esta referência para...";
- "o Bridge faz match através de...";
- "é uma limitação do código".

When a real limitation exists, do not lie about it. State the supported product boundary and the requirement necessary to operate inside that boundary.

---

# 10. Sales-to-Engineering Boundary

Sales owns:

- merchant discovery;
- commercial positioning;
- requirement communication;
- objection handling;
- onboarding coordination;
- collection of integration needs;
- integration follow-up with the merchant;
- maintaining this playbook.

Engineering owns:

- verification rules;
- payment-state authority;
- evidence architecture;
- security controls;
- Bridge implementation;
- API behaviour;
- deployment and production reliability;
- technical approval of new payment routes or capabilities.

Sales must never promise a technical capability that Engineering has not approved.

Engineering should not change verification integrity merely to satisfy a sales objection.

When a commercial opportunity requires a new capability, record it as a product/integration requirement and escalate it for technical evaluation.

---

# 11. New Objection Intake Procedure

When a new objection appears:

1. Capture the merchant's words as accurately as possible.
2. Identify the underlying concern: cost, trust, integration effort, coverage, timing, security, operations or something else.
3. Determine whether the issue is commercial, technical, product, legal or security-related.
4. Do not invent an answer when the product rule is unknown.
5. Obtain Engineering/Product clarification if required.
6. Create a new `CASE-XXX` entry.
7. Write the approved main response and follow-ups.
8. Define what may and may not be disclosed.
9. Define the desired next action.
10. Update the playbook so the same objection is handled consistently in the future.

---

# 12. Current Sales Department Priorities

At this stage of VEROX, Sales & Integration should prioritize:

- closing the first real merchant integration;
- understanding the merchant's actual payment channels;
- avoiding promises beyond the validated VEROX payment-verification model;
- preparing merchant integration requirements clearly;
- collecting real objections from actual conversations;
- building this objection playbook case by case;
- keeping production engineering discussions separate from commercial discussions.

This document must evolve from real merchant conversations, not hypothetical sales theory alone.

---

# 13. New Chat / Sales Director Agent Prompt

Use the following prompt to start a dedicated VEROX Sales & Integration conversation:

```text
You are acting as the Director of the VEROX Sales & Integration Department.

VEROX is an official RIGHTWARE product and is positioned as payment verification / payment trust infrastructure. Your responsibility in this conversation is commercial strategy, merchant discovery, sales process, integration communication, objection handling, onboarding coordination, sales scripts and maintenance of the VEROX Sales & Integration playbook.

The internal source of truth for this department is:

docs/sales/VEROX_SALES_DEPARTMENT.md

Always use that document as the operational baseline when it is available.

Your responsibilities:

1. Think like the director of a professional B2B fintech sales and integrations department, not like a generic copywriter.
2. Turn recurring merchant objections into formal CASE-XXX playbook entries.
3. For each objection, identify the merchant's real concern, VEROX's official position, the main response, follow-up responses, disclosure boundaries and the desired closing action.
4. Preserve VEROX product standards. Never weaken payment-verification integrity merely to close a sale.
5. Never promise an engineering capability that has not been approved or validated.
6. Do not expose unnecessary internal verification mechanisms, matching logic, parser rules, database controls, secrets or security implementation.
7. Do not lie about limitations. Present supported product boundaries as clear integration requirements and standards.
8. Keep merchant-facing language professional, explicit, understandable and commercially strong.
9. Separate merchant-facing scripts from internal notes.
10. When a sales conversation reveals a genuine new product requirement, mark it clearly for Product/Engineering review instead of silently changing the commercial promise.
11. Maintain continuity: keep track of the current merchant, objection, sales stage, next action and unresolved questions.
12. This chat is dedicated to Sales & Integration. Do not take over the production/backend engineering workflow unless the user explicitly asks for a cross-department technical clarification.

Current foundational rule:
A merchant can expose only payment methods for which the corresponding receiving channel has been configured and approved in VEROX. A merchant with e-Mola can start with e-Mola; M-Pesa and mKesh become available when their respective receiving channels are added. Internal verification mechanics behind this rule are not part of normal merchant-facing communication.

Start by reading the department document, summarize the current commercial state in a compact internal briefing, identify the active CASE and merchant situation if present, and then continue from the user's next sales instruction.
```

---

# 14. Document Governance

This is a living internal document.

Updates should be made when:

- a recurring objection appears;
- a product capability changes;
- a merchant onboarding requirement changes;
- Sales receives an approved new technical boundary from Engineering;
- a script proves ineffective and a better approved response is established;
- new merchant segments require different discovery questions or sales flows.

Do not overwrite historical case meaning merely to make a new situation fit an old case. Create a new case when the underlying objection is materially different.

The goal is to build a repeatable VEROX sales system where knowledge compounds instead of being lost inside individual conversations.

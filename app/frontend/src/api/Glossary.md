# Glossary

Lookup tables for the FiServ change process: acronyms, APM → CI mappings,
groups, datacenters and clients. Reference data, not prose — the narrative
lives in [[Work/FiServ/Change Management|Change Management]] and the procedure in [[Work/FiServ/Opening a Change|Opening a Change]].

**This page exists because of one specific failure.** Picking the wrong
Configuration Item is the most repeated mistake in the whole process and it
fails silently: a wrong CI passes every check and still leaves the change
unapprovable. The `APM → CI` table below is the cure. **Grow it every time
you resolve one** — a row added here is a rework loop nobody else has to
run.

> Everything here is "as observed up to early September 2026". Group names,
> people and CIs drift. Treat a row as a strong lead, and verify against
> the live record when it matters.

---

## Acronyms and terms

| Term | Means | Notes |
|---|---|---|
| **APM** | Application Portfolio Management ID | The application's identifier, `APM0009941`. **Search ServiceNow for it as `PM0009941`.** The key to finding the right CI. |
| **BAU** | Business As Usual — the Operations/support team | The team that provides the always-required Operations validation. Nobody on the recordings could expand the acronym; the role is unambiguous. |
| **BR / LAS / LAN** | The three CAB agendas | Brazil · Central+South America · North America (Mexico for us). See [[Work/FiServ/Change Management#Regional CABs|Change Management]]. |
| **BRD** | A project code format | e.g. `BRD 190`, used in `Related project`. |
| **CAB** | Change Advisory Board | The meeting where a change is presented and defended. |
| **CAC / CAQ** | Central America region code | **In practice means Panama.** |
| **CAT** | A testing environment / stage | Appears alongside UAT in the testing question. |
| **CCT / CTC** | Command Center | Same thing, two spellings. Regional: `CTC-ARG`, `CTC-BRA`. |
| **CI** | Configuration Item | The system/app/server the change targets, as it exists in the CMDB. |
| **CMDB** | Configuration Management Database | ServiceNow's inventory of everything that runs. |
| **credhub** | Credential store | Target of "update credhub" execution tasks. |
| **CS** | Client Service | Also one of the three roles that sign off at HRRB. |
| **CTASK** | Change Task | The child records carrying execution and validation work. |
| **DDA** | Debit-Driven Accounts | **In this context.** A general AI will expand it as a credit-card term and be wrong. |
| **DR** | Disaster Recovery | The standby environment. |
| **HRRB** | High Risk Review Board | A real meeting, regional (`HRRB BRA`), that minutes its verdict into `Division CAB notes`. Distinct from the `HighRisk Review Board` checkbox that flags a change for it. |
| **HSM** | Hardware Security Module | Thales HSM-10K in this estate. Everything needing a PIN block goes through it. |
| **ICTO** | *(unconfirmed)* | One of three roles signing off at HRRB, alongside PSH and CS. |
| **JWE** | JSON Web Encryption | The key type in the Banco Agricola tokenization change. |
| **M2M** | Many-to-many | The auto-derived client and product lists on a change. |
| **MAP** | A security remediation process ID | e.g. `MAP 28353`. CAB reviewers recognise `MAP` as security remediation without further explanation. |
| **MW** | Maintenance Window | `No MW` = the non-maintenance-window value, for validation/administrative tasks. |
| **PITE** | Pre-Implementation Testing Evidence | Did you test it, can you prove it. **The single most consequential field on the form** — see [[Work/FiServ/Opening a Change#Step 3c — PITE, and the rule for answering it|Opening a Change]]. |
| **PR** | Clarity Project ID | e.g. `PR25004659`. Lives in `Clarity Project ID`. Heard as "UPR" in recordings. |
| **PRO / Procesa** | Alias for Panama | Same as CAC/CAQ, seen in some documents. |
| **PSH** | *(unconfirmed)* | One of three roles signing off at HRRB. |
| **TSP** | Token Service Provider | As in `app-tsp-outbound-mastercard`. |
| **UAT** | User Acceptance Testing | Referenced in CAB question 2. |
| **VIP** | Virtual IP | NetScaler VIPs front the NGINX layer; SSL can terminate at either. |

## Tools in the estate

| Tool | What it does |
|---|---|
| **ServiceNow** | Where changes live. See [[Work/FiServ/Change Management#Sources|Change Management]] for what's native vs. FiServ customization. |
| **Venafi** | Certificate management. **Holds its own approval, separate from ServiceNow** — an HSM/hardware certificate can need a Venafi sign-off the change record knows nothing about. |
| **Moogsoft** | Alerting and monitoring. The `Maintenance Overridden` fields suppress its noise during your window. |
| **Clarity** | Project and portfolio tool. Source of the `PR` number. |
| **Sage AI** | FiServ's own AI change analysis. Produces `Sage AI Risk Score` — **which does not track the real `Risk` field**; treat it as commentary. |
| **Apigee / ApigeeX** | API gateway. |
| **NGINX / F5 / NetScaler** | Proxy and load-balancing layers. Certificates often terminate at NGINX with NetScaler as passthrough. |
| **Dynatrace** | Preferred over the CMDB's server view for seeing what software runs on a host. |

---

## APM → CI → datacenter

**The most valuable table on this page. Add a row every time you work one
out.**

| APM | Application / client | Configuration Item | Datacenter |
|---|---|---|---|
| `APM0009941` | AFNZ/AFINZ Brazil — Issuing API | `LATAM FIRSTVISION APIS - BRAZIL - HORTOLANDIA IN PRODUCTION` | Brazil (IBM) Hortolândia |
| `APM0009941` | *same, lower environment* | `LATAM FIRSTVISION APIS - BRAZIL IN INTEGRATION-LOWER` | Brazil (IBM) Hortolândia |
| `APM0009941` | *same, AFINZ RPA/integration lower* | `Latam ForteVision RPAs Brasil Integration Lower` *(group CI, covers 2 servers)* | — |
| `APM0007084` | Banco Agricola — `app-tsp-outbound-mastercard` | `LATAM ISSUING APIS - OMAHA IN PRODUCTION` | Omaha (primary) |
| `APM0007084` | *same, second production datacenter* | `LATAM ISSUING APIS - CHANDLER IN PRODUCTION` | Chandler |
| `APM0008241` | FALCON ROUTER LATAM / CAM Router | `stlp2camapp0003.1dc.com`, `stlp2camapp0004.1dc.com` | — |
| `APM0008241` | *same, DR pair* | `rklp2camapp0005.1dc.com`, `rklp2camapp0006.1dc.com` | — |
| *(APM 5327)* | Thales/HSM certificate | — | **Owned by a different APM than the app it serves** — which is why nobody on the team had Venafi access for it. |
| — | LATAM Tokenization & Digital Wallet | `LATAM TOKENIZATION & DIGITAL WALLET SOLUTION - PARQUE PATRICIOS IN PRODUCTION` | Argentina, Parque Patricios |
| — | Unified UI for VisionPLUS | `UNIFIED UI FOR VISIONPLUS - LATAM - IBM HORTOLANDIA IN PRODUCTION` | Brazil (IBM) Hortolândia |
| — | Latam ShareToken APIs (Transactions, Accounts, Cards, Customer) | `Latam ShareToken APIs Omaha in Productions` | Omaha (**primary**) |
| — | *same, DR* | `Latam ShareToken APIs Chandler in Productions` | Chandler (**DR**) |

**Traps recorded against this table:**

- `Latam ShareTokenization` **looks** like the CI for the ShareToken APIs and
  is not — the application lives elsewhere. Name resemblance is not evidence.
- CI names are inconsistent about **`Brasil` vs `Brazil`**. Search both.
- The naming pattern is `<application> <datacenter> in <environment>`, so the
  same datacenter appears once per environment — `Parque Patricios in
  Production` and `Parque Patricios in Certification` are different CIs.
- Certificate CIs are **server hostnames**, class `Certificates`, owned by
  `Sec - Certificate Management (SSL)` — a different shape from application
  CIs entirely.

## Datacenters

| Datacenter | Location string | Role |
|---|---|---|
| **Omaha** | `Omaha - 7305 Pacific St-Parent` | Primary for the Latam ShareToken/Issuing APIs |
| **Chandler** | `Chandler CHD1-Parent` | DR counterpart to Omaha — though both appear `IN PRODUCTION` on some CIs |
| **Parque Patricios** | `Argentina - Parque Patricios-Parent` | Argentina. Production and Certification variants |
| **Hortolândia** | `Brazil (IBM) Hortolandia-Parent` | Brazil. Primary and Secondary/DR instances |
| **Peru Street** | `Argentina - Peru Street-Parent` | Seen on a DR CI |

---

## Groups

The `LTAM.<n>.<name>-LAT` pattern is the local convention; the number looks
like a tier. Recordings call `LTAM.3.API Development-LAT` simply "group 3".

| Group | Owns |
|---|---|
| `LTAM.3.API Development-LAT` | The dev team — "group 3". Most changes' `Assignment Group`. |
| `LTAM.2.API Engineering-LAT` | Engineering. Also an `Assignment Group` on some changes. |
| `LTAM.3.Change Management` | The change managers — the second approval tier. |
| `LTAM.2.INFRASUPPORT-LAT` | NGINX, F5, servers, OS-level work. |
| `LTAM.3.Gateway Prod Support-LAT` | Gateway / credhub execution. |
| `TSRV.2.DBA-Oracle Operations` | Database work. |
| `LTAM.3.UUI VisionPlus-LAT` | The Unified UI for VisionPLUS. |
| `LTAM.2.FirstVision Engineering-LAT` | VisionPLUS validation. |
| `LTAM.1.Falcon UAT-LAT` | Falcon validation. |
| `LTAM.1.Command Center CTC-ARG` | Command Center for Panama, Colombia, Argentina, Mexico. |
| `LTAM.1.Command Center CTC-BRA` | Command Center for Brazil. |
| `CYBER.3.Application Security` | Cyber approval tier — **pulled in by `Is this a code change? = Yes`**. |
| `Cyber.F.Permit to Operate` | Second Cyber approval tier, same trigger. Often auto-approved by `OneTrust Service Account`. |

**Automated approvers seen in the flow:** `PTX Auto Approver`,
`OneTrust Service Account`, `Confidence Service Account`. `svc_SageAI` and
`fiserv.system` appear as record updaters.

---

## Clients

| Client | Special handling |
|---|---|
| **SPIN** | Every change is **automatically High Risk**, whatever the impact. Needs a post-CAB sign-off from a client-side VP. Client Service: Laura Azevedo. |
| **AFINZ** *(written `AFNZ` in ServiceNow)* | Needs Client Service sign-off before approval clears — **Romano and Carlos Wilson (São Paulo)**, by email. Brazil. `APM0009941`. |
| **MCB** | Has its own change template. No special CAB handling observed. |
| **Vivenda** | Appears in Restricted changes with named approvers. |
| **Sem Parar** | Toll-tag client. The segregation project splits across infra, UI and API changes. |
| **Banco Agricola** | Panama. Subject of the tokenization/JWE work. |
| **TOTALPLAY, BINEO** | Mexico. Consumers of the CAM Router certificate. |

> **AFINZ and AFNZ are the same client.** The recordings and hand-written
> notes say AFINZ; ServiceNow records say `AFNZ BRAZIL`. Search both.

A production change's auto-derived client list can run to ~30 banks across
Panama, El Salvador, Guatemala, Nicaragua, Mexico, Curaçao, Barbados and
Dominica. That list comes from the CI, not from you — read it to understand
your blast radius, don't panic at its length.

---

## Where to look things up

| Question | Where |
|---|---|
| "Which CI for this application?" | The table above — then verify by APM search per [[Work/FiServ/Opening a Change#Step 3a — Getting the Configuration Item right|Opening a Change]]. |
| "Who currently approves X?" | [[Work/FiServ/Change Management#Who's who|Change Management]] — and ask Nidia, who tracks the live roster. |
| "What does this field mean?" | [[Work/FiServ/Change Management#Header fields|Change Management]]. |
| "What order do I do things in?" | [[Work/FiServ/Opening a Change|Opening a Change]]. |
| "Why is this rule like this?" | [[Work/FiServ/Change Management|Change Management]]. |

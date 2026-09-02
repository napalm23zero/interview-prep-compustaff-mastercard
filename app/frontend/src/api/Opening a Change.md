# Opening a Change

The step-by-step for filling out a Change Request in ServiceNow, start to
finish, in the order the work actually happens. Written so someone opening
their first change can do it without anyone sitting next to them.

This is the **doing** document. The **why** — what each field means, what
breaks, the exceptions, the politics — lives in
[[Work/FiServ/Change Management|Change Management]]. Where something here is stated flatly, that page has
the reasoning and the war story behind it.

> **Scope.** Applies to any create, modify or retire action on software,
> infrastructure, certificates or configuration, raised through the
> ServiceNow portal. Does **not** apply to incident response, service
> requests, or work that already has its own process.

---

## Step 0 — Before you touch the form

Gather these first. Every hour lost in the recordings was lost hunting one
of them *after* starting, usually against a deadline.

| You need | Where it comes from |
|---|---|
| **The APM number** | The requester. Ask for it if it isn't on the card — it's the key to the CI. Check [[Work/FiServ/Glossary|Glossary]] first; it may already be mapped. |
| **Which client and region** is affected | The card, or the person who will execute. Never inferred from an old change. |
| **The execution date and window** | The requester. If unknown, you can still build the change — see step 6. |
| **Who executes and who validates** | Two different people. Check their groups. |
| **The step-by-step the executor will run** | The SME. Infra executes strictly off what the ticket says. |
| **Test evidence**, or a reason there is none | See `PITE` in step 3 — this one field decides your risk track. |

**If another team has to act (Infra, Middleware, Command Center), talk to
them before you build the change**, not after. Their representative sits in
CAB and will contradict a task they never agreed to, and the change dies
there. See [[Work/FiServ/Change Management#Coordinating other teams before you start|Change Management]].

---

## Step 1 — Clear your group's backlog

Your change cannot get through while your Assignment Group has stale work
sitting in the queue.

1. Open the **LATAM Changes Backlog** dashboard.
2. Filter to your Assignment Group in the multi-select.
3. Look at the **gauge** — the car-speedometer-looking thing counting
   overdue and pending changes.
4. It must read **zero**. *Green is not the goal, zero is* — green with one
   item on it still counts as not-zero.

**Do not read the `Changes in Implement State` tile.** That one is supposed
to be above zero: approved today, implemented tomorrow is healthy.

**If the gauge isn't zero,** find the stale change, track down its owner,
and:

1. Check its tasks. All closed → close the change. Any still open → ask the
   owner first; closing someone's live work makes enemies before lunch.
2. Set `State` to `Closed`.
3. Set `Close code` to `Successful`.
4. Click `Review`. The change closes itself and follows the flow.
5. Back to the gauge — the needle should drop.

> Make looking at this gauge a daily five-second habit. It saves hours on
> the day you're in a hurry.

---

## Step 2 — Decide the type

You pick this in the `Type` field, and **the environment you're touching
decides it for you**:

- **`Cert`, `Lower`, `Development`, `Testing`** → **Standard change.**
  Pre-approved, low-risk, repeatable, skips CAB entirely.
- **`Production`, `DR`, `Prod/DR`** → **Normal change.** Full review,
  approval flow, and a trip to CAB.

The `Type` field also offers `Expedited`, `Emergency`, `Model` and
`Unauthorized`. You'll live in Normal and Standard essentially always; the
others are handed to you deliberately, they aren't things you pick on a
whim. If you think you need Expedited or Emergency, read
[[Work/FiServ/Change Management#Change types|Change Management]] first — there is usually a cheaper route.

---

## Step 3 — The header fields

Open **Create a Change** → `New request` → the change form. Fill top-down.

| Field | What to put |
|---|---|
| `Requested by` | Usually you, the person opening it. |
| `Category` | The broad area. An API or application change is `Application`. Certificate and TLS work is `Security`. |
| `Sub-Category` | Stays empty until Category is set. `Application` → `Deploy`; `Security` → `Modify` covers most of this team's work. |
| `Configuration item` | **See step 3a — this is the field that goes wrong most often.** |
| `Assignment Group` | Your own group. This is the group whose backlog gates you, and whose members can approve at `Assess`. |
| `Environment` | The real target. Also silently decides Standard vs Normal (step 2). |
| `Change management group` | Arrives pre-filled (`LTAM.3.Change Management`). Leave it. |
| `On Behalf Division` | The division you're raising it for, e.g. `Issuer LATAM`. Behind a **padlock** — click to unlock, type, click again to lock. Required; blank blocks `Check Conflict`. |
| `Owning Division` | Read-only, filled from context. |
| `Impact` | `3 - Low` unless the system is genuinely down, every customer is affected, **or a shared cryptographic/infrastructure dependency is in play**. Marking everything `1 - High` is the change-management version of crying wolf. |
| `Risk` | **Read-only.** Calculated from your answers. You shape it indirectly, never type it. |
| `Project Scope` | Why the change exists, in business terms — see step 3b. |
| `Is this a code change?` | `Yes` if any code or dependency changes, even a pure version bump. **This is the most expensive `Yes` on the form** — see the warning below. Answering `No` on a change that touches code is an evasion, not a shortcut. |
| `Will this change require an update to DR?` | Does DR need this too, to stay a faithful copy of production? |
| `Will the DR update be included in this change?` | If yes to the above: are you doing it here, or in a separate change later? |
| `Why doesn't this change need to be completed in DR?` | Appears when you answer `No`. One specific line: "It's just a change of encryption keys", "This change is for DR". A blank here is how backups quietly rot. |
| `PITE` | See step 3c. **The single most consequential field on the form.** |
| `Playbook` | `Playbook Not Required` is a perfectly respectable answer. |
| `Clarity Project ID` | The `PR` number, e.g. `PR25004659`, when the work belongs to a tracked project. |
| `Related project` | Free text — the project or initiative this belongs to. |
| `Escalation contact` | Who to call when things get spicy. **Actual names** — one to three people. An approved change once reached `Implement` with this field still containing the template prompt ("Who do we contact… Name(1) & Phone#"), which is a question where an answer belongs. |

> **If you answered `Yes` to code change, add days, not hours.** That answer
> inserts **two Cyber approval groups** into your path —
> `Cyber.F.Permit to Operate` and `CYBER.3.Application Security` — on top of
> your own group and Change Management. A real code change carried **37
> approvers across four tiers**, versus 29 across two for a non-code change,
> and Cyber is a global team working other timezones. On the sampled change,
> every LATAM tier was approved while Application Security was still
> `Requested`. Start earlier than you think you need to.

### Step 3a — Getting the Configuration Item right

Do not guess this, and do not copy it from a similar-looking change. That is
the single most repeated mistake in this whole process, and it fails
silently: a wrong CI passes every pre-submission check and still leaves the
change unapprovable, because **the approver list is derived from the CI's
owning group**, not from your `Assignment Group`.

**The reliable way:**

1. Open a second ServiceNow tab.
2. Search the client's **APM number** in the top-right search box
   (e.g. `PM0009941`).
3. On the record, open the **organization** view.
4. Expand entries using the small arrow to the **left** of a row — not by
   clicking the row itself.
5. Look for the entry matching your environment (`development`, `lower`,
   `non-prod`, or the production one).

Two rules once you've found candidates:

- **If a CI group covers several servers and your change touches more than
  one of them, pick the group-level CI.** The CI is a declaration of blast
  radius, not a label — naming one server says you are touching only that
  server.
- **The search box needs an exact string match.** A near-miss returns
  nothing, which reads like "the CI doesn't exist" rather than "you typed it
  slightly wrong". Try both `Brasil` and `Brazil`; the naming is
  inconsistent.

Full treatment, including known-good examples:
[[Work/FiServ/Change Management#Getting the Configuration Item right|Change Management]].

### Step 3b — Project Scope

| Value | Use for |
|---|---|
| `BAU Maintenance / Housekeeping` | Routine upkeep. **Certificate renewals go here.** |
| `Defect / Incident Related` | Something is wrong in production and this corrects it. |
| `Vulnerability / Patching` | Security remediation and patching. |
| `Feature Updates` | New or changed functionality. |
| `Existing Client Conversion` | Migrating an existing client. |
| `Existing Client Expansion` | Growing an existing client's footprint. |
| `New Client Implementation` | Onboarding a new client. |

### Step 3c — PITE, and the rule for answering it

`PITE` is Pre-Implementation Testing Evidence: did you test this before
unleashing it, and can you prove it. Four options:

- `Yes, I completed in ServiceNow` — evidence lives in the tool.
- `Yes, I completed outside ServiceNow` — evidence lives elsewhere; link it.
- `No, I did not complete` — opens a mandatory reason field.
- `Not required for Low and Very Low risk`.

**Answering `No` drives the calculated `Risk` straight to `Very High`**, and
a Very High change goes to a different CAB committee on different days. That
is the system working as designed, not a bug to argue with — but it means
this field, and not the nature of your change, sets your whole track.

**The rule: `PITE` describes evidence, not intent.**

| Situation | Answer |
|---|---|
| Testing happened and the evidence lives in ServiceNow | `Yes, I completed in ServiceNow` |
| Testing happened, evidence lives elsewhere | `Yes, I completed outside ServiceNow` — and **say where in question 2** |
| There was genuinely nothing to test | `No, I did not complete` + a real reason — and **accept the Very High** |
| The change is already Low/Very Low risk | `Not required for Low and Very Low risk` |

The reason field is never left blank when you answer `No`. "It is a
certificate renewal" is a fine reason; an empty box is not, and the board
reads these.

**Why this rule exists.** Identical certificate-renewal work has been filed
both ways in real changes — one person `No` (landing `Very High`), another
`Yes, completed outside` (landing `Moderate`). Same work, opposite risk
tracks, entirely because two people read "did you test" differently. Both
readings are defensible, which is exactly the problem: **a governance field
this consequential cannot be a judgment call.**

Under the rule above, a certificate renewal with nothing testable is `No`,
and `Very High` for touching production cryptography is arguably the correct
classification rather than a punishment. But the value is in the team having
**one** written rule — which one matters less than that it is shared.

> This is a **proposed standard, not yet an agreed one.** It changes risk
> classification and will draw questions at CAB, so socialise it with the
> team before applying it unilaterally.

---

## Step 4 — Short description, description, and the seven questions

### Short description — the title standard

This one line does more work than any other field: it is what a CAB reviewer
scans on an agenda of forty changes, what lands in the approval email of ~29
people, and what you'll search for in six months. Write it for someone who
has three seconds.

```
<IDENTIFIER> - <VERB> <TARGET> [- <ENVIRONMENT if not Production>]
```

**The rules:**

1. **An identifier first, always.** A PR number in brackets
   (`[PR25004659]`), an APM (`APM0009941`), a client code (`MCB`), a project
   name (`Sem Parar`), or `LATAM` for estate-wide work. Which one matters
   less than having one.
2. **Then a verb.** `Renew`, `Deploy`, `Update`, `Configure`, `Restart`,
   `Migrate`. Never open with "This change is required to…" — a real change
   did, and the title truncated mid-sentence in the list view.
3. **Then the target**, specific enough that someone knows what's being
   touched without opening it.
4. **Environment suffix when it isn't plain Production** — `- Lower`,
   `- DR`, `- Prod/DR`. "Is this prod?" is the first thing anyone asks.
5. **Keep it under about 90 characters** so it survives lists and emails
   intact.
6. **No APM number** — it's too technical for a title. It goes in the
   description header.

**Applied to real titles:**

| Before | After |
|---|---|
| `This change is required to renew and install the SSL/TLS certificate used by the AFNZ BRAZIL` | `APM0009941 AFNZ Brazil - Renew Issuing API SSL certificate - Lower` |
| `LATAM - Certificates renewal and installation for "stlp2camapp0003.1dc.com"` | `LATAM - Renew SSL certificate on stlp2camapp0003/0004` |
| `MCB - Deploy fix to resolve incident impacting authorizeService transaction` | `MCB - Deploy fix for authorizeService 400 incident` |

The last one was already good — the standard mostly formalises what the best
existing titles already do.

### Description — the standard skeleton

Write it for a tired approver at 5pm with forty other changes to get
through. **Copy this skeleton and fill it in:**

```
APM:                  APM0009941
Client:               AFNZ BRAZIL
Application/Service:  Issuing API
Environment:          Production and Disaster Recovery

WHAT:          <what is being done — one or two sentences>
WHY NOW:       <what triggered this, why it can't wait>
NOT IN SCOPE:  <what this change explicitly does not touch>

--- CAB QUESTIONS ---

1. Which LATAM regions / countries will be affected?
A:

2. Has UAT completed testing and signed off on the change?
A:

3. What is the expected impact to businesses and clients?
A:

4. Is it necessary to inform the client?
A:

5. What other applications/services could be impacted?
A:

6. What is the impact if this change were to fail?
A:

7. Could this change trigger alerts? Should we inform the command center?
A:
```

**Why each part earns its place:**

- **The header block** answers "what am I even looking at" before any prose.
  It also puts the APM where it belongs — findable, but out of the title.
- **`WHAT` / `WHY NOW`** separate the change from its urgency. Reviewers
  push back on urgency, not on mechanics, so give them their target.
- **`NOT IN SCOPE` is the highest-leverage line in this whole document.**
  The strongest real change in the sample carries it ("No application code
  or functional behavior will be changed") and it **pre-empts the reviewer's
  first suspicion** before they voice it. In a board that asks question
  after question, answering the obvious one unprompted is what shortens the
  session.
- **The `A:` prefixes** make answers visually distinct from questions, so a
  blank one is obvious at a glance rather than hiding in a wall of text.

Answers stay short — none of the seven ran past two sentences on any real
production change, and the tone is factual, never defensive.

### The seven questions

These live at the end of the `Description`, after the prose. They are
**mandatory for any change going to CAB** and nothing on the form enforces
them, which is exactly why changes reach CAB with half of them missing.

**Where they apply, and where they don't:**

| Change type | Seven questions |
|---|---|
| Normal, Expedited, Emergency, Restricted | **Required.** All seven, numbered. |
| Standard | **Not required** — confirmed by a real approved Standard carrying none. |

**The rule: all seven or none, never a subset.** A Standard change with no
question block is correct. A CAB-bound change with five of seven is the
failure mode that actually happens, and it happens because *a missing
question is invisible* — you can only spot the gap if you know the list by
heart. Keep every question present with its number, and answer
`N/A - <reason>` rather than deleting it. Deleting is what makes the hole
unfindable.

This matters most when **cloning**: if you copy a Normal change to build a
Standard, either keep the whole block or remove the whole block. Half a
block is worse than either.

**Only one of the seven has a legitimate `N/A`.** Working through them, the
others always have a real answer — which means `N/A` anywhere except
question 2 is a smell worth a second look:

| # | Can it be N/A? |
|---|---|
| 1 · Regions | **No.** Every change touches somewhere, and this one decides your CAB. |
| 2 · UAT signed off | **Yes** — when there is genuinely nothing testable, e.g. a certificate renewal. Give the reason. |
| 3 · Expected impact | No. "No impact expected" is an answer, not an absence. |
| 4 · Inform the client | No. Yes and No are both real answers. |
| 5 · Other services impacted | No. "None" is an answer. |
| 6 · Impact if it fails | No. Always answerable, and the honest answer is often the short one. |
| 7 · Alerts / command center | No for Production. Arguably N/A for a lower environment with no alerting. |

1. **Which LATAM regions / countries will be affected?**
   Name real countries or region codes. This decides which CAB(s) you go to,
   so it cannot be fixed tomorrow — see step 6.
2. **Has UAT completed testing and signed off on the change?**
   Reference the actual evidence, or state why testing doesn't apply.
3. **What is the expected impact to businesses and clients?**
4. **Is it necessary to inform the client?**
   Give the reason, not just yes/no: *"No. No impact expected and the
   deployment will be executed outside of work hours."*
5. **What other applications/services could be impacted?**
   Confirm with the SME. Don't guess.
6. **What is the impact if this change were to fail?**
   Be honest where the honest answer is unflattering. *"None, first time
   deploying the code in production"* is a better answer than inventing a
   rollback that doesn't exist.
7. **Could this change trigger alerts? Should we inform the command center?**
   If you answer yes — and you almost always will — **you have just
   committed to creating a Command Center task** in step 7. Answering yes
   without creating it makes the change assert something untrue.

Answers are short. None of the seven ran past two sentences on any real
production change.

---

## Step 5 — The plans

| Field | What goes in it |
|---|---|
| `Change Execution Plan` | The high-level game plan, step by step but **not deeply technical**. Command-by-command detail belongs in the tasks. Close it with *"for more technical details please check the specific tasks."* |
| `Backout / Roll forward plan` | Pick which strategy you're committing to: undo it, or push forward to a fixed good state. |
| `Backout plan` | The exact steps to get back to safety. **Write it like you will run it at 3am under pressure**, because someone might. |
| `Roll forward plan` | The steps to fix forward, when going backward is worse than finishing. |
| `Backout plan duration` | `Less than 30 Minutes` · `Between 30 and 60 Minutes` · `More than 60 Minutes`. |
| `Validation plan` | How you'll prove it worked — the checks, commands and expected results. |
| `Client post-implementation validation` | `Yes - Inside Maintenance Window` · `No - Outside Maintenance Window` · `No - Internal Validation Only`. |
| `What functionality is impacted during the change implementation?` | What stops working or wobbles while the change runs. Set expectations so nobody panics at a blip you already knew about. |

**Stock phrasing that real changes reuse**, so you recognise it rather than
reinventing it:

- Backout for a first-time deployment: **"First deploy in production"** —
  i.e. there is nothing to roll back to, said in four words instead of
  padded into a fake procedure.
- Validation for a deploy: **"Validate the application is deployed and
  running."**

**Drafting these with AI works, with two rules.** Give it an example to work
from — *"read this backout plan from another change and, using it as a base,
create one for my scenario"* beats asking cold — and tell it explicitly when
the environment is non-production, because the treatment differs. Then cut
hard: it gets prolix and repeats itself, and it will confidently invent
FiServ acronyms it doesn't know. Every step from the source ticket must
survive the trim.

---

## Step 6 — Schedule

Set `Planned start date` and `Planned end date`.

- **Outside business hours, always.** Real windows run 00:00–01:00,
  00:00–03:00, 04:00–09:00, 06:00–09:00.
- **Give it real lead time.** A Normal change scheduled for tomorrow will
  probably not validate; expect a couple of business days.
- **On a Standard change, the window start must still be in the future when
  the approval happens.** If the clock catches up while you're still
  building, push the start forward *before* attempting approval rather than
  debugging the error.
- Requester-driven Standard work typically gets a **24-hour** window — open
  now, they act by end of tomorrow.

**You can build a change before the date is known.** Fill in everything you
have, create the meeting room, and leave it in `New`. Only the tasks need the
final date. Being blocked on a missing date is not a reason to sit idle.

> **The one thing you cannot defer to tomorrow is the affected region.** It
> decides which CAB the change enters. Everything else can be refined after
> submission; this can't.

---

## Step 7 — Tasks (CTASKs)

The change is the *what and why*. The tasks are the *how*, at full technical
depth. In the change description, a line like *"for more technical details
please check the specific tasks"* is your friend.

**Every change has at least two:**

- **1 Execution task** — the actual work.
- **1 Validation task** — proof it landed correctly.

**They must have different people on them.** ServiceNow will not stop you
putting the same name on both; the process still requires two.

**Then add a validation per thing the change touches:**

| When the change… | Add a validation for… |
|---|---|
| **Always. No exceptions.** | **Operations Team** |
| Hits PROD and involves a client | Command Center |
| Involves APIs | Engineering |
| Involves other systems (F5, Apigee, …) | One per system |

The Operations validation is unconditional — not skippable on a small
change, not waived because something looks harmless. That person watches the
logs live while your change runs. They are the human smoke detector.

**Filling a task:**

- Select the **person first, then their group** — the other order doesn't
  reliably work. If a name won't autocomplete, use the magnifier to browse.
- You are really picking the **group**: once scheduled, anyone in that group
  can reassign the task to themselves, but the group can't be changed. A
  placeholder assignee from the right group is normal practice.
- Required on an execution task: `Datacenter`, `Maintenance Window`,
  `Environment`, a scheduled date/time, and acceptance criteria.
- The task's `Datacenter` is a **separate field from the header CI**, and
  the same place often has a different name in each. Fill both.

**Pick the Command Center for the right region:**
`LTAM.1.Command Center CTC-BRA` for Brazil,
`LTAM.1.Command Center CTC-ARG` for Panama, Colombia, Argentina and Mexico.

**Naming — two patterns, pick by what the work is.**

*Paired by subsystem*, when the change splits across teams. Each execution
sits next to its matching validation, which makes it obvious at a glance
that nothing runs unchecked:

```
Execution - Deploy payment-batch-processor    LTAM.3.UUI VisionPlus-LAT
Validate Deploy payment-batch-processor       LTAM.3.UUI VisionPlus-LAT
Execution NGINX/F5                            LTAM.2.INFRASUPPORT-LAT
Validation NGINX/F5                           LTAM.2.INFRASUPPORT-LAT
Execution DB Update                           TSRV.2.DBA-Oracle Operations
Validation DB Update                          TSRV.2.DBA-Oracle Operations
Validation CCT Brazil                         LTAM.1.Command Center CTC-BRA
```

*Numbered*, when the order genuinely matters and one step gates the next:

```
1 - Certificate renewal
2 - Deploy | API | Certificate | server1 & server2
3 - Restart the application in servers
4 - Validate the successful certificate renewal and update (CamRouter)
5 - Validate the successful certificate renewal and update (Falcon)
6 - Validate the successful certificate renewal and update (VisionPLUS)
7 - Communication with Command Center (Argentina)
```

For work spanning datacenters, name them: `Execution - Omaha - Update
credhub`, `Execution - Chandler - Update credhub`. **Each datacenter being
touched gets its own execution task.**

**If DR is involved:** add a second *execution* task for the DR server. Do
not duplicate the whole set — validation is governed by the table above, not
by DR.

---

## Step 8 — The on-call meeting

The change needs a meeting link where everyone gathers to watch it happen.
There is no automation for this; you create it by hand.

1. Create a calendar event of type **Teams meeting**.
2. Add everyone involved in the change.
3. Paste the link into the `Meeting Link` field on the `On-Call` tab.

If **another team executes**, they own the meeting — ask their contact to
generate it and send you the link. Expect them to ask for the window, so
have the date and time ready.

> Re-check the meeting link after any save. It has been observed silently
> losing its value.

---

## Step 9 — The assertions pass

**Do this before the tooling checks, because the tooling cannot do it.**

A change *asserts* things. ServiceNow validates that fields are filled,
never that what's in them is true — so every assertion is unverified until a
human verifies it. All three defects found in real *approved* changes were
this exact class:

| The change claims… | Go and check |
|---|---|
| Q7: "command center was informed and a task created" | The task **exists**, and sits on `LTAM.1.Command Center CTC-BRA` or `CTC-ARG` — not on your own group |
| Q2: "Yes, signoff attached" | It is **actually attached** |
| `Escalation contact` names someone | It holds **names**, not the template's question text |
| Execution and validation are split | Two **different people**, not the same name twice |
| The CI is the right one | Its group matches your `Assignment Group` |

Read the change back once, slowly, as if you were the approver. It takes a
minute and it is the only check of meaning that exists anywhere in this
process.

## Step 10 — Pre-flight checks

In this order:

1. **`Check Conflict`.** Usually at the bottom of the page, though its
   position moves between forms.
   - It reports **missing required fields one at a time**, not all at once.
     Budget three or four round-trips.
   - **Only conflicts rendered in red block the change.** Amber ones — like
     `CI Already Scheduled`, meaning another change is booked against your
     CI in that window — are advisory. An approved change reached
     `Implement` carrying **two** of them. Read the list to know who else is
     in your window, then proceed.
   - If it complains about tasks, open each task, give it a date/time
     matching the parent's window, save it individually, then save the
     parent.
2. **`Reference CMDB`** (top-right corner). "Successfully completed **but
   found no data**" is a normal, non-blocking result — it appears on most
   approved changes.
3. **`Request Approval`.**

> **`Request Approval` is a one-way door.** Once clicked you can no longer
> edit; getting edit rights back means getting the change moved back to
> `New`. Front-load everything.

**On a Standard change there is no `Request Approval` button and no
`New` → `Assess` transition.** Its approvers are listed at the bottom of the
form and approve in place. A Standard change sitting in `New` with approvers
listed is working correctly, not broken.

**If a Normal change won't leave `New` with no visible error**, check
whether the approver list's group matches your header `Assignment Group`. If
they diverge, the CI is wrong — go back to step 3a. Editing the header alone
does not re-derive the approver list. Full diagnosis:
[[Work/FiServ/Change Management#When a change won't leave the New state|Change Management]].

---

## Step 11 — Getting approved

Approval happens in two tiers, and the first one is not the finish line.

**Tier 1 — your Assignment Group (`Assess`).** The request fans out to every
member of your group; **any one of them approving satisfies it**, and the
rest flip to `No Longer Required`. This is why people ask in chat for
approval — they need one click out of ~25 people and can't compel a specific
one.

**Tier 2 — the Change Management group (`Authorize`).** Named change
managers. Your group approving does not move this.

**How many tiers you actually get depends on the change:**

| Change | Tiers | Typical approver count |
|---|---|---|
| Standard | **1** — your group only, no Change Management | ~8 |
| Normal | 2 — your group, then Change Management | ~29 |
| Normal **with code change** | **4** — plus `Cyber.F.Permit to Operate` and `CYBER.3.Application Security` | ~37 |

A Standard change is genuinely light: one tier, one approver needed, minutes
rather than days. A code change is the opposite, and the Cyber tiers are the
ones that stall.

**The deadlines.** For a change to reach the next day's CAB it must be in
`Authorize` — meaning **actually approved**, not merely submitted — by
**17:00** the day before, with the group backlog at zero. Separately, a
change already on an agenda must be ready **at least five minutes before its
slot starts**. Treat 17:00 as the working deadline and the five minutes as
the absolute one.

**The CAB agendas:**

| Agenda | Covers | Brazil time |
|---|---|---|
| **BR** | Brazil | 10:00 |
| **LAS** | Central **and** South America | 16:00 |
| **LAN** | North America — for us, only Mexico | 17:00 |

A change spanning two regions is presented at **both** CABs, which costs a
whole working day.

**If you need to present:** expect three questions — what's the rollback
strategy, what's the downtime, which other regions are affected. They map
onto fields you already filled. Study your own change beforehand; "I think
so?" does not go down well. You don't have to be the SME, but if you aren't,
get the expected questions answered by them in chat first.

---

## Step 12 — The day after

**Check your email for the change number, every time.** ServiceNow sends an
automatic message either way — pre-approved, entered CAB, or rejected — and
nothing else will put it on your radar. A team once remembered at midday
that a change submitted the day before needed defending at that morning's
CAB, and only got away with it because it had been auto-approved.

Then keep following up: the schedule moves around a lot.

> Once submitted, **freeze the change**. Don't polish it while it's in
> flight. Wait until it appears on the CAB agenda, then edit.

---

## Step 13 — After execution, close it

This is the step everyone forgets, and it's the one that blocks the *next*
change — an unclosed change becomes your group's backlog.

1. Close your task when your part is done.
2. Whoever opened the change closes the change.
3. `Close code` → `Successful` (be honest; this feeds reports leadership
   stares at).
4. It moves to `Review`, then to `Closed` on its own.

---

## Pre-submission checklist

Run this before clicking `Request Approval`.

- [ ] Group backlog **gauge** reads zero
- [ ] `Environment` matches the real target, and the `Type` follows from it
- [ ] CI derived from the **APM number**, not copied from another change
- [ ] CI's group matches your `Assignment Group`
- [ ] `Project Scope` is one of the seven real values
- [ ] `PITE` answered honestly, with a reason if `No`
- [ ] DR questions answered, including the "why not" if `No`
- [ ] Padlocked fields (`On Behalf Division`, `Escalation contact`) locked back
- [ ] Short description follows `<IDENTIFIER> - <VERB> <TARGET> [- <ENV>]`
- [ ] Description uses the skeleton, including **`NOT IN SCOPE`**
- [ ] **All seven questions present and numbered** — `N/A - reason`, never deleted
- [ ] `N/A` appears on question 2 only, or you have a good reason otherwise
- [ ] Execution, backout, validation plans written — backout is real
- [ ] Window is outside business hours, with lead time
- [ ] ≥1 Execution + ≥1 Validation task, **different people**
- [ ] **Operations Team validation exists** — always
- [ ] Command Center task exists if you answered yes to question 7
- [ ] One execution task per datacenter being touched
- [ ] Meeting link pasted into the On-Call tab
- [ ] If Risk is Very High: **`HighRisk Review Board` ticked**
- [ ] `Check Conflict` clean (no red), `Reference CMDB` run
- [ ] Client Service notified if AFINZ or SPIN is involved
- [ ] `Escalation contact` holds **names**, not the template's question text
- [ ] If code change is `Yes`: Cyber's extra lead time is budgeted for

**Then read the whole change back once, slowly.** ServiceNow validates that
fields are *filled*, never that they make sense — every defect found in real
approved changes (same person on both halves, a task pointed at the wrong
group, a placeholder left where a phone number belongs) passed every
automated check there is. You are the only check.

---

## Sources

Built from seven recorded working sessions (2026-08-19 → 2026-09-01), five
real Change Request exports, and Rodrigo's own field-reference notes. The
reasoning, the exceptions, the failure modes and the open questions are in
[[Work/FiServ/Change Management|Change Management]]; this page is deliberately the short version.

# Change Management

FiServ's process for getting a Production change from "code is merged" to
"CAB approved and executed" in ServiceNow. Distilled from seven recorded
pairing and working sessions between 2026-08-19 and 2026-09-01, listed in
[[#Sources]]. This page is a living draft: more recordings are expected,
and each one should sharpen or correct what is written here rather than
append a new layer to it.

> **Three pages, different jobs.** [[Work/FiServ/Opening a Change|Opening a Change]] is the procedure —
> what to fill in, in what order, with a checklist. [[Work/FiServ/Glossary|Glossary]] is the
> lookup data — acronyms, APM → CI mappings, groups, clients. **This** page
> is the reference behind both: what the fields mean, what goes wrong, and
> why the rules exist.

## Why the conventions look the way they do

Worth knowing before following any of them, because it separates what the
platform imposes from what is ours to change.

**The ServiceNow underneath is stock.** The `change_request` and
`change_task` tables, the `New → Assess → Authorize → Scheduled → Implement
→ Review → Closed` state machine, the Standard/Normal/Emergency types, the
Standard Change Template mechanism, conflict detection, and the approval
engine (one group member approves, the rest flip to `No Longer Required`) —
all out-of-the-box product behaviour. None of it was built here.

**The form on top is heavily FiServ.** In ServiceNow a `u_` prefix marks a
customer-created field, which makes the boundary countable: **45+ custom
fields against ~30 native ones**, so more than half the form is local. And
it isn't cosmetic — it is whole governance concepts the product doesn't
ship: `PITE` as a risk trigger, a five-field DR discipline, a **second CAB
tier** (`u_division_cab_*`, where the product provides one), the entire
expedite exception flow, client and product as first-class entities.

Signs it grew by accretion rather than design: `u_pite_v2` (rebuilt at least
once), `u_highrisk_reviewboard1` (a trailing `1` is the scar of a field
created, deleted, and recreated under a new name), `u_requesting_division`
labelled "Owning Division" (technical name and label drifted apart), and two
separate custom fields for the Standard Change Template when the product
already provides the mechanism.

**The consequence that matters.** The seven mandatory CAB questions live as
**free text inside `description`** — not as fields. ServiceNow ships a
native mechanism for precisely this, a risk-assessment questionnaire, which
would make them enforced, validated and reportable. It isn't being used. So:

- changes reach CAB with five of seven answered, because nothing enforces a
  count;
- identical work gets opposite `PITE` answers, because there are no
  canonical values;
- nobody can report on how many changes answered "yes" to question 7 and
  actually created the Command Center task, because prose isn't queryable.

Every convention in [[Work/FiServ/Opening a Change|Opening a Change]] exists to do the job the schema
doesn't, which is why they lean on **visible absence** (all seven questions
always present, `N/A` rather than deleted), **canonical forms** (one title
pattern, one `PITE` rule) and **verifiable assertions** (a claim in the text
must match the record). If the platform config ever changes, most of them
stop being necessary.

**How to read this.** Three different confidence levels are deliberately
mixed here, and the difference matters when a deadline is on the line:

- Plain statements are things seen working, usually more than once.
- Anything hedged in the text ("unconfirmed", "unclear", "verify before
  relying on it") is a single observation nobody corroborated. Treat it as
  a lead.
- [[#Open questions]] holds the known unknowns as numbered questions, so
  they can be answered deliberately instead of rediscovered in a scramble.
  When one gets answered, fold the answer into the relevant section and
  delete the question rather than leaving both.

**Names rot faster than rules.** Approver rosters, shift contacts and who
can set which flag have already changed once inside this doc's short
lifetime. Every person named here is "as of early September 2026" — verify
the human before relying on them, even where the surrounding process still
holds.

## Change types

Four types, and which one applies is mostly mechanical rather than a
judgement call:

- **Standard change** — pre-approved, low-risk, repeatable; leans on a
  Standard Change Template that already carries the approved steps, which is
  exactly why it skips CAB. For **low environments**: `Cert`, `Lower`,
  `Development`, `Testing`.
- **Normal change** — full review and approval flow, CAB included. For
  **high environments**: `Production`, `DR`, `Prod/DR`. No small-change
  exception: touching Production means Normal.

**The `Environment` field is what decides between them.** That is the clean
rule, and it also explains the recordings' talk of "Search" changes going
Standard — `Search` isn't an `Environment` value at all, so those map onto
`Cert` or `Lower`.

**The Standard Change Template is a named record on the change.** A real
Standard carried `Standard Change Template: CM - NO IMPACT NON PROD` — so
the template is chosen, visible, and states its own precondition in its
name: no impact, non-production. That is what carries the pre-approval and
lets the change skip CAB. Still unconfirmed: the full catalogue of
templates, and whether `Environment` alone decides the type or the template
choice does too.
- **Restricted change** — skips CAB presentation entirely in favor of an
  email-approval flow. See [[#Restricted changes]] below.
- **Emergency change** — for a Production issue that can't wait for a normal
  CAB cycle at all. See [[#Emergency changes]] below.

Engineering fills in the actual change content the same way regardless of
type — what changes between types is **who** opens/owns the record and
**when** approval happens relative to that.

## Change lifecycle

Six stages, the two CAB gates sitting on the `New` → `Authorize` hop:

The full `state` list is **New • Assess • Authorize • Scheduled • Implement
• Review • Closed**, with **Canceled** always available as an exit. Three of
them matter while you're chasing approvals:

| Stage | What's happening | Who approves |
|---|---|---|
| `New` | Editing. Only you can see it. | Nobody — you're still typing |
| `Assess` | **Group-level** approval, at your Assignment Group | Any member of your group |
| `Authorize` | The bar goes up: named, designated approvers | Specific approvers |
| `Scheduled` | Approved and booked, waiting for its window | — |
| `Implement` | Work happens | — |
| `Review` | Someone validates | — |
| `Closed` | Automatic after review | — |

The system spells the second state **`Assess`**, not "Access" — worth
knowing so you don't go hunting for the wrong label in a filter.

A cheeky but legitimate trick at the `Assess` step: since *anyone* in the
assignment group can approve, adding a group that contains someone you know
is sitting there ready to click will get you through faster.

**`New` is a real, private, indefinite draft state.** A change stays in
`New` forever until someone clicks `Request Approval` — people accumulate
half-finished ones — and while it sits there **only you can see it**. That
makes it the right place to park a change you can't finish yet.

**`Request Approval` is a one-way door.** Once clicked you can no longer
edit the change; getting edit rights back means getting it moved back to
`New`. So front-load the work: build everything you can, and only click it
when you actually mean to hand the change over.

This is what makes it safe to **start a change before the execution date is
known** — fill in everything available, create the execution meeting room
already, leave it in `New`, and when the date and time firm up, edit the
tasks with the real window. Being blocked on a missing date is not a reason
to sit idle.

**Standard changes don't have this hop at all** — no `Request Approval`
button, no `New` → `Assess` transition. See
[[#When a change won't leave the New state]].

**`Implement` is entered automatically, at the start of the maintenance
window.** Nobody moves it there by hand: a change scheduled 06:00–09:00
enters `Implement` at 06:00 on the day. This is why the window's start time
is load-bearing rather than decorative, and why a downstream tool asking for
a change number can reject a change that is approved but whose window hasn't
opened yet.

**The 17:00 gate is about being *already approved*, not about having
submitted.** The requirement is that the change is sitting in `Authorize` by
then — someone in your group has to have actually clicked approve. Submitting
at 16:55 and hoping is not the same thing.

## The two CAB gates

A change reaches the next day's CAB only if both hold at once:

1. **Cutoff time.** The change must be in the `Authorize` stage by **17:00**
   the day before. Anyone in the requester's own assignment group can approve
   it at the `Assess` stage, so approval does not have to wait on one
   specific person.
2. **Group backlog at zero.** One stale change in the group blocks CAB entry
   for every change from that group, including ones otherwise on time.
   **Read the right number.** On the `LATAM Changes Backlog` dashboard,
   filtered to your Assignment Group, the gate is the **gauge — the thing
   that looks like a car speedometer**, counting overdue and pending
   changes. It must read **zero**, and *green is not the goal, zero is*:
   green with one item on it still counts as not-zero. You want the needle
   pinned at the bottom.

   The `Changes in Implement State` tile is **not** the gate and is
   *supposed* to be above zero — approved today, implemented tomorrow is a
   perfectly healthy state. Two other panels flag genuinely rotten work:
   **Abandoned Changes** calls out anything sitting in `New` for **60+
   days** or stuck in the approval queue for **7+ days**.

   Look at the gauge **every single day**. It's a five-second habit that
   saves hours later.

Being on time with backlog present does not count, and zero backlog submitted
after 17:00 does not count either. Both gates have to hold together.

**Two open conflicts on this page's own sources, worth resolving before you
lean on either.**

*What the backlog actually blocks.* This page has it as blocking **CAB
entry** — that is what the recordings show, watching the gauge before
submitting. A separate written note states it more strongly: *"You cannot
open a Change while there is a pending Change sitting in the same Assignment
Group."* If that's right, the gate is earlier and harder than described
here. Either way the remedy is identical — clear the queue first — so this
changes when you find out, not what you do.

*The 17:00 cutoff versus "five minutes before the slot".* The recordings are
emphatic about 17:00 the day before. A written note says the change must be
in `Authorize` **at least five minutes before its CAB slot starts**. These
are probably two different things — 17:00 being when Change Management
sweeps changes onto the next day's agenda, and the five minutes being the
hard floor for a change already on it — but nobody has said so explicitly.
Treat 17:00 as the working deadline and the five minutes as the absolute
one.

**The triage rule that follows from this: fill only the mandatory fields,
submit before the cutoff, refine tomorrow.** Stated repeatedly as the
governing priority near a deadline — "the most important thing is that we
send it by five", "it's just to make the time". A change that made the
cutoff can be polished the next day; a perfect change that missed it cannot.
The same posture applies to child tasks: fill what's mandatory, save, move
to the next one.

**One field is exempt: the affected region.** It can't be deferred, because
it decides *which CAB the change enters* — get it wrong and the change goes
to the wrong committee, which no amount of next-day polish fixes. "Tomorrow
we can edit it. The problem is the affected region, that one we can't." A
team down to its last fifteen minutes before the cutoff spent them phoning
the implementer for the region rather than improving anything else, and that
was the right call.

**Once it's submitted, freeze it.** Don't go back and improve a change that
has already gone in — wait until it actually appears on the CAB agenda, then
edit. "Don't touch it now; once it's on the calendar we can polish it."

The backlog gate is checked **manually, live, right before submitting** —
someone pulls up the group's dashboard and looks. It isn't enforced by the
form, so if nobody looks, nobody knows.

## How approval actually works

The approval list on a real change makes visible what the recordings only
gestured at. One production change carried **29 approval rows in two
tiers**:

**Tier 1 — your own assignment group.** Roughly 25 individual rows, one per
member of `LTAM.3.API Development-LAT`, all created at the same second. One
person approved; **every other row flipped to `No Longer Required`**. That
is the concrete mechanism behind "anyone in your group can approve" — the
request fans out to the whole group and the first approval satisfies it,
cancelling the rest. It also explains the "please, for the love of God,
approve my change" messages: the requester needs exactly one of ~25 people
to click, and has no way to make a specific one do it.

**Tier 2 — the Change Management group.** Three further rows, on
`LTAM.3.Change Management`, still sitting at `Requested` while tier 1 was
already satisfied. These are the change managers themselves.

Two things follow that the rest of this page depends on:

- **Your group approving is necessary but not sufficient.** The change sat
  in `Authorize` with `Approval: Requested` because tier 2 hadn't acted.
  This is the same gap described in [[#Restricted changes]] as "getting
  every approval reply is not the finish line" — here it is, visible in the
  record.
- **The tier-2 names are the change managers named throughout this page**,
  which is why they can grant waivers: they are literally on the approval
  list.

A `Group approval List` mirrors this at group level — one row `Approved`
for the dev group, one row `Requested` for Change Management.

**A Standard change has only one tier.** Confirmed on a real approved
Standard: **8 approval rows, all in the requester's own engineering group,
one approves and the rest go `No Longer Required` — and no Change Management
tier at all.** That is the whole approval story. It is why a Standard change
turns around in minutes while a Normal one waits on change managers.

**Answering `Is this a code change? = Yes` adds two more tiers.** This is the
single biggest scheduling consequence on the form and it is invisible until
it happens. A code change pulled in:

| Group | Outcome |
|---|---|
| `LTAM.2.API Engineering-LAT` | Approved (own group) |
| `Cyber.F.Permit to Operate` | Approved — by an automated `OneTrust Service Account` |
| `LTAM.3.Change Management` | Approved |
| `CYBER.3.Application Security` | **still `Requested`** while everything else was done |

The approval count went from the usual 29 to **37**, most of the extra rows
being a global Cyber team in other timezones (and a couple of automated
approvers — `PTX Auto Approver`, `OneTrust Service Account`). So `Yes` on
code change doesn't merely trigger a scan, it **inserts two approval bodies
you do not control into your critical path**, one of which is awake on
someone else's clock. Plan lead time accordingly rather than discovering it
at 17:00.

**There are also two CAB levels on the form**, which the recordings never
mentioned: `Enterprise CAB date` / `Enterprise CAB notes` (`cab_date`,
`cab_recommendation`) and `Division CAB date` / `Division CAB notes`
(`u_division_cab_date`, `u_division_cab_notes`). These are written *by* the
CAB, not by you — you read them to find out what the board said and what
conditions it attached. All four were empty on every sampled change, which
suggests the LATAM day-to-day runs at Division level and Enterprise CAB is
reserved for bigger things.

## Backlog is about closing, not volume

"Backlog" does not mean too many changes; it means changes nobody closed.

A change sitting in `Implement` for more than two or three days without being
closed counts as backlog against its assignment group, and that is what trips
gate 2 above. Opening a change scheduled for tomorrow is fine even if the
group already has one queued for tomorrow — the problem is only changes left
open from a week earlier that nobody followed up on.

Whoever **opens** a change is responsible for **closing** it, and its
execution/validation tasks, right after the work is done. Closing the tasks
moves the change from `Implement` to `Review`; once validated it moves to
`Closed` on its own. If the implementer is unavailable, someone else with
access can close it on their behalf, but the default expectation is that the
implementer closes their own change.

**Closing one, step by step** (the procedure nobody restates because it's
"obvious"): click the overdue counter to list the offending changes, open
one, and **check that both child tasks are `Closed` first** — if they
aren't, go find their owner and ask whether they can be closed, since
closing the parent over open tasks isn't yours to decide. Then set
`Close code` = `Successful` and `Save and close`. The change moves to
`Review` and gets to `Closed` on its own from there.

Counters on this board lag. After closing, one number can go to zero while
the other still reads one; it catches up on refresh. Don't close a second
change chasing a stale display.

## Regional CABs

There is no single CAB — three committees, each covering different
countries:

| Agenda | Covers | Brazil time | UTC |
|---|---|---|---|
| **BR** | Brazil | 10:00 | 13:00 |
| **LAS** | Central America **and** South America | 16:00 | 19:00 |
| **LAN** | North America — for us, only Mexico | 17:00 | 20:00 |

Two corrections to what this page previously said, both from the live
agenda rather than from audio: the third agenda is **LAN**, not "LAM", and
**LAS runs at 16:00**, not 15:00 — which settles the long-running
15:00-vs-16:00 conflict. Note also that LAS covers **Central** as well as
South America, so Panama-region work goes to LAS, not to the North America
agenda.

There is a separate **US** agenda that isn't ours. Brazil is UTC-3, so the
UTC column is simply Brazil +3.

**These are meeting times, not deadlines.** The change must be sitting in
`Authorize`, with approval emails attached and everything ready, **at least
five minutes before its slot starts**, or it isn't picked up. CAB does not
do "almost".

A change affecting systems in more than one region has to be presented at
every CAB that covers those regions — a client tied to a single region
(e.g. AFINZ, a Brazil client) is presented at that region's usual CAB slot,
so an AFINZ change goes to the 10:00 Brazil CAB, not a client-specific one.
This is why the `Which Latin regions or countries will be affected?` field
is load-bearing: each region has a **different committee with different
approvers**, so the answer decides who reviews the change, not just how it
reads. Answer with actual countries, never a blanket "LATAM".

**The field is not blank — it arrives pre-filled with `CAC`.** A wrong
default sits there and will be submitted verbatim if nobody looks at it.
Treat this field as one you always actively set, not one you fill in when
it's empty.

Region **codes** are acceptable answers, not just country names — `LAM and
BR` was given and accepted. The real rule is narrower than "always name
countries": don't answer with an undifferentiated "LATAM" that fails to say
which committees are implicated. And if a change spans all three regions, it
goes to all three CABs.

The region is whichever region the change actually touches, not where the
team asking for it sits, and not whatever a past, similar-looking change
used. In one case the Jira card's stale reference to "Parque Patricio,
Argentina" had nothing to do with the change actually being opened — the
real answer (confirmed by calling the implementer directly) was `LAM` and
`BR`. When the card doesn't say plainly, ask the person who will run the
change, don't infer it from an old ticket.

Region code glossary, as used in change descriptions:

- **CAC / CAQ** — Central America code, in practice means Panama.
- **PRO / Procesa** — same as CAC/CAQ, an alias for Panama seen in some docs.

**Multi-region costs a whole working day, not just a second form.** A change
touching Brazil plus another region has to be defended twice, at both
committees — one observed case ran the 10:00 Brazil session and then the
late-afternoon session for the other region, same person, same change. Budget
the day, and treat over-declaring the affected region as expensive rather
than merely tidy.

**These times move.** The non-Brazil afternoon slot was rescheduled from
17:00 to 18:00 at some point, said plainly on one call ("they moved it from
5 to 6"), which is the most likely explanation for the unresolved
15:00-vs-16:00 LAS discrepancy above: the table isn't a constant, it's a
snapshot. Confirm the current slot before planning a submission around it.

## Defending a change at CAB

Getting into CAB is not the same as getting through it. Someone has to be in
the session to present the change and answer for it, and if nobody shows,
the change fails there regardless of how well the form was filled.

A working opening, used verbatim on a client-impact fix: state who is
affected and why it can't wait — "we have a problem, a client of *(client)*
identified an error in production, the client is certifying in production,
we need to deploy to fix the errors and resolve this client's problem."

Then expect three questions, and have the answers ready before walking in:

1. **What's the rollback strategy?** — e.g. "we have blue/green."
2. **What's the downtime or impact?** — e.g. "no impact, no downtime."
3. **Which other regions does this affect?** — e.g. "it's segregated, we're
   only in *(our region)*."

These map exactly onto fields already on the form (`Backout Plan`, the
impact question, the affected-regions question), which is the real reason
those fields matter: they are the script for this conversation, not
paperwork. A change whose form answers can't survive being said out loud is
a change that will be argued with.

If a required reviewer drops out mid-session, the standing fallback is the
next day's high-risk CAB — but a specific reviewer can also be chased
individually over Teams to come back and review on the spot, which has
worked.

**The presenter doesn't have to be the SME, and sometimes shouldn't be.**
On one change the engineer who knew the system declined to present, for a
political reason — he'd been arguing the day before about whether
development should have to clear vulnerabilities to ship code, and didn't
want to reopen it in front of the committee. The workaround is the useful
part: **the change author presents, and sends the SME the anticipated
questions in chat beforehand** to answer, so the presenter walks in briefed.
Expect to be interrogated — "they ask so many questions at CAB" is the
standing complaint.

## Risk, Impact, and the weekday split

`Impact` is a field you set. `Risk` is computed by ServiceNow from a
combination of fields (impact among them) and is not editable — setting
`Impact` to `High` does not force `Risk` to `High` if the rest of the ticket
doesn't support it, and a `Risk` of `Moderate` has still been seen going to
pre-approval, so the mapping isn't a clean one-to-one.

`Risk` decides which CAB weekday the change belongs to, and this is easy to
discover too late:

- **Monday / Wednesday / Friday** — the normal committee, for Low/Moderate
  risk.
- **Tuesday / Thursday** — a separate, high-risk committee, for changes that
  carry real production risk (e.g. a direct code change to Production).

**The split blocks in both directions, and the direction that actually bit
this team twice is the less obvious one:** a **Low** risk change cannot be
presented on a Tuesday or Thursday either, because the normal committee
simply does not sit those days. It isn't "high risk needs a special day" —
each committee only hears its own class of change. A High Risk change is
stuck off Mon/Wed/Fri, and a Low Risk change is stuck off Tue/Thu.

Check the computed `Risk` field early, before committing to a presentation
date, not after. The cascade from getting this wrong is worse than it
sounds: one change built on a Wednesday for a Thursday CAB lost the slot
mid-call on discovering Thursday was high-risk-only, and the fallback was
presenting Friday to execute **Monday** — four calendar days from one
unchecked field. The root cause named on the call: the change should have
been built Tuesday, presented Wednesday, executed Thursday morning.

The reverse case has a named fix: a section called **`Expedite`**, which
gets a change into a CAB session it would not otherwise qualify for. It
lives inside the same change form, not as a separate record: click the
`Type: Normal` text near the header action row (by the Discuss/Follow
buttons) to reveal it further down the page, then set its `Expedite Type`
dropdown.

**Using Expedite changes the record's `Type` to `Expedited`** — it is not
merely a section you fill in. A real expedited change carries
`Type: Expedited` in its header where an ordinary one reads `Type: Normal`.

Filled in for real, the section is short:

| Field | Value |
|---|---|
| `Expedite type` | `Did Not Meet Lead Time OR Missed CAB` |
| `Why can't your change be rescheduled for representation in CAB?` | "Did not meet lead time" |
| `Reschedule Reason` / `Which Business Unit Requested Modifications?` | left blank |
| `Have the impacted Division's been notified and approved the Exception request?` | left blank |

**The full `Expedite type` list** (`u_request_exception`) — nine values, and
the one used on the sampled changes is only one of them:

`BU/CAB Requested Corrections` • `BU/CAB Requested Reschedule` •
`Did Not Meet Lead Time OR Missed CAB` • **`Expiring Certificate`** •
`Go Live` • `Outside MW` • `Urgent Customer Request` •
`Urgent Cyber Request` • `Urgent Incident Fix`

That "missed the lead time" and "missed CAB" share one value settles the
earlier confusion about whether Expedite was about weekdays or deadlines:
**it covers both, under one option.**

**Worth noticing:** the certificate renewals in the sample were expedited as
`Did Not Meet Lead Time OR Missed CAB`, when **`Expiring Certificate` is a
dedicated value for exactly that situation**. Both get you expedited, but
one describes the actual reason and the other says "we were late". Given
CAB reads these, pick the one that's true.

Some changes skip CAB entirely via automatic pre-approval. This isn't purely
`Risk = Low` — it's a system classification driven by a combination of
fields, so don't assume a `Moderate` or non-trivial change is CAB-bound by
default; check what the ticket actually says after saving. Two concrete,
contradictory-looking data points confirm this isn't just `Risk`-driven: a
change computed as `Low` risk still went to full CAB (no pre-approval),
while a separate change computed as `Moderate` was pre-approved
automatically — the team explicitly doesn't know the full rule set ServiceNow
uses here, only that `Risk` alone doesn't predict it.

**One confirmed driver of the Risk calculation:** no pre-implementation
test evidence pushes the computed Risk straight to **Very High**. Stated
causally on a call — "especially when there's no CAT test, it automatically
sets Very High" — and now confirmed end to end on a real change export,
where the whole chain is visible in one record:

| Field | Value |
|---|---|
| `PITE` | `No, I did not complete` |
| `Why was Pre-Implementation Testing not performed?` | "It is a certificate renewal" |
| `Risk` | **`Very High`** |
| `Impact` | **`1 - High`** |
| `HighRisk Review Board` | **`true`** |

Compare with changes that answered `PITE: Yes, I completed outside
ServiceNow` — those came out `Risk: Moderate`, `Impact: 3 - Low`. **`PITE`
is the single field that flips a routine change into the high-risk track.**

**But it does not set `HighRisk Review Board` — that flag is manual.** Two
near-identical certificate renewals, same author, same template, both
`Risk: Very High`: one carries `HighRisk Review Board: true`, the other
`false`. Nothing else distinguishes them. This matches what the recordings
said — someone with the right permission ticks it when asked — and it means
**a Very High change can reach `Authorize` without the flag ever being
set**. Don't assume the system caught it for you; check the flag explicitly
on any Very High change.

**`Sage AI Risk Score` is not the `Risk` field and doesn't track it.**
Observed pairings: 67 → Moderate, 67 → Very High, 65 → Moderate, 61 →
Moderate, 55 → Very High. The lowest score in the sample sits on a Very High
change and the joint-highest sits on a Moderate one. Treat the AI score as
advisory noise until someone explains what consumes it.

The mechanics, since the dropdown wording matters. The UAT/CAT question
offers, among others, **"Not required for open, very low risk"** and
**"Not completed"**. A change that is genuinely high risk can't honestly
take the first, so it takes `Not completed` — which then reveals a free-text
field labelled **"Why was pre-implementation testing not performed?"**, and
that combination is what triggers the Very High recalculation. If a change
truly has no functional testing to attach (a pure certificate renewal with
zero code change), expect the jump and have the justification ready rather
than being ambushed by it.

There's a real tension here worth naming: another session established that
when a client can only certify in Production, the right move is a written
statement of that fact instead of attached evidence. That is correct
process, and it will still drive Risk to Very High. Both things are true —
plan for the risk level rather than fabricating evidence to dodge it.

**High Risk changes need a "High Risk Review Board" flag marked**, on top
of the computed `Risk` field and the Tuesday/Thursday weekday rule. It isn't
self-service for everyone; in one case only a specific person (Sebastião)
could set it. It's easy to get a change approved and only later notice the
flag was never marked — check for it explicitly on any High or Very High
change rather than assuming approval covers it.

**The flag corresponds to a real meeting: HRRB.** A `High` risk production
change came back with its `Division CAB notes` reading *"Approved in HRRB
BRA"*, followed by three named sign-offs by role — **PSH**, **ICTO** and
**CS** (Client Service). So the board is regional (BRA here), it minutes its
decision onto the change, and it wants named owners from three different
functions. If your change is High or Very High, expect to be presented there
and to need those three, not just a tick.

## Clients and their special handling

Two clients carry extra process on top of everything else, and missing
either one stalls a change that otherwise looks CAB-ready. Others appear
regularly without special rules — as far as anyone has said out loud:

| Client | Special handling |
|---|---|
| **SPIN** | Automatically High Risk; extra post-CAB business approval. Below. |
| **AFINZ** | Business sign-off from Client Service before approval clears. Below. APM `9941`. |
| **Vivenda** | None known. Appears in Restricted changes with named approvers. |
| **MCB** | None known. Has its own change template; one recent change was an onboarding. |
| **Sem Parar** | None known. Toll-tag client; work splits across infra/UI/API changes. |

Absence of a rule here means "nobody mentioned one", not "there is none" —
verify before assuming a client is ordinary.

- **SPIN** — any change touching SPIN is automatically **High Risk**,
  regardless of how small the actual impact is. Plan for a Tuesday/Thursday
  CAB unconditionally; don't bother checking the computed `Risk` field
  first, it doesn't matter here.
- **AFINZ** — needs sign-off from its Client Service contact (**Romano**)
  before the change can be approved. Email him, attach the reply to the
  change, and only then does the pending approval clear.

**Client Service contacts are the bridge to the client**, and each special
client has their own: **Romano** and **Carlos Wilson (São Paulo)** for
Brazil/AFINZ — both must be notified by email on any AFINZ change —
and **Laura Azevedo** for SPIN. They brief the client; you don't contact the
client directly.

**The AFINZ pending flag is raised by CAB itself.** It isn't a form
validation you can spot beforehand: the change is presented, CAB notes the
missing client communication as an outstanding item, and the change then
sits unapproved waiting for you to do it and attach the evidence. Watched
live, with the moral stated plainly — "if I'd communicated beforehand, taken
the email, attached it, the change would have been fine." Send it before
building the rest of the change, not after presenting.

**SPIN's client communication applies even to non-production changes.** A
Search-environment SPIN change still required the Client Service heads-up,
because the client reacts to any availability blip regardless of
environment: "it's not production, but since it's SPIN it has to be aligned
too — if something goes wrong in Search they also make a scene."

**SPIN has one more gate after CAB.** Once CAB approves a SPIN change, it
still needs a final sign-off from a VP or equivalent on the client side —
named as **Eva** — and that approval is the slowest, least predictable step
in the whole process. Other directors are reportedly easier; this seat is
the one that hurts — changes have been stuck waiting on
it right up to the execution window, with no guarantee it lands in time.
Because of this, the team now deliberately presents SPIN changes at
Tuesday's CAB even when execution isn't planned until Friday, buying
Wednesday and Thursday as slack to chase that final approval down before
the window opens. Pad SPIN lead time specifically for this reason; it isn't
needed for a typical change.

One SPIN email-approval reviewer (Hugo Martinez) was known for asking
pointed clarifying questions before signing off — e.g. why something
Venafi should handle automatically is going through manual approval
instead — and for approving "with a scolding" attached even when he does
sign off. As of the most recent recording, **Hugo has moved to a different
department and Nelson Salazar approves in his place** for at least this
flow — expect the reviewer roster here to keep shifting, and verify who's
currently in the seat rather than assuming Hugo (or Nelson) is still it by
the time this is read.

## Coordinating other teams before you start

If a change needs another team's involvement — most often Infra — reach out
to that team's contact **before** building the change, not after. That
team's representative sits at the CAB session, and if the change references
a task for them that they were never consulted on, they will contradict it
live at CAB and the change fails to get approved on the spot.

The practical flow: message the other team informally to set expectations
(what needs doing, target date/window), then formally reference the change
itself once it exists. If the change requires that team to run a step, the
change has to spell out that step-by-step precisely — they execute strictly
off what the ticket says, so an incomplete step list means they either
guess or refuse to act.

**Meeting link for execution.** The change needs a meeting link attached
(an `on-call`/meeting field) for the actual execution window:

- **Engineering-only change** — create the meeting yourself in your own
  calendar tool, put the change number inside the meeting invite, then
  paste the meeting link back onto the change.
- **Change involving another team** — that team creates and owns the
  meeting invite (their tooling generates the link for their own group),
  and hands the link back to you to attach to the change. Confirm the
  execution date/time with them explicitly; the link doesn't exist until
  they generate it.

**Infra's own escalation ladder.** When the ambiguous problem is on the
infra side, it doesn't go straight to whoever executes:

1. **Middleware** investigates first when the right steps aren't already
   known.
2. Once Middleware determines the steps, it hands off to **Infra** to
   execute the routine work.
3. If Middleware itself can't figure it out, it escalates further to
   **Infrastructure Engineering** — a tier nobody on one team had ever
   needed to reach.

Infra runs two shifts (day, and a night shift roughly 19:00–03:00, with
some staggered variants), and who's actually on duty on a given night isn't
requestable in advance. **Raul (Raul Bernardo Santos)** is the day-shift
contact for booking an Infra execution window, with **Léo** as his backup;
**Marcelo (Hideki) Nakashima** covers nights — whichever of them is off
shift hands a window-booking request to the other. Infra reports up to a
manager named Nelson Croc.

When Infra can't be reached in time through normal channels, a direct,
practical unblock is to open a dedicated Teams group chat naming the
specific people involved (e.g. Raul, Nakashima, the requester, the
engineer), paste the change's number/link into it, and ask directly "how
can we act on this" — more reliable under time pressure than a general
channel post.

**Fast individual approval via Teams.** On a change's approver list, the
"Request" link next to one specific approver's name can be copied and
pasted into a Teams chat — it renders there as a live Approve/Reject button
visible only to that person, letting them approve straight from chat
without opening ServiceNow. Useful specifically when a cutoff (17:00, or
whichever deadline applies) is about to be missed and one named person's
approval is the last thing blocking it.

## Filling the form

Order follows the form as it appears when creating a Normal Change for
Production.

**Everything on the change is written in English.** Not a style preference —
stated as the standard, "it all has to be in English." Jira cards routinely
arrive in Portuguese, so translating them is part of the job: preserve the
meaning, don't paraphrase away detail, and drop anything that duplicates a
structured field rather than translating it twice.

**Getting to a new change from scratch:** search the menu for `change` →
`Create New` → first option → `New request` → `Open new normal change`. The
picker there offers Standard and Normal; the rule at that screen is simply
"if it's Production, it's Normal." `Assignment group` sits third on the
right and arrives defaulted from your user, so it needs checking rather than
accepting.

### Header fields

| Field | What goes there |
|---|---|
| `Assignment group` | The requester's own group. CAB entry depends on this group's backlog (see above). |
| `Environment` (`u_environment`) | Where the change actually lands, and **this is what quietly decides Standard vs Normal**. Seven values: `Production` · `Development` · `DR` · `Testing` · `Prod/DR` · `Cert` · `Lower`. The low four lean Standard; `Production`, `DR` and `Prod/DR` mean Normal and a trip to CAB. Don't copy the Jira card's wording blindly — a card saying "prod/DR" is describing the system, not dictating this field. |
| `Will this change require an update to DR?` | **A separate field from `Environment`.** The test is not *does a DR instance exist* but **are you touching both**: on a shared RabbitMQ served by a single server, the ruling was "it's one server — you'd only tick it if you were changing Prod *and* DR." |
| `Will the DR update be included in this change?` | The follow-up when the above is `Yes`. `Yes` means one change covers both sides — pair it with `Environment: Prod/DR` and give DR its own execution task. |
| `Why doesn't this change need to be completed in DR?` | Appears when the DR question is `No`. One line is enough, and the good answers are specific: "It's just a change of encryption keys", "This change is for DR" (i.e. this *is* the DR one), "Not required", "NA". |
| `Category` (`category`) | The broad area of tech being touched — it routes the change to the right people and reports. `Application` · `Data Center` · `Database` · `Distributed System` · `Corporate - Desktop` · `Corporate - Mobility` · `Mainframe` · `Midrange` · `Network` · `Cloud Migration` · `Storage` · `Security`. An API change is `Application`. |
| `Sub-Category` (`u_sub_category`) | Stays empty until `Category` is picked, then offers a different list per category — see [[#Sub-Category options, by Category]]. Basically the verb: what you're doing to the thing. |
| `Configuration Item` (CI) | The datacenter/server the changed application actually runs on. The single most error-prone field on the form — see [[#Getting the Configuration Item right]] below. |
| `Business Division` | The team's own business division, e.g. `Issuer LATAM` — not the client's. |
| `Project Scope` (`u_project_scope`) | Why the change exists, in business terms. Seven values — see [[#Choosing a Project Scope]] below. |
| `Is this a code change?` (`u_code_change`) | `Yes` if any code or dependency changes at all, even a pure runtime/library version bump with zero logic change (e.g. JDK 8 → 17). It's about whether code is touched, not whether business rules changed. **`Yes` has a real consequence:** Cyber Security automatically runs the code through the company's vulnerability scanners. No manual step, no opting out — which is also why answering `No` on a change that does touch code is not a shortcut, it's an evasion. |
| `Impact` | Usually `Low`, even in Production. `High` is for the system actually being down, a fix touching every customer, **or a shared cryptographic/infrastructure dependency** — an HSM certificate renewal was set to `High` despite being a trivial change, because everything needing a PIN block goes through that HSM. Judge by blast radius, not by effort. |
| `Change management group` | Arrives pre-filled — literal value on a real change: `LTAM.3.Change Management`. Left alone. This is the group that provides the *second* approval, separate from your own group's; see [[#How approval actually works]]. |
| `On Behalf Division` | The division(s) the change is being made on behalf of, e.g. `Merchant LATAM, Issuer LATAM`. Required — a blank one blocks `Check Conflict`. |
| `Owning Division` | Your own division, e.g. `Issuer LATAM`. Distinct from the above. |
| `Clarity Project ID` | The `PR` number, e.g. `PR25004659`. This is the field the recordings call "the PR" / "UPR". |
| `PITE` (`u_pite_v2`) | Pre-Implementation Testing Evidence — did you test this, and can you prove it. **Four options:** `Yes, I completed in ServiceNow` (evidence lives in the tool) · `Yes, I completed outside ServiceNow` (evidence lives elsewhere — link it) · `No, I did not complete` · `Not required for Low and Very Low risk`. Answering `No` opens `Why was Pre-Implementation Testing not performed?`, and that combination is what drives Risk to Very High. A blank reason is not acceptable; the board reads these. |
| `Sage AI Risk Score` (`u_ai_risk_score`) | A number from ServiceNow's Sage AI review. Higher means riskier *in the AI's eyes* — context, not the verdict, and it does not track the real `Risk` field. |
| **System-set, read-only** | `Heightened Awareness` (`u_heightened_awareness`), `Critical Client` (`u_critical_client`), `Vital Business Function` (`u_vital_business_function`), `Task outside maint window` (`u_task_outside_maintenance_window`). You never toggle these — they are the system telling *you* something. `Critical Client` lighting up means a critical client is in the blast radius; `Task outside maint window` means work is happening outside the agreed window. |
| `HighRisk Review Board` (`u_highrisk_reviewboard1`) | **An ordinary on/off checkbox, not system-set** — which is why a Very High change can reach `Authorize` with it still false. Someone has to tick it. |
| `Unauthorized` (`unauthorized`) | Checkbox. If this is on, something has gone off the rails. You should never see it set. |
| `Why doesn't this change need to be completed in DR?` | A conditional follow-up that appears when the DR question is `No`. Short justification, e.g. "It's just a change of encryption keys". |
| `Playbook` | Seen set to `Not required`. |
| Downtime/duration | A dropdown; `Less than 30 minutes` was the value chosen on a change expecting a ~5-minute interruption, so the buckets are coarse. |
| `Risk` | Auto-computed from a combination of fields, not editable directly — see [[#Risk, Impact, and the weekday split]]. SPIN changes are High Risk unconditionally, see [[#Clients and their special handling]]. |
| `Related Project ID` / PR | Link the Jira card / PR if available, or the initiative's project code (e.g. a `BRD` number). If the requester never gave a PR/UPR number, use the security process ID instead (e.g. the `MAP` ticket title/number) rather than inventing a placeholder or leaving it looking unfilled. |
| `Escalation Contact` | Required. The person responsible if execution goes wrong — **defaulting to the person who requested the change** is the observed practice. |
| `Backout Plan Duration` | Separate from the Backout Plan text. A coarse dropdown; `less than 30 minutes` was the observed value. |

Several fields (`Business Division`, `Escalation Contact`, and others) are
behind a small padlock icon: click it to unlock the field, type the value,
click it again to lock it back in — it won't save otherwise.

### Sub-Category options, by Category

The full map, so you don't have to click through all twelve to find out:

| Category | Sub-Category options |
|---|---|
| Application | Add · **Deploy** · Modify · Remove · Restart · Start · Stop |
| Data Center | Add · Modify · Remove |
| Database | Backup · Add · Modify · Remove · Restore · Query · Maintenance |
| Distributed System | Windows · Unix · Linux |
| Corporate - Desktop | Add · Remove · Modify · Configure · Install · Repair · Restore · Restart |
| Corporate - Mobility | Add · Remove · Modify · Configure · Install · Repair · Restore · Restart |
| Mainframe | Add · Remove · Modify · Restart (Maintenance) |
| Midrange | Tandem · Iseries · Unisys · OpenVMS |
| Network | Add · Remove · Modify · Restore · Restart/Reload |
| Cloud Migration | *(none — leave it blank)* |
| Storage | Add · Remove · Modify |
| Security | Add · Remove · **Modify** · Restart/reload |

The two combinations that cover most of this team's work: `Application` /
`Deploy` for a deployment, `Security` / `Modify` for certificate and TLS
work.

### Getting the Configuration Item right

The CI is where this form goes wrong most often, and it fails quietly: a
wrong CI can pass both pre-submission checks and still leave the change
unapprovable (see [[#Before submitting for approval]]).

- **Match the application, not the name.** A CI can be labelled after one
  project while the application under change lives somewhere else entirely
  — e.g. a CI named for "Latam ShareTokenization" when the app actually
  runs under "Latam ShareToken APIs" at datacenter "Omaha". Two changes can
  also target the "same" datacenter under different CI names because they
  are different applications (a messaging broker vs. an API) that don't
  share a host.
- **Never copy a CI from a similar-looking change**, including a change you
  cloned as a template. This is the single most repeated mistake across
  sessions.
- **Look it up by APM number instead of guessing.** Search ServiceNow by
  the client's APM number in the top-right search box (e.g. `PM0009941` for
  AFINZ); the record's "organization" view lists every CI genuinely tied to
  that client. This is the fix for the guessing problem above, not just a
  warning about it.
- **Prefer the group-level CI when a group covers several servers.** If the
  change touches more than one server inside a CI group — e.g. AFINZ's
  lower/dev group `Latam ForteVision RPAs Brasil Integration Lower` — select
  the group, not one member server.
- **The search box needs an exact string match.** A near-miss typed or
  dictated string silently returns nothing, which reads like "the CI doesn't
  exist" rather than "you typed it slightly wrong".

**The CI encodes the environment, and lower/Search maps to the
`Certification` variant.** CIs are named `<datacenter> in <environment>`, so
the same datacenter appears twice: AFINZ work goes to `Parque Patricios in
Production` for Production and `Parque Patricios in Certification` for
Search. Worth pairing with the "cert is ambiguous" gotcha below — here the
Certification CI is the *correct* pick for a lower-environment change, which
is exactly why the ambiguity bites.

**Client region and CI datacenter are different axes.** AFINZ is a Brazil
client whose application runs on the **Parque Patricios (Argentina)**
datacenter. Don't reason from "it's a Brazil client" to a Brazil datacenter,
or vice versa — the region answer on the form and the CI are answering
different questions.

**Child tasks have their own `Datacenter` field, separate from the header
CI**, and the same physical place often carries a different name in each:
"you put one thing in the Configuration Item, one name, and in the datacenter
field it'll be another name. Some are the same, others they name
differently." Fill both, and don't assume the string carries over.

Known-good examples collected so far: `Omaha` (Latam ShareToken APIs,
Production), `Hortolândia` (Vision Plus Brazil, Primary and Secondary/DR),
`Parque Patricios` (Argentina, Production and Certification variants),
`Latam ForteVision RPAs Brasil Integration Lower` (AFINZ lower, a group CI).

### Choosing a Project Scope

**The full list, read off the live form** (`u_project_scope`). An earlier
version of this page listed values transcribed from audio, and three of them
were wrong — "Future Update" is really **Feature Updates**, "Existing Client
Defect" does not exist, and "Vulnerability Patch" is **Vulnerability /
Patching**. Use these:

| Value | Use for |
|---|---|
| `BAU Maintenance / Housekeeping` | Routine upkeep. Certificate renewals live here. |
| `Defect / Incident Related` | Fixing something broken in production. |
| `Existing Client Conversion` | Migrating an existing client. |
| `Existing Client Expansion` | Growing an existing client's footprint. |
| `Feature Updates` | New or changed functionality. |
| `New Client Implementation` | Onboarding a new client. |
| `Vulnerability / Patching` | Security remediation and patching. |

**This changes a decision recorded earlier in the doc's own history.** On an
infra TLS remediation the team debated "Housekeeping vs Future Update" and
chose the latter. There is no "Future Update" — the real neighbours are
`Feature Updates` (wrong, no functionality changed) and
`Vulnerability / Patching` (right, it was a security remediation). The audio
sent them to a value that doesn't exist.

**Calibration on two of them.** `BAU Maintenance / Housekeeping` is the
value for a certificate renewal — confirmed on real changes whose business
reason reads "Routine Maintenance - Renew and update SSL certificates
nearing expiration…". The team had avoided Housekeeping out of
unfamiliarity ("I've only ever opened Feature Updates or Vulnerability"),
not because a rule excluded it.

`Defect / Incident Related` covers "something is wrong in production and
this change corrects it" — it was used for replacing broken encryption keys,
not only for formally raised Incidents.

### Short description, description, and the two plan fields

Each of these fields gets its own slice of information — don't repeat the
full story in all of them:

- `Short description` — one line, identifying the work as what it is, e.g.
  "Remediation, MAP 28353" — CAB reviewers recognize `MAP` as the security
  remediation process by itself, no need to spell it out. Don't put the
  **APM** number here — that goes in `Description`.

  **The real convention is `<scope> - <what>`, and the prefix varies.**
  Three real changes:
  - `[PR25004659] Banco Agricola - Tokenization MasterCard`
  - `MCB - Deploy fix to resolve incident impacting authorizeService transaction`
  - `LATAM - Certificates renewal and installation for "stlp2camapp0003.1dc.com"`

  So the prefix is whatever identifies the scope — a PR number in brackets,
  a client code, or just `LATAM` — followed by a dash and a plain statement
  of the work. The constant is that **something scannable comes first**, not
  which thing it is. The verbal guidance ("put SPIN or AFINZ in brackets")
  is the same instinct, stated more rigidly than practice actually follows.
- `Description` — the fuller explanation: what is being done and why, e.g.
  "Applying the required TLS fix on RabbitMQ Production instances as part of
  the remediation of MAP 28353 (Latam Tokenization Digital Wallet
  Solution)." Someone reading only this field at CAB should understand what
  is about to happen.
- `Explain Business Reason` — why this matters to the business, kept short:
  usually a compliance/continuity statement, e.g. "Ensure secure
  communication compliance, remove outdated TLS protocols."
- `Validation Plan` — the concrete steps that prove the change worked, tied
  to what actually changed (not a generic checklist).
- `Backout Plan` — what gets executed if the change fails, specific to the
  action: invalidate the credential just created if it was a credential
  change; redeploy the previous stable image if it was a deploy; and so on.
  Every change type has a different backout, because it undoes a different
  action.

### Using AI to draft these fields

Requesters usually hand over one line, e.g. "cria uma change pra deploy do
próximo IPD". A workable prompt pattern: paste that single line into an AI
assistant and ask it to produce, for change-verification-and-release-update
context, the `Explain Business Reason`, `Short Description`, `Description`,
`Validation Plan`, and `Backout Plan` fields from it. Review the output —
it tends to be more verbose than needed, so trim rather than paste
verbatim, and make sure every step from the source ticket survives (a
step-by-step plan trimmed to "just the first three steps" is wrong even if
it reads fine).

**Few-shot beats zero-shot, and it's what the team actually does.** The
better prompt isn't "write me a backout plan" — it's *"read this backout
plan from another change and, using it as a base, create one for my
scenario"*, with a real prior plan pasted underneath. Same for the
validation plan. Two other habits worth copying: **tell the AI explicitly
when the environment is non-production**, because the treatment differs, and
expect to cut length aggressively — the standing complaint is that it "gets
prolix, repeating itself redundantly."

A general-purpose assistant (Copilot, etc.) has no notion of FiServ's own
vocabulary and will confidently guess wrong on company-specific acronyms —
e.g. it once expanded `DDA` as a credit-card term when, in this context, it
means "debit-driven accounts". Sanity-check any domain-specific term the AI
fills in against what the team actually means before it goes in the ticket.

### The required questions

**There are exactly seven, they are mandatory only for changes going to
CAB, and they live inside the `Description` field** — they are not separate
form fields, they're a block of prose written into the description body.
That's why they're easy to under-fill: nothing on the form enforces them.
One session found itself with only five answered and had to hunt the rest
down mid-call.

Two consequences worth knowing before you start typing. A **Standard change
doesn't need them at all** — most of them don't even apply to a lower
environment, and the guidance is to save one good description and reuse it
verbatim on future Standard changes, since it's substantially the same
every time. And in practice the answers usually come from the requesting
SME as a block of text in chat, which the change author then adapts — you
are rarely inventing these from scratch.

**The seven, verbatim**, transcribed from a real approved change's
`Description`, with that change's own answers as worked examples:

1. **Which LATAM regions / countries will be affected?**
   → "LAN - Panama - Agricola"
2. **Has UAT completed testing and signed off on the change?**
   → "Yes, signoff attached."
3. **What is the expected impact to businesses and clients?**
   → "No impact expected."
4. **Is it necessary to inform the client?**
   → "No. No impact expected and the deployment will be executed outside of
   work hours." — note the answer gives a *reason*, not just "No".
5. **What other applications/services could be impacted?**
   → "Only mastercard outbound application will be impacted."
6. **What is the impact if this change were to fail?**
   → "If the change fails the rollback. None, first time deploying the code
   in production."
7. **Could this change trigger alerts? Should we inform the command
   center?** → "Yes, command center was informed and a task created."

Question 7 is the one that eluded every recording, and it explains something
the doc had recorded only as an unexplained obligation: **the Command Center
task exists because question 7 commits you to it.** Answering "yes" and not
creating the task leaves the change contradicting itself.

Two fields that look like members of this set but are **not** — they are
separate form fields elsewhere on the change, both confirmed on the same
export:

- `What functionality is impacted during the change implementation?`
- `Client post-implementation validation` (e.g. "No - Internal Validation
  Only")

Answers are short. None of the seven ran past two sentences on a real
production change, and the tone is factual rather than defensive.

**The strongest description format seen** opens with a structured header
before the prose, then prefixes each answer with `A:`:

```
APM: APM0009941
Client: AFNZ BRAZIL
Application/Service: Issuing API
Environment: Production and Disaster Recovery

<two short paragraphs: what the change does, and what it does NOT touch>

1. Which LATAM regions / countries will be affected?
A: Brazil - AFNZ BRAZIL Issuing API services in Production and DR.
```

Explicitly stating **what is out of scope** ("The activity is limited to
certificate renewal… No application code or functional behavior will be
changed") does real work at CAB, because it pre-empts the reviewer's first
suspicion.

**A caution on question 2 and `PITE`.** The same class of work is being
answered inconsistently across changes: certificate renewals where one
author sets `PITE: No, I did not complete` with "It is a certificate
renewal", and another sets `PITE: Yes, I completed outside ServiceNow` and
answers Q2 "UAT is not required because this is a certificate renewal with
no application code or functional changes". Both are defensible readings of
"did you test", but they land in **completely different risk tracks** —
Very High versus Moderate — for identical work. Whatever you answer, know
that this field, not the nature of the change, is what sets the track.

### Reusable phrasing, from a certificate-renewal change

- Short description: "Renew of Vision Plus Brazil HSM Certificate Before
  Expiration."
- `Explain Business Reason`, kept deliberately short and non-technical:
  "To make sure all services will remain available" — rather than a deep
  explanation of the underlying encryption/TLS mechanics.
- UAT/CAT test-evidence question, for a change with zero functional/code
  changes: "UAT testing is not applicable because the change only renews
  the existing HSM certificate and does not introduce application or
  functional changes. The certificate should be technically validated for
  installation, followed by a controlled production validation."
- Expected business impact: "An estimated 5-minute service interruption,
  which is the time it takes for the HSM router to load the renewed
  certificate." — impact phrased around the actual mechanical cause of any
  downtime, not a generic estimate.

**Cloning an existing change as a starting point.** Under time pressure, a
faster path than filling the form from scratch is finding a prior similar
change (e.g. an earlier cert-renewal or Restricted change for the same kind
of work) and duplicating it, updating only the dates, CI, and links. This
still needs the same verification as any other reused reference, though:
don't carry over a stale CI or region from the cloned change without
re-confirming it — cloning is precisely how a wrong CI propagates, so
re-derive it per [[#Getting the Configuration Item right]] every time.

ServiceNow separately supports a more formal version of this: a reusable
**Change Model** per client (pre-fills a new change's fields from a named
template) and a matching **CTSC model** for tasks. Mentioned as a
known-but-not-yet-fully-exercised capability (one was started for SPIN) —
worth a closer look next time, but not confirmed end-to-end yet, so treat
cloning an existing change as the proven path for now.

When a field is genuinely unknown, mark it `To Confirm` and save as a draft
rather than blocking. **The rule is specifically "never leave it blank"** —
put the placeholder in and escape the field: "put something in so we get out
of the field, don't leave it empty, and confirm it later." The reasoning is
about what saving buys you: "at least it's open now, we've secured the
window, we edit it later." A blank field and a `To Confirm` field look
identical to the form but not to the next person reading it.

### Before submitting for approval

Two checks at the bottom/top of the form, both expected to pass before
requesting approval:

1. `Check Conflict` — checks for a scheduling conflict. Usually at the
   bottom of the page, but its position moves between forms; on one it sat
   at the top.
2. `Reference CMDB` (top-right corner) — confirms the CI reference resolves
   cleanly against the CMDB.

Then `Request Approval`. That is the confirmed order: Check Conflict →
Reference CMDB → Request Approval.

**Not every conflict blocks.** An earlier version of this page said a
conflict makes ServiceNow refuse to save at all. That is wrong, and acting
on it wastes time: **only conflicts rendered in red are blocking**, the rest
are advisory.

`CI Already Scheduled` — another change booked against the same CI in your
window — is the common non-blocking one, and there is now hard proof: an
approved change reached `Implement` carrying **two** of them simultaneously,
both against other changes from the same team hitting the same CI. Read the
conflict list to know who else is in your window, then proceed.

**`Check Conflict` reports missing required fields one at a time.** It does
not list everything wrong at once: one run returns `Configuration Item`
missing, you fill it, the next run returns `On behalf of the vision`
missing, you fill that, and only the third run gets to the actual conflict
check. Budget three or four round-trips rather than expecting one pass.

**Fix the child tasks before trying to save the parent.** The first error on
a fresh change is usually `Change task missing information` — child tasks
created without a scheduled date. Open each task, set the same date/time as
the parent's execution window, save each task individually, then save the
parent.

Once both are clean, request approval. The stage indicator doesn't always
update live — refresh the page to see it move from `New` to the
approval-pending state, and again once someone approves it into `Authorize`.

### When a change won't leave the New state

Both checks passing clean is not a guarantee the change can be approved. One
session lost close to an hour here, against a deadline, with `Check Conflict`
and `Reference CMDB` repeatedly clean and **zero error message anywhere on
the form**. Two separate causes were tangled together, and it's worth
checking them in this order because the second one costs nothing to rule out:

**1. It might not be stuck at all — Standard changes have no `Assess` hop.**
A Standard change has no `Request Approval` button and never moves `New` →
`Assess`. Its approvers are simply listed at the bottom of the form and
approve in place. **A Standard change sitting in `New` with approvers listed
is working correctly, not broken.** Half the lost hour was spent waiting for
a state transition that does not exist for that change type.

**2. The approver list is derived from the CI's owning group, not from the
header `Assignment group`.** This is the real mechanism behind the failure.
The rule people state is "you can only approve if you're in the same group as
the change" — true, but incomplete in a way that matters: editing the header
`Assignment group` **does not re-derive the approver list**. The change had
its header corrected from `Latam 2` to `Latam 3`, was saved, re-checked, and
still wasn't approvable, because every entry on the approver list still read
`Latam 2` — inherited from the CI.

**The observable tell:** open the approver list and compare its group against
the header's `Assignment group`. If they diverge, the CI is the problem, not
the header. In this case the CI belonged to engineering rather than the team
opening the change ("what you're using isn't ours"), and the fix was
replacing the CI per [[#Getting the Configuration Item right]] — not editing
the header again.

**What was ruled out along the way**, so you can skip it: re-running both
checks after every save (always clean, stage never moved); the group backlog
(the group did hold ~6 open changes, but all were legitimately scheduled to
run that same night — open-and-scheduled is not backlog).

## Execution and validation tasks

On a task, assign the **person first, then search for their group** — doing
it the other way round doesn't reliably work. If a name won't autocomplete
in `Assigned to`, use the magnifier next to the field to browse the full
list instead of retyping.

**Mandatory fields on an execution task**, as surfaced by the save errors:
`Datacenter`, `Maintenance Window`, `Environment` (e.g. `PROD`), a scheduled
date/time, and an acceptance/validation-criteria field. The task
descriptions arrive pre-filled as `Execution Task` / `Validation Task` and
are fine to keep as-is.

**You are really picking the group, not the person.** Once a task is
scheduled, anyone inside the assigned group can reassign it to themselves —
but **the group itself can't be changed** at that point. So naming a person
is mostly a way of selecting their group and putting a plausible owner on
it; the actual human is expected to be swapped at execution time by whoever
is on shift. This is why placeholder assignees are normal rather than
sloppy, and why getting the *group* right is the part that matters.

A Normal Change spawns at least two child tasks that cannot share an owner:

- **Execution task** — same maintenance window as the parent change,
  assignment group is whoever runs the change.
- **Validation task** — same date, but a different owner than the executor.
  **Two readings of how strict this is, both observed.** In practice, at
  assignment time, the instruction is person-level and immediate: asked
  whether validation could take the same assignee as execution, the answer
  was a flat "no, it has to be someone else" (execution to one engineer,
  validation to another). But the *written* policy people quote is stricter
  — execution and validation must sit with **different groups**, not merely
  different names — hedged with "I don't know if they'll actually enforce
  it". Different people is the floor; different groups is the safer read
  when you have the option.

  **The exports settle the group half: different groups is a consequence,
  not a requirement.** Execution and validation sit in the same group on
  approved changes routinely; they land in different groups when the work
  genuinely spans teams (Gateway Prod Support executes, API Development
  validates).

  **The people half is weaker in practice than anyone states it.** A
  nine-task production change had the *same person* on both halves of three
  separate execution/validation pairs — the deploy pair, the NGINX/F5 pair
  and the DB pair — and was approved. So while everyone repeats "it has to
  be someone else", the enforced reality is closer to: the change as a whole
  shows a split, and per-subsystem pairs often don't. Aim for two names
  because that is the stated rule and it is what the control is *for*; know
  that nothing stops you, and that plenty of approved changes don't.

This makes group membership something to check rather than assume, and it
is genuinely non-obvious: one engineer discovered mid-call that he belonged
to a 24/7 support group he had never been told about, while a colleague
belonged to only one group. A single-group person can make the pair
unsplittable, so look at the candidate's actual group list before assigning
the second task.

In practice ServiceNow never checks that the two names match who really ran
or validated the work — the enforced part is the split on paper, not the
truth of it. That gap is used deliberately: the name on an execution task is
sometimes chosen to avoid a political argument about which team owns
production execution, rather than to record who touched the keyboard.

If the change also needs another team to act (typically Infra), it grows to
**four tasks**: `Execution Task` + `Validation Task` for engineering's own
part, plus `Execution Infra` + a technical validation task for the other
team. A technical validation task (referred to as the CCT task) is required
on every Normal Change regardless of risk level — this is a CAB requirement,
not optional even when everything else is low risk.

**Multi-region CCT.** CCT support isn't one team for all of LATAM, and the
group names make it explicit: **`LTAM.1.Command Center CTC-ARG`** covers
Panama, Colombia, Argentina and Mexico, while **`LTAM.1.Command Center
CTC-BRA`** is Brazil's own. Pick the one matching the affected region — a
Brazil change routed to CTC-ARG lands in the wrong operations room. The trigger for a second CCT task is narrower than
"touches two regions" — it is **an application that impacts more than one
client, in different regions**. When that happens you need one CCT task per
CCT actually covering those clients.

**Which validations to add, as a decision table.** Every change has at least
one Execution and one Validation task; beyond that you add a validation per
thing the change touches:

| When the change… | Add a validation for… |
|---|---|
| **Always. No exceptions.** | **Operations Team** |
| Hits PROD and involves a client | Command Center |
| Involves APIs | Engineering |
| Involves other systems (F5, Apigee, …) | One per system |

**The Operations Team validation is unconditional** — not optional, not
skippable on a small change, not waived because something looks harmless.
That person watches the logs live while your change runs, catching anything
that falls over or quietly changes. They are the human smoke detector, and
they are the reason a change that "worked" but broke something adjacent
gets caught at the time rather than in a postmortem.

This also resolves the **BAU** acronym the recordings couldn't expand: BAU
*is* the Operations/support team, which is why a card asking for "validation
BAU, validation CCT, validation engineering" is asking for exactly three
rows of the table above.

**More than four tasks.** The four-task ceiling above holds for a
typical engineering+Infra change, but it isn't a hard limit. A change that
crosses several infra subsystems (in one case: HSM, F5/load balancer,
Command Center, and Operations, on a Restricted/Emergency certificate
change) ended up with a dedicated Execution/Validation pair or standalone
Validation task **per subsystem team** — well beyond four. Whether this
expanded pattern also applies to an ordinary Normal change (vs. only
Restricted/Emergency) is unconfirmed, see [[#Open questions]] #4; treat
"four" as the common case, not a ceiling. When creating several of these, rename each task's default title
to name its team (e.g. "Command Center Validation Task", "Operations
Validation Task") instead of leaving ServiceNow's generic default — it
keeps a multi-task change legible later.

**Disaster recovery (DR).** When the change also touches a DR instance,
don't duplicate all four tasks. Only the execution side needs a second task,
one per server actually being touched (primary and DR each get their own
`Execution` task, pointed at their own CI/datacenter). Validation stays
governed by the "at least one CCT validation" rule above, it does not double
automatically just because DR is involved.

The maintenance window needs real lead time: scheduling a Normal Change for
"tomorrow" is unlikely to validate, expect at least a couple of business days.
Change Execution Plan, Back-out Plan, Validation Plan, and impacted
functionality are copied from that specific change's own runbook (often
shared privately per change), never from a template that belonged to a
different change, since these steps change with what is actually being done.

## Standard changes in practice

Search/non-prod changes go through as `Standard`, skipping CAB. In practice
this is fast: a requester (e.g. Pedro) asks for something small — create a
policy, spin up a proxy, in a Search environment — you fill the change in
a couple of minutes, a peer (commonly whoever's around, e.g. Osvaldo)
approves it, and it's already in effect.

For requester-driven work, a maintenance window of about 24 hours is typical
(open now, they act by end of next day), giving them a full business day to
actually do the thing without needing to babysit the exact timing. Worked
example: building it at 17:30, set the window 17:45 today → 16:45 tomorrow.

**The window's start time must still be in the future when the approval
happens.** If the start time has already passed, ServiceNow refuses the
approval and just throws errors. One session pushed the start forward four
times (18:00 → 18:10 → 18:20 → 18:30 → 19:00) as the clock caught up with
them while they were still building, with the 24h end sliding along with it
each time. If a change is taking longer to assemble than expected, push the
start forward *before* attempting approval rather than debugging the error.

**"Search" is not an `Environment` value.** Changes described verbally as
"for Search" are recorded as `Cert` or `Lower`, which is what makes them
Standard. One senior engineer's grumble that "in my day Search was Normal"
is worth keeping in mind as a sign the boundary has moved, but the field
itself is unambiguous: the environment you pick is what decides the type.

**The change must be `Implement`, not just approved, when it's actually
used.** Some downstream flows (e.g. generating a credential) ask for the
associated change number at the moment of use, and check its live status —
if the change is only `Authorize`d and hasn't rolled into `Implement` yet,
the tool refuses and the requester's action fails. Getting the change
approved isn't the finish line if the requester needs it *right now*; make
sure it has actually reached `Implement` first.

## Restricted changes

A Restricted change is owned by the **Application (APM) Owner**, not by
engineering: both `Opened by` and `Requested by` are supposed to be the app
owner, not whoever actually builds the ticket.

**Why this exists**, which makes the rest of the dance make sense: the app
owner's name on the change **substitutes for VP approval**. With the app
owner as owner, no separate VP sign-off is needed — that is the whole point
of the ownership rule, not bureaucracy for its own sake.

**The ordering matters and is easy to get backwards.** The app owner has to
**create** the record first — that is what `Opened by` captures, and it
can't be retrofitted — then hand it to engineering, who fill everything in,
then hand it back for the owner to request approval. Not: engineering builds
it and transfers it at the end.

**Handing it over is a one-way permission boundary, not a courtesy.** The
moment the change is saved under the app owner's name, engineering loses
edit rights entirely ("it's gone to Pedro's name, I can't edit anything any
more"). Engineering also never clicks `Request Approval` on a Restricted
change at all — the app owner does that from his own account. So every
subsequent correction costs a full round-trip through the owner.

**The strict version is already being relaxed.** This policy arrived
recently — roughly mid-2026, with the new CAB policy — and the team's
objection is structural, since the app owner is typically a manager and
having managers open every change is impractical. Change Management has
waived the create-first half live, on request: "it was like that initially,
but it's going badly — you create it and just put his name on it." So if the
create-first ordering is blocking you, it is worth asking rather than
assuming it's immovable.

Restricted changes **skip CAB presentation** and use an **email-approval
flow** instead: the requester (the app owner) sends an approval-request
email to a specific list of named stakeholders, and when they reply "OK",
those replies get attached to the change as evidence — this stands in for
CAB sign-off. Who exactly needs to reply depends on the app/client and
region; one case needed three named technical approvers together, another
(a LATAM-scoped Restricted change) needed two (Javier Pinheiro and Gervasio
Russo). Either way, a client-side contact can be cc'd for awareness only,
without being required to approve. This looks like the concrete mechanism
behind the AFINZ-style business approval described in
[[#Clients and their special handling]] — worth treating the two as
the same underlying flow until proven otherwise.

**Put the required approvers in the email subject.** That's how the set
being waited on is tracked — "we need those four approvals, from the four
people I tagged in the email title". The replies then get attached to the
change and stand in for the CAB approval that didn't happen.

**The named-approver list has backups.** When Javier Pinheiro was on
vacation, a delegate (Luis Alejandro) approved in his place, and that
substitution was accepted once Javier confirmed it was fine. Note the order:
the delegate's approval was taken first and verified with the principal
**after the fact**, not cleared in advance. Don't treat a named-approver
list as fixed; if the usual person is out, ask who their delegate is rather
than stalling.

**Individual approvers can carry their own standing requirements.** One
routinely demands successful test evidence attached to any Restricted
change before he'll approve; another reviews the entire change line by line
and asks questions. Ask who is on the list *before* writing the change, not
after, because it can change what you need to attach.

**A business stakeholder can reroute a Restricted change straight into a
same-day CAB slot**, bypassing the email-approval chain entirely — in one
case a stakeholder (working with whoever runs that day's CAB) decided on
short notice to present a change live at CAB instead of collecting the
named-approver emails, "because the approval comes directly from CAB" in
that case. Useful escape hatch symmetrical to the Emergency-change ad hoc
exception below: if the email chain is stalling, ask whether the change can
just be presented at CAB instead.

**The cutoff is 18:00 in the local time of the region the change affects** —
not BRT, and not a single fixed hour. The team works to **17:00 as a
self-imposed safety margin** rather than riding the real deadline. Three
independent confirmations, all reasoning the same way: a Mexico change had
until 21:00 BRT ("18:00 local, so since it was Mexico I had until 9pm"), a
Panama change was still viable at 18:20 BRT ("it's 16:20 in Panama now, so
I can still do a restricted"), and the change manager's first question
before ruling was which region was affected.

This cuts both ways. A non-Brazil region usually *buys* you hours rather
than costing them — Panama runs 2 hours behind Brazil, Mexico 3 — so a
change that looks past the deadline in BRT may be comfortably inside it.

**Getting every approval reply is not the finish line.** Change Management
still has to act on the collected replies, and they stop working before the
late regional cutoffs do. The war story worth internalising: all approvals
chased down and secured by 21:00, and then the change simply wasn't
processed because the Change Management side had already gone home for the
day. Approval turnaround itself is bimodal — three of four replies landed
in under five minutes on one change, while another took a full day of
chasing — so treat the approvers and the processor as two separate risks.
Against that, ~30 minutes between final approval and execution has worked
for a genuinely critical change, so late is not automatically lost.

## Emergency changes

An Emergency change is for a Production issue urgent enough that even a
Restricted change's same-day window doesn't fit. Two things distinguish it
from Restricted:

- **Order is reversed.** A Restricted change is built first, then the
  approval email goes out. An Emergency change sends the approval email
  **before** the change record even exists.
- **Who opens it is different again.** An Emergency change is opened by the
  **Incident Manager**, not engineering and not the app owner.

**An Emergency change always needs a backing Incident** — stated flatly by
Change Management: if you genuinely must execute tonight it has to be
Emergency, and for Emergency there is always an incident. Whether that
incident must be P1 specifically or P1/P2 both is still unconfirmed, see
[[#Open questions]] #2.

If a suitable incident already exists but its priority is too low, the
practice is to **raise the existing incident's priority rather than open a
new one**. A pre-existing, already-escalated, aged incident (one case was
15+ days old with the client escalating) can carry the Emergency change by
itself.

**The gate question a change manager actually asks** before granting
anything: *"if this doesn't execute, is there imminent impact?"* A
contractual or financial penalty counts — the case that cleared this bar was
a client mid-certification with an external certifier who would be
financially penalised on a missed date. "It's urgent for us" does not clear
it; a named consequence does.

**A change manager can waive the Restricted cutoff — which is usually the
better escape hatch than going Emergency.** In the expiring-certificate
case, the team had already concluded they needed an Emergency change (and
therefore an Incident and an Incident Manager). The change manager instead
granted an exception that let them **stay Restricted past the cutoff and
skip the incident entirely**: "by exception, I'll make this exception
because of a certificate — open it now, get these named approvers, send it
to me and I'll approve. But next time, open the change beforehand." The
change still had to be filled in properly, with the justification recorded
on it. So when the blocker is *timing* rather than *severity*, ask for a
cutoff waiver before reaching for Emergency.

## Certificate / HSM changes

A certificate renewal on hardware-backed (HSM) infrastructure has its own
sharp edges, distinct from a regular code/config change:

- **ServiceNow approval is not the only approval.** An HSM-hardware
  certificate can require a *separate* sign-off inside **Venafi**
  (certificate management tooling) before it applies to hardware, entirely
  independent of the ServiceNow CAB/Restricted/Emergency chain.
- **The certificate often lives under a different APM than your
  application — this is the root cause of the Venafi access problem.** In
  the observed case the Thales/HSM certificate sat under APM 5327 while the
  team owned a different APM: "it was under another PM, but it impacts us."
  The consequence was flat: nobody on the team had Venafi access for *that*
  APM, and someone from another team had to share their screen just to read
  the expiry date. Find out which APM owns the certificate, and who has
  Venafi access to it, well before an urgent renewal.
- **Both sides of the pair must be updated.** Updating only your side of an
  HSM certificate is worse than doing nothing: "it needs the updated
  certificate on our side and it needs updating on the HSM side. On the HSM
  side it isn't updated. If we touch it, it'll actually be worse, because
  then it stops." Confirm the counterpart side is ready before scheduling.
- **Schedule before the nightly batch, not after.** The LAM/BR batch starts
  at **00:00 BRT**, and one renewal was deliberately set for 23:00 to land
  ahead of it. The reasoning is about recovery room, not the batch itself:
  replace the certificate, and if something goes wrong you put the old one
  back and still have until morning to sort it out.
- **What execution and validation actually consist of**, per the SME:
  update the certificate, change the environment variables inside the
  application, restart the application (back up in roughly 200ms) — no
  region restart needed. Validation is making a request to an API that uses
  the HSM service. The ~5-minute impact figure quoted on the ticket is the
  HSM router's certificate-load time, not the app restart.
- **Don't trust a certificate handed over by the cert-management team
  blindly.** A prior incident involved a certificate provided directly by
  that team failing to work; the fix was to connect straight to the
  Production server and pull/generate the certificate data manually.
  Practical mitigation: back up the current certificate (same filename
  convention) before applying the new one, so a failed swap can be rolled
  back quickly.
- **A single HSM can back both Primary and DR via multiple IPs.** The
  observed setup: an **HSM-10K** from **Thales**, reached over two IP
  addresses that are *the same for Primary and for DR*, with load balancing
  done by Thales rather than by the application, and the same certificate
  used by both. Nobody in the room was sure a backup HSM exists at all
  ("generally we should have a backup HSM, right?" / "I don't think so, we
  have one"). On sequencing, the best available answer was an explicit
  guess — "my understanding is that we can do one IP address at once, I
  don't know how it operates" — so confirm with the HSM/Infra team rather
  than inheriting that assumption, see [[#Open questions]] #5.

## Patterns across real changes

Three approved production changes, read side by side. Where they agree is
convention; where they differ is choice.

| | **A** JWE keys | **B** incident fix | **C** cert prod | **D** cert DR | **E** cert AFNZ | **F** cert lower | **G** Sem Parar |
|---|---|---|---|---|---|---|---|
| Number | …3069 | …3079 | …2986 | …2777 | …5203 | …2447 | …9974 |
| Category / Sub | App/Deploy | App/Deploy | Sec/Modify | Sec/Modify | Sec/Modify | **App/Add** | App/Deploy |
| Type | Normal | Normal | Expedited | Expedited | Normal | **Standard** | Normal |
| Project Scope | Defect/Inc | Defect/Inc | BAU/House | BAU/House | BAU/House | BAU/House | **Feature Updates** |
| Environment | Production | Production | Production | DR | Prod/DR | **Lower** | Production |
| Code change | No | No | No | No | No | No | **Yes** |
| PITE | Yes | Yes | No | No | Yes | Yes | Yes |
| Risk / Impact | Mod / Low | Mod / Low | VHigh / High | VHigh / High | Mod / Low | **Low** / Low | **High** / Low |
| HRRB flag | false | false | true | false | false | false | **true** |
| Sage score | 67 | 61 | 67 | 55 | 65 | **blank** | 69 |
| Tasks | 5 | 3 | 7 | 7 | 4 | **2** | **9** |
| Approvals | 29 | 29 | 29 | 29 | 29 | **8** | **37** |
| Approval tiers | 2 | 2 | 2 | 2 | 2 | **1** | **4** |

The two newest rows are the informative ones: **F** is the only Standard in
the sample and shows how much lighter that path is (2 tasks, 8 approvers,
one tier, no seven questions), while **G** is the only code change and shows
how much heavier Cyber makes it (37 approvers across four tiers).

**What never varies**, and is therefore the actual convention:

- `Assignment Group: LTAM.3.API Development-LAT` and
  `Change management group: LTAM.3.Change Management` on all three.
- **29 approval rows, always.** One person in the dev group approves, ~25
  rows flip to `No Longer Required`, and the *same three* Change Management
  names sit at `Requested`. The tier-1 approver differs each time (whoever
  was around); tier 2 is a fixed roster.
- `Backout plan duration` is `Less than 30 Minutes` on four of five; the
  fifth, covering both Production and DR, uses `Between 30 and 60 Minutes`.
  The buckets are coarse and the default is the short one.
- Question 7 answered identically every time: "Yes, command center was
  informed and a task created" — with a matching Command Center task
  assigned to `LTAM.1.Command Center CTC-ARG`. This is boilerplate in the
  best sense: it is always true because they always create the task.
- `Conflict List: None` on all three.
- `Maintenance Overridden: true` (Moogsoft alert suppression) on all three.
- Windows start at midnight or 06:00 — outside business hours, always.

**Stock phrases worth reusing.** The two deploy changes share wording
verbatim, which tells you these are house phrases rather than fresh prose:

- Backout plan for a first-time deployment: **"First deploy in production"**
  — i.e. there is nothing to roll back to, stated in four words rather than
  padded into a fake procedure.
- Validation plan for a deploy: **"Validate the application is deployed and
  running."**
- Client post-implementation validation: **"No - Internal Validation Only"**.

**What changes with the *kind* of work:**

- A **deploy** gets `Application / Deploy`, a one-line execution plan, and a
  three-line validation. A **certificate renewal** gets `Security / Modify`,
  a seven-step execution plan naming the Venafi console and the exact
  `systemctl restart cam-router`, and a backout plan with troubleshooting
  branches. Effort in the plan fields scales with how much the executor
  needs told, not with the change's importance.
- The cert change's CI is a **server hostname** (`stlp2camapp0003.1dc.com`,
  class `Certificates`, owned by `Sec - Certificate Management (SSL)`),
  where the deploys point at **application services**
  (`LATAM TOKENIZATION & DIGITAL WALLET SOLUTION - PARQUE PATRICIOS IN
  PRODUCTION`). Pick the CI that matches what you're touching, not a
  standard shape.

**Task naming and numbering.** With three tasks, plain names are fine
(`Execution Task`, `Validation Task`, `Communication with Command Center`).
With seven, they are **numbered in execution order** and name their target:

```
1 - Certificate renewal
2 - Deploy | API | Certificate | stlp2camapp0003 & stlp2camapp0004
3 - Restart the application in servers
4 - Validate the successful certificate renewal and update (CamRouter)
5 - Validate the successful certificate renewal and update (Falcon)
6 - Validate the successful certificate renewal and update (VisionPLUS)
7 - Communication with Command Center (Argentina)
```

That change also spread tasks across **five different groups** — API
Development, INFRASUPPORT, Falcon UAT, FirstVision Engineering, Command
Center — one validation per consuming system. This is the "more than four
tasks" pattern in the wild, on an ordinary (non-Restricted) change.

**A second, cleaner naming pattern: paired by subsystem.** A nine-task
production deploy used `Execution <subsystem>` / `Validation <subsystem>`
side by side instead of numbering:

```
Execution - Deploy payment-batch-processor   LTAM.3.UUI VisionPlus-LAT
Validate Deploy payment-batch-processor      LTAM.3.UUI VisionPlus-LAT
Execution NGINX/F5                           LTAM.2.INFRASUPPORT-LAT
Validation NGINX/F5                          LTAM.2.INFRASUPPORT-LAT
Execution DB Update                          TSRV.2.DBA-Oracle Operations
Validation DB Update                         TSRV.2.DBA-Oracle Operations
Validation CCT Brazil                        LTAM.1.Command Center CTC-BRA
Create credentials                           LTAM.2.API Engineering-LAT
```

Each pair sits with the team that owns that subsystem, and the pairing makes
it obvious at a glance that nothing is being executed without a matching
check. Prefer this over numbering when the work splits by system rather than
by sequence; use numbers when the order genuinely matters.

**Groups seen owning tasks**, useful when you're guessing who does what:
`LTAM.2.INFRASUPPORT-LAT` (NGINX, F5, servers), `TSRV.2.DBA-Oracle
Operations` (database), `LTAM.3.UUI VisionPlus-LAT` (the VisionPlus UI),
`LTAM.3.Gateway Prod Support-LAT` (gateway/credhub),
`LTAM.1.Falcon UAT-LAT` and `LTAM.2.FirstVision Engineering-LAT`
(per-application validation), plus the two Command Centers.

**A third task type exists: `Communication`.** The Command Center task is
typed `Validation` on some changes and `Communication` on others, so the
typing is inconsistent in practice — don't read meaning into it.

### The certificate-renewal template

Two changes in the sample are **the same change twice**, differing only in
target: same author, identical description boilerplate, identical seven-step
Venafi execution plan, identical backout and validation text, and the same
seven tasks across the same five groups with the same people on them. Only
the servers, the environment, and the region answer change.

That is a **Change Model in use** — the capability the recordings described
as "started for SPIN but never finished" clearly exists and is being used
for certificate work. If you are opening a cert renewal, **find one of these
and clone it** rather than writing from scratch; the entire body is reusable
and only these vary:

| Varies | Example values |
|---|---|
| Servers in the description | `stlp2camapp0003/0004` vs `rklp2camapp0005/0006` |
| `Environment` | `Production` vs `DR` |
| Q1 region answer | "MEXICO - used by TOTALPLAY and BINEO" vs "Spin only" |
| Short description target | the hostname in quotes |

The stock body worth knowing exists, so you recognise it rather than
reinventing it: business reason is "Routine Maintenance - Renew and update
SSL certificates nearing expiration…"; Q2 is "Not required. This is a CAM
Router certificate update."; the execution plan is Venafi console → locate
cert (**"Must be a member of the certificate APM group"**) → download →
**BACKUP old certificate** → drop in `/idc/cert/` → `systemctl restart
cam-router` → assist validation team.

Note that the backout plan on this template carries **two embedded template
questions** answered inline — "At what point during the implementation will
you take the decision to backout your change?" and "Does your change window
include time to backout if you need to?" — so the field expects a small
narrative, not just steps.

### Two defects the exports reveal — check yours for both

Not every approved change is a well-formed one. Two errors survived into
records that were already sitting at `Authorize`, which tells you nothing in
the tooling catches them:

- **Execution and validation assigned to the same person.** Every other
  change in the sample splits them across two names; one puts the same
  person on both. The "different people" rule is a process expectation, not
  a validation — ServiceNow will happily let one person sign off their own
  work, and it will reach CAB looking fine.
- **The Command Center task pointed at the wrong group.** The convention is
  unmistakable across the sample — `LTAM.1.Command Center CTC-ARG`, with a
  named person from that team. One change instead left it on the author's
  own `LTAM.3.API Development-LAT` **with no assignee at all**. That task
  is the thing question 7 promises exists; if it sits in the wrong queue,
  nobody in the Command Center ever sees it, and the change is asserting
  something untrue.

- **A template placeholder left in a live field.** One approved change's
  `Escalation contact` read, verbatim: *"Who do we contact if there is an
  issue with the implementation? Name(1) & Phone# & Name(2) & Phone#"* —
  the prompt text, never replaced with actual names. That change reached
  `Implement`. The field whose entire purpose is "who do we call at 3am"
  contained a question instead of an answer, and nothing caught it.

Three defects, all in approved changes, none producing an error. The common
thread is that **ServiceNow validates presence, not sense** — a field with
the wrong thing in it is as "filled" as a field with the right thing. Read
your own change back once before requesting approval; it takes a minute and
it is the only check that exists.

**Blast radius is auto-derived and varies wildly.** Change A carried 2 CIs,
19 impacted services and ~30 client rows spanning banks across Panama, El
Salvador, Guatemala, Nicaragua, Mexico, Curaçao and more. Changes B and C
carried 1 CI and 1 impacted service. Same team, same week. Those lists come
from the CI, not from the author — read them, because they are what a
reviewer sees, but don't be alarmed that a small change touches a long list.

**Two `Reference CMDB` outcomes are both normal**: "successfully completed"
and "successfully completed **but found no data**". The latter appeared on
two of three approved changes, so it is not a blocker.

`svc_SageAI` appears as the last updater on one change — an automated
account touches these records after you do.

---

Below, change **A** in more depth, as a model for register.
`CHG002943069` — replacing JWE encryption keys on a MasterCard tokenization
proxy for one client, 2026-08-28, 00:00–01:00.

**How it classified.** `Type: Normal`, `Category: Application`,
`Sub-Category: Deploy`, `Environment: Production`,
`Project Scope: Defect / Incident Related`, `Impact: 3 - Low`,
`Risk: Moderate` (Sage AI risk score 67), `Is this a code change?: No`,
`Will this change require an update to DR?: No`,
`HighRisk Review Board: false`. A routine-but-real production change: low
impact, moderate risk, no code, no DR.

**How the text was written.** Terse and load-bearing — nothing decorative:

- Short description: `[PR25004659] Banco Agricola - Tokenization MasterCard`
- Description opens with the problem in one sentence — "We need to replace
  the jwe keys to fix a decryption problem in the communication between
  Agricola and Fiserv" — then the APM tag and the deployable's name
  (`[APM0007084] Version: - app-tsp-outbound-mastercard`), then the seven
  questions.
- Business reason carries three short lines: what it is, why now ("this is a
  new proxy that is not in production... the client is expecting production
  tests"), and a secondary benefit ("observability features").
- Execution plan is five imperative bullets, each one action:
  update the keys → restart blue proxy → restart green proxy → validate
  routing to green → validate expected behaviour.
- Backout plan is four words: "First deploy in production" — i.e. *there is
  nothing to roll back to*, stated plainly rather than padded. Duration:
  `Less than 30 Minutes`.
- Validation plan is one line: "Validate the application is deployed and
  running."

The register worth copying: **short declarative sentences, no hedging, and
an honest answer where the honest answer is unflattering.** "None, first
time deploying the code in production" is a better answer than inventing a
rollback that doesn't exist.

**The task structure — five tasks, and the pattern is legible from the
names alone:**

| Task | Group | Type |
|---|---|---|
| Deploy proxy in ApigeeX | API Development-LAT | Execution (**Cancelled**) |
| Communication with Command Center | `LTAM.1.Command Center CTC-ARG` | Validation |
| Execution - **Omaha** - Update credhub | Gateway Prod Support-LAT | Execution |
| Execution - **Chandler** - Update credhub | Gateway Prod Support-LAT | Execution |
| Validation Task | API Development-LAT | Validation |

Three things to take from this:

- **Task titles name the datacenter**: `Execution - <Datacenter> - <action>`.
  Omaha and Chandler each get their own execution task because each is a
  separate CI being touched — even though the DR question is `No`, since
  both are *Production* datacenters here rather than a primary/DR pair.
- **Execution and validation genuinely sit in different groups** — Gateway
  Prod Support executes, API Development validates. The stricter reading of
  the split, in the wild.
- **A cancelled task is left on the record** rather than deleted.

**The Command Center task is the answer to question 7 made real.** The
change says "yes, command center was informed and a task created"; the task
exists, assigned to a named person in the Argentina Command Center group.

**Blast radius vs. declared impact.** The change declares "only mastercard
outbound application will be impacted" and names one region, yet the record
carries **2 affected CIs, 19 impacted services, and ~30 client M2M rows**
spanning banks across Panama, El Salvador, Guatemala, Nicaragua, Mexico,
Curaçao, Barbados, Dominica and more. Those lists are auto-derived from the
CI, not typed by the author. Don't panic at them — but do read them, because
they are the system telling you what a "small" change actually sits on top
of, and they are what a CAB reviewer will see.

**Alert suppression is part of the record**: `Maintenance Overridden: true`
on the Moogsoft section, alongside an `On-Call` tab holding the Teams
meeting link. Silencing alerts for the window is a real step, not an
afterthought.

## Who's who

**Roles as of early September 2026, and this table will rot faster than the
rest of the page.** It already has one confirmed replacement in it. Treat it
as "who to ask first", not as an authority — and when someone turns out to
have moved, fix the row rather than working around it.

The single most useful entry: **Nidia knows the current approver roster.**
When the question is "who has to approve this", ask her rather than
inferring it from an old change.

**Change Management and CAB**

| Who | What they're for |
|---|---|
| **Sebastião** (also "Sebastian") | Runs and schedules the CAB slot, reviews changes before CAB, sits in the session. The person who can set the `High Risk Review Board` flag — a routine, cheap ask. |
| **Josué** | Change Management. Grants cutoff waivers and exceptions, names the required approver set on a Restricted change, then approves it once the replies are in. Can be slow to respond. |
| **Romina** | Josué's counterpart — the fallback when he's unreachable and the clock is running. |
| **Olga** | Business stakeholder who can reroute a Restricted change into a same-day CAB slot, working with Sebastião. |

**Restricted-change approvers (LATAM)**

| Who | What they're for |
|---|---|
| **Nidia** | Knows who currently approves what. Ask her first. |
| **Javier Pinheiro** | Director, named approver. **Luis Alejandro** covers as his delegate. |
| **Gervasio Russo** | Named approver. Standing requirement: successful test evidence attached before he signs. |
| **Nelson Salazar** | Approves the SPIN email flow. Took over from **Hugo Martinez**, who moved departments — Hugo reviewed changes line by line and is no longer in the loop. |

**Infra**

| Who | What they're for |
|---|---|
| **Raul Bernardo Santos** | Day shift. Books the execution window by blocking the calendar. |
| **Léo** (Leonardo Migliorini, spelling unconfirmed) | Raul's backup. |
| **Marcelo Hideki Nakashima** | Night shift. Also the standing placeholder name on Infra tasks. |
| **Nelson Croc** | Infra manager. Opening his record's "organization" view enumerates the whole infra org — handy when you don't know who to chase. |

**Engineering and adjacent**

| Who | What they're for |
|---|---|
| **Dani** | The source of most of this page. Currently handing production-execution ownership over to engineering, which is why she pushes execution tasks onto other names. |
| **Xeno** (Xenon / Zeno / "Shannon" in transcripts) | Requests changes with complete cards including the APM number. Built the SPIN Change Model — ask him to demo it. |
| **Pedro** | App owner for Restricted changes; tends to request with a single line and short notice. |
| **Aranha** (Jorge Luiz Aranha) | Executes and validates. |
| **Osvaldo** | Peer approver for Standard changes. |
| **Motoshima** | Has already written some change documentation — worth reading before writing more. |
| **Danilo Dalvio** | Certificates team; renews certificates in Venafi. |

## Gotchas

- **Check the next morning whether the change actually got approved —
  don't assume it's handled.** A change built one day for next-day CAB can
  be pre-approved automatically, get accepted into CAB, or get rejected,
  and nothing forces this onto your radar; it's easy to simply forget a
  change is awaiting an outcome once you've moved on to other work. In one
  case the team only remembered at midday that a change submitted the day
  before needed to be defended at that morning's 10:00 CAB, and got lucky
  that it had been auto pre-approved — if it hadn't been, nobody would have
  been present to defend it and the change would have failed outright.
  **Practice:** the morning after submitting any change, take its number and
  search your email for it — ServiceNow sends an automatic email either way
  (pre-approved, entered CAB, or rejected), and the two people whose names
  show up sending/handling these are usually **Sebastião (Sebastian) or
  Josué**. Do this before assuming a change is on track, especially when
  later deliverables depend on it actually executing.
- **A no-access link fails silently.** A ServiceNow link to a board you lack
  permission for does not error, it quietly redirects to the main dashboard.
  If a shared link "looks wrong", suspect a permissions gap before assuming
  the link itself is broken. The board this comes up for most is named
  **"LatamChangesBacklog"** — reconfirmed independently for two different
  people lacking access to it in a later session, so request access to it
  by that exact name rather than assuming it'll just show up once you're on
  the right team.
- **The group counter and a personal counter are different clocks.** The
  assignment-group backlog badge can hit zero the moment a task closes, while
  an unrelated per-user indicator takes longer to refresh. A stale personal
  counter after closing is not a sign the close failed.
- **Do not infer the CI from a similar change.** The most repeated mistake
  in this whole process, and it fails silently in several different ways —
  the full treatment is in
  [[#Getting the Configuration Item right]].
- **Set your ServiceNow timezone to São Paulo/BRT.** Preferences are
  per-user, and windows render in *your account's* timezone, not a fixed
  BRT assumption. The symptom is subtle: tasks entered as 23:00–02:00
  displayed as midnight–03:00 for one person and correctly for everyone
  else. **The fix, in order:** duplicate the browser tab, strip the URL back
  to the ServiceNow home page (delete everything after the slash), change
  the timezone preference there to São Paulo, then go back and **refresh the
  change page** — the change page will not pick up the new setting on its
  own.
- **"Cert" is ambiguous and has already cost an hour.** It means both
  *certificate* and the *CERT environment*, and the two lead to completely
  different CIs. A change described as being "for cert" was read as the CERT
  environment, a Certification CI was selected, and it was actually a lower
  environment. Ask which one is meant before touching the CI.
- **CI names are inconsistent about Brasil vs Brazil.** Search both
  spellings — one session was told to search "Brazil with a Z" and the CI
  they ultimately needed was spelled `Brasil` with an S.
- **The Restricted copy dance leaves you with two near-identical changes,
  and you will edit the wrong one.** Cloning a change and then handing a
  copy to the app owner produces two records differing only by owner. One
  session filled the entire `Expedite` section on the discarded copy and
  had to redo it on the real record ("this isn't the one I was working on,
  this is mine, not Pedro's"). After any handover, re-open the change by
  its number rather than trusting the tab you already have open.
- **Budget for referral chains when you need another team's specialist.**
  Reaching someone who actually knew the HSM setup took four hops, because
  the person the team knew had left: he pointed to a second, who pointed to
  a third, who pointed to a fourth, who finally called back. Start that
  chain early on anything urgent that depends on outside expertise.
- **Missing the 17:00 cutoff isn't fully recoverable same-day.** Once past
  cutoff, the paths are waiting for the next CAB slot, using the in-form
  `Expedite` section (see above) if the mismatch is about landing on the
  wrong weekday, or escalating to a senior approver directly for a genuine
  emergency — none of these have a guaranteed turnaround.
- **Realize the Risk/weekday mismatch as early as possible.** Missing that a
  change would classify High Risk (Tuesday/Thursday CAB only) after already
  committing to a Monday/Wednesday/Friday execution date cascades into a
  scramble — check the computed `Risk` field right after the header fields
  are filled in, not right before submission.

## Open questions

Unresolved items spotted across sessions, phrased as direct questions.
Answer inline (replace `_(unanswered)_`) as they get resolved — by Rodrigo
from memory, or from a future recording — then fold the answer into the
relevant section above and delete the entry here rather than leaving it
duplicated.

1. ~~**What are the seven mandatory CAB questions, exactly?**~~ **ANSWERED**
   from a real change export — all seven are now transcribed verbatim in
   [[#The required questions]], and both suspected impostors are confirmed
   to be separate fields. The seventh was the Command Center / alerts
   question.

2. **Does an Emergency change's backing Incident have to be P1 specifically,
   or does P2 also qualify?** Narrowed but not closed: an Emergency change
   *always* needs an incident (confirmed by Change Management), and the
   practice for an aged incident whose priority is too low is to raise it
   rather than open a new one. The P1-vs-P1/P2 threshold itself is still
   only someone's hedged recollection.
   Answer: _(unanswered)_

3. ~~**What is the actual cutoff time and timezone for Restricted-change
   email approvals?**~~ **ANSWERED** — 18:00 in the local time of the region
   the change affects, with the team working to 17:00 as a safety margin.
   Folded into [[#Restricted changes]]; delete this entry once you've
   sanity-checked that section.

4. ~~**Does the "more than four tasks" pattern apply to ordinary changes or
   only Restricted/Emergency?**~~ **EFFECTIVELY ANSWERED** — it applies to
   any change type. An ordinary **Standard** change carried three validation
   tasks (BAU, CCT, Engineering) plus execution, purely because the card
   asked for them. Task count is driven by **what the card requests and how
   many teams must validate**, not by the change's type. Folded into
   [[#Execution and validation tasks]]. The CCT/Command Center
   sub-question is also **answered**: they are the same team. The group on
   a real change task reads `LTAM.1.Command Center CTC-ARG` — CTC is the
   Command Center, based in Argentina, which is why the doc's "Argentina
   CCT" and "Command Center" were never two things.

5. **On a shared-HSM Primary+DR setup with two IPs, does a certificate
   update need to hit both IPs together, or can they be sequenced one at a
   time?**
   Answer: _(unanswered)_

6. **What does "MR jobs approval" actually refer to?** Now almost certainly
   **not a ServiceNow concept**: it reads as a per-repository git
   merge-request approval permission, held as a per-application role
   ("I have a bigger role... here I have a role that lets me approve, but
   not on that one"). The usual approvers named were Paceto and Yoshio,
   probably Pedro, unsure about Xeno. It has shown up as the blocker on a
   *release* while being entirely separate from the change record. Confirm
   once, then either delete this entry or move it out of this doc, since
   it likely belongs to the CI/CD process rather than Change Management.
   Answer: _(unanswered)_

7. **What does the client/project code "MCB" refer to?** Used as the name
   of a change template/model in one session. Is it a client on the same
   footing as SPIN/AFINZ, and if so, does it carry any special CAB handling
   of its own?
   Answer: _(unanswered)_

8. ~~**What is the literal dropdown label for the Incident-linked `Project
   Scope` value?**~~ **ANSWERED** — it is `Defect / Incident Related`.
   Folded into [[#Choosing a Project Scope]].

9. ~~**What time does the LAS CAB actually run — 15:00 or 16:00?**~~
   **ANSWERED — 16:00**, from the live agenda. The same source corrects the
   agenda's name to **LAN** (not LAM) and shows LAS covering Central *and*
   South America. Folded into [[#Regional CABs]].

10. **What is the exact `Maintenance Window` value required on a Production
    execution task?** Mostly answered, one string still unconfirmed. The
    always-safe value is **"No MW — Validation or Administrative Task"**,
    valid on validation tasks generally and on *both* tasks of a
    Standard/non-prod change. A **Production execution task** was said to
    need the API engineering team's own reserved production-deployment
    window instead — heard as "Issuer Latam", matching the `Issuer LATAM`
    business division, but never spelled out on screen — and using `No MW`
    there was said to get the change bounced ("it won't even pass, they'll
    send it back to be changed").
    **But the sessions contradict each other on this.** In the very first
    recording, the same person dictated the `No MW` value for the
    **execution** task of a **Production Normal change** without comment.
    So either the rule tightened between the two sessions, or the "gets
    bounced" claim is narrower than stated. Treat both the literal string
    and the rule itself as unsettled.
    Answer: _(unanswered)_

11. ~~**What does the field "On behalf of the vision" represent?**~~
    **ANSWERED — the name was a mis-hearing.** The field is
    **`On Behalf Division`**, and it takes divisions: one real change had
    `Merchant LATAM, Issuer LATAM`. It pairs with a separate
    **`Owning Division`** (`Issuer LATAM`). Nothing to do with Vision Plus —
    Whisper mangled "division" into "vision" and the team, copying the value
    from prior changes without reading it, never corrected the transcript.
    Folded into [[#Header fields]].

## Sources

- 2026-08-19 (morning) pairing session: Dani walking through opening a
  Normal Change, with Xenon/Nascimento clarifying region codes and Aranha as
  validator.
- 2026-08-19 (afternoon) pairing session: coordinating the Infra execution
  meeting with Raul, drafting description/business-reason/validation/backout
  fields, the DR task split, and the Risk-driven CAB weekday discovered
  partway through, live, on the change being built.
- 2026-08-21 pairing session: multi-region CCT tasks, the SPIN/AFINZ special
  handling and SPIN's post-CAB business approval, the `Expedited` resource,
  a Standard change end-to-end (Search environment), and the AI-assisted
  description workflow with its FiServ-vocabulary blind spot.
- 2026-08-25 session: a live HSM certificate-renewal fire drill (Vision Plus
  Brazil) that surfaced Restricted and Emergency change types, the
  `Expedite` form section's real location, a confirmed Risk-calculation
  driver (missing UAT/CAT evidence → Very High), the Venafi/HSM gotchas, and
  a multi-subsystem task structure beyond the usual four.
- 2026-08-26 session: Restricted-change named-approver variability and
  delegate coverage, a business-stakeholder escape hatch straight into CAB,
  a reviewer-roster change on the SPIN email flow (Hugo → Nelson Salazar),
  the "High Risk Review Board" flag, and cloning an existing change as a
  starting template.
- 2026-08-28 session: a near-miss where a submitted change was almost left
  undefended at CAB because nobody checked the next-day outcome — the
  resulting "check your email the morning after" practice, a second
  confirmed data point that pre-approval doesn't track `Risk` cleanly, and
  a conflicting restatement of the LAS CAB time.
- 2026-09-01 session: Rodrigo and Osvaldo building a Standard change for
  AFINZ under Dani's mentorship, hitting a change silently stuck in `New`
  from a CI/Assignment-Group mismatch (root-caused with Xeno's help), the
  APM-number CI-lookup technique, the `[Client]`-prefix short-description
  convention, Infra's escalation ladder and shift contacts, and the
  Teams-based fast individual approval trick.

**Rodrigo's own notes, "Changes: How to Make Them" (2026-09-01).** A
field-reference document written independently of this page. It is the
authority here for **form structure**: backend field names (`u_*`), complete
dropdown value lists, the Sub-Category map, the CAB agenda table, and the
validation decision table. It corrected this page's `Project Scope` values
(three had been mis-transcribed from audio), settled the LAS CAB time and
the LAN/LAM naming, supplied the always-required Operations validation rule,
and added the `Scheduled` state this page had been missing.

The two documents are complementary rather than overlapping: the notes are
strong on *what every field is*, this page is strong on *what goes wrong and
why* — the Restricted and Emergency flows, the seven CAB questions verbatim,
the two-tier approval model, the CI/Assignment-Group failure mode, and the
politics. Where they conflict, both readings are recorded in place rather
than silently reconciled.

**Change Request exports (from 2026-09-01).** Five real approved
production changes, read as full ServiceNow reports:

- `CHG002943069` — JWE key replacement, MasterCard tokenization proxy.
- `CHG002943079` — incident fix on the authorizeService transaction flow.
- `CHG002942986` — SSL cert renewal (Production), `Expedited`, Very High.
- `CHG002942777` — the same cert renewal for DR: same template, different
  target. The pair is the clearest evidence that a Change Model exists.
- `CHG002945203` — AFNZ Brazil Issuing API cert renewal across Prod and DR;
  also the source of the two defects in [[#Patterns across real changes]].

The first settled four open questions that seven recordings could not: the
seven CAB questions verbatim, the `Defect / Incident Related` label, the
`On Behalf Division` field (Whisper had rendered it "on behalf of the
vision"), and CCT/Command Center being one team. The next two settled the
`BAU Maintenance / Housekeeping` debate, the full `PITE` → Very High causal
chain, that Expedite sets `Type: Expedited`, and that the
execution/validation split is enforced on *people*, not groups.

**Prefer exports over recordings for anything about field names, literal
values, or structure** — recordings mishear labels and state rules more
rigidly than practice follows. Recordings remain better for reasoning,
exceptions, and politics. Two exports show a convention; one shows a
choice. Patterns in [[#Patterns across real changes]].

**Second pass, 2026-09-01 — all seven recordings.** Every recording was
re-read a second time, hunting specifically for detail the first pass had
discarded as "already covered". That was where most of this page's value
came from, including nearly every correction: the Restricted cutoff answer,
the ownership-ordering fix, `Check Conflict` red-vs-amber, the
approver-list-derives-from-CI mechanism, the backlog clock-vs-tile
distinction, the both-directions weekday split, `Environment` vs the DR
question, and almost all the "why" explanations.

**The lesson, for whoever extends this next:** the first pass asked "what's
new?" and threw away precision — detail that *sharpened* an existing line
looked redundant and got dropped. Ask instead "what does this contradict,
qualify, or explain?" Re-reading the same recording with a different
question was worth more than reading a new one.

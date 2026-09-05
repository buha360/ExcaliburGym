# Excalibur Gym Agent Guide

## Project purpose

Excalibur Gym is a local-first, browser-based administration system replacing the gym's paper membership and reception ledgers. The UI language and end-user terminology are Hungarian. Discuss product decisions with the user in Hungarian.

The repository contains an incrementally extracted service platform:

- `backend/`: authenticated Spring Boot core REST API
- `reporting-service/`: event-driven Spring Boot statistics projection service
- `sauna-service/`: independently deployable Spring Boot sauna reservation service with its own PostgreSQL database
- `solarium-service/`: independently deployable Spring Boot solarium minute ledger service with its own PostgreSQL database
- `event-contracts/`: shared, versioned Kafka envelope contract
- `frontend/`: Angular single-page application

## Current technology baseline

- Backend: Java 25, Spring Boot 4.1, Maven Wrapper, Lombok for constructor/boilerplate reduction
- Frontend: Angular 22.1, standalone components, strict TypeScript, CSS, npm
- Rendering: client-side SPA; do not add SSR or SSG
- API development: contract first with OpenAPI
- Database migrations: Liquibase is mandatory
- Authentication: employees and one administrator use a PIN or password
- Persistence direction: local relational database; SQLite is the current single-computer default
- Backup direction: consistent compressed database snapshots such as `.db.gz`; text and Excel files are exports, never the primary datastore
- Cloud/portfolio profile: PostgreSQL per service, Apache Kafka, Docker Compose, and Kubernetes manifests
- Service extraction strategy: strangler pattern; keep the core operational workflow deployable while extracting bounded capabilities
- Event API development: contract first with AsyncAPI in addition to HTTP OpenAPI
- Delivery semantics: transactional outbox with at-least-once publication and idempotent consumers

Do not silently replace a chosen technology or introduce a framework. Record material architectural changes in this file after the user accepts them.

## Commands

Run the complete Maven reactor from the repository root:

```powershell
mvn test
```

The root reactor covers `event-contracts`, `backend`, `sauna-service`, and `reporting-service`.

Run backend commands from `backend/`:

```powershell
.\mvnw.cmd test
# Local Windows fallback if the generated wrapper script fails:
mvn test
.\mvnw.cmd spring-boot:run
# Local Windows fallback:
mvn spring-boot:run
```

Run frontend commands from `frontend/`:

```powershell
npm install
npm start
npm test -- --watch=false
npm run build
```

Before handing off a change, run the smallest relevant tests and then the affected application's full test/build command when practical. Never claim a command passed unless it was run successfully.

## Architecture rules

- Treat the OpenAPI document as the source of truth for the HTTP contract.
- Generate Spring API interfaces/models and the Angular TypeScript API client from OpenAPI; do not maintain duplicate handwritten request/response models.
- Keep generated code separate from handwritten code and never edit generated files manually.
- Do not expose persistence entities directly through the REST API.
- Use Liquibase changelogs for every schema change. Never rely on Hibernate automatic schema creation outside tests.
- Store timestamps consistently and render them in the gym's local time zone (`Europe/Budapest`).
- Persist important actions transactionally before reporting success in the UI.
- Prefer append-only financial and audit records. Correct or reverse records instead of deleting history.
- Keep secrets and plaintext PINs/passwords out of source control. Store PINs/passwords using an appropriate password hash.
- A service owns its database; one service must never read or migrate another service's schema.
- Publish cross-service domain events through the transactional outbox, never directly inside the business transaction.
- Treat Kafka delivery as at-least-once. Every consumer must deduplicate by immutable `eventId` before changing projections.
- Version event payloads and keep `asyncapi/excalibur-events.yaml` aligned with emitted code.
- Local mode keeps SQLite and direct statistics without requiring Kafka. The `cloud` profile enables PostgreSQL, Kafka, and the remote reporting projection.
- Keep the core service at one replica while HTTP sessions are in-memory; externalize session storage before horizontal core scaling.

Demo records are opt-in only. Start a disposable demo database with the `demo` Spring profile; never activate that profile against production data:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

## Core domain rules

### Guests

Keep guest data deliberately minimal:

- full name
- generated internal guest identifier
- registration timestamp and import/origin marker
- current and historical passes
- visit history
- prepaid balance and transaction history

Do not add phone number, email, notes, profile picture, QR/barcode, or a manually maintained active/inactive/blocked guest flag unless the user later requests it.

Guest status is derived per service. A guest can simultaneously have an expired gym pass and remaining solarium minutes. Display separate service statuses rather than one ambiguous global status.

A new guest is someone who did not previously exist in the database. Guests imported from the old paper register must not inflate new-guest statistics; mark them as imported.

Names are not unique. Never use a guest's name as a database key.

### Employees and administrator

- Roles: `EMPLOYEE` and `ADMIN`.
- The administrator is the owner and may also perform reception work.
- The administrator manages employee accounts.
- Record login/logout events and their timestamps.
- There is no shift domain or shift entity.
- Attach the authenticated employee identity directly to every pass sale, check-in, payment, correction, booking, and relevant audit event.
- Do not add a redundant shift reference to these actions.
- The append-only activity log is visible in full to `ADMIN`; an `EMPLOYEE` can only read entries attached to their own employee identifier. Enforce this filter on the backend.

### Gym products and passes

Initial gym offerings:

- day pass
- 8-entry pass, valid for 60 days
- 12-entry pass, valid for 60 days
- unlimited monthly pass
- women's unlimited pass
- student/pensioner pass
- annual pass
- 3-month pass
- 6-month pass
- combined monthly pass: gym plus 4 sauna entries

Pass definitions and prices should be configurable rather than hard-coded as Java or TypeScript enums. A sold pass stores a price/product snapshot so later price changes do not rewrite history.

The administrator manages gym pass definitions and their current default prices. Removing a definition means deactivation, never deletion of historical sales.

Store purchase date, validity start/end, remaining entries where applicable, price, payment method, and the employee who issued or renewed the pass. Renewals create a new historical record; they do not overwrite the old pass.

Gym passes can be paid only by cash or bank card. The internal guest balance is not a valid gym-pass payment method.

Repeated same-day gym entry is allowed because trainers may enter multiple times. Warn the employee about the earlier entry and require confirmation. Do not silently block it.

Incorrect same-day check-ins can be reversed with a mandatory reason. Reversal restores any consumed entry. Preserve both the original action and reversal in the audit history.

Passes are invalidated or corrected, not hard-deleted.

### Solarium

Initial offerings:

- pay/use by whole minute
- 60-minute pass
- 90-minute pass

Track remaining minutes and each minute deduction as history. Do not permit an accidental negative balance without an explicit confirmed rule.

### Sauna

Initial sauna bookings are available for 15, 30, 45, or 60 minutes for 1, 2, or 3 people, plus the sauna entries included in the combined monthly pass.

The extracted `sauna-service` owns sauna reservations and their PostgreSQL schema. The authenticated core API is the browser-facing facade and supplies immutable guest and employee snapshots to the service. Reservation and cancellation events are published through the sauna service's transactional outbox.

Support a reservation calendar showing current occupancy and the next available time. Prevent conflicting reservations for the same sauna resource. Store guest, start/end, party size, status, payment data, and employee.

### Prepaid balance and reception sales

Guests may deposit money and later pay only for reception retail goods (for example coffee, water, protein, or creatine) from their internal balance. Gym passes and other service passes cannot be paid from this balance. Model this as an immutable transaction ledger; do not store only a mutable total.

Payment methods currently include:

- cash
- bank card
- internal prepaid balance, where applicable

Each sale item is recorded separately. For example, mineral water and a protein bar must remain two distinct product lines even if paid together. Corrections use reversal/storno records with a reason rather than deletion.

A guest-balance deposit is recognized as revenue when the money is received, under its actual tender (cash or bank card). A later retail purchase paid from that balance is tracked as prepaid usage but must not increase total revenue again.

Do not add inventory tracking until explicitly requested. Whether negative guest balance/debt is allowed remains an unresolved business decision.

### Statistics

The hamburger navigation contains a dedicated `Statisztika` page with these period views:

- daily
- weekly
- monthly
- yearly

Relevant metrics include visits, new guests, pass sales, revenue, revenue by payment method, prepaid deposits/usage, solarium minutes, sauna reservations, product sales, reversals/corrections, and expired-but-not-renewed passes.

Do not add pass-popularity reporting unless the user later requests it.

### Backups

- A successful UI action must be durably committed to the database immediately; this is persistence, not a full backup.
- Create consistent compressed local database snapshots on a schedule and on manual request.
- Use retention/rotation so backups do not grow without bound.
- Provide CSV/Excel or readable text exports where useful, but never treat them as the canonical database.
- Recovery after a power failure or restart must preserve data and allow the receptionist to continue after authentication.

## UI and UX direction

- Visual identity: forged graphite surfaces based around `#121415` and `#0c0e0f`, with layered charcoal elevation, champagne-gold `#c9952b` / `#f1c24f` focus accents, warm off-white text, and restrained status glows. Reserve bright gold for primary actions, focus, active navigation, and key metrics rather than outlining every surface.
- Layout: hamburger menu at top left, centered gym logo, employee/admin dropdown at top right, prominent quick guest search below the logo.
- On desktop, the sidebar is a persistent non-modal workspace rail: opening it smoothly makes room for the page, never blurs or blocks the content, and route navigation does not close it. On narrow screens it may overlay the page without a blocking backdrop.
- Search results use black cards with gold borders.
- Status must use text/iconography in addition to color: green active, gold warning/expiring, red expired/depleted.
- Clicking a search result opens the guest details view with pass history, validity, remaining uses/minutes, issuing employee, visits, and balances.
- Optimize receptionist workflows for speed and a small number of clicks.
- The dashboard quick search shows at most six guests in a responsive two-column grid, prioritizing the nearest active pass expiration.
- Keep components accessible and responsive; do not sacrifice readability for decorative glow effects.

## Frontend conventions

- Use standalone Angular components and lazy-loaded feature routes where useful.
- Use strict typing; do not introduce `any` to bypass type errors.
- Use typed reactive forms for non-trivial forms.
- Keep API access behind generated OpenAPI services and small application-facing adapters/facades.
- Keep business rules on the backend. The frontend may mirror rules for immediate feedback but is not authoritative.
- Add unit tests for meaningful state and validation behavior.

## Backend conventions

- Organize code by business feature/domain rather than one global controller/service/repository package tree.
- Keep controllers thin and transactional business behavior in application/domain services.
- Validate all externally supplied data on the server.
- Enforce authorization on the backend, not only by hiding frontend controls.
- Use database constraints for important invariants and Liquibase for indexes/constraints.
- Add unit tests for business rules and integration tests for persistence/API boundaries.

## Explicitly out of scope for now

- SSR/SSG
- public customer portal
- email notifications
- customer phone/email CRM features
- profile photos
- QR/barcode entry
- occupancy estimation or tracking who is currently inside
- manual banned/inactive guest state
- product popularity reports
- inventory management
- a separate shift-management model

## Working agreement

- Preserve user changes and inspect the current tree before editing.
- Prefer small, reviewable increments that leave both applications buildable.
- When a requirement is genuinely ambiguous and affects stored data or money, call it out rather than inventing a silent rule.
- Update this file when the user confirms a durable product or engineering decision.

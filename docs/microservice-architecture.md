# Microservice migration

The operational gym application remains usable as a local SQLite-backed core for reception workflows. The cloud profile extracts independently deployable services incrementally with the strangler pattern.

## Current service topology

```text
Angular SPA
    |
    v
Core service (session authentication + public BFF API)
    |                  |                    |
    | internal REST    | internal REST      | internal REST
    v                  v                    v
Reporting service  Sauna service       Solarium service
    ^                  |                    |
    | projections      | transactional      | transactional
    |                  | outbox             | outbox
    +------------------+------- Kafka ------+
                               ^
                               |
                       core transactional outbox
```

- The core service owns authentication, employees, guests, gym products, passes, check-ins, reception retail sales, guest balance and audit records.
- The sauna service owns selectable 15/30/45/60-minute reservations and its own PostgreSQL database. It receives immutable guest and employee snapshots from the authenticated core facade and never reads the core database.
- PostgreSQL prevents overlapping active sauna reservations with an exclusion constraint, so concurrent requests cannot double-book the resource.
- The solarium service owns minute products, guest minute balances and immutable transaction history in a separate PostgreSQL database.
- Mutating services persist integration events through a transactional outbox in the same transaction as the domain change.
- The reporting service consumes the domain events, deduplicates by immutable `eventId`, and owns its projection database.
- The frontend only calls the authenticated core API. Internal service API keys are never exposed to the browser.

## Contracts

- `openapi/excalibur-gym.yaml`: browser-facing core HTTP contract and generated Angular client.
- `openapi/sauna-service.yaml`: core-to-sauna internal HTTP contract.
- `openapi/solarium-service.yaml`: core-to-solarium internal HTTP contract.
- `openapi/reporting-service.yaml`: core-to-reporting internal HTTP contract.
- `asyncapi/excalibur-events.yaml`: Kafka topics and event payload contracts.
- `event-contracts/`: shared immutable event envelope and topic constants.

## Reliability rules

- A Kafka publish failure never rolls back an already committed reception operation; the outbox dispatcher retries it.
- Delivery is at least once. Consumers are idempotent because publication may repeat after a crash.
- Services never query or migrate another service's database.
- Business invariants such as sauna overlap and non-negative solarium minutes are enforced in the owning service.
- Kubernetes Deployments run stateless application containers. PostgreSQL and Kafka endpoints are external configuration; production should use managed or operator-backed stateful infrastructure.
- The core remains at one replica while authentication uses in-memory HTTP sessions. External session storage is required before horizontal core scaling.

## Deliberately deferred sauna sales

This slice covers scheduling and cancellation. Sauna pricing, payment method, prepaid-balance payment and revenue events require the gym's actual 1/2/3-person price list. No placeholder price is silently invented.
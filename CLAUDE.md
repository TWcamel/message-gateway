# MNP Pub/Sub ↔ NATS Validation Sandbox

Sandbox for validating the end-to-end path:
**HTTP → gcp-mnp (OUTBOX) → GCP Pub/Sub → nats-adapter (INBOX) → NATS (OUTBOX) → nats-consumer (INBOX, log)**.

## Architecture Decisions

| Decision | Rationale |
|---|---|
| Spring Boot **3.5.14 GA** / Java **21** | Stable LTS platform; Spring 6 ecosystem |
| **4 independent Maven projects** (`commons`, `gcp-mnp`, `nats-adapter`, `nats-consumer`) | Fully decoupled, each deployable independently |
| `google/cloud-pubsub-emulator` + official `nats:2.10-alpine` | Docker-sandbox faithful to production protocols |
| **Two NATS servers** (`nats-alpha`, `nats-beta`) with **isolated subjects** | Validates multi-server routing + subject isolation |
| Unified API envelope `{code, data|message}` with prefix **`MNPGCP`** | Consistent observability across services |
| No DB / Transactional Outbox | Validation-only scope; topics/subjects suffice |
| Lombok + Functional + Stream API + `Objects.isNull()/nonNull()` | Enforced coding standard |

## Component Map

```
[curl] ──POST──> [gcp-mnp:8080]/api/v1/publish           (OUTBOX)
                      │  topic: gcp-mnp-outbox
                      ▼
           [GCP Pub/Sub Emulator:8085]
                      │  subscription: gcp-mnp-outbox-sub
                      ▼
           [nats-adapter:8081]                            (INBOX→OUTBOX bridge)
                      │  subjects: mnp.{alpha|beta}.outbox
                      ▼
           [nats-alpha:4222]  [nats-beta:4222]            (two isolated servers)
                      │  subjects: mnp.{alpha|beta}.inbox
                      ▼
           [nats-consumer:8082]                           (INBOX consumer → log)
```

## Getting Started

### Prerequisites
- Docker + docker compose v2
- **Java 21** in current shell (`java -version` → 21.x)
- Maven 3.9+ (`mvn -v`)

### One-time: install the commons library
```bash
cd commons && mvn -B clean install -DskipTests
```

### Run the end-to-end test
```bash
./e2e-test.sh           # builds, runs stack, asserts, tears down
./e2e-test.sh --keep    # same, but keeps containers for debugging
```

### Manual operations
```bash
docker compose up -d                  # start stack
docker compose ps                     # check health
docker compose logs -f nats-consumer  # tail consumer logs
curl -X POST http://localhost:8080/api/v1/publish
docker compose down -v                # clean up
```

## Service Ports

| Service         | Port  | Path / Note                |
|-----------------|-------|----------------------------|
| gcp-mnp         | 8080  | POST `/api/v1/publish`     |
| nats-adapter    | 8081  | actuator/health            |
| nats-consumer   | 8082  | actuator/health            |
| pubsub-emulator | 8085  | GCP Pub/Sub emulator       |
| nats-alpha/beta | 4222/4223 | NATS client port       |

## Coding Conventions

- **No `var != null`** — use `Objects.isNull()` / `Objects.nonNull()` / `Preconditions.ensureNonNull()`
- **Lombok** for boilerplate (`@Data`, `@Slf4j`, `@RequiredArgsConstructor`, `@Builder`)
- **Stream API** for collection transforms and filters
- **Functional interfaces** (Optional, CompletableFuture) preferred over imperative null-checks
- **Preconditions** (`com.mnp.commons.validation.Preconditions`) for fail-fast assertions
- **Utils** (`NatsUtils`, `IdUtils`, `TimeUtils`) are reusable across services

## Verification Checklist

After running `./e2e-test.sh` you should see:
1. Script exits with code 0 and prints `E2E PASSED`
2. nats-consumer log contains the published `messageId`
3. nats-adapter log shows publish to **both** `alpha` and `beta` outbox subjects

Error path: `curl -X POST http://localhost:8080/api/v1/publish -H 'Content-Type: application/json' -d ''`
→ expect `{"code":"MNPGCP500","message":"..."}`

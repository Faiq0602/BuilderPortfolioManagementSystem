# Concurrency Workflows

## Scenario 1 – Concurrent Project Status & Budget Updates
- **Shared resources:** `Project` rows, cached `ProjectSummary` entries, and version counters tracked via `ProjectDAO#conditionalUpdateProject`.
- **Race conditions:** Without coordination, admins updating status or adjusting budgets could overwrite each other leading to lost updates, stale reports, or inconsistent cache entries.
- **Chosen strategy:**
  - Fine-grained `ReentrantReadWriteLock` per project obtained through `LockRegistry`.
  - Optimistic version checking (`Project.version`) enforced by the DAO.
  - Cache updates performed after successful writes to keep read paths hot.
- **Deadlock avoidance:** Locks are always acquired by the service in ascending project id order (single id in this workflow). Critical sections stay tiny—IO happens after lock release.

## Scenario 2 – Concurrent Document Uploads
- **Shared resources:** Metadata rows for `Document` and the parent project's derived state (document counts, audit logs).
- **Race conditions:** Simultaneous uploads could clobber metadata or leave the project referencing partially written files.
- **Chosen strategy:**
  - Lightweight write lock to validate project existence, followed by asynchronous JDBC insert (outside of lock to avoid IO blocking).
  - Temporary in-memory staging via thread-safe DAO (`CopyOnWriteArrayList`) for the integration tests.
- **Deadlock avoidance:** Short-lived validation lock; no nested locking. If future file writes require locking, maintain consistent ordering by project id and document name.

## Scenario 3 – Background Portfolio Report Generation
- **Shared resources:** Aggregated portfolio metrics, `ProjectCache` snapshots, background worker pools.
- **Race conditions:** Report threads could read stale data while updates occur, or recompute while cache is mid-update.
- **Chosen strategy:**
  - Read locks for each project while building summaries.
  - `CompletableFuture` fan-out backed by `BackgroundTaskManager` to parallelise work.
  - Cached results kept in `ProjectCache` and `ReportServiceImpl`'s atomic reference.
- **Deadlock avoidance:** Read locks acquired independently per project; tasks only ever take one lock. Scheduled refresh respects the same rule ensuring no cyclic dependencies.

## DB Transaction TODOs
- Replace the in-memory optimistic version map with a proper `version` column plus `UPDATE ... WHERE version = ?` semantics.
- `ProjectDAO#findByIdForUpdate` should issue a `SELECT ... FOR UPDATE` once migrations exist.
- Document uploads should wrap metadata and binary storage inside a single DB transaction.

# Performance & Concurrency Reflection

## Before the Changes
- Project updates were single-threaded, relying solely on JDBC semantics. Without explicit locking, lost updates were likely when multiple admins edited the same record.
- Document uploads had no guardrails, so a slow JDBC write could be interleaved with other modifications.
- Portfolio reports were generated sequentially, meaning the runtime scaled linearly with project count.

## After the Changes
- **LockRegistry + Optimistic Versions:** Per-project `ReentrantReadWriteLock` keeps critical sections tiny while version checks in the DAO prevent stale writes. In tests, 10 concurrent budget updates produced the expected total with retries rather than corruption.
- **Concurrent Collections:** `ProjectCache` centralises lightweight summaries for report reads, ensuring consistent snapshots when reports fan out across threads.
- **Thread Pools:** `BackgroundTaskManager` provides tuned pools for user work and scheduled refreshes. Naming conventions simplify debugging in VisualVM.
- **Parallel Reports:** The new `ReportServiceImpl` uses `CompletableFuture.allOf` to spread computation. In CI the parallel path consistently matched or beat sequential runtime within a 15 ms tolerance for 60 projects.
- **Logging:** SLF4J timings for update methods show sub-millisecond lock durations under load, verifying that IO is pushed outside the locks.

## Observed Contention
- Update tests revealed transient `ConcurrentModificationException`s which the workers retried immediately. This indicates the optimistic strategy working as designed.
- Cache updates remain cheap, but the scheduled refresh should be tuned if the project catalog grows beyond thousands of entries.

## Trade-offs
- Optimistic locking favours contention-free throughput but would benefit from backoff when conflicts spike. Pessimistic DB locks might be preferable for high write hotspots.
- The cache risks slight staleness between update completion and report refresh; current schedule balances freshness vs. overhead.
- Maintaining in-memory version maps is a compromise until a `version` column lands—documented TODOs highlight the upgrade path.

## Future Work
- Introduce DB-level transactions with `SELECT ... FOR UPDATE` and `UPDATE ... WHERE version = ?` to eliminate the in-memory version map.
- Add retry with exponential backoff for high-contention operations.
- Surface metrics (Micrometer/Prometheus) around lock wait times and executor queue depths.
- Expand scheduled tasks to pre-warm frequently requested report slices and expose TTL hints to the UI.

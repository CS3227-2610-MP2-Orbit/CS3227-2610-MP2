---
name: database-migration-and-integrity
description: Design and verify normalized PostgreSQL schema changes, migrations, constraints, transactions, and data-integrity rules.
---

# Database migration and integrity

Use this skill for PostgreSQL schema changes, Flyway migrations, repositories,
booking intervals, and workflows that update multiple records.

## Inspect first

Read the existing schema, migrations, entities, repositories, transaction
helpers, and database configuration. Reuse existing entities and relationships
where possible. Do not duplicate data already represented elsewhere.

## Design rules

- Use normalized tables with explicit primary and foreign keys.
- Validate required values, timestamps, and interval boundaries.
- Add indexes for real lookup and conflict-detection paths.
- Define how duplicate and overlapping records are prevented.
- Keep related writes atomic and specify rollback behavior.
- Make migrations ordered, repeatable in a clean database, and compatible with
  existing data.

## Verify

Review migration syntax, repository queries, constraints, transaction failure
paths, and integration tests. Report database checks that could not run because
PostgreSQL or required dependencies were unavailable.

# Cursor project hooks for MP2 single-agent guardrails

## Purpose

These hooks mechanically enforce process boundaries for a **single AI agent**
workflow. They complement skills under `.agents/skills/` (how to do work) with
deterministic allow/deny/ask decisions (what must not happen).

All hooks are **command hooks** (Python 3 stdlib). No prompt/LLM hooks.

## Files

| File | Role |
| --- | --- |
| `.cursor/hooks.json` | Registers events → scripts |
| `.cursor/hooks/deny_subagent.py` | Deny every `subagentStart` |
| `.cursor/hooks/guard_shell.py` | Gate `beforeShellExecution` |
| `.cursor/hooks/protect_secrets.py` | Gate `beforeReadFile` for secrets |
| `.cursor/hooks/verify_on_stop.py` | Run focused Gradle tests on `stop` (once) |
| `.cursor/hooks/audit_lib.py` | Shared JSONL audit helper |
| `.cursor/hooks/tests/test_hooks.py` | Deterministic unit tests |

Audit log (local): `evals/artifacts/hook-events.jsonl` (gitignored).

## Hook catalogue

### 1. Deny subagents — `subagentStart`
- **Allows:** nothing (always deny)
- **Blocks:** Task / explore / generalPurpose / any subagent start
- **Evidence:** audit `decision=deny`, reason mentions single-agent
- **Failure:** `failClosed: true` — crash denies the subagent

### 2. Guard shell — `beforeShellExecution`
- **Allows:** `./gradlew …`, `git status/diff/log/branch`, `ls`, `rg`, `python3` graders, etc.
- **Asks:** `curl`/`wget`/`ssh`, `pip`/`npm`/`brew install`, `--refresh-dependencies`
- **Blocks:** `rm -rf`, `git reset --hard`, `git clean`, force push, push to main/master, `DROP`/`TRUNCATE`, `flyway clean`, `git checkout --`
- **Evidence:** audit line with sanitized command + decision
- **Failure:** `failClosed: true`
- **False positives:** unusual but safe commands may `ask`; approve in UI when intentional
- **Does not execute** the agent command inside the hook (parse + classify only)

### 3. Protect secrets — `beforeReadFile`
- **Allows:** `.env.example`, `.env.template`, `.env.sample`, ordinary source files
- **Blocks:** `.env`, `.env.local`, private keys, `credentials.json`, `.ssh`/`.aws` paths
- **Evidence:** audit path redacted as `<secret-file:…>` when denied
- **Failure:** `failClosed: true`

### 4. Verify on stop — `stop` (`loop_limit: 1`)
- **Command:**
  `./gradlew test --tests 'seedu.eventmanager.common.*' --tests 'seedu.eventmanager.event.*' --tests 'seedu.eventmanager.MainTest' --no-daemon`
- **Once per stop cycle** via `evals/artifacts/.verify-once`
- **On failure:** `followup_message` tells the agent not to claim success
- **Limitation:** does not replace full CI (no Postgres integration suite here)

## Workflow contract

| Requirement | Observable evidence |
| --- | --- |
| No subagents | Subagent invocation denied and logged |
| Dangerous commands controlled | Shell hook deny/ask logged |
| Sensitive files protected | Secret-file deny logged |
| Actions traceable | Valid JSONL in `evals/artifacts/hook-events.jsonl` |
| Verification before completion | Verify command + exit recorded on stop |
| Single-agent workflow | No prompt hooks; subagentStart always deny |

## Manual tests

```sh
# Unit tests (no Cursor UI required)
python3 -m unittest discover -s .cursor/hooks/tests -v

# Synthetic allow
echo '{"command":"./gradlew test"}' | python3 .cursor/hooks/guard_shell.py

# Synthetic deny
echo '{"command":"rm -rf /"}' | python3 .cursor/hooks/guard_shell.py

# Secret deny / template allow
echo '{"file_path":".env"}' | python3 .cursor/hooks/protect_secrets.py
echo '{"file_path":".env.example"}' | python3 .cursor/hooks/protect_secrets.py

# Subagent deny
echo '{"subagent_type":"explore"}' | python3 .cursor/hooks/deny_subagent.py
```

In Cursor: enable project hooks, then attempt a Task/subagent and a `rm -rf` to
confirm UI messaging. Restart Cursor if hooks do not reload.

## Limitations

- Shell classification is pattern-based; unknown commands default to **ask**.
- Absolute paths outside the repo used by tools may be over-blocked.
- `stop` verification is a focused unit subset, not full CI.
- Hook stdin field names can vary by Cursor version; scripts accept common aliases.

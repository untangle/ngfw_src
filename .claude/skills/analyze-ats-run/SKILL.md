---
name: analyze-ats-run
description: Analyze the most recent ATS (Automated Test Suite) run from the team's internal ATS results server for the master (latest) and released (one-lesser) NGFW versions. For each device, diff today's failures vs the prior run — flagging regressions (new failures), resolved failures, and changed skip reasons — then root-cause each regression by reading the test source and the git commit range between the two runs' build SHAs. Writes a markdown report under /tmp/ and prints a summary inline. Invoke when the user asks to analyze ATS results, check the nightly run, look for new test failures, or compare recent ATS runs.
---

# analyze-ats-run

Analyze the most recent ATS run for the latest NGFW version (master) and the one-lesser NGFW version (ngfw-release-<MAJOR.MINOR>), surface regressions and skip changes, and root-cause regressions against the git diff between build SHAs.

## Inputs

- Optional arg: a specific version (e.g. `17.5.0`). If omitted, analyze both latest and second-latest.
- Optional arg: `--no-confirm` to skip user confirmation of version/date picks (default: confirm).

## Outputs

- `/tmp/ats-analysis-<version>-<YYYY-MM-DD>.md` per version analyzed (full report).
- Concise inline summary in chat: counts, regressions, top suspect commits.

## Hard rules

- **Never** switch git branches or modify the working tree. All git reads are SHA-based (`git show <sha>:path`, `git log <oldSha>..<newSha>`).
- **Never** silently assume the version or dates — always confirm with the user before fetching logs (unless `--no-confirm`).
- **Fail fast** if the ATS server is unreachable. Don't write a partial report.
- **Context discipline** (see section below). This skill processes many large logs; naive reads will exhaust the window before the report is written.

## Context discipline

This skill fans across (versions × devices × 2 runs) and each `unit-tests.log` is 2-3 MB. Hold raw log bytes in context for as little time as possible.

- **Always download logs to disk** via `curl -sS -o <localPath>`, then process with shell tools (`grep`, `awk`, `sed`, `wc`). Stage them under `/tmp/ats-cache/<version>/<runDir>/`.
- **Never** `Read` `unit-tests.log` whole. Use `grep -n "skipped '" <file>` plus a small `-B5` window to extract test ID + reason in a few hundred lines max.
- `failures.log` is moderate (~200 KB) but still — first pass: `grep -E "^\[[0-9]+/" <file>` to enumerate failed test IDs only. Second pass: extract full entry only for tests you actually need to discuss (regressions). Don't pull the whole file just to count.
- After parsing each log into a structured dict (`{testId: errorSummary}` / `{testId: reason}`), **discard the raw text**. The dict is what diffing and reporting need.
- Per failure entry, keep only: `type`, `fullName`, `errorSummary` (the last meaningful line), `tracebackPath:line`, and **at most 5 lines** of `UVM Logs:` (the most recent/relevant). Drop the long error/command-output blocks unless a regression genuinely needs the full text — and even then, write it to the report file and drop it from context after.
- Process devices **sequentially**, not in parallel. Finish device A's diff → write its section to the report file → drop its logs from context → move to device B. Never hold all devices' logs at once.
- For git: `git log --oneline <prior>..<recent>` may be hundreds of commits on a busy day. Filter by test-relevant paths FIRST (`git log --oneline <prior>..<recent> -- <path>`); only fall back to the unfiltered range if the filtered list is empty. Don't enumerate the full range "just to see."
- If context tightens mid-run: **compact aggressively**. Keep diff conclusions (regression list, resolved list, skip changes, suspect commits) and device health findings. Shed raw log content, full tracebacks (already in the report file), and unfiltered commit listings. The report file on disk is the durable artifact — context only needs enough to keep writing it coherently.

## Flow

### 1. Ask for ATS host, then probe reachability

**Ask the user for the host or IP on every run.** Do not reuse a value from prior runs or hard-code one in this file. Use `AskUserQuestion`:

> What is the ATS results server host or IP for this run? (e.g. `10.112.11.86` or `ats-results.lab.internal`)

Accept just the host/IP (the common case) or a full URL (graceful fallback). Construct `<ATS_BASE>` from the input:

- If input starts with `http://` or `https://`: use as-is, strip any trailing `/`.
- Otherwise: `<ATS_BASE>` = `http://<input>/ats`

Then probe:

```bash
curl -sSf --max-time 5 -o /dev/null <ATS_BASE>/
```

If this fails: tell the user "ATS server at `<ATS_BASE>` is unreachable — check VPN/network or the host/IP" and stop. No further work.

### 2. Enumerate versions and confirm with user

Fetch the index:

```bash
curl -sS <ATS_BASE>/
```

Parse `<a href="X.Y.Z/">` entries to get version directories. Sort by semver descending.

- **Latest (master)** = the top entry — the highest semver overall.
- **Released (ngfw-release-<MAJOR.MINOR>)** = the **highest** semver among entries whose `MAJOR.MINOR` is **different** from the latest. Older patches within the same MAJOR.MINOR as latest, and older patches within the released MAJOR.MINOR, are **ignored**.

Worked example with three versions `[18.0.0, 17.5.1, 17.5.0]`:
- Latest = `18.0.0` → branch `master`
- Released = `17.5.1` → branch `ngfw-release-17.5` (highest patch in the next-distinct `MAJOR.MINOR=17.5`; `17.5.0` is **not** analyzed)

Edge case: if every available version shares the same `MAJOR.MINOR` as latest (no distinct second `MAJOR.MINOR` exists), there is no released version to analyze — tell the user and proceed with master only.

**Confirm with the user** using `AskUserQuestion` before proceeding:

> I see latest = `<X.Y.Z>` (branch `master`) and released = `<A.B.C>` (branch `ngfw-release-<A.B>`). Analyze both?

Options: "Both", "Latest only", "Released only", "Different versions" (user enters via Other).

### 3. Per version: enumerate runs, pick dates per device, confirm

Fetch the version index:

```bash
curl -sS <ATS_BASE>/<version>/
```

Each run directory matches this pattern:

```
<version>.<buildTimestamp>.<buildSha>-1<distro>_<runISO>_<device>/
```

Example shape: `<X.Y.Z>.<YYYYMMDDThhmmss>.<sha10>-1<distro>_<runIsoWithOffset>_<device>` (concrete values vary per build/device and are not committed to this file).

Parse with a regex like:
```
^(\d+\.\d+\.\d+)\.\d{8}T\d{6}\.([0-9a-f]+)-\d+\w+_(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+\-]\d{2}:\d{2})_(.+?)/?$
```
Capture: `version`, `buildSha`, `runIso`, `device`.

Group runs by `device`, sort each group by `runIso` descending. For each device, take the top 2 runs as `recent` and `prior`.

**Device health detection** (compute alongside, surface in report):

- **Expected device set** = the union of all devices that appear in any run within the last 7 days for this version. This is the baseline of "devices that normally run on this version."
- **Missing today** = devices in the expected set whose most recent run is **not** from the latest run date observed across all devices. These devices skipped the latest cycle.
- **No-prior** = devices that have exactly 1 run total in the window (can't be diffed; recent is reported as-is, no diff section).
- **Zero-result run** = a run whose `unit-tests.log` is missing, empty, or contains no test results (see step 5). The device ran but produced nothing — usually a crash before tests started.

None of these are errors that stop the skill. They are **findings** that get a dedicated "Device health" section in the report. Do not silently drop them.

**Confirm with the user** before fetching logs. Show defaults *and* offer a date-pair override via `AskUserQuestion`:

> For `<version>`, the latest run cycle is `<latestDate>` and the prior is `<priorDate>`. Per device:
> - `<device-A>`: recent `<recentIso>` (sha `<recentSha>`) vs prior `<priorIso>` (sha `<priorSha>`)
> - `<device-B>`: recent `<recentIso>` (sha `<recentSha>`) vs prior `<priorIso>` (sha `<priorSha>`)
> Available run dates for this version (last 30 days): `<list>`
> How would you like to pick the two dates to compare?

Options:
- **"Use defaults (last + second-last)"** — proceed with the recent/prior pairs shown.
- **"Pick specific dates"** — user replies via Other with two dates `<newerDate> vs <olderDate>` (e.g. `2026-06-04 vs 2026-06-01`). One global pair applied to all devices.
- **"Pick date range"** — walk the range and produce multiple diffs, one per consecutive-checkpoint pair. User replies via Other with one of:
  - Two ISO dates separated by `to` or `..`: e.g. `2026-06-22 to 2026-06-29` or `2026-06-22..2026-06-29` (whitespace optional around the separator).
  - Relative window: `last N days` (e.g. `last 7 days`) — resolved to `[today - N + 1, today]` inclusive, where "today" is the current date.

  Validation rules (applied in order; the skill rejects and re-asks on each, never silently substitutes):
  - **Parse failure** — the input doesn't match either ISO-date-pair or `last N days` shape. Re-ask once with the original prompt; if the second reply also fails to parse, abort the date-range path and surface the same `AskUserQuestion` from the start of Step 3 so the user can pick a different option (Use defaults / Pick specific dates / Cancel).
  - **`last N days` lower bound** — `N < 2` is rejected. `last 0 days` is nonsensical and `last 1 days` resolves to a single date `[today, today]` with no possible pair. Tell the user "range needs at least 2 days; use `last 2 days` or wider, or pick `Pick specific dates` for a single pair." Re-ask once with the original prompt.
  - **`start > end`** — reject and re-ask: "Range end `<end>` is before start `<start>`; check the order."
  - **`start == end` (same-date range)** — reject and re-ask: "Range is a single date `<start>`; need at least 2 distinct dates for a pair. Use `Pick specific dates` if you want a single-pair analysis."

  If after one re-ask the input is still invalid for the same reason, abort the date-range path (don't loop indefinitely) and surface the top-level Step 3 question again.

  **After the range is parsed, ask the user for the hop interval** via a second `AskUserQuestion`, and explicitly state the recent-window rule:

  > Range parsed: `<start>` to `<end>` (<N> days). The last 3 days will always be analyzed consecutively (every day). For older days in the range, how many days should I hop between analyses? Default is 3 (analyze every 3rd day) — useful when consecutive days usually produce identical results and per-day analysis would be redundant.

  Options:
  - **"Hop = 3 (default, every 3rd day)"** — proceed with hop=3.
  - **"Hop = 1 (analyze every day)"** — no hop; every day in the range is a checkpoint. Big report; only pick this if per-day granularity matters.
  - **"Hop = 7 (weekly)"** — coarse scan; one checkpoint per week.
  - **"Custom hop"** — user replies via Other with an integer ≥ 1.

  **Checkpoint generation** (deterministic; document for users so the picks aren't surprising):

  Let `rangeLen = (end - start) + 1` (inclusive day count, so a range `[05-01, 05-31]` has `rangeLen = 31`).

  1. **Recent window** = the last 3 dates of `[start, end]` if `rangeLen >= 3`; otherwise the entire range. Always checkpoints, always consecutive (every day, regardless of hop).
  2. **Older portion** = `[start, end - 3]` (inclusive on both ends). If `rangeLen <= 3`, the older portion is empty — the entire analysis is the recent window from rule 1, and rule 2 contributes nothing. Otherwise: generate older checkpoints starting at `start`, stepping by `hop`, including each date `d` for which `d <= end - 3` (inclusive). The cap is **inclusive of `end - 3`** — e.g. with `end = 2026-05-31`, the highest older checkpoint allowed is `2026-05-28`.
  3. Combine the recent-window set and the older-checkpoint set, sort ascending, de-duplicate. This is the **checkpoint list** for the analysis. (De-dup matters in the boundary case where the older walk lands exactly on `end - 3` AND `end - 3` is also the first day of the recent window for a shorter range.)
  4. **Analysis pairs** = consecutive pairs of checkpoints, i.e. `(checkpoints[0], checkpoints[1]), (checkpoints[1], checkpoints[2]), …`. Each pair becomes one diff section in the report. If the checkpoint list has only 1 element (i.e. `rangeLen == 1` after validation slipped — should not happen given the rejects above, but defend against it), abort with a clear message: "Range produced 1 checkpoint, 0 pairs — nothing to analyze; widen the range."

  Short-range examples (so the implementer can sanity-check rule 1's "or shorter" branch):
  - `rangeLen = 2` → recent window `[start, end]` (2 days), older portion empty, checkpoints `[start, end]`, **1 pair**.
  - `rangeLen = 3` → recent window `[start, start+1, end]` (3 days), older portion empty, **2 pairs** (consecutive).
  - `rangeLen = 4`, `hop = 3` → recent window `[end-2, end-1, end]`, older portion `[start, end-3] = [start, start]`, older checkpoints `[start]`, combined `[start, end-2, end-1, end]`, **3 pairs**.

  Worked example: `start=2026-05-01`, `end=2026-05-31`, `hop=3` → `rangeLen=31`.
  - Recent window: `2026-05-29, 2026-05-30, 2026-05-31` (last 3 dates, consecutive).
  - Older portion cap: `end - 3 = 2026-05-28` **(inclusive)** — `05-28` is eligible to be an older checkpoint.
  - Older checkpoints: `2026-05-01, 2026-05-04, 2026-05-07, 2026-05-10, 2026-05-13, 2026-05-16, 2026-05-19, 2026-05-22, 2026-05-25, 2026-05-28` (the last `start+k*hop` that is `≤ 2026-05-28`; `k=9` gives `05-28`, `k=10` would give `05-31` which exceeds the cap, so stop at `k=9`).
  - Combined checkpoints (sorted, de-duplicated): `[05-01, 05-04, 05-07, 05-10, 05-13, 05-16, 05-19, 05-22, 05-25, 05-28, 05-29, 05-30, 05-31]` — 13 checkpoints.
  - Analysis pairs: `(05-01, 05-04), (05-04, 05-07), …, (05-25, 05-28), (05-28, 05-29), (05-29, 05-30), (05-30, 05-31)` — **12 pairs total** (one less than checkpoint count, by construction).

  **Per device, per pair:** find each device's run on the older date (`prior`) and the newer date (`recent`). Each device-pair becomes one diff. Conventions:
  - If a device has multiple runs on a checkpoint date, take the most recent of that day (matches "Use defaults" behavior for that date).
  - If a device has no run on a checkpoint date, that **single pair** is skipped for that device with a device-health note (`<device> no run on <date>` — do not silently substitute a nearby date). Other pairs for the same device continue.
  - If a device has no run on **any** checkpoint date, flag as device health "no runs in range `<start>..<end>`" and exclude entirely.

  **Coverage cap (sanity):** if the resulting **analysis-pair count** is > 19 (equivalent to checkpoint count > 20, since `pairs = checkpoints - 1`), warn the user before fetching with the pair count, the checkpoint count, and the implied download multiplier:

  > This range produces `<P>` analysis pairs per device (`<C>` checkpoints, `<D>` devices → `<P × D × 3>` log files to download). That's a large report. Increase hop or shrink range?

  Let them confirm-as-is or adjust. The warning uses the **pair count `<P>`** as the headline number throughout (not checkpoint count) so the threshold language stays consistent with the rest of the spec.
- **"Cancel and let me re-invoke"** — abort without fetching.

If the user picks specific dates: for each device, find the run whose `runIso` date matches `<newerDate>` and the one matching `<olderDate>`. If a device has no run on one of the picked dates, it gets a **device health flag** ("no run on <date>") and is excluded from diffing — never silently substituted with a nearby date.

If user adjusts (either "Pick specific dates" or "Pick date range"), re-display the resulting per-device pairs (now reflecting the chosen dates or resolved endpoints) and ask one final "Proceed?" before fetching.

### 4. Fetch logs

For each `(device, recent, prior)` triple, **download exactly three files** per run — nothing else from the run directory:

```bash
mkdir -p /tmp/ats-cache/<version>/<runDir>
curl -sS -o /tmp/ats-cache/<version>/<runDir>/failures.log    <ATS_BASE>/<version>/<runDir>/failures.log
curl -sS -o /tmp/ats-cache/<version>/<runDir>/unit-tests.log  <ATS_BASE>/<version>/<runDir>/unit-tests.log
curl -sS -o /tmp/ats-cache/<version>/<runDir>/metadata.js     <ATS_BASE>/<version>/<runDir>/metadata.js
```

`metadata.js` is JSON (despite the `.js` extension). Shape (field names only — never commit example values to this file; UID/IP/client are lab-identifying):
```
{
  "version":   "<full build version string>",
  "time":      "<ISO with offset>",
  "timestamp": "<ISO with offset>",
  "hostname":  "<device name>",
  "client":    "<client IP>",
  "uid":       "<device UID>",
  "ip":        "<device IP>"
}
```

Parse the **recent** run's `metadata.js` per device and surface `uid`, `ip`, `client`, and the full `version` string in the "Devices analyzed" table at the top of the report — these uniquely identify the physical ATS box and the exact build, which the run-dir name alone doesn't make easy to read.

If `failures.log` returns 404 (curl exits non-zero, or the file is HTML error page), the run had zero failures — handle gracefully. Same applies to `metadata.js` — if absent, skip the metadata enrichment for that row but keep the rest of the report intact.

From here on, parse the files on disk with `grep`/`awk`/`sed`/JSON parser — never `Read` `unit-tests.log` whole (see Context discipline).

**Zero-result detection**: if `unit-tests.log` is missing (404), empty (0 bytes), or contains no parseable test entries (no PASS/FAIL/SKIP lines), mark this run as a **zero-result run**. Skip diffing for this device's pair and add it to the device health findings instead — root-causing zero-result runs requires looking at the device-side test harness, not the source tree, so flag-and-stop is the right behavior.

### 5. Parse and diff

**Failures** (from `failures.log` — validated format):

- File header: `=== ATS Test Failure Report ===`, then `Generated: <iso>`, then `Total failures: <N>`.
- Entries separated by a line of 70 `=` characters.
- Per-entry first line: `[<N>/<Total>] <app-name> / <test_method>` — capture with `^\[(\d+)/(\d+)\] (\S+) / (\S+)$`. The pair `(app-name, test_method)` IS the diff key for failures.
- Per-entry fields, each on its own line followed by indented multi-line content (2 spaces): `Type:` (FAIL | ERROR | N/A), `Full name:` (e.g. `tests.test_captive_portal.CaptivePortalTests`), `Description:`, `Duration:`, `Time:` (`<startIso> -> <endIso>`), `Error:`, `Traceback:`, `UVM Logs:`.
- `UVM Logs:` either has real syslog lines or the literal `No UVM logs in this timeframe` — both are signal.
- Traceback contains source-path lines like `File "/usr/lib//python3/dist-packages/tests/test_<module>.py", line <N>, in <method>` — strip the `/usr/lib//python3/dist-packages/` prefix; the suffix (`tests/test_<module>.py:<N>`) is what to locate in the repo.

Build `recent.failures` and `prior.failures` as `{(app, test_method): {type, fullName, errorSummary, traceback, uvmLogs, tracebackPath, tracebackLine}}`. `errorSummary` = the **last non-empty line** of the `Error:` block (usually the assertion/exception line) — enough for the report; full block stays available if needed.

**Skipped tests** (from `unit-tests.log` — validated format):

- `unit-tests.log` is large (~2-3 MB typical). DO NOT read it whole. Use `grep -n "skipped '" <file>` to extract just skip lines and their line numbers.
- Skip line shapes:
  1. Inline: `test_<method> (tests.<module>.<ClassName>) ... skipped '<reason>'`
  2. Wrapped (description on its own line above): the line is just `... skipped '<reason>'` and the `test_<method> (tests.<mod>.<Class>)` line is 1-2 lines earlier.
- For each skip line, capture reason with `skipped '([^']+)'`. To resolve the test method + class, scan a small window above (e.g. `grep -n -B5` or back-walk to the nearest `test_<NAME> start \[` marker — every skipped block has one).
- Build `recent.skips` and `prior.skips` as `{testId: reason}` where `testId = tests.<module>.<class>.<method>` (this form is unique within and across modules and matches the qualified class shown in the log).

Skip categories worth distinguishing in the report (don't merge):
- **Code-disabled**: reason matches `disabling|disabled|TODO|hanging|fix` — likely a known broken test commented out, not an environment issue.
- **Environment**: reason mentions `interface|wan|wireless|machine|not on client|unreachable` — device/lab state, not a code issue.
- **Other**: anything else.

This categorization is best-effort and goes in the report next to the reason — it helps the user triage skip-reason changes quickly.

Per device, compute:
- **Regressions** = `recent.failures - prior.failures` (new failures today)
- **Resolved** = `prior.failures - recent.failures` (failures gone today)
- **Persistent failures** = `recent.failures ∩ prior.failures` (failed both runs)
- **New skips** = tests skipped in recent but not prior
- **Changed skip reasons** = same test skipped in both, different reason

### 6. Root-cause regressions — narrow to test-file modifications only

**Skip this entire step in multi-pair mode** ("Pick date range"). Date-range analysis is intentionally ATS-results-only — running per-pair git diffs across a multi-day range would inflate the runtime and the report without clear value, since the per-pair diff already shows which pair introduced the regression and the user can run a targeted single-pair analysis on that window afterward if they want git blame. Multi-pair reports therefore omit the "Source", "Test-file modifications", and per-regression repo-link fields from every per-pair section. The report's per-pair entries stop at the ATS-level fields (test ID, error signature, devices, UVM Logs, full traceback in appendix).

Steps 6 below applies only when the user picked "Use defaults" or "Pick specific dates".

For each version, the build SHAs are in the run paths (`recentSha`, `priorSha`).

```bash
# Run from the repo root (the skill's CWD when invoked).
git cat-file -e <sha>^{commit} 2>/dev/null || git fetch origin
```

Verify both SHAs are reachable. If still missing after fetch, report "SHA `<x>` not reachable in local repo — was the branch force-pushed or pruned?" and continue without git context for that pair.

For each regression test, find its source file. Tests live under:
- `uvm/hier/usr/lib/python3/dist-packages/tests/`
- Plus per-app paths like `<app>/hier/usr/lib/python3/dist-packages/tests/` (use `git ls-tree -r --name-only <recentSha>` to locate by filename if needed).

**Important — what to flag and what NOT to flag:**

The only commits worth surfacing as candidates are ones that **modify the test file itself** (the `.py` under `tests/`). A change to the test (assertion tightened, fixture reworked, timing shortened, helper renamed) directly explains why the same test now fails. Find them with:

```bash
git log --no-merges --oneline <priorSha>..<recentSha> -- <testFilePath>
```

Do **NOT** try to attribute failures to Java/scala/JS changes in the code-under-test. Any non-trivial commit could plausibly affect any test, and surfacing them produces a noise list, not a signal — the user will discount the section. If the test file itself wasn't modified in the range, just say so: "No modifications to the test file in this range — failure is from non-test code (Java/etc.) or an environment issue, not narrowable via git diff." That's a useful negative finding.

Also read the test source at the recent SHA so the report links to the right line:

```bash
git show <recentSha>:<testFilePath>
```

Include up to 3 test-file modifications per regression in the report, with their subject lines.

### 7. Build the report

**One report file per version**: `/tmp/ats-analysis-<version>-<YYYY-MM-DD>.md`. So if both master and released are analyzed, two files are written, with no cross-version mixing.

**Single-pair vs multi-pair output.** The "Use defaults" and "Pick specific dates" options produce ONE diff per device — the report has one top-level analysis section (1. Regressions, 2. Resolved, etc.). The "Pick date range" option produces MULTIPLE diffs per device (one per consecutive checkpoint pair) — the report has a top-level "Analyses by pair" header, then one `## Pair: <olderDate> → <newerDate>` sub-section per pair, each containing the full 1-5 analysis structure. Pair sub-sections render in chronological order (oldest pair first, most recent pair last) so the reader can scroll the timeline. Device health and the appendix stay at the top level (not per-pair); appendix entries are indexed `R-<pair>-<n>` to disambiguate.

**Organizing principle: finding-first, device-second.** Inside each section (or each pair sub-section, in multi-pair mode), group by test ID + error signature first, then list affected devices under each finding. If 3 devices all hit the same test with the exact same error message, that is **one entry** with `Devices: <device-A>, <device-B>, <device-C>` — not three near-duplicate entries.

#### Grouping rules

- **Regression grouping key**: `(testId, normalizedErrorSignature)`. `normalizedErrorSignature` = the failure's `errorSummary` (last meaningful line of the `Error:` block) with volatile substrings stripped: timestamps, IPs, ports, file/temp paths, numeric IDs. Two regressions group iff the keys match exactly.
- If same test fails across devices with **different** error signatures, keep them separate — surface each signature with its device list. Add a note `(same test, N distinct signatures)` so the reader knows.
- **Resolved grouping key**: just `testId` (the prior error doesn't have to match across devices). For each resolved test, classify as:
  - **Fixed on all eligible devices** = every device that ran BOTH prior and recent and had this failure prior, now passes. Lead with this; it's the "fix landed" signal.
  - **Fixed on some devices only** = partial. Explicitly list which devices still fail (cross-reference into Persistent).
- **Persistent grouping**: `testId` with same-or-different signatures grouped together; list devices.
- **Skip grouping**: same skip reason across devices → one entry with combined device list. Different reasons → separate entries.

#### Template

```markdown
# ATS Analysis — <version> (<branch>) — <date>

## Devices analyzed
| Device | UID | IP | Client | Build | Recent run | Prior run | Recent F/S | Prior F/S |
|---|---|---|---|---|---|---|---|---|
| `<device>` | `<uid>` | `<ip>` | `<client>` | `<version-full>` | <recentIso> | <priorIso> | <F>/<S> | <F>/<S> |

(The build SHA is embedded in the `<version-full>` string, so it's not duplicated as a separate column.)

## Summary
- Regressions: <N> failures across <D> devices (<U> unique signatures)
- Resolved: <N> tests now passing (<X> fixed on all eligible devices, <Y> partial)
- Persistent failures: <N>
- New skips: <N> | Changed skip reasons: <N>
- Device health issues: <N> (see Device health section)

## 1. Regressions — passed prior run, failing in recent run
*Grouped by (test, error signature). Same signature across devices = one entry.*

### `<app>/<test_method>` — `<one-line normalized error>`
- **Devices**: <comma-separated list>  *(or "all eligible devices: <list>")*
- **Source**: [<repo-relative-path>:<line>](<path>#L<line>) (read at `<recentSha>`)
- **Test-file modifications** in `<priorSha>..<recentSha>` (up to 3):
  - `<sha>` <subject>
  - *or:* "No modifications to the test file in this range — failure is from non-test code or environment."
- **UVM Logs** (up to 5 lines, most recent): `<one-line each>` or `No UVM logs in this timeframe`
- **Full traceback / error**: see appendix `R-<n>` at end of file

*(If same test has multiple distinct signatures, add a note above the first entry: "Same test, 2 distinct signatures across devices.")*

If no regressions, write: "**No regressions.** All tests passing in the recent run that also passed in the prior run."

## 2. Resolved — failed prior run, passing in recent run
*Confirms fixes landed. Grouped by test across all devices.*

### Fixed on all eligible devices
- `<app>/<test_method>` — was: `<prior error one-liner>`
  - Devices where fix confirmed: <list>

### Fixed on some devices only (partial)
- `<app>/<test_method>` — was: `<prior error one-liner>`
  - Now passing on: <list>
  - Still failing on: <list>  *(cross-reference Persistent or Regressions)*

If none, write: "No tests resolved since prior run."

## 3. Persistent failures — failed in both runs
*No change; included so they're not lost.*

- `<app>/<test_method>` — `<error one-liner>` *(devices: <list>)*

## 4. New skips — not skipped prior run, skipped in recent run
- `tests.<mod>.<class>.<method>` — reason: `'<reason>'` [<category>]
  - Devices: <list>

## 5. Changed skip reasons — skipped both runs, reason differs
- `tests.<mod>.<class>.<method>`
  - Prior: `'<old reason>'` [<category>]
  - Recent: `'<new reason>'` [<category>]
  - Devices: <list>

## Device health
- **Missing from latest cycle**: `<device>` — last run `<runIso>` (sha `<sha>`); expected on `<latestCycleDate>`
- **Zero-result run**: `<device>` `<runIso>` (sha `<sha>`) — `unit-tests.log` missing/empty; investigate harness
- **No prior run available**: `<device>` — recent counts: failed `<N>`, skipped `<N>`

If clean: "All expected devices reported results for the latest cycle."

## Appendix — full failure detail
For each regression, indexed `R-1`, `R-2`, ... (single-pair mode) or `R-<pair>-<n>` (multi-pair mode):
- Full `Error:` block
- Full `Traceback:`
- Full `UVM Logs:` block
```

The appendix exists so the **top of the report stays scannable** — a reviewer sees regressions and resolved at a glance, then drills down only if they need the raw error.

#### Multi-pair variant (Pick date range)

When the user picked "Pick date range" and the skill walked N checkpoints producing K consecutive pairs, the report keeps the same top-level shape but replaces sections 1-5 with a per-pair structure. Sections that stay top-level: `## Devices analyzed` (with a `Checkpoints analyzed` row showing all dates), `## Summary` (rolled-up counts AND per-pair counts), `## Device health`, `## Appendix`.

```markdown
# ATS Analysis — <version> (<branch>) — <date>

## Devices analyzed
| Device | UID | IP | Client | Build |
|---|---|---|---|---|
| `<device>` | `<uid>` | `<ip>` | `<client>` | `<version-full>` |

**Checkpoints analyzed** (range `<start>` → `<end>`, hop=`<H>`, recent-window=3):
`<date-1>, <date-2>, …, <date-K+1>` → <K> consecutive pairs

## Summary
- **Total regressions across all pairs**: <N> failures (<U> unique signatures)
- **Total resolved across all pairs**: <N>
- **New skips across all pairs**: <N> | Changed skip reasons: <N>
- **Device health issues**: <N> (see Device health section)
- **Per-pair counts**:
  | Pair | Regressions | Resolved | New skips | Changed skips |
  |---|---|---|---|---|
  | `<date-1>` → `<date-2>` | <N> | <N> | <N> | <N> |
  | … | | | | |

## Analyses by pair

### Pair: `<date-1>` → `<date-2>`
*(Same 1-5 structure as the single-pair template: Regressions, Resolved, Persistent, New skips, Changed skip reasons. **No "Source" or "Test-file modifications" fields** — git root-causing is intentionally skipped in multi-pair mode per Step 6. Each regression entry stops at: testId, normalized error signature, affected devices, UVM-log excerpt, and appendix reference. Appendix indices for this pair are `R-1-1, R-1-2, …`.)*

### Pair: `<date-2>` → `<date-3>`
*(…and so on for every consecutive pair, in chronological order — oldest first.)*

## Device health
*(Top-level, not per-pair. Includes range-specific findings: "no run on <date> for <device>" notes from the checkpoint resolution.)*

## Appendix — full failure detail
*(Indexed `R-<pairIndex>-<n>` so cross-references from per-pair sections resolve unambiguously.)*
```

The **summary table at the top** is what makes a multi-pair report useful — the reader scans the table to see WHICH pair introduced regressions, then jumps straight to that pair's sub-section. A monolithic chronological dump without the table reduces the report to a wall of text.

### 8. Inline summary

After writing files, print to chat — **one block per version**, finding-first (matching the report structure).

**Single-pair mode** ("Use defaults" / "Pick specific dates"):

```
ATS <version> (<branch>) — <date>
  Regressions:  <N> failures / <U> unique signatures across <D> devices
                - <app>/<test> on <devices>: <error one-liner>
                - <app>/<test> on <devices>: <error one-liner>
  Resolved:     <X> fixed on all eligible devices, <Y> partial
                - <app>/<test> (fix landed)
  Skip changes: <N> new, <N> reason-changed
  Device health: <N> issues  [<short summary>]
  Report:       /tmp/ats-analysis-<version>-<date>.md
```

**Multi-pair mode** ("Pick date range"): roll up totals first, then list per-pair regression counts so the reader can see which pair was the bad one. Don't expand every per-pair finding inline — that's what the report is for; the chat output stays a navigator.

```
ATS <version> (<branch>) — range <start>..<end>, hop=<H>, <K> pairs
  Totals across all pairs:
    Regressions:  <N> failures / <U> unique signatures
    Resolved:     <X> fixed
    Skip changes: <N> new, <N> reason-changed
    Device health: <N> issues  [<short summary>]
  Per pair (chronological):
    <date-1> → <date-2>: <R> regressions, <X> resolved
    <date-2> → <date-3>: <R> regressions, <X> resolved
    …
    <date-K> → <date-K+1>: <R> regressions, <X> resolved   ← most recent
  Report:       /tmp/ats-analysis-<version>-<date>.md
```

Lead the version block with the regression count (or total-regression count in multi-pair). If `Regressions: 0` (or `Totals: 0 regressions` in multi-pair), say so explicitly on the first line ("**No regressions.**") — that's the answer the user usually wants first, and burying it under counts wastes their first read.

In multi-pair mode, **also call out the pair-with-the-most-regressions** on its own line under the per-pair listing: `Worst pair: <date-A> → <date-B> (<R> regressions)`. That's the navigation hint the reader needs to drill into the right report section first.

**Tie-break for worst pair:** if two or more pairs tie on regression count, pick the **most recent** pair (highest `<date-B>`) — recency is what the user usually cares about. If pairs still tie after that (same recency on the newer endpoint, different older endpoints — unusual but possible after de-dup), pick the **smaller span** (the pair whose `(date-B - date-A)` is smallest). Document the chosen pair's selection with `(tie-break: most recent)` or `(tie-break: smallest span)` so the user knows the worst-pair line isn't arbitrary.

If both versions have zero regressions, the whole inline output collapses to one line per version plus a single `All clean.` headline.

## Common pitfalls

- **Don't switch branches.** Use SHA-based reads only. The user can be on any feature branch.
- **Don't compare across versions.** Master tests and release-branch tests can diverge; diffs are within-version only.
- **`MAJOR.MINOR` for release branch.** `17.5.2` → `ngfw-release-17.5`, not `ngfw-release-17.5.2`.
- **Latest "latest" detection.** Sort versions by semver, not lexically (`18.0.0 > 17.10.0 > 17.5.0`).
- **Date confirmation.** A device might be missing from today's runs (offline). Confirm dates per device with the user rather than silently skipping.
- **Empty failures.log.** A clean run may omit `failures.log` (404). Treat as zero failures, not an error.
- **Device health is a finding, not an error.** Missing devices, zero-result runs, and no-prior devices are expected and recurring — they belong in the Device health section of the report, never as exceptions or silent skips. Confirm the inline summary calls out the count too.

## Reference — validated data formats

Use these directly; they've been verified against real ATS output. Don't re-probe the HTML in every run.

### Versions index (`<ATS_BASE>/`)
Plain Apache autoindex. Extract version dirs with:
```bash
curl -sS <ATS_BASE>/ | grep -oE 'href="[0-9]+\.[0-9]+\.[0-9]+/"' | sed 's/href="//;s/\/"//' | sort -t. -k1,1n -k2,2n -k3,3n -r
```
Sort by semver descending, not lexically.

### Version run index (`<ATS_BASE>/<version>/`)
**HTML table**, not a plain autoindex. Each row:
```
<tr><td>DATE</td><td>TIME</td><td>DEVICE</td><td><a ... href='../<version>/<runDir>/test.log/../'>log files</a></td><td>PCT%</td><td>FAILED</td><td>SKIPPED</td></tr>
```
Use a Python script with a regex like:
```python
row_re = re.compile(
    r"<tr>\s*<td>(\d{4}-\d{2}-\d{2})</td>\s*<td>(\d{2}:\d{2}:\d{2})</td>\s*<td>([^<]+)</td>"
    r"\s*<td><a[^>]*href='\.\./[^/]+/([^']+?)/test\.log/\.\./?'[^>]*>[^<]*</a></td>"
    r"\s*<td>([0-9.]+)%</td>\s*<td>(\d+)</td>\s*<td>(\d+)</td>"
)
```
**Zero-result detection is available directly from the index** — rows with `0.0% / failed=0 / skipped=0` are zero-result runs; no need to download logs to detect this (saves bandwidth + context).

### Run directory filename
```
<version>.<buildTimestampUTC>Z?.<buildSha>-<rev><distro>_<runISO>_<device>
```
- `<buildTimestampUTC>` is `\d{8}T\d{6}` (sometimes followed by `Z`)
- `<distro>` is `bullseye` or `trixie` (mismatch with the host's distro causes zero-result runs)
- `<runISO>` is `\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+\-]\d{2}:\d{2}`
- Composite regex: `^(\d+\.\d+\.\d+)\.\d{8}T\d{6}Z?\.([0-9a-f]+)-\d+\w+_(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+\-]\d{2}:\d{2})_(.+)$` — captures `(version, sha, runIso, device)`.

### Per-run files (exactly these three, nothing else)
- `failures.log` — see Step 5 parsing rules
- `unit-tests.log` — see Step 5 parsing rules (grep-only, never Read whole)
- `metadata.js` — **JSON despite the extension**, shape: `{version, time, timestamp, hostname, client, uid, ip}`. Use `uid`+`ip`+`client` in the device table to uniquely identify the physical box and exact build.

### Inner-pytest failures (`Type: N/A`)
A subset of entries have `Type: N/A` and embed a full pytest traceback (with its own `===` separators and `FAIL:` / `Ran N test` / `FAILED (failures=1)` markers) inside the `Error:` field — there's no separate `Traceback:` field. Two consequences:
- `errorSummary` heuristic must prefer the **last `*Error: ...` exception line** in the Error block, not the literal last non-empty line (which is `test_NAME end [ts]` noise).
- `tracebackPath:line` extraction must fall back to scanning the Error block when the dedicated `Traceback:` field is empty.

Validated regex for the exception-line preference:
```python
exc_re = re.compile(r'^\s*([A-Za-z_][A-Za-z0-9_]*Error|Exception|Failure)(\s*:|\s*$)')
noise_re = re.compile(r'(end\s+\[|^FAIL$|^FAILED\b|^OK\b|^Ran \d+ test|^-{3,}|^={3,})')
```

### Skip categorization heuristics (validated)
- `code-disabled`: `disabl|todo|hanging|need to fix|review changes`
- `environment`: `interface|wan|wireless|machine|not on client|unreachable|requires|already in use`
- Anything else: `other`

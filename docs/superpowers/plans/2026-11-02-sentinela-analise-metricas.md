# Sentinela Analysis and Metrics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a manual, local-only Sentinela report analyser with Windows host metrics and an actionable dashboard.

**Architecture:** Keep `scripts/test_lab.py` as the only Gradle runner. Add a standard-library Python analyser that reads immutable run directories and writes a new analysis directory. Extend the runner with optional Windows sampling during each suite; unavailable GPU data remains explicit rather than guessed.

**Tech Stack:** Python 3.9 standard library, Windows PowerShell, existing Sentinela JSON/log/XML artifacts.

**Spec:** `docs/superpowers/specs/2026-11-02-sentinela-observatorio-design.md`

## Global Constraints

- Manual execution only; no login task, scheduler, CI change, network dependency, or Gradle invocation by the analyser.
- Preserve existing run evidence. Write analysis output only below `reports/sentinel-analysis/`.
- Java batteries remain suspended by author order. Use Python unit tests, syntax checks, and `--dry-run` only.
- GPU metrics are optional and must report unavailable hardware/tooling without failing a suite.
- Every changed Sentinela capability updates `Sentinela/TUTORIAL.md` and root `README.md`.

## Review Focus

- A malformed historical `summary.json` must be reported and skipped, not abort all analysis.
- A `RUNNING` report without a live process must be classified as incomplete, not a regression.
- A compile failure shared by several suites must be grouped into one root-cause signature.
- No XML artifact must yield an evidence-gap warning, not a fictional test-case count.
- A machine without NVIDIA tooling must retain CPU/RAM metrics and label GPU unavailable.

---

### Task 1: Analyse persisted run evidence

**Files:**
- Create: `scripts/sentinel_analysis.py`
- Test: `tests/test_sentinel_analysis.py`

**Interfaces:**
- Consumes: `analyse_reports(runs_root: Path, output_root: Path) -> dict`
- Produces: `analysis.json`, `analysis.md`, and `dashboard.html` below a timestamped directory.

- [ ] **Step 1: Write failing Python tests**

Cover a compile failure repeated across suites, a later passing suite, missing XML, malformed JSON, and stale `RUNNING` evidence.

- [ ] **Step 2: Run focused Python test to verify it fails**

Run: `py -3 -m unittest tests.test_sentinel_analysis`

Expected: FAIL because `scripts/sentinel_analysis.py` does not exist.

- [ ] **Step 3: Implement `analyse_reports(runs_root: Path, output_root: Path) -> dict`**

Read `summary.json`, suite logs, copied XML, and run metadata. Classify evidence into compilation, test, infrastructure, incomplete, unstable, and evidence-gap findings. Deduplicate normalized signatures and note supersession only when a later equivalent suite passed.

- [ ] **Step 4: Run focused Python test to verify it passes**

Run: `py -3 -m unittest tests.test_sentinel_analysis`

Expected: PASS.

### Task 2: Capture optional local metrics

**Files:**
- Modify: `scripts/test_lab.py`
- Modify: `tests/test_test_lab.py`

**Interfaces:**
- Consumes: suite start/end events from `run_suite(...)`.
- Produces: `metrics.json` per run, with suite CPU/RAM samples and optional GPU availability.

- [ ] **Step 1: Write a failing Python test**

Assert that an unavailable GPU collector produces `available: false` and does not affect suite status.

- [ ] **Step 2: Run focused Python test to verify it fails**

Run: `py -3 -m unittest tests.test_test_lab`

Expected: FAIL because the metrics collector does not exist.

- [ ] **Step 3: Implement minimal optional sampler**

Sample Windows CPU/RAM through PowerShell while a suite runs. Query `nvidia-smi` only when available. Persist samples and aggregate per-suite peak/average values; collector errors become metadata warnings.

- [ ] **Step 4: Run focused Python test to verify it passes**

Run: `py -3 -m unittest tests.test_test_lab`

Expected: PASS.

### Task 3: Expose manual command and document it

**Files:**
- Modify: `Sentinela/sentinela_gui.py`
- Modify: `Sentinela/TUTORIAL.md`
- Modify: `Sentinela/README.md`
- Modify: `README.md`
- Modify: `STATE.md`
- Modify: `CHANGELOG.md`
- Modify: `TODO.md`
- Modify: `docs/archive/technical/Development-Log.md`

**Interfaces:**
- Consumes: `scripts/sentinel_analysis.py` CLI.
- Produces: a GUI button and documented conversational keyword `analisar Sentinela Local`.

- [ ] **Step 1: Write a failing GUI command-construction test**

Assert the UI invokes only the analyser command and does not call Gradle.

- [ ] **Step 2: Run focused Python test to verify it fails**

Run: `py -3 -m unittest tests.test_sentinel_analysis tests.test_test_lab`

Expected: FAIL before the GUI wiring exists.

- [ ] **Step 3: Add the smallest GUI action and documentation**

Open the generated analysis directory after successful analysis. Document sources, categories, metrics limits, command, and that running tests remains separate.

- [ ] **Step 4: Run static and focused checks**

Run: `py -3 -m py_compile scripts/sentinel_analysis.py scripts/test_lab.py Sentinela/sentinela_gui.py`

Run: `py -3 -m unittest tests.test_sentinel_analysis tests.test_test_lab`

Expected: PASS; no Gradle process started.

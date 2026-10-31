# AGENTS.md — Codex no Village Colony

`CLAUDE.md` é a fonte canônica do projeto. Este arquivo concentra apenas o fluxo do Codex e as regras que ele deve aplicar neste repositório; em conflito, `CLAUDE.md` vence.

## Contexto mínimo

1. Leia `STATE.md` — somente as primeiras 60 linhas.
2. Leia `CLAUDE.md` e use `docs/PATTERNS.md` na seção correspondente ao sintoma.
3. Consulte a ADR ou o documento do subsistema que será alterado.

Não leia documentos históricos inteiros. Use `rg` para localizar trechos em `docs/archive/technical/` (`Development-Log.md`, `Project-State.md`, `Backlog.md`). Em `TODO.md`, leia o topo (100 linhas) e pesquise o item específico.

## Antes de alterar

- **Uma linha por vez** (ADR-035 §2): branch nova a partir da `main` atualizada, de volta por PR. Se outra branch viva toca o mesmo sistema, pare e pergunte ao autor.

- Identifique o problema, o sistema responsável, os arquivos envolvidos e se há decisão arquitetural.
- Se a mudança exigir decisão não coberta por ADR, pare e peça deliberação; não invente arquitetura.
- Antes de editar código, rode `./gradlew build` (Windows: `./gradlew.bat build`). Se a linha de base falhar, registre e pare.
- Para defeito, reproduza primeiro com teste que falhe pela causa correta; depois implemente e rode o teste novamente.

## Invariantes

- Mod Fabric para Minecraft 1.21.1, Java 21. Dependências têm versões fixas; nunca use `latest`, `+` ou SNAPSHOT.
- `core/` não importa Minecraft, Fabric, `data/` nem outros domínios do core. A exceção entre domínios é `core/coordination`.
- O mundo é a fonte da verdade: não persista estado que o Minecraft já guarda. Recurso sem origem física só nos casos aceitos pela ADR-028 e pela ADR-034 (lava nunca).
- Comentário diz a regra em vigor e cita a fonte (Regra N, ADR-NNN, KF-NNN); a história fica no commit e no ADR. Mudou uma regra? Corrija no mesmo commit todo comentário que a descreve (ADR-035 §6).
- Nunca use `@Overwrite`. Mixin novo exige ADR; mixins delegam a lógica e não cancelam IA Vanilla.
- Prefira uma responsabilidade por classe. Arquivos acima de 500 linhas exigem justificativa e divisão por responsabilidade quando apropriado.
- Código e identificadores em inglês; documentação e Javadoc em português.

## Verificação e conclusão

- `./gradlew test`: unitários; `./gradlew runGametest`: testes Fabric no servidor.
- Alterações em `fabric/` exigem GameTests. Teste verde não substitui evidência em jogo; declare explicitamente o que ainda depende do playtest do autor.
- Teste novo deve demonstrar a falha antes da correção. Rode os testes proporcionais ao escopo e relate exatamente o que foi executado.
- Atualize `STATE.md` quando o estado vivo mudar, `TODO.md` quando uma pendência abrir/fechar e `docs/archive/technical/Development-Log.md` para registrar uma sessão de implementação. Decisão arquitetural nova também exige ADR.
- Cada commit deve conter uma tarefa. Formato: `P0.x: descrição (teste que prova)`. O JAR fica fora do git (ADR-036 item 26): `downloads/` é cópia local, e a versão publicada vai para a página de versões do GitHub.

## Skills

- Código Fabric/aldeões: use `fabric-development` e, conforme o escopo, `minecraft-villager-systems`, `minecraft-code-research` e `minecraft-testing`.
- CI/CD e release: siga `.github/workflows/` (a skill `minecraft-ci-release` está em `docs/archive/skills/`) e mantenha a classificação **categoria 1.9** nas auditorias relacionadas.
- Carregue skills sob demanda; não abra instruções de áreas que a tarefa não toca.

<!-- fabric:bootstrap:begin -->
# Fabric Bootstrap

This project uses Fabric to manage cross-client AI knowledge and behavior rules. This file is synced into the managed block on both clients by `fabric install` — **do not hand-edit the block on any client**; edit here + re-run `fabric install`.

## For Developers

This file is the **AI client's policy & convention config**, not dev onboarding. You don't need to read the Self-archive / Cite / Phase 0.4 details.
As a dev you only need to: run `fabric install` once per repo, use `fabric store bind <alias>` / `fabric store switch-write <alias>` to wire up a write store, and run `fabric doctor` when something breaks.
**Never hand-edit `.fabric/agents.meta.json`** — derived state is rebuilt by the engine.

## Dev Quickstart

**What Fabric is**: a cross-client (Claude Code / Codex CLI) AI knowledge layer. Store the team/project **decisions / pitfalls / guidelines / models / processes** as markdown; hooks surface them to the AI automatically so it doesn't re-learn every time.

**What you DO** vs **what the engine does (DON'T hand-edit)**:

| You DO | You DON'T |
| --- | --- |
| Run `fabric install` once per repo | Hand-edit `.fabric/agents.meta.json` |
| Run `fabric doctor` (--fix self-heals) on trouble | Hand-edit `.cjs` under `.claude/hooks/` |
| Use `fabric-archive` / `fabric-review` / `fabric store ...` to manage store-backed knowledge | Hand-write any non-store knowledge root |
| `npm install -g @fenglimg/fabric-cli@latest` to upgrade | Memorize the 35 doctor lint codes |

**4-step loop**: `fabric install` (once) → bind and pick a write store → AI works normally (hook on session start + edit) → the AI writes pending entries into the current write store via MCP and returns a `pending_path` → review with the `fabric-review` skill.

**Real example**: a sprite black-edge root cause was the `atlas.premultiplyAlpha` flag being inverted — once archived into the store's `knowledge/pitfalls/`, the AI auto-references it next time a similar issue shows up.

See `docs/USER-QUICKSTART.md` for the full maintainer version.

## Behavior Rules
- **Pre-action gating (MUST before Edit/Write)**: (1) call `fab_recall(paths=[<file-being-edited>], session_id=<client-session-id>)`; (2) optionally native-Read selected `entries[].read_path` bodies (PostToolUse records `knowledge_body_read`); (3) when skipping a hit, say `dismissed: <id> (<reason>)`. `fab_recall` no longer delivers bodies; lean default is description+index discovery (KT-GLD-0005). PreToolUse is soft nudge only per KT-DEC-0007 (never permanent gate / no `decision:block`).
- **Archive-as-truth**: the **only** path into canonical is `fab_propose` → pending → `fabric-review`/`fab_review` approve; never auto-promote chat/logs into canonical.
- **Never hand-edit `.fabric/agents.meta.json`** (co-location index retired; knowledge lives in mounted stores). For anomalies run `fabric doctor` / `fabric doctor --fix` on fixable state.

## Knowledge Base (KB)
- **Discovery**: the SessionStart hook lists broad-scoped entries (scoped by `semantic_scope` across three tiers: `team` team-wide / `project:<id>` this-project-only (surfaces only in repos bound to that project) / `personal` `KP-*`, all three referenced the same way); editing a file may trigger a narrow hint via the PreToolUse hook.
- **Scope's 3 axes (why something isn't surfacing)** (KT-MOD-0001): whether an entry surfaces is decided by three **orthogonal** axes — ① `semantic_scope` audience (`team` / `project:<id>` / `personal`; the wrong project binding hides it) ② `relevance_scope` timing (`broad` always-on / `narrow` only when you edit a matching file) ③ `store` physical lib (not read without `fabric store bind`). The axis names collide ("team" is both an audience value and a possible store alias), so when puzzled about "why isn't this surfacing" run `fabric audit why-not-surfaced <id>` for a per-cause diagnosis (store unbound / scope mismatch / narrow timing).
- **Usage**: go one-step `fab_recall(paths=[...])` to fetch the relevant KB descriptions + read paths in one call; when you need a body, do a native Read of its `entries[].read_path` (no second MCP round-trip for the body).
- **session_id**: when calling `fab_recall`, always pass the current client session id as the `session_id` argument (Claude Code's session id is in the stdin payload; Codex's corresponding identifier likewise). This lets `fabric doctor --archive-history` and the `fabric-hint.cjs` Stop hook track cross-session debt accurately.
- **Skills (6)**: write flow `fabric-archive` (with source-mode cold-start backfill from git/docs) / `fabric-review` (with retire-deprecation + relate-edge sub-flows); retrieval usage `fabric-recall-playbook` (when/how to recall, lazy body Read, failure paths); config check-up and conversational tuning `fabric-config`; store ops `fabric-store` / `fabric-sync`.
- **Language**: rendered per the `language` field in `~/.fabric/fabric-global.json` (machine-wide tone).
- **Archive cadence nudge** (rc.36 / finish→archive): after a significant decision lands or an edit batch reaches config `archive_edit_threshold` (default 20), lightly self-trigger `fabric-archive` at a suitable turn (max 1 per turn; not a task engine / not Stop-hook flood). Stop hook remains a soft threshold nudge per KT-DEC-0007 — without an archive cadence the KB slowly dies.
- **Finish ritual** (W3 finish-gate): when a stretch of work closes — the user says "done / ship it / commit", or you have just cleared the last gate and are about to report — **explicitly run the archive judgement and state its verdict**, where "nothing worth archiving here" is a valid verdict (give a one-line reason). ORed with the cadence nudge above, not a replacement: cadence covers "enough has changed", the ritual covers "this thing is finished". A prompt-layer ritual, **not a hook gate** (KT-DEC-0007) — the judgement itself is free; the real cost is forgetting to make it.
- **Review backlog nudge** (rc.36): to judge the pending backlog, go through `fab_pending action="list"` or the `pending_path` returned by `fabric-review`; don't glob the project-local `.fabric/knowledge/pending`. Two independent signals, either one is enough to proactively propose the `fabric-review` skill at a suitable turn: ① **volume** — visible pending > 10 (`review_hint_pending_count`); ② **age** — the oldest entry has sat ≥ 7 days (`review_hint_pending_age_days`). Volume alone misses the "only three, but one has been rotting for a month" flavour of draft deadlock.

## Self-archive policy (v2.2 C1: lean spec)

When a clear archival signal appears, autonomously invoke the `fabric-archive` skill at the end of that turn (AI self-trigger entry E3):

- **Trigger** (either): ① **User-driven normative** — the user says `以后` / `always` / `never` / `下次` / `记一下`, or locks a direction with rationale after weighing ≥2 candidates, or rejects a suggestion and states a reason (the reason is knowledge); ② **Wrong-turn-and-revert** — you tried path X, then after reflection switched to path Y (a two-step "negate + replace" structure, not mere exploratory failure).
- **Does NOT trigger**: pure user questions / simple refactor·typo / a baseless "I learned something" insight.
- **Anti-loop**: at most 1 self-invocation per turn; no repeat for the same outcome in the same session; the skill's Phase 2.5 viability gate is the backstop.
- **Receipt (marker-free)**: just invoke the `fabric-archive` skill directly — no marker string to print: the skill routes an AI self-invocation to E3 by default (deterministic else-branch, no longer dependent on the AI emitting an exact string). The skill returns `pending_path` after writing pending — reply `undo` if it shouldn't be recorded (I'll call fab_review reject).

## Cite policy (v2.2 C1: recall auto-accounting, zero first-line burden)

- **Core (recall-first auto-accounting)**: before changing any file, run `fab_recall(paths=[<file-being-edited>])` first. The system auto-accounts the recalled KB as that edit's citation by "paths recall-hit this session overlap the edit target" —— **no hand-written first reply line needed** (C1 removes the first-line `KB:` contract boilerplate: think-then-speak, recall is the real signal that a citation happened). The PreToolUse hook gives a soft nudge when it detects no relevant recall (nudge, not a gate, per KT-DEC-0007).
- **The only time to speak up (dismissed / override)**: when you judge a recalled KB should NOT apply, say one line `dismissed: <id> (<reason>)`; reason enum `scope-mismatch | outdated | not-applicable | other:<text>`. For precise annotation you may still use a first-line `KB: <id> [applied|dismissed]` (parser retained, backward compatible).
- **`[applied]` verification duty**: citing any id (auto or hand-written) requires first fetching the KB via fab_recall (a native Read of its body path when needed) to prevent fabricated ids. Verification failing = you cannot cite.
- **Audit & full spec**: `fabric audit cite` reports coverage (does not block your work, only records); the full spec (contract operators / store prefix / skip·dismissed dictionaries / type routing / adjudication ladder) lives authoritatively in the `fabric-review` skill's `ref/cite-contract.md` —— bootstrap keeps only the executable core.

<!-- fabric:bootstrap:end -->

> **HISTÓRICO** — arquivado em 2026-10-06 (ADR-036, item 24). Não descreve o estado atual do mod; o estado vivo está em `STATE.md` e as regras em `docs/RULES.md`.

# Documentos históricos

Planos e modelos do início do projeto (agosto de 2026), arquivados em
2026-10-02 sem edição. **Não descrevem o código atual:** citam classes que
nunca existiram (`ColonyManager`, `TaskManager`, `BuildingStatus`,
`ResourceRegistry`…) ou fases já concluídas. Ver a análise em
[`docs/technical/Revisao-2026-09-17.md`](../technical/Revisao-2026-09-17.md) §2.

| Documento | O que era | O que vale hoje |
|---|---|---|
| `Class-Architecture.md` | Classes planejadas | ADR-006 (layout de pacotes) e o código |
| `Fabric-Implementation-Plan.md` | Plano da camada Fabric | ADR-006 e `src/main/java/com/villagecolony/fabric` |
| `Data-Model.md` | Modelos e campos | `core.*.model` e ADR-005 |
| `MVP.md` | Escopo do MVP | MVP concluído; estado em `STATE.md` |
| `MVP-Tasks.md` | Tarefas do MVP | `TODO.md` |
| `START_PROJECT.md` | Prompt da primeira sessão | `CLAUDE.md` |
| `Development-Roadmap.md` | Roteiro de 28-08 | `TODO.md` e `STATE.md` |

Os comentários de código e as ADRs que citam esses nomes continuam válidos
como referência histórica: o arquivo está aqui.

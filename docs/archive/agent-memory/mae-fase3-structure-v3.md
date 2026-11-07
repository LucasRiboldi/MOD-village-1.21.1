---
name: mae-fase3-structure-v3
description: "MAE Fase 3 retomada do handoff Codex em 2026-11-04; writer structure_v3 commitado 6f4d52d2 sem push, conclusão pendente"
metadata:
  node_type: memory
  type: project
  modified: 2026-11-04T13:54:57.908Z
---

Linha MAE Fase 3 vive na branch `codex/mae-structure-access`, worktree
`C:\Users\lucas\.codex\worktrees\sentinela-analytics\MOD-village-1.21.1` (não na main).
HANDOFF.md, RULE-/TOOLS-RECONCILIATION.md só existem lá.

Estado em 2026-11-04: autor aprovou P1–P4 do `structure_v3` (ADR-046: envelope v3,
material exato, cursor estrito, retirada antes da escrita). Writer (6f4d52d2) e tarefa 7
(fd3d8026: built → prova de acesso real → `complete` + `BUILDINGS.registerOrMerge`; BuildClosing
não é chamado por exigir projeto vivo). Lacunas fechadas (013d6e76); casa/oficina/curral/depósito reais concluídos em GameTest (369626b8). `MaeStructureAccess.beginFor` deriva circulação/destino (492b34aa); `MaeStructureSite` liga terreno → acesso → obra, terreno plano aceito por prova sem mudança (escolha do autor) (7883b3da); 4 modelos reais pelo fluxo nas 4 rotações (3fc5b188). Push feito em 2026-11-04 até ab6adaf7; sem PR para a main.
Verificado: 105/105 GameTests `-PgametestOnly=Mae`, 23 unitários (arquitetura + MaeWorkStoreTest).
Autor liberou testes focados para esta etapa.

**Why:** modo §0.2.2 exige "ok" de modelo a cada etapa; ADR-046 é a fonte do contrato.
**How to apply:** falta escolha de lote/origem real e comando/chamador supervisionado em jogo; vaso com planta, curral
grande (água/arado/trigo) e payload (taiga/savanna mason/snowy mason) recusados no contrato 1; ativação automática segue bloqueada por carga real + playtest da Fase 2. Pedir "ok" de modelo.
Armadilha: GameTest que chama `runSlice` no mesmo tick de outra fatia esbarra no orçamento
global (4 escritas/2 ms) — esperar um tick. Recusa em GameTest via `context.assertTrue(false, …)`, nunca `AssertionError` em callback (derruba o servidor). Ver [[commit-so-com-teste-verde]].

Decisões do autor (2026-11-04) para a próxima etapa: comando em jogo `/vc mae obra <modelo>` com
lote/origem pela posição do jogador (rotação pela direção do olhar) **e** argumento opcional de busca
automática de lote; PR #52 integrado em 2026-11-04 (merge 746f9d8f); próxima etapa em branch nova da main atualizada.

2026-11-04 tarde: comando `/vc mae obra|estado|parar` (MaeCommand + MaeSites) commitado em
`claude/mae-obra-comando` (f26e7f08) no checkout Desktop, a partir da main 746f9d8f; 1103/1103
GameTests locais. **Sem push nem PR** (aguarda pedido). Não exercitado em jogo; versão segue 0.4.16.

2026-11-04 noite, branch `claude/mae-obra-comando` (checkout Desktop, **sem push**), fim do plano MAE:
item 1 rua 505288ed; item 2 vaso f45a5880; item 3 curral grande (água/arado/trigo moldados, água antes
das vizinhas) fe327e3a; item 4a placa/baú/barril/fornalha vazios 133d963a; item 4b storage_majest recriado
com porta térrea (6 arquivos, CSV atualizado) 0828ebd1. Bateria completa 1108/1108 a cada item.
Item 5 pastor anda pelo portão (sem teleporte, WALK_LIMIT 200) 8965a845; item 6 mobília provada 23d0033f.
Plano MAE fechado em `claude/mae-obra-comando`, candidata 0.4.17: push + PR #53; CI corrigido (5eda7cc5: storage_variants.py gera os depósitos, _door em SAME_SHAPE; pastor redireciona navegação); JAR DA497943… instalado no TLauncher (0.4.16 .disabled), não jogado.
Restam só gates do autor: playtest, carga real, fila automática, Fase 4 (grafo). Depois: correções do mod.
Decisões do autor: água fica na altura do solo (abaixo não umedece); contract=2 para extra/shaped.
Armadilha: storage_majest de estilo saem de scripts/storage_variants.py (nbtlib 2.0.4 do CI); editar só o genérico e rodar o script.
PR #53 integrado na main em 2026-11-04 (merge 7e37095b). Próximo: playtest da 0.4.17 e correções do mod.

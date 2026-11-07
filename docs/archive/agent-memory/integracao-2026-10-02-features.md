---
name: integracao-2026-10-02-features
description: "consolidação de 02-10 (35dc1cee) — features, Regra 48 e JAR AB762691 em codex/bighousemod; PR #3 para a main e limpeza de branches pendentes"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-03T01:53:25.620Z
---

Em 2026-10-02 tudo foi consolidado em `35dc1cee`, já em `origin/codex/bighousemod` (PR #8 mesclado
pelo avanço): linha principal + `feature/mod-menu-settings` + `feature/profession-overlays` + o
trabalho sem commit do worktree inspiring-torvalds (GroundPickup/Regra 48, B-4 e B-5 parciais).
Verificado: 1293 unitários, 88 Python, 564/564 GameTests em duas rodadas, mutação do
GroundPickupGameTest. JAR SHA-256 AB762691…9FFA em downloads/ e .minecraft/mods. **Nada visto em jogo.**

PR #3 mesclado em 02-10 (`6022021e`); `main` = `codex/bighousemod` = árvore testada de 35dc1cee.
Worktree inspiring-torvalds removido. **Pendente com o autor** (o classificador do auto mode negou
apagar branches duas vezes, mesmo com aval): branches feature/*, claude/sync-local-github-80cc8a,
claude/gallant-rubin-9fwxlw, claude/analise-atualizacoes-projeto-9b8886 e
claude/inspiring-torvalds-bdc8c3 (08-19; a correção dele já existe em TreeShape.isNaturalLeaf).

Decisões do merge: a fundação NÃO segue a ordem da política (o carpinteiro logo depois do
lenhador é decisão do autor de 30-09); cotas em `HiringQuota`; ADR das políticas = ADR-030.

**Why:** a feature reordenava a fundação pela ordem padrão da política e desfazia o carpinteiro titular.
**How to apply:** funcionalidade nova parte da `main` depois do PR #3. ArchUnit recongelado
pelo procedimento de [[climb-out-nunca-desiste]].

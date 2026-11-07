---
name: decisoes-adr-036-andamento
description: ADR-036 aplicada e na main (PRs #9, #10, #11 juntados 06-10); v0.3.1-alpha publicada; falta playtest e revisão dos conflitos C1-C8
metadata:
  type: project
---

**Estado em 2026-10-06, 23h:** todos os itens de código e arrumação da ADR-036 feitos e enviados,
um commit por item (tabela "Estado" da ADR tem os hashes). PR #10 (base main) aberto, depende do
PR #9 (integra → main). JAR 0.3.1 = `278086C5…0673` em build/libs, downloads/ (local, fora do git)
e .minecraft/mods. Versão `v0.3.1-alpha` criada como **rascunho** no GitHub (publicar é do autor).

**Depois (06-10, noite):** #9, #10 e #11 juntados na main (main = 26b8e02f, 627/627 GameTests);
branches apagadas, ficam main e codex/bighousemod (+ branch local do worktree do Codex);
v0.3.1-alpha publicada como pré-lançamento com o JAR 278086C5…0673.
**Falta (com o autor):** decidir os conflitos C1–C8 da ADR-036 (inclui limiares da felicidade,
item 20); playtest listado no STATE.md.

**Verificado:** 1348 unitários, 627 GameTests (intermitentes: KF-003 escadas, fundidor, FarmPlan),
95 Python, PIT 86%. Nada visto em jogo.

**Lições da sessão:** depositor tem teto de 3 compartimentos — cenário que entope baú usa
TestChests.fillEmptySlots; GameTest de navegação precisa de chão de pedra e navigation.stop()
antes de findPathTo; nunca editar arquivo com `powershell` 5.1 (BOM e acento estragado) — usar
Python; dois nextTarget de mina com UUIDs diferentes disputam o mesmo ramal.

**Why:** pedido do autor de aplicar tudo e reanalisar. **How to apply:** próxima sessão começa
pelo playtest e pelas respostas do autor aos conflitos C1–C9.

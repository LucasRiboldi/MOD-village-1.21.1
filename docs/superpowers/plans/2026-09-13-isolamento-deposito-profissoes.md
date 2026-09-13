# Isolamento dos baús de produção Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Depositar a produção de cada profissão no baú do trabalhador que a produziu, sem impedir a retirada compartilhada de insumos pela colônia.

**Architecture:** Preservar `ColonyChests.nearestFirst` e as retiradas compartilhadas. Alterar apenas destinos de produção do mineiro, lenhador, fundidor, carpinteiro e pedreiro; o minério mantém a prioridade do baú da boca da mina conforme Regra 30. Acrescentar GameTests de integração que mantenham um segundo baú profissional como controle negativo.

**Tech Stack:** Fabric GameTest, Minecraft 1.21.1, Java 21, Gradle wrapper.

**Spec:** `docs/decisions/ADR-001-Core-Principles.md` §7 e `docs/technical/Plano-de-Correcao.md` P0.5.

## Global Constraints

- Core nunca importa `net.minecraft.*`, `net.fabricmc.*`, `fabric.*` nem `data.*`.
- Nunca usar `@Overwrite` em mixin.
- Não persistir dados que o mundo já guarda.
- Rodar `./gradlew build` antes das mudanças e `./gradlew runGametest` após tocar na camada Fabric.
- Atualizar `STATE.md`, `TODO.md` e `docs/technical/Development-Log.md` para mudança de comportamento.
- Parar após este lote para revisão do autor; mineração e construção são lote posterior.

---

### Task 1: Testes de isolamento por profissão

**Files:**
- Modify: `src/gametest/java/com/villagecolony/gametest/MinerOverflowGameTest.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/LumberjackGameTest.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/SmelterGameTest.java`
- Modify: `src/gametest/java/com/villagecolony/gametest/CraftingGameTest.java`

- [x] Criar testes em que insumo pode estar em baú de outro trabalhador, mas saída manufaturada deve aparecer apenas no baú do produtor.
- [x] Executar cada teste e confirmar falha específica antes da correção.

### Task 2: Destinos de produção

**Files:**
- Modify: `src/main/java/com/villagecolony/fabric/work/MinerHaul.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/TreeChoice.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/TreeFelling.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/SmelterWork.java`
- Modify: `src/main/java/com/villagecolony/fabric/work/CraftingWork.java`

- [x] Direcionar saída produzida para o baú pessoal; em transformações, devolver matéria-prima ao baú de origem quando a saída não couber.
- [x] Preservar baú da boca da mina para minério e manter insumos consultados/retirados em todos os baús da colônia.
- [x] Rodar testes focados, `./gradlew runGametest` e `./gradlew build`.

### Task 3: Registro vivo e artefato

**Files:**
- Modify: `STATE.md`
- Modify: `TODO.md`
- Modify: `docs/technical/Plano-de-Correcao.md`
- Modify: `docs/technical/Development-Log.md`

- [x] Registrar o resultado como aguardando validação em jogo, sem declarar as profissões corrigidas com base apenas em testes.
- [x] Atualizar o JAR de distribuição e o JAR do launcher apenas depois dos testes, preservando arquivos locais existentes até a cópia nova estar validada.
- [x] Parar para revisão antes do lote de mineração/construção.

# ADR-004-Mixin-Policy.md

# Architecture Decision Record 004

# Village Colony Mixin Policy

**Status:** Accepted
**Date:** 2026-08-06
**Accepted:** 2026-08-06
**Decision Type:** Architecture / Vanilla Integration
**Blocks:** Phase 9 onwards (Lumberjack, Manufacturer, Builder)

---

# 1. Context

Nenhum dos documentos originais menciona **Mixin**.

Isso é uma omissão arquitetural, não um detalhe de implementação.

---

## O problema

`PROJECT_CONSTITUTION.md §4` exige preservar a IA Vanilla e diz que o mod
"estende o comportamento".

Mas o MVP exige que aldeões:

* encontrem uma árvore e **quebrem** blocos de log;
* **coloquem** blocos de construção;
* retornem à casa e **depositem** em um baú.

Nenhum desses comportamentos existe em `VillagerEntity`.

---

## Por que não é trivial

O aldeão em 1.21.1 **não usa `Goal`**.

Ele usa:

```text
Brain

├── Activity
├── Schedule
├── MemoryModuleType
└── Task / MultiTickTask
```

Para adicionar comportamento é necessário injetar tasks no Brain.

**A Fabric API não expõe API pública para isso.**

Não existe evento, registro ou hook oficial.

---

## Conclusão

Mixin é obrigatório.

Portanto precisa de política.

---

# 2. Decision

Usar Mixin, com escopo **mínimo e declarado**.

---

# 3. Superfície de Mixin permitida

Apenas as classes abaixo.

Qualquer adição exige nova ADR.

---

## Mixin 1 — VillagerEntityMixin

Alvo:

```text
net.minecraft.entity.passive.VillagerEntity
```

Método:

```text
initBrain
```

Tipo:

```text
@Inject(at = @At("TAIL"))
```

Objetivo:

Registrar a `Activity` da colônia no Brain.

---

## Mixin 2 — VillagerEntityMixin (mesma classe)

Método:

```text
onDeath
```

Tipo:

```text
@Inject(at = @At("HEAD"))
```

Objetivo:

Notificar a colônia da morte do worker.

Alternativa preferida:

`ServerLivingEntityEvents.AFTER_DEATH` da Fabric API.

Se o evento for suficiente, **este mixin não deve existir**.

Vanilla First aplica-se também ao Fabric API.

---

# 4. Regras de Mixin

---

## Regra 1 — Nunca @Overwrite

Proibido:

```text
@Overwrite
```

Motivo:

Quebra qualquer outro mod que toque a mesma classe.

Permitido apenas:

```text
@Inject

@ModifyVariable   (com justificativa)
```

---

## Regra 2 — Nunca cancelar comportamento Vanilla

Proibido:

```text
ci.cancel()
```

em métodos de IA do aldeão.

O aldeão Vanilla deve sempre poder concluir sua rotina.

---

## Regra 3 — Mixin não contém lógica

O mixin apenas **delega**.

Correto:

```text
VillagerEntityMixin

↓

ColonyBrainInitializer

↓

Core Service
```

Incorreto:

```text
VillagerEntityMixin

↓

lógica de colônia inline
```

Conforme `CODE-STANDARDS.md §12`.

---

## Regra 4 — Falha isolada

Todo mixin deve degradar com segurança.

Se a colônia não existir:

```text
Não fazer nada
```

Nunca lançar exceção dentro de um método Vanilla.

---

# 5. Prioridade da Activity da Colônia

O ponto crítico da Constituição §4.

---

## Registro

```text
Activity: villagecolony:colony_work
```

---

## Prioridade

Abaixo de:

```text
CORE

PANIC

RAID

HIDE
```

Acima de:

```text
IDLE
```

Ao mesmo nível de:

```text
WORK
```

porém só ativa quando existir memória de task.

---

## Memórias customizadas

```text
villagecolony:current_task

villagecolony:task_target

villagecolony:home_storage
```

---

## Cessão imediata

Quando a task termina ou é cancelada:

```text
Limpar memória

↓

Activity desativa

↓

Brain retorna ao Schedule Vanilla
```

Isso implementa literalmente `PROJECT_CONSTITUTION.md §4`.

---

# 6. Conflito com profissão Vanilla

Um aldeão já possui profissão Vanilla ligada a uma workstation.

O Brain o envia para lá durante `WORK`.

---

## Regra

A profissão da colônia **não altera** a profissão Vanilla.

Nunca chamar:

```text
setVillagerData
```

---

## Resolução do conflito de agenda

A Activity da colônia só assume durante `WORK`.

Fora de `WORK`:

```text
O aldeão dorme, socializa e come normalmente.
```

Um aldeão sem workstation Vanilla é o candidato preferencial a receber
profissão de colônia.

Motivo: não há agenda concorrente.

---

# 7. Compatibilidade

---

## Risco

`VillagerEntity.initBrain` é o ponto de conflito nº 1 com outros mods
de aldeão.

---

## Mitigação

* usar apenas `@Inject` em `TAIL`;
* nunca remover tasks existentes;
* nunca assumir índice fixo de lista;
* documentar incompatibilidades conhecidas no README.

---

# 8. Arquivos necessários

```text
src/main/resources/villagecolony.mixins.json
```

Referenciado em:

```text
fabric.mod.json → "mixins"
```

Pacote:

```text
com.villagecolony.fabric.mixin
```

Conforme ADR-006.

---

# 9. Alternativas rejeitadas

---

## NPC customizado

Rejeitado por `ADR-001 §15`.

---

## Substituir o Brain inteiro

Rejeitado.

Quebra Vanilla e todos os mods de aldeão.

---

## Usar apenas Fabric API

Rejeitado.

Não existe hook de Brain. Foi verificado.

---

# 10. Final Statement

O mixin é a porta, não a casa.

Ele apenas deixa a colônia entrar.

Toda a inteligência permanece no Core.

# ADR-002-Chunk-Loading-Strategy.md

# Architecture Decision Record 002

# Village Colony Chunk Loading Strategy

**Status:** Accepted
**Date:** 2026-08-06
**Accepted:** 2026-08-06
**Decision Type:** Architecture / Simulation
**Blocks:** Simulation-Loop, Resource-System, Storage-System, Phase 1 onwards

---

# 1. Context

Dois requisitos aprovados colidem.

---

## Requisito A

`PROJECT_CONSTITUTION.md §3` — Autonomous Simulation:

> A colônia deve continuar funcionando mesmo quando nenhum jogador está próximo.

---

## Requisito B

`Save-Data-System.md` — Recursos:

> O mod não salva quantidade de itens.
>
> O valor será calculado a partir dos baús reais.

`ADR-001 §6` — Physical Resources Only.

---

## O conflito

Sem jogador por perto, o chunk está descarregado.

Consequências técnicas:

* a `VillagerEntity` não existe em memória;
* o `ChestBlockEntity` não existe em memória;
* `world.getBlockEntity()` **força o carregamento do chunk**.

Forçar carregamento é proibido por `Performance-Rules.md §6`.

---

Resultado:

```text
Colônia autônoma

+

Recursos apenas físicos

+

Sem carregar chunk

=

Impossível
```

Um dos três precisa ceder.

---

# 2. Options Considered

---

## Option 1 — Forceload permanente da colônia

Manter chunk tickets ativos sobre a área da vila.

---

### Prós

* simulação real e contínua;
* recursos permanecem físicos;
* nenhuma aproximação necessária.

---

### Contras

* custo de TPS constante por colônia;
* `Performance-Rules.md §12` exige escalar para 100 vilas — inviável;
* aldeões continuam consumindo IA, pathfinding e mob ticking;
* o jogador paga performance por vilas que nunca visita.

---

### Veredito

Rejeitada como padrão.

Escala mal e contraria a meta declarada de escalabilidade.

---

## Option 2 — Simulação offline aproximada

Sem jogador, estimar produção por fórmula e aplicar ao voltar.

---

### Prós

* custo de CPU quase zero;
* colônia "progride" sempre.

---

### Contras

* viola `ADR-001 §6` — recursos passariam a existir sem origem física;
* viola `PROJECT_CONSTITUTION.md §9` — Resource Conservation;
* o item apareceria no baú sem ninguém tê-lo coletado;
* exatamente o "Forbidden Model" desenhado na ADR-001.

---

### Veredito

Rejeitada.

Quebra o princípio central do projeto.

---

## Option 3 — Hibernação total

Sem jogador, a colônia congela por completo.

---

### Prós

* custo zero;
* simples de implementar;
* nenhum princípio de recurso é violado.

---

### Contras

* viola `PROJECT_CONSTITUTION.md §3` de forma literal;
* a vila deixa de "viver" quando ninguém olha.

---

### Veredito

Rejeitada isoladamente, mas aproveitada como base da Option 4.

---

## Option 4 — Hibernação com estado persistente e retomada

A colônia possui dois modos.

---

### ACTIVE

Condição:

```text
Chunk da colônia carregado
```

Comportamento:

Simulação completa conforme `Simulation-Loop.md`.

---

### DORMANT

Condição:

```text
Chunk da colônia descarregado
```

Comportamento:

* nenhuma task executa;
* nenhum recurso é criado;
* nenhum bloco é colocado;
* o **estado** da colônia permanece salvo e íntegro;
* tasks em andamento permanecem `RESERVED` ou `EXECUTING`.

---

Ao recarregar:

```text
Chunk carregado

↓

Colônia acorda

↓

Revalidar workers (UUID)

↓
Revalidar storages (baú existe?)

↓

Reconstruir Resource Registry

↓

Retomar tasks pendentes
```

---

### Prós

* custo zero quando ninguém está por perto;
* escala para 100+ vilas sem mudar arquitetura;
* recursos continuam 100% físicos;
* nenhuma aproximação, nenhum item inventado;
* `Save-Data-System.md` funciona exatamente como está escrito.

---

### Contras

* a vila não progride enquanto o jogador está longe.

---

# 3. Decision

Adotar a **Option 4 — Hibernação com estado persistente e retomada**.

---

# 4. Reinterpretação da Constituição §3

O princípio §3 deve ser lido como:

> A colônia não depende do **jogador** para funcionar.

E não como:

> A colônia funciona em chunks descarregados.

---

Justificativa:

O texto original de `§3` lista o que a colônia faz sem o jogador:

```text
coleta de recursos;
produção;
construção;
atribuição de profissão;
expansão.
```

Nenhum desses itens exige chunk descarregado.

Exige apenas que o jogador **não dê ordens**.

Vilas Vanilla já se comportam assim: existem, mas só tickam carregadas.

Esta leitura preserva a intenção original e a filosofia Vanilla First.

---

# 5. Consequences

---

## Simulation-Loop.md

Adicionar estado de colônia:

```text
DORMANT
```

O loop só executa para colônias ACTIVE.

---

## Resource-System.md

O Resource Registry é **cache de runtime**, reconstruído ao acordar.

Já está correto no documento atual. Nenhuma mudança necessária.

---

## Storage-System.md

Ao acordar, todo storage precisa ser revalidado.

Baú removido durante hibernação:

```text
Storage Missing
```

---

## Save-Data-System.md

Reforçar: o estado salvo deve ser suficiente para retomar sem perda.

---

## Performance-Rules.md

Colônias DORMANT não contam para o orçamento de tick.

---

# 6. Exceção futura (fora do MVP)

Um jogador poderá, futuramente, marcar **uma** colônia como permanentemente
carregada, por escolha explícita e com custo assumido.

Não implementar no MVP.

Exige nova ADR.

---

# 7. Rejected Alternatives Summary

```text
Forceload permanente     → não escala

Simulação aproximada     → viola ADR-001 §6

Hibernação sem retomada  → perde estado
```

---

# 8. Final Statement

A colônia dorme quando ninguém a observa.

Ela não esquece.

Ao acordar, continua exatamente de onde parou.

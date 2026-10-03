# ADR-025 — Mineiro autônomo: mina andável, movimento com ações e registro de veios

**Status:** aceita (autor, 2026-09-25: "Projeto bom, aplique ele como prioridade")
**Data:** 2026-09-25
**Minecraft:** 1.21.1 · **Mappings:** Yarn 1.21.1+build.3 · **Fabric API:** 0.116.15+1.21.1

## Contexto

O mineiro anda só pela navegação Vanilla, que não quebra nem põe bloco, numa
mina que o mod escava por geometria fixa. Quando a mina real difere da
geometria, ele empaca: em 25-09 voltou à mesma pedra inalcançável em todas as
sessões, ficou preso e saiu da escala. O autor pediu um mineiro independente,
que entre e saia da mina sozinho, ponha e tire bloco para se mover, evite
queda, água e lava, e colha os veios mais valiosos da área acessível.

## Problema

Dar ao mineiro mobilidade própria e segura sem trocar a navegação Vanilla onde
ela funciona, e sem custo de CPU descontrolado.

## Pesquisa que sustenta

| Documento | O que estabeleceu |
|---|---|
| `docs/research/2026-09-25-mineiro-autonomo.md` | Diagnóstico, comparação Baritone / mineflayer / MineColonies / Workers, plano |
| `docs/research/E32-miner-walk-target.md` | O que a navegação faz com alvo em ar e sólido |
| `docs/research/opcoes-de-melhoria-das-profissoes.md` | Penalidade de água por mob |

**Fatos relevantes:**

- `[FATO]` `LandPathNodeMaker` só lê o mundo (1.21.1, `javap`).
- `[FATO]` `LAVA = -1`, `WATER = 8` nas penalidades padrão (1.21.1, `javap`).
- `[FATO]` `MineMarks` não é persistido; a pedra recusada volta a cada sessão
  (log de 25-09).

**Incertezas ainda abertas:**

- `[HIPÓTESE]` A fase 1 sozinha resolve a maioria dos travamentos. Se errada, a
  fase 2 passa de reserva a obrigatória.

## Opções

### Opção A — Só a mina andável (estilo MineColonies/Workers)

**Degrau da escada:** 6 (composição). Piso, tampa de fluido e marca persistida
ao cortar; a navegação Vanilla continua sendo o único movimento.
**Custo:** baixo. **Risco de compatibilidade:** LOW.
**Limite:** não tira o mineiro de lugares que a mina não previu (caverna,
buraco natural), nem alcança veio fora da geometria.

### Opção B — Movimento próprio completo (estilo Baritone)

**Degrau da escada:** 6. A* próprio para todo passo, com quebra e colocação.
**Custo:** alto (CPU, código, testes). **Risco de compatibilidade:** LOW.
**Limite:** troca um sistema que funciona na maior parte do tempo; mais lugares
para errar.

### Opção C — Híbrido em três fases (A, depois planejador local só quando a navegação falha, depois registro de veios)

**Degrau da escada:** 6. **Custo:** médio, distribuído em entregas testáveis.
**Risco de compatibilidade:** LOW. **Risco de CPU:** controlado por raio e
orçamento, e só depois de a navegação falhar.

## Critérios

Este projeto prioriza, nesta ordem: **não travar a colônia** > naturalidade
(o que um jogador faria) > CPU dentro do tique > simplicidade.

## Decisão

`[DECISÃO]` Opção C: primeiro a mina que se mantém andável e a marca
de fracasso persistida; depois o planejador local com ações, usado só quando a
navegação Vanilla falha; por fim o registro de veios com prioridade por valor.

## Por quê

A fase 1 ataca as causas medidas com risco baixo. A fase 2 dá a autonomia que o
autor pediu sem pagar o custo de um pathfinder completo em todo passo. A fase 3
transforma "segue o veio que tocou" em "colhe o veio que viu", sem x-ray.

## O que estamos abrindo mão

- Movimento idêntico ao de um jogador em todos os casos (parkour, balde d'água).
- Varredura oculta de minério: o mineiro só conhece o que viu.

## Consequências

**Positivas:** mineiro que não empaca na mesma pedra; mina que não mata nem
afoga; minério valioso colhido primeiro.

**Negativas:** blocos a mais no mundo (piso, pontes, pilares, tampas); estado
novo no save (marcas e veios).

**Neutras, mas a lembrar:** toda colocação consome pedregulho da colônia e passa
por `BlockProtection`.

## Andamento

- **2026-09-25 — fase 1 no código**, não vista em jogo: `WorkMarksSavedData`,
  `MineFloor`, `MinerCaution`, `MinerReport.brainOf`. Desvio: o piso da fase 1
  não consome pedregulho (como a vedação do `MineFlooding`); o consumo entra
  com a fase 2. Pendente da fase 1: recusar quebrar bloco que segura fluido
  protegido. Ver `docs/research/2026-09-25-mineiro-autonomo.md` §9.
- **2026-09-25 — fase 1 fechada e fase 2 no código**, não vistas em jogo. O
  autor ampliou o pendente: o mineiro não quebra bloco com **qualquer** líquido
  atrás (`MineFlooding.holdsBackFluid`). Fase 2: `core/movement`
  (`DetourPlanner`, A* com raio 16 e 2.500 nós) e `DetourWalker`, usados pelos
  guardas de travamento do mineiro e pelo encalhado sem escada natural. O
  consumo de pedregulho entrou aqui, como previsto. Ver §9-§10 da pesquisa.

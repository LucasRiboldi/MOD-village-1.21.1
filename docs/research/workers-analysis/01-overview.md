# 01 — Visão geral

**Data da análise:** 2026-08-15
**Objeto:** `workers-maingit/` (Villager Workers 2, versão 2.0.3)
**Objetivo:** engenharia reversa arquitetural. Nenhum código foi copiado,
e nenhum arquivo do Village Colony foi alterado nesta fase.

---

## 1. O que é o Workers

Mod do CurseForge que adiciona **trabalhadores contratáveis pelo jogador**.
O jogador compra um trabalhador, coloca no mundo uma **área de trabalho**
(uma entidade invisível com caixa 3D), e o trabalhador procura essa área,
vai até ela e executa a profissão dele dentro dos limites da caixa.

Não é um mod de vila. É um mod de **peões do jogador**. A diferença
importa para tudo o que vem depois: no Workers, a vila do Vanilla é
cenário; quem é dono, quem manda e quem paga é o jogador.

---

## 2. Matriz de versão — os dois projetos

| | Village Colony (seu) | Workers |
|---|---|---|
| Minecraft | 1.21.1 | 1.20.1 |
| Loader | Fabric (loader 0.19.3, API 0.116.15) | Forge 47.1.0 |
| Java | 21 (`options.release = 21`) | 17 |
| Mappings | Yarn `1.21.1+build.3` | `official` (Mojang) |
| Build | Loom 1.17.18 | ForgeGradle 6 + Mixin 0.7 + Shadow |
| Mixins | 1 (`VillagerEntityMixin`) | 1 (`MixinVillager`) |
| Testes | 366 unitários + 80 gametest | **nenhum** |
| Deps de terceiros | nenhuma | **Recruits (obrigatória)**, corelib, e 6 mods de compat |
| Classes Java | 148 (main+test+gametest) | 144 (só main) |
| Linhas Java | ~22.800 | ~26.100 |

Fontes: `build.gradle` e `gradle.properties` de cada projeto.

**Consequência imediata:** nenhuma classe do Workers compila no seu
projeto. Mudam o loader, os mappings (`Level`/`World`,
`BlockPos.getCenter()`/`toCenterPos()`), a versão do jogo e a versão do
Java. Qualquer aproveitamento é necessariamente **reimplementação**.

---

## 3. A dependência que define o Workers

`AbstractWorkerEntity extends AbstractChunkLoaderEntity`
(`com.talhanation.recruits.entities`) — arquivo
`workers-maingit/src/main/java/com/talhanation/workers/entities/AbstractWorkerEntity.java:50`.

O Workers **não é um mod autônomo**. Ele é um addon do mod *Recruits*, do
mesmo autor, e herda dele:

* a entidade base (com dono, `followState`, moral, pagamento, inventário);
* o carregamento de chunk (`AbstractChunkLoaderEntity`);
* a navegação assíncrona (`AsyncGroundPathNavigation`, `AsyncPath`,
  `NodeEvaluatorCache`);
* o sistema de facções, times e claims;
* o sistema de contratação (`RecruitsHireTradesRegistry`, em
  `VillagerEvents.registerWorkerTrades`);
* o *upkeep* — comida, pagamento, reequipar (`RecruitUpkeepEntityGoal`).

Isso é a informação mais importante de toda a análise. Grande parte do
que o Workers "resolve" está **fora** do repositório que você tem. O que
está aqui é a camada de profissões sobre uma base que não veio junto.

---

## 4. O que o repositório local NÃO contém

Registrado para que nenhuma conclusão futura o esqueça:

```text
sem .git                  → não há histórico de commits para minerar
sem CHANGELOG             → só update.json (4 linhas) e updatetamplete.txt
                            (releases 1.5.2/1.5.3, de outra major)
sem testes                → nenhum teste unitário, nenhum gametest
sem documentação          → README de 6 linhas
sem o Recruits            → a metade da arquitetura é externa
```

O §14 do briefing pedia mineração de histórico de correções. **Não é
possível com o material local.** O que sobrou de evidência histórica está
em `update.json`:

```text
2.0.3  improved courier and fixed bugs, added keep setting to mining area
2.0.2  improved pathfinding and movement AI, fixed crash, added mine modes
2.0.1  fixed courier, improved compat with farmers delight
2.0.0  Initial Release of Villager Workers 2
```

Duas de quatro linhas citam o *courier*, e uma cita pathfinding. Isso é
consistente com o que o código mostra (ver §04 e §05 desta análise), mas
é indício, não prova.

O que **substitui** o changelog perdido são os comentários no código. O
autor deixou comentários que explicam a correção no ponto onde ela foi
feita — e esses são a fonte histórica mais rica que existe aqui. Estão
catalogados no `03-ai-analysis.md §7` e no `05-pathfinding.md §4`.

---

## 5. O contraste central

| | Village Colony | Workers |
|---|---|---|
| Quem decide | a **colônia** (`ColonyCycle`) | o **trabalhador** (Goal) |
| Quem é dono | ninguém — a vila | o **jogador** |
| Quem define onde trabalhar | o mod, varrendo o mundo | o **jogador**, plantando áreas |
| Entidade | aldeão Vanilla + Brain | entidade própria + Goals |
| Estado da tarefa | objeto `Task` persistido no domínio | `enum State` dentro do Goal |
| Núcleo | Java puro, testável, sem MC | inseparável do Minecraft |

São arquiteturas **opostas por decisão**, não por qualidade. O Workers é
top-down do jogador para baixo; o Village Colony é top-down da colônia
para baixo, com o jogador de fora (PROJECT_CONSTITUTION §4, ADR-001).

Por isso, quase nada do Workers entra como está — e quase tudo dele
ensina alguma coisa sobre a **fronteira com o Minecraft**, que é
exatamente onde o §11 do seu `Project-State.md` registra que moraram
todos os defeitos sérios do seu projeto.

---

## 6. Método usado

1. Leitura de `build.gradle`, `gradle.properties`, licença e créditos dos
   dois projetos.
2. Inventário completo de classes dos dois (144 e 148 arquivos).
3. Leitura integral de 14 classes-chave do Workers, e parcial de outras 12.
4. Leitura das seis ADRs e do `Project-State.md` do Village Colony.
5. Nenhuma conclusão foi tirada de nome de arquivo. Onde não foi lido, o
   documento diz "não lido".

---

## 7. Mapa dos demais documentos

```text
02-architecture.md          o mapa real do Workers
03-ai-analysis.md           Goals, máquinas de estado, falha e retry
04-task-system.md           por que o Workers não tem sistema de tarefas
05-pathfinding.md           o pathfinder assíncrono — a joia técnica
06-inventory-logistics.md   NeededItem, depósito, coleta, courier
07-work-areas.md            o conceito de Work Area e sua extensibilidade
08-persistence.md           NBT de entidade, e o que ele custa
09-reusable-patterns.md     a matriz de aproveitamento (A/B/C/D)
10-project-comparison.md    a matriz conceitual sistema a sistema
11-license-analysis.md      All Rights Reserved — o que isso proíbe
12-recommendations.md       arquitetura recomendada, roadmap, riscos
```

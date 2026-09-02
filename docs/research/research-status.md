# Minecraft Research Status

Índice vivo da pesquisa técnica do projeto. Quem retoma lê este arquivo
primeiro. Criado em 2026-09-02, na primeira sessão que usou a skill
`minecraft-code-research` num trabalho real do mod.

## Objetivo atual

Diagnosticar o **E32** — o mineiro não entra na própria escada e fica
estacionado com `0/0 ticks`. Status: **causa encontrada e conserto escrito**,
com fase vermelha conferida. Falta a sessão de jogo.

## Ambiente

| | |
|---|---|
| Minecraft | 1.21.1 |
| Mappings | Yarn 1.21.1+build.3 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.116.15+1.21.1 |
| Java | 21 |
| Conferido em | 2026-09-02, via `gradle.properties` |

Os fontes do Vanilla foram gerados (`./gradlew genSources`) e estão em
`.gradle/loom-cache/minecraftMaven/.../minecraft-merged-*-sources.jar`.

## Sistemas Vanilla investigados

| Sistema | Status | Documento |
|---|---|---|
| `MobNavigation.findPathTo(BlockPos, int)` — o que a navegação faz com alvo em ar e alvo sólido | concluído | [`E32-miner-walk-target.md`](E32-miner-walk-target.md) |
| Ciclo de vida do trabalhador — o que limpa o quê quando o dono some | concluído (auditoria) | [`estado-que-sobrevive-ao-dono.md`](estado-que-sobrevive-ao-dono.md) |

## Fatos confirmados

- `MobNavigation.findPathTo` trata os dois casos de forma **oposta**: alvo no
  **ar** é abaixado até o chão da coluna; alvo **sólido** é subido até o
  primeiro bloco não-sólido acima. Lido no fonte 1.21.1.
- `Mine.cut` conta posição **entregue**, não bloco **cavado**.
- `MinerReach.legTowards` não recebe `ServerWorld` e não pode checar
  pisabilidade; `MinerWork.approachTo` checa (`BuilderApproach.standable`).
- `GoToWorkTargetTask` não valida o alvo — só o embrulha num `WalkTarget`.

## Inferências importantes

- O mineiro acaba na superfície acima da mina porque a perna entrega um bloco
  da ordem de cavar que ainda é rocha, e o Vanilla sobe esse alvo até sair da
  rocha. Explica o sintoma registrado em 2026-08-28 (`y=66`, 21 blocos acima
  da galeria).

## Hipóteses derrubadas

- *"A navegação recusa descer em buraco de um bloco de largura"* (suspeita
  registrada no TODO para o E32) — **falsa**. A navegação não recusa; ela
  realoca o alvo, e é a realocação para cima que produz o sintoma.
- *"As camadas de cabeça da escada (2 de cada 3 posições) quebram o destino"*
  — **falsa**. Alvo no ar é abaixado até o chão pelo próprio Vanilla.

## Pendências

| Prioridade | O que falta |
|---|---|
| ~~P0~~ | ✅ **Feito em 2026-09-02.** `legTowards` passou a receber um `Predicate<BlockPos>` — e não o `ServerWorld`, para que `MinerReach` continue testável sem subir jogo. `MinerWork` passa `at -> BuilderApproach.standable(world, at)`; o teste passa o mundo que quiser. Dois testes novos, fase vermelha conferida por `AssertionFailedError` |
| ~~P1~~ | ✅ **Feito em 2026-09-02.** `theLegNeverAimsAtRockNobodyDug` monta rocha maciça, abre os três primeiros degraus, deixa o `cut` em trinta e afirma que o passo é pisável **perguntando ao mundo**. Fase vermelha conferida por `GameTestException` com a mensagem da afirmação. **O que ele não prova:** que o mineiro *anda* até lá — isso continua dependendo de sessão |
| P2 | Sessão de jogo — o E32 nunca foi visto depois de conserto nenhum |
| P3 | O E34 mora ao lado: o mod ainda não distingue o que ele cavou do que o jogador cavou. O filtro novo aceita como destino um bloco pisável que o **jogador** abriu, e a ordem de cavar continua sendo a única fonte de contiguidade |

## Decisão pendente do autor

**O trabalhador fantasma** — aldeão que some sem disparar `AFTER_DEATH` nem
`MOB_CONVERSION` fica registrado para sempre, ocupando vaga de profissão e
reserva de baú, e atravessa o save. Três saídas possíveis, todas com custo;
a auditoria deliberadamente não escolheu. Ver
[`estado-que-sobrevive-ao-dono.md`](estado-que-sobrevive-ao-dono.md).

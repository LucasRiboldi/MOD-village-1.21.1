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
| ~~P3~~ | ✅ **Feito em 2026-09-02 (E34).** O risco que este item previa era real e maior do que eu escrevi: não era só bloco do jogador — o laço do E32 **pulava** posição fechada e seguia somando, então qualquer vão aberto num índice mais avançado (bolsão do jogador **ou caverna natural**) virava destino do outro lado de uma parede. O laço passou a parar na primeira posição não-atravessável. Nasceu daí a separação entre **atravessar** e **ficar de pé** (`MinerReach.Footing`): parar no que não é pisável travaria a descida no primeiro degrau, porque duas de cada três posições da escada são peito e cabeça |

## Decisões do autor — respondidas em 2026-09-02

**O trabalhador fantasma:** escolhida a saída de **medir antes de
consertar**. `PhantomWorkerLog` conta ausências seguidas em colônia ativa e
noticia na terceira, uma vez. A poda foi recusada porque *ausência não é
morte* e o gatilho é raro. **A próxima sessão de jogo responde se o fantasma
existe** — e essa resposta é a que decide se algum conserto vale.

**A colônia abandonada no ciclo:** escolhido **pular só o planejamento de
obra**, não o ciclo inteiro. Corta a varredura de lote e o crescimento de
rua para colônia sem vila; trabalhadores seguem cuidados, porque a marca de
abandono oscila (E9). Regra em `ColonyAbandonment.plansConstruction`.

**A varredura:** escolhido **não mexer** até uma sessão dizer se o índice de
ruas de 08-27 já a resolveu. A primeira linha a ler no log é `still
sweeping`: uma vez seguida de `planned ... at` significa resolvido; a sessão
inteira significa que é ali que está toda a fluidez.

Ver [`estado-que-sobrevive-ao-dono.md`](estado-que-sobrevive-ao-dono.md).

# STATE — 2026-10-20

> **0.3.68** (20-10): **as bancadas funcionam de verdade para o jogador.** Defeito achado: as telas
> Vanilla (mesa, cortador, tear) só ficam abertas diante do bloco Vanilla exato — com o bloco do mod abriam
> e fechavam no mesmo tique, então o "o jogador usa as bancadas" da 0.3.63 **nunca funcionou**. Agora cada
> bancada tem tela própria e funciona como a estação Vanilla do ofício (decisão do autor): mesa
> (carpinteiro, lenhador, construtor, mineiro), cortador de pedra (pedreiro), **fornalha de verdade**
> (fundidor, BlockEntity próprio), tear (pastor), composteira (fazendeiro). Todas cubos cheios. **Também:**
> os blocos não tinham tabela de drop (quebrar não devolvia o bloco) — corrigido, com tags de ferramenta.
> **Texturas:** recorte novo pela grade real dos mockups (as faces sul/fundo da 0.3.65 pegavam a tarja da
> legenda) com 6 painéis diferentes; fazendeiro e mineiro ganharam arte nova do ofício
> (`scripts/assets/art_farmer_miner.py`). **Fundidor:** a argila ausente aparece no baú dele
> (`KnownAbsent`) em vez de 4 buscas inúteis por lote. **Não jogado.** Verificado: test 1534/0;
> runGametest 773/773 (`BenchUseGameTest`: telas com controle Vanilla, forja funde, composteira composta,
> todo bloco cai ao quebrar); mutações da tela e do drop derrubam os testes. JAR `village-colony-0.3.68.jar`
> = `CDD55F4F07D588B096C00C6401FDCA936B1DA8C057C0E0354E1A5BD3D1DF4819`, instalado. **Próximo:** simulação
> por bioma (plano em memória `bancadas-telas-e-biomas-plano`).

> **0.3.67** (20-10): **corrige os 3 defeitos graves da varredura de 20-10**
> ([`docs/audit/2026-10-20-varredura.md`](docs/audit/2026-10-20-varredura.md)), todos em código não
> jogado das 0.3.64/0.3.66. **G1:** a proteção da bancada zerava o prazo ao desistir e o artesão voltava
> a caminhar a cada 200 tiques (produção ~200× menor com bancada inalcançável) — agora, estourado o
> prazo, fabrica no lugar até o fim da tarefa. **G2:** a bancada era procurada todo tique (~9.800
> leituras de bloco por carpinteiro) — agora uma vez por tarefa (`CraftStation.Approach`). **G3:** o
> não-mineiro que desistia de escalar seguia marcado como encalhado, fora da fila, repetindo busca e log
> para sempre — agora volta à vila pelo teleporte da cama (`BedTeleport.toBed`); sem cama, é liberado.
> **Não jogado.** Verificado: test 1529/0; runGametest 769/769; mutação do G1 derruba o teste. JAR
> `village-colony-0.3.67.jar` = `A0594C3F1C24F75D9BEF3A6CBC3D15352114FBA59DC7C4DDEFCA4851E3E37E23`,
> instalado e conferido. **Próximo:** jogar ≥30 min na vila montanhosa (pastor fora do laço, artesão sem
> queda de produção, MSPT estável); depois o resto da ordem de ataque da varredura.

> **0.3.62→0.3.66** (15-10, branch `fix/climb-nao-mineiro-so-natural`): sessão de bancadas + terreno
> montanhoso. **0.3.62:** bancada do mod para as 8 profissões. **0.3.64:** só o mineiro remodela o
> terreno para escalar; as outras sobem por degrau natural (raio 6) ou desistem sem tocar o mundo
> (vila montanhosa — pastor em laço de encalhe). **0.3.65:** cada bloco do mod com os 6 lados
> diferentes (HD 64px recortadas dos mockups 1254px). **0.3.66:** o artesão vai até a bancada antes de
> fabricar (mod→Vanilla→normal) com salvaguarda do `APPROACH_BUDGET` — reconcilia com a regra de 10-11
> ("fabrica onde está"), bancada inalcançável não trava. **Nenhuma jogada** (0.3.66 instalada,
> FB13…→ver install-mod). Verificado: test 1530/0; runGametest 769/769.
> **Pendente (ociosidade, roteiro em memória):** 6 dos 7 ócios têm raiz na ALTURA → a **terraplanagem
> da vila** (nivelar a montanha para subida ≤1 bloco, sem afrouxar o critério de obra) é o plano futuro
> que resolve 4 deles; só o SMELTER (espera clay inexistente, C4) é acionável sem ela. Próximo: jogar
> 0.3.64/66, depois SMELTER, depois terraplanagem.

> **0.3.61** (15-10): **o desvio da mina respeita o beco** (A3, fecha o P1). O playtest da 0.3.60
> (Spark `HNi26AzwWD`, 44 min, **versão confirmada no log**) mostrou o reroute **executando** (10×) mas
> **sem atingir o aceite**: NO_STANDING subiu a 35×, MINER Regra 51 = 60%. Causa (leitura das coords):
> as recusas são uma **parede/teto de caverna** (50 de 59 em y67-68, cluster apertado), não pedras
> isoladas — o `roomBeside` escolhia um vizinho **na mesma região sem pé**, e o desvio trocava
> NO_STANDING por TOO_HIGH. **Correção:** o desvio não prossegue quando o alvo está num **beco**
> (`MineMarks.isInADeadEndAt`, ≥3 recusas na vizinhança), e o `roomBeside` passa a respeitar
> `isUnreachableAround` (não só `isOutOfReach`) — numa parede, deixa o contorno/curva agir em vez de
> desviar para dentro dela. O desvio segue valendo para a pedra **isolada**. **Não jogado** (0.3.61
> instalada e conferida, sha256 542A761B…). Verificado: test 1523/0 (2 novos); runGametest 769/769
> exit 0; mutação (ignorar o beco) derruba `aStoneInsideADeadEndDoesNotReroute`.
> **O próximo jogo precisa mostrar:** NO_STANDING caindo numa vila com caverna, MINER Regra 51 < 15%.

> **0.3.60** (15-10): **o mineiro desvia para a pedra ao lado antes de largar** (P1 do plano, mina).
> ⚠️ **Os consertos do mineiro (0.3.59 DirectPath, 0.3.60 reroute) ainda NÃO foram jogados:** os JARs
> não estavam na pasta de mods — os 3 últimos playtests rodaram **0.3.58** (o `SendUserFile` entrega ao
> chat, não instala; ver memória `jars-nao-instalados-deslocamento`). O salto MINED 0,8→~19/min foi de
> **0.3.57→0.3.58**, não do conserto do DirectPath. A 0.3.60 foi instalada e conferida (sha256
> F164319032…) em 15-10; o próximo jogo é o **primeiro** com o conserto do mineiro. Restava o 2º defeito
> em série, alvo deste ciclo: o approach **encala a 4-5 bl** quando o
> mineiro chega perto (22× NO_STANDING_POSITION + 9× TOO_HIGH, conclusão 11%). **Correção:** antes de
> `passOver` marcar e largar, se faltava só o pé (`worthRerouting` = só NO_STANDING), tenta
> `MineVein.roomBeside` (ADR-038 P6) e redireciona o alvo para a pedra vizinha que abre o pé; TOO_HIGH/
> sem-caminho seguem o contorno de sempre. **Não jogado.** Verificado: test 1521/0 (3 novos em
> `MinerRerouteTest`); runGametest 769/769 exit 0; mutação (desviar para qualquer rejeição) derruba
> `tooHighAndNoPathAreNotReroutable`. JAR `village-colony-0.3.60.jar` =
> `F164319032E6DE6342027DE336B89B7AD34A5CC42527A1A51E0A2AF37295C169`.
> **O próximo jogo precisa mostrar:** NO_STANDING_POSITION caindo (desvio funciona), MINER idle+blocked
> < 15% no expediente (Regra 51), `went one level deeper` > 1. Playtest **de dia**.

> **0.3.59** (14-10): **o mineiro recusa o falso caminho à superfície** (P0, mina). Playtest da 0.3.58
> (Spark `YOT1oqyhIv`, 100 min) mostrou TPS 20 estável, mas o mineiro passava **janelas longas**
> "walking to ‹pedra›" sem chegar — parado em y89-93 sobre alvo em y80, `went one level deeper` 1× em
> 100 min, só bursts intermitentes. **Causa (spike):** `MinerDirectPath.reaches` aceitava
> `path.reachesTarget()` sem conferir onde o caminho termina; o `findPathTo` sobe o alvo sólido/distante
> até a superfície (E32) e reporta "chegado" com o fim dezenas de blocos acima no Y, curto-circuitando
> o `MineApproachPath`/perna. **Correção:** `endReachesApproach` exige o fim do caminho a ≤2 Manhattan
> nos **três eixos** (Y incluído); `reachesTarget()` deixa de bastar sozinho. **Não jogado.** Verificado:
> test 1518/0 (3 novos); runGametest 769/769 exit 0 (1ª rodada foi flake ~25%); mutação (ignorar a
> distância) derruba `endOnTheSurfaceAboveTheStoneDoesNotReach`. JAR `village-colony-0.3.59.jar` =
> `FE20DDC6437E519A6158267CFF9FB7C199C96D6615FB2E81F605477E2CBBAF68`.
> **Aberto (2º defeito em série, próximo ciclo):** mesmo quando chega perto, o approach encala a **4-5
> blocos** (fora do REACH 4) — o NO_STANDING_POSITION/roomBeside (A1/A3), não medido nesta correção.
> **O próximo jogo precisa mostrar:** o mineiro chegar à frente e descer (`went one level deeper` > 1,
> MINED/min sobe), playtest **de dia**.

> **0.3.58** (14-10): playtest da 0.3.57 (Spark `iALIIoOwEK`, 42 min) — a horta destravou (obra
> avança, 201 peças, TPS 20), mas **ociosidade geral alta**: lenhador 98% ocioso, pedreiro 71%,
> pastor 80%, fundidor 60× "no task open". Raiz medida: a **vila está madura e sem obra** — 21×
> "no empty plot", estoque cheio (837 cobble, 322 toras); os produtores param porque não há obra
> pedindo, não por falta de material. E a **expansão era lenta**: esperava 3 voltas de varredura
> (`FULL_AGAIN_AFTER=3*FULL_AFTER`, dezenas de min) entre anexos. **Correção (decisão do autor):**
> `FULL_AGAIN_AFTER = FULL_AFTER` — a vila anexa a faixa já na volta que confirma a falta de lote,
> crescendo no ritmo em que fica ociosa; a salvaguarda contra inchaço (só lado plano/seco/sem outra
> colônia) permanece. **Não jogado.** Verificado: test 1515/0; runGametest 769/769. JAR
> `village-colony-0.3.58.jar` = `429511EA11985FA09724E6A69F5E4C77A222DBB4DD6824709ABF0FDBD39BA659`.
> **Aberto:** o mineiro ainda tem 172× NO_STANDING_POSITION (44% bloqueado) — o único ócio por
> defeito real, não por vila madura.

> Arquivo de estado vivo. **Sobrescreve, não acumula.** Se passar de 150 linhas, algo
> está errado: P0 não está fechando. O que já foi resolvido mora em
> [`docs/archive/technical/Historico-2026-10.md`](docs/archive/technical/Historico-2026-10.md)
> (o STATE de 10-10 inteiro, 709 linhas, sem edição) e
> [`Historico-2026-09.md`](docs/archive/technical/Historico-2026-09.md); consulta por `grep`.

---

## 🔴 P0 — As profissões não trabalham de forma contínua; a mina é a raiz

Reavaliação 14-10 ([`docs/audit/2026-10-14-reavaliacao-pos-0359.md`](docs/audit/2026-10-14-reavaliacao-pos-0359.md)),
plano de execução ([`docs/technical/Plano-Melhoria-2026-10-14.md`](docs/technical/Plano-Melhoria-2026-10-14.md)).
O fio comum: **ação interrompida** (parada, bloqueada, esperando), e a **mina** concentra o P0.

**O que a mina é hoje (correção da varredura de 14-10):** o PMD atual mostra o pior método em
complexidade **62** (`MinerSteps.step`), seguido de `MineEdge.inside` 48 e `MineCuts.nextCut` 46 — a
mina concentra a complexidade (o A1 da varredura estava certo). O problema da mina é **complexidade +
tamanho de subsistema** (31 classes) e
**geografia** (boca a ~77–95 blocos numa vila grande), não um método-monstro. A 0.3.59 fechou o
mecanismo do falso caminho à superfície; **falta o 2º defeito em série (approach encalha a 4–5 bl) e a
geografia**. Em volta: cadeia de material que trava a obra (C2), profissões periféricas em laço
improdutivo (C4: fundidor pede clay inexistente, fazendeiro sem lote).

**Plano em execução:** Fase 0 higiene (H1 feito — este STATE; H2 time_ledger dia/noite; H3 TODO) →
Fase 1 produto (P1 approach 4–5 bl; P2 geografia da boca; P3 troca de tarefa) → Fase 2 forma (F1
subpacotes `work/`; F2 teto de 500; F3 complexidade PMD).

**Aceite do P0:** playtest de dia, ≥ 30 min; Regra 51 (ocioso+bloqueado+encalhado) ≤ 15% por ofício no
expediente; `MINED`/min > 0,8; nenhuma profissão em laço de recusa visível no `action_report`.

## Branches e processo

**`main` é a linha oficial** (ADR-035 §2). B1 resolvido: a `claude/autossuficiencia` foi integrada
(0.3.25→0.3.55) e os ciclos seguintes (0.3.56→0.3.59) são branches curtas por PR. Ciclo atual:
`fix/miner-directpath-superficie` (0.3.59 + reavaliação + plano), ainda sem push/merge.

## O que o próximo jogo precisa mostrar

JAR **0.3.59** instalado (o 0.3.58 foi jogado: Spark `YOT1oqyhIv`, 100 min; é dele a reavaliação).
Playtest **de dia** (CLAUDE.md §0.4):

| # | Item | Sinal |
|---|---|---|
| 1 | Mineiro chega à boca e desce | `MINED`/min > 0,8; sem "walking to nowhere" prolongado; telemetria do percurso (a implementar) |
| 2 | Profissões contínuas | `time_ledger`: Regra 51 ≤ 15% por ofício no expediente |
| 3 | Sem laço de recusa | `action_report`: soltas/pegas < 10%, sem o mesmo motivo repetido por profissão |
| 4 | Obra não trava | `grep VC_SUPPLY_ERROR` perto de zero; "opened a build task" avança entre obras |

Depois de jogar: `scripts/analyze_village_log.py`, `time_ledger.py`, `action_report.py`,
`cost_ledger.py` (CLAUDE.md §0.4).

## 🔴 Aberto

A fila completa e priorizada está no `TODO.md` (seção AUD) e no roteiro
(`docs/technical/Roteiro-Correcoes-2026-10-10.md`, tabela "Estado da execução").

- **2.4, fundidor e despacho:** o código da linha Codex está integrado; falta medir em jogo.
- **AUD-11 (parcial):** `fabric.network` já passa dos 60% (67%); faltam `fabric.command` (27%) e o cliente (0%).
- **AUD-04 e AUD-16:** só com gatilho (Fase 4). Sem número, sem otimização (ADR-035 §5).
- **Fase 5 (dívida):** AUD-15, 20, 10 e 09; o AUD-09 pede ADR.
- **Decisões do autor:** apagar as 7 branches já contidas na `main` (AUD-24); o número da
  regra do ócio, que divide o "Regra 51" com a da peça na 4ª tentativa (AUD-26); a ADR do AUD-09.
- **AUD-18:** tag por JAR publicado. Não foi feito: criar tags é publicação.

## Dívida conhecida

- Teto de 500 linhas virou colete (A3): **4 arquivos exatamente em 500** (`TreeChoice`, `SmelterWork`,
  `BuilderWork`, `ColonyGoals`), ~20 entre 470–500. Tratar 500 como gatilho de extração (ciclo F2).
- `runGametest` não filtra por teste dentro da bateria completa; `-PgametestOnly=Classe` roda
  uma classe isolada. Testes rodam juntos num mundo só: quem conta entidades num raio grande
  afirma sobre as suas (o `FreeChestCountGameTest` falhava 2 em 8 por isso).
- Cobertura da bateria: `fabric.command` e `fabric.network` baixas; cliente 0% (sem cliente no teste).
- Hooks do Claude Code em `scripts/hooks/`; ligá-los no `.claude/settings.json` é do autor.

## Como avaliar e investigar

- **Avaliação técnica:** `docs/technical/avaliacao/` (auditoria de 10-10, régua v2 na
  `METODOLOGIA.md`); `python scripts/assess/assess_project.py --run --gametest-runs 2`.
- **Verificações reprodutíveis:** `scripts/audit/check-*.sh` (README na pasta).
- **Instrumentar antes de consertar**; montar a linha do tempo antes de declarar impasse.
- **Ler o save quando o log não basta:** a RC2 só se decidiu lendo as tochas do mundo.

  | Ferramenta | Para quê |
  |---|---|
  | `scripts/analyze_village_log.py` | assinaturas e peças esperadas no log |
  | `scripts/time_ledger.py`, `scripts/action_report.py`, `scripts/cost_ledger.py` | Regras 50 e 51, diário de ações, custo do ciclo |
  | `scripts/verdict.py` | veredito por item pendente |
  | `ChainRootsGameTest`, `StructureCoverageGameTest` | onde cada cadeia começa; quem fabrica cada peça |
  | `CraftReasons`, `VolumeSample`, `ProtectionSample` | por que algo não saiu ou foi recusado, no log |

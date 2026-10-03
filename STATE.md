# STATE — 2026-10-03

> Arquivo de estado vivo. **Sobrescreve, não acumula.**
> Se passar de 150 linhas, algo está errado — P0 não está fechando.
>
> O que já foi resolvido mora em
> [`docs/technical/Historico-2026-09.md`](docs/technical/Historico-2026-09.md)
> e se consulta por `grep`. Ele já passou do teto três vezes (2.277 linhas
> em 09-19; 659 em 09-24; 348 em 09-30): o texto antigo foi arquivado lá, sem
> edição. O texto completo de cada correção abaixo está na seção
> "Arquivado do STATE.md em 2026-09-30".

---

## 🟡 02-10 — consolidação na `main` (aguarda playtest)

- Mod Menu opcional (ADR-030), overlays no cliente, Regra 48 (`GroundPickup`),
  B-4 parcial e B-5. Playtest pendente: lista no topo do `TODO.md`.

## 🟡 03-10 — medida da vila, Emenda 8 (aguarda playtest)

- Peças da vila gerada (142 × 116 → 143 × 117), 15 em volta das obras, só o
  lado que passa avança, centro num bloco; rua com 10 caminhos fora. Saves
  antigos são medidos de novo.

## 🟡 03-10, manhã — revisão das profissões (aguarda playtest)

- Regra nova do autor: material não achado em **3 buscas** aparece no baú de
  quem o usa (natureza inclusive); achado no alcance, o aldeão vai buscar.
- Pastor sem ovelha solta a tarefa; carpinteiro lê os mesmos baús da meta;
  arte em pixel desenhada no `LAST`; fundidor sem spam.
- Duas decisões abertas: ociosos sem ofício (ADR-011) e pedreiro ocioso.
  Relatório: `docs/research/2026-10-03-paradas-por-profissao.md`.

## 🟡 03-10, 01:02–01:32 — playtest (Spark `r6nErbWNSL`)

- **Desempenho ok:** TPS 20, MSPT mediano 8–12 ms, o mod é 1,9% do tick do
  servidor. As duas janelas de TPS 13–14 são a recarga de pacote de textura e
  a pausa (01:07–01:09), não o mod. Nenhuma exceção do mod no log.
- **Travamento era de fluxo, na obra:** a casa do pastor (`-292, 65, 386`)
  levou 26 min para 172 blocos. O construtor escolhia o lugar de pé dentro da
  casa fechada e ficava parado fora até o guarda de 300 tiques (6×) ou o
  `sets … aside` de 200 (dezenas). **Corrigido sem jogo:** `UnreachableSpots`
  lê o `CANT_REACH_WALK_TARGET_SINCE` do Vanilla e troca de lado em 1 s.
- **Vidraça sem areia:** a vila disse `has no sand anywhere in the radius` às
  01:05:54 e a barreira só riscou a vidraça às 01:12:02. Resolvido pela regra
  das 3 buscas (a areia aparece), não mais riscando a vidraça.
- Sinais para o próximo jogo: `gives up standing at`, `no place to stand
  within reach can be walked to`; menos `has not moved a block` de construtor.

## Em uma linha

03-10: corrigíveis sem jogo em `claude/corrigiveis-sem-jogo`, depois o
playtest da madrugada e as duas correções dele — 1.295 unitários, 579/579
GameTests, três mutações mortas. As correções do playtest ainda não foram
vistas em jogo.

## 30-09, noite — playtests 18h e 21h (Spark `YUm45D9Sw4`, `qI5h6MXtDA`)

- **E51 (`2895d71`):** a ponta da vila nascia colônia nova com BigHouseMOD e
  7 adultos — `44cd9e5a` tem 7 BigHouseMOD. Análise e o que falta em
  `docs/technical/Identidade-da-Vila-2026-09-30.md`. Não visto em jogo.
- **Caixa da vila (ADR-003 Emenda 6), 01-10:** busca de camas em coluna com
  janela de altura; caixa que cresce (construção/lote +12, rua inclui o bloco);
  identidade e fusão pela caixa; só trabalha com jogador dentro (+5 min). Aberto: limpar o save.
- **E52 (01-10):** `PenEscape` abre o portão (fecha atrás) ou pula a cerca, por rota
  própria. **E47 revisto:** o encalhado larga o ofício e `ClimbOut` sobe por escada,
  pilar ou túnel, sem desistir. Os dois vistos em jogo em 01-10.
- **01-10:** Regra 45 (baús da caixa); Emenda 7 (lote: ½ diagonal + 12, sem teto).
- **Playtest 01-10 23:12–00:17 (Spark `jPsGP2hsPo`):** TPS 20, mod 1,7%. Curral ok; pilar parou sob a grama da vila → `mayDigOut`.
- **Playtest 02-10 00:43–01:04 (Spark `LhqqBh973A`):** TPS 20, mod 1,7%; 2 casas prontas. Piso da obra nunca assentado (proteção da própria obra) → `mayBuildOver`; mineiro preso sob pedregulho → entulho; `/vc log` "travado" antigo → `WorkerStrikes.worked`. Pedidos: golem no curral, base construída, escada de madeira, roça acima da rua. Análise: `docs/research/2026-10-02-travamentos-e-tentativas.md`. **01:36 (Spark `f0wlFJg1kL`):** golens pularam a cerca; 3 mineiros encalhados 7x em y=10 saíam furando a vila → `MineReturn` (volta pelo rastro, casca de 4 blocos). **F-1/F-2 (02-10):** `MineDescent`, `EmptySweeps`, tora de casa no teto, `TaskChain`. Regras 2-e1, 49, 50 e F-3 (02-10).

## Git

- 02-10: PRs #8 e #3 mesclados com aval do autor; `main` e `codex/bighousemod`
  no mesmo commit. Funcionalidade nova: branch a partir da `main`.

## Corrigido e testado, pendente de playtest

Cada item tem teste que falhou antes da correção. Nenhum foi visto em jogo.

| Data | Correção | O que confirmar no save |
|---|---|---|
| 30-09 | Revisão das profissões: lava nunca, carpinteiro titular, viveiro do lenhador, pão, rebanho, drops automáticos, teto 256, ofício Vanilla bloqueado | `baked`, `fed two sheep`, `leaves the vanilla trade`, `lava is never placed`, `drop ingredients appear`; BigHouseMOD nova com 7 camas |
| 30-09 | Rebento não nasce no espaço de obra (viveiro e replantio) | nenhum `planted … sapling` dentro de lote com obra aberta |
| 30-09 | Busca de lote limitada pelo prazo; motivo da ponta recusada | `sweep:` com 1–2 passagens por volta; `no road end … — N motivo`; `Planner turns` e `Colony cycle took` |
| 30-09 | Placa órfã (PR #4) | nenhuma placa sobre lote sem obra; obra aberta com placa |
| 30-09 | Obra anda longe do jogador (PR #6) | `blocks left` caindo entre 64 blocos e a distância de simulação; vila pausa depois dela; `Colony cycle took` com várias vilas |
| 28-09 | Obra pede a próxima peça artesanal; troncos brutos reservados | obra com escadas ou `oak_log` sem ficar sem peça |
| 28-09 | Levantamento de baús em rodadas de 8; baú inalcançável degrada sem travar | ciclo sem 76–122 ms no levantamento; baú descarregado ao lado de baú compartilhado |
| 28-09 | Obra só reservada com ponto de apoio; fundação completa lacunas de 1 camada | fim do `WORK_STALLED` do construtor; trabalhador saindo de ponto preso |
| 27-09 | Boca seca exige degraus em solo firme; vila na água tenta escada selada | mina seca 3x3 e mina aquática |
| 27-09 | Lote validado na orientação final da planta | obra retangular ao lado de lavoura original |
| 27-09 | Construtor mede alcance pela posição física | fim da falsa caminhada a 5,1 blocos |
| 27-09 | Mina esgotada sem boca oposta espera 600 tiques | um único estado de espera por ciclo |
| 27-09 | Viveiro sem ponto livre espera 6.000 tiques | lenhador sem árvore sem custo no Spark |
| 27-09 | Desvio do mineiro mira a perna intermediária | os dois alvos antes abandonados avançam até a galeria |
| 27-09 | Rua fechada abre ramo perpendicular | vila sem ponta acha lote sem invadir rua, lavoura ou estrutura |
| 27-09 | Trabalhador legado sem casa migra para cama livre | aviso sem repetir a cada varredura |
| 27-09 | Construtor na evolução por população; déficit de camas prioriza moradia | vila com mais adultos que camas; mensagem do `/vc log` |
| 27-09 | Mineiro volta depois de cair dois blocos (pedregulho do baú) | retorno num poço real |
| 27-09 | `WaitingWork` conserva a obra com rota no bioma; `BuilderApproach` escolhe ponto livre | retomada de obra já marcada como abandonada |
| 25–26-09 | Oito correções mais antigas, ainda sem playtest | `Historico-2026-09.md`, "Arquivado do STATE.md em 2026-10-02" |

## O que o próximo jogo precisa mostrar

O roteiro completo está em [`docs/proxima-sessao.md`](docs/proxima-sessao.md).
Em ordem:

| # | Item | Sinal no log |
|---|---|---|
| 1 | Perfil de desempenho (spark) | link do `/spark profiler stop`; ver `docs/technical/Profiling-spark.md`. O de 30-09 (`hUQeDXo9U6`) não foi lido: o ambiente remoto bloqueia `lucko.me` |
| 2 | Colônia presente e ciclo mais leve | `Planner turns`, **menos** `Colony cycle took` |
| 3 | E47: o encalhado sai cavando | `is stranded at`, `dug a step`, `is out at`; nunca `cannot dig out` em massa |
| 4 | E48: casa quando falta cama, rodízio sem repetir | um segundo `the house is up` |
| 5 | N1: filhote nasce e ganha ofício | `shared supper with`; nenhum adulto aparecendo do nada depois da fundação |
| 6 | N7, N9, N10 | placa 5 blocos acima do telhado; roça ou oficina depois da 1ª casa; `finished backfilling` |
| 7 | Os 5 playtests da Task 14 | arco da mina, mina finita, baú cheio, BigHouse migrada, traço de atividade |
| 8 | Bosque fundacional | duas árvores maduras distintas a 48–56 blocos |
| 9 | Placa órfã e obra longe (PR #4 e #6) | ver a tabela acima |

Depois de jogar: `python scripts/analyze_village_log.py` (assinaturas) e
`python scripts/time_ledger.py` (tempo por profissão — Regra 50, critério de toda verificação).

## 🔴 Aberto

A lista completa e priorizada está no `TODO.md`.

- **R1, desempenho.** Prazo de 15 ms e cota ajustável no código; falta medir
  em jogo, agora também com a execução em toda colônia `ACTIVE`. É o único
  critério da avaliação com nota 1.
- **`surface_worker_unreachable`** (7 no log de 28-09): reproduzir em
  GameTest antes de mudar a coleta.
- **Estado global (R2).** `ServerMemory` garante a limpeza, mas os 89 campos
  estáticos mutáveis continuam; consolidá-los é o que falta para o C05.
- **Itens 9 e 10 (ciclo de tarefa comum aos ofícios; regras de decisão
  para o `core`).** Pedem ADR antes do código.
- **ADR-025 fase 3** (veios por valor), depois do playtest das fases 1 e 2.
  Ver `docs/research/2026-09-25-mineiro-autonomo.md` §8-§11.
- **Decisões do autor** (ver `TODO.md`, "Decisões que faltam", e
  `docs/research/2026-09-25-decisoes-simples.md`): E43, TASK-048, TASK-044
  (fusão; a ADR-007 está aceita e não implementada), TASK-046, E38, E45.
- **Sessão longa de 26-09** (`docs/research/2026-09-26-sessao-longa.md`):
  lote que não cresce (§7.1; o motivo da rua sai no próximo log), obra
  largada sem blocos prendendo o lote (decisão) e aldeão ocioso preso.
- **Mineiro que não entrega (E44/E45)** e **segunda obra que não abre:**
  sem playtest novo desde as correções; detalhe no `Historico`.

## Dívida conhecida

- **Nenhum arquivo de produção acima de 500 linhas** desde 02-10 (os oito
  foram divididos); a lista congelada do `FileSizeRuleTest` está vazia.
- **Hooks do Claude Code:** os scripts estão em `scripts/hooks/`; quem liga
  no `.claude/settings.json` é o autor.
- **Bateria de jogo:** 3 testes intermitentes isolados em 24-09; a taxa
  histórica era ~1 falha a cada 8 rodadas. `runGametest` não filtra teste.
  O timeout isolado do construtor alcançando o topo (27-09) não se repetiu.
- **`ColonyDetectionGameTest`:** a falha de 09-19 (24 trabalhadores em vez
  de 30) nunca foi reproduzida.
- **Cobertura da bateria de jogo** medida desde 02-10 (`runGametest` →
  `build/reports/jacoco/gametest`): `fabric/work` 83%, `integration` 84%;
  furos em `command` (3%), `network` (20%) e cliente (0%, sem cliente no teste).
- **GameTest com jogador** desde 02-10 (`FakePlayer` no mundo): o foco e a
  passagem extra da busca de lote já são testados com jogador dentro e longe.

## Como avaliar e investigar

- **Avaliação técnica:** `docs/technical/avaliacao/METODOLOGIA.md`, rodada
  por `python scripts/assess/assess_project.py --run --gametest-runs 2`. A
  última deu B, 3,21 de 4.
- **Instrumentar antes de consertar.** Três defeitos de 09-19 se decidiram
  numa única leitura depois de instrumentados.
- **Quando a mesma causa reaparece em vários itens, desconfie da
  ferramenta.**
- **Ferramentas:**

  | Ferramenta | Para quê |
  |---|---|
  | `scripts/analyze_village_log.py` | assinaturas e peças esperadas no log |
  | `scripts/verdict.py` | veredito por item pendente |
  | `ChainRootsGameTest` | onde cada cadeia de produção começa |
  | `StructureCoverageGameTest` | quem fabrica cada peça |
  | `CraftReasons`, `VolumeSample`, `ProtectionSample` | por que algo não saiu ou foi recusado, no log |

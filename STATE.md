# STATE — 2026-09-26

> Arquivo de estado vivo. **Sobrescreve, não acumula.**
> Se passar de 150 linhas, algo está errado — P0 não está fechando.
>
> O que já foi resolvido mora em
> [`docs/technical/Historico-2026-09.md`](docs/technical/Historico-2026-09.md)
> e se consulta por `grep`. Ele já passou do teto duas vezes (2.277 linhas
> em 09-19; 659 em 09-24): o texto antigo foi arquivado lá, sem edição.

---

## Em uma linha

O código está à frente do jogo. Desde o último playtest (24-09, madrugada)
entraram E47/E48, a revisão de naturalidade, a rodada de qualidade, a
refatoração e a **vila foco**. Nada disso foi visto em jogo ainda. A
**próxima sessão de jogo** é o que destrava o resto.

## Versão publicada

- **JAR republicado em 26-09, SHA-256 `C9568D4B…1601E`:** alternativa A
  entrega a obra totalmente adiada depois da paciência sem liberar o lote e a
  ADR-008 conserva e gira o `facing` horizontal da estrutura. Para peça de
  manufatura que nenhuma profissão consegue recolher ou fabricar, a terceira
  tentativa entrega o item no baú do construtor ou, se ausente/cheio, em outro
  baú livre da colônia. Recursos naturais continuam responsabilidade dos
  ofícios. `test --rerun-tasks` e `runGametest --rerun-tasks`: 474/474;
  ainda falta validar os fluxos no save.

- **JAR republicado em 26-09 (manhã), commit `70af4c8`, SHA-256 `9EB559D1…9423`:**
  tudo de 26-09 (lenhador, viveiro, baú da cama, tapete, relógio, desvio, casa
  na altura da rua). Não visto em jogo.
- O JAR em `mods` e em `downloads/` foi republicado em 25-09 à tarde, do
  commit `a222342`, SHA-256 `8862EC4F…07C0`: **ADR-025 fases 1 e 2** — marca
  de recusa salva, piso sob a passagem, mineiro fora da água, pedra com
  líquido atrás nunca vira alvo, desvio que cava e põe bloco (mineiro travado
  e encalhado sem escada), linha `brain:` no travamento. Ver `CHANGELOG.md`.
- Build limpo e 1112 unitários verdes; GameTest 455/455 numa rodada; PIT
  1265/1430 (88%), força 96%; no pacote novo só 5 sobreviventes, todos
  equivalentes.
- A publicação anterior (`6923E840…AC8C`, manhã de 25-09) trouxe a obra
  abandonada na vez do tipo e o mineiro cavando sem parar.
- **Sessão de 25-09, 09:13:** o templo de 539,70,201 **fechou** às 09:34 (as
  nove peças voltaram às 09:22); em seguida o reparo reabriu o templo
  abandonado de z=211 — corrigido acima. O mineiro ficou preso a y=41 na
  mesma pedra inalcançável e fora da escala o resto da sessão (E44/E45).
- **Sessão de jogo de 25-09 (01:19–01:45):** TPS 20 o tempo todo; o mod caiu
  de 4,1% para 0,3% da thread do servidor; vila foco escolhida e só um
  `Colony cycle took` (na entrada, 165 ms; eram 196 no log de 24-09). O
  construtor parou no templo por falta de tocha — a vila não tinha carvão.
- O PIT esteve parado do `78e7efc` ao `7619b1d`, calado pelo
  `continue-on-error` do CI — corrigido em 25-09, e o CI agora reprova
  quando o PIT nem começa.
- PR #2 levou o branch para a `main`; o **PR #3** (desde então) está aberto.

## O que o próximo jogo precisa mostrar

O roteiro completo está em [`docs/proxima-sessao.md`](docs/proxima-sessao.md).
Em ordem:

| # | Item | Sinal no log |
|---|---|---|
| 1 | Perfil de desempenho (spark) | link do `/spark profiler stop`; ver `docs/technical/Profiling-spark.md` |
| 2 | Vila foco e ciclo mais leve | `Focus village is now`, `Planner turns`, **menos** `Colony cycle took` |
| 3 | E47: o encalhado sai cavando | `is stranded at`, `dug a step`, `is out at`; nunca `cannot dig out` em massa |
| 4 | E48: casa quando falta cama, rodízio sem repetir | um segundo `the house is up` |
| 5 | N1: filhote nasce e ganha ofício | `shared supper with`; nenhum adulto aparecendo do nada depois da fundação |
| 6 | N7, N9, N10 | placa 5 blocos acima do telhado; roça ou oficina depois da 1ª casa; `finished backfilling` |
| 7 | Os 5 playtests da Task 14 | arco da mina, mina finita, baú cheio, BigHouse migrada, traço de atividade |

Depois de jogar, rodar `python scripts/analyze_village_log.py`, que conta
todas essas assinaturas.

## 🔴 Aberto

A lista completa e priorizada está no `TODO.md`, nas seções "Pendências de
correção levantadas pela avaliação" e "Avaliação técnica". Em aberto:

- **R1, desempenho.** Correção feita (vila foco, prazo de 15 ms, cota
  ajustável); falta medir em jogo. É o único critério da avaliação com nota 1.
- **Estado global (R2).** A limpeza já é garantida pelo `ServerMemory`
  (item 3, feito em 24-09), mas os 89 campos estáticos mutáveis continuam
  — consolidá-los num contexto por servidor é o que falta para o C05.
- **PIT: C08 alcançado em 25-09** — 1151/1312 mortas (87,73%), força 95%,
  50 sobreviventes. Zerados ou só com equivalentes: `MineShaft`,
  `Building`, `ColonyCycle`, `Worker`, `ProfessionAssigner`, `Mine`,
  `ColonyGoals`, `BuildingRegistry`, `ConstructionProject`, `ColonyRoads`,
  `VacancyEnforcer`, `HiringLog`. Nenhuma classe passa de 4.
- **Itens 9 e 10 (ciclo de tarefa comum aos ofícios; regras de decisão
  para o `core`).** Pedem ADR antes do código.
- **ADR-025 aceita (mineiro autônomo), fases 1 e 2 no código, não vistas em
  jogo:** marca de recusa salva, piso sob a passagem, mineiro fora da água,
  pedra com líquido atrás nunca vira alvo, linha `brain:` no travamento, e o
  desvio que cava e põe bloco (mineiro travado e encalhado sem escada). A geometria de 553, 39, 158 reconstruída do
  save é andável (GameTest forense) — a causa do travamento ali está no
  cérebro ou na tarefa, e a linha `brain:` da próxima sessão decide. Depois:
  fase 3 (veios por valor). Ver
  `docs/research/2026-09-25-mineiro-autonomo.md` §8-§11.
- **Sessão de jogo de 26-09 (00:02–00:40), JAR `8862EC4F…07C0`:** TPS 20,
  mod ~0,5% da thread. Mineiro novo funcionou (10 desvios, 8 concluídos; 5
  encalhados saíram; 15 vãos com piso). Achados corrigidos no mesmo dia, com
  teste e não vistos em jogo: lenhador expulso procurando árvore, viveiro
  lento (lote de 4), baú da cama (regra b do autor), tapete verde pela receita
  de tingir, relógio de espera salvo, e dois defeitos do desvio (o próprio
  corpo no degrau; queda sem replanejar). Build 1122 unitários; GameTest
  464/464; PIT 1277/1442.
- **Sessão de jogo de 26-09 (01:52–02:55), ainda com o JAR `5b98…`/`8862EC4F`**
  (as correções da madrugada não estavam nele): a `plains_small_house_5` fechou
  sobre um monte de terra, piso em 67 e porta em 68 com a rua em 63–65. Causa
  e correção: camada da rua (`Blueprint.streetLayer`, `BuriedPieces`). Spark
  `mmw9xhgKqL`: TPS 20, mod ~0,3%; a janela de TPS 8,2 é pausa do jogo.
- **Sessão longa de 26-09 (03:22–08:14)**, auditada em
  `docs/research/2026-09-26-sessao-longa.md`: uma casa em 2 h e 2h51 sem obra.
  Causa principal: lenhador, mineiro e pedreiro sem baú. Corrigido no código
  (não visto em jogo): baú para todo aldeão de profissão e salvo, peça pronta só
  de manufatura, pastor/fazendeiro contínuos, fundidor sem busca inútil, guarda
  de alcance com a rua do lote. Pendentes: lote que não cresce (§7.1), obra
  largada sem blocos prender o lote (decisão), aldeão ocioso preso.
- **Decisões em aberto têm resposta simples proposta**, e duas travas foram
  achadas na varredura: obra com todas as peças restantes adiadas nunca fecha
  (`WaitingWork.giveUpIfStalled`) e encalhado sem saída fica fora da escala
  para sempre. Aguardam o autor. Ver
  `docs/research/2026-09-25-decisoes-simples.md`.
- **Mineiro que não entrega (E44/E45)** e **segunda obra que não abre.**
  Estado de 09-20, sem playtest novo desde as correções; o detalhe está no
  `Historico`, seção "Arquivado do STATE.md".

## Dívida conhecida

- **Hooks do Claude Code:** os scripts estão em `scripts/hooks/`; quem liga
  no `.claude/settings.json` é o autor (a escrita pelo agente foi recusada).
- **Presença da vila foco** não vai para o save. Ao reabrir o mundo, o foco
  se refaz em poucos ciclos.
- **Bateria de jogo:** 3 testes intermitentes foram isolados em 24-09. A
  taxa histórica era de ~1 falha a cada 8 rodadas, e só a repetição prova
  que acabou. `runGametest` não filtra teste.
- **`ColonyDetectionGameTest`:** a falha de 09-19 (24 trabalhadores em vez
  de 30) nunca foi reproduzida nem diagnosticada.
- **Cobertura da camada `fabric`:** não é medida, porque o JaCoCo não
  instrumenta a bateria de jogo (item 5 das pendências).

## Como avaliar e investigar

- **Avaliação técnica:** a metodologia está em
  `docs/technical/avaliacao/METODOLOGIA.md` e roda com
  `python scripts/assess/assess_project.py --run --gametest-runs 2`. A
  última deu B, 3,21 de 4.
- **Instrumentar antes de consertar.** Três defeitos de 09-19 se decidiram
  numa única leitura depois de instrumentados.
- **Quando a mesma causa reaparece em vários itens, desconfie da
  ferramenta,** e não conclua que houve várias regressões.
- **Ferramentas:**

  | Ferramenta | Para quê |
  |---|---|
  | `scripts/analyze_village_log.py` | assinaturas e peças esperadas no log |
  | `scripts/verdict.py` | veredito por item pendente |
  | `ChainRootsGameTest` | onde cada cadeia de produção começa |
  | `StructureCoverageGameTest` | quem fabrica cada peça |
  | `CraftReasons`, `VolumeSample`, `ProtectionSample` | por que algo não saiu ou foi recusado, no log |

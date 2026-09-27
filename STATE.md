# STATE — 2026-09-27

> Arquivo de estado vivo. **Sobrescreve, não acumula.**
> Se passar de 150 linhas, algo está errado — P0 não está fechando.
>
> O que já foi resolvido mora em
> [`docs/technical/Historico-2026-09.md`](docs/technical/Historico-2026-09.md)
> e se consulta por `grep`. Ele já passou do teto duas vezes (2.277 linhas
> em 09-19; 659 em 09-24): o texto antigo foi arquivado lá, sem edição.

---

## Em uma linha

A boca seca da mina agora so abre quando os tres primeiros degraus das tres
faixas da escada podem sair por solo firme, portanto uma coluna alta isolada
nao vira uma entrada suspensa. Quando uma mina esgota todos os niveis, uma
vila fundada em agua tenta antes uma escada selada de tres lances ate uma
saida natural 8x8; se ela nao for segura ou carregada, a reabertura seca do
lado oposto continua sendo usada. Os mineiros nao podem quebrar seus degraus
nem o casco de vidro. O planejador
tambem envia ao scanner a pegada que cada planta tera depois de virar para a
rua: lote com lavoura ou estrutura passa a ser recusado antes de registrar a
obra. `runGametest --rerun-tasks` passou em 488/488. Falta confirmar em um
save real uma mina seca 3x3, uma mina aquatica e uma obra retangular ao lado
de uma lavoura original.

A auditoria do log do save de 27-09 encontrou uma mina esgotada cuja boca
oposta nao podia ser aberta. A recuperacao nao repete mais a busca e o aviso a
cada tique: ela tenta imediatamente e, se o mundo ainda recusar a boca, espera
600 tiques antes da proxima tentativa. Minas secas, escadas 3x3, protecoes e o
acesso submerso permanecem inalterados. `MineBottomRetryTest` e
`runGametest --rerun-tasks` passaram em 488/488; falta conferir no save que a
mina aguardando uma boca oposta emite apenas um estado de espera por ciclo.

A evolucao por populacao agora inclui o Construtor na mesma lista de vagas
permanentes: o segundo aparece no adulto 23 e o terceiro no 38, sem mistura-lo
ao calculo de necessidade de recursos. Qualquer deficit observado de camas,
inclusive zero camas, obriga a proxima obra a ser uma moradia cuja planta tem
cama. A prioridade agora e explicita no Core e o `/vc log` a explica antes das
atividades. Os dois bloqueios de `FarmPlanGameTest` eram fixtures com vinte
adultos e zero camas observadas; a fixture corrigida e a rodada Fabric passaram
em 485/485. Falta validar no save real uma vila com mais adultos que camas e a
mensagem do diagnostico.

A varredura de 27-09 confirmou o retorno do mineiro depois de cair dois
blocos: ao sair de uma rota, o desvio replaneja do fundo do poco e usa dois
pedregulhos do seu bau para formar os degraus de volta a superficie. O novo
`DetourWalkerGameTest` passou na bateria Fabric. Falta observar esse retorno
em um poco real no save do autor, com pedregulho no bau do mineiro.

O playtest de 27-09 isolou as duas obras pendentes: uma casa pequena foi
abandonada enquanto aguardava `oak_log` que os lenhadores locais ainda podiam
obter, e a casa do pastor recebeu um ponto de trabalho fixo que nao produziu
nenhum passo de rota. `WaitingWork` agora conserva a obra enquanto o proximo
bloco tiver rota profissional no bioma; `BuilderApproach` escolhe o ponto
livre mais proximo dentro do alcance de construcao. A rodada Fabric passou em
483/483. Falta confirmar o comportamento no save real, inclusive a retomada
de uma obra que ja tenha sido marcada como abandonada.

O playtest de 26-09 confirmou camas e baús ao ar livre. A correção publicada só
adota uma vila nova depois de colocar a BigHouseMOD, funda moradores somente
nela e só aceita baú em cômodo fechado e coberto; 480/480 GameTests passaram.
O JAR foi instalado nas três cópias; falta validar no mesmo save que novas vilas não deixam cama
ou baú fora de estruturas.

A correção em validação restringe a simulação à vila onde há jogador agora:
sem jogador, nenhum trabalho por tique roda; longe do jogador, detecção,
planejamento, profissões, refeições, fuga e placas ficam pausados.
O bloqueio da bateria foi isolado e corrigido: o retorno sem jogadores também
impedia os tiques de profissões da arena GameTest. Uma ponte exclusiva da
fonte `gametest` executa esses tiques sem alterar a pausa de produção. A
rodada Fabric de 26-09 passou em 480/480; `./gradlew.bat build` também passou.

O menu de diagnóstico `/vc log` foi adicionado nesta sessão: no chat, perto da
vila, ele traduz o último estado de cada profissional em ativo, aguardando ou
travado. A implementação passou nos testes unitários próprios e na bateria
Fabric; ainda falta validar ambos no save real. A auditoria completa está em
[`docs/technical/Auditoria-Simulacao-2026-09-26.md`](docs/technical/Auditoria-Simulacao-2026-09-26.md).

O playtest de 26-09 também confirmou que uma fazenda já finalizada era reaberta
como reparo e parecia uma construção invisível. Construções concluídas agora
não reabrem pela varredura nem por uma pendência legada do save; somente obras
abandonadas podem retomar na vez do seu tipo. `test` passou em 1145/1145 e
`runGametest --rerun-tasks` em 481/481. Falta validar no mesmo save que a
pendência antiga é descartada e a fazenda não volta a ter blocos quebrados.

## Correção publicada, pendente de playtest

- **Fundação atômica, camas e baús somente dentro de estrutura (26-09):** uma
  vila inédita não entra no registro até a `BigHouseMOD` caber em lote seguro;
  nessa mesma criação ela recebe as seis camas, seis baús e moradores. Sem
  lote, a próxima detecção tenta a adoção inteira novamente. `VillageFoundation`
  não cria camas nem moradores sem casa, e `ChestPlacer` exige cama, teto e
  cômodo horizontalmente fechado dentro da peça de vila ou construção
  registrada; as aproximações da porta continuam bloqueadas. O novo
  `aVillageWithoutASafeBigHouseLotIsNotAdopted` falhou contra a regra antiga,
  junto dos dois cenários de cama/baú externo. A rodada completa
  `runGametest --rerun-tasks` passou com 480/480. O JAR
  `C1D41213…32B7A` foi comparado em `build/libs/`, `downloads/` e
  `%APPDATA%/.minecraft/mods/`. Itens já existentes no mundo não são removidos
  para não destruir inventários; falta confirmar em jogo.

## Versão publicada

- **JAR atualizado em 26-09, SHA-256 `478303C8…24F1`:** bosque
  fundacional para o lenhador. Vila nova recebe duas árvores maduras e
  distintas do bioma a 48–56 blocos; cada dez adultos vivos tentam uma árvore
  adicional sem avançar a dezena quando não há posição segura carregada.
  `ColonySavedDataTest` (18), `build` e `runGametest --rerun-tasks`
  (477/477) passaram; falta o playtest no save.

- **JAR republicado em 26-09, SHA-256 `46CF0C7A…6444`:** obra só abre quando
  cada coluna da pegada está no nível da rua; não há aterro automático. O baú
  de profissão só nasce ao lado da cama, dentro de uma peça de vila vanilla ou
  construção finalizada registrada pela colônia, e a regra existente continua
  proibindo a frente da porta. `clean build` e `runGametest --rerun-tasks`:
  477/477. Falta validar no save.

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
| 2 | Colônia presente e ciclo mais leve | `Planner turns`, **menos** `Colony cycle took`; nenhuma atividade de vila distante |
| 3 | E47: o encalhado sai cavando | `is stranded at`, `dug a step`, `is out at`; nunca `cannot dig out` em massa |
| 4 | E48: casa quando falta cama, rodízio sem repetir | um segundo `the house is up` |
| 5 | N1: filhote nasce e ganha ofício | `shared supper with`; nenhum adulto aparecendo do nada depois da fundação |
| 6 | N7, N9, N10 | placa 5 blocos acima do telhado; roça ou oficina depois da 1ª casa; `finished backfilling` |
| 7 | Os 5 playtests da Task 14 | arco da mina, mina finita, baú cheio, BigHouse migrada, traço de atividade |
| 8 | Bosque fundacional | ao criar uma vila, duas árvores maduras distintas a 48–56 blocos; a cada 10 adultos, só uma árvore adicional por ciclo |

Depois de jogar, rodar `python scripts/analyze_village_log.py`, que conta
todas essas assinaturas.

## 🔴 Aberto

A lista completa e priorizada está no `TODO.md`, nas seções "Pendências de
correção levantadas pela avaliação" e "Avaliação técnica". Em aberto:

- **R1, desempenho.** Em validação: só a vila com jogador atual roda; prazo de
  15 ms e cota ajustável permanecem. Falta medir em jogo. É o único critério
  da avaliação com nota 1.
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
- **Bosque fundacional (código local, ainda sem playtest):** a criação de uma
  colônia tenta duas espécies maduras adequadas ao bioma no anel de 48–56
  blocos. A cada dez moradores adultos vivos, o ciclo tenta uma árvore a mais
  e só grava a dezena após a geração física. Não carrega chunks nem substitui
  copa ocupada, estrutura ou baú. `ColonySavedDataTest` (18) e
  `runGametest --rerun-tasks` (**477/477**, 1m05s) passaram; falta confirmar
  no save a aparência do bosque e o lenhador encontrando seus troncos.
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
- **Ativação por presença atual:** não há foco persistido. A produção, a
  sondagem e o planejamento só avançam para colônias com jogador dentro do
  raio de detecção; ao sair, o trabalho daquela colônia pausa.
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

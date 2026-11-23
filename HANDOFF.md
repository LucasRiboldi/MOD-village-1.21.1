# Handoff — Village Colony

Ponto de retomada para Codex ou Claude Code. Confira a branch, o estado do Git e os PRs abertos antes de trabalhar. O handoff da Fase 3 do MAE está no histórico do Git.

## Candidata local 0.4.124 (2026-11-23)

- A branch `codex/playtest-04124` reúne o resgate de telhado (`c8b32ace`) e a correção do rebanho (`07afdcdc` na branch `codex/pastor-rebanho-nos-currais`, incorporado como `cd6924f9`).
- A coleta e a entrega registram os animais por UUID. A reprodução de corte usa pares do mesmo curral. O abate usa a meta vigente no total da vila e a reserva do curral da vítima.
- JAR instalado em `downloads/` do checkout do Desktop e na pasta `mods/` do TLauncher. As cópias e o build têm SHA-256 `53E341158D3963E0D1A7724A1B0AA0394593E906CA4C190E3AE07E128AC43FF8`. A 0.4.123 ficou como cópia desativada.
- No Sentinela `fast` (`20261123T170632Z`), unitários e propriedades passaram. Dos 1.247 GameTests, 1.244 passaram. Falharam a tosquia N05 e dois cenários de coleta de `ShepherdRoundGameTest`. Na repetição focada de rebanho, telhado, ronda e tosquia, os 20 passaram. O log confirmou nascimento e morte por queda do mesmo filhote, e abate com dois adultos preservados no curral da vítima.
- Retome pelo [roteiro do rebanho](docs/technical/Playtest-0.4.122-2026-11-23.md#próximo-playtest-do-rebanho) e pelo [TODO](TODO.md). A sessão anterior não tinha informação suficiente para explicar toda a perda de animais.

## Candidata local 0.4.123 (2026-11-23)

- Na branch `codex/resgate-telhado-04123`, a opção 1 protege o resgate de telhado. Um salto curto usa a distância segura de queda do aldeão. Para o desnível alto, o resgate o coloca em um piso natural ou rua com espaço livre, zerando a velocidade e a distância de queda.
- A regressão de oito blocos falhou antes da mudança. Os oito GameTests focados passaram após a correção, e o Sentinela `fast` passou nas três suítes (`20261123T152335Z`, 1.244 GameTests).
- Use o [roteiro do telhado](docs/technical/Playtest-0.4.122-2026-11-23.md) para conferir a chegada ao chão e a volta ao trabalho. O [TODO](TODO.md) mantém a investigação da subida e do retorno.

## Candidata 0.4.122-alpha (2026-11-23)

- A entrega reúne a correção do grafo e as mudanças do pastor da série 0.4.119–0.4.121. Os 17 GameTests do grafo passaram na candidata. Consulte as [notas da entrega](docs/releases/v0.4.122.md) e o [TODO](TODO.md) para a próxima rodada.
- Confirme as três colunas observadas na 0.4.118 e os casos originais da mina e da casa do mineiro. Siga o [roteiro de jogo](docs/technical/Playtest-0.4.118-2026-11-22.md).
- O [CI do PR #104](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/runs/45101176577) reproduziu N26 no desnível lateral. Após reposicionar o pastor, o teste passou na bateria local completa. O N26 passou nas duas rodadas do CI de `1a841653`. A primeira rodada falhou na tosquia (N05) e a segunda na travessia entre redes viárias após fusão de colônias. Confira o [PR #104](https://github.com/LucasRiboldi/MOD-village-1.21.1/pull/104) para o estado da integração.
- O JAR de `1a841653` foi instalado no TLauncher e em `downloads/` com SHA-256 `2CFB3FD629A42E03C19AF44064313CD1A815BD9B54D55123D07195BD7B85396B`. O [playtest de 23-11](docs/technical/Playtest-0.4.122-2026-11-23.md) usou o JAR anterior. Prioridade em jogo é impedir as mortes no resgate do telhado. O [TODO](TODO.md) mantém os demais pontos.

## Histórico da candidata local 0.4.121 (2026-11-22)

Na branch `codex/pastor-curral-04119`, `FlockHarvest` exige três adultos da espécie no curral da vítima e preserva dois. A reserva de seis na área da vila continua valendo. `FlockHarvestGameTest` cobre dois currais, animais soltos, outra espécie e um filhote. O [roteiro da candidata](docs/technical/Playtest-0.4.118-2026-11-22.md) cobre a validação em jogo e a investigação da queda na contagem.

Os testes da reserva passaram. A rodada focada confirmou na 0.4.120 a falha de guia longe da porteira. O cenário da vaca no desnível lateral falhou na bateria completa e passou isolado. Consulte o `STATE.md` antes de mexer na condução.

## Histórico da candidata 0.4.120 (2026-11-22)

A opção 3 do autor está em `ShepherdDelivery` na branch `codex/pastor-curral-04119`. A física do salto reutiliza `PenMoves`. A medição do curral continua em `ShepherdPens`. A confirmação no mundo do autor está pendente. O roteiro do relatório da 0.4.118 segue na candidata atual.

## Histórico da candidata 0.4.119 (2026-11-22)

- A branch `codex/pastor-curral-04119` parte de `606eb0f6`. A coleta, a separação e o abate respeitam
  recintos cobertos com porta. A navegação do animal alinha a entrada da porteira enquanto a guia continua presa.
- Sentinela `fast` passou com 1.886 testes unitários, uma suíte de propriedades e 1.241 GameTests (`20261123T004749Z`).
- O [relatório do jogo da 0.4.118](docs/technical/Playtest-0.4.118-2026-11-22.md) contém as seis notas e o roteiro
  da próxima sessão. Priorize confirmar a condução e cruzar a contagem do curral grande com `VC_FLOCK_HARVEST`.
  As entradas de casa e a autoria de blocos na obra continuam na fila.

## Histórico da entrega 0.4.118 (2026-11-22)

- No Claude Code, atualize a `main` antes de retomar. O PR #102 integrou o Lote A
  (`eb222eba`, N12/N28) e a orientação da placa do baú (`a9999d09`, N14).
  A correção do pastor (N05/N26/N27) já estava na `main` pelo PR #101.
- `remapJar` passou em `91241a66`. O JAR da 0.4.118 foi instalado como único ativo no
  TLauncher. Os arquivos em `build/libs/`, `downloads/` do checkout do
  desktop e `mods/` conferem com SHA-256
  `ED7D96FEC95A398C08E7E8DECDA881713E07CC5003D09995BC7D5651E4E6C4F5`.
- O autor dispensou novos testes locais nessa entrega. As
  [notas da 0.4.118](docs/releases/v0.4.118.md) registram as evidências e o roteiro.
- Continue pelo [plano do playtest](docs/technical/Plano-Playtest-0.4.115-2026-11-21.md)
  e pelo [TODO](TODO.md). Confirme N05/N12/N14/N26/N27/N28 no jogo da 0.4.118.
  O atraso do baú segue aberto. N23 (cores das placas) segue pendente.

## Histórico do N14

- A branch local `codex/n14-placa-bau` contém a correção da placa e saiu da `main` em
  `6aa5cacf`. O atraso na criação do baú continua aberto. N10 aguarda o Lote A e N24
  requer decisão separada.
- O roteiro está no [plano do playtest](docs/technical/Plano-Playtest-0.4.115-2026-11-21.md#n14--roteiro-da-placa-do-baú).
  Os focados `ChestMarkerGameTest` e `BedChoiceGameTest` passaram (17 casos).
  Sentinela `fast`: 1.886 unitários, uma suíte de propriedades e 1.238 GameTests
  (`reports/test-runs/20261122T002039Z/`, relatório local).
- Para montar a candidata local 0.4.118, acrescente à revisão desta branch apenas o delta
  de `6aa5cacf..eb222eba` sob `src/` em uma área de build temporária. Isso inclui a
  reconexão da 0.4.117 (Lote A, N12/N28) e seu teste. As duas branches seguem separadas.
- A combinação passou em `build`, `remapJar` e 25 GameTests focados. A 0.4.118 é o
  único JAR ativo do mod no TLauncher. A 0.4.117 fica como `.jar.disabled`.
  O SHA-256 em `build/libs/`, `downloads/` e `mods/` é
  `ed7d96fec95a398c08e7e8decda881713e07cc5003d09995bc7d5651e4e6c4f5`.

## Histórico da reconexão 0.4.117 e do pastor 0.4.116

- **Prioridade atual:** validar em jogo a reconexão da mina e da casa do mineiro na 0.4.117 (Lote A, N12/N28).
  A candidata inclui a correção do pastor (N05/N26/N27) da 0.4.116.
  As 28 notas, seis imagens e demais pendências estão em `docs/technical/Plano-Playtest-0.4.115-2026-11-21.md`;
  o relatório é `docs/testing/relatorios/jogo-0.4.115-2026-11-21.md`. Não descartar os outros lotes.
- **Branch de retomada:** `codex/lote-a-reconexao`, criada sobre a `main` com a 0.4.116 integrada no PR #101.
  A cópia usada para gerar o JAR fica em `C:/Users/lucas/Desktop/MOD-village-1.21.1`.
- **Prova da 0.4.117:** a recusa antiga bloqueou a única passagem seca em GameTest. Com a correção,
  os 16 casos focados passaram. `fast` PASS em `reports/test-runs/20261121T233603Z/`.
- **Prova da 0.4.116:** `fast` PASS em `reports/test-runs/20261121T191357Z/`: 1.885 unitários,
  uma suite de propriedades e 1.237 GameTests. Build passou; 196 testes Python passaram.
  Mutação para a produção 0.4.115: cinco regressões falharam em `20261121T191211Z`; 23 focados verdes em `20261121T191032Z`.
  A revisão foi concluída, inclusive cleanup antecipado da arena. A análise histórica está em
  `reports/sentinel-analysis/20261121T191903Z/`; inclui falhas antigas e mutações, não é um relatório sem achados.
- **Artefato da 0.4.117:** use `village-colony-0.4.117.jar` para o próximo jogo. A entrega usa `remapJar`,
  `downloads/` e a pasta `mods` do TLauncher, com comparação de SHA-256.
- **Próximo passo:** seguir o roteiro do plano para mina, casa e pastor. Ao final do jogo, use `/vc salvar`.
  Desnível entre casas, autoria do corte no arco, demais itens do Lote A e lotes C, D e E seguem no plano.
- **Regras do autor:** relato de que entrou no jogo/testou sempre usa `vc-log-driven-fix`.
  Toda versão nova deve atualizar `downloads/` e `%APPDATA%/.minecraft/mods`, com SHA-256 igual ao build
  e apenas um `village-colony-*.jar` ativo; anteriores ficam `.jar.disabled`.
- **Candidata 0.4.115:** resgate de telhado limpa a memória de caminhada anterior. O teste da memória falhou antes
  e passou depois; Sentinela `fast` passou em `reports/test-runs/20261121T143122Z/`. Confirmar em jogo se o aldeão
  deixa de retornar ao telhado; a causa completa do retorno ainda não está comprovada.
- **Candidata local 0.4.114:** diário conserva os motivos de entrada e de saída, e `/vc salvar` mostra
  as mudanças recentes, sono e destino. A mina espera no corte que segura líquido, e a união de
  colônias reaproveita as duas redes. Sentinela `fast` passou com 1.232 GameTests
  (`reports/test-runs/20261121T125319Z/`); análise em `reports/sentinel-analysis/20261121T125835Z/`.
  `build` e `remapJar` passaram. Cópias em `build/libs/`, `downloads/` e `mods/` com SHA-256
  `10ebc1116c15bb8fda82bd23feaf876dbb3848fa7961930b769ce765ab17dfce`. A 0.4.113 foi desativada. Falta o jogo do autor.
- **Plano do ciclo:** `docs/technical/Proximo-ciclo-0113.md` — as nove escolhas (A8, A7, A9, A6, A2, A3a, A4, A5, R1) estão implementadas. Próxima verificação em mundo novo, com `/vc salvar` antes de sair.
- **R1, candidato (Regra 346):** relatório da sessão com a tabela completa de blocos, mina, obras, último grafo, janelas de tempo e eventos da colônia. GameTest focado passou e detectou a restauração do limite de 15 linhas.
- **A5, candidato:** desistência do pastor registra posições da ovelha e do pastor e imobilidade ou prazo esgotado. Teste falhou antes da mudança e sob mutação; quatro GameTests focados e Sentinela `fast` passaram. Relatório em `reports/test-runs/20261121T064752Z/`. Confirmar o diagnóstico no próximo jogo.
- **A4, candidato:** desistência do fazendeiro registra canteiro, posição do aldeão e imobilidade ou prazo esgotado. Teste falhou antes da mudança e sob mutação; três GameTests focados passaram. Sentinela `fast` passou em `reports/test-runs/20261121T063830Z/`. Confirmar o diagnóstico no próximo jogo.
- **Histórico da 0.4.113:** `fix/miolo-pelas-pecas` reunia também a 0.4.111 (Regra 347) e a 0.4.112 (Regra 348).
- **Entrega local:** candidata 0.4.113 em `downloads/` e no TLauncher, com a 0.4.112 desativada. SHA-256 igual nas três cópias, `928eaaf4e1a2a63525a7e14b1def77fa584be01ac27487476a1e523ea5afae99`. Sentinela `fast`, análise e build passaram. O Gradle foi encerrado.
- **A8, candidata (Regra 349):** quatro reservas reduzidas, `ingot` removido só da medição e `wool` preservado. Cinco regressões falharam com a mutação, e os 21 GameTests focados passaram com a regra restaurada.
- **Sentinela `fast` após o A9:** unitários, propriedade e 1.225 GameTests passaram na terceira execução. Relatório local em `reports/test-runs/20261121T045420Z/`. As duas primeiras execuções estão registradas no plano.
- **A7, candidata (Regra 350):** o cenário registra dois baús públicos com 256 terras no total. Sem o filtro, o aldeão recolhe terra com o estoque no teto. Com o filtro, os três GameTests focados passaram, cobrindo também a coleta de pedregulho e a retomada da coleta de terra abaixo de 256.
- **Análise do último jogo:** `docs/audit/2026-11-21-analise-jogo-0111.md` (achados A1 a A9, R1).
- **Linha de base restabelecida em 21-11:** `gradlew.bat build` passou com JDK 21 após isolar `earthAboveTheCeilingStaysOnTheGround` no lote `ground_pickup_earth_ceiling`.
- **A9, candidata (Regra 351):** os oito GameTests focados e os 196 testes Python passaram. Grama e caminho sem autor declarado saem do conflito. Entorno de obra e piso da mina têm autoria explícita.
- **A6, candidata (Regra 352):** sem argila disponível, a obra pede tijolos de pedra e o estoque exclui terracota. A retomada preserva peças assentadas. Os dois testes novos falharam sob mutação e os 18 focados passaram.
- **A2, candidata (Regra 353):** a escada, os patamares e o salão recebem tochas da colônia sem custo. As duas regressões falharam antes da correção e os 6 GameTests focados passaram depois.
- **Sentinela `fast` após o A2:** unitários, propriedade e 1.228 GameTests passaram após isolar o teste que limpa o cache. Relatório em `reports/test-runs/20261121T055553Z/`. O build final passou.
- **A3a, candidata (Regra 354):** cinco mudas no bosque por lenhador, primeiro do estoque da espécie escolhida. Adubação usa excedente da reserva de quatro ou lotes de 16 declarados no `VC_SUPPLY`. O GameTest reproduziu o estoque intacto antes da correção e sob mutação. Os 9 GameTests focados e 196 testes Python passaram.
- **Sentinela `fast` após o A3a:** unitários, propriedade e 1.228 GameTests passaram na primeira execução. Relatório em `reports/test-runs/20261121T061045Z/`. O build final passou.
- **Sentinela `fast` após o A6:** unitários, propriedade e 1.227 GameTests passaram na terceira execução. Relatório em `reports/test-runs/20261121T052007Z/`. O build final passou. As duas primeiras execuções repetiram falhas conhecidas de pastoreio.
- **Regras do modo:** `CLAUDE.md` §0.2.2 (escolha do autor antes de código, teste que falha primeiro, Sentinela, um JAR por ciclo, push só se o autor pedir). Responder em português. Build: `JAVA_HOME` do JDK 21, `./gradlew.bat build`; GameTests focados por `python scripts/gametest_focus.py <classe…>` (+ `restore`); em Bash no Windows, escreva scripts Python em arquivo em vez de heredoc com aspas.
- **Armadilhas já vistas:** ErrorProne com `-Werror` (parênteses, campos não lidos); `FileSizeRuleTest` (500 linhas); `ArchitectureRulesTest` (sem ciclo de pacote); `MineMarks.dug` apaga marcas a 8 blocos; a escada planejada da mina começa na altura da boca.
- **Avisar o autor:** a trava `wool` foi mantida contra o "saem" dele (é lógica, protege a lã branca); a tocha da colônia na mina abre exceção à Regra 212.

## Histórico até a 0.4.93

- **Versão:** 0.4.93 na `main` (branch `fix/jogo-0490` integrada por PR), **não jogada**; publicada como [`v0.4.93-alpha`](https://github.com/LucasRiboldi/MOD-village-1.21.1/releases/tag/v0.4.93-alpha). A 0.4.79 foi jogada em 2026-11-13 (taiga, Spark `oujZp2IxmS`); a triagem dos oito sintomas virou as Regras 275 a 282.
- **Modo:** correção por playtest (`CLAUDE.md` §0.2.2): log primeiro, opções ao autor, ele escolhe, teste que falha, correção, Sentinela, commit, JAR.
- **Entregue desde a 0.4.53 (todas candidatas):**
  - 0.4.54 a 0.4.56, Regras 243 a 249: plantação grande sobre areia, baú de qualquer lugar, aldeão fora da simulação não congela, cama longe vem para perto, chão nivelado é trabalho, um terço da madeira em tora, fundição sem cru chama o pedreiro.
  - 0.4.57 a 0.4.59, Regras 250 a 253: rua no chão novo, flor não segura coluna, acessório sem entrada, curral com terra no solo.
  - 0.4.60 a 0.4.62, Regras 254 a 256: patamar nas dobras da mina (ADR-053), pastor não amarra com curral, arco de pinheiro e lenhador só com folha.
  - 0.4.63 e 0.4.64, Regras 257 e 258 (reanálise dos logs): areia e argila sem reserva de cru; adiantamento lê todas as estruturas.
  - 0.4.65 a 0.4.68, Regras 259 a 262 (regras novas do autor de 12-11): oito ofícios na fundação, casa do mineiro na fundação, poste de luz, estruturas por bioma.
  - 0.4.69 a 0.4.74, Regras 263 a 269 (jogo da 0.4.68): fundidor funde onde está, grafo nunca sobre a água (174 revogada), linha planejada sai da construção, uma casa do mineiro basta, plantação grande em todo bioma, bosque em qualquer altura, prazo do pastor pela distância.
  - 0.4.75 a 0.4.79, Regras 270 a 274 (pedidos do autor de 13-11): sem ofício fora das análises, muda do bosque surge, corante das flores, um construtor por vila, obra conta como trabalho do construtor.
  - 0.4.80 a 0.4.82, Regras 275 a 282 (jogo da 0.4.79): soleira não é curral, alçapão em pé, lenhador encosta, água só sem caminho seco, solto só fora do lugar, árvore no caminho sai, log de tronco posto, só a rede reserva chão contra lote.
  - Varredura de falhas de 12-11 (`docs/audit/2026-11-12-varredura-de-falhas.md`): VF-001, VF-007 e VF-010 instrumentados; GameTest que mexe na hora ou na memória do mundo roda no próprio lote.
  - 0.4.93, Regras 315 a 325 e ADR-057 (simulações de 3 dias da 0.4.92 e escolhas de 18-11): fazendeiro semeia a terra comum, rua da vila gerada fica, `changes=` no grafo, três modos do grafo, porta do jogador é entrada, só aresta estável é calçada, terreno da vila inteira por fila (`VC_TERRAIN`), lenhador tira a folha, plantação grande inteira pelo molde. Sentinela com perfil longo.
  - 0.4.92, Regras 313 e 314 (jogo da 0.4.91): entrada no chão depois do degrau da porta; casa do mod com caminho na porta.
  - 0.4.91, Regras 305 a 312 (jogo da 0.4.90): entrada da casa no piso, aresta fixa só de rosa a rosa, telhado fora do chão do grafo, fundição na quantidade pedida, pastor sem prazo rumo ao curral, pó de osso do fazendeiro, areia moída antes, mineiro preso sobe rumo à borda.
  - 0.4.90, Regras 299 a 304 (jogo da 0.4.89): pastor leva o animal até o curral, porta e partes soltas no grafo, argila na hora, nó da mina sob o arco, frente da porta no piso, casa com solo. Falta o ponto 2 (nivelar ao longo do grafo).
  - 0.4.89, Regras 295 a 298 (jogo da 0.4.88 em montanha): ligação do grafo que não some, peças da vila no solo suavizado, chão suavizado em volta da obra nova, vila nova com um adulto por ofício e sobra sem ofício. Falta o ponto 5 (atividade por profissão).
  - 0.4.88, Regras 289 a 294 (ADR-056, escolhas G1–G7 do autor de 15-11): plantação grande semeada com 2 fazendeiros, tronco inteiro e folha no caminho, grafo na tábua e na frente da porta, sem torre de minério, pátio da mina, sobra tirada pelos ociosos. Falta a G8 (casa do mineiro medieval).
  - 0.4.87, Regra 288 (jogo da 0.4.86): nenhuma estrutura nasce sobre água; a água dentro da vila vira chão na geração (mundo novo).
  - 0.4.86, Regra 287 (jogo da 0.4.85): a tora deitada é chão; o fazendeiro entra na plantação grande.
  - 0.4.85, Regras 285 e 286 (jogo da 0.4.84): água com margem e gelo sob o pulo; mineiro novo na casa junto da mina; diagnósticos do grafo e do terreno.
  - 0.4.84, Regra 284 (VF-010): o animal atravessa o portão amarrado; relógio, vaca solta e canal de água estáveis nos GameTests.
  - 0.4.83, Regra 283 (GameTests instáveis de 14-11): o pastor só recolhe dentro da caixa da vila; testes do pastor, da tosquia e da taiga sem depender dos vizinhos (os lotes rodam juntos num mundo só).

### Roteiro histórico da 0.4.93

**O autor precisa jogar a 0.4.93 num mundo novo e mandar o Spark,** num jogo longo (o da 0.4.91 durou 6 min e não gerou `VC_TIME`). Confira `VC_TERRAIN` com nivelados todo dia (Regra 323), `CLEARED_LEAVES` do lenhador (324) e a plantação grande entregue inteira semeada (325); o modo do grafo é `atual` até o autor trocar com `/vc grafo modo`. Da 0.4.92: Confira a frente das portas no nível do chão (Regra 313) e o caminho na porta da BigHouseMOD (Regra 314). Da 0.4.91: Confira `doors` acima de 0 no `VC_GROUND` (Regra 305), `fixed=` e `loose=` no `VC_ROAD_GRAPH` (306 e 307), `CLAY_ON_REQUEST` e `BONE_MEAL_FOR_THE_FARM` no `VC_SUPPLY` (308 e 310), animais no curral grande (309), `MISSING_PIECE` de areia (311) e o mineiro sem voltar a encalhar no mesmo ponto (312). Da 0.4.90: As Regras 288, 296, 298, 303 e 304 (geração) só valem em chunk e vila gerados depois delas; confira também `doors` no `VC_GROUND`, `VC_PENS` com animais no curral grande e `CLAY_ON_REQUEST` no `VC_SUPPLY`; confira `refused=` no `VC_ROAD_GRAPH`, `moved`/`most` no `VC_GROUND` e `maxStep` no `VC_SITE_EDGE`. As triagens dos jogos da 0.4.85 e da 0.4.86 deixaram achados abertos no `TODO.md`. Use as notas `/vc bioma nota` e a lista `docs/testing/checklist-jogo-avaliacao.md`. Ao receber o log, siga `CLAUDE.md` §0.2.2 e confira:

- Lote (Regra 282): `lots_ok` acima de 0 no `VC_BIOME` e obra aberta; o overlay da obra volta com ela.
- Troncos (Regra 281): ler as linhas `VC_LOG_PLACED` e achar quem pôs os troncos que o autor viu na vila.
- Encalhe (Regra 279): nenhum "out at ... after 0 steps" seguido de novo encalhe no mesmo ponto.
- Ramal da mina (Regra 280) e água (Regra 278): o mineiro chega à mina pela rua; nenhum aldeão nadando longe; nenhum afogado.
- Pastor e alçapões (Regras 275 e 276): nenhum "fenced in" em soleira de casa nem em canteiro de alçapões.
- Pastor (Regra 283): o animal solto perto do curral ainda é recolhido; nenhum animal de fora da caixa da vila.
- Curral (Regra 284): o animal entra pelo portão sem fugir para longe; nenhum "gave up: the cow did not walk into the pen".
- Água (Regra 285): `VC_WATER_RESCUE` e `VC_WATER_ICE`; ninguém preso na água.
- Mineiro (Regra 286): dorme na casa junto da mina desde o primeiro dia (no jogo da 0.4.85 não houve linha de log que provasse).
- Fazendeiro (Regra 287): trabalho acima de 1% no `time_ledger`; nenhum "could not reach the crop" na plantação grande.
- Ler `loose=` no `VC_ROAD_GRAPH`, `VC_SMOOTH` e `VC_LOT_EDGE` para escolher as correções das notas 1, 3, 7 e 8 do jogo da 0.4.84.
- Varredura: a linha `VC_COST_PHASES` (VF-001) e o fantasma com `lastSeen` (VF-007).

Em aberto no `TODO.md`: VF-001 a VF-003 da varredura e os itens das fases F0 a F5.

## Ordem de leitura

1. Este arquivo.
2. `AGENTS.md`, inteiro no topo; `CLAUDE.md` §0 a §2.1. `CLAUDE.md` vence em conflito.
3. `STATE.md` (cabe em 150 linhas).
4. `docs/audit/2026-11-13-avaliacao-qualidade.md`: notas, portões e cronograma até a 1.0. O `TODO.md` segue as fases dele;
   os abertos antigos foram triados em `docs/technical/TODO-herdado-2026-11-13.md` (cada um diz se fechou ou para onde foi).
5. `docs/RULES.md`: procure a regra por número. ADRs ficam no índice `docs/decisions/README.md`.

## Regras do autor que não estão nos outros documentos

Elas viviam só na memória local do Claude Code. O que está em `CLAUDE.md` e `AGENTS.md` continua valendo.

- **Idioma:** sempre português do Brasil. Ao perguntar ou oferecer opções, frases completas, de modo leigo. Ele quer opções para decidir: 2 ou 3, com efeito e risco, e a recomendada primeiro.
- **"commit push":** commite, envie e responda só `commit push realizado`. Se algo falhar, não diga a frase: diga o que houve.
- **"relatorio commit":** ciclo completo: estado atual, revisão, testes de verdade, README, `TODO.md` por prioridade, higiene, commit, push e relatório `✅ CICLO ENCERRADO`. Separe "tem teste" de "foi verificado rodando de verdade".
- **Merge na `main`:** autorizado sem perguntar quando é o próximo passo, com CI verde e sem conflito (`gh pr merge N --merge`). **Release e tag exigem pedido.** Nunca `--force`.
- **Sistema velho:** autor em 2026-11-06: "ignora a antiga, esquece o que é velho e passa sempre a usar o sistema mais atual".
- **Não marque como feito o que não rodou:** diga o que foi verificado, o que não foi e o que depende do jogo. Correção é candidata até o próximo playtest.
- **Imagens do autor valem mais que a leitura do texto dele:** o desenho da 0.4.43 derrubou uma recomendação minha de juntar encontros próximos.
- **Limpeza:** ao fim de cada etapa, `./gradlew.bat --stop` e mate o que ficou ocioso.
- **CI:** só altere os workflows com pedido do autor (o de 13-11 autorizou: cobertura da bateria guardada, checagem de espaços contra a base, release só com CI verde do mesmo commit).

## Ambiente

- **Máquina do autor:** Windows 11, projeto em `C:\Users\lucas\Desktop\MOD-village-1.21.1`.
- **JDK 21:** o `PATH` não basta. Em Git Bash: `export JAVA_HOME=/c/Users/lucas/.jdks/jdk-21.0.12+8`.
- **Compilar:** `./gradlew.bat compileJava compileTestJava compileGametestJava -q`. ErrorProne roda com `-Werror`, então aviso reprova.
- **Sentinela:** `python scripts/test_lab.py fast`, depois `python scripts/sentinel_analysis.py`, com o Minecraft fechado. Mudou regra ou teste: `python scripts/rule_coverage.py`. Mudou `docs/RULES.md`: `python scripts/rules_to_readme.py`.
- **Instalar para o autor jogar:** `./gradlew.bat remapJar`, copie `build/libs/village-colony-<versão>.jar` para `downloads/` e para `%APPDATA%\.minecraft\mods` (TLauncher), renomeie o anterior para `.jar.disabled` e compare o SHA-256 das três cópias.
- **Publicar:** com `docs/releases/v<versão>.md` na `main`, `gh workflow run Release --ref main`. O workflow compila outro JAR, com outro SHA-256.

### Só existe na máquina do autor, fora do GitHub

`%APPDATA%\.minecraft\logs\latest.log` (sobrescrito a cada jogo), o diário `logs\villagecolony-actions-*.jsonl`, o save `saves\Novo mundo`, `reports/`, `graphify-out/`, `downloads/` e os checkpoints `.handoff/`. Sem acesso a eles, peça o log ao autor: os relatórios e o `scripts/save_read.py` dependem deles.

O hook do Claude Code que manda rodar `graphify` antes de ler arquivos não se aplica se `graphify-out/` não existir.

## Ferramentas

| Script | Para quê |
|---|---|
| `scripts/time_ledger.py <log>` | tempo por ofício (Regras 50 e 51) |
| `scripts/action_report.py` | diário e motivos de recusa; toda análise de Spark o mostra ao autor |
| `scripts/cost_ledger.py <log>` | custo por fase do ciclo |
| `scripts/spark_decode.py` | resumo de um Spark baixado |
| `scripts/save_read.py` | grafo e mapa do save, sem abrir o jogo |
| `scripts/gametest_focus.py` | roda só algumas classes de GameTest; `runGametest` não aceita filtro |
| `scripts/gametest_summary.py` | total e falhas do relatório de GameTest |

Triagem no log: `VC_BIOME_NOTE` primeiro, depois `VC_MAE_SITE`, `VC_ROAD_GRAPH`, `VC_ROAD_COVERED`, `VC_SUPPLY`, `VC_SUPPLY_ERROR`, `VC_PENS`, `VC_DAY`, `VC_STATE`.

**Prova com sabotagem:** teste verde de primeira não prova nada. Depois de implementar, troque um comportamento de cada vez e confira que algum teste falha. Na 0.4.43 isso expôs um teste que passava de qualquer jeito.

## Verificado e não verificado na 0.4.53

- **Verificado:** Sentinela com 1085 GameTests e 1787 unitários, PIT local de 85%, CI da PR 65 e da `main` verdes, sabotagens pegas em cada versão.
- **Não verificado:** nada das 0.4.48 a 0.4.52 foi jogado; os efeitos em jogo das Regras 231 a 242 dependem do próximo jogo.
- **Testes instáveis:** `animalsWalkInThroughEachOfTheFourGates` e `workerCollectsWhileWalkingPast`, sem causa achada. Veja o `TODO.md`. Repita a bateria antes de culpar a mudança.

## O que falta no MAE

Lista viva em `TODO.md`, seção "MAE: o que falta depois da 0.4.26". Resumo:

- **Depende de jogo:** `/vc mae obra <modelo>` do `queued` ao `done`, retomada depois de reiniciar, obra `fora`, socorro até o fim (só disparou, e a obra foi parada), carga real.
- **Depende de código:** testes de `MaeSites.move` e da volta que zera a conta em `LotSeeking`; medir de novo o custo de `MaeGenerationEvidence`. Os testes de `WaitingWork` e da soma para o socorro entraram (`MaePatienceGameTest`).
- **Limitação conhecida:** ruínas de saves que já abriram a 0.4.38 ou depois não são achadas, porque as regiões antigas sumiram do save.

## Git

- **PR aberta antiga:** [#45](https://github.com/LucasRiboldi/MOD-village-1.21.1/pull/45), `codex/local-test-sentinel`, sem atividade desde 2026-11-02. Pergunte ao autor antes de mexer.
- **Só locais e fora da `main`:** `docs/regra-166-ferramentas` e `fix/miner-direct-path-gametest`, 1 commit cada, de outubro. Não foram enviados: são velhos e não fazem falta ao ponto atual.
- **Muitas branches `origin/claude/*` e `origin/codex/*`:** históricas. A existência de uma branch não prova que está integrada nem concluída.
- **Atribuição:** o rodapé `Co-Authored-By: Claude` dos commits antigos é do Claude Code. Use o seu.

## Memória das sessões anteriores

[`docs/archive/agent-memory/`](docs/archive/agent-memory/MEMORY.md) traz a cópia da memória do Claude Code: uma lição por arquivo, com o índice em `MEMORY.md`. É histórico: procure por tema (`rg`), não leia inteiro, e confira no código antes de confiar, porque cada nota reflete o dia em que foi escrita. Vale ler quando for mexer no assunto: gametest, mina, pastor, grafo, MAE.

As armadilhas que mais custaram:

- Classe de GameTest fora de `src/gametest/resources/fabric.mod.json` some da bateria em silêncio.
- Relatório de GameTest velho engana: apague `build/gametest-report.xml` antes de rodar.
- GameTests do mesmo lote interferem entre si: use `batchId` próprio e coordenada longe.
- `findPathTo` logo depois de `spawnEntity` dá `null`: espere pousar.
- Heredoc grande no shell quebra: escreva o script em arquivo.
- Rodar `gradlew.bat` de dentro de um `subprocess` do Python com `shell=True` no Windows falha: use Git Bash.

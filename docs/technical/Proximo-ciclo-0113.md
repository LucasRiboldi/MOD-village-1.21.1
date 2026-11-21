# Próximo ciclo (0.4.113) — escolhas do autor de 2026-11-21

Retomada: ler `STATE.md` e este arquivo. Origem: `docs/audit/2026-11-21-analise-jogo-0111.md` (achados A1 a A9, R1).
Branch de trabalho: `fix/miolo-pelas-pecas` (0.4.111 e 0.4.112 empilhadas, sem push, JAR 0.4.112 `ae11f4e1` instalado, não jogado).
Modo temporário §0.2.2: um commit atômico por correção, um JAR no fim, Sentinela `fast`, nada de push sem pedido.
Numeração: Regras 349 em diante (a 348 é a descida única).

## Estado ao parar

Na retomada de 21-11, `GameTestBatchRuleTest` falhou porque dois testes de `GroundPickupGameTest` alteravam o relógio global no mesmo lote. O novo teste agora roda em `ground_pickup_earth_ceiling` e `gradlew.bat build` passou com JDK 21. `ColonyGoalsTest` passou os 56 casos.

| Item | Estado |
|---|---|
| A8 travas de cru | **Candidata (Regra 349).** `BoneMealRound.CHEST_RESERVE` e `SmelterAdvance.KEEP` passam a 4. `SmelterAdvance.MINE_FILL` e `ValueGoods.KEEP_RAW` passam a 16. `ingot` sai do `VC_KEEP`; teto de 64 por lingote e trava `wool` de 16 em vigor. Cinco dos 21 GameTests focados falharam sob mutação, e os 21 passaram após restaurar a regra. Falta confirmação no próximo jogo. |
| A7 terra | **Candidata (Regra 350).** `GroundPickup.collectNearby` consulta `EarthSurplus` antes de guardar terra. O cenário registra dois baús públicos para atingir 256 unidades sem esbarrar no limite por baú. Sem o filtro, o teste falhou porque a terra foi recolhida no teto. Com o filtro, os três GameTests focados passaram. Falta confirmação no próximo jogo. |
| A9 telemetria | **Candidata (Regra 351).** O vigia ignora transições de grama e caminho sem autor declarado. O relatório histórico desconsidera as equivalentes atribuídas a `mod?natural`. O nivelamento em volta da obra e o reparo do piso da mina registram `entorno_obra` e `piso_mina` ao escrever. As regressões falharam sem a correção. Com ela, passaram oito GameTests focados e 196 testes Python. Falta confirmação no próximo jogo. |
| A6 terracota | **Candidata (Regra 352).** Sem argila nos baús acessíveis ou exposta na área de coleta carregada, a planta pede tijolos de pedra e o estoque deixa de pedir terracota. A leitura da obra reconhece as peças já assentadas. Os dois testes novos falharam sob mutação. Com a regra restaurada, passaram 18 GameTests focados e 196 testes Python. Falta confirmação no próximo jogo. |
| A2 luz da mina | **Candidata (Regra 353).** O mineiro usa tochas da colônia na parede da escada a cada seis degraus, em cada patamar e no chão dos salões em grade de seis blocos. As duas regressões falharam no código anterior e sob mutação. Com a correção, passaram 6 GameTests focados e 196 testes Python. Falta confirmação no próximo jogo. |
| A3a bosque | **Candidata (Regra 354).** Cinco mudas por lenhador, inclusive durante pedido de madeira. O cenário usa cinco das 88 mudas de carvalho, preserva a reserva de quatro pós de osso e confirma o lote de 16 no `VC_SUPPLY` e o avanço do crescimento. A busca do bosque usa a posição do lenhador. Falta medir as caminhadas e a entrega de toras no próximo jogo. |

Sentinela `fast` de 21-11 (`20261121T040824Z`): unitários e propriedade passaram. Dos 1.224 GameTests, só o cenário do A7 falhou. A análise foi gerada em `reports/sentinel-analysis/20261121T041222Z/`. A matriz de regras inclui a 349, e a suíte Python passou após limpar a variável de configuração Git herdada.

Após o A7, o Sentinela `fast` (`20261121T042100Z`) passou em unitários, propriedade e 1.224 GameTests. Análise em `reports/sentinel-analysis/20261121T042503Z/`. A matriz inclui a Regra 350, e a suíte Python passou.

No A9, os dois primeiros `fast` passaram em unitários e propriedade. Nos 1.225 GameTests, a primeira
execução (`20261121T044555Z`) falhou no teste da ovelha desgarrada, já observado em `20261120T021714Z`.
A segunda (`20261121T045021Z`) falhou no teste da cerca da taiga e no dos quatro portões do curral MAE.
A terceira (`20261121T045420Z`) passou em unitários, propriedade e todos os 1.225 GameTests.
A análise está em `reports/sentinel-analysis/20261121T045745Z/`. O build final também passou.

No A6, o primeiro `fast` (`20261121T051201Z`) passou em unitários e propriedade. Dois
dos 1.227 GameTests falharam em `ShepherdRoundGameTest` (animal dentro do curral e ovelha
inacessível). Os mesmos testes já falharam em `20261120T021714Z`. A análise está em
`reports/sentinel-analysis/20261121T051606Z/`.
A segunda execução (`20261121T051630Z`) passou em unitários e propriedade e falhou só na
ovelha inacessível. A análise está em `reports/sentinel-analysis/20261121T051948Z/`.
A terceira (`20261121T052007Z`) passou em unitários, propriedade e todos os 1.227
GameTests. A análise está em `reports/sentinel-analysis/20261121T052316Z/`. O build final passou.

No A2, o primeiro `fast` (`20261121T055043Z`) apontou que o teste da escada compartilhou um lote
embora limpasse o cache global. A suíte de GameTests repetiu a falha conhecida dos quatro portões do
curral MAE. A análise está em `reports/sentinel-analysis/20261121T055449Z/`.
Depois de isolar esse cenário no lote `mine_stair_colony_torches`, passaram os testes unitários,
de propriedade e os 1.228 GameTests (`20261121T055553Z`). A análise está em
`reports/sentinel-analysis/20261121T060022Z/`. O build final passou.

Após o A3a, o Sentinela `fast` (`20261121T061045Z`) passou na primeira
execução em unitários, propriedade e 1.228 GameTests. A análise está em
`reports/sentinel-analysis/20261121T061527Z/`. O build final e os 196 testes
Python passaram. A regressão do estoque falhou sob mutação e os nove
GameTests focados passaram com a regra restaurada.

## O que falta fazer, na ordem

1. **A8**: candidata, ver estado acima.
2. **A7**: candidata, ver estado acima.
3. **A9**: Regra 351, ver estado acima. As três reafirmações perto do açougue aparecem após a suavização do entorno,
   e o log confirma o reparo do piso em `121, 69, -125`. Os quatro eventos históricos mantêm o rótulo original do log.
4. **A6**: Regra 352, ver estado acima. Em vilas sem argila, os trechos afetados das casas ficam com a cor e a textura dos
   tijolos de pedra. Conferir o visual e os pedidos do fundidor no próximo jogo.
5. **A2 (A2a + A2z)**: Regra 353, ver estado acima. O mineiro morreu para um zumbi no salão
   (`VC_VILLAGER_DEATH … mob at=129, 58, -124`). A tocha instalada pelo mineiro acende o anel de luz
   e bloqueia o nascimento natural nas proximidades. As posições saem do traçado da mina e o limite
   de escavação segue a Regra 348.
6. **A3a**: o lenhador **planta 5 mudas** perto da vila (88 mudas paradas no estoque; `oak_sapling`) e o **pó de osso
   surge** para fazê-las crescer, como o `BONE_MEAL_FOR_THE_FARM` do fazendeiro (declarado no `VC_SUPPLY`, `BoneMealRound.SURGE`).
   Hoje: lenhador 67% andando, 5 tarefas soltas `got no closer to the tree`, estoque de tora 0, obra pondo 124 peças de
   lado (`SET_ASIDE: no material`, cerca 52×, tora ~70×). Ver `Lumberjack*`/`TreeMarks`/`GroveSapling` (`VC_SUPPLY GROVE_SAPLING`).
7. **A4** (diagnóstico, sem mudar comportamento): fazendeiro soltou 16 `could not reach the crop`, 0 colheitas
   entregues, 7 `STRANDED frozen twice`. Gravar, por recusa, a posição do canteiro, a do aldeão e o motivo no diário/log;
   pedir o resultado no próximo jogo.
8. **A5** (diagnóstico): pastor soltou 6 de 6 (`could not reach the sheep`, `fenced in`), 252 s BLOCKED perto do
   portão do curral grande (76, 68, -210). Mesma instrumentação do A4.
9. **R1 `/vc salvar` completo**: o retrato passa a trazer também estado da mina (hélices, recusas, nível), a última
   `VC_ROAD_GRAPH`, obras e peças postas de lado, `VC_TIME` do jogo inteiro (não só a última janela), mortes,
   `VC_LOOP`, `VC_SUPPLY_ERROR`, `VC_PLAYER_*` e a lista **inteira** de `/vc blocos` (hoje corta em 15). Origem:
   `SnapshotCommand` (Regra 346), `SnapshotCommandGameTest`. Alternativa mais barata: gravar a saída de
   `scripts/game_report.py`.

## Ao fechar o ciclo

- Documentos por regra: `docs/RULES.md` (linha), `python scripts/rules_to_readme.py`, `python scripts/rule_coverage.py`,
  `python -m unittest discover -s tests -p "test_*.py"`, `CHANGELOG.md`, `STATE.md`, `TODO.md`, Development-Log.
- Versão: `gradle.properties` `mod_version` 0.4.113.
- `./gradlew.bat build`, GameTests focados (`scripts/gametest_focus.py`), `python scripts/test_lab.py fast` +
  `python scripts/sentinel_analysis.py` (Minecraft fechado), `remapJar`, copiar para `downloads/` e
  `%APPDATA%/.minecraft/mods`, SHA-256 das três cópias.
- Mutação conferida em cada teste novo (vermelho sem a mudança).
- Lembrar o autor: 0.4.111 (chão pela forma das peças) e 0.4.112 (uma descida por arco) ainda **não foram jogadas**; mundo novo.

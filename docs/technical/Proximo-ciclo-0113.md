# Próximo ciclo (0.4.113) — escolhas do autor de 2026-11-21

Retomada: ler `STATE.md` e este arquivo. Origem: `docs/audit/2026-11-21-analise-jogo-0111.md` (achados A1 a A9, R1).
Branch de trabalho: `fix/miolo-pelas-pecas` (0.4.111 e 0.4.112 empilhadas, sem push, JAR 0.4.112 `ae11f4e1` instalado, não jogado).
Modo temporário §0.2.2: um commit atômico por correção, um JAR no fim, Sentinela `fast`, nada de push sem pedido.
Numeração: Regras 349 em diante (a 348 é a descida única).

## Estado ao parar (WIP no commit `733133e2`)

Na retomada de 21-11, a linha de base `gradlew.bat build` com JDK 21 no commit `5375d9dc` falhou (1 de 1.885 testes). `GameTestBatchRuleTest` apontou que `earthAboveTheCeilingStaysOnTheGround` e `workerCollectsWhileWalkingPast` chamam `setTimeOfDay` no mesmo lote `ground_pickup`. A linha de base precisa voltar a passar antes do A8. `ColonyGoalsTest` passou (56 testes); GameTests focados, mutações e Sentinela ficaram pendentes.

| Item | Estado |
|---|---|
| A8 travas de cru | **Editado, sem teste novo.** `BoneMealRound.CHEST_RESERVE` 16→4, `SmelterAdvance.KEEP` 16→4, `SmelterAdvance.MINE_FILL` 64→16, `ValueGoods.KEEP_RAW` 64→16, linha `KeepLedger.atLeast("ingot", …)` removida. Falta: rodar `ColonyGoalsTest`, `IdleFixesGameTest`, `SmelterAdvanceGameTest`, `ShepherdWoolGoodsGameTest` e corrigir o que codificava os valores velhos; Regra 349 no `docs/RULES.md`. A trava `wool` **foi mantida** (é lógica, não só medida: protege a lã branca), contra o "saem" do autor — dizer isso a ele. |
| A7 terra | **Editado, teste vermelho pelo cenário.** `EarthSurplus` (novo) + filtro em `GroundPickup.collectNearby`; GameTest `earthAboveTheCeilingStaysOnTheGround` falha com "o cenário não tem baú da colônia": falta registrar o baú da colônia como o `workerCollectsWhileWalkingPast` faz (`colony.measure(VillageBounds…)`, `colony.attend(...)` em loop, baú em `CHEST`) antes de `ColonyChests.nearestFirst`. Depois: provar o vermelho tirando o filtro. Regra 350. |

## O que falta fazer, na ordem

1. **A8**: terminar como acima. Regra 349 (limites a um quarto; `ingot` sai da medição).
2. **A7**: consertar o cenário do teste. Regra 350 (terra acima de 256 nos baús fica no chão).
3. **A9** (telemetria): grama que se espalha e caminho que volta a grama não contam como "o mod desfez o jogador".
   Origem: `EditLedger` (`mod?natural: dirt→grass_block` 99×, `grass_block→dirt` 45×, `dirt_path→grass_block` 45×);
   ver `scripts/player_report.py` e `EditLedgerGameTest`. Também dar dono ao sistema `mod?outro` (4 reafirmações 🔴).
4. **A6**: onde não há argila ao alcance, terracota sai dos pedidos e vira tijolo ou pedra (`VC_SUPPLY_ERROR clay NOT_FOUND`
   ×9; `white_terracotta` pedida pelas obras). Achar onde a obra pede terracota (`StructureBlueprintReader`, rotas de
   fabricação) e a rota que faz tijolo. Muda o visual das casas: dizer ao autor.
5. **A2 (A2a + A2z)**: o mineiro põe a **tocha da colônia** (`ModContent.COLONY_TORCH` e `COLONY_WALL_TORCH`,
   `ColonyTorchBlock`, com anel de luz e sem nascer monstro perto) na escada, nos patamares e no salão, a cada 6 blocos,
   sem custo, só dentro da mina. **Exceção à Regra 212** ("nenhum aldeão põe a tocha da colônia sozinho"): editar a 212
   e todo comentário que a cita (`grep "Regra 212"`). Hoje o salão só ganha luz de chão (`MineFurnishing`,
   `lit the hall floor`; `MineShaft.lightSpots`/`landingLights`). Motivo: o mineiro morreu para um zumbi no salão
   (`VC_VILLAGER_DEATH … mob at=129, 58, -124`). Cuidado: `MineShaft.plannedCells` e a `MineDigLimit` (Regra 348)
   — a tocha em parede não pode virar célula fora do desenho.
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

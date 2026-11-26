# Casa grande reformada (etapa 1 das casas de ofício) — plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or
> superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Objetivo:** a `big_house_mod.nbt` nova é 2 blocos mais funda e tem:
- piso de pedra no térreo;
- piso de cima sem buraco, a não ser o vão da escada, sem corrimão (autor 11-26: "não colocar corrimão");
- tochas por dentro;
- 7 camas e 7 baús, todo baú encostado em parede e tudo alcançável a pé da porta.

E baú novo posto ao lado de cama nunca fecha passagem.

**Arquitetura:**
- A casa passa a ser gerada por um script Python, no padrão de `scripts/gen_miner_house.py`.
- Um verificador Python das regras de casa (`scripts/houses/house_rules.py`) serve aos testes e, nas etapas 2 e 3, às
  casas `majest_`.
- No Java, o `ChestPlacer` ganha a recusa de lugar que fecha passagem. O código da fundação continua o mesmo, a não ser
  o que a altura nova da planta exigir.

**Tecnologia:** Python 3.13 com `nbtlib==2.0.4` (o CI já instala), Java 21, Fabric 1.21.1, Fabric GameTest.

**Desenho:** `docs/superpowers/specs/2026-11-26-casas-de-oficio-design.md`, seções 5, 8 e 10.

## Regras gerais

- Versão: `mod_version=0.4.145`. Regras novas: **369** (a casa grande reformada) e **370** (baú não fecha passagem).
- Formato de commit: `P0.1: <descrição> — Regra N (<teste que prova>)`. Sem trailer `Co-Authored-By`.
- Fluxo do GameTest focado:
  1. `python scripts/gametest_focus.py <nome completo da classe>`;
  2. `rm -f build/gametest-report.xml`;
  3. `./gradlew.bat runGametest --rerun-tasks -q`;
  4. `PYTHONIOENCODING=utf-8 python scripts/gametest_summary.py`;
  5. `python scripts/gametest_focus.py restore` antes de qualquer commit.
- Classe nova de GameTest entra em `src/gametest/resources/fabric.mod.json`.
- Comentário diz a regra e cita a fonte (ADR-035 §6). Código em inglês, comentário em português. Arquivo com menos de
  500 linhas: `ChestPlacer.java` tem 437, então a regra nova vai em classe própria.
- O caminho misto (🅲): o gerador escreve em `scripts/houses/out/` e só copia para os recursos um arquivo que ainda não
  existe lá, ou com `--overwrite <nome>`. Nesta etapa a cópia é com `--overwrite big_house_mod`, uma vez.

## Pontos de atenção (o que os testes não cobrem sozinhos)

1. **Altura da porta.** Com o piso novo na camada 0, a porta passa para a camada 1. `StreetLayer.of` então devolve a
   camada 0 como a da rua. O esperado é o piso de pedra no nível da rua (no lugar do chão) e a porta 1 acima, como nas
   casas da vila gerada. O `BigHouseLevelGameTest` e o `BigHouseEntranceGameTest` acusam se a casa assentar 1 bloco alta
   ou baixa: o teste da Tarefa 2 prova isso.
2. **Casa grande já de pé.** Obra pronta nunca reabre (`BuildingRepairPlanner` pula a casa grande), então a casa velha
   do mundo não muda. A casa grande inacabada num save antigo retoma pela planta nova: aceito, e anotado no CHANGELOG.
3. **Contas fixas nos testes.** `BigHouseModBlueprintGameTest` fixa o tamanho `(7, 10, 11)`, os 299 blocos postos e o
   corredor em `(3, 1, 5)`. Esses números mudam junto com a planta.
4. **Regra 35.** Baú do carpinteiro em porta + (1, 0, 1), cama em porta + (1, 0, 2), corredor livre em porta + (1, 0, 0)
   e porta + (2, 0, 0). A planta nova mantém isso.
5. **Baú na casa da vila gerada.** A recusa de lugar que fecha passagem vale para todo baú novo, também nas casas do
   jogo. O baú que já existe fica onde está.

---

### Tarefa 1: verificador das regras de casa (Python)

**Arquivos:**
- Criar: `scripts/houses/__init__.py` (vazio) e `scripts/houses/house_rules.py`
- Teste: `tests/test_house_rules.py`

**Interfaces:**
- Produz:
  - `load(path: str) -> House`, com `House.size: tuple[int, int, int]` e
    `House.block(x, y, z) -> tuple[str, dict[str, str]]`. O nome vem sem `minecraft:`; a posição fora da lista de
    blocos devolve `("void", {})`.
  - `problems(house: House, require_sign: bool = False) -> list[str]`: a lista vazia quer dizer que a casa passou.
  - `VANILLA_JOB_BLOCKS: frozenset[str]`.

**Modelo de andar** (decide o que é andável, para quem escreve o corpo):
- Uma célula `(x, y, z)` é **de pé** quando cumpre as duas condições:
  - o bloco em `y-1` tem corpo cheio, ou é degrau, laje de baixo ou dupla;
  - `y` e `y+1` são passáveis: `air`, `void`, porta, tocha, tocha de parede, tapete ou placa.
- Movimento nas 4 direções, com `dy` entre -1 e +1. Subir exige `y+2` passável na célula de origem.
- O ponto de partida é a célula da metade de baixo de cada porta. A porta é passável nos dois sentidos.

**Regras que `problems` devolve.** Um texto por problema, com a posição, prefixado:
- `sem cama`, `sem baú` e `sem luz` (luz é `torch`, `wall_torch`, `lantern` ou `colony_wall_torch`);
- `baú solto x,y,z`: nenhuma vizinha horizontal é bloco de corpo cheio que não é baú;
- `baú sem frente x,y,z`: a célula na direção `facing` do baú não é de pé ou não é alcançada;
- `cama fora do caminho x,y,z`: nenhuma célula vizinha da cabeceira ou do pé é alcançada;
- `buraco no piso x,y,z`: uma célula de piso de um andar (camadas `0` e `4` da casa grande, dentro das paredes) é
  `air`/`void` e não está acima de um degrau a até 2 blocos abaixo;
- `posto de trabalho do jogo <nome>`: o bloco está em `VANILLA_JOB_BLOCKS`;
- `sem placa`: só com `require_sign`.

`VANILLA_JOB_BLOCKS` = `barrel, composter, smoker, blast_furnace, stonecutter, loom, fletching_table,
cartography_table, lectern, cauldron, water_cauldron, lava_cauldron, powder_snow_cauldron, smithing_table, grindstone,
brewing_stand, bell`.

- [ ] **Passo 1: Escrever o teste que falha.** `tests/test_house_rules.py`, `unittest`, lendo
  `src/main/resources/data/villagecolony/structure/houses/big_house_mod.nbt`:
  - `test_the_current_big_house_traps_a_villager`: `problems(load(BIG))` contém um item que começa com
    `"cama fora do caminho"` e um que começa com `"buraco no piso"`. Este é o retrato da planta de hoje, e é apagado na
    Tarefa 2.
  - `test_a_chest_in_the_middle_of_the_room_is_loose`: uma `House` montada à mão, 5×4×5, com piso, paredes, porta e
    um baú no meio, dá `"baú solto"`.
  - `test_a_vanilla_job_block_is_refused`: a mesma casa com `composter` dá `"posto de trabalho do jogo composter"`.
  - `test_a_clean_small_house_passes`: a mesma casa com cama e baú na parede, e tocha, dá `[]`.

  Para montar a casa à mão, `House` aceita `House(size, blocks: dict[tuple[int, int, int], tuple[str, dict]])`.
- [ ] **Passo 2: Rodar e ver falhar.** `python -m unittest tests.test_house_rules -v`. Esperado: `ModuleNotFoundError:
  scripts.houses.house_rules`.
- [ ] **Passo 3: Implementar** `load`, `House` e `problems` em `scripts/houses/house_rules.py`. O alcance é uma busca em
  largura sobre o modelo de andar acima. Ler a paleta e `blocks` do `.nbt` pelo `nbtlib`, como faz o
  `scripts/gen_miner_house.py`.
- [ ] **Passo 4: Rodar e ver passar.** O mesmo comando, depois `python -m unittest discover -s tests`. Esperado: tudo `OK`.
- [ ] **Passo 5: Commit.**
  `git add scripts/houses/__init__.py scripts/houses/house_rules.py tests/test_house_rules.py`;
  mensagem `P0.1: verificador das regras de casa — Regra 369 (test_house_rules)`.

---

### Tarefa 2: a planta nova da casa grande

**Arquivos:**
- Criar: `scripts/houses/gen_big_house.py`
- Mudar: `src/main/resources/data/villagecolony/structure/houses/big_house_mod.nbt`, gerado
- Mudar: `src/gametest/java/com/villagecolony/gametest/BigHouseModBlueprintGameTest.java`, nas contas fixas e no teste
  novo
- Mudar: `tests/test_house_rules.py`, onde o retrato da Tarefa 1 vira o teste da casa nova
- Mudar se o teste de altura pedir: `src/main/java/com/villagecolony/fabric/integration/BigHouseFoundation.java`

**Interfaces:**
- Consome: `house_rules.load` e `house_rules.problems` (Tarefa 1).
- Produz: `big_house_mod.nbt` de tamanho **9 × 12 × 11** (x, y, z). As demais tarefas não dependem de nomes desta.

**A planta.** Exata, porque é decisão do plano. `x` cresce para leste e `z` para o sul.
- Paredes em `x=1`, `x=7`, `z=1` e `z=9`; miolo `x=2..6`, `z=2..8`; beiral em `x=0`, `x=8`, `z=0` e `z=10`.
- **Camada 0, piso.** `stone_bricks` no miolo, e `cobblestone` sob as paredes, que vira arenito no deserto pela Regra
  262. O piso fica em tijolo de pedra para não virar arenito.
- **Camadas 1 a 3, térreo.**
  - Paredes de `cobblestone`, com `oak_log` nos cantos e de cada lado da porta (como hoje). `glass_pane` na camada 2,
    nas paredes compridas, em `z=3` e `z=7`, e no meio das paredes curtas.
  - Porta: `oak_door` em `(1, 1, 5)`, de baixo, e `(1, 2, 5)`, de cima; `facing=east`, `hinge=left`.
- **Camada 4, piso de cima.** `oak_planks` em todo o miolo, a não ser o vão da escada em `(6, 4, 4)` e `(6, 4, 5)`.
- **Camadas 5 e 6, andar de cima.** Paredes de `oak_planks`, com `oak_log` nos cantos e `glass_pane` como no térreo.
- **Camadas 7 a 11, telhado.** Na camada `7 + k`, para `k` de 0 a 4, há `oak_planks` em `x = k` e `x = 8 - k` para
  todo `z` de 0 a 10. As empenas `z = 1` e `z = 9` são fechadas entre esses dois `x`. A cumeeira fica em `x = 4`, na
  camada 11.
- **Escada.** `cobblestone_stairs` com `facing=north`, `half=bottom`, em `(6, 1, 6)`, `(6, 2, 5)` e `(6, 3, 4)`.
  Sem corrimão: quem cai no vão cai na escada, que leva ao térreo.
- **Térreo, camada 1.** O `facing` da cama aponta do pé para a cabeceira; o baú tem `facing` para onde abre.

  | Peça | Pé → cabeceira | Baú (facing) |
  |---|---|---|
  | cama A | `(2,1,3)` → `(2,1,2)`, facing north | `(2,1,4)` east |
  | cama do carpinteiro (Regra 35) | `(2,1,7)` → `(2,1,8)`, facing south | `(2,1,6)` east |
  | cama B | `(6,1,7)` → `(6,1,8)`, facing south | `(5,1,8)` west |
  | cama C | `(6,1,3)` → `(6,1,2)`, facing north | `(5,1,2)` west |

- **Andar de cima, camada 5.**

  | Peça | Pé → cabeceira | Baú (facing) |
  |---|---|---|
  | cama D | `(2,5,3)` → `(2,5,2)`, facing north | `(2,5,4)` east |
  | cama E | `(2,5,7)` → `(2,5,8)`, facing south | `(2,5,6)` east |
  | cama F | `(4,5,8)` → `(5,5,8)`, facing east | `(6,5,8)` south (a frente oeste é a cama) |

- **Tochas.** `wall_torch` em `(3, 3, 2)` facing south, `(3, 3, 8)` facing north, `(3, 6, 2)` facing south e
  `(3, 6, 8)` facing north. Fica a tocha de fora acima da porta, como hoje.
- Por dentro, o miolo fica `void`, como na planta de hoje: a limpeza do lote abre o espaço.

- [ ] **Passo 1: Escrever os testes que falham.**
  - Em `tests/test_house_rules.py`, trocar `test_the_current_big_house_traps_a_villager` por
    `test_the_big_house_is_walkable`: `problems(load(BIG)) == []`, `load(BIG).size == (9, 12, 11)`, 14 metades de
    `white_bed` e 7 `chest`.
  - Em `BigHouseModBlueprintGameTest`:
    - `blueprint.size().equals(new ColonyPos(9, 12, 11))`;
    - `placedBlocks` com o número que o gerador imprimir;
    - o corredor livre em `(2, 1, 5)` e `(3, 1, 5)`;
    - o teste da Regra 35 inalterado, porque lê a partir da porta.
  - GameTest novo `theBigHouseFloorSitsAtTheRoad`, no mesmo arquivo. Fundar a casa como faz o
    `BigHouseLevelGameTest` e conferir:
    - o bloco da camada 0 sob a porta está na altura da rua em volta;
    - a metade de baixo da porta está 1 acima.
- [ ] **Passo 2: Rodar e ver falhar.** `python -m unittest tests.test_house_rules -v` dá `FAIL`, com problemas na planta
  de hoje. O GameTest focado `com.villagecolony.gametest.BigHouseModBlueprintGameTest` falha no tamanho.
- [ ] **Passo 3: Implementar `scripts/houses/gen_big_house.py`.**
  - `main(argv)` escreve `scripts/houses/out/big_house_mod.nbt` (`DataVersion` 3955).
  - Com `--overwrite big_house_mod`, também escreve em `src/main/resources/data/villagecolony/structure/houses/`.
  - Imprime quantos blocos não-`void` a planta tem.
  - Antes de escrever, roda `house_rules.problems` e se recusa a gravar se a lista não vier vazia.
  - Rodar: `python -P scripts/houses/gen_big_house.py --overwrite big_house_mod`.
- [ ] **Passo 4: Rodar e ver passar.**
  - `python -m unittest discover -s tests` dá `OK`.
  - GameTests focados: `BigHouseModBlueprintGameTest`, `BigHouseLevelGameTest`, `BigHouseEntranceGameTest`,
    `BigHouseFoundationGameTest`, `VillageFoundationGameTest`, `DoorPathsGameTest`, `StreetLevelGameTest`,
    `BuildSiteGameTest`, `ConstructionCancellationGameTest`, `FoundationRepairGameTest` e `ClimbOutPiecesGameTest`.
    Esperado: `0 failed`.
  - Se só a altura falhar, o ajuste fica no cálculo da origem em `BigHouseFoundation`, para que a camada da rua do
    `StreetLayer` caia na altura da rua.
- [ ] **Passo 5: Ver no jogo de teste.** `./gradlew.bat runClient` não é exigido. O autor confere com
  `/place template villagecolony:houses/big_house_mod` no criativo, depois do JAR.
- [ ] **Passo 6: Commit.** Commitar o gerador, o `.nbt`, os testes e o que mudou em `BigHouseFoundation`. Mensagem:
  `P0.1: a casa grande mais funda, com piso de pedra, sem buraco e com tudo alcançável — Regra 369
  (test_house_rules, BigHouseModBlueprintGameTest)`.

---

### Tarefa 3: baú ao lado da cama nunca fecha passagem

**Arquivos:**
- Criar: `src/main/java/com/villagecolony/fabric/integration/ChestPassage.java`
- Mudar: `src/main/java/com/villagecolony/fabric/integration/ChestPlacer.java`, em `placeBeside`, linha ~100
- Teste: `src/gametest/java/com/villagecolony/fabric/integration/ChestPassageGameTest.java`, mais o registro no
  `fabric.mod.json`

**Interfaces:**
- Produz: `static boolean closesAPassage(ServerWorld world, BlockPos spot, BlockBox room)`.
  - Verdadeiro quando pôr um bloco cheio em `spot` aumenta o número de partes ligadas das células de pé do cômodo na
    altura de `spot`.
  - Célula de pé: chão de corpo abaixo, e 2 blocos sem colisão acima. Ligação nas 4 direções, dentro da `room`.
  - A célula de `spot` não conta.
- Consome: nada de outra tarefa.

- [ ] **Passo 1: Escrever o teste que falha.** `ChestPassageGameTest.aBedChestNeverSplitsTheRoom`, num cômodo de
  pedra 3 × 5, com porta numa ponta e cama na outra, montado de modo que o único lugar ao lado da cama com parede seja
  o vão que liga as duas metades:
  - `ChestPlacer.placeBesideBedInStructure(world, bed, box)` não põe baú nesse vão;
  - o resultado é `PLACED` em outro lugar ou `SKIPPED`;
  - `ChestPassage.closesAPassage(world, gap, box)` é `true`.
- [ ] **Passo 2: Rodar e ver falhar.** GameTest focado na classe nova. Esperado: o baú posto no vão, e o assert da
  Regra 370 falha.
- [ ] **Passo 3: Implementar** `ChestPassage.closesAPassage`. Em `ChestPlacer.placeBeside`, pular o candidato para o
  qual a função dá `true`, como já pula o que não tem parede (`wallBeside(world, spot).isEmpty()`). O javadoc cita a
  Regra 370.
- [ ] **Passo 4: Rodar e ver passar.** A classe nova mais `ChestPlacerGameTest`, `VillageChestsGameTest` e o que mais
  citar `ChestPlacer` em `src/gametest`. Esperado: `0 failed`.
- [ ] **Passo 5: Commit.** Mensagem: `P0.1: baú ao lado da cama nunca fecha passagem — Regra 370
  (ChestPassageGameTest)`.

---

### Tarefa 4: regras, documentos, Sentinela e JAR

**Arquivos:** `docs/RULES.md`, `README.md` e `docs/testing/regras-e-testes.md`, estes dois gerados; `CHANGELOG.md`,
`STATE.md`, `HANDOFF.md`, `TODO.md`, `gradle.properties` e
`src/main/resources/data/villagecolony/structure/colony/LEIAME.md`, onde entra a linha sobre `scripts/houses/`.

- [ ] **Passo 1: Escrever as Regras 369 e 370** em `docs/RULES.md`, depois da 368, com o pedido do autor citado:
  - **369:** "o modelo da casa do mod precisa ser 2 blocos maior de profundidade ... tapar os buracos do segundo andar";
  - **370:** "todos baú estarem encostados em uma parede e ter acesso de trânsito dentro da casa".

  Rodar `python scripts/rules_to_readme.py` e `python scripts/rule_coverage.py`. Esperado: `370 regras; sem teste: 2
  [33, 47]`.
- [ ] **Passo 2: Sentinela.** Com o Minecraft fechado, `python scripts/test_lab.py fast` e depois
  `python scripts/sentinel_analysis.py`. Esperado: `PASS` e `All N required tests passed`.
- [ ] **Passo 3: Versão e JAR.**
  - `mod_version=0.4.145`, depois `./gradlew.bat remapJar`, `bash scripts/install-mod.sh` e
    `cp build/libs/village-colony-0.4.145.jar downloads/`.
  - Comparar o SHA-256 das três cópias pelo Python (`hashlib`).
  - Só um `village-colony-*.jar` ativo no TLauncher.
- [ ] **Passo 4: Commit dos documentos.** Mensagem: `docs: 0.4.145 instalada — Regras 369 e 370, SHA-256`.

---

## Etapas 2 e 3 (esboço; cada uma ganha o seu plano)

- **Etapa 2: mudança e placa, com a casa do carpinteiro.**
  - ADR-065.
  - `ColonyModels` aceita `majest_<ofício>`, mas pela vaga de casa e não pela de oficina (desenho, seção 6).
  - Cama reservada ao titular, emenda da Regra 74 (seção 7).
  - Texto da placa escrito quando a casa fica pronta.
  - `scripts/houses/gen_majest.py` com `majest_carpenter`.
  - O verificador roda com `require_sign=True` em todo `majest_*.nbt`.
- **Etapa 3: as outras 7 casas e a do mineiro.**
  - Uma função de planta por ofício no `gen_majest.py`.
  - A `majest_miner` em pedras variadas.
  - `MineHomePlans.MODEL = "majest_miner"`, e saem os `miner_house.nbt` por estilo.

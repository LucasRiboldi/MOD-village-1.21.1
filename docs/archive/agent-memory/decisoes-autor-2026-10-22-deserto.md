---
name: decisoes-autor-2026-10-22-deserto
description: "Decisões do autor em 22-10 depois do jogo da 0.3.86 no deserto — lote+rua juntos na faixa (A5), mina com alcance e insistência, arenito mantém recusa, mais trigo para o rebanho"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-22T06:02:11.680Z
---

O autor escolheu em 2026-10-22 (AskUserQuestion), sobre o jogo da 0.3.86 (Spark j7jTrVIaPQ):

1. **Rua na duna → "Lote e rua juntos na faixa" (A5).** O autor NÃO escolheu "outras pontas"
   nem "rua corta a duna". Muda a Regra 56 D (lote só encostado em rua): na faixa anexada, a
   vila planeja o lote e a rua até ele. Esforço grande.
2. **Mina → "Alcance + insistir".** A boca nova só vale se a entrada for alcançável a pé da vila.
   O nível bloqueado tenta outro rumo ou ramal antes de abandonar a mina. Muda a ADR-037 M1. Hoje,
   BLOCKED ou EXHAUSTED em `MineDigging.java:184` são tratados como "fundo do mundo".
3. **Arenito na rua dentro da vila gerada → manter a recusa** (Regra 3). Não mexer em
   `VillageRoad.isPavableGround`.
4. **Pastor → "Aumentar o trigo".** A meta de trigo do fazendeiro passa a contar os animais. A
   faixa de colheita do `FlockHarvest` (procria até 12, abate acima de 8) continua.

Já feito antes das decisões: c6095310 (BedTeleport.SAME_PLACE = túnel + 1,5, vaivém do encalhe).

**Sub-decisões do A5 (22-10):**
- O caminho do lote até a rua **segue o terreno**: degrau de no máximo 1 (BFS), sem cortar e sem aterrar.
- O caminho tem **até 32 blocos**.
- É calçado **na abertura da obra** (`SiteOpening.pavePathTo`, como o recuo da Regra 62).
- O lote nasce **mais fundo na faixa**.

**Feito:** 252a90b6 (trigo: `scarcestSeed` + `SEEDS_FOR_SOWING`); af6391ef (mina: `MineStairPoints`, 20
partidas em 5 pontos, + `walkableFromTheVillage`). A bateria deu 837/837 ×2 depois deles.

**A5 feito:** 5ffc2a49 (`StripLot` + `RoadGrowthPlanning.lotInTheStrip` + `VillageExpansion.hasPendingStrip`;
Regra 56 emendada). 7f6fc112 marca a versão 0.3.87, com JAR 5B4DC077… instalado, NÃO jogado. Bateria
840/840 ×2, test 1560/0. Sem push nem PR.

**Armadilhas desta rodada:**
- Arquivo acima de 500 linhas reprova o `FileSizeRuleTest`. O `Mine` foi a 553, e a solução foi extrair
  o `MineStairPoints`.
- O `colony.measure()` recentra a colônia no meio da caixa. Um teste que conta com o centro original
  falha.
- O `-Werror` reprova mutação que gera código morto (`if (false && …)`). Mutar com uma expressão que
  compila limpa.

**Pendente:** jogar a 0.3.87 com um anoitecer (para o D2); o vaivém do fundidor a cada ~30 s (🟡).

**0.3.88 (22-10, depois do jogo da 0.3.87):**
- 62501caf: Regra 71, o mineiro não constrói fora da montanha.
- 70a345a2: `STRIP_AWAITS`, porque um lote em outro lugar encerrava a faixa.
- dd668591: `MineAccess`, o acesso cavado, que o autor escolheu contra "mudar a boca".
- aff29065: Regra 70, corredor de caminho.
- b1844629: versão, com JAR FBB7FB55… instalado e NÃO jogado. Bateria 846/846 e 845/846 (KF-008).
- Sem push.
- Ver [[corredor-e-mineiro-profundidade]].

**Why:** a vila do deserto ficou sem obra por falta de lote. A mina se mudou para uma entrada que
ninguém alcançava. Os mineiros passaram 71% do tempo encalhados.

**How to apply:** implementar nessa ordem (trigo → mina → A5), cada item com GameTest e mutação, na
branch `fix/deserto-a1-c1-b1-d2`. Não reabrir as opções descartadas sem um dado novo.

Relacionado: [[playtest-0385-deserto-IAXTyE0LXM]], [[movimentacao-natural]], [[abandono-por-deriva-do-centro]].

**Pastor e currais (autor, 22-10):**
- Regra 72 já feita em a3241e8f: um curral por espécie.
- Novos limites: procria até 8 e abate acima de 6, por espécie (eram 12 e 8).
- Lotação do curral pelo tamanho: 1 animal a cada 4 blocos de chão (era 5 fixos).
- Gatilho: com TODOS os currais cheios, um curral novo (animal_pen) entra na fila de obras. O autor
  escolheu isso no lugar de "mais de 20 em curral".
- Pedido junto: o bloco "caminho de areia", uma cópia do dirt_path Vanilla com a cor do arenito, que é o
  caminho exclusivo da vila do deserto.

**Jogo da 0.3.88 (Spark YFIroI1U1x), decisões e feitos (22-10):**
- 7aeee2d2: o fazendeiro alcança a 3 blocos com a borda incluída (`FarmerWork.reaches` ≤ 3; `FarmerStand` até distância² 9).
- 4427073e, escolha do autor "Nadar só na superfície": `MinerCaution` dá -1 à água só com algo sólido acima (OCEAN_FLOOR). Ao ar livre vale o custo do Vanilla. O mineiro ficou 12 min numa margem de 12 blocos.
- 1ba29753, escolha do autor "Cortar degraus" + "Trincheira de 3": `Corridor.plan(maxCut)`, `MAX_CUT = 12` (a duna sul pede 12), `LotTerraform.cutAbove`, `CorridorCut` na abertura da obra, `StripLot` com o terreno natural primeiro. NÃO visto em jogo.
- Save do mundo legível offline: `scratchpad/blocks_around.py`, `reach_bfs.py`, `heights.py`, `ramp.py` (nbtlib; rodar com `python -P`, sem `-I`).
- 89d3c8ea pastor 8/6 + lotação 1/4 de chão + `PenPlans`/`UrgentPlans`; 99ad6810 caminho de areia (`SandPathBlock`, Regra 73, só no bioma deserto); 8028ba7b versão 0.3.89, JAR 2F8E0DDD… instalado, NÃO jogado. Bateria 853/853 em 3 de 4 (KF-009 MineReturn, isolado 4/4). PR #31 mesclada na main (f88c4646) em 22-10 a pedido do autor; o CI da main ainda rodava no merge, e o CI da main da PR #30 tinha falhado.
- **0.3.90** (PR #32, 3679c056): corrige o crash da 0.3.89 (Corridor `State` Comparable) e o PIT do Core (84 → 85,2%). JAR A378F6DA… instalado e NÃO jogado. O CI da main, depois do merge, ainda não foi visto.
- **Jogo da 0.3.90 (Spark LYMx4yHjKk, 12:52–13:20), nota 7,6:**
  - A faixa deu o 1º lote: um curral no platô, com 486 blocos cortados e `sand_path` num vale de 3, conferido no save.
  - A busca com corte travou o servidor 7× por 3 s.
- **0.3.91 (PR #33, branch fix/corredor-teto-de-tempo, NÃO mesclada):**
  - memória por passo (`Move`/`Expansion`), teto de 50 ms (`OUT_OF_TIME`), StripLot retoma do mesmo tamanho de casa;
  - runGametest apaga o mundo de teste;
  - ColonyDetection em lote próprio; Crafting usa `runColonyCycleNow`.
  - JAR F5826A0D… instalado, não jogado.
- **Abertos:**
  - mineiros andam 36% (cama no alto, mina no lago);
  - `storage_majest` pede deepslate/calcita no deserto;
  - KF-007.
- **PR #33** (fix/corredor-teto-de-tempo): 0.3.91, 0.3.92 (Regra 74), KF-007 corrigida (d78566a5) e o registro das regras 75–109, com o README gerado (13e1bf9d).
  - O autor pediu "mescla quando passar".
  - O repo NÃO permite auto-merge, e o CI do 13e1bf9d ainda estava pendente às 18:16.
  - Próximo passo: ler o CI (uma vez) e mesclar com `gh pr merge 33 --merge` se estiver verde.
- **Regras:** `scripts/rules_to_readme.py` gera a seção do README a partir do RULES.md, e `tests/test_rules_to_readme.py` reprova se o README divergir. Toda regra nova exige rodar o script.
- **PR #33 mesclada** (6b898574) com o CI verde. A main está na 0.3.92.
- **Repositório** (pedido do autor, 22-10): `allow_auto_merge` ligado, e a main exige o check "Build and test". Sem exigir revisão e sem valer para o admin.
  - Para mesclar quando passar: `gh pr merge N --merge --auto`, ou o `set_auto_merge` do app.
  - Com o CI já verde, o GitHub recusa o auto ("clean"); aí é só mesclar direto.

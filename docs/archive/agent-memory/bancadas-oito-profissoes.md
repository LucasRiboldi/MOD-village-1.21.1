---
name: bancadas-oito-profissoes
description: 0.3.62 (15-10) — cada uma das 8 profissões ganhou bancada do mod (antes só carpinteiro/pedreiro/fundidor). Texturas 16x16 pixel-art geradas por scripts/assets/gen_benches.py. benchBlock/benchPoi viraram switch exaustivo.
metadata:
  node_type: memory
  type: project
  modified: 2026-10-15T06:04:37.325Z
---

**0.3.62 (15-10):** o autor pediu bancada de trabalho para **todas as 8 profissões** e texturas novas
(mandou 7 mockups de alta-res). Antes só 3 tinham (`CARPENTER_BENCH`, `MASON_CUTTER`,
`BLACKSMITH_FORGE`). Adicionadas: `LUMBERJACK_BENCH`, `FARMER_BENCH`, `SHEPHERD_BENCH`,
`BUILDER_BENCH`, `MINER_BENCH` em `ModContent.java`.

**Padrão por bancada** (replicar se adicionar outra): bloco `new Block(Settings.copy(base).luminance(10))`
+ `registerBlock` + `registerPoi` + entrada no criativo (`ItemGroups.FUNCTIONAL`); recursos
`blockstates/<n>.json`, `models/block/<n>.json` (cube, up/front/side + down=planks ou cobblestone),
`models/item/<n>.json`, `data/.../recipe/<n>.json` (GGG/GSG/GGG, gold nos cantos, estação Vanilla
temática no centro), lang em en_us/pt_br/pt_pt. `benchBlock`/`benchPoi` são switch **exaustivo** (sem
default — o compilador exige todas as 8).

**Texturas:** 16x16 pixel-art feitas à mão (NÃO downscale dos mockups — o autor escolheu pixel-art
nova). Geradas por `scripts/assets/gen_benches.py` (Pillow; rodar com `OUT_DIR=<dir> python ...` e copiar
para `textures/block/`). Motivos: serra+machado (lenhador), fogo (forja), trigo+foice (fazendeiro),
lã listrada+tesoura (pastor), planta da casa+esquadro (construtor), picareta+minério (mineiro).
**Pendente de refino** (visto no preview): o machado do carpenter_side parece martelo, o balde do
pastor ficou "caneca" — ajustar se o autor pedir, depois de ver em jogo.

**Teste atualizado:** `ProfessionBenchGameTest.aProfessionWithoutAModBench` (afirmava que o mineiro
NÃO tinha bancada) virou `theMinerDoublesAtItsOwnBench` + loop provando que nenhum `benchBlock` é nulo;
o teste de luz cobre as 8. test 1523/0; runGametest 769/769.

**ATENÇÃO instalação:** JAR 0.3.62 (0D3C90EF…) buildado mas **NÃO instalado** — a 0.3.61 estava em
playtest. Instalar com `scripts/install-mod.sh` e confirmar no log (ver [[jars-nao-instalados-deslocamento]]).

**Atualização 0.3.63 (15-10, pedido do autor):**
- **Luz 10→14** (a da tocha): `LIGHT=14` em ModContent, impede spawn de mobs.
- **Player usa as bancadas:** as 6 sem estação Vanilla (fundidor, lenhador, fazendeiro, pastor,
  construtor, mineiro) viraram `CraftingTableBlock` (abrem grade 3×3). Pedreiro=stonecutter,
  carpinteiro=crafting. **Fundidor = crafting, NÃO fornalha real** — FurnaceBlock custom exige
  BlockEntity registrado (o Vanilla valida o bloco e trava) e arrisca perda de item; decisão do autor.
- **Texturas HD 64×64** recortadas dos mockups 1254px do autor (`scripts/assets/crop_benches_hd.py`):
  grade 4col×3lin, faces TOPO(col0)→top, FRENTE(col1)→front, LAT ESQ(col2)→side. Mapa: img1 lenhador,
  2 carpinteiro, 3 pedreiro, 4 pastor, 5 construtor, 6 Mesa genérica→mineiro, 7 fornalha→fundidor.
  Farmer (sem mockup) ficou com o pixel-art ampliado a 64px. **16px vira borrão, HD 64px fica nítido**
  (testado). O mod agora mistura HD (bancadas) com 16px (resto) — contraste aceito pelo autor.
- **Dobro do PLAYER** (craft/produção) **NÃO feito** — fica para ciclo próprio (mecânica de duplicação
  de item é delicada, exige Mixin no resultado do crafting). Só o dobro do aldeão por ora.

Relacionado: [[bancadas-do-mod]], [[jars-nao-instalados-deslocamento]].

---
name: bancadas-telas-e-biomas-plano
description: "20-10 — DEFEITO GRAVE: telas Vanilla (crafting/stonecutter/loom) exigem o bloco Vanilla exato e fecham com o bloco do mod; as bancadas da 0.3.63 NÃO funcionam para o jogador. Plano aprovado: bancadas (telas próprias, fornalha real, tear, composteira, cubo cheio, 6 faces recortadas certo) e depois simulação por bioma com chave só de teste."
metadata:
  node_type: memory
  type: project
  modified: 2026-10-20T16:20:07.399Z
---

**Defeito confirmado por javap (MC 1.21.1, yarn build.3):** `CraftingScreenHandler`, `StonecutterScreenHandler` e
`LoomScreenHandler` fazem `canUse(context, player, Blocks.CRAFTING_TABLE / STONECUTTER / LOOM)`. Com bloco do mod
a tela abre e fecha na hora. A 0.3.63 disse "o player usa as bancadas" sem testar em jogo — **estava errado**.
Correção: subclasse do ScreenHandler que guarda o próprio context e confere o bloco do mod, e o bloco sobrescreve
`createScreenHandlerFactory`. O cliente continua usando o tipo Vanilla (canUse só vale no servidor).

**Decisões do autor (20-10) para as bancadas:**
- Função Vanilla por ofício: carpinteiro=mesa; pedreiro=cortador; **fundidor=fornalha de verdade** (BlockEntity
  próprio — revê a decisão de 15-10 "fundidor = crafting"); pastor=tear; fazendeiro=composteira;
  lenhador/construtor/mineiro (sem equivalente)=mesa.
- **Cubo cheio** para todos, com colisão de cubo (substitui a escolha anterior "pedreiro baixo").
- Texturas: **recortar certo dos mockups** (as faces south/down da 0.3.65 pegaram a tarja da legenda
  "FRENTE"/"TOPO"; o south de vários repetia o topo) com 6 painéis diferentes; **fazendeiro** ganha arte nova.
- Também achado: os blocos do mod **não têm loot table** → quebrar não devolve o bloco. Incluir.

**BANCADAS FEITAS (0.3.68, commit 690dee98, branch fix/varredura-g1-g3, push feito, sem merge, não jogado):**
telas próprias (BenchCraftingBlock/StonecutterBlock/LoomBlock), forja = fornalha real (BenchForgeBlock +
BlockEntity), composteira (BenchComposterBlock), cubos cheios, blockstate por facing, loot tables + tags,
recorte novo pela grade real (`crop_benches_6faces.py` acha as faixas de legenda), arte nova de fazendeiro e
mineiro (`art_farmer_miner.py`). BenchUseGameTest com controle Vanilla; test 1534/0, runGametest 773/773.
Lição: o "funciona" da 0.3.63 era afirmação sem teste — agora há GameTest com controle.

**Depois: biomas** (pedido do autor, "muito importante"): bancada de simulação — servidor dev sem tela
(`runServer`), mundo de bioma único por bioma, `/place structure` (5 vilas Vanilla + planície forçada em
biomas sem vila), `/tick sprint`, chave **só de teste** `-Dvillagecolony.simulate=true` para a colônia não
pausar sem jogador; medir com time_ledger/action_report/cost_ledger por bioma; mais previsão por leitura de
código (recurso de cada profissão por bioma). Hoje o mod reconhece 5 estilos (VillageBiomes: plains, desert,
savanna, taiga, snowy); o resto cai na planície. Só a planície foi jogada.

Pausado pela troca de rumo: pedreiro baixo (substituído por cubo cheio), 3 faces pixel-art do fazendeiro
(substituído por arte nova), medição road-only do índice de ruas (ainda pendente, não começada).

Relacionado: [[varredura-2026-10-20-graves]], [[bancadas-oito-profissoes]], [[roteiro-ociosidade-pendente]].

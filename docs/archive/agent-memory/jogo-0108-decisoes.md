---
name: jogo-0108-decisoes
description: Jogo da 0.3.108 (29-10) — defeitos achados e escolhas do autor para solo largo, mineiro e casa de dois andares
metadata:
  type: project
---

Diagnóstico em docs/research/2026-10-29-jogo-0108.md (98 min, planície, 13 obras, 25 ms/tique).

**Escolhas do autor (29-10):**
- Solo fora da vila = opção A ([[solo-suave-da-vila]]): mistura suave; degrau ≤1 só no miolo.
- Alcance: AMPLIAR a área da vila (caixa da estrutura +64 só para vilas, no lugar do +12 Vanilla de
  Structure.expandBoxIfShouldAdaptNoise) — sem isso chunk além de peças+12 nunca recebe a referência e não é moldado.
- Largura da transição: 4× o desnível, de 16 a 64.
- Árvore e rocha sobre chão cortado: tirar inteiras.
- Mineiro: NÃO quebra bloco que tenha em cima cascalho/areia (bloco que cai), água ou lava (morte do 4364ea75 por cascalho).
- Casa de dois andares (plains_small_house_5): aldeão da colônia não recebe cama no andar de baixo (9 de 21 encalhes).
- Portão da mina fora da entrada real: NÃO escolhido agora (fica no TODO). Argila, fazendeiro, construtor sem material, lenhador/pastor sem caminho: só registrados.

**Também:** CI da PR #39 falhou no PIT (84 < 85) — o VillageSurface novo precisa de testes que matem mutantes.

**Feito (29-10, branch fix/playtest-0102, não enviado ainda):** d1fc2d12 mineiro looseAbove (Regra 157); 6561ccda cama
fora do andar de baixo (Regra 158, BedPlan.leave, GeneratedVillages.villagesReaching filtra pela caixa do jogo);
5c56455a solo VillageBlend + Mixin referência +64 + árvore inteira (Regra 152 refeita). 1654 unit, 936 GameTests, 153 Py.
Linha de base da 0.3.108 (paredões/no ar, janela 200×200, 46 cenários) em docs/research/2026-10-29-solo-base-0108.tsv.
**Depois:** 0cfb7252 testes de mutação (PIT local 2450/2882 = 85%, margem de 1); 6ff417f4 release 0.3.109 (JAR D056EB60…0F90 instalado, não jogado), enviado à PR #39 (auto-merge). Bancada de todos os biomas com a 0.3.109 rodando no worktree vc-sim-all (6ff417f4); comparar com docs/research/2026-10-29-solo-base-0108.tsv. Portão da mina fora da entrada real: pendente (não escolhido).

**Bancada 0.3.109 (46 cenários) PIOROU** (paredões 18205→22438): offset sobre altura interpolada somado ao topo real +
arredondamento por coluna + margem da água subindo. Corrigido em 029f170f (Shape.target = round((1−w)·topo + w·nível),
peso some em 8 até a água, SAMPLE 8: plano 20 s → 5–6 s). Seis cenários contra a 0.3.108: chão sobre vão cai em todos,
maior degrau igual, paredões +8–25 % entre 8 e 24 blocos da vila (relevo natural que a faixa de 16 aplainava).
74ce098c docs; JAR 0.3.109 refeito (EEAC3D63…5A17) instalado, não jogado; PIT 2514/2957; enviado à PR #39.
Opção não feita: miolo de degrau 1 até peças+16 (CORE_PAD 16) para tirar os paredões perto da vila.

**Jogo 0.3.109 (29-10 13:17–14:14, MESMO mundo "Novo mundo" — solo novo NÃO exercitado, 0 VC_GROUND):** 1 obra em 53 min;
construtor preso 3× no telhado de casa em 3128,73,1341–1346 (sai "after 0 steps") e a casa ficou 32 min com 1 peça,
fechada por tempo (236/267); mina desceu a y16 e recomeçou a escada em 3072,73,1281 (mineiros não chegavam, Regra 119);
casa de escada de mão ainda prende (d70aeeb9 em 3077,84,1310; lenhador 3× em 3056–3059,1308–1311) — a regra da cama não
basta; pastor cercado 4× (curral sem portão); looseAbove disparou 0×; pedreiro 88% ocioso; 16 brewing_stand de blaze_rod surgido.

**Escolhas do autor após o jogo da 0.3.109 (29-10):** corrigir (1) mina que recomeça — NÃO abrir várias bocas: usar
a mesma primeira descida e continuar para outro lado (mudar a ordem/direção); (2) curral sem portão + laço da vara de
blaze; (3) construtor no telhado — o construtor NÃO precisa subir nem escalar: estar a até 2 blocos da área da obra basta.
Texto livre cortado: "todos aldeões devem ser capazes de pular dois blocos e pular cerca ce…" — confirmar o resto e o
desenho (mexe na navegação de todos os aldeões; pular 2 é mais que o Vanilla de 1). Casa de dois andares não escolhida agora.
**Confirmado:** frase = "pular dois blocos e pular cerca e cercado"; desenho = PULO DE VERDADE: a navegação de todo
aldeão da colônia aceita degrau de 2 e cerca, e ele pula mais alto nesse ponto (só aldeões da colônia).
Ordem planejada: construtor (até 2 blocos basta) → mina (mesma descida, outro lado) → curral/blaze → pulo de 2 (pesquisar
LandPathNodeMaker/step height antes — minecraft-code-research).

**Desenvolvido 29-10 SEM rodar testes (autor: "evite os testes nesta chamada, só desenvolva") — NÃO commitado:**
RoofDescent (+ gametest RoofDescentGameTest registrado, não rodado) ligado no StrandedEscape (499 linhas); BuilderApproach
só pé até chão+1; MineRestart volta à mesma boca e vira (descent.clockwise), boca nova nunca; ColonyPathing.stepFor +
LandPathNodeMakerMixin @WrapOperation MobEntity.getStepHeight (getSuccessors/getStepHeight) = 2.0 p/ aldeão da colônia,
canWalkOverFences, jump_strength +0.2 (modificador villagecolony:colony_jump). Regras 159/160 + emenda 119 no RULES/README.
Laço da blaze NÃO é defeito: estoque "um pouco de tudo" (16 de cada). Curral sem portão: resolvido pelo pulo de cerca.
Compila (compileJava/compileGametestJava). Falta: rodar unit + gametest (MineRestartGameTest pode depender da boca nova),
commit, versão, JAR.
JAR 0.3.110 (7FC3F2E9…42A5) instalado a pedido do autor, construído SEM testes (build -x test -x runGametest -x pmdMain), código não commitado; gradle.properties já em 0.3.110 no disco.

**Jogo 0.3.110 (29-10 14:50–15:13):** construtor ainda preso no telhado 3128,73,1341 — telhado a só 2 do chão (DROP era 3);
pastor preso no cercado novo 3083,77,1297 porque o miolo foi tampado com grama à altura da cerca pelo alisamento
(GroundSmoothing via vão entre cercas = buraco). Pedidos do autor: cercado com miolo livre; aldeão SE JOGA do telhado;
Regra: comer/receber comida = vida máxima.
**Feito sem testes (não commitado):** RoofDescent DROP 2 + pulo (velocity 0.5 rumo ao chão, 3 tentativas, depois teleporte);
GroundSmoothing pula caixas de construção/obra (+1) e clearPens limpa miolo dos cercados (até 3 acima do piso);
VillagerMeals + VillagerEntityMixin (consumeAvailableFood/eatForBreeding/loot de comida → vida cheia). Regras 160–162.
JAR 0.3.110 refeito (E8493E8B…ABC1) sem testes. Atenção: O2 (aroundTheOpenBuild) agora não mexe dentro do lote da obra.

**29-10 ~15:55:** 878d2096 (commit não testado, pedido do autor) + e92e1fea (bateria verde 939/939, 1663 unit, 153 Py; JAR 0.3.110 BCB95743…D9C0 instalado). Enviados à PR #39. Instáveis vistos 1× cada: ShepherdHerding (vaca solta), ShepherdFeeding (galinhas), Builder groundBlocksAreShapedWithoutTheChest — talvez ligados ao pulo de cerca. Regras 161/162 sem teste próprio. CHANGELOG/STATE da 0.3.110 ainda não escritos.

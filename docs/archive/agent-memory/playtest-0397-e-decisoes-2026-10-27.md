---
name: playtest-0397-e-decisoes-2026-10-27
description: "Playtest 0.3.97 (Spark n4DVevJAmw, savana, 38 min) — 7 defeitos achados e as escolhas do autor para tocha, mina, 5 min, horta, nomes coloridos, curral, nivelamento, árvore rara, receitas, bancadas"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-28T02:24:03.608Z
---

**Playtest 0.3.97 (27-10, savana, Spark n4DVevJAmw):** TPS 20, MSPT ~18, mod 1,4%. Ócio: pedreiro 68%, construtor 59%, lenhador 39%.

**Defeitos (corrigir sem perguntar):**
1. Obra presa 15 min: `FoundationPreparation.scan` recusa a base porque a água/farmland da PRÓPRIA planta (horta na camada da rua) conta como "gap with fluid".
2. O pastor dá trigo para reproduzir enquanto a obra pede trigo (19 pedidos).
3. Fundidor: pedra lisa precisa de stone; 626 cobblestone no baú e "nenhum baú tem stone" 806×. Falta a cadeia cobble→stone.
4. Tocha invisível: os pacotes de textura (Faithful/FA+Details) trocam `minecraft:block/template_torch`. Precisa de modelo próprio, versão de parede e item `VerticallyAttachableBlockItem`.
5. Tocha da mina na parede cai quando o mineiro cava a parede.
6. A escada da mina pode ser fechada pelo `MineSettling.shoreUp`/`MineFlooding.seal`.
7. Rede de caminhos sem nenhum traçado: `PathLinks.entranceOf` só olha a borda da caixa; nas casas Vanilla a porta fica recuada.

**Escolhas do autor:**
- **5 min:** vigia em dois degraus. 5 min sem trabalho → larga a tarefa e entra na cadeia de ajudas. 10 min com a mesma peça → a peça surge.
- **Horta da obra:** o construtor faz no lugar (farmland = terra + enxada, trigo como semente, água grátis).
- **Nomes:** cor no rótulo atual. Laranja = ofício, azul = ajuda, vermelho = travado >30 s, cinza = fora do expediente.
- **Tocha da mina:** no chão, a cada 12, nunca no degrau nem no canto da próxima descida.
- **Nivelamento:** anel de 2 na altura da rua ±1 + rampa de até 3, feito pelo construtor ANTES da obra.
- **Curral:** com 5 amarrados no mesmo poste, abre obra de curral com prioridade. Depois o pastor leva 1 ou 2 por vez no laço.
- **Lenhador:** 1 em cada 10 mudas da borda é de outra espécie, sorteada.
- **Receitas:** atual + 1 diamante (G D G / . S . / G . G). Tocha = 4 tochas + 1 ouro → 4. Caminho de areia pela pá.
- **Bancadas:** painel com shift+clique + PESQUISAR e PROPOR habilidades passivas por bancada (perguntar antes).

**Feito na branch `fix/playtest-0397` (com push, sem PR; runGametest 884/884, nada jogado):**
- 81598c45: base, fundidor, porta recuada. A reserva de trigo do pastor foi DESFEITA em a408cdb3.
- fe002e0a: tocha (modelo, parede, saque) e receitas.
- d56278f5: mina (tocha de chão em grade, selo que drena, caminho longe da boca).
- a408cdb3: horta sem pedido (`AppearingPieces.needsNoStock`).
- bd6b187e: árvore rara.

- 722eace3: nomes coloridos (Regra 129, `WorkTime.Badge`) e vigia (Regra 128, `IdleWatch`); 886/886.

- fc9832ec: curral (Regra 130); 5fe432dc: nivelamento (Regra 131, `SiteSurroundings`); 888/888.

**Passivas das bancadas escolhidas pelo autor (27-10, valem só por a bancada estar na vila):**
- **Carpinteiro:** ferramenta criada na vila ganha encantamento aleatório; repara a ferramenta de quem está a até 8 blocos.
- **Lenhador:** árvores a até 32 crescem mais rápido.
- **Pedreiro:** tapa buracos de rua a até 32; +1 pedregulho a cada 4.
- **Fundidor:**
  - restaura armadura a até 8;
  - armadura criada na vila ganha encantamento aleatório;
  - fornalhas a até 32 são 2× mais rápidas.
- **Fazendeiro:** roça rápida a até 24; compostagem (sobra vira farinha de osso).
- **Pastor:**
  - um par cruza sozinho a cada 10 min abaixo da meta, a até 36;
  - lã rápida a até 36;
  - itens que os animais derrubam vão sozinhos para os baús;
  - abate rende o dobro de carne e couro.
- **Construtor:** a obra a até 48 assenta 1 bloco por minuto sozinha; o material da obra vai para o baú do construtor.
- **Mineiro:**
  - nenhum monstro nasce a até 64 da boca da mina;
  - minério de ferro, cobre, carvão, ouro, lápis e redstone rende o dobro;
  - diamante, esmeralda, ametista e quartzo rendem +1;
  - 1% de chance de cair uma ferramenta do material (ferro, ouro, diamante).

- 8bd51ca2: painel e passivas (Regra 132).
- 801714cc: versão 0.3.98, JAR F6FC2F1B… instalado, **PR #37 aberta, sem mescla**.
- Verificado: 892/892 e build OK. **Não jogado.**

**Limites ditos ao autor:**
- a escada em caracol não leva tocha;
- a proteção do caminho cobre só a entrada do nível atual da mina;
- o encantamento só vale para o que o jogador fabrica.

**How to apply:** branch nova a partir da main (ADR-035), um marco por commit verificado. Ligado a [[decisoes-autor-2026-10-27-sete-itens]] e [[rede-de-caminhos-decisoes]].

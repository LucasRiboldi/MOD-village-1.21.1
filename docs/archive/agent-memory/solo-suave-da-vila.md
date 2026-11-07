---
name: solo-suave-da-vila
description: Decisões do autor 2026-10-29 para o solo padronizado da vila na geração do chunk (beard_box não bastou) e a fila T1/T5/O1–O8 pedida junto
metadata:
  node_type: memory
  type: project
  modified: 2026-10-29T03:30:30.164Z
---

Autor jogou a 0.3.106 (29-10): beard_box não deixou o solo suave; vilas com crateras e no meio de montanhas.
Prioridade: solo padronizado em toda a área da vila, integrado ao mapa, buracos tampados, caminhos suaves.

**Escolhas (AskUserQuestion 29-10):**
- **Quando:** na geração do chunk (não em runtime, não por aldeão) — só mundo novo; vila que cresce depois não é aplainada por isso.
- **Montanha:** vira encosta suave (vizinho ±1, corta o mínimo), não platô.
- **Água:** lago/rio ficam, margem suave; buraco seco e caverna aberta são tampados.
- **Borda:** transição de 16 blocos até o terreno natural.

**Desenho:** gancho no HEAD de ChunkGenerator.generateFeatures (antes das peças SURFACE_STRUCTURES): superfície alvo por
vila (cache por start), âncoras = piso das peças rígidas (box.minY + groundLevelDelta − 1), água fixa, anel externo de
16 = natural; relaxação Lipschitz-1 (4 vizinhos) perto da altura natural (getHeight do gerador, amostrado). Aplica só
no próprio chunk: corta/aterra até a superfície com o bloco de topo/subsolo da coluna; tampa vãos logo abaixo.
Ruas (terrain_matching) seguem o terreno novo sozinhas. Verificação real: biome_sim.py (mundo novo) + mapa de altura.

**Também pedido no mesmo dia (executar depois do solo):** T1 (neve surge automática; lenhador corta a espécie pedida;
madeira equivalente após N tentativas), T5 (argila surge automática; vidraça areia→vidro→vidraça no fundidor) e
todos os O1–O8 de [[decisoes-0103-correcoes]].

Relacionado: [[plano-terraplanagem-vila]], [[caminho-rente-ao-chao]], [[playtest-0104-savana]].

**Feito:** núcleo 3a959f9e; geração 50a04347 (Regra 152; degrau>1 na área: deserto 4,29→0,19%, savana 2,11→0,10%, neve 1,95→0,42%); CI com Pillow d11c490b (PR #38 falhava por PIL); T1-neve + T5 argila/vidraça 4f242886.
**Feito também:** lenhador por espécie e8cf9469 (WantedTree; madeira equivalente já valia pela Regra 27 ALTERNATIVE — dito ao autor).
**Feito também:** O8/O3/O4/O1 94ab8363 (Regra 153; O1 "ajudar a assentar" NÃO feito).
**Feito também:** O5/O6/O7 7d1d2b8b (Regra 154: árvore fora 1 dia, bosque 4–6, areia/argila +40, composto). Release 0.3.107 (SHA 4EEA0884…34FB) instalada, não jogada.
**Falta:** O2 (construtor com obra parada busca o que falta / prepara próximo lote), O1 'ajudar a assentar', O7 'aplainar canteiro antigo'. PR #38 auto-merge (CI).

**29-10, rodada 2 (pedido do autor):**
- O2 = construtor com obra parada limpa e nivela o lote (da próxima obra).
- Pedreiro alisa a vila inteira aos poucos: **qualquer tamanho com degrau > 1** vira rampa de degrau 1 (como a geração), **qualquer altura**, pondo/tirando solo; nunca toca construção/peça da vila/obra/bloco do jogador, roça e água, boca e escada da mina. Plano: VillageSurface numa janela (~32×32) que varre a caixa, com tipo novo "não restringe" para o que não é chão; poucos blocos por passada.
- Portão da mina = `Majest_entrada_mina.nbt` (9×7×5, vão de 3 no meio ao longo de z) no lugar do arco do MineMouth, centro (4,0,2) sobre a boca, vão girado no rumo da descida; **baú e barril só enfeite**.
- Depois: commit, push, merge (PR #38), JAR, README.

**Rodada 2 feito:** portão da mina 5d13fd77 (Regra 155; centro do vão na pista do meio — as 3 pistas saem da boca para a esquerda da descida; pos do NBT do autor é IntArray). **Rodada 2 concluída:** calombo ≤4 cortado + SKIP c078ce9f; pedreiro alisa (Regra 156, GroundSmoothing janela 24, 32 colunas) 1383684c; bancada 100×100 todos os biomas fac49fa7; O2 a3b9850d; release 0.3.108 16ff22f2 (SHA A4C61D2F…65D0, instalada, não jogada). PR #38 já tinha mesclado ANTES desses commits → PR #39 aberta com auto-merge. **Falta:** O1, O7, resultado da bancada de todos os biomas.

**29-10, jogo da 0.3.108:** autor viu o solo FORA da vila agressivo (montanha cortada ao meio, partes voando). Causas no código: anel FIXED a 16 + Lipschitz 1 → "meio" entre faixas = paredão; `topOf` pula folha/tronco e só corta a coluna → árvore e beiral no ar. Pesquisa (sem código) em docs/research/2026-10-29-solo-fora-da-vila.md: beardifier Vanilla (gauss e^-d²/16, alcance 12), WorldEdit //smooth (gauss + estica coluna), Lithostitched (mixin no beardifier). Recomendado A: mistura gaussiana larga 32–48 fora, degrau ≤1 só no miolo, consertar árvore/beiral. Escolhas: A + largura proporcional ao desnível (com teto) + árvore/beiral tirados inteiros. Não codificado (autor pediu só pesquisa).

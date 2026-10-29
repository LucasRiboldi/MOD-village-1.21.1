# Solo em volta da vila: por que ficou agressivo e como outros fazem (2026-10-29)

Pedido do autor depois de jogar a 0.3.108: o alisamento para fora da vila "quebrou uma montanha no meio e deixou
partes voando"; deve ser menos agressivo, mais espaçado, com aparência natural, podendo abranger área maior. **Só
pesquisa — nada codificado.**

## 1. O que o nosso faz hoje (Regra 152, `VillageGround` + `VillageSurface`)

- Área = caixa das peças da vila + `MARGIN` 16. O anel de fora é FIXED na altura natural.
- Toda coluna livre obedece degrau ≤ 1 com as 4 vizinhas (Lipschitz 1 = rampa de 45°), partindo do terreno tampado.
- [FATO] `VillageSurface.smooth`: quando duas fixas não cabem no degrau de 1 (piso da casa a 70, anel na montanha a
  110, só 16 colunas entre eles), cada coluna fica no **meio** entre as duas faixas — o salto que sobra vira um
  paredão num ponto só. É o "quebrou a montanha no meio".
- [FATO] `VillageGround.topOf` pula folha e tronco ao achar o topo, e `column` só corta da superfície para baixo.
  A árvore sobre o chão cortado e o beiral de rocha acima do topo ficam no ar — as "partes voando".
- [INFERÊNCIA] Mesmo onde cabe, a rampa de 45° com degrau exato de 1 é uma escada regular, sem cara de terreno.

## 2. Como o Minecraft faz (fonte local, Yarn 1.21.1)

`net.minecraft.world.gen.StructureWeightSampler` (o "beardifier"):
- Roda **na densidade do ruído**, antes da superfície: a pedra nasce já na forma nova. Por isso não sobra nada
  voando, cavernas e minério continuam coerentes, e a grama/areia da superfície vem por cima normalmente.
- Peso gaussiano `e^(-d²/16)` numa grade de 24 (alcance de 12 blocos, quase nada além de 8). `beard_thin` e
  `beard_box` somam densidade sob a peça e tiram acima dela, com peso 0,8.
- A vila Vanilla já usa `beard_thin`; a 0.3.105 trocou para `beard_box` e não bastou (alcance curto demais para
  montanha).

## 3. Como outros fazem

| Fonte | Como | O que serve para nós |
|---|---|---|
| **WorldEdit `//smooth`** ([HeightMap](https://github.com/EngineHub/WorldEdit/blob/master/worldedit-core/src/main/java/com/sk89q/worldedit/math/convolution/HeightMap.java), [GaussianKernel](https://github.com/EngineHub/WorldEdit/blob/master/worldedit-core/src/main/java/com/sk89q/worldedit/math/convolution/GaussianKernel.java)) | Mapa de alturas → desfoque gaussiano (raio 5, σ 1, N passadas) → regrava. Ao subir/descer, **estica a coluna inteira** (camadas proporcionais), não só o topo. O próprio código deixa árvore no ar ("Drop trees to the floor — TODO"). | Desfoque gaussiano dá encosta arredondada e natural; esticar a coluna mantém as camadas. Mesma falha nossa com árvores. |
| **Lithostitched** ([BeardifierMixin](https://github.com/Apollounknowndev/lithostitched/blob/main/src/fabric/21.1/main/java/dev/worldgen/lithostitched/mixin/common/BeardifierMixin.java)) | Mixin no beardifier que troca o tipo de adaptação por peça; usa os pesos Vanilla. | Prova que mexer no beardifier por Mixin é o caminho que a comunidade usa e que convive com outros mods. Não aumenta alcance. |
| **ChoiceTheorem's Overhauled Village** | Vilas que "se misturam ao terreno"; a página não diz como. | Nada verificável. |
| **Countered's Terrain Slabs** ([repo](https://github.com/Coun7ered/terrain_slabs_multiloader)) | Lajes ao lado de bloco cheio para suavizar o andar. | Ideia para o degrau de 1 parecer rampa, não para a forma. |
| [Pedido no feedback da Mojang](https://feedback.minecraft.net/hc/en-us/community/posts/39203733004685-Terrain-Adaptation-Template-Pool-Element-for-Jigsaw-structures) | Diz que beard_thin/box/bury/encapsulate lidam mal com desnível vertical grande. | Confirma o limite do Vanilla. |

Não achei mod Fabric 1.21.1 que aplaine a vila inteira na geração; o mais próximo é o beardifier e o `//smooth`.

## 4. Caminhos possíveis

- **A. Mistura gaussiana larga, no mapa de alturas (o que temos, refeito).** Dentro da vila: degrau ≤ 1 como hoje.
  Fora: `alvo = w(d)·vila + (1−w(d))·natural`, com `w` caindo suave (smoothstep/gaussiana) de 1 na borda das peças a
  0 a 32–48 blocos; depois um desfoque gaussiano leve. A montanha vira encosta larga com a forma dela, sem paredão;
  degrau maior que 1 volta a existir longe da vila, como na natureza. Mais o conserto do que voa: árvore cortada
  inteira, beiral acima do topo removido, coluna esticada como no WorldEdit.
- **B. Beardifier com alcance maior (densidade, como o Vanilla).** Um Mixin no `StructureWeightSampler` dá à vila um
  peso gaussiano de alcance 24–32 em vez de 12. Fica natural de verdade (pedra, cavernas, nada voando), mas o
  resultado é "terreno puxado", não degrau ≤ 1 garantido; o nosso pós-processo de hoje ainda teria de alisar o
  miolo. Mais arriscado com mods de terreno (Terralith, Tectonic) e com o custo por bloco do ruído.
- **C. Só consertar o que voa e alargar a margem.** `MARGIN` 16 → 32 e degrau livre fora do miolo, sem mudar o
  método. Menor, mas a escada de 45° continua com cara de artificial.

Recomendação: **A**. Mantém o que já funciona dentro da vila (bancada de 41 biomas abaixo), resolve os dois
defeitos vistos e é verificável pela mesma bancada.

## 5. Bancada de todos os biomas (0.3.108, dia 0, janela 100×100 em volta da vila)

Degrau > 1 nos pares de colunas vizinhas: 0,03–0,2 % em 33 dos 46 cenários. Acima de 1 %: picos de gelo 13,8 %
(máx. 18), badlands erodidas 7,3 % (máx. 52), bosque nevado 1,95 %, encosta nevada 1,9 %, badlands e badlands
arborizadas 1,7 % (máx. 31), deserto fixo 1,5 % (máx. 31), planície de girassóis 1,15 % (25 colunas em buraco).
[HIPÓTESE] Os máximos altos são relevo natural dentro da janela de 100 (pico de gelo, chaminé de terracota) e
não o solo da vila; a bancada ainda não separa "dentro da área" de "fora". Nenhum destes números mede o que o autor
viu (corte na montanha da borda): falta uma medida de **profundidade de corte na margem** e de **bloco solto no ar**.

## 6. Escolhas do autor (2026-10-29)

- **Método:** A — mistura suave larga; degrau ≤ 1 só dentro da vila, fora o chão vai da altura da vila à natural
  mantendo a forma do terreno.
- **Largura:** proporcional ao desnível entre a vila e o terreno natural, com um teto.
- **No ar:** a árvore sobre chão cortado sai inteira (tronco e folhas), e a rocha saliente acima do corte também.
- A correção vem com a medida que falta na bancada: profundidade de corte na margem e bloco solto no ar.

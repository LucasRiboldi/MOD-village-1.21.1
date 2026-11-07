---
name: mina-longe-por-vila-grande
description: Playtest 14-10 — mineiro 0% porque a boca da mina fica num canto de uma vila de 132x152; o alvo a ~95 blocos
metadata:
  node_type: memory
  type: project
  modified: 2026-10-14T03:26:32.554Z
---

Playtest 14-10 (Spark MiIM0AisSh, 12 min, ~84% à noite/"off hours" — 54 off vs 10 work, por isso
"tudo travado" foi muito a noite, não bug). TPS 20. Investigação pedida pelo autor: **por que a
mina ficou longe.**

Lido do save (`villagecolony_colonies.dat`, parser scratchpad/readdat.py):
- Vila bounds: X 225→357 (**132 blocos**), Z 1147→1299 (**152 blocos**) — vila grande.
- Centro: (291, 94, 1223) — canto **nordeste** da caixa.
- Boca da mina: entryX=250, entryY=90, entryZ=1158 — **sudoeste**, na borda sul (boundsMinZ=1147).
  descent=NORTH, gallery=SOUTH, **cut=391** (desceu fundíssimo).
- Distância centro→boca ≈ **77 blocos** (√(41²+65²)); o mineiro em z≈1245 ao alvo em z≈1150 ≈ **95-98**.

**Veredito:** a mina NÃO foi aberta no lugar errado nem a vila "derivou" — a boca está na borda
(Regra 53, correto). O problema é que a **vila é enorme e o centro/obras ficam no canto oposto à
boca**; o mineiro deposita perto do centro e atravessa ~77-95 blocos + desce ~390 de cut. A
navegação não traça caminho tão longo pela rocha → "walking to nowhere", MINER 0% / 66% blocked,
55× NO_STANDING_POSITION. Em cascata: fundidor sem cru, obra sem cobblestone (embora o save mostre
COBBLESTONE=355, então a obra travada é outra coisa — ver abaixo).

**Obra travada (projects[0]):** `plains_small_house_8` com deferredPieces de `minecraft:dirt`
(a base, supportFingerprint down=stone up=dirt). O construtor "walked 200 ticks without reaching"
a peça dirt em 254,90,1235. Base de terra que ele não alcança — somado à noite (off hours não
deixa a GoToWorkTargetTask começar). Grande parte da trava do BUILDER (93%) foi a noite.

Correção (decisão do autor aberta, ele pediu investigar primeiro): abrir boca nova perto do centro
quando a existente fica longe demais, OU manter a mina acessível de perto por rota pronta. Relacionado:
[[pendencias-0354-mina-folhas-esperando]], [[mineiro-preso-na-descida]], [[obra-orfa-por-deriva-do-centro]].

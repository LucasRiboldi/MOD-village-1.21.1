---
name: mine-return-rastro
description: Encalhado volta pelo rastro da mina (MineReturn, 910dbba4) e a casca de 4 blocos da vila não se fura; causa raiz do encalhe repetido (pedra inalcançável) segue aberta
metadata:
  type: project
---

Playtest 02-10 01:36 (Spark f0wlFJg1kL): 3 mineiros encalharam 7x em
-241,10,379 (desciam a passagem até a caverna com lava; alvo -241,-12,378
inalcançável) e a fuga subia reto saindo em -240,65,384, no meio da vila.

Pedido do autor: voltar por onde desceu, tirar/pôr bloco só para reencontrar o
caminho, nunca quebrar o chão da vila. Feito: `MineReturn` (rastro por
passagem, recomeça na superfície; refaz em trechos de 10 via DetourPlanner com
`steer`), casca = 4 blocos de cima do terreno dentro da caixa medida (vale
também na subida de reserva), reserva mira a borda da caixa. JAR ae251a7c.

**Why:** subir na direção do centro da vila era o que abria o buraco.
**How to apply:** próximo log: `goes back the way it came` → `is back on its
trail` → `is out at` na boca. O rastro não está no save (F-3). A causa do
encalhe (voltar ao alvo inalcançável) é o F-1 do estudo
`2026-10-02-melhorias-profissoes-vida-natural.md`. Teste de "fora de qualquer
vila" não funciona na bateria: caixas de outras colônias de teste cobrem
tudo. Ver [[climb-out-nunca-desiste]], [[playtest-2026-10-02-piso-e-traco]].

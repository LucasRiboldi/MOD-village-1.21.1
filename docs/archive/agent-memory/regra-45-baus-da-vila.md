---
name: regra-45-baus-da-vila
description: "Regra 45 (f69e27c): baús da colônia = os da caixa da vila; fora = fora de alcance; baú de trabalhador de outra colônia nunca é livre — era a causa do flake craft_family"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-01T17:14:29.646Z
---

Pedido do autor 2026-10-01: varrer todos os baús dentro da vila e evitar baús
fora de alcance. Tudo passa por `ColonyChests.nearestFirst` (obra, ofícios,
fabricação, alívio baú↔baú, levantamento do ciclo).

Commit f69e27c: vila medida → varredura da caixa inteira (janela de altura das
camas), dentro ou fora de casa; fora da caixa sai da conta (log 1x por baú);
vila não medida segue raio 64 + só dentro de casa. Baú em STORAGES de qualquer
colônia nunca é "livre".

**Why:** o flake carpinteiro/pedreiro (craft_family, ~3 em 16) era a obra aberta
de cada teste cobrir o próprio baú ("dentro de casa") e a colônia vizinha a 13
blocos lê-lo como estoque público. 0 falhas em 6 rodadas depois — indício, não
prova. A regra "só dentro de casa" de 09-16 foi relaxada para vila medida; se o
autor quiser de volta, é o `scan(world, box)` em VillageChests.

**How to apply:** em teste, registrar Building da PRÓPRIA colônia fora da caixa
faz a caixa crescer (Emenda 6) — use outra colônia para testar "fora da vila".
No log de jogo: `is outside the village of colony … out of reach`. JAR md5
4a2b31f3 instalado em mods (anterior 95f120d7 no scratchpad). Ver [[quadrado-da-vila-proposta]].

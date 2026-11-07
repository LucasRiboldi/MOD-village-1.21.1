---
name: playtest-2026-10-07-madrugada
description: Primeiro playtest do JAR 0.3.1 (278086C5); floresta da fundação nasce com 0 árvores (capim baixo barra o plantio), mina desce 5 níveis num segundo, lenhador vive de 7 replantios, comida 1/adulto
metadata:
  type: project
---

Sessão de 06-10 23:41 a 07-10 00:43, JAR certo (278086C5), Spark QK8BBjPGXr: TPS 20 sempre,
mod 2,7% do servidor; picos de 100–190 ms por tique vêm da fase `population`
(VillageForest.plantRequestedSapling → hasClearCanopy refeito a cada ciclo, sem espera).

Achados (log ~/.minecraft/logs/latest.log da sessão):
1. **Floresta da fundação: "Colony created ... 0 forest trees" nas duas vilas.** Causa PROVADA por
   GameTest temporário: `VillageForest.hasClearableNaturalCanopy` exige ar/folha no bloco 11×11×16;
   capim baixo (planície) não é ar → planted=0. O GameTest existente limpa o anel até virar ar,
   por isso passa.
2. Lenhador: só 15 árvores em 1 h, todas nos 7 pontos de replantio (4–6 toras); alvos a 84–167
   blocos (item 18 ampliou o alcance). Obra esperou `oak_log` em 255 relatórios.
3. Mina: às 00:23:50 "went one level deeper" ×5 no mesmo segundo — ramais fecham por
   "nowhere to stand"/"holds back water" sem cavar, e o fecho conta como nível feito; alvo foi a
   y=0, mineiro desistiu, boca nova a ~115 blocos. Mineiros esperando ramal (só 1 aberto na escada
   compartilhada): MINER 32% ocioso.
4. Felicidade: 1 ponto de comida por adulto (sem pão nos baús) → sempre infeliz/satisfeita; os
   limiares do item 20 (C7) não são alcançáveis com a produção atual.
5. Fundidor converte pedregulho→pedra→pedra lisa sem pedido (ChestRelief "over half full", ~900 em
   1 h); construtor deixa oak_log de lado 15× (não alcança); 7533 s de aldeões sem profissão.

**How to apply:** corrigir 1 (contar vegetação substituível como livre e limpar antes de gerar;
espera de 6000 tiques após busca falha) e 3 (nível só conta feito se a escada do nível foi
cavada) antes do próximo playtest; os demais esperam o autor. Ver [[decisoes-adr-036-andamento]].

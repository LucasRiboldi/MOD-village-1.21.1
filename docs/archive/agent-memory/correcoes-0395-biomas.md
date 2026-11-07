---
name: correcoes-0395-biomas
description: "Lote de correções de 27-10 (Regras 114–118) feito a partir dos logs da 0.3.93; está na branch fix/obra-pecas-de-lado (PR #36), ainda não jogado"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-27T16:24:15.936Z
---

Pedido do autor em 27-10, depois de [[analise-biomas-logs-0393]]. Tudo foi commitado e enviado para a branch `fix/obra-pecas-de-lado`, empilhado sobre a Regra 113, na PR #36.

**Entregue:**
- **Regra 114** (c9d9b884): o teleporte só pousa onde o corpo cabe (`SafeLanding`). O preso sem cama vai para o centro da vila (`BedTeleport.toSafety`). Quem passa 3 rodadas cercado é posto logo atrás da cerca (`PenLift`).
- **Regra 115** (62d79287): o armazém pisa na camada 0, ou seja, no nível do chão. Cada estilo tem o seu (`colony/<estilo>/storage_majest.nbt`, gerado por `scripts/storage_variants.py`). Saíram folhas, alçapões e teias.
- **Regra 116** (5e6ba61b): a argila surge na 3ª busca vazia, e não na 4ª.
- **Regra 117** (5e6ba61b): o pastor recebe ração (`VC_SUPPLY ANIMAL_FEED`) só quando os baús não têm nenhuma comida da espécie.
- **Regra 118** (63f058e6): no deserto o bosque tem o dobro de mudas e elas ficam espalhadas em pontos sorteados das bordas.

**Animais da fundação** (2 de cada espécie): já existiam desde a Regra 102, e os logs mostram 8 animais em todas as vilas. Não foi preciso mudar nada.

**Versão:** a 0.3.95 já está fechada (CHANGELOG, STATE, `gradle.properties`, README), com o JAR instalado (SHA-256 67BE8C39…) e a branch enviada.

**Falta:**
- jogar;
- os demais achados: o lenhador que não corta acácia na savana, o fazendeiro que não alcança a roça ou esgota o orçamento de busca, as peças snow/snow_block/diorite da neve, o smooth_sandstone do deserto, o mineiro com NO_STANDING, a Regra 74 sem troca de cama e os códigos UNCLASSIFIED.

**Why:** o autor pediu correções pontuais por bioma.
**How to apply:** no jogo, procure no log "lifted from", "to the village center", "animal feed" e "VC_SUPPLY exception=ANIMAL_FEED", e confira se ainda há sufocamentos.

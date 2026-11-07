---
name: pendencias-pos-auditoria-2026-10-28
description: Decisões do autor para os pontos abertos após o PROJECT_AUDIT.md (T1/T2 feitos) — ordem e escolhas de cada item
metadata:
  node_type: memory
  type: project
  modified: 2026-10-28T17:47:56.830Z
---

Escolhas do autor em 2026-10-28, depois do PROJECT_AUDIT.md (T1 16dfe634 e T2 95aaaa72 já feitos):

1. lightTheStreet: guarda de chunk no lugar da lanterna (GameTest red→green, cenário longe das arenas como o T2).
2. T3: JUnit no ColonySavedDataTest (roundTrip com a caixa crescida; centro fixo da Regra 141).
3. Mina: **salão 7×7** + SHAPE_VERSION 9 (corrige também o defeito: a 0.3.96/ec35f3ad mudou o salão para 17 sem subir a versão 8). Antes: graphify (regra 4 do CLAUDE.md) e GameTest que mede blocos por nível.
4. T4 (casa a ~60 blocos sem teleporte, longe das arenas) e T5 (fundidor leva minério da boca ao baú público) como GameTests.
5. Simulação: as 5 vilas naturais, 5 dias cada (`biome_sim.py plains desert savanna taiga snowy --days 5`).
6. Fechamento: atualizar PROJECT_AUDIT.md e commitar; versão 0.3.103 + JAR instalado; commitar a atualização da skill minecraft-testing (SKILL.md + skills-lock.json). **Sem PR** por ora.

**Why:** o autor pediu ponto a ponto com pesquisa e pergunta antes de cada um.
**How to apply:** seguir a ordem 1→6, commit por item verificado; ver [[simulacao-taiga-10-dias]].

**Feito (2026-10-28):** 1 ce977bee (lanterna fora da borda do chunk), 2 3003e1a1 (T3 achou defeito real: carregar o mundo movia o centro — VillageBoundsSave usava measure), 3 d20c0d10 (salão 9 — o 7 quebra o traçado; SHAPE 9 corrige a forma não subida na 0.3.96), 4 611b0529 (T4/T5 com mutação), 6 5cd341b9 (skill) + c5b44697 (0.3.103, JAR instalado DA400FF7…2782) + db30015e (PROJECT_AUDIT com andamento).
**5 — simulação 5 vilas × 5 dias (semente padrão 20261020):** notas planície 8,0 · deserto 7,6 · savana 7,7 · taiga 7,7 · neve 8,9; todas criaram colônia. Diário funcionou (5 VC_DAY, 5 VC_AUDIT, 18–41 VC_EVENT por vila); pedreiro tapou falhas de rua (28–103) e nivelou; lanternas em 4 vilas; mina da planície desceu 4 níveis em 5 dias (y −29). Fundidor não levou minério (HAULED=0) porque nunca ficou ocioso. **Achado aberto:** todo VC_AUDIT com problema é "peças restantes já no lugar" (deserto 128 de 416) — terreno coincidente ou defeito de controle, não investigado. Regra 51 segue acima de 15% em várias profissões.

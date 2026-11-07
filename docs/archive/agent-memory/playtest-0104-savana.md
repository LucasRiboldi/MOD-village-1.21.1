---
name: playtest-0104-savana
description: "Primeiro jogo da 0.3.104 (savana \"Novo mundo\", 85 min, Spark 9QvtolhHPo) — o que melhorou, e os 6 pontos do autor (terreno, fazenda, rua, água, porta→rua, rua preferida)"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-28T23:53:15.007Z
---

Jogo de 2026-10-28, colônia 2f4b65fc (savana), centro ~(-3831, 951), caixa -3927..-3735 x 866..1036.

- **Spark:** 20 TPS, tique mediano 20–23 ms, picos até 320 ms; o mod é 0,9% do server thread. 9.122 chunks por render/simulation distance 32 do cliente (não é o mod).
- **Melhorou:** pastor 0 "fenced in" (sims 45/19/14); mineiro 3.234 blocos, 4 níveis; obras 9 em 1,4 h; diário e auditoria funcionando.
- **T4 em jogo:** 15 TARGET_REJECTED TOO_HIGH e 0 SKIPPED — todas do caso "anda até a pedra ~9 acima" (doc 0103 §5), não das cadeias.
- **Autor pediu:** (1) smooth do terreno não visto — D1 B nunca foi implementado; vila sobe a encosta oeste (altura 54–120). (2) fazendas irregulares com blocos quebrados — FarmerTill ara em manchas ao lado da roça. (3) ruas estranhas → voltar a só bloco de caminho (StreetSurface.edge sorteia cascalho/terra grossa/arenito, Regra 138). (4) aldeões na água — 4 de 7 teleportes noturnos saíram do meio do lago (y62, água); Vanilla VillagerEntity setCanSwim(true). (5) porta → rua: só 8 de 22 portas a ≤2 da rede principal; 13 redes soltas. (6) aldeão deve preferir andar pela rua.
- Gancho existente para (4) e (6): LandPathNodeMakerMixin + PenFloors.forWalker.
- Ferramentas de medida em scratchpad novo_tools (portas.py, altura.py, fazenda.py); o stuck_map.py falhou porque o autor rodou da pasta do repo (latest.log relativo) — dar padrão igual ao action_report.

Relacionado: [[decisoes-0103-correcoes]], [[caminho-rente-ao-chao]], [[rede-de-caminhos-decisoes]], [[opcao-ambigua-confirmar]].

**Escolhas do autor (2026-10-28, depois do jogo):**
- Rua: só bloco de caminho; o desgaste já assentado pela colônia vira caminho (pedido direto, sem opção).
- Água: **nadar só se não houver volta** (água muito cara no cálculo de rota, não proibida).
- Preferir rua: **as duas juntas** — gramado custa mais dentro da caixa E trajeto longo (~24+) vai pela rede de ruas.
- Porta→rua: **1 de largura, todas as casas** (originais e da colônia), rente ao chão, contorna, desiste se aterro alto.
- Terreno: **só suavizar na geração** (datapack embutido, terrain_adaptation das vilas; só chunks novos). Lote do mod fica como está.
- Fazenda: **canteiro retangular 9×9** com água na linha do meio, alinhado e em chão plano; manchas antigas ficam.
**Feito:** rua só caminho + unworn f9519c8d; stuck_map sem argumentos (mesmo commit); terreno beard_box af3aa6d3 (não visto em mundo novo); Regra 151 f194c1c4 (água 48, gramado na caixa custa 1 via DAMAGE_CAUTIOUS); porta→rua 765fe273 (layStep só eixo, casas originais via GeneratedVillages.housesAt; sem GameTest das originais).
**Feito também:** fazenda 9×9 714c98b3 (FarmPlots); release 0.3.105 instalada (SHA 227E3933…C95B), não jogada.
**Feito:** trajeto longo 0a7f568a (StreetRoute + MoveToTargetTaskMixin @WrapOperation; caminho completo pelo cérebro sem teste — arena pequena).
**Feito:** mineiro 414e3be6 — era o 1º degrau do caracol do ramal novo, recusado pelo judge a 4 de altura só pela geometria; marca encerrava o ramal (11 no jogo). judge agora roda a busca a pé no TOO_HIGH de perto. Release 0.3.106 (SHA 2FAD5283…A41E) instalada, não jogada.
**Falta:** jogar 0.3.106 em mundo novo; pendências antigas: T1 (neve/espécie), T5, L3, O1–O8.
**Lições:** arena de GameTest pode vir girada — compare nós em coordenada absoluta, não getRelativePos; chão além de x≈14 da arena não existe para a busca; o PathNodeNavigator usa heurística 1,5× (aceita caminho até 1,5× pior), então custo de evitar tem de ser alto.

---
name: decisoes-0103-correcoes
description: "Decisões do autor 10-28 para os problemas abertos da 0.3.103 (T1–T5, L3, O1–O8, D1, D4) e a base de comparação; doc docs/research/2026-10-28-diagnostico-e-decisoes-0103.md"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-28T20:29:41.057Z
---

Fonte: `docs/research/2026-10-28-diagnostico-e-decisoes-0103.md` (commit 5a1d6b64). É a **base de comparação** —
o autor quer comparar depois de jogar: mesma simulação (`biome_sim.py plains desert savanna taiga snowy --days 5
--snapshot`) e a tabela §4 do doc.

Escolhas:
- T1 A: gelo compactado surge com registro; neve com prioridade na pá; lenhador corta a espécie pedida; madeira equivalente depois de N tentativas.
- T2 **A** (corrigido pelo autor: NÃO trocar portão por porta): pastor tosquia/abate por cima da cerca a 4,5 blocos do olho; só entra para levar animal e fecha o portão.
- T3 B: mapa de encalhe/teleporte (script).
- T4 B: mineiro filtra o alvo alto/sem caminho antes de escolher.
- T5 A: argila mais longe + surge após 3 buscas; vidraça pela cadeia areia → vidro → vidraça.
- L3 B, **só no deserto e na taiga**: espinha de peixe.
- O1 A pedreiro faz pedra da próxima obra e ajuda a assentar; O2 A construtor busca o que falta ou prepara o próximo lote; O3 A pastor cuida do rebanho e ajuda o fazendeiro; O4 B carpinteiro faz madeira antes; O5 A árvore inalcançável fora 1 dia e bosque mais perto; O6 A fundidor coleta mais longe; O7 A fazendeiro roça nova, pão, composto, aplaina; O8 A sem profissão vira carregador.
- D1 B: smooth na geração (terrain_adaptation); D4 A: raio forçado da bancada cresce com a caixa.
- Não escolhidos: E1–E4, L1, L2, D2, D3.

**Why:** o autor vai jogar e comparar antes/depois item a item.
**How to apply:** um item por vez, com teste antes da correção (CLAUDE.md §2), commit por item; anotar aqui o commit de cada item feito.

**Feito:** T1-gelo 4b9f7d7a; T2 39207840 (Regra 150, EYE_REACH 4,5); D4 74794a02 (VC_STATE ganhou "box"); isolamento do BenchPassives 74b4f6c3; T3 e525d7a6 (scripts/stuck_map.py); T4 2bc9f5e4 (MinerPicks: até 4 pedras por passagem, recusada vira SKIPPED; sem GameTest da ligação).
**Feito também:** D1 af3aa6d3 (beard_box nas 5 vilas); achado do T4 resolvido 414e3be6 (1º degrau do ramal novo).
**Falta:** T1 (neve na pá, espécie pedida, madeira equivalente), T5, L3 (deserto/taiga), O1–O8 (O7 só tem o canteiro 9×9 de 714c98b3). Depois: simular as 5 vilas e comparar com a §1 do doc.


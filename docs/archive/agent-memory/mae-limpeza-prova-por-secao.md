---
name: mae-limpeza-prova-por-secao
description: "A reserva MAE exige seções idênticas à geração; a limpeza da 0.4.30 refaz a prova, e mudança natural ainda pode recusar"
metadata:
  node_type: memory
  type: project
  modified: 2026-11-05T19:48:58.166Z
---

A reserva MAE passa por duas portas, nesta ordem: `MaeProtection.inspect` (`protected_volume`) e
`MaeGenerationEvidence.inspect` com `requireGeneration` (`changed_generation_evidence`). A segunda compara a seção inteira de
16³ com a impressão da geração: qualquer bloco mudado ali, inclusive por aldeão ou pelo próprio jogo, recusa.

Por isso a limpeza da 0.4.30 (`MaeClearing`, Regra 200) corta antes da reserva, espera 40 tiques e refaz a prova com
`MaeGenerationEvidence.rebase`. Isso é emenda ao item 9 da ADR-044, pedida pelo autor em 2026-11-05.

**Why:** o MAE nunca construiu em jogo. Resolver `protected_volume` (Regras 200 e 201) pode só revelar a porta seguinte.

**How to apply:** se a próxima obra falhar com `changed_generation_evidence` ou `changed_prepared_terrain`, olhar primeiro
mudança natural na seção (folha secando, areia caindo, grama espalhando). Isso não foi visto em jogo, é risco lido no código.
Relacionado: [[mae-regiao-entregue-0426]].

Medido no save em 2026-11-05 (recalculando o SHA-256 das seções contra o recibo): o recibo sai antes de o mundo assentar.
Seção com água ou lava corrente: 100% diverge. Seção com folha: 92%. Isso acontece a 300 blocos da vila e sem ninguém por
perto. Chunk com Y 3 a 5 intacto: 11% até 64 blocos do centro, 40% de 64 a 128, 63% de 128 a 192. A região cobre de 9 a 16
chunks, então quase nenhum lote passa. E a limpeza da Regra 200 confere a prova antes de cortar, logo árvore na seção recusa
o lote antes da limpeza.

Agua do curral grande (provado em 2026-11-05 com fatias a cada 3 tiques): `MaeStructurePieces` poe a agua antes das 4 vizinhas e conta com fechar antes do tique do fluido (5). Se a obra atrasa, a agua escorre e a conferencia recusa com `protected_volume`. O teste do curral falha assim quando a maquina esta ocupada.

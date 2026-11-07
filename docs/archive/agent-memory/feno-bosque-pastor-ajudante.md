---
name: feno-bosque-pastor-ajudante
description: 0.3.16 (08-10) — hay_block na lista STOCKED_AFTER_MISSES, VillageGrove (3 mudas perto da vila), BuildHelper (pastor ajuda a obra); lição da mutação invertida
metadata:
  type: project
---

0.3.16, commits 0f7fce63..103a556c (enviados; branch claude/obra-lote-e-trava, sem PR). Não visto em jogo.

- `BuilderMaterials.STOCKED_AFTER_MISSES`: terracota + hay_block → baú do construtor na 4ª falta.
- `VillageGrove`: 3 mudas fora da caixa (4–10 da borda), longe de casa/obra 4, rua 6, entre si 5; chamado no plantio do viveiro, 1×/min.
- `BuildHelper`: pastor sem tarefa ganha 2ª tarefa BUILD reservada (BuilderWork abre Job para toda BUILD com executor); lã na fila cancela.
- Lições: (1) cenário de teste deve ficar no caminho que a busca percorre primeiro — o do bosque tinha a rua do lado que ela nunca visitava; (2) mutação `Boolean.getBoolean("vc.never") || X` = X — para desligar uma condição use `!Boolean.getBoolean(...) || X`. Ver [[teste-vazio-por-condicao-impossivel]].

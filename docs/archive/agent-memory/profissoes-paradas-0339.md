---
name: profissoes-paradas-0339
description: "Profissões pararam" na 0.3.39 = nenhuma obra por falta de lote (cascata); 0.3.40 anexa após 1 volta, corrige mina e lenhador; recusa de lote por faixa de rua ainda aberta
metadata:
  type: project
---

Na 0.3.39 nenhuma obra abriu em 23 min, e o resto parou em cascata: carpinteiro e pedreiro ficaram com o estoque no teto e o construtor ficou 100% ocioso.

O lote não servia: 115 mil recusas, 37 mil por faixa reservada da rua e 30 mil por nível da rua. Uma volta completa da varredura levou 20 min.

Na 0.3.40 (59be374c):
- `VillageExpansion.FULL_AFTER` passou a 1.
- `quarryFor` passou a ler a marca `MineMarks` (a mesma pedra tinha sido escolhida 53 vezes).
- `TreeChoice` passou a contar os baús públicos.

**Why:** o sintoma "profissões pararam" é a falta de obra, não defeito de cada ofício. Cheque primeiro "opened a build task" e "lot refusals".

**How to apply:**
- Se continuar sem obra, medir a regra da faixa reservada da rua (`ConstructionZone`).
- A 0.3.40 não foi jogada.
- Ver [[regras-0339-bau-publico-borda]] e [[playtest-0336-e-expansao]].

**0.3.41 (b5ee2e19):** na 0.3.40 o anexo funcionou, mas a faixa ficou sem rua: o lote só nasce encostado em rua, e a extensão de sempre calçou para o lado oposto. Agora `StripRoad.lead` puxa um trecho para dentro da faixa, via `RoadExtension.startGrowing`, seguido pelo `keepGrowing`. Não jogado.

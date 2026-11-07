---
name: caminho-rente-ao-chao
description: "0.3.98 o caminho fazia aterros de 4 de altura e travou o servidor 12 s; o autor escolheu caminho rente ao chão, contorna ou desiste, não desfazer os aterros antigos"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-28T03:15:26.512Z
---

**Playtest 0.3.98 (28-10, Spark S3FcIdzPmh).** Dois defeitos graves na rede de caminhos (Regras 123–125).

**1. Blocos altos.**
- `VillagePathPlanner.MAX_FILL` era 4, e o aterro custava `FILL`/3 por coluna.
- O `PathPaving.layStep` aterrava as 3 colunas do chão até o piso.
- Resultado: paredes de terra de até 4 na encosta e aterro sobre a depressão.
- O `CLEAR` também quebrava terreno acima do piso.

**2. Travão de 12 s.**
- O `PathLinks.tend` tentava todas as portas no mesmo tique.
- Cada falha esgotava 40.000 nós, sem cache do `groundY`.
- Picos medidos: 1,1 s, 0,7 s e "Can't keep up 12255 ms".

**Escolhas do autor (28-10):**
- **Rente ao chão:**
  - troca só o bloco de cima;
  - aterra só o buraco de 1 (vizinhos mais altos) e corta no máximo 1;
  - nunca nasce bloco acima do terreno;
  - cada faixa lateral segue o próprio chão ±1 do meio; se não der, o caminho estreita ali.
- **Desnível > 1:** contorna com degraus ≤ 1, ou a porta fica sem ligação e volta a ser tentada depois.
- **Aterros já feitos:** deixar como estão (o autor remove à mão).

**Feito em d2ef5921 (0.3.99, PR #37):**
- `fitsTheHoleRule` e `laneFits`;
- `MAX_FILL`/`MAX_CUT` = 1 e `MAX_NODES` = 20.000;
- cache no `WorldPathTerrain`;
- um traçado por passagem e `RETRY_AFTER` de 72.000;
- JAR DA910747… instalado; 892/892. **Não jogado.**

**Corredor do lote — opção 3 do autor (28-10), feito em d28a35bd (0.3.101):**
- primeiro a serpentina (`Corridor.planWinding`, 6 curvas, sem corte);
- só sem ela, corte de no máximo 3 (`MAX_CUT` era 12), com talude nos lados (`CorridorCut`, degrau por bloco e grama de volta);
- 899/899; não jogado; a serpentina não tem teste dedicado.

**Why:** o caminho não pode criar construção nem pesar no servidor.
**How to apply:** ligado a [[rede-de-caminhos-decisoes]] e [[playtest-0397-e-decisoes-2026-10-27]].

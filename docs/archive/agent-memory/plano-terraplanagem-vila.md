---
name: plano-terraplanagem-vila
description: "PLANO FUTURO (pedido do autor 15-10) — terraplanagem da vila inteira: um aldeão nivela a montanha para subida uniforme de ≤1 bloco de altura entre colunas vizinhas, permitindo a caminhada. NÃO afrouxar o critério de nascimento de obra; em vez disso, PREPARAR o terreno."
metadata:
  node_type: memory
  type: project
  modified: 2026-10-15T17:01:59.738Z
---

**Pedido do autor (15-10), guardado para o futuro.** Decisão sobre a ociosidade da vila montanhosa
(playtest 0.3.64, [[playtest-0364-ociosidade-altura]]): **não mudar como a obra nasce** (não afrouxar
MAX_SLOPE nem a Regra 19). Em vez disso, a vila **terraplana a montanha**.

**O plano:**
- A vila em terreno montanhoso é **devidamente reconhecida** como tal (detectar relevo acidentado no
  raio da colônia).
- Um aldeão **nivela a montanha** transformando-a numa **subida uniforme de terreno**: equalizar o
  terreno da vila inteira para **no máximo 1 bloco de altura de diferença entre colunas vizinhas**, o
  que permite a caminhada dos aldeões (Vanilla sobe 1 bloco). Terraplanagem ativa — corta o que sobra,
  aterra o que falta — até o terreno ficar caminhável e lotes poderem nascer naturalmente.

**O que isso resolve:** a raiz da ociosidade geral na montanha. O spike do LotRefusals mostrou que 61%
das recusas de lote são relevo (vertical window MAX_SLOPE=2 = 31%, street level Regra 19 = 30%; reserved
road 35% é outro sistema). Nivelando o terreno, os lotes passam no critério atual sem afrouxá-lo, e as
profissões de navegação (miner/shepherd/farmer) param de travar no desnível.

**Infra que já existe para reusar:** `BuilderPlacement`/`BlockShaping.groundFor` já molda o chão sem
gastar recurso (a horta da 0.3.57 usa — [[horta-da-casa-surge]]); `FoundationPreparation`, `LotLevel`,
`StreetLayer` tratam nível de rua/fundação. A terraplanagem seria um sistema novo que generaliza isso
da pegada de uma casa para o **raio da vila**.

**Escopo:** é feature grande (nova profissão/atividade ou extensão do construtor; detecção de relevo;
loop de nivelamento por colunas; custo de performance de varrer e alterar muitos blocos). Merece spec
própria (brainstorming → spec → plano) quando for a vez. NÃO começar sem desenho aprovado.

Relacionado: [[playtest-0364-ociosidade-altura]], [[horta-da-casa-surge]],
[[obra-sem-construtor-trava-de-fundacao]], [[indice-de-ruas-vira-rua-candidata]].

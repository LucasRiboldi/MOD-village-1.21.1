# ADR-048 — Todo caminho e toda rede pelo grafo

**Estado:** aceita pelo autor em 2026-11-06, na varredura `docs/audit/2026-11-06-conflitos-e-redundancia.md`. Regras 214 a 216.

## Decisão do autor

- Quatro partes criavam caminho por conta própria: todas passam a pedir ao grafo, e só os aldeões constroem (1A).
- Tudo que precisa saber das ruas usa o grafo (2A).
- A vila antiga deixa de ter sistema próprio: "ignora a antiga, esquece o que é velho e passa sempre a usar o sistema mais atual" (3).
- Um leitor de porta, uma lista de quem está livre, uma régua de lote (5A, 6A, 8A).
- O pedreiro ocioso nivela e calça (7A).

## Como

- `PathProposals` guarda o pedido; `RoadGraphRefresh` o leva a `PlannedPaths`, e `PathPaving` calça. Coluna pedida ou planejada já conta como rua para o lote.
- `RoadNetwork.of` responde pelas linhas do grafo; `roadsOf` fica para quem precisa da coluna física.
- `NewVillage`, `VillageOrigin` e `PathLinks.tend` saem; `PathLinks` é só o trecho em calçada. Isso revoga o §0.2 itens 2 e 3 do plano mestre do MAE para caminho: a releitura vale também em vila de save antigo, e ela calça em volta das casas originais.

## Consequências

- Caminho só avança no expediente, no ritmo de quem calça: medido na 0.4.38, cerca de 40 passos por dia de jogo com um calçador.
- A rua pedida para achar lote existe antes de ser calçada: o lote pode nascer ao lado de chão ainda sem caminho.
- O grafo decide: ponta de rua sem porta pode ser cortada (Regra 206) e o pedido envelhece sem ser calçado.
- Vila de save antigo passa a ser relida e calçada. O efeito em save antigo não foi jogado.
- O plano de calçada vive na memória: reiniciar traça de novo.

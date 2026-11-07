---
name: rede-de-caminhos-decisoes
description: "Decisões do autor (27-10) para a rede de caminhos da vila — 3 de largura no lugar da Regra 70, aterra, serpentina, pedreiro/carpinteiro/fundidor um por vez, distância mínima de 3 entre construções"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-27T19:52:56.303Z
---

O estudo está em `docs/research/2026-10-27-rede-de-caminhos.md`, no §7.1. Decisões do autor em 27-10:

- **D1:** o caminho tem 3 de largura e substitui o corredor de 1 bloco da Regra 70. Construção nova nasce a no mínimo 3 blocos de qualquer construção, mas pode encostar no caminho.
- **D2:** o buraco do traçado é aterrado com o chão do bioma.
- **D3:** primeiro a serpentina pela encosta, depois corte de até 2 em degraus de 1, por último o contorno.
- **D4:** o caminho de terra vale em todo bioma.
- **D5:** cada entrada se liga ao ponto mais barato da rede, com atalho ao chafariz quando o desvio passa de 1,6×.
- **D6:** pedreiro, carpinteiro e fundidor ociosos fazem o caminho, **um por vez**. O "ferreiro" do autor é o fundidor.
- **D7:** as ruas Vanilla entram na rede como estão.
- **D8:** o lote novo é escolhido pela rede já na fase 1.
- **D9:** preferência pelo caminho e tráfego ficam para a fase 2.

Regras previstas:
- 123: a rede de caminhos;
- 124: a distância mínima de 3 entre construções;
- 125: quem faz o caminho;
- 126: o lote pela rede.

Critérios que o autor acrescentou em 27-10: o caminho não destrói construção, busca o menor percurso, não se sobrepõe nem corre colado a outro, e todos se unem numa rede só.

**Estado:**
- **Fase 1 feita** (52da47e8): `VillagePathPlanner`, `PathTerrain` e `PathStep` no núcleo, com 9 testes; a mutação do custo de colar foi pega. Custos: STEP 1, TURN 0.5, CLIMB 1, FILL 1.5, CUT 4, REUSE 0.25, BESIDE 3.
- **Falta:**
  - `PathTerrain` lendo o mundo;
  - o grafo salvo (chafariz, entradas, junções);
  - o assentamento (`RoadPaving`, terra do bioma, aterro);
  - o trabalho do pedreiro, carpinteiro e fundidor ociosos, um por vez (Regra 125);
  - a distância de ≥3 entre construções (124);
  - o lote pela rede (126);
  - a substituição do `Corridor` da Regra 70.

**Fase 2 feita** (30376c9d):
- `WorldPathTerrain` e `PathLinks` (porta lida do mundo; ligação à rede ou ao centro);
- `PathPaving` (Regra 125, registrado no `VillageColonyMod`);
- `LotSpacing` (Regra 124, `TOO_CLOSE`);
- o `WorkPath` saiu do ciclo, e a Regra 70 ficou como substituída.

Falta a D5 (atalho ao chafariz quando o desvio passa de 1,6×) e a Regra 126 (lote pela centralidade). Nada foi jogado.

**Why:** o autor quer a vila crescendo por uma rede registrada e segura para todos os aldeões.
**How to apply:** implementar por fases (§8 do estudo), com o grafo e o A* puros no núcleo primeiro, um commit verificado por fase. Ligado a [[decisoes-autor-2026-10-27-pendentes]] e [[movimentacao-natural]].

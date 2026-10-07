# Varredura das bordas para o centro — análise do plano (2026-10-03)

Pedido do autor: *"a varredura de recursos pelos aldeões deve começar pelas
bordas da área da vila, fazendo um caminho em espiral até o centro; se não
encontrar, começa em outro ponto, não varrendo espaços que já tenha varrido,
permitindo que espaços fora da área da vila possam ser buscados também."*

## Como é hoje

- `RingSweep`: espiral **do centro para fora**, anel por anel, até o raio da
  busca; 1.024 colunas por passagem, com cursor por dono e por tipo de busca
  (`GENERAL`, `FARMING`, `SURFACE`).
- O raio é um círculo em volta do centro (areia 48, roça 32, superfície pelo
  setor mais distante da vila), não a caixa da vila.
- Uma volta inteira sem achar vira castigo (`EmptySweeps`) e, na 3ª, o
  material aparece no baú (`LocateFallback`).
- Desde hoje, água e lava lidas uma vez são puladas (`FluidColumns`).

## O plano proposto, em passos

1. **Começar pela borda da caixa** e andar em espiral para dentro, anel a anel,
   até o centro.
2. **Não achou:** escolher outro ponto de partida sem repetir o que já foi
   varrido. Isso exige uma memória das colunas varridas por tipo de recurso.
3. **Fora da vila:** depois do miolo, continuar em anéis para fora da borda.

## Avaliação

| Ponto | A favor | Contra |
|---|---|---|
| Bordas primeiro | O recurso da periferia (areia, argila, terra, árvore) costuma estar fora das casas; o miolo da vila é casa, rua e lote protegido (Regra 3) e quase nunca rende | O aldeão anda mais até o primeiro alvo: hoje o mais perto do centro sai primeiro |
| Espiral para dentro | Mesmo custo da espiral atual (só a casca de cada anel) | Nenhum relevante |
| Não repetir o varrido | Corta a releitura: a volta seguinte só olha o que mudou | Precisa de memória por coluna e tipo (≈ 17 mil colunas numa vila de 143 × 117, cerca de 2 KB por tipo em bitset) e de uma regra de quando esquecer: árvore cresce, areia é cavada e volta a ser ar |
| Fora da vila | Resolve a vila sem recurso dentro (a areia da planície) antes de o material aparecer no baú | Fora da caixa não há proteção da vila, e o aldeão vai longe; precisa de teto (a caixa + 64, por exemplo) |

## Recomendação

Fazer em duas etapas, nesta ordem:

1. **Ordem da busca pela caixa:** `RingSweep` ganha um modo "da borda para
   dentro" que percorre os anéis da caixa (retângulo) do maior para o menor,
   mantendo o cursor atual. Afeta coleta de superfície, roça e areia; a busca
   de lote e de árvore continuam como estão. Custo baixo, risco baixo.
2. **Memória do varrido e expansão para fora:** um bitset por colônia e
   tipo de recurso, esquecido quando a vila cresce (como as marcas de água e
   lava) ou a cada N minutos para recursos que voltam (árvore, cultivo). Ao
   esgotar a caixa, a busca segue em anéis para fora até a caixa + 64. Só
   depois disso conta como "busca vazia" para a regra das 3 tentativas.

**Decisão do autor:** o teto de busca fora da vila (proposta: 64 blocos além
da borda) e se a árvore e a roça entram na ordem nova ou continuam do centro.

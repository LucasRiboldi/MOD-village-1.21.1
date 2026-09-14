# ADR-014 — Coleta de superfície do fundidor

**Status:** Aceita pelo autor em 2026-09-13
**Tipo:** Arquitetura / cadeia produtiva

## Contexto

O fundidor executa transformação pela receita vanilla, mas a areia necessária
para vidro está roteada ao mineiro. Além disso, construções podem exigir
`grass_block`, material que precisa vir do mundo e não pode ser fabricado pelo
mod. A coleta dentro da vila pode danificar estruturas.

## Decisão

- Adicionar `SURFACE_GATHERED` e `COLLECT_SURFACE_RESOURCE`, com capacidade
  `COLLECT_SURFACE_RESOURCE` declarada pelo fundidor junto de `SMELT_ITEMS`.
- Areia para vidro continua sendo meta derivada da demanda de vidro; sua tarefa
  de coleta passa ao fundidor. `grass_block` só vira meta quando uma construção
  aberta realmente o solicita.
- O fundidor recebe pá de ferro com Toque Suave. O loot é consultado com a
  ferramenta real e o bloco só é removido se os drops necessários couberem no
  baú pessoal.
- A grama só pode ser retirada além de 64 blocos horizontais do centro da
  colônia. A direção inicial é o setor cardinal cuja borda tem a maior distância
  mínima das peças de estrutura da vila observáveis em chunks carregados. A
  consulta não carrega chunks; sem estruturas disponíveis, a direção é
  determinística a partir da identidade da colônia.
- Estruturas geradas, infraestrutura da colônia e outros blocos protegidos
  continuam fora dos alvos elegíveis.

## Consequências

A cadeia material continua orientada por dados no Core e a coleta no mundo
permanece na camada Fabric. Jogadores ainda podem alterar o mundo durante a
partida; a seleção é recalculada ao abrir/reatribuir trabalho e cada alvo é
revalidado antes da quebra. A área de 64 blocos é proteção geométrica adicional,
não uma estimativa de limites de vila do Minecraft.

## Validação

Testes de responsabilidade e demanda no Core; GameTests para a zona protegida,
setor escolhido, proteção de estrutura, loot Toque Suave e armazenamento.

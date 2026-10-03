# Modelos de estrutura da colônia

Pedido do autor, 2026-10-02. Aqui ficam os modelos próprios do mod. **Quando
um modelo existe, ele é o prioritário sobre a estrutura do jogo.** Sem modelo,
o mod constrói como sempre: a pasta vazia não muda nada.

Lista de controle do que existe e do que falta: `CATALOGO.md`, gerado por
`python scripts/structure_catalog.py`.

## Formato e caminho

- **Extensão: `.nbt`.** É o arquivo de estrutura do Minecraft, o mesmo que o
  **Bloco de Estrutura** grava no modo *Salvar*. É também o formato das
  estruturas de vila do jogo (`data/minecraft/structure/village/...`).
- **Só arquivos nesta pasta do jar são lidos.** O caminho de dentro do jar é
  `data/villagecolony/structure/colony/`. O jogo carrega estruturas de
  `data/<mod>/structure/` desde a 1.21; o id fica `villagecolony:colony/<...>`.

## Dois tipos de modelo

| Para quê | Caminho | Efeito |
|---|---|---|
| Casa de um ofício, num estilo | `colony/<estilo>/trade_<oficio>.nbt` | entra na lista de obras **à frente** das casas de ofício do jogo (Regra 49) |
| Casa de um ofício, todos os estilos | `colony/trade_<oficio>.nbt` | idem, quando não há a do estilo |
| Trocar uma estrutura do jogo | `colony/override/<caminho do jogo>.nbt` | a colônia levanta o modelo no lugar daquela estrutura; o nome do jogo continua, e o tipo e o rodízio não mudam |

- **Estilos:** `plains`, `desert`, `savanna`, `taiga`, `snowy`.
- **Ofícios:** `miner`, `lumberjack`, `mason`, `smelter`, `carpenter`,
  `farmer`, `shepherd`, `builder`.
- **Exemplo de troca:**
  `colony/override/village/plains/houses/plains_small_house_1.nbt`.

## Como fazer um modelo no jogo

1. **Construa a casa** num mundo criativo.
2. **Grave com o Bloco de Estrutura** (`/give @s structure_block`), no modo
   *Salvar*, com o nome `trade_miner`, por exemplo.
3. **Pegue o arquivo** em `saves/<mundo>/generated/minecraft/structures/trade_miner.nbt`
   e copie para o caminho acima, no projeto
   (`src/main/resources/data/villagecolony/structure/colony/...`).
4. **Atualize a lista:** rode `python scripts/structure_catalog.py`.
5. **Gere o JAR** (commit + push atualiza o JAR, Regra 33).

## O que o modelo precisa ter

- **Uma porta.** A camada logo abaixo dela vira a altura da rua: o piso da
  porta fica no nível da rua.
- **Cama e baú**, se for para morar: a moradia precisa de cama (Regra 21).
- **Blocos que a colônia consiga fazer** (Regra 27): o construtor espera pelo
  que falta, e uma peça que ninguém fabrica trava a obra.
- **Ofício:** o bloco de trabalho dele, como cortador de pedra, fornalha ou
  bancada.

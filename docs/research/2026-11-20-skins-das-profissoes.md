# Skins próprias das profissões do mod — análise de viabilidade (2026-11-20)

Pedido do autor, 2026-11-20: projeto futuro de roupa própria para os aldeões das profissões do mod. Análise simples de
dificuldade, peso e ferramentas, antes de qualquer decisão. Nada implementado.

## 1. Como o Vanilla veste o aldeão (1.21.1)

- A roupa é uma camada desenhada por cima do corpo (`VillagerClothingFeatureRenderer`): o bioma
  (`textures/entity/villager/type/<bioma>.png`), a profissão (`textures/entity/villager/profession/<id>.png`) e o
  nível. O caminho da profissão sai do registro de profissões (`Registries.VILLAGER_PROFESSION`): uma profissão
  registrada pelo mod (`villagecolony:lumberjack`) procura a textura em `assets/villagecolony/textures/...` sozinha.
- A textura é um PNG de 64 × 64 no molde do aldeão. O custo de desenhar é o mesmo de qualquer aldeão Vanilla: não pesa
  no jogo.
- O mod já tem lado do cliente (entrypoint `client`, texturas das bancadas), então o jogador já precisa dele instalado.

## 2. O que existe hoje

A profissão do mod mora só no servidor (`Worker.profession`), e a Regra 44 (ADR-029) põe o ofício Vanilla em "nenhum"
para quem tem profissão do mod: no jogo ele aparece como aldeão sem ofício. O autor viu um trabalhador **trocar de
roupa** no jogo: é sinal de que ele recebeu ofício Vanilla, ou seja, a Regra 44 vazou (registrado no `TODO.md`, a
diagnosticar).

## 3. Caminhos

| # | Caminho | Esforço | Risco |
|---|---|---|---|
| S1 | **Camada própria no cliente.** O servidor manda a profissão do mod ao cliente (dado sincronizado na entidade), e uma camada de desenho do mod veste o aldeão com a textura dela. O ofício Vanilla continua "nenhum". | Médio: sincronizar o dado, uma camada de render, 9 texturas. ~1 a 2 dias. | Baixo: não toca a IA; só cliente e um dado sincronizado. Mixin ou registro de camada pede ADR. |
| S2 | **Profissão Vanilla registrada.** Cada profissão do mod vira uma `VillagerProfession` de verdade, e o Vanilla desenha a roupa sozinho. | Alto: o Vanilla dá a toda profissão posto de trabalho, troca, fofoca e reabastecimento; teria de ser desligado um por um. | Alto: desfaz a ADR-029 e mexe na IA do aldeão. |
| S3 | **Pacote de recursos que troca a roupa Vanilla.** | Baixo | Não serve: troca a roupa de todos os aldeões do mundo, não só os do mod. |

## 4. Como as texturas seriam criadas pelo Claude

- **Ferramenta:** Python com Pillow (instalado: 12.2.0). O Claude não desenha à mão nem gera imagem por IA; ele escreve
  pixel por pixel, com script, sobre o molde de 64 × 64 do aldeão: cor da roupa, avental, chapéu, cinto, ferramenta no
  bolso. É bom para roupa de cores e formas simples (o estilo do próprio Minecraft).
- **Skill:** `minecraft-resource-pack` (texturas, caminhos de assets, mcmeta). O código da camada usa
  `fabric-development` e `minecraft-code-research` (como o `VillagerClothingFeatureRenderer` desenha).
- **Prévia:** o script gera também uma imagem ampliada da textura (e uma montagem das 9 lado a lado) para o autor
  aprovar antes de entrar no JAR. Ajuste fino, se o autor quiser, no Blockbench.
- **Direito de uso:** desenho próprio, não cópia editada da textura da Mojang.

## 5. Veredito

Viável e leve no jogo. O caminho recomendado é o **S1**: não mexe na IA, não reabre a ADR-029, e as 9 roupas são
arquivos pequenos. O maior trabalho é o desenho: o Claude faz uma primeira versão por script, com prévia, e o autor
aprova ou ajusta. Antes dele, vale corrigir o vazamento da Regra 44, senão a roupa do mod e a do Vanilla brigam.

# Medição do terreno da vila e a solução definitiva do chão — pesquisa (2026-11-25)

Pedido do autor (25-11, depois do E3 do jogo da 0.4.139): criar um sistema que faça medições profundas, salve o estado e
o desenho de toda a vila no começo e, ao salvar, analise os blocos que o jogador alterou, para entender como devem ser
os desníveis e os acessos e trazer dados concretos do jogo. Pesquisar soluções e projetos parecidos, alternativas e
melhorias, e oferecer uma ideia definitiva para o chão desnivelado e as construções. Nada aqui foi decidido.

## O que o mod já tem

| Peça | O que guarda | Falta |
|---|---|---|
| `EditLedger` (Regra 345) | Cada bloco que o jogador quebra ou põe na vila (`VC_PLAYER_EDIT`, com posição, bloco de antes e de depois e alvo); quando ele desfaz o mod (`VC_PLAYER_UNDO`) e quando insiste (`VC_PLAYER_INSIST`). No mundo 2 do jogo da 0.4.139: 611 linhas. | A foto do terreno inteiro: o log diz o bloco, não o desnível em volta. |
| `PlayerGround` (Regra 334) | A coluna que o jogador mexeu fica como ele deixou. | Não diz por que ele mexeu. |
| `GradeSurvey` e `VC_GRADE` (Regra 357) | Colunas, âncoras, degraus maiores que 1, pendentes e conflitos, uma vez por dia. | Só números; nenhuma foto, nenhum antes e depois por coluna. |
| `TerrainQueue` e `VC_TERRAIN` | Degraus e quadrados isolados por dia. | Idem. |
| `scripts/research/lot_terrain_dump.py` | Lê do save o topo de cada coluna, sem abrir o jogo (feito no E3). | Roda à mão, sem quem mexeu. |

## Projetos e técnicas parecidas

- **GDMC — Generative Design in Minecraft** ([Salge et al., 2018](https://arxiv.org/abs/1803.09853)): competição de
  geradores de vila que se adaptam ao terreno. O artigo aponta como falha comum aplainar áreas grandes e construir sem
  refletir o mapa em volta. A adaptação ao terreno é um dos quatro critérios do júri. O vencedor de 2022
  ([van der Staaij et al., 2023](https://arxiv.org/abs/2309.10871)) começa por uma fase de análise do terreno e constrói
  em rodadas, por agentes. Lição: medir o terreno antes e julgar o resultado por critérios fixos, em muitos mapas.
- **Adaptação do Vanilla** (`StructureTerrainAdaptation`, [documentação Yarn 1.21.1](https://maven.fabricmc.net/docs/yarn-1.21.1+build.3/net/minecraft/world/gen/StructureTerrainAdaptation.html),
  [referência da Microsoft](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/worldgenreference/examples/jigsawjigsawstructures)):
  `beard_thin` põe chão sob a estrutura e tira um pouco acima; `beard_box`, `bury` e `encapsulate` são variações. Um
  [pedido na página de sugestões da Mojang](https://feedback.minecraft.net/hc/en-us/community/posts/39203733004685-Terrain-Adaptation-Template-Pool-Element-for-Jigsaw-structures)
  lembra que essas opções não dão conta de mudança grande de altura. É o mesmo problema da vila no morro.
- **Registro de blocos com volta atrás**: o [Ledger](https://modrinth.com/mod/LVN9ygNV) (Fabric) e o
  [Blockwatch](https://modrinth.com/mod/blockwatchs) guardam quem mudou cada bloco e desfazem por filtro. O `EditLedger`
  já faz a parte de registrar; falta a comparação com a foto do começo.
- **Comparar o mundo com uma planta**: o verificador de esquemáticos do
  [Litematica](https://www.curseforge.com/minecraft/mc-mods/litematica) (o autor tem instalado) marca o bloco que
  falta, o errado e o que sobra. Serve de modelo para o mapa de diferenças.
- **Terraplanagem na engenharia**: a malha de corte e aterro em volta de uma cota de projeto, com o volume equilibrado
  ([estudo de nivelamento por grade](https://catalog.lib.kyushu-u.ac.jp/opac_download_md/17814/55_1_p131.pdf)), e
  otimização do perfil para gastar menos terra ([programação linear](https://www.academia.edu/98439209/Minimizing_earthwork_volumes_by_optimizing_vertical_alignment_with_linear_programming_algorithm)).
  O `GradePlan` já faz a versão do jogo: corte mínimo com degrau de até 1.
- **Aprender com as edições do autor**: a modelagem do designer ([Liapis et al.](https://mail.antoniosliapis.com/papers/designer_modeling_for_sentient_sketchbook.pdf))
  captura as preferências de quem edita para ajustar a ferramenta. Aqui, cada bloco que o jogador mexe na vila é um
  exemplo do que ele quer.

Não achei documentação de como o Millénaire nivela o terreno; não uso como referência.

## O sistema de medição

1. **Foto do começo.** Ao adotar a vila, uma vez: para cada coluna da caixa mais 16 de margem, a altura do topo, o bloco
   do topo e o tipo (natural, caminho, estrutura, obra, água), mais as caixas das estruturas e a frente de cada porta.
   Vai para `logs/villagecolony-terrain-<colônia>-start.bin`, ao lado do diário de ações. É telemetria: o jogo não lê de
   volta, então não fere a regra de o mundo ser a fonte da verdade.
2. **Foto de cada dia e do salvamento.** A mesma foto no fim do expediente (para a curva) e ao salvar (`/vc salvar` e o
   fechamento do servidor), com mais uma coisa por coluna: quem mexeu por último (jogador, sistema do mod ou mundo),
   tirada do `EditLedger`.
3. **Relatório do terreno** (`scripts/terrain_report.py`, ao lado do `game_report.py`):
   - **acesso**: por estrutura, quantos lados se alcançam a pé com degrau de até 1 a partir do centro, se a porta se
     alcança, e a parte da vila que se anda a pé;
   - **desnível**: degraus maiores que 1 em caminhos, em volta de portas e no resto, antes e depois;
   - **terra mexida**: colunas e volume cortados e aterrados, por sistema do mod;
   - **o que o jogador quis**: cada grupo de colunas que o jogador mexeu, com o maior degrau em volta antes e depois,
     classificado como passagem aberta, degrau feito, aterro do mod desfeito, aplainado ou enfeite; daí os números que
     ele prefere (o degrau que deixa, a largura que aplaina em volta da porta, a altura de aterro que ele desfaz);
   - **mapa**: imagem PNG vista de cima, com altura, degraus maiores que 1, o que o jogador mexeu e o que o mod mexeu.
4. **Banco de terrenos.** As fotos de cada jogo viram dados de teste: o `GradePlan` e a escolha de lote ficam no `core`,
   sem Minecraft, então rodam em teste de unidade sobre o terreno real dos jogos do autor. Uma mudança no plano mostra,
   antes do JAR, quantas casas e quantos lados acessíveis ela daria em cada mapa já jogado.

### Alternativas para medir

- **Só fora do jogo**: copiar o save no começo e no fim e comparar com o script do E3. Não muda o mod, mas depende de
  lembrar de copiar e não sabe quem mexeu em cada coluna.
- **Mod externo de registro** (Ledger): duplicaria o `EditLedger` e criaria dependência.
- **Litematica à mão**: salvar a vila como esquemático no começo e usar o verificador no fim. Bom para o autor ver; não
  gera números.

## A ideia definitiva: um só modelo do chão, que também escolhe o lote

Hoje duas regras decidem o chão sem conversar: a escolha de lote (Regras 19 e 111, com o aterro de 2 e o corte de 5) e
o plano de alturas (Regra 357). No morro, a primeira recusa tudo, e a segunda não tem casa para acertar.

1. **O lote passa pelo plano.** Para cada candidato, o `GradePlan` roda numa janela em volta dele, com a casa como
   âncora. O lote vale se:
   - a porta se alcança da rua com degrau de até 1;
   - pelo menos 2 lados ficam acessíveis logo (o da entrada sempre) — a regra do autor;
   - os outros lados ficam acessíveis depois que o Zelador trabalhar, sem conflito novo no plano — a volta inteira.
   Entre os que valem, vence o que mexe menos terra.
2. **O Zelador faz o resto.** As fases 4 (rampa e escada onde não cabe) e 5 (praia) da ADR-063.
3. **A medição prova e calibra.** Cada jogo traz o relatório do terreno; as edições do jogador dão os números, e o
   autor escolhe se eles viram constante. Nenhum número muda sozinho.

Isso pede uma ADR nova (escolha de lote pelo plano), porque muda as Regras 19 e 111 e a fase 1 da terraplanagem.

# ADR-064 — A medição do terreno da vila e o chão decidido pelo plano

**Estado:** aceita pelo autor em 2026-11-25 (programa 🅲 da pesquisa `docs/research/2026-11-25-medicao-do-terreno.md`,
depois do E3 do jogo da 0.4.139). Implementação em etapas, com um jogo do autor entre elas: etapa 1 (medição, 0.4.141),
etapa 2 (banco de terrenos), etapa 3 (lote pelo plano, em ADR própria), etapa 4 (fases 4 e 5 da ADR-063).

## Contexto

O chão da vila é decidido por duas regras que não conversam. A escolha de lote (Regras 19 e 111) aceita a coluna da
pegada só entre 2 abaixo e 5 acima da rua, e uma coluna fora reprova o lote. O plano de alturas (Regra 357, ADR-063)
nivela a vila em degraus de 1. No mundo 1 do jogo da 0.4.139, na montanha, a primeira recusou 27.411 candidatos, e o
plano não teve casa para acertar.

Os números de hoje são contagens diárias (`VC_GRADE`, `VC_TERRAIN`). O mod registra cada bloco que o jogador mexe
(`VC_PLAYER_EDIT`, Regra 345), mas não guarda como o terreno estava antes nem o desnível em volta. O autor pediu medições
profundas, com o estado e o desenho da vila no começo e a análise do que ele alterou, para trazer dados concretos de
desnível e acesso.

## Decisão

### 1. Fotos do terreno (etapa 1)

O mod grava uma foto do terreno da colônia em `logs/villagecolony-terrain-<colônia>-<rótulo>.json.gz`:

- `start`: uma vez, quando a colônia é medida (ou na primeira oportunidade, se o arquivo não existe);
- `day<N>`: no relatório do fim do expediente;
- `last`: em `/vc salvar` e no fechamento do servidor.

Para cada coluna da caixa da vila mais 16 de margem: a altura do topo, o bloco do topo, o tipo (natural, caminho, água,
obra da colônia, outro), quem mexeu nela desde que o servidor abriu (jogador, mod, mundo) e a altura-alvo do plano
(Regra 357). Mais as caixas das estruturas, a frente de cada porta e as bocas da mina.

É telemetria, como o diário de ações: o jogo nunca lê a foto de volta, e o estado continua no mundo.

### 2. Relatório do terreno

`scripts/terrain_report.py` compara a primeira foto com a última: a parte da vila que se anda a pé do centro com degrau
de até 1, os degraus maiores que 1 (nos caminhos, em volta das portas e no resto), quantos lados de cada estrutura se
alcançam, as portas que não se alcançam, a terra que cada um mexeu e o que o jogador quis em cada trecho que mexeu
(abriu passagem, fez desnível, desfez o mod, aplainou). Desenha o mapa visto de cima (PNG). O `game_report.py` mostra o
resumo.

### 3. Etapas seguintes

- **Banco de terrenos:** as fotos viram dados de teste do `GradePlan` e da escolha de lote, no `core`.
- **Lote pelo plano:** o lote vale se a porta se alcança da rua com degrau de até 1, se pelo menos 2 lados ficam
  acessíveis logo (o da entrada sempre) e se os outros ficam acessíveis depois do Zelador, sem conflito novo no plano;
  vence o que mexe menos terra. Muda as Regras 19 e 111 e pede ADR própria.
- **Rampa, escada e praia:** fases 4 e 5 da ADR-063.

Os números que saírem das edições do jogador são sugestões; só o autor decide se viram constante.

## Consequências

- Arquivos novos na pasta `logs` do jogo, de dezenas a centenas de kB por foto.
- Uma leitura do terreno por dia e por salvamento no servidor, do tamanho da caixa da vila; a linha
  `VC_TERRAIN_SNAPSHOT` diz quanto custou.
- Quem mexeu em cada coluna vale desde a abertura do servidor; o que veio antes de reabrir o mundo aparece como mudança
  sem dono, e o `VC_PLAYER_EDIT` do log completa a parte do jogador.

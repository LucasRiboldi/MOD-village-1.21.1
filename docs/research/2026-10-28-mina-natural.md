# Mina natural: estudo e decisões (2026-10-28)

Pedido do autor (28-10):
- trocar a entrada por um portal ornamental de mina real;
- pesquisar como a mina desce e minera, focando no material das obras e nos minérios de valor;
- percurso harmônico, que contorna e abre percurso novo diante de dificuldade;
- caminho salvo para a volta segura;
- descer até o fundo do mundo, com naturalidade e sem travar.

## O que o jogo da 0.3.102 mostrou (planície, 4h16, `VC_STATE`)

- A mina desceu **um nível só** (boca y 68 → nível y 44). Depois a Regra 137 (recomeço da escada, 0.3.100)
  a mandou de volta à boca (y 71), e ela ficou no nível 0 até o fim.
- Os mineiros cavaram mais de 7.000 blocos, quase todos pedra; o baú nunca passou de 9 ferros.
- A causa estrutural: cada nível abre um salão de 17×17×3 e mais 4 salões de ramal (~4.300 blocos)
  antes de descer. A mina não chega às camadas onde o minério está.
- Três mineiros num poço só:
  - dois "esperando ramal";
  - à noite os três "andando para lugar nenhum";
  - 56 desistências `NO_PATH`.

## O que a pesquisa diz

- **Camadas de minério (1.21, iguais desde 1.18):** cobre ~y 48; ferro ~y 16 (e montanhas ~y 232); lápis ~y 0;
  ouro ~y -16; diamante e redstone ~y -59; esmeralda só em montanha.
  ([guia](https://xgamingserver.com/blog/minecraft-best-y-level-ore-distribution-1-21/),
  [Shockbyte](https://shockbyte.com/en-au/blog/minecraft-ore-levels); as fontes variam em carvão e ouro.)
- **MineColonies:**
  - o poço desce até a profundidade do nível, e depois saem túneis dele;
  - o veio achado sai inteiro;
  - o mineiro "perdido" abre túneis novos para se achar.
  ([wiki](https://minecolonies.com/wiki/workers/miner/))
- **Mineração em espinha de peixe** (corredor de 2 de altura, ramais a cada 3 blocos): é o padrão mais
  eficiente de achar minério por bloco cavado.
- **Entradas de mina realistas:**
  - cortar a encosta e emoldurar com madeira (toras, vigas, cerca, portão), na madeira do bioma;
  - lanternas;
  - trilho com vagonete;
  - guindaste de madeira "puxando" pedra;
  - o visual das minas abandonadas do jogo.
  - Referências em vídeo: Gorillo (entrada na encosta), DiddiHD (guindaste), WaxFraud (túnel com lanternas e
    vagonete).
  ([Sportskeeda](https://sportskeeda.com/minecraft/best-minecraft-mine-entrance-designs),
  [1v9](https://1v9.gg/blog/minecraft-best-mineshaft-build-ideas))

## Decisões do autor (28-10)

1. **Portal: boca de mina de madeira, com decoração extra.**
   - Estrutura própria por bioma: moldura de toras e vigas cortando a encosta, telhadinho, lanternas
     penduradas, trilho entrando com vagonete, guindaste de madeira ao lado, barris e caixotes, placa e o
     baú da boca.
   - Em terreno plano vira uma "casa do poço" com o mesmo acabamento.
2. **Descida: escada direta + espinha de peixe.**
   - A escada em caracol (degrau ≤ 1, ida e volta pelo mesmo caminho) desce direto até as camadas de
     minério: y 48, y 16, y 0, y -16, y -59.
   - Em cada camada: um corredor de 2 de altura com ramais a cada 3 blocos.
   - O veio achado sai inteiro.
   - Os salões grandes saem.
3. **Dificuldades, todas:**
   - contornar o que não se cava e abrir outro percurso;
   - atravessar caverna e ravina com degraus de pedra assentados;
   - selar água e lava sem fechar a passagem;
   - **caminho salvo e iluminado:** o percurso boca → frente de trabalho fica gravado no save como pontos,
     iluminado a cada 12, refeito do último ponto bom se o mundo mudar, até o fundo do mundo (y -59).
4. **Coleta: tudo vai para os baús da vila** (como hoje).

## Desenho

- **`MinePlan`** (núcleo, puro) — o plano da mina a partir da boca:
  - a escada até a próxima camada-alvo;
  - o corredor e os ramais da espinha de peixe;
  - a ordem de cavar.
  - Substitui o caracol com salões da Regra 119; a ida e volta pela mesma escada fica.
- **`MineRoute`** (save) — os pontos do caminho salvo, da boca à frente atual:
  - o mineiro anda por eles (o `MineApproachPath` passa a lê-los);
  - quando um ponto não serve mais, o trecho é refeito a partir do anterior;
  - a luz vai no chão, a cada 12.
- **Obstáculos:**
  - bloco não cavável ou desnível sem saída: desvio lateral de 1 a 3 colunas e a escada segue;
  - vazio (caverna, ravina): degraus de pedra assentados por baixo;
  - água e lava: a face tocada vira pedra; a célula da passagem é drenada (a lava vira pedra/obsidiana, nunca
    fica).
- **Mineiros por frente:** um mineiro por frente aberta (escada ou corredor); o que sobra vai à próxima
  camada ou ao minério raso.
- **Portal:** NBT por estilo de bioma, como o armazém (`scripts/storage_variants.py`), assentado pela obra da boca.
- **Medida:** o `VC_STATE` ganha camada atual, profundidade, minérios por tipo e blocos por minério; o
  `state_report.py` mostra a descida por hora.

## Ordem de implementação

1. Caminho salvo (`MineRoute`) e volta por ele: a base anti-trava.
2. Descida direta às camadas e espinha de peixe (`MinePlan`), com migração do save da mina em caracol.
3. Obstáculos: contornar, ponte de pedra, selar água e lava.
4. Mineiros por frente.
5. Portal ornamental por bioma.
6. Medida no `VC_STATE`.

Cada etapa com GameTest e um commit.

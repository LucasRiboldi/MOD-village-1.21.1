# Casas de ofício `majest_` e a casa grande reformada — desenho

Data: 2026-11-26. Origem: jogo da 0.4.143, H5 (o pastor ficou preso no térreo da casa grande, num vão de 2 blocos
cercado por baús e camas). Pedido do autor, nas palavras dele, em ordem:

> "o modelo da casa do mod precisa ser 2 blocos maior de profundidade, reorganizar as camas e baus, para todos bau
> estarem encostados em uma parede e ter acesso de transito dentro da casa, adicionar tochas dentro da casa, chão de
> pedra no primeiro andar, tapas os buracos do segundo andar. criart um modelo de casa de arquitetura diferente com
> recursos padroa vanilla, para cada aldeao do mod, depois sempre que essa casa for construida pelo mod a profissao se
> muda para ela (mineiro ja tem casa do lado do arco da mina [atualizar a estrutura da casa dele para ser de estilo
> rustico e toda de varios tipos de pedras com troncos de madeira])"
>
> "criar um tipo de casa para cada profissao do jogo, salvar nos arquivos .bnt com o nome "Majest_""
>
> "todoas as casas deveem ter cama e bau, alem de decoração interna relativa a profissao, adicione placas na frente
> mostrando a profissao"

Escolhas do autor: as 9 profissões do mod (🅰️), e o caminho misto (🅲): o script gera as casas, o autor ajusta no
criativo e salva por cima.

## 1. Intenção

A vila do mod tem de ser andável por dentro das casas e ter cara de vila de ofícios. Cada titular ganha a casa do seu
ofício, que se reconhece de fora pela arquitetura e pela placa e de dentro pela decoração. A casa grande deixa de prender
aldeão.

**Objetivos**
- Uma casa `majest_<ofício>.nbt` para cada profissão do mod. O mineiro tem a dele refeita, e as outras 8 são novas:
  lenhador, pedreiro, fundidor, carpinteiro, fazendeiro, pastor, construtor e Zelador.
- Cada casa tem arquitetura própria, só com blocos do jogo, e leva pelo menos uma cama, um baú, decoração do ofício e
  uma placa na frente com o nome do ofício.
- Quando a casa do ofício fica pronta, o titular daquele ofício passa a dormir nela.
- A casa grande (`big_house_mod.nbt`) fica 2 blocos mais funda e ganha estas mudanças:
  - todo baú encostado numa parede;
  - corredor livre entre a porta, cada cama, cada baú e a escada;
  - tochas por dentro;
  - chão de pedra no térreo;
  - nenhum buraco no piso do andar de cima.
- A casa do mineiro fica rústica: vários tipos de pedra e troncos.

**Fora do escopo**
- Casa para as 13 profissões do Minecraft.
- Trocar as casas da vila gerada.
- Mudar quantas obras a vila faz (Regra 218).
- A manutenção do fundidor e do carpinteiro (o outro item do H5, ainda sem escolha).

**O que seria fracasso, mesmo funcionando**
- Aldeão preso dentro de casa.
- Baú solto no meio do cômodo.
- Casa que a vila não consegue terminar no deserto por falta de material.
- Bloco de trabalho do Minecraft que vire profissão do jogo em aldeão comum da vila.
- Ajuste que o autor fez à mão apagado por uma nova geração do script.

## 2. O que já existe e muda pouco

- **A vaga da casa de ofício existe** (`ColonyModels.tradeHousesFor`, Regra 49): um modelo
  `colony/trade_<ofício>.nbt` entra na lista de obras à frente das casas de ofício do jogo, uma de cada tipo. Hoje não
  há nenhum arquivo `trade_`. O leitor passa a aceitar `majest_<ofício>` como o nome da casa de ofício, e o `trade_`
  continua valendo, para não quebrar modelo de quem já usa.
- **Troca automática por bioma** (Regra 262): um arquivo serve todos os biomas.
  - A madeira vira a madeira da vila, na mesma forma.
  - O pedregulho e o arenito viram a pedra da paleta da vila (no deserto, o arenito).
  - As outras pedras não mudam: tijolo de pedra, andesito, pedregulho com musgo, pedra lisa.
  - Onde existe `colony/<estilo>/<modelo>.nbt`, o arquivo do estilo vale como está.
- **Placa da moradia** (`HomeDisplays`, `VC_HOME_SIGN`): já há uma placa externa por moradia, ligada às camas.
- **Cama** (Regra 74): cada aldeão dorme na cama mais perto do trabalho, revista ao amanhecer. A casa do mineiro já tem
  regra própria (Regras 211, 266, 286).
- **Bancadas do mod**: cada ofício tem a sua (`carpenter_bench`, `mason_cutter`, `blacksmith_forge`, `lumberjack_bench`,
  `farmer_bench`, `shepherd_bench`, `builder_bench`, `miner_bench`). As passivas da Regra 132 dependem de a bancada
  estar na vila.
- **Script da casa do mineiro**: `scripts/gen_miner_house.py` gera `miner_house.nbt` e uma versão por estilo. O
  `MineHomePlans.MODEL` aponta para `miner_house`.

## 3. Caminhos considerados

| | Como | Custo | Escolha |
|---|---|---|---|
| A | Script Python gera tudo, refaz a cada mudança | o gosto é do script; o autor corrige pedindo | — |
| B | O autor constrói as 9 casas no criativo | arquitetura toda dele; 9 obras à mão | — |
| **C** | **Script gera, autor ajusta no criativo e salva por cima** | **ajuste à mão se perde se o script rodar de novo** | **✅ autor** |

**Como o caminho C não apaga o ajuste do autor.**
- O script escreve em `scripts/houses/out/`, fora dos recursos do mod.
- Ele só copia para `src/main/resources/.../colony/` o arquivo que ainda não existe lá, ou quando roda com
  `--overwrite <nome>`.
- Depois que o autor salva por cima, o arquivo nos recursos é a fonte. O script não o toca mais sem pedido.
- Um teste confere todo `majest_*.nbt` dos recursos, gerado ou salvo à mão, contra as regras da seção 4 e da 8.

## 4. As casas

Cada casa tem uma arquitetura diferente, com tamanho entre 7×7 e 9×11 de planta. O desenho de cada uma é do script, e o
autor ajusta. Proposta de partida:

| Arquivo | Ofício | Cara | Decoração do ofício |
|---|---|---|---|
| `majest_lumberjack` | lenhador | cabana de toras deitadas, telhado de escada de duas águas | toras descascadas empilhadas, barril de mudas (vaso com muda) |
| `majest_mason` | pedreiro | sobrado de tijolo de pedra e pedra lisa, janelas com arco | blocos de pedra variados expostos, laje polida, muro de pedra |
| `majest_smelter` | fundidor | casa de tijolo de barro com chaminé alta | lanterna, bloco de ferro, corrente, campfire apagado na lareira |
| `majest_carpenter` | carpinteiro | casa de enxaimel (tronco + tábua), varanda | bancada (crafting table), pilhas de tábua, escadas e lajes de madeira |
| `majest_farmer` | fazendeiro | casa baixa com horta de janela e cerca | vasos com flores e plantas, fardos de feno, abóbora |
| `majest_shepherd` | pastor | casa com telhado de lã sobre tábuas, curral de janela | tapetes e lã coloridos, fardo de feno |
| `majest_builder` | construtor | casa com andaime de tábua por fora e oficina aberta | pilhas de blocos de obra, escada de mão, laje e degrau soltos |
| `majest_groundskeeper` | Zelador | casa simples com jardim e caminho | caminho de terra, vasos, lanterna de jardim |
| `majest_miner` | mineiro | **rústica: pedregulho com musgo, tijolo de pedra, andesito, pedra; troncos nos cantos** | trilho, lanterna, minério decorativo (bloco de carvão) |

**Regras de todas as casas**
- Pelo menos uma cama e um baú.
- Todo baú encostado numa parede, com passagem livre na frente.
- Corredor de 1 bloco de largura e 2 de altura entre a porta, a cama, o baú e a escada, se houver.
- Ao menos uma fonte de luz dentro.
- Piso inteiro, sem buraco.
- Porta no térreo com saída reta para fora (Regras 207 e 366).
- Uma placa na frente, ao lado da porta.

**Placa.**
- O arquivo traz o bloco de placa; o texto ("Carpinteiro", "Pedreiro"…) é escrito pelo mod quando a casa fica pronta.
  Os blocos da obra são postos sem os dados de bloco, então o texto não viria do arquivo.
- O texto é em português, por uma chave de tradução, com o português como padrão.

**Materiais.** Só blocos que a vila sabe obter ou fabricar: madeira, pedra, pedregulho, arenito, vidro, lã, tijolo de
barro, ferro em pouca quantidade. O teste da seção 8 recusa bloco sem origem na vila (Regra 13). Bloco caro fica só como
decoração de 1 ou 2 unidades.

**Sem bloco de trabalho do Minecraft.** Barril, composter, defumador, alto-forno, cortador de pedra, tear, mesa de
flechas, mesa de cartografia, atril, caldeirão, bigorna de ferreiro, rebolo e mesa de ferraria são postos de trabalho do
jogo: um aldeão comum da vila viraria profissão do Minecraft. A decoração usa blocos parecidos que não são posto de
trabalho. Este ponto é decisão minha, para o autor conferir.

**Bancada do mod: não vai dentro da casa.** A bancada fica onde o ofício trabalha. Pôr uma em cada casa ligaria as
passivas da Regra 132 sozinho. Este ponto também é decisão minha, para o autor conferir.

## 5. A casa grande

- **Profundidade.** A planta de hoje é 7 × 10 × 11 (largura × altura × comprimento), com a porta no lado comprido. Os
  2 blocos a mais vão da porta ao fundo: 7 → 9. O miolo de cada andar passa de 3 para 5 blocos de largura.
- **Camas.** Continuam 7 camas e 7 baús, para os titulares antes de terem casa própria (Regra 109). As camas ficam nas
  paredes do fundo e dos lados, cada baú ao pé ou ao lado da sua cama, encostado na parede.
- **Corredor.** Um corredor central de 1 bloco liga a porta, a escada e cada cama e baú.
- **Piso e luz.** O chão do térreo é de pedra (pedra lisa ou tijolo de pedra, que não vira arenito pela Regra 262). O
  piso de cima fica fechado, com escada de verdade no lugar dos buracos. Há tocha nas paredes dos dois andares.
- **Baú ao lado da cama.** O `VanillaBedChests` põe baú ao lado de cama: ele passa a pôr só encostado em parede e nunca
  fechando passagem. A casa grande já vem com os seus 7 baús, então ele não precisa pôr nenhum nela. A regra vale para
  todo baú novo que ele puser, também nas casas da vila gerada, porque é uma regra de segurança: baú não fecha
  passagem. O baú que já existe fica onde está. Decisão minha, para o autor conferir.
- **Casas já de pé.** A casa grande que já existe no mundo não muda. Só a vila nova nasce com a reformada.

## 6. Onde a casa de ofício entra na fila de obras

A casa `majest_` é **moradia**, e não a oficina do ofício.
- Hoje um modelo `trade_` conta como oficina (`ConstructionOrder.missingWorkshops`). Se a `majest_farmer` contasse
  assim, a vila deixaria de levantar a plantação; a do pastor tiraria o curral, e a do construtor nunca sairia, porque o
  armazém já ocupa a vaga dele.
- Por isso a `majest_` não ocupa a vaga de oficina: plantação, curral e armazém seguem como estão.
- Ela entra pela vaga de **casa** da Regra 49. Quando a vila vai levantar casa, ela escolhe primeiro a `majest_` de um
  ofício que tem titular e ainda não tem a sua casa, na ordem dos ofícios. Senão, levanta a casa de sempre.
- Assim a conta de obras da Regra 218 não muda. A casa de ofício conta como casa no rodízio e na conta de camas.
- Uma casa de cada ofício por vila. Decisão minha, para o autor conferir.

## 7. A mudança para a casa do ofício

- A casa `majest_<ofício>` pronta reserva a cama dela ao titular daquele ofício.
- A troca de cama da Regra 74 não tira o titular dali enquanto ele tiver o ofício.
- A cama dele na casa grande fica livre para outro adulto.
- A troca acontece assim que a casa fica pronta, como já é com a do mineiro (Regra 286). Ela não espera o amanhecer.
- Com 2 titulares do mesmo ofício, o primeiro fica na casa do ofício e o outro segue pela Regra 74.
- A reserva vale qualquer que seja a distância até o trabalho: o autor pediu que o ofício se mude. O lote da casa
  continua escolhido pela busca de sempre. Se o titular encalhar no caminho, isso volta como achado do jogo.
- **A casa do mineiro é a `majest_miner`, ao lado da boca da mina** (o autor: "mineiro já tem casa do lado do arco da
  mina, atualizar a estrutura"). O `MineHomePlans.MODEL` passa a apontar para `majest_miner`, e ela não entra na fila de
  casas da seção 6. Os arquivos `colony/<estilo>/miner_house.nbt` e o `miner_house.nbt` saem, para a casa rústica valer
  em todos os biomas. A casa do mineiro já de pé no mundo continua reconhecida pelo nome antigo.

## 8. Prova

- **Teste Python dos arquivos (`tests/`).** Para todo `majest_*.nbt` dos recursos e para a casa grande, confere:
  - cama, baú, luz e placa;
  - baú encostado em parede, com passagem na frente;
  - um caminho a pé, com altura 2 e degrau de até 1, ligando a porta a cada cama e a cada baú;
  - piso sem buraco;
  - nenhum posto de trabalho do Minecraft;
  - nenhum bloco sem origem na vila.
- **GameTest da mudança.** Com a casa do ofício pronta, o titular do ofício recebe a cama dela e não sai na troca do
  amanhecer.
- **GameTest da placa.** A casa pronta ganha o texto do ofício.
- **GameTest da casa grande.** Um aldeão posto em cada cama alcança a porta.
- **No jogo.** O autor vê cada casa com `/place template villagecolony:colony/majest_<ofício>` num mundo criativo antes
  de jogar.

## 9. Decisões que viram ADR

ADR-065, "Casas de ofício `majest_`":
- o nome `majest_` como casa de ofício;
- a cama reservada ao titular, como emenda da Regra 74;
- nenhum posto de trabalho do Minecraft dentro das casas;
- a bancada fora da casa;
- a casa de ofício pela vaga de casa, e não pela de oficina.

Regras novas, numeradas na ordem do `docs/RULES.md`.

## 10. Ordem de entrega

Uma versão por etapa; o autor joga entre elas.

1. A casa grande reformada, o baú ao lado da cama só na parede e o teste de passagem. É o que prende o pastor hoje.
2. O leitor do `majest_`, a mudança para a casa e a placa, com uma casa só (a do carpinteiro) para provar o caminho.
3. As outras 7 casas novas e a do mineiro rústica.

## 11. O que fica para quem constrói

- O desenho exato de cada casa: medidas, janelas e telhado, dentro das regras da seção 4.
- A escolha dos blocos de decoração que não são posto de trabalho.
- O texto da chave de tradução da placa.

# O quadrado da vila — proposta do autor, avaliada (2026-09-30)

Proposta do autor depois das medidas de `scripts/world_survey.py`
(`docs/technical/Identidade-da-Vila-2026-09-30.md` §7–8):

1. a busca de camas é uma **coluna com janela de altura**, e não uma esfera —
   abaixo do solo e no céu aberto não há cama de vila;
2. **planejar obras** usa como referência **a vila**, e não o centro nem o
   jogador;
3. a vila é um **quadrado**: nasce do tamanho típico, o centro fica no meio
   dele, e o quadrado **cresce** quando ruas e construções passam da borda —
   o centro se reposiciona no meio da nova medida e a vila fica registrada
   maior.

**Estado:** marcos 1 a 5 implementados no mesmo dia, com as decisões do autor
(ADR-003 Emenda 6). As diferenças para esta proposta: o foco não usa margem
em volta do jogador — só a caixa, mais 5 minutos depois que ele sai, e vila
fora disso não roda nada; construção ou lote empurra a borda a 12 blocos
além da peça, e rua inclui só o bloco. Os marcos 6 e 7 (raios do centro →
distância até a caixa) ficaram de fora, exceto o guarda da obra.

## 1. Busca de camas em coluna, com janela de altura

**Os dados confirmam.** Das 318 camas do save, **74 estão fora de qualquer
vila gerada**, e **67 delas estão no subsolo** (Y −7 a −38), em grupos de 3 a
5: são as camas das Trial Chambers do 1.21 (o chunk referencia
`minecraft:trial_chambers`). Dois grupos ficam **embaixo de vilas**: Y −20 sob
a vila do autor (#4) e Y −13 sob a #25.

**Por que a esfera de hoje não protege.** De um jogador na superfície (Y ~70),
a esfera de 64 não chega a Y −20. Mas a detecção tem outros gatilhos: a cama
de chunk recém-carregado (ADR-003, Emenda 3) dispara a busca **centrada na
própria cama** — inclusive na da Trial Chamber —, e o jogador numa caverna
dispara de onde está. Para validar, bastam 3 camas e 2 aldeões vivos na caixa
das camas + 32, e os mineiros da colônia cavam justamente lá embaixo (um
estava a Y −27, a 90 blocos dessas camas). O céu é o mesmo caso com a base do
jogador.

**Que janela.** Uma janela fixa em volta de uma altura falha na vila de
montanha: as camas ocupam 6 blocos de altura na mediana, mas **42** na vila
#7, cuja caixa gerada tem 55 de altura. Sugestão:

- **vila com estrutura gerada:** a faixa Y da caixa da vila, com 8 de folga
  para baixo e para cima;
- **vila sem estrutura** (feita pelo jogador, ou estrutura ainda não lida):
  do nível da rua − 8 ao nível da rua + 24, sendo o nível da rua a mediana Y
  das camas do aglomerado validado;
- **descoberta** (ninguém sabe ainda se há vila): coluna de raio 64 em X/Z em
  volta do gatilho, com Y entre `nível do mar − 8` e `superfície + 24` na
  coluna do gatilho. Corta Trial Chamber, mina e céu sem conhecer a vila.

O custo cai: hoje a busca percorre todas as seções de altura dos chunks do
raio; com a janela, só as 3 a 5 seções que a contêm.

> **Decisão:** as folgas (8 abaixo, 24 acima) e o caso da descoberta.

## 2. Planejar a partir da vila

**Hoje:** a colônia planeja e detecta quando há jogador a até 64 blocos **do
centro**. A vila do autor tem 157 blocos em Z: quem está na borda norte, a
~80 blocos do centro, está dentro da vila e mesmo assim fica de fora.

**Proposta:** planeja a colônia cujo quadrado, ampliado por uma margem,
contém o jogador. A execução segue a decisão de 30-09 (toda colônia
`ACTIVE`).

> **Decisão:** a margem — sugerida **32** (dois chunks
> de folga) ou a distância de simulação do servidor.

## 3. O quadrado da vila

### 3.1 O que os números dizem do tamanho típico

| Medida (35 vilas geradas) | Mediana | Faixa |
|---|---|---|
| X | 129 | 86–161 |
| Z | 141 | 96–158 |
| Lado maior | 144 | p90 157 |
| Altura (Y) | 20 | até 55 |

### 3.2 Avaliação

**O que a ideia resolve de uma vez.** As três regras da identidade do E51
(centro a 64, mesma vila gerada, cama perto de construção), as quatro da
fusão e o "perto do jogador" viram **uma pergunta**: *este ponto está dentro
do quadrado desta vila, mais a margem?* Hoje cada pergunta usa uma régua
diferente (ver §8 da análise).

**O que ela conserta além do pedido.** O centro hoje é a **média das camas**
e se move a cada leitura da sonda. Isso já custou caro: "obra órfã por deriva
do centro" (o centro oscilava entre 8 posições e a obra saía do raio) e o E46.
No quadrado, o centro só se move quando a vila **cresce**, e na direção do
crescimento: ele fica estável.

### 3.3 Melhorias sugeridas para a ideia

1. **Começar pela vila gerada, não pelo tamanho típico.** Quando o jogo gerou
   a vila, a caixa dela é a medida exata e está disponível (estrutura
   `#village`). O tamanho típico (**144 × 144**, a mediana do lado maior)
   fica para a vila sem estrutura, centrado no sino ou na média das camas.
2. **Retângulo, e não quadrado.** As vilas medem 129 × 141 na mediana, e
   chegam a 86 × 150. Um quadrado obrigatório cresce no eixo curto sem
   ter vila ali e toca vizinhas mais cedo. O desenho continua sendo uma caixa;
   X e Z crescem cada um por si.
3. **O que faz crescer, e o que não faz.** Cresce com o que a colônia
   **construiu ou abriu**: construção terminada, lote aberto, rua nova
   (`RoadIndex`), BigHouseMOD. **Não** cresce com mina, escada de fuga, bosque
   fundacional, roça de coleta nem cama solta — senão um túnel de mineiro
   arrasta a vila 200 blocos para o lado.
4. **Só cresce, nunca encolhe sozinho.** Encolher por leitura é o que fazia o
   centro oscilar. A vila abandonada continua a cargo da ADR-003 §6.
5. **Teto de crescimento.** Sem teto, duas vilas que crescem uma na direção da
   outra se tocam e se fundem. Nos dados, o menor vão entre vilas distintas é
   **37 blocos**: com margem de 16 nenhum par se toca hoje; com 32, um par
   (#23 e #26). Sugestão: margem de identidade **16**, e cada lado cresce no
   máximo até **256** blocos.
6. **Altura de referência = nível da rua**, e não a média Y das camas. É a
   mesma altura que a Regra da rua já usa para assentar as casas.
7. **Quem usa o centro como origem de raio passa a medir pela caixa.** São 89
   usos em 34 arquivos. Eles caem em três grupos:
   - raio a partir do centro (lote, coleta, rua, detecção, foco) → **distância
     até a caixa**, para o centro poder andar sem deixar obra órfã;
   - log, placa, nome, ordenação → continuam no centro;
   - ciclo de vida (`ACTIVE` quando o chunk do centro simula) → passa a valer
     **qualquer chunk da caixa**; numa vila de 157 blocos, o chunk do centro
     pode estar fora da simulação com o jogador dentro da vila.
8. **Vai para o save.** A caixa (4 inteiros + o nível da rua) é gravada; saves
   antigos a calculam ao carregar, a partir da estrutura gerada, das
   construções e das camas.

### 3.4 O que a ideia não resolve

- **Vila nunca visitada não existe para o mod.** 14 das 35 vilas não têm cama
  registrada, porque o jogo só registra a cama quando o chunk carrega. A
  caixa não muda isso.
- **Duas colônias já salvas na mesma vila** (#20) continuam até a fusão ver os
  chunks carregados. Com a caixa gravada no save, a fusão passa a responder
  com o chunk descarregado.

## 4. Como implementar, por marcos

| # | Marco | Arquivos principais | Risco |
|---|---|---|---|
| 1 | Janela de altura na busca de camas | `VillageScanner.collectBeds` | baixo |
| 2 | `VillageBounds` no Core: caixa, nível da rua, crescer, contém com margem | core `colony/model` + save | baixo |
| 3 | Caixa inicial pela vila gerada (ou 144 × 144) ao adotar; migração do save | `VillageAdoption`, `ServerLifecycleHandler` | médio |
| 4 | Identidade e fusão por "dentro da caixa + 16" (substitui as regras do E51) | `ColonyIdentity`, `ColonyMergeTrigger` | médio |
| 5 | Foco e detecção pela caixa + margem | `VillageFocus`, `VillageDetectionHandler` | médio |
| 6 | Crescimento: construção, lote e rua estendem a caixa; centro no meio | `BuildingRegistry`, `RoadIndex`, `Colony` | alto (89 usos) |
| 7 | Raios do centro → distância até a caixa | lote, coleta, rua | alto |

Os marcos 1 a 5 são independentes do 6 e do 7 e já cobrem o pedido. O 6 e o 7
mudam muito código e pedem ADR própria (emenda da ADR-003).

## 5. Decisões do autor

1. Folgas da janela de altura (sugestão: 8 abaixo, 24 acima da faixa da vila).
2. Margem do foco do jogador (sugestão: 32).
3. Retângulo ou quadrado (sugestão: retângulo).
4. O que faz a caixa crescer (sugestão: construção, lote e rua; nunca mina).
5. Teto por lado (sugestão: 256) e margem de identidade (sugestão: 16).
6. Se os marcos 6 e 7 entram agora ou depois do playtest dos marcos 1 a 5.

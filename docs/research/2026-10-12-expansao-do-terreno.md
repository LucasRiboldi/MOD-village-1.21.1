# Expansão do terreno da vila — opções (2026-10-12)

> Pedido do autor, 2026-10-12: *"o mod deve ter um plano para expansão do terreno da
> vila, pesquise e mostre opções"*. Este documento só propõe; a escolha é do autor.

## 1. Como a vila cresce hoje

- [FATO] A caixa nasce das peças da vila gerada (ADR-003 Emenda 8) e só cresce por
  consequência: obra ou lote fora dela empurra o lado que passa (+15), rua assentada
  entra inteira (`VillageGrowth.byPiece`, `byRoad`). Nunca encolhe.
- [FATO] O lote novo é procurado até ½ diagonal da caixa + 12 (Emenda 7), sem teto.
- [FATO] No save do autor a caixa foi a 115×163 (`grew to`, latest.log de 12-10).
- [INFERÊNCIA] Não existe plano: a vila só ocupa o que já é chão bom dentro do
  alcance. Quando o miolo enche, a varredura volta sem lote e a obra seguinte espera
  — o ritmo de crescimento é o da sorte do terreno, não uma decisão.

## 2. O que outros projetos fazem

| Projeto | Como cresce |
|---|---|
| MineColonies | Começa com um quadrado de chunks em volta da prefeitura (4 de raio); cada cabana construída reivindica um quadrado de chunks em volta dela; teto configurável de 20 chunks. Demolir devolve o que a cabana reivindicou. ([wiki](https://minecolonies.com/wiki/systems/border)) |
| Millénaire | Raio fixo por tipo de vila; as construções têm regras de lugar e podem não caber ("out of space" corrigido afrouxando o lugar); muralha em volta do centro, que melhora com a vila. ([notas de versão](https://www.curseforge.com/minecraft/mc-mods/millenaire/files/2695523)) |
| Vanilla | Não há terreno: a vila é o conjunto de camas e postos (POI). |

## 3. Opções

### A. Manter como está (crescimento por consequência)
- Custo zero. A vila cresce só onde acha chão; morro, rio e floresta param o lado.
- Risco: vila cheia para de construir sem dizer por quê.

### B. Reivindicação por obra (MineColonies)
- Cada construção pronta reivindica um quadrado fixo em volta (ex.: 16 para casa,
  24 para oficina e roça); a caixa é a união. Parecido com hoje, mas a margem vira
  regra por tipo e a caixa pode ter teto configurável.
- Simples; não resolve o miolo cheio — continua dependendo de achar lote.

### C. Anel planejado (recomendada com D)
- Gatilho: a vila está cheia — N varreduras seguidas sem lote, ou ocupação do chão
  da caixa acima de X%.
- A vila escolhe **um lado** para anexar uma faixa (ex.: 16 blocos): o mais plano,
  com menos água, menos construção alheia e menos desnível (medidas que o
  `world_survey` e o `isLotGround` já sabem fazer).
- Antes de abrir lote na faixa, prepara o chão: lenhador derruba e replanta fora,
  construtor aplaina degraus de 1–2 blocos.
- O log diz `expands east by 16 — west is river, north is cliff`.

### D. Bairro puxado pela rua
- A expansão começa estendendo uma rua principal para fora da caixa (corrente de
  caminhos, que a Emenda 8 já conta como vila); os lotes seguintes encostam na rua.
- Fica com cara de vila Vanilla (ruas com casas dos dois lados) e a rua já leva o
  aldeão até a obra nova.

### E. Estágios por população (Millénaire)
- Aldeia → vila → cidade: cada estágio, por número de adultos, libera um raio maior,
  uma muralha e tipos de obra. Mais desenho de jogo do que correção; fica para depois.

## 4. Recomendação

**C + D**: quando a vila encher, escolher o melhor lado, estender uma rua por ele e
abrir os lotes ao longo dela, preparando o chão antes. Reaproveita a medida da caixa
(Emenda 8), o crescimento por rua (`byRoad`) e a busca de lote (Emenda 7); o que é
novo é o gatilho, a escolha do lado e o preparo.

## 5. Decisões do autor

1. Qual opção (A–E, ou combinação)?
2. Gatilho: varreduras sem lote, ocupação, ou ambos? Qual número?
3. Tamanho da faixa anexada por vez.
4. Teto da caixa: sem teto (Emenda 7) ou um limite?
5. O preparo do chão pode derrubar árvore e aplainar morro, ou só usar chão já bom?

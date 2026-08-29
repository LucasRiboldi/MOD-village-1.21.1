# Comércio

**Trades são um sistema separado da profissão.** Esta é a confusão mais cara do
domínio, e vale repetir:

`[FATO]` MC 1.21.1 — o record `VillagerProfession` contém `id`, os dois predicados
de workstation, `gatherableItems`, `secondaryJobSites` e `workSound`.
**Não contém trades.**

Consequência prática: **para mudar o que um aldeão vende, você não toca na
profissão.**

## O modelo

```text
Profession
  ↓  define QUAL conjunto de trades se aplica
Trade Levels (1–5)
  ↓  cada nível libera um grupo
Trade Offers
  ↓  o que aparece na tela
Experience
  ↓  comerciar dá XP; XP sobe o nível
Restock
  ↓  ofertas esgotadas voltam ao trabalhar
```

Verifique na sua versão como as ofertas são declaradas — a estrutura
(`TradeOffers`, os factories por nível) mudou entre releases:

```bash
javap -cp "$MC_JAR" net.minecraft.village.TradeOffers | head -30
```

## O que considerar ao mexer

```text
LEVEL             em qual nível a oferta aparece
EXPERIENCE        quanto XP a troca dá
RESTOCK           com que frequência reabastece
DEMAND            preço sobe com a demanda
PRICE             base, e a variação
LOCKED TRADES     travadas até subir de nível
REPUTATION        gossip altera o preço
DISCOUNTS         herói da vila, cura de zumbi
PLAYER INTERACTION
```

## A regra de ouro

> **Nunca sobrescreva toda a lógica de comércio para alterar uma oferta.**

É o equivalente, no comércio, de substituir o Brain para mudar um comportamento.
Custa compatibilidade com todo mod de trades e congela a versão.

Ordem das alternativas:

```text
1. acrescentar uma oferta ao conjunto de uma profissão existente
2. definir o conjunto da SUA profissão nova
3. ajustar preço/estoque de uma oferta específica
4. só então, algo mais invasivo — com justificativa
```

## Economia

Trades mexem na economia do mundo. Um trade mal calibrado quebra o jogo mais
silenciosamente que um bug:

```text
[ ] a oferta cria item raro barato demais?
[ ] permite ciclo infinito de lucro? (comprar A → vender B → comprar A)
[ ] o preço é razoável frente às ofertas Vanilla?
[ ] o restock permite farm ilimitado?
```

O segundo é o mais comum: duas ofertas que se fecham em ciclo transformam
qualquer aldeão numa máquina de emeralds.

## Reputação e preço

O gossip altera preços. Se o seu mod mexe em gossip ou em preço, os dois se
influenciam — ver `references/gossip-and-reputation.md`.

```text
[ ] um jogador com reputação péssima ainda consegue comerciar?
[ ] herói da vila aplica desconto às SUAS ofertas também?
[ ] cura de zumbi aplica desconto?
```

## Quando o aldeão não comercia

Nem toda profissão precisa de trades. Um aldeão de trabalho puro — mineiro,
lenhador que produz para uma colônia — pode existir **sem** trades, **sem**
níveis e **sem** XP. O Vanilla não exige.

Isso simplifica muito o desenho, e é frequentemente o que se quer num mod de
automação. **Declare essa escolha** no plano da profissão, para ninguém achar que
faltou.

## Interface

A tela de comércio é **cliente**; a validação é **servidor**.

```text
[ ] o servidor valida a troca (o jogador tem os itens? a oferta existe? há estoque?)
[ ] o cliente só exibe
[ ] a oferta é sincronizada corretamente
```

Confiar no cliente aqui é um vetor de trapaça direto: item de graça.

## Checklist

Ver `checklists/trading.md` e `templates/trade-plan.md`.

```text
[ ] não sobrescrevi toda a lógica de comércio
[ ] a profissão NÃO foi alterada para mudar trades
[ ] os níveis fazem sentido (item raro em nível alto)
[ ] o XP por troca é coerente
[ ] o restock não permite farm ilimitado
[ ] nenhum ciclo infinito de lucro
[ ] preços coerentes com o Vanilla
[ ] a validação é server-side
[ ] testado em multiplayer
[ ] compatibilidade com mods de trades avaliada
```

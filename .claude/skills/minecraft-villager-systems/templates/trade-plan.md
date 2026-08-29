# Plano de comércio — <profissão / conjunto>

> **Trades são um sistema separado da profissão.**
>
> `[FATO]` MC 1.21.1: o record `VillagerProfession` não contém trades. Para mudar
> o que um aldeão vende, você **não** toca na profissão.

**Minecraft:** <versão> · **Data:** AAAA-MM-DD

## Escopo

```text
[ ] acrescentar oferta a uma profissão existente
[ ] definir o conjunto de uma profissão nova minha
[ ] ajustar preço/estoque de uma oferta específica
[ ] outra coisa — justifique
```

> **Nunca sobrescreva toda a lógica de comércio para alterar uma oferta.** É o
> equivalente, no comércio, de substituir o Brain para mudar um comportamento.

## Verificar a versão

```bash
javap -cp "$MC_JAR" net.minecraft.village.TradeOffers | head -30
```

<A estrutura de declaração de ofertas mudou entre releases.>

## As ofertas

| Nível | Dá | Recebe | XP | Estoque máx. | Multiplicador de preço |
|---|---|---|---|---|---|
| 1 Novice | | | | | |
| 2 Apprentice | | | | | |
| 3 Journeyman | | | | | |
| 4 Expert | | | | | |
| 5 Master | | | | | |

<Item raro em nível alto. Um item forte no nível 1 quebra a progressão.>

## Economia — as quatro perguntas

```text
[ ] a oferta cria item raro barato demais?
[ ] permite CICLO INFINITO de lucro? (comprar A → vender B → comprar A)
[ ] o preço é razoável frente às ofertas Vanilla?
[ ] o restock permite farm ilimitado?
```

> O segundo é o mais comum: duas ofertas que se fecham em ciclo transformam
> qualquer aldeão numa máquina de emeralds.

**Verificação do ciclo:**

<Liste as ofertas desta profissão e das próximas, e mostre que não fecham.>

## Restock

| | |
|---|---|
| Com que frequência | |
| Quantidade reabastecida | |
| Depende de trabalhar no POI? | |

## Reputação e desconto

```text
[ ] jogador com reputação péssima ainda consegue comerciar?
[ ] herói da vila aplica desconto às MINHAS ofertas?
[ ] cura de zumbi aplica desconto?
```

<Gossip altera preços — ver `references/gossip-and-reputation.md`.>

## Demanda

<Preço sobe com a demanda. Isso afeta as suas ofertas como esperado?>

## Client / Server

```text
[ ] a tela é CLIENTE
[ ] a validação é SERVIDOR: o jogador tem os itens? a oferta existe? há estoque?
[ ] as ofertas são sincronizadas corretamente
```

> Confiar no cliente aqui é trapaça direta: item de graça.

## Compatibilidade

```text
[ ] não sobrescrevi a lógica de comércio
[ ] testado com um mod de trades, se houver
[ ] classificação: LOW / MEDIUM / HIGH
```

## Teste

```text
[ ] as ofertas aparecem no nível certo
[ ] comerciar dá XP e sobe de nível
[ ] o restock funciona
[ ] os preços refletem a reputação
[ ] nenhum ciclo infinito de lucro
[ ] em multiplayer, dois jogadores veem preços coerentes com a reputação de cada um
[ ] fechar e reabrir o mundo mantém nível, XP e ofertas
```

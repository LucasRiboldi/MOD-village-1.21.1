# Checklist — comércio

> `[FATO]` MC 1.21.1: o record `VillagerProfession` **não contém trades**. Para
> mudar o que um aldeão vende, você não toca na profissão.

## Escopo

```text
[ ] NÃO sobrescrevi toda a lógica de comércio
[ ] a profissão NÃO foi alterada para mudar trades
[ ] escolhi a menor alteração que resolve
```

Ordem das alternativas:

```text
1. acrescentar oferta ao conjunto de uma profissão existente
2. definir o conjunto da minha profissão nova
3. ajustar preço/estoque de uma oferta específica
4. algo mais invasivo — com justificativa
```

## Versão

```bash
javap -cp "$MC_JAR" net.minecraft.village.TradeOffers | head -30
```

```text
[ ] confirmei a estrutura na minha versão
```

## Níveis

```text
[ ] item raro em nível ALTO
[ ] nenhuma oferta forte no nível 1
[ ] a progressão faz sentido
```

## Economia — as quatro perguntas

```text
[ ] a oferta cria item raro barato demais?
[ ] permite CICLO INFINITO de lucro?     ← o mais comum
[ ] o preço é razoável frente ao Vanilla?
[ ] o restock permite farm ilimitado?
```

> Duas ofertas que se fecham em ciclo (comprar A → vender B → comprar A)
> transformam qualquer aldeão numa máquina de emeralds.

```text
[ ] listei as ofertas desta profissão e das próximas, e verifiquei que não fecham
```

## XP e restock

```text
[ ] o XP por troca é coerente
[ ] o restock tem frequência definida
[ ] o restock depende de trabalhar no POI (como no Vanilla)?
```

## Reputação

```text
[ ] jogador com reputação péssima ainda consegue comerciar
[ ] herói da vila aplica desconto às minhas ofertas
[ ] cura de zumbi aplica desconto
[ ] o efeito da demanda no preço foi avaliado
```

## Client / Server

```text
[ ] a tela é CLIENTE
[ ] a validação é SERVIDOR: tem os itens? a oferta existe? há estoque?
[ ] as ofertas são sincronizadas corretamente
```

> Confiar no cliente aqui é trapaça direta: item de graça.

## Persistência

```text
[ ] nível, XP e ofertas sobrevivem a fechar e reabrir o mundo
[ ] o estoque volta como esperado
```

## Compatibilidade

```text
[ ] testado com um mod de trades, se houver
[ ] classificação LOW / MEDIUM / HIGH
```

## Teste

```text
[ ] as ofertas aparecem no nível certo
[ ] comerciar dá XP e sobe de nível
[ ] o restock funciona
[ ] os preços refletem a reputação
[ ] NENHUM ciclo infinito de lucro
[ ] em multiplayer, dois jogadores veem preços coerentes com a reputação de cada um
[ ] fechar e reabrir mantém nível, XP e ofertas
[ ] as profissões Vanilla continuam comerciando normalmente
```

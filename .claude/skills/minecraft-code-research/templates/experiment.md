# Experimento — <pergunta em poucas palavras>

> Guarde em `docs/experiments/`. **Uma pergunta, uma variável.** Cinco hipóteses
> no mesmo experimento significam cinco experimentos depois para descobrir qual
> errou.

**Data:** AAAA-MM-DD
**Minecraft:** <versão> · **Mappings:** <…> · **Fabric API:** <…> · **Loader:** <…>

## Pergunta

<Uma frase. Uma variável.>

## Por que ler não resolveu

<Duas leituras plausíveis? Depende de ordem de execução? O código é claro mas o
jogo contradiz? — se você ainda não rodou `javap` no alvo, faça isso antes: é
leitura, é barato, e resolve boa parte das dúvidas sem experimento.>

## Hipótese

<O que você espera, e **por quê**. A justificativa importa: sem ela, um resultado
inesperado não ensina nada.>

## Como saber que estou errado

<O que eu observaria se a hipótese fosse falsa. Escrever isto antes evita
interpretar o resultado a favor da expectativa.>

## Setup mínimo

<O menor mundo/código que responde. Se o experimento sobe o mod inteiro, ele não
isola nada — qualquer parte pode ser a causa.>

- Mundo: <gametest EMPTY_STRUCTURE / mundo de teste / save específico>
- Entidades: <quantas, quais>
- Código carregado: <o mínimo>

## Implementação

```java
// o código do experimento, ou o caminho para ele
```

Se for gametest:

```bash
./gradlew runGametest
```

> Lembretes: coordenadas do `TestContext` são **relativas** — use
> `context.getAbsolutePos(...)` antes de falar com o `ServerWorld`. O mundo de
> teste é **vazio**: sem vila, sem estrutura, sem worldgen. `context.complete()`
> no fim.

## Resultado esperado

<…>

## Resultado real

> **Escreva isto antes de interpretar.** A ordem importa — interpretar enquanto
> observa é como o resultado vira o que você queria ver.

<…>

**Repetições:** <n> — <observado n vezes, com o mesmo resultado?>

## Logs

```text
<trecho relevante, não o arquivo inteiro>
```

## Conclusão

`[FATO]` / `[INFERÊNCIA]` <…>

**A hipótese estava:** correta / incorreta / parcialmente correta

> Resultado negativo é resultado: eliminou um caminho e evitou uma arquitetura.
> Registre com o mesmo cuidado do positivo.

## Confiança

alta / média / baixa — <por quê>

## Limitações

<O que este experimento **não** prova. Ex.: "o mundo do gametest não tem vila
gerada, então a metade 'vila original' da regra não foi exercitada.">

## Impacto arquitetural

<O que isto muda na decisão. Se não muda nada, diga.>

## Próximo passo

<Nova pergunta, ou `[DECISÃO]` pronta para ADR.>

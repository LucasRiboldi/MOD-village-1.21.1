# Resumo de implementação — <feature / sistema>

> O que se entrega ao concluir. Escrito para quem **não acompanhou** o trabalho.

**Data:** AAAA-MM-DD · **Modo:** SMALL / FEATURE / SYSTEM / SURGERY
**Minecraft:** <versão> · **Fabric API:** <versão>

## Feature

<O que passou a ser possível. Uma ou duas frases, do ponto de vista de quem joga.>

## Arquivos alterados

| Arquivo | O que mudou |
|---|---|

```bash
git diff --stat
```

## Arquitetura

<A menor arquitetura correta que foi escolhida, e por quê. Se houve alternativa
descartada, diga qual.>

**Onde o estado mora:**

| Estado | Dono | Mecanismo |
|---|---|---|

## Integração com o Vanilla

| Ponto | Degrau da escada | Observação |
|---|---|---|

<Se subiu a escada, a justificativa vai aqui.>

## Integração com a Fabric

| API / evento | Para quê |
|---|---|

## Client / Server

<Quem tem autoridade. O que o cliente vê e como recebe. — não se aplica.>

## Persistência

<O que sobrevive, onde. E o que **deliberadamente não** é persistido, com o
motivo.>

## Networking

<Packets criados. — não se aplica: <por quê>>

## Mixins

| Alvo | Tipo | Ponto | Risco | Justificativa |
|---|---|---|---|---|

<Nenhum, se for o caso — e isso é uma boa notícia digna de nota.>

## Resources

```text
[ ] lang   [ ] modelo   [ ] textura   [ ] blockstate
[ ] loot table   [ ] recipe   [ ] tags   [ ] item group
```

---

## Testes realizados

> **Separe explicitamente.** "Tem teste" e "foi verificado rodando" são coisas
> diferentes, e a diferença importa.

**Verificado rodando:**

| Verificação | Comando | Resultado |
|---|---|---|
| compila | `./gradlew build` | |
| gametest | `./gradlew runGametest` | |
| cliente | `./gradlew runClient` | |
| servidor dedicado | `./gradlew runServer` | |
| save/load | manual | |

**Tem teste escrito (não executado agora):**

**Não verificado, e por quê:**

## Riscos

| Risco | Tipo | Severidade | Mitigação |
|---|---|---|---|

## Compatibilidade

**Classificação:** LOW / MEDIUM / HIGH — <justificativa>

<Conflitos conhecidos, documentados.>

## Performance

<Frequência × população, com números. Se foi medido, os números; se não foi, diga
que não foi.>

## Trabalho restante

> O que ficou de fora, e por quê. Escopo reduzido **declarado** é entrega;
> reduzido em silêncio é dívida escondida.

| Item | Motivo | Prioridade |
|---|---|---|

## Conhecimento devolvido à base

| Onde | O quê |
|---|---|
| `docs/research/` | descoberta sobre o Vanilla |
| `docs/architecture/` | mudança de arquitetura |
| `docs/decisions/` | decisão que dura |
| `docs/experiments/` | experimento rodado |
| `research-status.md` | pesquisa aberta atualizada |

> Conhecimento descoberto durante implementação é o mais caro que existe: custou
> o bug. Deixá-lo só na mensagem de commit é jogá-lo fora.

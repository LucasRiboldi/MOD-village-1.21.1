# Avaliação das skills — 2026-08-29

Registro honesto do que foi testado, o que passou e o que **não** se sustentou.
Está aqui para que uma sessão futura não refaça o trabalho nem repita a alegação
errada.

## Veredito

| Camada | Resultado |
|---|---|
| Estrutura | ✅ aprovada |
| Precisão factual | ✅ aprovada **após 3 correções que o teste encontrou** |
| Comportamento (conhecimento) | ⚖️ empate — 12/12 × 12/12 |
| Comportamento (processo) | ⚖️ empate — 9/9 × 9/9 |

**Nenhuma vantagem de qualidade foi medida.** As skills custaram ~14% mais
tokens (221k × 194k) na tarefa de implementação.

## O que a camada factual encontrou

Esta foi a única camada que se pagou. Conferir cada `[FATO]` contra o jar, em vez
de contra a memória:

1. **`setTaskList` tem cinco sobrecargas, não três.** O erro nasceu do comando —
   `grep -m3` parava no terceiro resultado. O número veio da ferramenta, não do
   jogo.
2. **Faltava metade do fato.** `setTaskList` acrescenta a lista de tasks
   (`computeIfAbsent` + `Set.add`), mas `requiredActivityMemories` usa `Map.put`
   — as memórias exigidas da Activity são substituídas.
3. **Exemplo redundante.** `.maxCount(1).maxDamage(180)`: `maxDamage` já seta
   `MAX_STACK_SIZE = 1`. **Quem apontou foi o agente de baseline, sem skill.**

Bônus: `getWorldChunk` também devolve `null` fora da thread do servidor.

## Como os testes foram feitos

**Rodada 1 — conhecimento.** 4 perguntas de uma tacada (WALK_TARGET, profissão
vs trades, POI/block states, item simples), com e sem skill, 12 critérios.

**Rodada 2 — processo.** Tarefa de implementação real (contador de árvores por
colônia, persistido, logado no stop), em clones isolados do repositório. 9
critérios lidos do código produzido e do relatório de entrega — nenhum media
conhecimento de API.

A rodada 2 tinha uma armadilha deliberada: `static Map<UUID, Integer>` resolve o
pedido, compila e perde tudo no restart. **Nenhum dos dois caiu nela.**

## O baseline fez, sem skill nenhuma

- rodou `./gradlew build` de verdade (BUILD SUCCESSFUL em 4m26s)
- rodou testes de unidade e `runGametest`
- integrou ao `ColonySavedData` existente, no mesmo arquivo
- tratou save antigo (chave ausente → 0) e save corrompido (total negativo)
- atualizou gametest, testes de unidade e o `Save-Data-System.md`
- escreveu seções "o que rodou de verdade", "o que **não** foi verificado" e
  "limite conhecido (não corrigido de propósito)"

## Aviso metodológico

O corretor por palavra-chave produziu **quatro falsos resultados**, todos
pegos por leitura manual, e **três deles favoreciam a skill**. Correção
automática por regex não é confiável aqui: o baseline expressa a mesma ideia
com outro vocabulário ("Não executado" em vez de "não verificado").

**Só o par script + leitura vale.**

## Limites deste resultado

- n=1 por condição na rodada 2. Uma tarefa não prova ausência de diferença.
- Ambas as rodadas usaram Opus 5. Modelos menores podem se beneficiar mais.
- Não foi medida consistência entre sessões, que é onde a skill mais plausivelmente
  ajuda — e que exigiria repetição, não uma execução.

## O que isso significa na prática

As skills **não** devem ser vendidas como "o Claude não sabe isso" — ele sabe.

O que elas continuam sendo, com valor real:

- **referência verificada** contra o jar desta versão, com os comandos de checagem
- **documentação do projeto**, versionada junto do código
- **convenções explícitas** para quem não é o autor
- possivelmente úteis para modelos menores ou sessões longas

O que **não** se pode afirmar sem novo teste: que melhoram a qualidade da saída
do Opus 5.

# Exemplo — comparar implementações

**Situação:**

> Três mods resolvem "aldeão faz trabalho customizado". Precisamos escolher a
> abordagem do nosso projeto e a decisão vai durar anos.

Este exemplo mostra o modo **DEEP** fechando numa decisão registrada.

---

## Fase 0 — A pergunta comparativa

Escrita **antes** de abrir código. Sem ela, você compara arquitetura em geral e
não conclui nada.

```text
Como diferentes mods fazem o aldeão executar trabalho customizado, e qual
abordagem preserva melhor o comportamento Vanilla?
```

## As linhas da comparação

Sempre incluindo o Vanilla e o nosso projeto:

```text
├── Vanilla          o que o jogo já faz sozinho
├── Mod A            captura em bloco
├── Mod B            task acrescentada ao Brain
├── Mod C            substituição do Brain
└── Nosso projeto    ?
```

A linha do **Vanilla** é a mais importante. Duas vezes em cinco ela é a resposta
— o jogo já fazia, e ninguém tinha olhado.

## A matriz

| Dimensão | Vanilla | A: captura | B: task no Brain | C: substitui Brain | Nosso |
|---|---|---|---|---|---|
| Versão MC | 1.21.1 | <ver> | <ver> | <ver> | 1.21.1 |
| Abordagem | Schedule + Brain decidem | aldeão vira estado de BlockEntity | `setTaskList` acrescenta task | remove e reescreve tasks |  |
| Degrau da escada | — | 10 (Mixin) + BlockEntity | 1 + 6 + 10 mínimo | 11 (remoção/overwrite) |  |
| Touchpoint Vanilla | — | entidade sai do mundo | `initBrain` | `initBrain` + listas |  |
| Mixins | — | vários, alguns `@Redirect` | 1 `@Inject` em `TAIL` | vários, invasivos |  |
| Vanilla preservado | — | **não** (IA não roda) | **sim** (integral) | **não** |  |
| Persistência | mundo | BlockEntity NBT | registro + PersistentState | próprio |  |
| Client/Server | — | precisa de sync de tela | server-only | server-only |  |
| Performance | baixa | baixa (IA desligada) | média (task por aldeão) | variável |  |
| Risco compat. | — | **HIGH** | **LOW** | **HIGH** |  |
| Convive com mods de aldeão | — | não | sim | não |  |

> As células de versão ponderam tudo. Um mod de 1.20 mostra que a abordagem é
> viável; não mostra como escrevê-la hoje.

## Ler a matriz

Aplicando os padrões de `references/comparison-analysis.md`:

- [x] **Um usa muito menos Mixin que os outros** → B achou um extension point que
      os demais não usaram (`setTaskList` acrescenta). **Investigar B a fundo.**
- [x] **Cada um resolveu diferente** → não há caminho canônico; a escolha é nossa
      e precisa de ADR.
- [ ] Todos usam o mesmo mecanismo — não é o caso.
- [x] **Todos evitam a mesma coisa**: nenhum tenta controlar o aldeão por
      `getNavigation()`. Vale descobrir por quê antes de ser o primeiro a tentar.

O último item mandou a pesquisa para `examples/analyze-vanilla-system.md`, onde a
resposta apareceu: o Brain sobrescreve a navegação no mesmo tick.

> `[FATO]` Em 1.21.1, quem controla o caminho de um mob de Brain é a memória
> `WALK_TARGET`. Verificado por `javap` sobre `minecraft-merged` 1.21.1.

Ou seja: a ausência na matriz **era** informação. Todos evitavam porque não
funciona.

## Critérios — declarados antes da conclusão

> Sem isto, a conclusão parece objetiva e é preferência não declarada.

```text
Este projeto prioriza, nesta ordem:
1. preservar a experiência Vanilla   (é a missão do mod)
2. compatibilidade com outros mods
3. performance
4. controle total sobre a rotina
```

Com esses pesos, a matriz decide sozinha. Com pesos diferentes — se "controle
total" fosse o primeiro — A ou C ganhariam, e isso estaria certo para *aquele*
projeto.

## Decisão

`[DECISÃO]` **Abordagem B**: task própria acrescentada ao Brain via `setTaskList`,
instalada por um `@Inject` em `TAIL` no `initBrain`, sem remover nada do Vanilla.

**Por quê:** é a única que preserva o comportamento Vanilla integralmente
(critério 1) e a única classificada LOW em compatibilidade (critério 2).

**Do que abrimos mão:** controle total sobre a rotina. O aldeão continua
dormindo, comendo e socializando quando a agenda Vanilla mandar — e o trabalho da
colônia só acontece na janela de trabalho.

Para este projeto isso não é custo, é requisito. Para outro projeto, seria custo.

**O que faria mudar de ideia:** se o design passar a exigir que o aldeão ignore a
agenda Vanilla, B deixa de bastar e a decisão precisa ser reaberta.

## Encaminhamento

- [x] vira ADR em `docs/decisions/` (`templates/architecture-decision.md`)
- [x] gera hipótese a validar: a task em CORE roda durante PANIC?
      (`templates/experiment.md`)
- [x] `research-status.md` atualizado
- [x] handoff para `fabric-development` com degrau, riscos e documentos

---

## O que este exemplo demonstra

1. **A pergunta comparativa vem antes do código.** Ela é o critério de parada.
2. **O Vanilla é uma linha da tabela.** Frequentemente é a resposta.
3. **A ausência é informação.** "Todos evitam X" mandou a pesquisa para o lugar
   certo e produziu o fato mais útil da análise.
4. **Os critérios ponderados foram declarados antes da conclusão** — é o que
   separa decisão de racionalização.
5. **"O que faria mudar de ideia" é o que evita** que a decisão seja defendida por
   inércia dois anos depois.

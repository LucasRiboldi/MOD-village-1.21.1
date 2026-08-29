# Análise de sistema — <NOME DO SISTEMA>

> Artefato principal dos modos FEATURE e DEEP. Guarde em
> `docs/research/systems/` ou `docs/research/vanilla/`.
>
> Apague as seções que não se aplicam. Seção vazia mantida é ruído; seção
> apagada com um motivo (`— não se aplica: <por quê>`) é informação.

**Sistema:** <ex.: trabalho do aldeão>
**Minecraft:** 1.21.1 · **Mappings:** Yarn 1.21.1+build.3 · **Fabric API:** <versão>
**Data:** AAAA-MM-DD · **Modo:** FEATURE / DEEP

## Objetivo

<A pergunta que esta análise responde. Uma frase. Se você não consegue escrever,
a pesquisa ainda não tem alvo.>

---

## Arquitetura Vanilla

<Como o jogo faz isso hoje. Prosa curta, depois a cadeia.>

```text
Entry        →
Core Class   →
Supporting   →
Data         →
State        →
Events       →
Persistence  →
Integration
```

## Classes principais

| Classe | Papel | Observação |
|---|---|---|
| `net.minecraft…` | | |

> Uma análise detalhada por classe importante: `vanilla-class-analysis.md`.

## Ciclo de vida

<Em qual linha do tempo o sistema atua — jogo, entidade ou block entity — e em
qual ponto dela.>

## Fluxo de execução

```text
TRIGGER → ENTRY → VALIDATION → DECISION → ACTION → STATE CHANGE → SYNC → PERSISTENCE
```

> Fluxo detalhado, com callers: `execution-flow.md`.

## Fluxo de dados

<De onde vem a informação, quem transforma, para onde vai.>

## Estado

| Estado | Dono | Mecanismo | Sobrevive a |
|---|---|---|---|
| | | campo / DataTracker / NBT / PersistentState | tick / chunk unload / save / restart |

## Registries

| Registro | Entrada | Quando |
|---|---|---|

## Eventos

| Evento | Quando dispara | Side | Cancelável |
|---|---|---|---|

## IA

<Goal ou Brain? Quais memórias, sensores, activities e tasks participam?
— não se aplica: <por quê>>

## Persistência

<O que sobrevive, onde, escrito por quem, lido por quem.>

## Networking

<O que o cliente precisa saber, e como recebe. — não se aplica: <por quê>>

## APIs Fabric disponíveis

| Necessidade | API | Limitação |
|---|---|---|

## Como outros mods resolvem

| Mod | Abordagem | Degrau | Risco |
|---|---|---|---|

> Comparação completa: `comparison.md`.

---

## Extension points

Em ordem da escada de extensão, do menos ao mais invasivo:

| # | Ponto | Viável? | Observação |
|---|---|---|---|
| 1 | Sistema Vanilla existente | | |
| 2 | Registro Vanilla | | |
| 3 | Data-driven (JSON/tag/datapack) | | |
| 4 | Fabric API | | |
| 5 | Fabric Events | | |
| 6–8 | Composição / interface / herança | | |
| 9–11 | Mixin (accessor → inject → overwrite) | | |

## Abordagens possíveis

**Opção A — <nome>**
Degrau <n>. <Descrição.> Custo: <…>. Risco: <…>.

**Opção B — <nome>**
…

## Riscos

| Risco | Tipo | Severidade | Mitigação |
|---|---|---|---|
| `[RISCO]` | | | |

## Performance

<Frequência × população. Números, não adjetivos.>

## Compatibilidade

**Classificação:** LOW / MEDIUM / HIGH — <justificativa em uma linha>

---

## Evidência

| Afirmação | Etiqueta | Fonte | Versão |
|---|---|---|---|
| | `[FATO]` / `[INFERÊNCIA]` / `[HIPÓTESE]` | arquivo · classe · método | |

**Confiança geral:** alta / média / baixa

## Conclusão

<Resposta direta à pergunta do objetivo. Não um resumo do que foi lido.>

**Recomendação:** degrau <n> da escada — <abordagem>, porque <motivo>.

## Perguntas em aberto

| Pergunta | Prioridade | Próximo passo |
|---|---|---|
| | P0–P4 | `[VALIDAÇÃO NECESSÁRIA]` <…> |

# ADR-<NNN> — <título da decisão>

> Guarde em `docs/decisions/`. Documente decisões que **afetam o futuro** — não
> decisões triviais. Uma ADR que ninguém consultaria não deveria existir.

**Status:** proposta / aceita / substituída por ADR-<NNN> / revogada
**Data:** AAAA-MM-DD
**Minecraft:** <versão> · **Mappings:** <…> · **Fabric API:** <…>

## Contexto

<A situação que torna a decisão necessária. O que existe hoje, e o que mudou para
que isso precise ser decidido agora.>

## Problema

<Uma frase. O que precisa ser resolvido.>

## Pesquisa que sustenta

| Documento | O que estabeleceu |
|---|---|
| `docs/research/…` | |

**Fatos relevantes:**

- `[FATO]` <…> — fonte, versão

**Incertezas ainda abertas:**

- `[HIPÓTESE]` <…> — impacto se estiver errada: <…>

## Opções

### Opção A — <nome>

**Degrau da escada:** <1–11>
**Como funciona:** <…>
**Custo:** <…>
**Risco de compatibilidade:** LOW / MEDIUM / HIGH
**Risco de versão:** <…>

### Opção B — <nome>

…

### Opção C — <nome>

…

## Critérios

> Declare o peso **antes** da conclusão. Sem isso, a decisão parece objetiva e é
> preferência não declarada.

Este projeto prioriza, nesta ordem: <ex.: compatibilidade > performance >
simplicidade > completude>.

## Decisão

`[DECISÃO]` <A escolha, em uma frase.>

## Por quê

<Ligado aos critérios acima. Se subiu a escada de extensão, a justificativa do
salto vai aqui — é o motivo desta seção existir.>

## O que estamos abrindo mão

<Toda escolha tem custo. Nomeá-lo é o que permite revisitar depois sem refazer a
análise inteira.>

## Consequências

**Positivas:**

**Negativas:**

**Neutras, mas a lembrar:**

## Riscos aceitos

| `[RISCO]` | Severidade | Mitigação | Aceito porque |
|---|---|---|---|

## O que faria mudar de ideia

> A seção mais valiosa e a mais esquecida. O gatilho para reabrir a decisão.

<Ex.: "se precisarmos que funcione com o chunk descarregado, esta escolha não
serve mais.">

## Validação

<Como saberemos que a decisão foi boa? Que teste, métrica ou observação?>

## Escopo de versão

`[VERSÃO]` Esta decisão vale para <MC/Fabric>. <O que a invalidaria numa migração.>

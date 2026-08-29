# Comparação — <a pergunta comparativa>

> Guarde em `docs/research/comparisons/`. Não é para eleger o melhor mod: é para
> ver o **leque** de abordagens e escolher com critério.

**Data:** AAAA-MM-DD · **Nossa versão:** MC <…>, mappings <…>

## Pergunta

<Uma frase, escrita **antes** de abrir o código. Ex.: "como diferentes mods fazem
o aldeão trabalhar sem quebrar a IA Vanilla?">

## Implementações comparadas

| # | Fonte | Versão MC | Mappings | Licença |
|---|---|---|---|---|
| — | **Vanilla** | <nossa> | — | — |
| A | | | | |
| B | | | | |
| — | **Nosso projeto** | | | |

> A linha do **Vanilla** é a mais importante: é a linha de base e, com frequência,
> a resposta. Versão diferente da nossa responde "é possível?", não "como escrevo
> isso hoje".

## Matriz

| Dimensão | Vanilla | A | B | Nosso projeto |
|---|---|---|---|---|
| Abordagem (1 frase) | | | | ? |
| Degrau da escada | — | | | ? |
| Touchpoint Vanilla | — | | | ? |
| Fabric API usada | — | | | ? |
| Mixins (nº / tipos) | — | | | ? |
| Dados (JSON/NBT/componentes) | | | | ? |
| Persistência | | | | ? |
| Client / Server | | | | ? |
| Performance aparente | | | | ? |
| Risco de compatibilidade | — | LOW/MED/HIGH | | ? |
| Vantagens | | | | |
| Riscos | | | | |

## Padrões observados

> O que a tabela mostra quando fica pronta:

- [ ] **Todos usam o mesmo mecanismo Vanilla** → é o caminho previsto; divergir exige motivo forte
- [ ] **Cada um resolveu diferente** → não há caminho canônico; documente a decisão
- [ ] **Um usa muito menos Mixin** → achou um extension point que os outros não viram; investigue **esse**
- [ ] **O mais novo é mais simples** → a API ganhou suporte no caminho; os antigos carregam contorno histórico
- [ ] **Todos evitam a mesma coisa** → há uma armadilha ali; descubra qual antes de ser o primeiro a cair

<Qual(is) se aplicam, e o que significam aqui.>

## Critérios de escolha

> Declare o peso antes de concluir.

Este projeto prioriza: <ex.: compatibilidade > performance > simplicidade>.

## Conclusão

**A escolha:** <uma frase>

**Por quê:** <ligado aos critérios acima>

**Do que abrimos mão:** <o custo da escolha>

**O que faria mudar de ideia:** <o gatilho para reabrir>

## Encaminhamento

- [ ] vira `[DECISÃO]` em `docs/decisions/` (`architecture-decision.md`)
- [ ] gera hipótese a validar (`experiment.md`)
- [ ] atualiza `research-status.md`

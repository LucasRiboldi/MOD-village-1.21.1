# Roteiro de revisão de código — Village Colony

**Criado em 2026-09-30.** Junta o guia de revisão do Google
([eng-practices](https://google.github.io/eng-practices/review/reviewer/looking-for.html))
com as armadilhas que este projeto já pagou. O `/code-review` e o
`gauntlet-verifier` revisam contra ele. Ver
`docs/research/2026-09-30-avaliacao-profissional-de-codigo.md`.

A ordem importa: desenho primeiro, porque um desenho errado torna o resto
irrelevante.

## 1. Desenho

- [ ] O problema, o sistema responsável, os arquivos e a decisão
      arquitetural estão respondidos (CLAUDE.md §0.3)?
- [ ] Mudou uma regra do autor? Então há ADR nova ou emendada, e a linha em
      `docs/RULES.md`.
- [ ] `core` continua sem Minecraft (ADR-006). O `ArchitectureRulesTest` e o
      `DependencyRuleTest` dizem isso, mas a pergunta é se a lógica nova
      **deveria** estar no `core` e foi parar na `fabric`.
- [ ] Mixin novo? Só se não houver evento (ADR-004), só `@Inject` ou
      `@ModifyVariable`/MixinExtras, nunca `@Redirect` ou `@Overwrite`, e ele
      só delega.
- [ ] Excesso de engenharia: a generalização atende um caso que existe hoje?

## 2. Funcionalidade

- [ ] Chunk descarregado, entidade morta ou sumida, baú cheio, baú
      inalcançável: cada caminho que toca o mundo trata os quatro?
- [ ] Transição, e não só estado: o que acontece no ciclo seguinte, quando o
      trabalhador é recontratado, quando a tarefa volta à fila?
- [ ] Castigo que vira rodízio: punir o item sem frear a taxa empurra para o
      próximo.
- [ ] Alvo repetido com castigo ativo: a porta consulta a marca que a outra
      porta escreveu?
- [ ] Estado estático novo: está no `ServerMemory` (o `ArchitectureRulesTest`
      reprova se não estiver)?
- [ ] Custo por tique: varredura cara tem orçamento e não roda duas vezes no
      mesmo tique? `ChestWithdrawer.countIn` em laço?

## 3. Testes

- [ ] O teste falhou antes da correção (fase vermelha registrada)?
- [ ] O teste chama o código de produção, e não uma cópia do predicado.
- [ ] A condição do cenário é possível. Uma condição impossível deixa o teste
      vazio; só a mutação separa "passou certo" de "não mediu nada".
- [ ] GameTest novo está registrado no `fabric.mod.json` do gametest, e o
      batch aparece no log da bateria.
- [ ] Contagem honesta: o número vem do XML de `build/test-results` ou do
      "All N required tests passed", nunca de memória.
- [ ] Regra de colônia com transições: cabe uma propriedade em
      `HiringProperties` (jqwik)?

## 4. Complexidade e legibilidade

- [ ] Arquivo ≤ 500 linhas (`FileSizeRuleTest`); congelado não cresce.
- [ ] Método novo com complexidade cognitiva acima de 15 no relatório do PMD
      (`build/reports/pmd/main.html`) tem motivo.
- [ ] O comentário diz o **porquê** e a data; o **o quê** está no código.

## 5. Entrega

- [ ] `gradlew build` (unitários, propriedades, PMD, Error Prone + NullAway
      com `-Werror`) e `gradlew runGametest` rodados de verdade.
- [ ] `gradlew pitest` acima do limite (85%) quando o `core` mudou.
- [ ] "Tem teste" separado de "visto em jogo" no commit e no STATE.
- [ ] JAR: SHA-256 igual em `build/libs`, `downloads/` e `mods`
      (`release_manifest.py`).
- [ ] Depois do playtest: `python scripts/analyze_village_log.py
      --max-slow-cycles N --max-cycle-ms M` para o orçamento de tique.

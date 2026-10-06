# ADR-035 — Direção técnica depois da auditoria

**Status:** Accepted
**Date:** 2026-10-06
**Decision Type:** Architecture / Process
**Origem:** avaliação técnica de 2026-10-06 (sete pontos), pedida e aprovada
pelo autor: *"Aplique todos os 7 pontos listados, não modifique o que listou
para não modificar, aplique ADR nova, faça tudo devagar e devidamente
estruturado"*. Base: `docs/audit/`.

## Contexto

A auditoria de 2026-10-06 achou uma base sólida (núcleo puro, dois mixins, CI
bloqueante, planejador com orçamento) e quatro fontes de fragilidade: o
registro da colônia só ia ao disco no fechamento, o trabalho correu em duas
linhas paralelas, a espera de material cresceu por casos especiais sem um
conceito próprio, e a lógica de decisão mora na camada `fabric/`, que só
GameTest alcança.

## Decisão

Sete pontos, aplicados **um por commit**, cada um verificado antes do
próximo. O número é o da avaliação; a ordem de execução está na tabela.

| # | Ponto | O que entra agora | O que fica para depois |
|---|---|---|---|
| 1 | Salvar o estado durante o jogo | `sync` também em `ServerLifecycleEvents.BEFORE_SAVE` (autosave, `/save-all`), nunca depois do `SERVER_STOPPING` | — |
| 2 | Uma linha de desenvolvimento por vez | regra escrita em `CLAUDE.md` e `AGENTS.md`; a integração de 06-10 vira a única linha | apagar branches antigas fica com o autor |
| 3 | Pedido de material explícito | **fase 1:** `core` ganha o pedido com estado (`aberto → em resolução → entregue / sem solução`), a fonte que está tentando e o motivo; os caminhos atuais o alimentam; `/vc log` o mostra | **fase 2:** a cadeia de fontes passa a *decidir*. Muda comportamento; espera playtest |
| 4 | Decisão no `core`, mundo na borda | padrão "ler o mundo numa foto, decidir em código puro", aplicado a casos pequenos e já duplicados | os demais, cada um quando a regra for tocada |
| 5 | Medir o custo por fase | o detalhamento do ciclo sai a cada N ciclos, não só ≥ 50 ms, e um script o resume | otimizar — só com o número na mão |
| 6 | Comentário não é diário | regra escrita; corrigidos os comentários que a auditoria achou mentindo | reescrita ampla: não |
| 7 | Testes | paridade do registro, auditoria fora da bateria comum, PIT em PR/`main`, filtro de GameTest, `/test`, fixture que monta, regra de pasta | parametrização e divisão do `MinerGameTest` |

### Fora de alcance (não muda)

A separação `core` × Fabric e suas regras ArchUnit; os dois mixins sem
`@Overwrite`; o planejador central; os oito serviços estáticos e o
`ServerMemory`; a colônia trabalhar só com jogador por perto; `ColonyGoals`,
`StockRules`, `PlanningBudget`, `SweepDeadline`; o formato do save e o
`SaveMigration`; o CI bloqueante; `ClimbOut`, `PenEscape`, `MineReturn`; as
versões de Minecraft/Fabric/Loom; nenhum GameTest é apagado.

### Como cada ponto é provado

Teste mais específico primeiro; bateria completa ao fim de cada ponto que
toque produção; duas rodadas `--rerun-tasks` no fim. Cada commit diz o que foi
**comprovado automaticamente** e o que **ainda precisa de playtest**.

## Consequências

- O crash deixa de apagar o cérebro da colônia (ponto 1) — comprovável por
  GameTest no mecanismo; no jogo, só matando o processo.
- A fase 1 do ponto 3 não muda o que a vila faz: só torna visível por que a
  obra espera. A fase 2 depende do que o playtest mostrar.
- Ponto 4 é deliberadamente lento: cada extração vem com teste unitário e sem
  mudar comportamento.

## Estado

| # | Commit | Verificação |
|---|---|---|
| 1 | — | — |
| 2 | — | — |
| 3 | — | — |
| 4 | — | — |
| 5 | — | — |
| 6 | — | — |
| 7 | — | — |

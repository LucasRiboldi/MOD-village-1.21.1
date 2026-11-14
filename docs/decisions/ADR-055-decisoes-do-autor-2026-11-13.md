# ADR-055 — Decisões do autor de 13-11 (pendências da avaliação de qualidade)

**Estado:** aceita pelo autor em 2026-11-13. Fecha a lista "Decisões do autor que faltam" do `TODO.md` e as pendências de
processo da avaliação de qualidade (`docs/audit/2026-11-13-avaliacao-qualidade.md`). Mantém a ADR-050 como está.

## Contexto

A avaliação de 13-11 e a triagem do TODO antigo deixaram 14 decisões que só o autor podia tomar. Elas foram perguntadas
em quatro lotes, com opções, efeito e risco; esta ADR registra o que ele escolheu. Cada item de jogo vira uma regra
própria em `docs/RULES.md` quando é implementado, com o teste que a prova.

## Processo e ferramentas

| # | Decisão | Estado |
|---|---|---|
| 1 | CI: a cobertura da bateria de GameTest fica guardada antes da auditoria; a checagem de espaços compara o commit com o pai; a release só publica com CI verde no mesmo commit | feito (`ci.yml`, `release.yml`) |
| 2 | PR #45 (Sentinela Local) fechada sem merge: o Sentinela já está na `main` | feito |
| 3 | ADR-024 retirada: o `WarehouseIndex` e as classes `Supply*` saem do código | feito |
| 4 | O import de `.fabric/AGENTS.md` sai do `CLAUDE.md`; a ferramenta Fabric o recoloca se for reinstalada | feito |
| 5 | `docs/workers-analysis/` vai para `docs/research/workers-analysis/`, como consulta | feito |
| 6 | Números do agente confirmados como estão: felicidade da vila (ADR-036, item 20), limites do rebanho, mineiro com carvão abaixo de 32 e ferro abaixo de 16 (ADR-039, D3), caminho até a obra de 4 blocos por ciclo até 24 (ADR-039, D2), e o texto da Regra 33 | feito (decisão) |

## Jogo

| # | Decisão | Estado |
|---|---|---|
| 7 | Aldeão sem ofício do mod fica **fora de toda análise** de profissão: tempo de ócio, bloqueio, encalhe, Regra 51, relatórios de tempo e de ações. Continua calçando o caminho e tirando neve (Regras 219 e 220 valem) | a implementar |
| 8 | Com dois ou mais construtores, **uma obra para cada**: o segundo abre outra obra ao mesmo tempo | a implementar |
| 9 | Biblioteca e outras casas com duas portas próximas: **uma entrada por porta**, como hoje | feito (sem mudança) |
| 10 | Deserto: o bosque do lenhador recebe **mudas que surgem** (exceção declarada de recurso sem origem física), plantadas em solo preparado para crescer | a implementar |
| 11 | Corante: o pastor, **no ócio do ofício**, colhe flores em volta da vila e faz corante; as cores cuja flor não existe no bioma **surgem**; se a colheita falhar ou demorar demais, o corante também surge | a implementar |
| 12 | Mina: o mineiro sem ramal livre **abre um ramal três níveis abaixo** do nível em uso, em vez de esperar | a implementar |
| 13 | Grafo: o trecho travado **continua no recálculo** a cada releitura (ADR-050 fica) | feito (sem mudança) |
| 14 | Deserto em duas alturas: quem trabalha em cima e mora embaixo **troca de cama** com um aldeão de outro ofício que mora em cima e trabalha embaixo, quando os dois ganham | a implementar |

## Consequências

- O `TODO.md` deixa de ter decisões do autor pendentes; os itens "a implementar" entram nas fases dele.
- Recurso que surge (itens 10 e 11) é exceção declarada, contada no `VC_SUPPLY` como as outras, e entra na lista de
  "blocos que surgem" de cada análise de jogo.

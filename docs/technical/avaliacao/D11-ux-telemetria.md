# D11 — UX e telemetria em jogo

> Auditoria técnica de 2026-10-10 · commit `b290ed10` · fila em [ACTION-QUEUE.md](ACTION-QUEUE.md)

**Skills:** `minecraft-modding` (comando, overlay, rede) e `minecraft-testing`
(cobertura da bateria nesses pacotes).
**Nota da dimensão:** 7/10.

## O que existe

| Peça | O que mostra | Evidência |
|---|---|---|
| `/vc log` | colônia ativa, profissionais, prioridade da obra, felicidade, alcance a pé e **"Esperas longas"** acima de 2 min (`[ESPERANDO] <profissão>: sem tarefa aberta — há N min`) | `VillageLogCommand.java:40-141`; `VillageLogPresenter.java:70-71` (`LONG_WAIT_MILLIS = 120_000`), `:121-171` |
| Overlay de profissão e de obra | painel com nome e profissão; progresso da obra e primeiro material que falta | `client/VillageColonyClient.java:14-17`; servidor envia a cada 20 tiques (`OverlaySync.java:16,26-44`) |
| Diário de ações | JSONL por aldeão + `action_report.py` | `ActionJournal.java:50` |
| Linhas `VC_TIME`/`VC_COST` | réguas de tempo e custo | `WorkTime.java:57,179`; `CycleCost.java:178,207` |

**Convivência cliente/servidor:**
- Cliente Vanilla em servidor com o mod: o servidor só envia o overlay se o
  cliente tem o canal (`OverlaySync.java:40`, `ServerPlayNetworking.canSend`), e a
  placa de nome continua escrita no servidor. Correto.
- Cliente com o mod em servidor Vanilla: nada chega, e os renderizadores saem
  com estado vazio (`ProfessionOverlayRenderer.java:40,45`). O Mod Menu envia o
  pedido sem `ClientPlayNetworking.canSend` (`ClientProfessionPolicies.java:31,36`);
  o efeito não foi testado.

## Achados

| ID | Sev. | Achado | Evidência | Impacto | Recomendação | Teste que valida |
|---|---|---|---|---|---|---|
| AUD-19 | P3 | O jogo não mostra a Regra 51 | `/vc log` mostra esperas de mais de 2 min, mas nem ocioso% nem bloqueado% por ofício; o veredito só sai fora do jogo, pelo `time_ledger.py` | O autor só descobre o ofício ocioso depois da sessão (caso de AUD-01) | Uma linha por ofício no `/vc log` com a última janela do `WorkTime` (trabalho / espera / ócio) e a marca "Regra 51" acima de 15% | Teste do presenter com janela sintética de 16% |
| AUD-11 | P2 | `fabric.command` com 2,4% de cobertura na bateria e `client.*` com 0% | D7 | Regressão de texto ou de permissão passa no CI | D7 | D7 |

As mensagens são acionáveis: o `[ESPERANDO]` traz o motivo e o tempo, e os
`WARN` da bateria dizem o que fazer (`housing is now a colony priority`,
`task returned to the queue`).

## Não verificado

- Overlay num cliente sem nenhum outro mod (o `.minecraft` de teste tem Sodium e
  Iris, que mexem em renderização).

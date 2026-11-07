---
name: roteiro-correcoes-2026-10-10
description: "Estado do roteiro de correções da auditoria de 10-10 — PRs empilhadas #20→#24, o que está feito, o que falta (RC2 do mineiro, playtest)"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-11T05:58:17.065Z
---

Roteiro: `docs/technical/Roteiro-Correcoes-2026-10-10.md` (tabela de estado no fim). Decisões do autor em ADR-041.

PRs empilhadas, nenhuma na main ainda (2026-10-11): #20 auditoria → #21 integração Codex (0.3.24) → #22 save (AUD-02/03) → #23 testes (AUD-06/07/11) → #24 Fase 2 (AUD-25, regra do ócio, AUD-19, RC1+RC2 do mineiro, `claude/correcoes-fase2-cadeia`) → #25 Fases 5/6 (AUD-08/11/12/13/14/21/22/23, `claude/correcoes-fase5-6-higiene`; bateria 8/8, 705/705).

**Why:** a raiz do AUD-01 (Regra 51 reprovada) é a mina: o dono do único ramal ia e voltava numa descida até um patamar de pedra maciça, e na descida nenhum prazo corria. A RC1 pôs um prazo na descida; a RC2 corrigiu a rota, que seguia o rumo da escada girada em vez da escada cavada.

**How to apply:** RC1 (08b0c34f) e RC2 (36d3946b) do mineiro estão feitas e na PR #24. Os próximos passos são a 2.4 (medir o fundidor e o despacho) e a 2.5 (playtest de aceite). Sem jogo restam AUD-15 (imports), 20 (classes grandes), 10 (estáticos) e 09 (ADR); AUD-11 falta command/cliente; AUD-18 (tags) não foi feito por ser publicação. Nada da Fase 2 foi visto em jogo. Para ler o save e as regiões, os scripts do scratchpad (`nbtread.py`, `mcablocks.py`) somem com a sessão; refazer se precisar. Ver [[cadeia-de-producao-recursiva]], [[playtest-2026-10-08-noite]].

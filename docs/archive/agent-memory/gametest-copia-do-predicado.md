---
name: gametest-copia-do-predicado
description: "Gametest que reimplementa um predicado de produção passa a validar a cópia, e a correção real entra sem ninguém notar"
metadata: 
  node_type: memory
  type: feedback
  modified: 2026-09-05T18:25:35.316Z
---

`MinerGameTest.realFooting` reimplementava, linha por linha, o
`MinerWork.footingIn` — as duas mesmas chamadas a `BuilderApproach`. Em
2026-09-05 a correção da escada do jogador entrou no `footingIn`, e o teste do
E32 continuou medindo o predicado **antigo**: verde, e sem tocar no código
corrigido.

**Why:** teste que valida uma cópia da regra não valida a regra. É primo do
[[gametest-registro-silencioso]] — nos dois casos a bateria diz "passou" sobre
uma coisa que não é a que roda em jogo.

**How to apply:** quando um gametest montar um objeto que a produção também
monta — `Footing`, política, comparador, predicado —, **exponha o de produção**
e chame-o, em vez de repetir as linhas. Aqui virou
`public static MinerReach.Footing footingIn(ServerWorld)`, e o gametest passou a
delegar. Antes de confiar num teste que cerca uma correção, verifique se o
caminho que ele exercita é mesmo o que mudou.
</content>
</invoke>

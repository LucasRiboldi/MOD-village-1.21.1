---
name: grafo-aneis-diagnostico-0435
description: "0.4.36 tirou o anel da plantação e as quinas fixas (Regra 208) e tratou o quintal fechado (Regra 209); 7 portas de x=45–65 são o lago, ainda sem diagnóstico"
metadata:
  node_type: memory
  type: project
  modified: 2026-11-06T04:00:03.871Z
---

Em 2026-11-06 o autor escolheu B e D2 e a candidata 0.4.36 saiu, não jogada.

- Causa dos azuis centrais da 0.4.35: quinas de anel em `fixed`, que `RoadTips.prune` não corta. `RoadLines` não tem mais âncora; recebe `farmRings`, e `FarmRingPaths` retira o trecho colado na plantação se a rede continua inteira.
- O caminho do anel continua no chão e entra no grafo pela varredura. Só parar de pedir o anel não bastaria num save com anel calçado: por isso o anel antigo também vira sobra, a qualquer distância.
- `doorFronts`: a frente do ferreiro de armas é o primeiro chão depois da porta (122,-162), que tem caminho até a rua; o último chão antes do rebolo é piso da forja. Tinha dito o contrário ao autor na opção D2 e corrigi antes de implementar.
- Conferir no próximo jogo: até 4 azuis, plantação com uma entrada, anel antigo voltando ao solo pelo pedreiro.

**Why:** a conta de "restam 4 azuis" foi feita sobre o save, e a Regra 206 já tinha sido dada como resolvida antes do jogo mostrar o contrário.
**How to apply:** tratar como candidata até o jogo confirmar. Próximo diagnóstico pendente: "MAE e lago" (`TODO.md`, Abertos do jogo da 0.4.35).

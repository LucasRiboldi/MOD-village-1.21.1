---
name: quadrado-da-vila-proposta
description: Caixa da vila (ADR-003 Emenda 6) implementada em 7df8fca — janela de altura, crescimento +12/rua, foco só com jogador dentro +5 min; não visto em jogo
metadata:
  type: project
---

Autor quer: (1) busca de camas em coluna com janela de altura, não esfera;
(2) planejar a partir da vila, não do centro/jogador; (3) vila = caixa que nasce
no tamanho típico, cresce com ruas/construções e recentra. Avaliação e marcos em
docs/research/2026-09-30-quadrado-da-vila.md (commit ee15d96). Sugestões minhas:
começar pela caixa da vila gerada; retângulo; cresce só com construção/lote/rua
(nunca mina); nunca encolhe; teto 256/lado; margem de identidade 16; foco +32.

**Why:** 67 camas de Trial Chambers no subsolo (duas sob vilas) podem virar
"vila" pelo gatilho de chunk carregado + mineiros perto; centro por média de
camas causa deriva (obra órfã).
**How to apply:** esperar as 6 decisões do §5 antes de codar; marcos 1–5
independentes dos 6–7 (89 usos de center() em 34 arquivos). Ver [[medidas-das-vilas]], [[e51-identidade-da-vila]].

**Implementado 2026-10-01 (7df8fca, pushed; JAR 0AB84595 instalado em mods, anterior 89C93B68 no scratchpad):** marcos 1–5 com as edições do
autor: foco sem margem do jogador (só a caixa + 5 min, fora disso nada roda);
construção/lote +12, rua inclui o bloco. 1.246 unitários, 533 GameTests.
Foco só testado no core (GameTest sem jogador). Sinais no log: `measured at`,
`grew to`, `is attended`, `rests — no player inside`.

**Armadilha ArchUnit:** o store congelado ordena as dependências por número de
linha; empurrar uma chamada congelada para outra linha (ex. adicionando imports
acima) reprova. VillageScanner.survey precisa da chamada toColonyPos(trigger)
entre as linhas 89 e 91. Pacote novo `fabric.world` (só MC + core) evita ciclos.

# ADR-037 — Decisões do autor depois do playtest de 2026-10-07

**Status:** Accepted
**Date:** 2026-10-07
**Decision Type:** Gameplay
**Origem:** o primeiro playtest do JAR 0.3.1 (log de 06-10 23:41 a 07-10 00:43, Spark
`QK8BBjPGXr`) e a resposta do autor à lista de defeitos, melhorias e conflitos C1–C8 da ADR-036.

## Decisões

| # | Decisão |
|---|---|
| F1 | Nascem **5 árvores nas bordas da vila para cada 10 aldeões**, longe das estruturas |
| F2 | Para cada **5 aldeões**, uma **árvore natural** da espécie que a obra pede, perto das bordas e longe das estruturas |
| F3 | Capim, flor e samambaia não impedem o plantio (o defeito que deu 0 árvores na fundação) |
| M1 | A mina não prende ninguém e se anda nela sem travar; o mineiro não fica ocioso; caminhos de mineração mais úteis e persistentes. Cada aldeão de profissão é independente, com sua rotina |
| R1 | Na reunião da vila, a comida de qualquer baú é doada e comida pelos aldeões; o convívio os deixa felizes. A felicidade conta o trigo como comida, e o fazendeiro assa pão com o trigo guardado (C7 b e c) |
| V1 | Desempregado age como no Vanilla |
| V2 | Todo aldeão, ao ir dormir, fecha a porta da casa; o teletransportado para a cama também (C3) |
| B1 | O construtor põe bloco de qualquer lugar dentro da zona da obra, sem subir ou ir até cada bloco (resolve também as toras inalcançáveis, M4) |
| L1 | Lenhador busca até a borda + 10; sem alvo, até a borda + 20 (M2) |
| M3 | O alívio do baú cheio fica |
| C1 | Só o `storage_majest`: sai o armazém de emergência no salão da mina |
| C2 | Teto de 3 compartimentos de 64 por item vale para todo baú, o do mineiro inclusive (sai o teto de 256 por tipo) |
| C4 | Fica a forma mais completa (eixo, metade, formato) |
| C5 | O mais simples: fica como está |
| C6 | Fica como está; pesquisar melhorias eficientes |

## Estado

| # | Commit | Verificação |
|---|---|---|
| F1–F3 | `7e53f848` | `ForestQuota` (core): fundação 5 × (aldeões ÷ 10), mínimo 5; mais 5 a cada dezena; obra esperando madeira ganha 1 árvore natural madura a cada 5 aldeões (sem muda do baú, sem fazendeiro). `VillageForest`: chão pelo mapa de altura, contorno de 4 a 10 blocos fora da caixa (sem caixa, o anel de 48–56), nenhuma estrutura no quadrado 11×11 nem rente ao chão; capim, flor, samambaia e neve fina são limpos; busca que falha espera 5 min (fim dos picos de 100 ms). GameTest do capim (era 0 de 8) e da borda; 628/628 ×3 |
| C1, C2 | `ac4fa380` | `MineOverflowStorage` apagado (classe, GameTest e as leituras em `ColonyChests` e `ProfessionChestOverflow`); baú de emergência já posto fica no mundo como baú comum, fora da conta. `MinerHaul.TYPE_CAP` = 3 × 64 = 192. 627/627 ×2 |
| B1, M4 | `77267f06` | `BuilderApproach.isInsideZone`: dentro da pegada da planta mais 4 blocos, em qualquer altura, o construtor põe o bloco sem andar até ele (as toras que ficavam de lado por falta de lugar de pé deixam de ser caso); fora da zona ele anda até entrar. Ninguém vivo no espaço do bloco: se houver, ele se afasta e tenta de novo. 629/629 ×3 |
| L1 | `e04a2e1a` | `ResourceReach.WIDE_MARGIN` = 20: busca do lenhador sem árvore faz a próxima ir até a borda + 20; achando, volta à borda + 10 (`ResourceSearches.found`). 1352 unitários; 629/629 ×2 |
| M1 (1ª parte) | este commit | **Cascata corrigida:** ramal zero fechado antes do salão dá `LevelAdvance.BLOCKED` em vez de nível feito; a mina gira a escada (`MineTrouble.rerouteBlockedStair`) e, esgotadas as hélices, segue como fundo (rampa ou boca nova). **Mineiro sem ramal livre não fica ocioso:** raspa pedra exposta em volta da vila, fora das células planejadas da mina (essa exclusão não é provada por teste: a mutação sobreviveu). `MineTest` novo; 629 ×3 (uma rodada com KF-003) |

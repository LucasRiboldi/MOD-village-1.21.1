# TODO

**Atualizado:** 2026-10-20 (biomas, 0.3.69–0.3.76; antes: 2026-10-14, ciclo H3). Só o que está **aberto**. Os checklists de playtest de
versões ≤0.3.44 foram arquivados em `docs/archive/technical/TODO-ate-2026-10-07.md` (grep, nunca
inteiro). Estado vivo: `STATE.md`. Plano de melhoria: `docs/technical/Plano-Melhoria-2026-10-14.md`.

Prioridade: 🔴 bloqueia · 🟠 importante · 🟡 melhoria · 🟢 futuro.

## 🔴 Biomas — o mod em todos os biomas (bancada de 20-10, 0.3.69–0.3.76)

Medido em `scripts/biome_sim.py` (uma vila por bioma, 2 dias, margem ±1–2 pontos; régua em
`docs/testing/roteiro-por-bioma.md` §5). Notas atuais: savana 9,3 · meadow 9,0 · planície 7,2–9,9 ·
neve 7,2 · taiga 6,8–8,8 · deserto 5,3–5,8 · taiga nevada 4,2 · floresta 4,0 · cerejeira 3,0 ·
ermos 3,0. **Nada disto foi jogado** — o próximo passo de verdade é jogar pelo roteiro.

Feito neste ciclo (não repetir): vila espalhada e vila na borda de bioma viram colônia (ADR-003
Emenda 9); casa fundadora com busca maior, aplainamento e limpeza de mata; G2 (peça que trava
surge no baú, registro `villagecolony/supply-gaps.jsonl`); caminho com o bloco do bioma; obra
comum limpa lote na mata; mineiro escora areia; terraplanagem do lote fase 1 (±2); rotas
próprias (madeira e argila com prova, descascar, ingrediente de superfície); `/vc bioma`,
`VC_BIOME`, `biome_report.py`, `BiomeCoverageGameTest`.

- [ ] 🟠 **Casa presa no último bloco — falta jogo.** Taiga: 44 ciclos em "1 blocks left". A causa
      vista na bancada era a copa de pinheiro de fora do lote: 29 peças de parede puladas por
      "spruce_leaves is in the way" e a tocha da parede sem apoio. 0.3.77: folha natural dá lugar à
      peça; a peça inalcançável surge depois de 3 vezes de lado (`UnreachablePieces`,
      `villagecolony/unreachable-pieces.jsonl`, `VC_UNREACHABLE`). Bancada: taiga 0 → 2 casas
      concluídas, nota 7,6. Pendente: tronco natural no volume ainda é pulado ("spruce_log is in the
      way", 3×); peça de lado por apoio (`UNSUPPORTED`) que nunca ganha apoio ainda segura a obra.
- [ ] 🟠 **Deserto jogado (0.3.77, 20-10, Spark IgpFCEon8m): 0 obras em 2,8 dias.** 0.3.78 corrige três
      causas medidas na bancada com a semente do autor (`--near=-460,601`): a rua de arenito liso da
      vila gerada não era rua (só no índice, que só se enche de rua), a busca de rua só via +2 do
      centro, e a duna acima da rua não era cortada (corte até 5). Bancada: 0 → 4 casas, nota 5,1 → 8,8.
      0.3.83 (autor, 21-10: "mudar de casa"): quem desiste do trabalho morando 2+ abaixo dele passa a
      dormir numa cama livre da vila na altura do trabalho, ou troca de casa com um aldeão sem ofício
      que mora lá (HomeMove); à noite o jogo já o leva para a cama. Bancada: 9,3 / 9,3 / 5,9 — na de
      5,9 todas as camas de cima eram de aldeões com ofício e o construtor não tinha para onde ir.
      Pendente 🟠: esse caso (decisão do autor: trocar com quem tem ofício? construir casa em cima?).
      0.3.82 (autor, 21-10, depois de jogar a 0.3.81): a escada fixa e a subida por pilar saíram —
      "quebra o visual", "a movimentação precisa ser a mais natural possível". Só o mineiro cava
      (degrau, túnel) para subir; os outros, só degrau natural (regra de 15-10 de volta). Pendente 🔴:
      quem mora na parte baixa da vila do deserto volta a não chegar à obra de cima (bancada: 0 obras
      em 2 rodadas com o construtor morando em y63) — falta uma saída natural, decisão do autor.
      0.3.81 (deserto jogado na 0.3.80, nota 8,2, Spark ETMetpWVS9; a vila tem duas alturas, y63 e y80+):
      toda profissão que desiste abaixo do destino sobe até a altura dele (pondo bloco ou cavando — a
      regra de 15-10 "só o mineiro" vale fora desse caso), com prazo de 2 min e descanso de 5; escada
      fixa de arenito entre as alturas (VillageStairs); viveiro do anel do baú aceita areia da vila a
      céu aberto, só na vila em duas alturas e dentro da caixa; canteiro novo na areia além da borda da
      roça (mais trigo para o pastor cruzar os bichos). Pendente: na bancada a nota variou de 4,4 a 9,0
      (sem ganho claro sobre a 0.3.80); escada parou em 4 de 18 degraus por peça da vila; KF-006.
      0.3.80 (deserto jogado na 0.3.79, nota 9,1, Spark jiRJWBtgDi): encalhado abaixo do caminho sobe
      até a altura dele (pilar/escada rumo ao destino — o mineiro da casa baixa saía "after 0 steps" 9×);
      grama também no chão do curral; viveiro primeiro em volta do baú do lenhador, na altura dele;
      terracota, feno e argila aparecem quando a obra pede (AppearingPieces, sem pedido nem falta).
      Pendente: KF-005 (teste do carvão do mineiro cai na bateria inteira, 3/7).
      0.3.79: fazendeiro vai ao lugar de pé ao lado da planta (FarmerStand) e, como o construtor, anda
      por etapas quando longe — desistências 7 → 0 na bancada; grama ao pé do poste da ovelha amarrada
      (PostGrass, pedido do autor). Pendente: a grama do poste não foi exercida na bancada (o pastor não
      amarrou ovelha nenhuma) — só GameTest; pastor segue 75% ocioso; lenhador e carpinteiro parados
      (40–60% ócio); cacto e cana contam como chão do lote; a bancada varia muito entre rodadas (nota
      3,9 a 8,8 com o mesmo código) — comparar com 2+ rodadas.
- [ ] 🔴 **Jogar 0.3.77 pelo roteiro** (`docs/testing/roteiro-por-bioma.md`): deserto e uma floresta,
      com `/vc bioma nota`; relatório com `scripts/biome_report.py`. A bancada não vê casa torta,
      aldeão preso nem rua estranha.
- [ ] 🟠 **Rotas que travam na execução (`STALLED`).** Deserto: arenito liso (7×), cortado (3×) e comum —
      fundidor e pedreiro não pegam a tarefa; deserto também tem fundidor 100% parado. Neve: bloco e
      camada de neve agora têm tarefa e travam (6× e 2×). Savana: vidraça, terracota colorida.
- [ ] 🟠 **Terraplanagem fase 2** (a vila inteira em subida de 1 em 1, plano de 15-10): ermos e taiga
      nevada têm 69% e 28% das recusas "sem chão na janela", relevo acima de 2.
- [ ] 🟠 **Lavoura e viveiro na neve e no deserto.** `FarmerTill` e `TreeNursery` exigem ar em cima do
      chão: a camada de neve barra. No deserto, roça nova exige grama/terra (aterrar com terra da mina) e
      o fazendeiro "could not reach the crop" (9–11×). Fazendeiro 75–81% parado no deserto.
- [ ] 🟠 **Pastor sem ovelha.** Deserto e neve: 84–96% parado, "no sheep with wool within the radius";
      lã branca é a falta `NOT_FOUND` mais comum (todas as vilas). Decisão: o pastor traz/cria animais?
- [ ] 🟡 **Estilo e madeira do bioma vizinho** para biomas fora da tabela (ermos → deserto, picos de
      gelo → neve; selva, floresta escura, cerejeira com a madeira local). Hoje caem em planície de carvalho.
- [ ] 🟡 **Reserva de madeira** onde não há árvore (lenhador 85–88% bloqueado no deserto).
- [ ] 🟡 **Mineiro: alvo alto demais e sem onde pisar** (TOO_HIGH/NO_STANDING_POSITION em todo bioma) —
      a mina 2ª fase.
- [ ] 🟡 **Ofício sem trabalho ajuda os outros** (cadeia da Regra 51 para pastor e fazendeiro) — mexe
      na Regra 59, **decisão do autor**.
- [ ] 🟡 `NO_TASK` novos: tronco e madeira de acácia na savana. Melancia (`melon`) sem rota: o
      fazendeiro não planta melancia.
- [ ] 🟡 **Bancada mais fiel:** 3 sementes por bioma; vila posta por `/place structure` não fica
      registrada como estrutura (mede o pior caso); medir o custo da limpeza de mata (`cost_ledger`).
- [ ] 🟢 `MinerDirectPathGameTest` intermitente (~1/3, já no 690dee98) — card de tarefa aberto.
- [ ] 🟢 PR e merge na `main` das branches `fix/varredura-g1-g3` e `feat/biomas-simulacao` (ADR-035).

## 🔴 Auditoria técnica 10-10 (fila AUD, P0→🔴 P1→🟠 P2→🟡 P3→🟢)

Detalhe, evidência e teste de aceite: `docs/technical/avaliacao/ACTION-QUEUE.md`.
Ordem de execução: `docs/technical/Roteiro-Correcoes-2026-10-10.md`.
Scripts: `scripts/audit/check-*.sh`.

- [ ] 🔴 **AUD-01** Regra 51 reprovada nas duas sessões de 10-10 (0.3.23: SMELTER 91%, BUILDER 92%, MINER 17%;
      fundidor 311/311 soltas). Raiz (diagnóstico de 11-10): o dono do único ramal vai e volta numa descida até
      um patamar de pedra maciça sem nenhum prazo correr; a rota vinha do rumo da escada girada, não da cavada.
      RC1 (prazo da descida) e RC2 (rota pela escada cavada) feitas; falta o playtest.
      Roteiro: `docs/technical/Roteiro-Correcoes-2026-10-10.md`, 2.2.
- [x] 🟠 **AUD-02** Save malformado não apaga mais as colônias: entrada ruim é descartada com aviso
      (`ColonySaveReader.readEntry`), versão negativa vira 0, `toBlock` não lança. `ColonySaveFuzzTest` 13/13.
- [x] 🟠 **AUD-03** `saveVersion` gravado no save (`SaveMigration.stamp`, `11bd4799`).
- [x] 🟠 **AUD-05** `codex/fluxo-grafo-waypoints` integrada (ADR-041 #2) na `claude/integra-fluxo-grafo-waypoints`,
      0.3.24: 1402 unitários, 696/696 GameTests × 3, PIT 85,56%. Falta: PR na `main` e playtest.
- [x] 🟠 **AUD-06** `WorkDemandTest` fixa cada material da obra: `WorkDemand` 20/20 mutantes mortos; PIT 86,17%, folga 24.
- [x] 🟡 **AUD-07** Régua v2: C14 conta seções 🔴, C01 conta como o `wc -l`, sem quebra em cp1252. Recalculadas: 3,14 e 3,14.
- [x] 🟡 **AUD-08** `STATE.md` reescrito em 11-10 (709 → 111 linhas); o texto antigo, sem edição, em `docs/archive/technical/Historico-2026-10.md`.
- [ ] 🟡 **AUD-09** Ciclo raiz ↔ `fabric.*` por `VillageColonyMod` como localizador global.
- [ ] 🟡 **AUD-10** 181 estáticos mutáveis; config estática não-final (`MineLease`, `TreeChoice`, `MinerDetours`, `FarmerWork`). A regra ArchUnit já existe congelada: esvaziar o congelado.
- [ ] 🟡 **AUD-11** `fabric.network` com 67% de linhas no unitário (meta de 60% batida em 11-10): codec, os três
      pacotes pelo fio (`ProfessionPolicyPayloadsTest`), save da política e porta de permissão, com mutação conferida.
      Falta: `fabric.command` em 27% (bateria) e o cliente em 0%; receptores por lambda e o resto do `/vc log`.
- [x] 🟡 **AUD-12** Aviso quando outro mod tira a tarefa da colônia do `Brain` (`BrainHeartbeat`, `BrainHeartbeatGameTest`); `suggests: modmenu`. `breaks` fica para quando houver mod testado (nível 4 do modsmith).
- [ ] 🟡 **AUD-04** Ciclo p95 67 ms na 0.3.22, 27,7 ms na 0.3.23: medir de novo (≥ 20 amostras) antes de otimizar.
- [x] 🟡 **AUD-25** `TASK_RELEASED` grava o motivo (`Task.release(why)` nas 15 solturas das profissões) e o
      `action_report.py` agrupa as solturas por motivo.
- [ ] 🟡 **AUD-13** Alternância de obras: `docs/behavioral-tests/README.md` atualizado (11-10); falta provar em jogo.
- [x] 🟢 **AUD-14** CHANGELOG com 0.3.1 a 0.3.23, reconstruídas do commit que trocou a versão (11-10).
- [ ] 🟢 **AUD-15** 848 imports não usados (`RemoveUnusedImports` no Error Prone).
- [ ] 🟢 **AUD-16** Cache de `CraftingLookup.listAllOfType`, se o Spark mostrar.
- [x] 🟢 **AUD-17** `/vc log` continua aberto (ADR-041 #4).
- [ ] 🟢 **AUD-18** Tag por JAR publicado (0.3.4–0.3.23 sem tag).
- [x] 🟡 **AUD-19** `/vc log` mostra a última janela do `WorkTime` por ofício e marca a regra do ócio (ADR-041 #4).
- [ ] 🟢 **AUD-26** **Escolha do autor:** número da regra do ócio (colide com a Regra 51 do `docs/RULES.md`).
- [ ] 🟢 **AUD-20** Dividir as 15 classes em 488–500 linhas antes da próxima mudança nelas.
- [x] 🟢 **AUD-21** 8 rodadas em 11-10: a primeira série teve 2 falhas do `FreeChestCountGameTest` (aldeões dos vizinhos no raio de 64), corrigido; a série final deu 8/8, 705/705.
- [x] 🟢 **AUD-22** A fundação usa a semente do mundo e do lugar. Sem teste: a planta não tem processador nem baú com loot, então hoje a semente não muda nada observável.
- [x] 🟢 **AUD-23** Javadoc de `VillagerEntityMixin` não diz mais "o único mixin" (são três).
- [ ] 🟢 **AUD-24** **Escolha do autor:** apagar as 7 branches já contidas na `main`.

## 🟠 Fluxo físico de materiais da obra

- [ ] Medir no save real a entrega de `CALCITE` pelo mineiro e comparar contra as 63 inserções
      instantâneas registradas; rota declarada e meta nominal não substituem a confirmação de entrega.
- [ ] Para cada bloco sem catálogo, criar primeiro uma cadeia executável de profissão ou mantê-lo no
      fallback terminal. Não voltar a declarar uma rota genérica sem trabalhador que a cumpra; folhas
      permanecem fora da coleta do lenhador pela regra de preservar a copa.
- [ ] Conciliar demanda da obra, reserva de tarefa, estoque em baú e entrega para que uma reserva
      sem retirada não impeça outra profissão de atender a peça.
- [x] Remover o ciclo de reserva/liberação do fundidor quando falta insumo, mantendo
      alternativa de construção produtiva enquanto espera. `SmelterGameTest` 5/5 e
      GameTests 652/652; confirmar no próximo save a redução de liberações e a
      ociosidade abaixo de 15%.
- [x] Registrar no relatório cada fallback terminal (`VC_SUPPLY_ERROR`) por peça, profissão e
      tentativas; `test_analyze_village_log` 11/11 e Python 96/96. Comparar o próximo playtest
      com os 334 abastecimentos instantâneos da sessão anterior.
- [x] Implementar a ADR-040: quatro despachos executáveis sem entrega física acionam o fallback;
      consulta do construtor e reserva de tarefa não contam, e a entrega no baú zera o relógio.
      `BuilderMaterialsGameTest` e GameTests 654/654; medir no save a entrega profissional e
      cada fallback terminal antes de reduzir a política.

## 🟠 Erros abertos

- [ ] **KF-002** (aldeão de coleta some no tique 1): não reapareceu em ~27 baterias de 07-10.
      Correção recomendada: montar o cenário dentro da própria arena, com o raio protegido encurtado
      por gancho de teste (como `MineDigging.shortenSurfaceRadiusTo`), em vez de 65–97 blocos fora.
- [ ] **KF-003**: causa achada e corrigida (fusão de colônias no tique de teste). Fecha com
      `python scripts/gametest_battery.py 30` limpo; até agora 7 + 6 limpas.
- [ ] **`lumber_stall_guard`** caiu 1 vez em ~6 baterias de 07-10 (tarde), sem relação com a mudança
      da hora. Medir com o script antes de investigar.

## Regra de teste

Todo teste novo ou alterado: **o commit diz a mutação feita e que o teste caiu com ela**. Teste que
passa com a regra desligada não entra como prova. Cenário fora da arena de 8x8 bate na barreira da
borda (ela já fez um teste passar sem medir nada, o D2).

Plano de 07-10 aplicado: D2, B1, B3, B4, B5, B6 e o arquivo do save provados por mutação (ADR-039).
Falta só fechar e abrir um mundo de verdade (playtest).

## 🟡 Estrutura

- [ ] Outros seis arquivos de produção estão em 497–500 linhas (`SmelterWork`,
      `Mine`, `ClimbOut`, `SurfaceGatheringWork`, `Worker`, `BuilderWork`) e devem ser divididos
      antes da próxima mudança neles. `ColonyGoals` foi reduzido a 500 linhas com a extração de
      `AdvanceConstructionStock`; `FileSizeRuleTest` voltou a passar.
- [ ] Branch local do worktree do Codex (`codex/village-visuals-logistics-mine-sweep`): já toda na
      `main`; remover o worktree.

## Decisões do autor que faltam

- [ ] Aldeões sem ofício 100% parados (cota da ADR-011): vagas para todos, ou ajudantes?
- [ ] Dois construtores na mesma obra.
- [ ] E5/E6: limites da felicidade e do rebanho (números do agente). E7: texto da Regra 33.
- [ ] Números do agente na ADR-039 (carvão < 32, ferro < 16, caminho 4/ciclo até 24).
- [ ] `docs/workers-analysis/`: mover para `docs/research/`, arquivar ou apagar.

## Decidido, não fazer

- Obra antiga fica como está: mudanças valem daqui para frente (casas antigas sem piso não são refeitas).
- Peça sem rota: depois de 4 tentativas aparece no baú de quem precisa (`BiomeConstructionSupply`,
  já em vigor, testado em `LocateFallbackGameTest`).

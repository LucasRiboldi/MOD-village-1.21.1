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

- [ ] 🔴 **Casa presa no último bloco.** Taiga, lote aplainado: a casa subiu 305 blocos e ficou 44
      ciclos em "1 blocks left", segurando a vaga única de obra. Achar a peça (log `builders:` e o
      save) e por que não assenta — parentes: [[peca-de-parede-sem-direcao]], [[riscado-nao-e-assentado]].
- [ ] 🔴 **Jogar 0.3.76 pelo roteiro** (`docs/testing/roteiro-por-bioma.md`): deserto e uma floresta,
      com `/vc bioma nota`; relatório com `scripts/biome_report.py`. A bancada não vê casa torta,
      aldeão preso nem rua estranha. JAR 0.3.74 instalado — instalar o 0.3.76 antes.
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
